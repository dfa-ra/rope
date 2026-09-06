# Developer setup

## Prerequisites

- Rust 1.83+ (`rustup`)
- Go 1.22+
- JDK 17+ (JDK 21 works)
- Android SDK + NDK 26+ (for the app and JNI libraries)
- `cargo-ndk` for Android native builds: `cargo install cargo-ndk`

## Rust core

```bash
cd core/rust
cargo test
cargo build --release
```

Generate Kotlin bindings after a host build of the cdylib:

```bash
./scripts/generate-kotlin-bindings.sh
```

Cross-compile Android `.so` files (arm64 + x86_64 for emulator):

```bash
./scripts/build-android-native.sh
```

## Go server

```bash
cd server/go
go test ./...
go build -o rope-server ./cmd/rope-server
```

Local HTTP (debug only, never used by the installer):

```bash
./rope-server --config /tmp/rope-dev.json --allow-http --listen 127.0.0.1:8443
```

A sample config is created automatically on first start if `--config` points at a missing file and `--init` is passed:

```bash
./rope-server --config /tmp/rope-dev.json --init --allow-http --listen 127.0.0.1:8443
```

## VPS installer (from a Linux machine)

```bash
sudo ./deployment/scripts/install.sh \
  --binary ./rope-server \
  --host 203.0.113.10 \
  --port 8443
```

The script is idempotent. It prints the TLS fingerprint and one-time `setup_token`. `--upgrade` replaces the binary and keeps `data.db`. `--reinstall` wipes the database and config so a new device can become owner. A bare re-run on an existing install exits with `ROPE_ALREADY_INSTALLED`.

## Android

Open `apps/android` in Android Studio or:

```bash
cd apps/android
./gradlew test
./gradlew :app:assembleDebug
```

Create `apps/android/local.properties` with `sdk.dir=...` if you are not using Android Studio.

Debug builds allow a "plain HTTP" toggle so an emulator can reach `10.0.2.2:8443`. Release builds require TLS + fingerprint pin.

## GitHub release signing

`release.yml` publishes:

- `rope-server-linux-amd64`
- `rope-server-linux-arm64`
- `SHA256SUMS`
- `rope-<version>.apk`

If these repository secrets exist, the APK is signed:

| Secret | Meaning |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | base64 of the `.jks` / `.keystore` file |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Without those secrets the workflow uploads a debug APK and writes a warning on the release notes. Play Store / AAB is out of scope for MVP.

Private GitHub repos: release assets are private too. The phone downloads them with an optional PAT (Contents: Read) and SFTP-uploads the binary. The VPS never stores the token. See [rollout.md](rollout.md).
