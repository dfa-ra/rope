#!/usr/bin/env bash
# Idempotent Rope server installer for Ubuntu LTS / Debian (x86_64 / arm64).
# Installs rope-server plus coturn on the same host (TURN / TURNS).
set -euo pipefail

BINARY=""
HOST=""
PORT="8443"
UPGRADE=0
REINSTALL=0
DATA_DIR="/var/lib/rope"
ETC_DIR="/etc/rope"
BIN_DIR="/opt/rope/bin"
SERVICE_USER="rope"
TURN_PORT="3478"
TURNS_PORT="443"
RELAY_MIN="49152"
RELAY_MAX="49311"

usage() {
  echo "usage: $0 --binary /path/to/rope-server --host <ip-or-dns> [--port 8443] [--upgrade|--reinstall]"
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --binary) BINARY="$2"; shift 2 ;;
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --upgrade) UPGRADE=1; shift ;;
    --reinstall) REINSTALL=1; shift ;;
    *) usage ;;
  esac
done

[[ -n "$BINARY" && -x "$BINARY" && -n "$HOST" ]] || usage

if [[ "$(id -u)" -ne 0 ]]; then
  echo "run as root" >&2
  exit 1
fi

port_in_use() {
  local p="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -lntn 2>/dev/null | awk '{print $4}' | grep -Eq "[.:]${p}$"
  elif command -v netstat >/dev/null 2>&1; then
    netstat -lnt 2>/dev/null | grep -Eq "[.:]${p} "
  else
    return 1
  fi
}

is_ipv4() { [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; }

json_get() {
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2],"") or "")' "$1" "$2"
}

merge_ice_config() {
  local path="$1" host="$2" secret="$3" turn_port="$4" turns_port="$5"
  python3 - "$path" "$host" "$secret" "$turn_port" "$turns_port" <<'PY'
import json, sys
path, host, secret, turn_port, turns_port = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4]), int(sys.argv[5])
with open(path) as f:
    cfg = json.load(f)
if not cfg.get("public_host"):
    cfg["public_host"] = host
if not cfg.get("turn_secret"):
    cfg["turn_secret"] = secret
cfg["turn_port"] = turn_port
if not cfg.get("turns_port"):
    cfg["turns_port"] = turns_port
else:
    # Keep a previously chosen TLS port so an upgrade does not flip 443 ↔ 5349.
    pass
with open(path, "w") as f:
    json.dump(cfg, f, indent=2)
    f.write("\n")
PY
}

if ! id -u "$SERVICE_USER" >/dev/null 2>&1; then
  useradd --system --home "$DATA_DIR" --shell /usr/sbin/nologin "$SERVICE_USER"
fi

mkdir -p "$BIN_DIR" "$DATA_DIR" "$ETC_DIR/tls"
install -o root -g root -m 0755 "$BINARY" "$BIN_DIR/rope-server"

already_installed=0
if [[ -f "$ETC_DIR/config.json" || -f "$DATA_DIR/data.db" ]]; then
  already_installed=1
fi

if [[ "$REINSTALL" -eq 1 && "$already_installed" -eq 1 ]]; then
  systemctl stop rope 2>/dev/null || true
  rm -f "$DATA_DIR/data.db" "$DATA_DIR/data.db-wal" "$DATA_DIR/data.db-shm"
  rm -f "$ETC_DIR/config.json"
  echo "wiped previous Rope data (owner, members, mailbox)"
  already_installed=0
fi

if [[ ! -f "$ETC_DIR/tls/cert.pem" || ! -f "$ETC_DIR/tls/key.pem" ]]; then
  openssl req -x509 -newkey rsa:2048 -sha256 -days 825 -nodes \
    -keyout "$ETC_DIR/tls/key.pem" \
    -out "$ETC_DIR/tls/cert.pem" \
    -subj "/CN=${HOST}" \
    -addext "subjectAltName=DNS:${HOST},IP:${HOST}" 2>/dev/null \
    || openssl req -x509 -newkey rsa:2048 -sha256 -days 825 -nodes \
      -keyout "$ETC_DIR/tls/key.pem" \
      -out "$ETC_DIR/tls/cert.pem" \
      -subj "/CN=${HOST}"
