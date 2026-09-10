package config

import (
	"fmt"
	"os"
	"path/filepath"
	"testing"
)

func TestLoadOrInitUsesLocalDataDir(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "config.json")
	cfg, err := LoadOrInit(path, true, true, "127.0.0.1:0", "")
	if err != nil {
		t.Fatal(err)
	}
	if cfg.DataDir != filepath.Join(dir, "data") {
		t.Fatalf("data dir %s", cfg.DataDir)
	}
	if _, err := os.Stat(cfg.DataDir); err != nil {
		t.Fatal(err)
	}
	if cfg.SetupToken == "" || cfg.ServerID == "" {
		t.Fatal("missing bootstrap material")
	}
}

func TestHTTPDevFingerprintStable(t *testing.T) {
	fp := HTTPDevFingerprint()
	if len(fp) != 64 {
		t.Fatalf("len %d", len(fp))
	}
	if fp != HTTPDevFingerprint() {
		t.Fatal("not stable")
	}
}

func TestLoadOrInitRejectsCrLfPublicHost(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "config.json")
	body := fmt.Sprintf(
		`{"listen":"127.0.0.1:0","data_dir":%q,"public_host":"vps.example.com\n"}`,
		dir,
	)
	if err := os.WriteFile(path, []byte(body), 0o640); err != nil {
		t.Fatal(err)
	}
	if _, err := LoadOrInit(path, false, false, "", ""); err == nil {
		t.Fatal("public_host CR/LF must fail")
	}
	ok := fmt.Sprintf(
		`{"listen":"127.0.0.1:0","data_dir":%q,"public_host":"vps.example.com"}`,
		dir,
	)
	if err := os.WriteFile(path, []byte(ok), 0o640); err != nil {
		t.Fatal(err)
	}
	cfg, err := LoadOrInit(path, false, false, "", "")
	if err != nil {
		t.Fatal(err)
	}
	if cfg.PublicHost != "vps.example.com" {
		t.Fatalf("host %s", cfg.PublicHost)
	}
}
