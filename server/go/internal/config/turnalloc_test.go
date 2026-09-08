package config

import (
	"crypto/hmac"
	"crypto/md5"
	"crypto/sha1"
	"encoding/binary"
	"net"
	"strings"
	"testing"
	"time"
)

func TestStunRoundTripBindingAndAllocate(t *testing.T) {
	ln, err := net.ListenPacket("udp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer ln.Close()
	secret := "unit-test-secret"
	go serveFakeTURN(t, ln, secret)

	addr := ln.LocalAddr().String()
	mapped, err := StunBinding("udp", addr, time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if mapped != "203.0.113.9" {
		t.Fatalf("mapped %s", mapped)
	}

	user := TurnUsername(time.Now(), time.Hour)
	pass := TurnCredential(secret, user)
	res, err := turnAllocate("udp", addr, user, pass, time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if !res.OK || res.RelayedIP != "203.0.113.50" {
		t.Fatalf("%+v", res)
	}

	bad, err := turnAllocate("udp", addr, user, "wrong-pass", time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if bad.OK || !strings.Contains(bad.Error, "HMAC 401") {
		t.Fatalf("bad creds %+v", bad)
	}
}

func TestRelayedAddressError(t *testing.T) {
	if relayedAddressError("203.0.113.9") != "" {
		t.Fatal("public")
	}
	if !strings.Contains(relayedAddressError("10.0.0.4"), "частн") {
		t.Fatal("private")
	}
	if !strings.Contains(relayedAddressError("127.0.0.1"), "частн") {
		t.Fatal("loopback")
	}
	if relayedAddressError("") == "" {
		t.Fatal("empty")
	}
}

func TestLongTermKeyMatchesRFC5766(t *testing.T) {
	// username:realm:password → MD5
	sum := md5.Sum([]byte("user:rope:pass"))
	got := longTermKey("user", "rope", "pass")
	if string(got) != string(sum[:]) {
		t.Fatal("md5 key")
	}
}

func TestEncodeParseStunIntegrity(t *testing.T) {
	var tid [12]byte
	copy(tid[:], []byte("123456789012"))
	key := longTermKey("1:rope", "rope", "cred")
	raw := encodeStun(turnAllocReq, tid, []stunAttr{
		{Type: attrUsername, Val: []byte("1:rope")},
		{Type: attrRealm, Val: []byte("rope")},
	}, key, true)
	msg, err := parseStun(raw)
	if err != nil {
		t.Fatal(err)
	}
	if msg.Type != turnAllocReq {
		t.Fatalf("type %x", msg.Type)
	}
	mi := stunAttrVal(msg, attrMessageInt)
	if len(mi) != 20 {
		t.Fatalf("mi len %d", len(mi))
	}
	// Recompute over the message without MI/FP, with length including MI.
	cut := raw
	// drop fingerprint (last 8) then MI (last 24)
	cut = cut[:len(cut)-8]
	body := cut[:len(cut)-24]
	binary.BigEndian.PutUint16(body[2:4], uint16(len(body)+24-20))
	mac := hmac.New(sha1.New, key)
	_, _ = mac.Write(body)
	if !hmac.Equal(mac.Sum(nil), mi) {
		t.Fatal("integrity")
	}
}

func serveFakeTURN(t *testing.T, ln net.PacketConn, secret string) {
	t.Helper()
	buf := make([]byte, 2048)
	for {
		n, addr, err := ln.ReadFrom(buf)
		if err != nil {
			return
		}
		req, err := parseStun(buf[:n])
		if err != nil {
			continue
		}
		var resp []byte
		switch req.Type {
		case stunBindingReq:
			resp = encodeStun(stunBindingOK, req.TID, []stunAttr{
				xorMappedAttr(attrXorMapped, "203.0.113.9", 3478, req.TID),
			}, nil, true)
		case turnAllocReq:
			user := string(stunAttrVal(req, attrUsername))
			if user == "" {
				resp = encodeStun(turnAllocErr, req.TID, []stunAttr{
					errorAttr(401, "Unauthorized"),
					{Type: attrRealm, Val: []byte("rope")},
					{Type: attrNonce, Val: []byte("n1")},
				}, nil, true)
				break
			}
			pass := TurnCredential(secret, user)
			key := longTermKey(user, "rope", pass)
			if !checkIntegrity(buf[:n], key) {
				resp = encodeStun(turnAllocErr, req.TID, []stunAttr{
					errorAttr(401, "Unauthorized"),
					{Type: attrRealm, Val: []byte("rope")},
					{Type: attrNonce, Val: []byte("n1")},
				}, nil, true)
				break
			}
			resp = encodeStun(turnAllocOK, req.TID, []stunAttr{
				xorMappedAttr(attrXorRelayed, "203.0.113.50", 49152, req.TID),
			}, key, true)
		default:
			continue
		}
		_, _ = ln.WriteTo(resp, addr)
	}
}

func errorAttr(code int, phrase string) stunAttr {
	v := make([]byte, 4+len(phrase))
	v[2] = byte(code / 100)
	v[3] = byte(code % 100)
	copy(v[4:], phrase)
	return stunAttr{Type: attrErrorCode, Val: v}
}

func xorMappedAttr(typ uint16, ip string, port int, tid [12]byte) stunAttr {
	parsed := net.ParseIP(ip).To4()
	v := make([]byte, 8)
	v[1] = 0x01
	binary.BigEndian.PutUint16(v[2:4], uint16(port)^uint16(stunMagic>>16))
	binary.BigEndian.PutUint32(v[4:8], binary.BigEndian.Uint32(parsed)^stunMagic)
	return stunAttr{Type: typ, Val: v}
}

func checkIntegrity(raw []byte, key []byte) bool {
	msg, err := parseStun(raw)
	if err != nil {
		return false
	}
	mi := stunAttrVal(msg, attrMessageInt)
	if len(mi) != 20 {
		return false
	}
	// Message as received, strip FP then MI, set length to include MI.
	cut := raw
	if fp := stunAttrVal(msg, attrFingerprint); len(fp) == 4 {
		cut = cut[:len(cut)-8]
	}
	body := append([]byte{}, cut[:len(cut)-24]...)
	binary.BigEndian.PutUint16(body[2:4], uint16(len(body)+24-20))
	mac := hmac.New(sha1.New, key)
	_, _ = mac.Write(body)
	return hmac.Equal(mac.Sum(nil), mi)
}
