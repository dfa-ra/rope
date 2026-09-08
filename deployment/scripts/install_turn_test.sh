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
echo "$conf443" | grep -qE '^external-ip=203.0.113.10(/[0-9.]+)?$' || fail "external-ip public IPv4"
echo "$conf443" | grep -qE '^relay-ip=[0-9.]+$' || fail "relay-ip IPv4"
echo "$conf443" | grep -q '^use-auth-secret$' || fail "use-auth-secret"
echo "$conf443" | grep -q '^static-auth-secret=unit-test-secret$' || fail "secret mismatch"
echo "$conf443" | grep -q '^lt-cred-mech$' || fail "lt-cred-mech"
echo "$conf443" | grep -q '^fingerprint$' || fail "fingerprint"
echo "$conf443" | grep -q '^no-cli$' || fail "no-cli"
echo "$conf443" | grep -q '^no-ipv6$' || fail "ipv4-only default"
echo "$conf443" | grep -q '^keep-address-family$' || fail "keep-address-family"
echo "$conf443" | grep -qv '^listening-ip=127.0.0.1$' || fail "must not bind loopback only"

confNat="$(ROPE_WAN_IP=198.51.100.20 ROPE_RELAY_IP=10.0.0.4 "$SCRIPT" --print-turn-conf --host 10.0.0.4 --secret unit-test-secret --turns-port 443)"
echo "$confNat" | grep -q '^external-ip=198.51.100.20/10.0.0.4$' || fail "NAT VPS external-ip=public/local"
echo "$confNat" | grep -q '^relay-ip=10.0.0.4$' || fail "relay-ip must be local, not WAN"
echo "$confNat" | grep -qv '^relay-ip=198.51.100.20$' || fail "must not bind relay to 1:1 NAT address"

confDns="$(ROPE_WAN_IP=198.51.100.20 ROPE_RELAY_IP=10.0.0.4 "$SCRIPT" --print-turn-conf --host vps.example --secret unit-test-secret --turns-port 443)"
echo "$confDns" | grep -qE '^external-ip=198.51.100.20(/10.0.0.4)?$' || fail "WAN when DNS has no A"
echo "$confDns" | grep -q '^relay-ip=10.0.0.4$' || fail "relay-ip local for DNS host"

confNoRelay="$(ROPE_WAN_IP=198.51.100.20 "$SCRIPT" --print-turn-conf --host 198.51.100.20 --secret unit-test-secret --turns-port 443)"
echo "$confNoRelay" | grep -qE '^external-ip=198.51.100.20(/[0-9.]+)?$' || fail "public external-ip without override"

conf5349="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --turns-port 5349)"
echo "$conf5349" | grep -q '^tls-listening-port=5349$' || fail "turns 5349"
echo "$conf5349" | grep -qv 'tls-listening-port=443' || fail "443 must not appear when 5349 selected"

conf0="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --turns-port 0)"
echo "$conf0" | grep -qv '^tls-listening-port=' || fail "no tls port when TURNS disabled"
echo "$conf0" | grep -q '^listening-port=3478$' || fail "plain TURN remains"

conf6="$("$SCRIPT" --print-turn-conf --host 203.0.113.10 --secret unit-test-secret --allow-ipv6)"
echo "$conf6" | grep -qv '^no-ipv6$' || fail "--allow-ipv6 must omit no-ipv6"

dropin="$ROOT/deployment/systemd/coturn.service.d/rope.conf"
[[ -f "$dropin" ]] || fail "missing coturn drop-in template"
grep -q 'SyslogIdentifier=coturn' "$dropin" || fail "journal identifier"
if grep -q '^PartOf=' "$dropin"; then fail "PartOf would kill coturn with rope"; fi

echo "install_turn_test.sh OK"