fi

FINGERPRINT="$(openssl x509 -in "$ETC_DIR/tls/cert.pem" -outform DER | sha256sum | awk '{print $1}')"

# Prefer TURNS on 443 (rope-server is typically 8443). If 443 is taken, use 5349.
if [[ -f "$ETC_DIR/config.json" ]]; then
  existing_turns="$(json_get "$ETC_DIR/config.json" "turns_port")"
  if [[ -n "$existing_turns" ]]; then
    TURNS_PORT="$existing_turns"
  elif port_in_use 443; then
    TURNS_PORT="5349"
  fi
elif port_in_use 443; then
  TURNS_PORT="5349"
fi

if [[ ! -f "$ETC_DIR/config.json" ]]; then
  SERVER_ID="$(openssl rand -hex 16)"
  SETUP_TOKEN="$(openssl rand -hex 24)"
  TURN_SECRET="$(openssl rand -hex 32)"
  cat > "$ETC_DIR/config.json" <<EOF
{
  "listen": "0.0.0.0:${PORT}",
  "data_dir": "${DATA_DIR}",
  "tls_cert": "${ETC_DIR}/tls/cert.pem",
  "tls_key": "${ETC_DIR}/tls/key.pem",
  "setup_token": "${SETUP_TOKEN}",
  "server_id": "${SERVER_ID}",
  "mailbox_ttl_seconds": 604800,
  "max_envelope_bytes": 65536,
  "allow_http": false,
  "fingerprint": "${FINGERPRINT}",
  "public_host": "${HOST}"
}
EOF
else
  SETUP_TOKEN="$(json_get "$ETC_DIR/config.json" "setup_token")"
  SERVER_ID="$(json_get "$ETC_DIR/config.json" "server_id")"
  TURN_SECRET="$(json_get "$ETC_DIR/config.json" "turn_secret")"
  if [[ -z "$TURN_SECRET" ]]; then
    TURN_SECRET="$(openssl rand -hex 32)"
  fi
  existing_turns="$(json_get "$ETC_DIR/config.json" "turns_port")"
  if [[ -n "$existing_turns" ]]; then
    TURNS_PORT="$existing_turns"
  fi
fi

chown -R "$SERVICE_USER:$SERVICE_USER" "$DATA_DIR"
chown -R root:"$SERVICE_USER" "$ETC_DIR"
chmod 640 "$ETC_DIR/config.json" "$ETC_DIR/tls/key.pem"
chmod 644 "$ETC_DIR/tls/cert.pem"

UNIT_SRC="$(cd "$(dirname "$0")/.." && pwd)/systemd/rope.service"
if [[ -f "$UNIT_SRC" ]]; then
  install -m 0644 "$UNIT_SRC" /etc/systemd/system/rope.service
else
  cat > /etc/systemd/system/rope.service <<'EOF'
[Unit]
Description=Rope relay server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=rope
Group=rope
ExecStart=/opt/rope/bin/rope-server --config /etc/rope/config.json
Restart=on-failure
RestartSec=3
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/rope

[Install]
WantedBy=multi-user.target
EOF
fi

