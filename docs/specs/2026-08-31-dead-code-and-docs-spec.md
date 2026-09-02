# T8 — Dead-code sweep + SHIPPED-DELTA

**Status:** specified, 2026-08-31
**Baseline:** after T6
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Do

Re-grep immediately before every deletion.

Candidates: `DeskSectionPlaceholder` if unused; `ComingScreen` if unused;
`ZeroDayDraft` leftovers; stale R2 / “13th chip” comments in BoardPane KDoc,
BoardPaneTest header, `AGENTS.md`, `CLAUDE.md`, v4 spec R2 — rewrite to:
transport shipped 1.27.0; chip is the fourth-line row as of 1.28.0.

Canonical `version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`:

- Add **Letters** to the 08-30 retired table.
- Record **CentreEditScreen vs CentreOpsScreen**.
- Replace the 1.22.0 Day-11 “confirm before drawing it back” line: Board
  fourth-row chip shipped; dashed GAP badge never drawn.
- List **applicant desk history** as shipped; server Advanced Search stays
  parked.
- Mark `version-3/SHIPPED-DELTA.md` superseded.
- Bump the header off “live 1.22.0” to the closing version.
- Record the three parked items (server search, photo upload, centre metrics).

## Never-touched

`docs/LIVE-DESK-HAR.md`. Mock `/staff/*` behind `-Pdipi.useMock`.
