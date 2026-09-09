# Rope

Private self-hosted messenger for Android. You provision a personal VPS from the app, invite people with a QR / deep link, and exchange E2EE text, voice, photos, files, and group chats through a Go relay that never sees plaintext.

Stack: **Kotlin UI → Rust security core (UniFFI) → HTTPS/WSS → Go + SQLite**.

**Лендинг:** [web/index.html](web/index.html)

```bash
cd web && python3 -m http.server 8080
# http://127.0.0.1:8080
```

## Layout

```
apps/android      Android client
core/rust         identity, envelope, crypto
server/go         relay, mailbox, invites
deployment/       systemd + install.sh
protocol/docs     wire format
docs/             product, architecture, threat model, company, organization, decisions, tasks
web/              marketing landing (open web/index.html)
```

## Quick start (developers)

```bash
# Rust core
cd core/rust && cargo test

# Go relay (debug HTTP)
cd server/go
go test ./...
go run ./cmd/rope-server --config /tmp/rope.json --init --allow-http --listen 127.0.0.1:8443
```

The installer prints `SETUP_TOKEN`. The first `/v1/bootstrap` with that token becomes the owner.

Android: open `apps/android` in Android Studio, or `./gradlew test assembleDebug`. Debug builds can join a local `--allow-http` server via the “Join debug HTTP” screen (`10.0.2.2` from an emulator).

VPS install (Ubuntu/Debian x86_64):

```bash
sudo ./deployment/scripts/install.sh \
  --binary ./rope-server-linux-amd64 \
  --host YOUR.IP \
  --port 8443
```

Re-run with `--upgrade` to replace the binary, keep `data.db`, and (from 0.2.7) install/refresh coturn on the same host so calls get TURN.  
`--reinstall` wipes `data.db` + config and issues a new owner `SETUP_TOKEN` (lost-phone recovery).

Раскатка у пользователя (телефон → SSH → VPS): [docs/rollout.md](docs/rollout.md).

## Сайт

Посадочная страница: откройте [`web/index.html`](web/index.html) в браузере (позже можно включить GitHub Pages из папки `web/`).

## Releases

Push a tag `vX.Y.Z`. GitHub Actions publishes:

- `rope-server-linux-amd64` and `linux-arm64` (в приложении выбирается архитектура, без правки URL)
- Android APK
- `SHA256SUMS`

Production APK signing uses repository secrets documented in [docs/dev-setup.md](docs/dev-setup.md).

Stage 2 (v0.2): photos/files, hold-to-record voice, groups, call signaling, encrypted object store. Details: [docs/stage2.md](docs/stage2.md), metrics: [docs/stage2-metrics.md](docs/stage2-metrics.md).

Canonical trunk is `main`. Current product version is **0.3.28**. Do not retag or replace older release assets (`v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, `v0.3.12`, `v0.3.13`, `v0.3.14`, `v0.3.15`, `v0.3.16`, `v0.3.17`, `v0.3.18`, `v0.3.19`, `v0.3.20`, `v0.3.21`, `v0.3.22`, `v0.3.23`, `v0.3.24`, `v0.3.25`, `v0.3.26`, and `v0.3.27` stay published).

## What is not in this tree

iOS, federation, web/desktop messenger. TURN/TURNS runs on the same VPS as `rope-server` (coturn; advertised on `GET /v1/info`).

## Working here (CryptoGalera)

Company operating model for humans and cloud agents:

- [cryptogalera/README.md](cryptogalera/README.md) — company runtime wrapping Rope (STABILIZATION; delivery law D-015)
- [cryptogalera/RUNTIME.md](cryptogalera/RUNTIME.md) — spawn / task / report protocol
- [`.cryptogalera/`](.cryptogalera/COMPANY_STATE.md) — live company memory (PO-owned)
- [AGENTS.md](AGENTS.md) — rules agents must follow
- [docs/company.md](docs/company.md) — roles, briefs, reviews, escalation
- [docs/organization.md](docs/organization.md) — org chart, file ownership, how work is staffed
- [docs/product.md](docs/product.md) — product goal, in/out of scope, canonical line
- [docs/decisions.md](docs/decisions.md) — durable decisions
- [docs/tasks.md](docs/tasks.md) — current work queue
- [docs/api.md](docs/api.md) — API pointer (current-trunk protocol docs)

## License

See repository owner for distribution terms.
