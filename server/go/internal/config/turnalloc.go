package config

import (
	"crypto/hmac"
	"crypto/md5"
	"crypto/rand"
	"crypto/sha1"
	"encoding/binary"
	"fmt"
	"hash/crc32"
	"io"
	"net"
	"strings"
	"sync"
	"time"
)

// AllocResult is a local TURN Allocate against coturn (HMAC + relayed address).
type AllocResult struct {
	OK        bool   `json:"ok"`
	RelayedIP string `json:"relayed_ip,omitempty"`
	Proto     string `json:"proto,omitempty"`
	Error     string `json:"error,omitempty"`
}

const (
	stunMagic        = 0x2112A442
	stunFingerprint  = 0x5354554e
	stunBindingReq   = 0x0001
	stunBindingOK    = 0x0101
	turnAllocReq     = 0x0003
	turnAllocOK      = 0x0103
	turnAllocErr     = 0x0113
	attrMappedAddr   = 0x0001
	attrUsername     = 0x0006
	attrMessageInt   = 0x0008
	attrErrorCode    = 0x0009
	attrLifetime     = 0x000D
	attrRealm        = 0x0014
	attrNonce        = 0x0015
	attrXorRelayed   = 0x0016
	attrReqTransport = 0x0019
	attrSoftware     = 0x8022
	attrXorMapped    = 0x0020
	attrFingerprint  = 0x8028
	errUnauthorized  = 401
	errStaleNonce    = 438
)

type stunAttr struct {
	Type uint16
	Val  []byte
}

type stunMsg struct {
	Type  uint16
	TID   [12]byte
	Attrs []stunAttr
}

// RunTurnAllocate is replaced in tests. Production talks to 127.0.0.1.
var RunTurnAllocate = allocateLocal

var (
	allocCacheMu  sync.Mutex
	allocCacheKey string
	allocCacheAt  time.Time
	allocCacheVal AllocResult
)

func (c Config) cachedAllocate(timeout time.Duration) AllocResult {
	key := fmt.Sprintf("%s|%d|%s", c.TurnSecret, c.EffectiveTurnPort(), strings.TrimSpace(c.PublicHost))
	allocCacheMu.Lock()
	if allocCacheKey == key && time.Since(allocCacheAt) < 30*time.Second {
		out := allocCacheVal
		allocCacheMu.Unlock()
		return out
	}
	allocCacheMu.Unlock()

	res := RunTurnAllocate(c, timeout)

	allocCacheMu.Lock()
	allocCacheKey = key
	allocCacheAt = time.Now()
	allocCacheVal = res
	allocCacheMu.Unlock()
	return res
}

func ResetTurnAllocCache() {
	allocCacheMu.Lock()
	allocCacheKey = ""
	allocCacheAt = time.Time{}
	allocCacheVal = AllocResult{}
	allocCacheMu.Unlock()
}

func allocateLocal(c Config, timeout time.Duration) AllocResult {
	if timeout <= 0 {
		timeout = 800 * time.Millisecond
	}
	user := TurnUsername(time.Now(), time.Hour)
	pass := TurnCredential(c.TurnSecret, user)
	port := c.EffectiveTurnPort()
	addr := net.JoinHostPort("127.0.0.1", fmt.Sprintf("%d", port))

	udp, err := turnAllocate("udp", addr, user, pass, timeout)
	if err == nil && udp.OK {
		udp.Proto = "udp"
		if e := relayedAddressError(udp.RelayedIP); e != "" {
			udp.OK = false
			udp.Error = e
		}
		return udp
	}
	tcp, err2 := turnAllocate("tcp", addr, user, pass, timeout)
	if err2 == nil && tcp.OK {
		tcp.Proto = "tcp"
		if e := relayedAddressError(tcp.RelayedIP); e != "" {
			tcp.OK = false
			tcp.Error = e
		}
		return tcp
	}
	msg := "TURN ALLOCATE не прошёл"
	if err != nil {
		msg = "UDP: " + err.Error()
	} else if udp.Error != "" {
		msg = "UDP: " + udp.Error
	}
	if err2 != nil {
		msg += "; TCP: " + err2.Error()
	} else if tcp.Error != "" {
		msg += "; TCP: " + tcp.Error
	}
	return AllocResult{Error: msg}
}

func relayedAddressError(ip string) string {
	ip = strings.TrimSpace(ip)
	if ip == "" {
		return "ALLOCATE без XOR-RELAYED-ADDRESS"
	}
	if IsPrivateIPv4(ip) || ip == "127.0.0.1" {
		return fmt.Sprintf("ALLOCATE выдал частный адрес %s — неверный external-ip/relay-ip", ip)
	}
	return ""
}

