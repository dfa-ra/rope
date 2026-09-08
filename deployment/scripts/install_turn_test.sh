#!/usr/bin/env bash
# Dry-run checks for install.sh TURN config rendering. No root, no apt.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT="$ROOT/deployment/scripts/install.sh"

fail() { echo "FAIL: $*" >&2; exit 1; }

conf443="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --turns-port 443)"
echo "$conf443" | grep -q '^listening-ip=0.0.0.0$' || fail "must listen on 0.0.0.0"
echo "$conf443" | grep -q '^listening-port=3478$' || fail "udp/tcp 3478"
echo "$conf443" | grep -q '^tls-listening-port=443$' || fail "turns 443"
echo "$conf443" | grep -q '^external-ip=203.0.113.10$' || fail "external-ip IPv4"
echo "$conf443" | grep -q '^use-auth-secret$' || fail "use-auth-secret"
echo "$conf443" | grep -q '^static-auth-secret=unit-test-secret$' || fail "secret mismatch"
echo "$conf443" | grep -q '^lt-cred-mech$' || fail "lt-cred-mech"
echo "$conf443" | grep -qv '^listening-ip=127.0.0.1$' || fail "must not bind loopback only"

conf5349="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --turns-port 5349)"
echo "$conf5349" | grep -q '^tls-listening-port=5349$' || fail "turns 5349"
echo "$conf5349" | grep -qv 'tls-listening-port=443' || fail "443 must not appear when 5349 selected"

conf0="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --turns-port 0)"
echo "$conf0" | grep -qv '^tls-listening-port=' || fail "no tls port when TURNS disabled"
echo "$conf0" | grep -q '^listening-port=3478$' || fail "plain TURN remains"

echo "install_turn_test.sh OK"