install_coturn() {
  if ! command -v turnserver >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    if command -v apt-get >/dev/null 2>&1; then
      apt-get update -qq
      apt-get install -y -qq coturn
    else
      echo "ROPE_TURN_SKIPPED=no-apt" >&2
      return 1
    fi
  fi
  if ! command -v turnserver >/dev/null 2>&1; then
    echo "ROPE_TURN_SKIPPED=no-turnserver" >&2
    return 1
  fi

  if ! id -u turnserver >/dev/null 2>&1; then
    useradd --system --home /var/lib/turnserver --shell /usr/sbin/nologin turnserver 2>/dev/null || true
  fi
  if id -u turnserver >/dev/null 2>&1; then
    usermod -aG "$SERVICE_USER" turnserver 2>/dev/null || true
  fi

  mkdir -p "$ETC_DIR/turn-tls" /var/log/turnserver /var/lib/turnserver
  local cert_user=root
  local cert_group="$SERVICE_USER"
  if id -u turnserver >/dev/null 2>&1; then
    cert_user=turnserver
    cert_group=turnserver
  fi
  install -o "$cert_user" -g "$cert_group" -m 644 "$ETC_DIR/tls/cert.pem" "$ETC_DIR/turn-tls/cert.pem"
  install -o "$cert_user" -g "$cert_group" -m 640 "$ETC_DIR/tls/key.pem" "$ETC_DIR/turn-tls/key.pem"
  if id -u turnserver >/dev/null 2>&1; then
    chown turnserver:turnserver /var/log/turnserver /var/lib/turnserver 2>/dev/null || true
  fi

  local ext_line=""
  if is_ipv4 "$HOST"; then
    ext_line="external-ip=${HOST}"
  else
    local resolved
    resolved="$(getent ahostsv4 "$HOST" 2>/dev/null | awk '{print $1; exit}' || true)"
    if [[ -n "$resolved" ]]; then
      ext_line="external-ip=${resolved}"
    fi
  fi

  cat > /etc/turnserver.conf <<EOF
# Generated by Rope installer. HMAC secret lives in /etc/rope/config.json
# and is not written here.
listening-port=${TURN_PORT}
tls-listening-port=${TURNS_PORT}
listening-ip=0.0.0.0
${ext_line}
min-port=${RELAY_MIN}
max-port=${RELAY_MAX}
realm=rope
server-name=rope
fingerprint
lt-cred-mech
use-auth-secret
static-auth-secret=${TURN_SECRET}
cert=${ETC_DIR}/turn-tls/cert.pem
pkey=${ETC_DIR}/turn-tls/key.pem
no-tlsv1
no-tlsv1_1
no-cli
no-multicast-peers
no-stdout-log
simple-log
log-file=/var/log/turnserver/turn.log
pidfile=/run/turnserver/turnserver.pid
denied-peer-ip=0.0.0.0-0.255.255.255
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=100.64.0.0-100.127.255.255
denied-peer-ip=127.0.0.0-127.255.255.255
denied-peer-ip=169.254.0.0-169.254.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=192.0.0.0-192.0.0.255
denied-peer-ip=192.0.2.0-192.0.2.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=198.18.0.0-198.19.255.255
denied-peer-ip=198.51.100.0-198.51.100.255
denied-peer-ip=203.0.113.0-203.0.113.255
denied-peer-ip=::1
stale-nonce=600
EOF
  chmod 640 /etc/turnserver.conf
  cp /etc/turnserver.conf "$ETC_DIR/turnserver.conf"
  chmod 640 "$ETC_DIR/turnserver.conf"

  if [[ -f /etc/default/coturn ]]; then
    if grep -q '^TURNSERVER_ENABLED=' /etc/default/coturn; then
      sed -i 's/^TURNSERVER_ENABLED=.*/TURNSERVER_ENABLED=1/' /etc/default/coturn
    else
      echo "TURNSERVER_ENABLED=1" >> /etc/default/coturn
    fi
  fi

  mkdir -p /etc/systemd/system/coturn.service.d
  cat > /etc/systemd/system/coturn.service.d/rope.conf <<'EOF'
[Unit]
PartOf=rope.service

[Service]
# Package unit already points at /etc/turnserver.conf.
Restart=on-failure
RestartSec=3
EOF

  systemctl daemon-reload
  systemctl enable coturn >/dev/null 2>&1 || systemctl enable turnserver >/dev/null 2>&1 || true
  systemctl restart coturn 2>/dev/null || systemctl restart turnserver 2>/dev/null || true
  if systemctl is-active --quiet coturn || systemctl is-active --quiet turnserver; then
    echo "ROPE_TURN_OK"
    echo "TURNS_PORT=${TURNS_PORT}"
    echo "TURN_PORT=${TURN_PORT}"
    return 0
  fi
  # 443 may be taken by nginx/caddy; never move rope-server off 8443 — only TURNS.
  if [[ "$TURNS_PORT" == "443" ]]; then
    echo "coturn failed on 443; retrying TURNS on 5349" >&2
    TURNS_PORT="5349"
    if grep -q '^tls-listening-port=' /etc/turnserver.conf; then
      sed -i "s/^tls-listening-port=.*/tls-listening-port=${TURNS_PORT}/" /etc/turnserver.conf
    fi
    cp /etc/turnserver.conf "$ETC_DIR/turnserver.conf"
    chmod 640 /etc/turnserver.conf "$ETC_DIR/turnserver.conf"
    systemctl restart coturn 2>/dev/null || systemctl restart turnserver 2>/dev/null || true
    if systemctl is-active --quiet coturn || systemctl is-active --quiet turnserver; then
      echo "ROPE_TURN_OK"
      echo "TURNS_PORT=${TURNS_PORT}"
      echo "TURN_PORT=${TURN_PORT}"
      return 0
    fi
  fi
  echo "ROPE_TURN_SKIPPED=inactive" >&2
  systemctl status coturn --no-pager >&2 || systemctl status turnserver --no-pager >&2 || true
  return 1
}

