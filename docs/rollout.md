# Раскатка Rope у пользователя

Релиз появляется на [Releases](https://github.com/dfa-ra/rope/releases) после тега `vX.Y.Z` (или ручного запуска workflow **Release**). В нём два типа файлов:

| Файл | Кто ставит |
| --- | --- |
| `rope-<version>-debug.apk` (или подписанный `rope-<version>.apk`) | пользователь на Android |
| `rope-server-linux-amd64` / `arm64` | приложение по SSH на VPS |

Телефон **не** кладёт сервер в APK. При «Создать сервер» он скачивает `rope-server` с `.../releases/latest/download/`. Пока релиза нет, установка с телефона упадёт на `curl`.

## Первая установка (организатор)

1. На VPS открыт SSH (Ubuntu/Debian x86_64, пользователь с `sudo` или `root`).
2. В приложении: **Create server** — IP/hostname, SSH-порт, логин, пароль или ключ, порт Rope (по умолчанию 8443).
3. Телефон по SSH:
   - заливает `install.sh`
   - на VPS делает `curl` бинарника с GitHub Releases
   - запускает installer от root
4. Installer один раз:
   - создаёт системного пользователя `rope`
   - кладёт бинарник в `/opt/rope/bin/rope-server`
   - пишет `/etc/rope/config.json`, SQLite в `/var/lib/rope/data.db`
   - выпускает self-signed TLS и считает fingerprint
   - ставит `systemd` unit `rope.service`
   - открывает порт (если есть `ufw`)
   - ждёт `GET /health`
   - печатает `SERVER_ID`, `FINGERPRINT`, одноразовый `SETUP_TOKEN`
5. Приложение пинит TLS fingerprint, меняет `SETUP_TOKEN` на owner-устройство и больше не использует SSH.

SSH-пароль/ключ остаются только на телефоне организатора и не становятся кредлами мессенджера.

## Гость

Организатор жмёт **Invite** → QR / `rope://join?...` (host, port, server id, fingerprint, token).  
Гость ставит тот же APK, сканирует QR. При несовпадении fingerprint подключение стоп.

## Обновление ядра на уже живом VPS

В приложении: **Server → Update server core** (снова SSH).  
Installer запускается с `--upgrade`: заменяет `/opt/rope/bin/rope-server`, `systemctl restart rope`, **не** трогает `data.db` и TLS.

С машины администратора то же самое:

```bash
curl -fsSL -o rope-server https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64
chmod +x rope-server
sudo ./deployment/scripts/install.sh --binary ./rope-server --host YOUR.IP --port 8443 --upgrade
```

## Как выпустить новую версию

```bash
git tag v0.1.1
git push origin v0.1.1
```

Либо Actions → **Release** → Run workflow → версия `0.1.1`.  
После зелёного job на странице Releases появятся новый APK и новые server binaries. Пользователи обновляют приложение вручную (APK) и ядро кнопкой в приложении.
