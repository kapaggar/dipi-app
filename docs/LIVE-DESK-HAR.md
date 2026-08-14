# Live desk flow (from `dipi.vridhamma.org.har`)

Captured 2026-08-13. Centre in this session: **91 Dhamma Ganga**. Course opened: **68669** (`Dhamma Ganga / STP / 2026 / 19th-Aug to 27th-Aug`).

There is **no** `POST /api/user/login` and **no** `/staff/*` in the capture. The browser uses Drupal HTML + one embedded JSON array.

## Login

1. `GET /` (anonymous) → **403** HTML that still contains the login block  
   `form action="/home?destination=home"` `id="user-login-form"`.
2. `POST /home?destination=home`  
   `application/x-www-form-urlencoded`:

   | field | value |
   |---|---|
   | `name` | Drupal username |
   | `pass` | password |
   | `form_build_id` | from the 403/home page |
   | `form_id` | **`user_login_block`** |
   | `op` | `Log in` |

   No `form_token` on this block. Response **302** → `/home`.
3. `GET /home` → **302** `/centre`
4. `GET /centre` → **302** `/centre/{cid}` (here `/centre/91`)
5. `GET /centre/91` → **200** “Manage Dhamma Ganga”

Logout: `GET /user/logout` → 302 `/` (then 403 again).

Session is a normal Drupal cookie (`SESS…`). The Android client must persist the full cookie jar, not a single header.

## Centre dashboard — `GET /centre/{cid}`

HTML. Nav (only links the account may open):

- `/centre/{cid}/edit`
- `/manage-course/{cid}`
- `/letters/{cid}`
- `/search-app/{cid}` (global search form — **not** used in this capture)
- `/daily-activity/{cid}`, `/at-schedule/{cid}`, `/referral/{cid}`
- `/centre/{cid}/course-report`, `/centre/{cid}/bulk-mail-schedule`

Plus a table of **upcoming courses**. This capture listed:

| id | name |
|---|---|
| 68669 | Dhamma Ganga / STP / 2026 / 19th-Aug to 27th-Aug |
| 68670 | Dhamma Ganga / 10 Day / 2026 / 2nd-Sep to 13th-Sep |
| 68671 | Dhamma Ganga / 10 Day / 2026 / 16th-Sep to 27th-Sep |
| 68672 | Dhamma Ganga / 3 Day / 2026 / 3rd-Oct to 6th-Oct |

Each row links to `/course/{cid}/{courseId}` and many `/search-course/{cid}/{courseId}?s=…` count cells.

## Course page — `GET /course/{cid}/{courseId}`

HTML. Title is the course name. Actions (from `dh_course_dashboard`):

- **View Applications** → `/search-course/{cid}/{courseId}?s=&t=&g=&d=a`
- Add Application, Assign Teacher
- Day 0 List, Zero Day
- Teachers / Manager / Cell list, Seating, Group seating
- Laundry, Valuable, Student Chit, Checking Slip
- Course Summary Report, Male/Female PDF

Below that: **Course Summary** — counts by status × gender × old/new, each cell a `search-course` URL.

## Worklist — `GET /search-course/{cid}/{courseId}`

This is the registrar list. **GET only** (not `POST /search-app`). Query:

| param | meaning |
|---|---|
| `s` | status, empty = all, comma-separated allowed (`Confirmed`, `Received`, `Clarification-Response,Received`, …) |
| `t` | old student: `1` old, `0` new, empty both |
| `g` | `M` / `F` / empty |
| `d` | `a` applicant (default), `s` student DB |
| `at` | `a` Student, `s` Sevak |

Examples from the HAR:

- all applicants: `/search-course/91/68669?s=&t=&g=&d=a` (55 rows)
- confirmed only: `/search-course/91/68669?s=Confirmed&t=&g=` (39 rows)

Response is HTML. The table is filled from inline JS:

```js
var dataset = [ { aid, name, app_status, confno, gender, … }, … ];
```

Same shape as `dh_manageapp_search_results()`. Includes NPI (`aadhar`, `passport`, `voterid`, `pancard`) — **do not store or log those**. Public fields we already map: `aid`, `name`, `app_status`, `confno`, `gender`, `o_n`, `type`, `city`, `state`, `country`, `dob`, `age`, phones, email, `photo`, course counts, `first_course`, `last_course`.

Change-status in the page is still `GET /change-status/{aid}?s=&l=&c=` (not hit in this HAR).

## Client (from 1.2.0)

The app mirrors this HAR. Do **not** call Services `POST /api/user/login` on the live host.

Mirror the browser:

1. `GET /` (or `/home`) → scrape `form_build_id` + `form_id=user_login_block`
2. `POST /home?destination=home` with `name` / `pass` / tokens
3. Follow redirects to `/centre/{cid}`
4. Parse course ids/names from that HTML (or follow `/course/{cid}/{id}`)
5. Load the worklist with `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` and parse `var dataset`

`GET /get-courses/{cid}` and `POST /search-app/{cid}` were **not** used in this session.