func turnAllocate(network, addr, username, password string, timeout time.Duration) (AllocResult, error) {
	var tid [12]byte
	if _, err := rand.Read(tid[:]); err != nil {
		return AllocResult{}, err
	}
	first := encodeStun(turnAllocReq, tid, []stunAttr{
		{Type: attrReqTransport, Val: []byte{17, 0, 0, 0}},
		{Type: attrLifetime, Val: uint32Bytes(30)},
		{Type: attrSoftware, Val: []byte("rope-turn-check")},
	}, nil, true)

	raw, err := stunRoundTrip(network, addr, first, timeout)
	if err != nil {
		return AllocResult{}, err
	}
	msg, err := parseStun(raw)
	if err != nil {
		return AllocResult{}, err
	}
	if msg.Type == turnAllocOK {
		return allocFromSuccess(msg, network), nil
	}
	code, _, realm, nonce := stunError(msg)
	if code != errUnauthorized && code != errStaleNonce {
		return AllocResult{Error: fmt.Sprintf("ALLOCATE %d", code)}, nil
	}
	if realm == "" {
		realm = "rope"
	}
	if nonce == "" {
		return AllocResult{Error: "ALLOCATE 401 без nonce — coturn не в режиме use-auth-secret"}, nil
	}

	if _, err := rand.Read(tid[:]); err != nil {
		return AllocResult{}, err
	}
	key := longTermKey(username, realm, password)
	authed := encodeStun(turnAllocReq, tid, []stunAttr{
		{Type: attrReqTransport, Val: []byte{17, 0, 0, 0}},
		{Type: attrLifetime, Val: uint32Bytes(30)},
		{Type: attrUsername, Val: []byte(username)},
		{Type: attrRealm, Val: []byte(realm)},
		{Type: attrNonce, Val: []byte(nonce)},
		{Type: attrSoftware, Val: []byte("rope-turn-check")},
	}, key, true)

	raw, err = stunRoundTrip(network, addr, authed, timeout)
	if err != nil {
		return AllocResult{}, err
	}
	msg, err = parseStun(raw)
	if err != nil {
		return AllocResult{}, err
	}
	if msg.Type == turnAllocOK {
		return allocFromSuccess(msg, network), nil
	}
	code, phrase, _, _ := stunError(msg)
	if phrase == "" {
		phrase = fmt.Sprintf("ALLOCATE %d", code)
	}
	switch code {
	case 401:
		phrase = "HMAC 401 — turn_secret не совпадает с static-auth-secret"
	case 438:
		phrase = "HMAC stale-nonce"
	case 403:
		phrase = "ALLOCATE 403 — coturn отказал в выделении (relay-ip/квота)"
	case 508:
		phrase = "ALLOCATE 508 — нет свободного relay-порта (49152–49311)"
	}
	return AllocResult{Error: phrase}, nil
}

func allocFromSuccess(msg stunMsg, proto string) AllocResult {
	ip, _ := xorAddr(msg, attrXorRelayed)
	if ip == "" {
		ip, _ = mappedAddr(msg, attrMappedAddr)
	}
	return AllocResult{OK: true, RelayedIP: ip, Proto: proto}
}

func longTermKey(username, realm, password string) []byte {
	sum := md5.Sum([]byte(username + ":" + realm + ":" + password))
	return sum[:]
}

func uint32Bytes(v uint32) []byte {
	b := make([]byte, 4)
	binary.BigEndian.PutUint32(b, v)
	return b
}

func encodeStun(msgType uint16, tid [12]byte, attrs []stunAttr, integrityKey []byte, fingerprint bool) []byte {
	body := encodeAttrs(attrs)
	// MESSAGE-INTEGRITY is HMAC over header+attrs with length including MI (24 bytes).
	total := 20 + len(body)
	if integrityKey != nil {
		total += 24
	}
	if fingerprint {
		total += 8
	}
	out := make([]byte, 20, total)
	binary.BigEndian.PutUint16(out[0:2], msgType)
	binary.BigEndian.PutUint32(out[4:8], stunMagic)
	copy(out[8:20], tid[:])
	out = append(out, body...)
	if integrityKey != nil {
		binary.BigEndian.PutUint16(out[2:4], uint16(len(out)+24-20))
		mac := hmac.New(sha1.New, integrityKey)
		_, _ = mac.Write(out)
		mi := mac.Sum(nil)
		out = append(out, encodeAttrs([]stunAttr{{Type: attrMessageInt, Val: mi}})...)
	}
	if fingerprint {
		binary.BigEndian.PutUint16(out[2:4], uint16(len(out)+8-20))
		crc := crc32.ChecksumIEEE(out) ^ stunFingerprint
		out = append(out, encodeAttrs([]stunAttr{{Type: attrFingerprint, Val: uint32Bytes(crc)}})...)
	} else {
		binary.BigEndian.PutUint16(out[2:4], uint16(len(out)-20))
	}
	return out
}

func encodeAttrs(attrs []stunAttr) []byte {
	var out []byte
	for _, a := range attrs {
		pad := (4 - (len(a.Val) % 4)) % 4
		hdr := make([]byte, 4+len(a.Val)+pad)
		binary.BigEndian.PutUint16(hdr[0:2], a.Type)
		binary.BigEndian.PutUint16(hdr[2:4], uint16(len(a.Val)))
		copy(hdr[4:], a.Val)
		out = append(out, hdr...)
	}
	return out
}

