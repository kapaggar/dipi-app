# AGENTS.md

Guidance for autonomous coding agents (Claude Code, Cursor, Codex, **Fable**, Grok). Humans may use it too.

## What this repository is

A **centre-staff Android client** for DIPI’s registrar desk. Application id (planned): `org.dhamma.dipi.staff`.

The server is Drupal 7 `dh_manageapp` in the sibling repo `/Users/wizops/DIPI/dipi-web`. This repo does **not** contain that PHP. Do not invent field names — read the desk source listed below.

## Read before you change product or API

| Priority | Path | Why |
|----------|------|-----|
| 1 | `docs/plans/2026-08-13-p0-fable-validation.md` | P0 lock + Fable checklist |
| 2 | `docs/00-architecture.md` | Hybrid API and Vertical 1 screens |
| 3 | `docs/openapi-staff.yaml` | HTTP contract |
| 4 | `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md` | Full scope and bridge rules |
| 5 | `docs/DIPI_MEMORY_MAP.md` | Desk facts from dipi-web |

Desk PHP to open (only these until a Vertical 1 action requires more):

- `dipi-web/sites/all/modules/dh_manageapp/dh_manageapp.module` — `_change_status`, `_update_status`, `generate_conf_no`, `_manageapp_check_access`
- `dipi-web/sites/all/modules/dh_manageapp/inc/search.inc` — worklist (HTML today)
- `dipi-web/sites/all/modules/dh_manageapp/inc/application.inc` — `a_*` prefixes only
- `dipi-web/sites/all/modules/dh_manageapp/inc/course.inc` — `c_finalized`

Do **not** start in `dipi_api.module`, `dh_atportal/`, `letters.inc` internals, or `zero-day.inc`.

## Hard rules

1. This is not student-apply, not a WebView, not a DataTables clone.
2. Do not use Drupal role **APP API** or `/api/dipi/get-app-detail`.
3. Auth = real centre-staff Drupal user + `dh_user_center`. Gender perms (`access male` / `access female`) are first-class.
4. All status writes go through existing PHP (`_change_status`). Never `UPDATE a_status` from the app.
5. `/search-app` is HTML — new JSON only. Do not scrape.
6. `/app-update-attended` is a Day-0 **room** write. Vertical 1 attended is a thin `a_attended` façade only.
7. Do not fetch or store `ae_*`, Aadhaar, PAN, passport, voter id.
8. Do not implement Day-0, seating, CameraX, AT portal, LC review UI, letters admin, finalize, until Vertical 1 is demoable.
9. Bridges (`dh_send_letter`, `update_status_external`, WhatsApp, SMS) stay black boxes. Trigger via `_change_status` only.
10. Do not commit `local.properties`, keystores, session cookies, or real student fixtures.
11. Evidence before “done”: unit tests + what you ran. Do not claim a live DIPI host works unless you hit one.

## Planned layout (not scaffolded yet)

```
:app
:core:model
:core:network
:core:database
:core:datastore
:core:ui
:feature:auth
:feature:course
:feature:applicants
```

No `:feature:day0` until Vertical 2.

## Skills (implementation session)

Install before Kotlin: Google `jetpack-compose`, `navigation-3`, `edge-to-edge`, `android-intent-security`, `testing-setup`. Chris Banes `kotlin-api-design`, `kotlin-concurrency-and-flow`, `compose-state-and-effects`. CameraX only in Vertical 2.

This machine already has `android-data-layer`, `android-jetpack-compose`, `adaptive` (Nav3 list-detail).
