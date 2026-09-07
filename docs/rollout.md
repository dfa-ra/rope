# Раскатка Rope у пользователя

Релиз появляется на [Releases](https://github.com/dfa-ra/rope/releases) после тега `vX.Y.Z` (или ручного запуска workflow **Release**). В нём два типа файлов:

| Файл | Кто ставит |
| --- | --- |
| `rope-<version>-debug.apk` (или подписанный `rope-<version>.apk`) | пользователь на Android |
| `rope-server-linux-amd64` / `arm64` | приложение по SSH на VPS |

Телефон **не** кладёт сервер в APK и **не** просит VPS ходить на GitHub.

1. Приложение скачивает `rope-server` **на телефон**.
2. По SSH заливает бинарник и `install.sh` на VPS (SFTP).
3. На сервере installer только ставит systemd — без `curl` наружу.

**Публичный репозиторий:** приложение само берёт `rope-server-linux-amd64` или `arm64` с Releases (выбор архитектуры на экране, либо «Авто» через `uname -m`). URL руками указывать не нужно.

**Приватный репозиторий:** анонимный download с VPS и с телефона даст 404. Нужен Personal Access Token (fine-grained: Contents Read на этот repo, или classic `repo`). Поле «GitHub token» на экране Create server. Токен живёт только в памяти телефона на время скачивания и **не** пишется в `/etc/rope` и не уезжает на VPS.

## Первая установка (организатор)

1. На VPS открыт SSH (Ubuntu/Debian x86_64, пользователь с `sudo` или `root`).
2. В приложении: **Создать сервер** — IP/hostname, SSH-порт, логин, пароль или ключ, архитектура VPS (`Авто` / Linux x86_64 / Linux ARM64). Порт Rope по умолчанию 8443. Для приватного GitHub — PAT в «Дополнительно».
3. Телефон:
   - скачивает `rope-server` с Releases (с токеном, если репо закрытое)
   - по SSH заливает бинарник и `install.sh`
   - запускает installer от root
4. Installer один раз:
   - создаёт системного пользователя `rope`
   - кладёт бинарник в `/opt/rope/bin/rope-server`
   - пишет `/etc/rope/config.json`, SQLite в `/var/lib/rope/data.db`
   - выпускает self-signed TLS и считает fingerprint
   - ставит `systemd` unit `rope.service` и coturn (TURN 3478, TURNS 443 или 5349)
   - пишет HMAC `turn_secret` в `/etc/rope/config.json` (не в репозиторий)
   - открывает порты Rope + TURN (если есть `ufw`)
   - ждёт `GET /health`
   - печатает `SERVER_ID`, `FINGERPRINT`, одноразовый `SETUP_TOKEN`
5. Приложение пинит TLS fingerprint, меняет `SETUP_TOKEN` на owner-устройство и больше не использует SSH.

SSH-пароль/ключ остаются только на телефоне организатора и не становятся кредлами мессенджера.

На Android системный BouncyCastle не умеет X25519 (`NO X25519 for provider BC`). С `v0.1.2` приложение подменяет провайдер и при необходимости переходит на ECDH nistp256. Ставьте APK не ниже этой версии.

## Гость

Организатор жмёт **Invite** → QR / `rope://join?...`.  
Гость ставит тот же APK, сканирует QR и **придумывает логин** (уникальный на этом сервере). При несовпадении fingerprint подключение стоп.

## Обновление приложения

На экране **Сервер → Обновить приложение**. APK ставится поверх через системный установщик, без удаления. Нужен `versionCode` больше текущего и та же подпись (debug-релизы обновляют debug). История и ключи остаются.

Перед установкой приложение кладёт в Загрузки `rope-device.backup` и копию APK. Если система откажет из‑за другой подписи (сборки до закреплённого CI-ключа): удалите старое приложение, поставьте APK из Загрузок и на старте нажмите **Восстановить устройство из Загрузок**.

## Обновление ядра на уже живом VPS

На том же экране **Сервер**: SSH-пароль или ключ и кнопка **Обновить ядро**. Не открывает «Создать сервер».  
Installer заменяет `/opt/rope/bin/rope-server`, ставит/обновляет coturn на том же IP, дописывает `public_host` / `turn_secret` в config если их не было, `systemctl restart rope` (+ coturn), **не** трогает `data.db` и TLS. Повторный запуск на уже установленном Rope — то же самое (не пишет «на сервере уже есть Rope»). После обновления ядра звонки берут ICE с `GET /v1/info`.

Если телефон-owner потерян или приложение поставили заново: **Создать сервер → Дополнительно → Стереть старое и стать владельцем** (`--reinstall`). Стирает `data.db` и `config.json`, ставит новое ядро, выдаёт новый `SETUP_TOKEN`. TLS остаётся.

С машины администратора то же самое:

```bash
curl -fsSL -o rope-server https://github.com/dfa-ra/rope/releases/latest/download/rope-server-linux-amd64
chmod +x rope-server
sudo ./deployment/scripts/install.sh --binary ./rope-server --host YOUR.IP --port 8443 --upgrade
```

## Как выпустить новую версию

```bash
git tag v0.1.2
git push origin v0.1.2
```

Либо Actions → **Release** → Run workflow → версия `0.1.2`.  
После зелёного job на странице Releases появятся новый APK и новые server binaries. Пользователи обновляют приложение вручную (APK) и ядро кнопкой в приложении.