func parseStun(raw []byte) (stunMsg, error) {
	var m stunMsg
	if len(raw) < 20 {
		return m, fmt.Errorf("короткий STUN %d", len(raw))
	}
	m.Type = binary.BigEndian.Uint16(raw[0:2])
	mlen := int(binary.BigEndian.Uint16(raw[2:4]))
	if len(raw) < 20+mlen {
		return m, fmt.Errorf("обрезанный STUN")
	}
	copy(m.TID[:], raw[8:20])
	rest := raw[20 : 20+mlen]
	for len(rest) >= 4 {
		typ := binary.BigEndian.Uint16(rest[0:2])
		l := int(binary.BigEndian.Uint16(rest[2:4]))
		pad := (4 - (l % 4)) % 4
		if 4+l+pad > len(rest) {
			break
		}
		val := make([]byte, l)
		copy(val, rest[4:4+l])
		m.Attrs = append(m.Attrs, stunAttr{Type: typ, Val: val})
		rest = rest[4+l+pad:]
	}
	return m, nil
}

func stunAttrVal(m stunMsg, typ uint16) []byte {
	for _, a := range m.Attrs {
		if a.Type == typ {
			return a.Val
		}
	}
	return nil
}

func stunError(m stunMsg) (code int, phrase, realm, nonce string) {
	if v := stunAttrVal(m, attrErrorCode); len(v) >= 4 {
		code = int(v[2])*100 + int(v[3])
		phrase = string(v[4:])
	}
	if v := stunAttrVal(m, attrRealm); len(v) > 0 {
		realm = string(v)
	}
	if v := stunAttrVal(m, attrNonce); len(v) > 0 {
		nonce = string(v)
	}
	return
}

func xorAddr(m stunMsg, typ uint16) (ip string, port int) {
	return decodeXorAddr(stunAttrVal(m, typ), m.TID)
}

func mappedAddr(m stunMsg, typ uint16) (ip string, port int) {
	v := stunAttrVal(m, typ)
	if len(v) < 8 {
		return "", 0
	}
	port = int(binary.BigEndian.Uint16(v[2:4]))
	if v[1] == 0x01 && len(v) >= 8 {
		return net.IP(v[4:8]).String(), port
	}
	return "", port
}

func decodeXorAddr(v []byte, tid [12]byte) (ip string, port int) {
	if len(v) < 8 {
		return "", 0
	}
	xport := binary.BigEndian.Uint16(v[2:4]) ^ uint16(stunMagic>>16)
	if v[1] == 0x01 && len(v) >= 8 {
		raw := binary.BigEndian.Uint32(v[4:8]) ^ stunMagic
		b := make([]byte, 4)
		binary.BigEndian.PutUint32(b, raw)
		return net.IP(b).String(), int(xport)
	}
	if v[1] == 0x02 && len(v) >= 20 {
		x := make([]byte, 16)
		magic := uint32Bytes(stunMagic)
		copy(x[0:4], v[4:8])
		copy(x[4:16], v[8:20])
		for i := 0; i < 4; i++ {
			x[i] ^= magic[i]
		}
		for i := 0; i < 12; i++ {
			x[4+i] ^= tid[i]
		}
		return net.IP(x).String(), int(xport)
	}
	return "", int(xport)
}

func stunRoundTrip(network, addr string, req []byte, timeout time.Duration) ([]byte, error) {
	d := net.Dialer{Timeout: timeout}
	conn, err := d.Dial(network, addr)
	if err != nil {
		return nil, err
	}
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(timeout))
	if _, err := conn.Write(req); err != nil {
		return nil, err
	}
	buf := make([]byte, 2048)
	if network == "tcp" {
		if _, err := io.ReadFull(conn, buf[:20]); err != nil {
			return nil, err
		}
		mlen := int(binary.BigEndian.Uint16(buf[2:4]))
		if mlen < 0 || mlen > 1500 {
			return nil, fmt.Errorf("плохой STUN length")
		}
		if _, err := io.ReadFull(conn, buf[20:20+mlen]); err != nil {
			return nil, err
		}
		return buf[:20+mlen], nil
	}
	n, err := conn.Read(buf)
	if err != nil {
		return nil, err
	}
	return buf[:n], nil
}

// StunBinding is a no-auth STUN Binding used by tests and --turn-check.
func StunBinding(network, addr string, timeout time.Duration) (mapped string, err error) {
	var tid [12]byte
	if _, err := rand.Read(tid[:]); err != nil {
		return "", err
	}
	req := encodeStun(stunBindingReq, tid, []stunAttr{{Type: attrSoftware, Val: []byte("rope-turn-check")}}, nil, true)
	raw, err := stunRoundTrip(network, addr, req, timeout)
	if err != nil {
		return "", err
	}
	msg, err := parseStun(raw)
	if err != nil {
		return "", err
	}
	if msg.Type != stunBindingOK {
		return "", fmt.Errorf("STUN type 0x%04x", msg.Type)
	}
	ip, _ := xorAddr(msg, attrXorMapped)
	if ip == "" {
		ip, _ = mappedAddr(msg, attrMappedAddr)
	}
	return ip, nil
}
