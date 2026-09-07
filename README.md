# Rope

Private self-hosted messenger for Android. You provision a personal VPS from the app, invite people with a QR / deep link, and exchange E2EE text, voice, photos, files, and group chats through a Go relay that never sees plaintext.

Stack: **Kotlin UI → Rust security core (UniFFI) → HTTPS/WSS → Go + SQLite**.

## Layout

```
apps/android      Android client
core/rust         identity, envelope, crypto
server/go         relay, mailbox, invites
deployment/       systemd + install.sh
protocol/docs     wire format
docs/             architecture and threat model
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
`--reinstall` wipes `data.db` + config and issues a new owner `SETUP_TOKEN` (lost-phone recovery).

Раскатка у пользователя (телефон → SSH → VPS): [docs/rollout.md](docs/rollout.md).

## Releases

Push a tag `vX.Y.Z`. GitHub Actions publishes:

- `rope-server-linux-amd64` and `linux-arm64` (в приложении выбирается архитектура, без правки URL)
- Android APK
- `SHA256SUMS`

Production APK signing uses repository secrets documented in [docs/dev-setup.md](docs/dev-setup.md).

Stage 2 (v0.2): photos/files, hold-to-record voice, groups, call signaling, encrypted object store. Details: [docs/stage2.md](docs/stage2.md), metrics: [docs/stage2-metrics.md](docs/stage2-metrics.md).

## What is not in this tree

iOS, WebRTC media/TURN in production, federation, web/desktop.

## License

See repository owner for distribution terms.
