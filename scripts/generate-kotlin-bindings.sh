#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/core/rust"
cargo build --features cli
OUT="$ROOT/apps/android/app/src/main/java"
mkdir -p "$OUT"
cargo run --features cli --bin uniffi-bindgen -- generate \
  --library "$ROOT/core/rust/target/debug/librope_core.so" \
  --language kotlin \
  --out-dir "$OUT"
echo "Kotlin bindings written under $OUT"
