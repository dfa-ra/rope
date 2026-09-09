# Active risks

PO-owned. Only current risks.

| ID | Risk | Severity | Mitigation |
| --- | --- | --- | --- |
| R-001 | Dual product: `main` 0.1.0 vs GitHub `v0.2.x` | closed | D-005 A landed: `main` matches tag `v0.2.15`; published assets kept |
| R-002 | Protocol fork if mixed MVP/Stage-2 clients | high | After CG-004, `main` protocol **is** the shipped Stage-2 protocol; mixed old MVP clients unsupported |
| R-003 | Agent sprawl on overlapping UI/calls/landing | medium | CG-003 freeze remains for **new** overlapping agents |
| R-004 | Session SoT vs runtime drift | medium | After promotion, rewrite product.md / AGENTS.md to match trunk |
| R-005 | Unverified session docs treated as installed law | low | Labeled unverified; preserved; constitution + RUNTIME are runtime law |
| R-006 | Architecture.md says Room; code is SQLiteOpenHelper | low | CG-014 docs fix; do not rewrite store |
| R-007 | Android unit tests not run in this cloud env (no SDK) | medium | CI android job is the baseline; do not claim local gradle green |
| R-008 | jniLibs not in tree; debug APK needs native build | medium | `scripts/build-android-native.sh`; existing `v0.2.15` APK is the shipped artifact — do not rebuild/replace it |
| R-009 | Unique unshipped chrome on `telegram-chrome-872f` | low | Keep that branch; do not merge into the release promotion |
| R-010 | Accidental retag / rewrite of GitHub Releases | high | Do not move tags `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10` / `v0.3.11` / `v0.3.12` / `v0.3.13` / `v0.3.14` / `v0.3.15` / `v0.3.16`; do not delete release assets |
| R-011 | Persistent WSS FGS battery / Android 14 type mismatch | medium | `dataSync`+`remoteMessaging`; low-importance connection channel; sideload still crashes if type missing |
| R-012 | In-memory pending RING vs mailbox CALL dual path | medium | Drop pending on hangup/TTL; Android must ignore stale rings after caller ended |
| R-013 | Process killed still cannot notify until next connect | medium | Honest: no FCM; FGS covers background; mailbox+pending flush on reconnect |
