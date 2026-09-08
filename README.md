# Rope

Private self-hosted 1-to-1 messenger for Android. You provision a personal VPS from the app, invite a second device with a QR / deep link, and exchange E2EE text through a Go relay that never sees plaintext.

MVP stack: **Kotlin UI → Rust security core (UniFFI) → HTTPS/WSS → Go + SQLite**.

## Layout

```
apps/android      Android client
core/rust         identity, envelope, crypto
server/go         relay, mailbox, invites
deployment/       systemd + install.sh
protocol/docs     wire format
docs/             product, architecture, threat model, company, organization, decisions, tasks
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

Re-run with `--upgrade` to replace the binary and keep `data.db`.

## Releases

Push a tag `vX.Y.Z`. GitHub Actions publishes:

- `rope-server-linux-amd64` and `linux-arm64`
- Android APK
- `SHA256SUMS`

Production APK signing uses repository secrets documented in [docs/dev-setup.md](docs/dev-setup.md).

## What is not in MVP

iOS, calls, voice notes, files, groups, federation, web/desktop, polished Telegram/Amnezia UI.

## Working here (CryptoGalera)

Company operating model for humans and cloud agents:

- [AGENTS.md](AGENTS.md) — rules agents must follow
- [docs/company.md](docs/company.md) — roles, briefs, reviews, escalation
- [docs/organization.md](docs/organization.md) — org chart, file ownership, how work is staffed
- [docs/product.md](docs/product.md) — product goal, in/out of scope, canonical line
- [docs/decisions.md](docs/decisions.md) — durable decisions
- [docs/tasks.md](docs/tasks.md) — current work queue

## License

See repository owner for distribution terms.
