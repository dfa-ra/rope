#!/usr/bin/env bash
# Idempotent Rope server installer for Ubuntu LTS / Debian (x86_64).
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

# Re-run is Amnezia-style upgrade: new binary, keep owner and chats.
# Only --reinstall wipes membership so a new phone can become owner.
if [[ "$already_installed" -eq 1 ]]; then
  systemctl daemon-reload
  systemctl enable rope 2>/dev/null || true
  systemctl restart rope
  echo "upgraded binary; data and config preserved"
  echo "ROPE_UPGRADE_OK"
  exit 0
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

if [[ ! -f "$ETC_DIR/config.json" ]]; then
  SERVER_ID="$(openssl rand -hex 16)"
  SETUP_TOKEN="$(openssl rand -hex 24)"
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
  "fingerprint": "${FINGERPRINT}"
}
EOF
else
  SETUP_TOKEN="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["setup_token"])' "$ETC_DIR/config.json")"
  SERVER_ID="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["server_id"])' "$ETC_DIR/config.json")"
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

if command -v ufw >/dev/null 2>&1; then
  ufw allow "${PORT}/tcp" >/dev/null 2>&1 || true
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

echo "ROPE_INSTALL_OK"
echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "SERVER_ID=${SERVER_ID}"
echo "FINGERPRINT=${FINGERPRINT}"
echo "SETUP_TOKEN=${SETUP_TOKEN}"
