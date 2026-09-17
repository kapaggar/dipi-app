# Docs index

One file owns each topic. Dated plans, specs and handovers that shipped are not kept
here: their outcome is a `CHANGELOG.md` entry, their rulings are in `DECISIONS.md`,
and their text stays in git history (last present at `c829e17`, 2026-09-17).

| Path | Owns |
|---|---|
| [../AGENTS.md](../AGENTS.md) | Product rules (Hard rules), current transport assumptions, tablet install, build and test commands |
| [../CHANGELOG.md](../CHANGELOG.md) | Release history from 2.0.0, one entry per `versionName` / `versionCode` |
| [../CLAUDE.md](../CLAUDE.md) | Agent orientation: current version, binding amendments, test invocation |
| [DECISIONS.md](DECISIONS.md) | Owner decision ledger, 2026-08-13 onward, including retirements and parked items |
| [DESIGN.md](DESIGN.md) | Design authority: measurements (1px = 1dp) and the shipped-delta ledger per release. Binding artifact: [design/DIPI-Staff.dc.html](design/DIPI-Staff.dc.html) (open in a browser) |
| [design/desk-2.2/HANDOVER.md](design/desk-2.2/HANDOVER.md) | The 2.2 desk pass delta to the visual authority: tokens, panes, must-not-change list |
| [LIVE-DESK.md](LIVE-DESK.md) | The live Drupal desk: captured transport (HAR), PHP page inventory, server memory map, sheet routes and markup skeletons |
| [openapi-staff.yaml](openapi-staff.yaml) | The mock-only `/staff/*` contract behind `-Pdipi.useMock=true`; fixtures for tests, never built server-side |
| [PHOTO-BUILDS.md](PHOTO-BUILDS.md) | The `-Pdipi.photoReview` build flag: compact vs photo-enabled APKs |
| [WHATSAPP.md](WHATSAPP.md) | Centre-specific WhatsApp automation: approved boundaries, provisioning, pilot and QA record |
| [BACKLOG.md](BACKLOG.md) | Open findings and owner decisions still pending, distilled from retired handovers |
| [FONTS.md](FONTS.md) | Bundled font licences (OFL) |
| [java-desktop/2026-09-05-java-desktop-handover.md](java-desktop/2026-09-05-java-desktop-handover.md) | Java desktop twin (sibling repo): **paste this** into the implement session |
| [java-desktop/2026-09-05-java-desktop-design.md](java-desktop/2026-09-05-java-desktop-design.md) | Java desktop twin: design spec (JavaFX 21, P0 to P4) |
| [java-desktop/2026-09-05-java-desktop-plan.md](java-desktop/2026-09-05-java-desktop-plan.md) | Java desktop twin: phase-wise plan |
| [java-desktop/2026-09-05-java-desktop-source-inventory.md](java-desktop/2026-09-05-java-desktop-source-inventory.md) | Verified 1.42.0 Android inventory the Java sessions read first |

Server clone (not in this repo): `/Users/wizops/DIPI/dipi-web`
