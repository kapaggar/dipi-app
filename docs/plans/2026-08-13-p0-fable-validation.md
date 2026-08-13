# P0 plan — Fable validation

**Date:** 2026-08-13  
**Repo:** `/Users/wizops/DIPI/dipi-staff-android`  
**Server (read-only):** `/Users/wizops/DIPI/dipi-web`  
**Audience:** Fable (or any design reviewer) — **validate or reject this plan before any Android scaffold.**

This is **P0**: product lock + API truth + risks. It is not an implementation plan. Do not write Kotlin or PHP while reviewing.

Paste this file into Fable and ask: *Validate or reject each claim. Cite desk PHP if you disagree.*

---

## 1. One sentence

> A phone/tablet for **centre staff** talks to DIPI `dh_manageapp` so a registrar can work **today’s course applicants** without the Drupal DataTables desk.

---

## 2. Locked product decisions

| Item | Decision | Fable: accept? |
|------|----------|----------------|
| Shape | Native staff client, not WebView, not student-apply | |
| Server | Drupal 7 `dh_manageapp` only | |
| Vertical 1 | Login, centre+course, worklist/search, public card, status change, `a_attended` | |
| Vertical 2 (later) | Day-0 lists, conf-no scan, rooms/cells/seating | |
| Out | Letter admin, bulk mail, finalize, AT portal, LC review UI, 50-field editor, `/api` as registrar API | |
| Auth | Real staff Drupal user + `dh_user_center`. Never APP API keys | |
| Tenancy | Centre + `access male` / `access female` on every list | |
| Status writes | Existing `_change_status` only. No client-side status engine | |
| Letters | Black box: may fire from `_change_status`. No body preview | |
| NPI | No `ae_*`, Aadhaar, PAN, passport, voter id in v1 | |
| Offline | Cache **one** course worklist + outbox for status/attended. Server wins | |

If Fable rejects a row, stop. Do not implement around a rejected lock.

---

## 3. Claims Fable must verify against PHP

Each claim is **true** in the 2026-08-13 `dipi-web` tree or the plan is wrong.

| ID | Claim | Evidence to open |
|----|--------|------------------|
| C1 | There is **no** registrar REST. `/search-app/{cid}` is an HTML page. | `dh_manageapp.module` menu `search-app/%centre_id` → `dh_manageapp_search_form`; `search.inc` builds HTML + DataTables |
| C2 | Status write already returns JSON: `/change-status/{app_id}` with `s`, `l`, `c` → `{status, msg, confno, newstatus}` | `dh_manageapp.module` `_change_status()` ~1456–1536 |
| C3 | Confirm mints `{N\|O\|S}{M\|F}` + sequence via `generate_conf_no` / `nextval1`. Expected→Confirmed does not remint | `_update_status` + `generate_conf_no` ~1540–1913 |
| C4 | `_change_status` with `s=Approved` or `s=R-ATReview` is an **LC** path (may fail without Area/Reco AT). v1 UI must **not** offer `Approved` | `_change_status` first branch |
| C5 | `/app-update-attended/{id}` is **not** a boolean. If `a!=false` and `today <= c_start`, it **requires** room section `s` and room `r` | `dh_app_update_attended` ~2072–2115 |
| C6 | Therefore v1 “mark attended” **cannot** reuse `/app-update-attended`. Need a thin `a_attended` write | same |
| C7 | `search.inc` SELECT includes NPI (`a_aadhar`, `a_pancard`, `a_passport`, `ae_desc_*`). New list JSON must **whitelist** public `a_*` | `search.inc` ~83–165 |
| C8 | Centre mapping is `dh_user_center` (`uc_user`, `uc_center`, `uc_deleted=0`) | `center-dashboard.inc` ~22; `_manageapp_check_access` |
| C9 | `/api` + APP API is apply/AT/catalogues and is **unsafe** as a staff client (`get-app-detail` IDOR) | `dipi_api` (do not design against it; only confirm “do not use”) |
| C10 | `c_finalized=1` blocks add/edit application | `application.inc` ~38–44 |

**Fable output required:** for each C1–C10, `ACCEPT` or `REJECT` + one-line reason. If REJECT, quote the function that contradicts.

---

## 4. Hybrid API (proposed)

| Route | Kind | Backs onto |
|-------|------|------------|
| `POST /api/user/login` + CSRF token | Reuse shape | Drupal Services |
| `GET /staff/session` | **New** | `users`, `dh_user_center`, `user_access`, `mode_test` |
| `GET /staff/centres/{cid}/courses` | **New** | `dh_course` unfinalized |
| `GET /staff/courses/{id}/applicants` | **New** | `search.inc` WHERE + **whitelist SELECT** |
| `POST /staff/applicants/{id}/status` | Façade **or** raw `/change-status` | `_change_status` |
| `POST /staff/applicants/{id}/attended` | **New thin write** | `a_attended` only |
| `GET /staff/meta/statuses` | **New** | `dh_type_detail` |

Full schema: `docs/openapi-staff.yaml`.

**Fable must answer:**

1. Is the “new search JSON” the **only** forced read API? (plan says yes)
2. Is a thin `a_attended` façade acceptable for P0, or must attended wait for Vertical 2 rooms?
3. Should status go **directly** to `/change-status` from the app, or only through `/staff`? Plan allows either; pick one for implementers.

---

## 5. Screens (Vertical 1)

1. Login (base URL, username, password)
2. Centre + course picker (hide finalized)
3. Today — status chips, counts, search, list
4. Applicant **public card**
5. Change-status sheet — “this may send a letter”
6. Settings — test-mode banner, logout

Tablet: Navigation 3 list-detail. Phone: stacked.

**Fable:** reject if a Vertical 1 screen is missing for the daily loop, or if Day-0 leaked in.

---

## 6. Assumptions (challenge these)

| A | Assumption | If false |
|---|----------------|----------|
| A1 | Staff will log in with existing Drupal desk accounts | Need a new auth product |
| A2 | One active course at a time is enough cache | Need multi-course Room |
| A3 | Letter template id `0` (server default) is OK in v1 | Must list `dh_letter` |
| A4 | Public card without medical/ID is usable at the desk | Card is too thin |
| A5 | Mock server is enough to demo before `dh_staff_api` exists | Blocked on PHP first |
| A6 | Centres can type a base URL (no baked hostname) | Need discovery |

---

## 7. What Fable must **not** do

- Redesign as student-apply or AT app
- Add seating, CameraX, WhatsApp, or `/zeroday` to P0
- Invent status transitions in Kotlin
- Expand OpenAPI beyond Vertical 1
- Copy `search.inc` SELECT wholesale (NPI)

---

## 8. Pass / fail for this review

**PASS** if Fable:

- ACCEPT all of C1–C10, or REJECT with a cited PHP fix we can apply to the plan
- Chooses status transport (raw `/change-status` vs `/staff` façade)
- Chooses attended (thin façade in V1 vs defer to V2)
- Lists no extra Vertical 1 screens
- Does not require APP API

**FAIL** if Fable requires WebView, HTML scrape, `/api` staff client, or Day-0 in P0.

---

## 9. After PASS

Human says “approve Vertical 1”. Then scaffold `org.dhamma.dipi.staff` per `docs/00-architecture.md` §8 (six steps). Do not start that in the validation session.

---

## 10. Sources in this repo

- `docs/00-architecture.md`
- `docs/openapi-staff.yaml`
- `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md`
- `docs/DIPI_MEMORY_MAP.md`
- `AGENTS.md`
