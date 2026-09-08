package main

import (
	"flag"
	"log"
	"os"
	"time"

	"github.com/dfa-ra/rope/server/go/internal/config"
	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/dfa-ra/rope/server/go/internal/httpapi"
)

func main() {
	cfgPath := flag.String("config", "/etc/rope/config.json", "path to config.json")
	listen := flag.String("listen", "", "override listen address")
	dataDir := flag.String("data-dir", "", "override SQLite data directory")
	initCfg := flag.Bool("init", false, "create config and data dir if missing")
	allowHTTP := flag.Bool("allow-http", false, "listen without TLS (debug only)")
	turnCheck := flag.Bool("turn-check", false, "probe coturn ALLOCATE and exit")
	flag.Parse()

	logger := log.New(os.Stdout, "rope ", log.LstdFlags)
	cfg, err := config.LoadOrInit(*cfgPath, *initCfg, *allowHTTP, *listen, *dataDir)
	if err != nil {
		logger.Fatal(err)
	}
	if err := os.MkdirAll(cfg.DataDir, 0o750); err != nil {
		logger.Fatal(err)
	}
	store, err := db.Open(cfg.DBPath())
	if err != nil {
		logger.Fatal(err)
	}
	defer store.Close()
	if err := store.EnsureMeta(cfg.ServerID, config.ServerVersion, config.ProtocolVersion); err != nil {
		logger.Fatal(err)
	}
	if *turnCheck {
		config.ResetTurnAllocCache()
		rep := cfg.ProbeTurn(1500 * time.Millisecond)
		logger.Printf("turn_running=%v allocate_ok=%v relayed=%s turns=%v err=%q urls=%v",
			rep.Running, rep.AllocateOK, rep.RelayedIP, rep.TurnsListening, rep.Error, rep.Advertised)
		if !rep.Running || !rep.AllocateOK {
			os.Exit(1)
		}
		return
	}
	srv := httpapi.New(cfg, store, logger)
	if err := srv.ListenAndServe(); err != nil {
		logger.Fatal(err)
	}
}