TURN_OK=0
if install_coturn; then
  TURN_OK=1
  merge_ice_config "$ETC_DIR/config.json" "$HOST" "$TURN_SECRET" "$TURN_PORT" "$TURNS_PORT"
  TURNS_PORT="$(json_get "$ETC_DIR/config.json" "turns_port")"
  [[ -n "$TURNS_PORT" ]] || TURNS_PORT="443"
  chmod 640 "$ETC_DIR/config.json"
else
  echo "coturn not running; GET /v1/info omits ice_servers (app keeps public STUN fallback)" >&2
fi

if command -v ufw >/dev/null 2>&1; then
  ufw allow "${PORT}/tcp" >/dev/null 2>&1 || true
  ufw allow "${TURN_PORT}/tcp" >/dev/null 2>&1 || true
  ufw allow "${TURN_PORT}/udp" >/dev/null 2>&1 || true
  ufw allow "${TURNS_PORT}/tcp" >/dev/null 2>&1 || true
  ufw allow "${RELAY_MIN}:${RELAY_MAX}/udp" >/dev/null 2>&1 || true
fi

systemctl daemon-reload
systemctl enable --now rope

for i in 1 2 3 4 5 6 7 8 9 10; do
  if curl -ksS "https://127.0.0.1:${PORT}/health" | grep -q '"ok"'; then
    break
  fi
  sleep 1
done

if ! curl -ksS "https://127.0.0.1:${PORT}/health" | grep -q '"ok"'; then
  echo "health check failed" >&2
  systemctl status rope --no-pager || true
  exit 1
fi

if [[ "$already_installed" -eq 1 || "$UPGRADE" -eq 1 ]]; then
  echo "upgraded binary; data and config preserved"
  echo "ROPE_UPGRADE_OK"
  if [[ "$TURN_OK" -eq 1 ]]; then
    echo "ROPE_TURN_OK"
    echo "TURN_PORT=${TURN_PORT}"
    echo "TURNS_PORT=${TURNS_PORT}"
  fi
  exit 0
fi

echo "ROPE_INSTALL_OK"
echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "SERVER_ID=${SERVER_ID}"
echo "FINGERPRINT=${FINGERPRINT}"
echo "SETUP_TOKEN=${SETUP_TOKEN}"
if [[ "$TURN_OK" -eq 1 ]]; then
  echo "ROPE_TURN_OK"
  echo "TURN_PORT=${TURN_PORT}"
  echo "TURNS_PORT=${TURNS_PORT}"
fi
