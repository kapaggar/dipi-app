# DIPI Staff Android

Native **centre-staff** client of the DIPI registrar desk (`dh_manageapp`).

Not a student-apply app. Not a WebView of `dipi.vridhamma.org`. Not AT portal, SMS, WhatsApp, IVR, Mitra, or Patrika.

**Status (2026-08-13):** documentation only. No Android modules yet. Vertical 1 is waiting on P0 validation (Fable) and human approval.

## Read first

1. [docs/plans/2026-08-13-p0-fable-validation.md](docs/plans/2026-08-13-p0-fable-validation.md) — validate this before scaffolding
2. [docs/00-architecture.md](docs/00-architecture.md)
3. [docs/openapi-staff.yaml](docs/openapi-staff.yaml)
4. [AGENTS.md](AGENTS.md) / [CLAUDE.md](CLAUDE.md)

## Vertical 1 (Today’s applications)

Login as a Drupal centre user → pick centre + unfinalized course → worklist + search → public applicant card → change status (existing PHP path) → toggle `a_attended`.

Day-0 lists, seating, CameraX, letter admin, and the 50-field editor are **out**.

## Sibling server

`/Users/wizops/DIPI/dipi-web` — Drupal 7. Prefer local clone over guessing GitHub.
