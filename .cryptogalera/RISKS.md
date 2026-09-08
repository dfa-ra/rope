# Active risks

PO-owned. Only current risks.

| ID | Risk | Severity | Mitigation |
| --- | --- | --- | --- |
| R-001 | Dual product: `main` 0.1.0 vs GitHub `v0.2.x` / PRs #2–#3 | high | D-005 user choice; freeze CG-003; do not merge Stage-2 |
| R-002 | Protocol fork (envelope types, groups, calls) if mixed clients | high | Same as R-001; `docs/api.md` points at current-trunk protocol only |
| R-003 | Agent sprawl (~38 overlapping product agents historically) | medium | RUNTIME.md spawn rules; Workers never spawn; CG-003 freeze |
| R-004 | Session SoT vs runtime drift (`docs/product.md` vs MIGRATION freeze) | medium | Cursor rule + `.cryptogalera/COMPANY_STATE.md` win for mode |
| R-005 | Unverified session docs treated as installed law | low | Labeled unverified; preserved; constitution + RUNTIME are runtime law |
| R-006 | Architecture.md says Room; code is SQLiteOpenHelper | low | CG-014 docs fix; do not rewrite store |
| R-007 | Android unit tests not run in this cloud env (no SDK) | medium | CI android job is the baseline; do not claim local gradle green |
| R-008 | jniLibs not in tree; debug APK needs native build | medium | `scripts/build-android-native.sh`; release.yml |
