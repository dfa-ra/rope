package authz

import (
	"encoding/base64"
	"testing"
)

func TestParseHeaderAndWsParts(t *testing.T) {
	sig := base64.RawURLEncoding.EncodeToString([]byte("sigbytes!!sigbytes!!sigbytes!!ab"))
	raw := "AbC." + "1700000000." + sig
	p, err := ParseWsParts(raw)
	if err != nil {
		t.Fatal(err)
	}
	if p.DeviceID != "abc" {
		t.Fatalf("id %q", p.DeviceID)
	}
	if p.Timestamp != 1700000000 {
		t.Fatalf("ts %d", p.Timestamp)
	}
	rest, err := ParseHeader("Rope " + raw)
	if err != nil {
		t.Fatal(err)
	}
	if rest.DeviceID != p.DeviceID || rest.Timestamp != p.Timestamp {
		t.Fatalf("%+v vs %+v", rest, p)
	}
	if _, err := ParseHeader(raw); err == nil {
		t.Fatal("REST header must require Rope scheme")
	}
}

func TestWsCredsPrefersHeader(t *testing.T) {
	sig := base64.RawURLEncoding.EncodeToString([]byte("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
	header := "deadbeef.1700000000." + sig
	p, err := WsCreds(header, "other", "1", "aaaa")
	if err != nil {
		t.Fatal(err)
	}
	if p.DeviceID != "deadbeef" || p.Timestamp != 1700000000 {
		t.Fatalf("%+v", p)
	}
	q, err := WsCreds("", "CafeBabe", "99", sig)
	if err != nil {
		t.Fatal(err)
	}
	if q.DeviceID != "cafebabe" || q.Timestamp != 99 {
		t.Fatalf("query %+v", q)
	}
	if _, err := WsCreds("", "", "", ""); err == nil {
		t.Fatal("empty must fail")
	}
	if _, err := WsCreds("not-three-parts", "x", "1", sig); err == nil {
		t.Fatal("garbage header must not fall back to query")
	}
}

func TestParseWsPartsRejectsCRLF(t *testing.T) {
	sig := base64.RawURLEncoding.EncodeToString([]byte("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
	if _, err := ParseWsParts("abc\nhttp://evil.1700000000." + sig); err == nil {
		t.Fatal("newline in device id")
	}
	if _, err := ParseWsParts("abc.1700000000.\r" + sig); err == nil {
		t.Fatal("CR in signature")
	}
	if _, err := ParseWsParts("abc def.1700000000." + sig); err == nil {
		t.Fatal("space in device id")
	}
	if _, err := WsCreds("", "abc\r\ndef", "99", sig); err == nil {
		t.Fatal("CRLF query device must fail")
	}
	ok := "deadbeef.1700000000." + sig
	if _, err := ParseWsParts(ok); err != nil {
		t.Fatal(err)
	}
}
