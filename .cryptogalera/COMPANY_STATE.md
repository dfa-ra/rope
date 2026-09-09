# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.16** (`2a659cb`). Next slice **0.3.17** is Telegram-like **chat-list last-message preview** (draft «Черновик:»; DM «Вы:»; mute after title, time trailing). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, and **v0.3.16** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015):** PR → review → merge to `main` → tagged GitHub Release. PO is Product Owner of Rope. CG-105 landed. Next: **0.3.17** chat-list last-message preview (D-030). User sprint is core + UI; continue until the user says stop. REV-01 PASS_WITH_CONCERNS on in-thread empty-search (lowercase quoted q «анн»; same 0.3.14 nit, not wrong-state) is accepted; miss copy is «Ничего не найдено», not idle, not «Ничего не нашли».

## Staffing this cycle

```
User
 └── PO (this run; integrator)
      └── AND-01 — chat-list last-message preview on cursor/chat-list-preview-ae19
```

## Canonical git

- Default: **`main`** = tag `v0.3.16` (`2a659cb44443d846987deb5817b1f979e1af3f33`)
- Working: `cursor/chat-list-preview-ae19`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.16
- Next ship: **v0.3.17** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16)
