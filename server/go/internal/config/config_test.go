package config

import (
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
