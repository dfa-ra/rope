#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/core/rust"
rustup target add aarch64-linux-android x86_64-linux-android >/dev/null
cargo ndk -t arm64-v8a -t x86_64 -o "$ROOT/apps/android/app/src/main/jniLibs" build --release
echo "native libs copied to apps/android/app/src/main/jniLibs"
