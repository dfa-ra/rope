# Stage 2 effectiveness metrics

These are product metrics, not vanity counts. Each one maps to a failure users already hit in 1-to-1 text MVP: too many taps, silent crypto mismatches, opaque admin JSON, offline loss.

## Target values

| ID | Metric | Target | How we hit it |
| --- | --- | --- | --- |
| M1 | Taps to send a voice note | **1** (hold mic) | Composer shows mic when draft is empty; release sends, slide-left cancels |
| M2 | Taps to send a photo | **≤ 2** | Attach → picker. No extra “send file” confirm |
| M3 | Taps to answer a call | **1** | Full-screen incoming overlay, not a snackbar |
| M4 | Taps to create a group | **≤ 2** | FAB → name (+ optional checks) → Создать |
| M5 | v1 text compatibility | **100%** | Envelope version stays 1; type 1 still uses `encrypt_message` |
| M6 | Object hash acceptance | **100%** | Server rejects `X-Rope-SHA256` mismatch; client decrypts only on matching SHA-256 |
| M7 | Removed member isolation | **100%** | `group_send` and `/v1/groups` refuse a device after `removed_at` |
| M8 | Offline mailbox for typed envelopes | **100%** | Same mailbox path as text; outbox flushes media/group on reconnect |
| M9 | Unknown envelope type | **fail loud** | Chat shows “Обновите Rope”, never a blank bubble |
| M10 | Admin health fields | **≥ 6 cards** | version, members, devices/online, mailbox, objects, groups, limit, listen |
| M11 | Voice duration cap | **10 min** | Recorder `setMaxDuration`; UI timer auto-sends at cap |
| M12 | Max object | **25 MB** | Rust + Go + Android all reject larger plaintext/ciphertext |

## Measurement in CI

- M5, M6, M7, M8, M10: Go tests in `server/go/internal/httpapi`
- M5, M6, unknown type: Rust tests in `core/rust`
- M1–M4, M9, M10: Android unit tests in `Stage2UxTest`

## UX rules that protect the numbers

1. Composer never shows both Send and Mic. Text → send. Empty → hold mic.
2. Incoming call covers the whole window. One green tap answers.
3. Chat list mixes DMs and groups with last-message preview so users do not hunt.
4. Delivery labels stay `ожидает / на сервере / доставлено` — three states, same as protocol.
5. Server screen is cards, not raw JSON. Raw dump remains only as fallback.
