# Live desk — source of truth

Everything the app knows about the live Drupal desk at `https://dipi.vridhamma.org`,
in four parts: the captured transport (HAR), the PHP page inventory, the
server facts mined from `dipi-web` (`dh_manageapp`), and the sheet route map and
markup skeletons. Backend PHP is immutable — no `/staff/*`, no changes to
`dipi-web` (AGENTS.md).

- [Part 1 — Captured transport](#part-1--captured-transport-har)
- [Part 2 — PHP page inventory for Android](#part-2--php-page-inventory-for-android)
- [Part 3 — Server memory map](#part-3--server-memory-map-dipi-web)
- [Part 4 — Sheet routes and markup skeletons](#part-4--sheet-routes-and-markup-skeletons-har-sweep-2026-09-02)

---

## Part 1 — Captured transport (HAR)

Captured 2026-08-13. Centre in this session: **91 Dhamma Ganga**. Course opened: **68669** (`Dhamma Ganga / STP / 2026 / 19th-Aug to 27th-Aug`).

There is **no** `POST /api/user/login` and **no** `/staff/*` in the capture. The browser uses Drupal HTML + one embedded JSON array.

### Login

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

### Centre dashboard — `GET /centre/{cid}`

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

### Course page — `GET /course/{cid}/{courseId}`

HTML. Title is the course name. Actions (from `dh_course_dashboard`):

- **View Applications** → `/search-course/{cid}/{courseId}?s=&t=&g=&d=a`
- Add Application, Assign Teacher
- Day 0 List, Zero Day
- Teachers / Manager / Cell list, Seating, Group seating
- Laundry, Valuable, Student Chit, Checking Slip
- Course Summary Report, Male/Female PDF (desk site only — the app Board dropped the two course PDFs in 1.37.1)

Below that: **Course Summary** — counts by status × gender × old/new, each cell a `search-course` URL.

### Worklist — `GET /search-course/{cid}/{courseId}`

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

### Client (from 1.4.1)

The app mirrors this HAR. Do **not** call Services `POST /api/user/login` on the live host.

1. Drop persisted cookies (`commit()`, not a racy `apply()`).
2. `GET /user/login` (200) first; fallback `GET /` or `GET /centre` (403 HTML is in Retrofit `errorBody()` — `Response.html()`).
3. POST to the parsed form action (`/user/login` with `user_login`, or `/home?destination=home` with `user_login_block`).
4. Follow redirects to `/centre` → `/centre/{cid}` (`dh_user_center`). Parse all mapped centres if the select is present.
5. Parse upcoming course links from `/centre/{cid}`.
6. Worklist: `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` and parse `var dataset`.
7. While signed in: every 20 minutes `GET /services/session/token` + `GET /centre`.

`GET /get-courses/{cid}` and `POST /search-app/{cid}` were **not** used in this session. Photo upload is not exposed live.


---

## Part 2 — PHP page inventory for Android

**Scope (observed):** read-only scan of `/Users/wizops/DIPI/dipi-web/sites/all/modules/dh_manageapp`. No website login. No PHP edits. No credentials, cookies, tokens, or student values.

**Android v1 baseline (observed from local app docs, not PHP):** `dipi-app` Vertical 1 **1.4.1** on `feat/vertical-1`. Screens: Login, Courses, Today worklist, Applicant card, Change-status sheet, Day 0 summary (read-only stub seating), Photo review (mock / no live upload), Settings.

**How to read this file**
- **Fact** = quoted from PHP (`dh_manageapp_menu`, callbacks, `$rs` keys, HTML regions).
- **Guess** = inference for Android design, not proven by PHP.
- Login wall is omitted (Drupal core `user/login`, not in this module’s `hook_menu`).
- `templates/tpl/js` does **not** exist. Desk JS lives in `js/` (`manageapp.js`, `location-common.js`, select2, jquery-confirm) plus large **inline** scripts in `search.inc`, `zero-day.inc`, `at-schedule.inc`, `referral.inc`. `tpl/` only has `center-dashboard-page.tpl.php` and `vri-management-page.tpl.php` (teacher/VRI dashboards, not the registrar desk).

---

### 1. Hierarchical map — authenticated desk pages a centre registrar uses

Source: `dh_manageapp_menu()` in `dh_manageapp.module` (lines 96–692). Access is Drupal permission strings in `access arguments` unless noted.

**Entry (fact):** `dh_manageapp_home()` on `home` (`access content`) redirects: `access centre` → `centre`; else `access at portal` → `at-portal`. A single-centre user in `dh_zero_select_centre()` is auto-sent to `centre/{cid}`.

**Not registrar desk (listed so they are not mistaken for v1):** `admin/dh_manageapp` (site admin), `user-mapping` / `users`, `center-dashboard` / `center-dashboard-page` / `vri-management` (teacher/VRI), `search-lc` (AT LC review; **menu key registered twice**, second wins with `a-at review`), `at-portal/referral/add`, `webhook/*`, `wa-hook*`.

#### 1.1 Centre

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| Centre picker | Select centre (Select2); 1 centre → auto-goto | `centre` | `dh_manage_centre` → `dh_zero_select_centre` | `access centre` |
| **Centre dashboard** | Module link list + course picker + received/upcoming blocks | `centre/%centre_id` | `dh_manage_centre` → `dashboard()` | `access centre` |
| Centre settings | Edit centre + acco INI + hall/seat config | `centre/%centre_id/edit` | `dh_addedit_centre` → `dh_ma_centre_form` | `access centre settings` |
| Add centre | Create centre (admin) | `centre/add` | `dh_addedit_centre` | `access all centres` |
| Acco AJAX | CRUD rooms/sections | `centre/%centre_id/acco-handler` | `dh_acco_handler` | `access centre` |
| Manage courses (list/editor) | DataTables Editor create/edit/delete | `manage-course/%centre_id` | `dh_manage_courses` | `manage course` |
| Course handler AJAX | Editor POST | `course/handler/%centre_id` | `dh_manage_course_handler` | `manage course` |
| Course types JSON | Types for editor | `course/get-types` | `dh_get_course_types` | `access course` |
| Advanced search form | Filter then same worklist table | `search-app/%centre_id` | `drupal_get_form` → `dh_manageapp_search_form` | `access manageapp` |
| Cross-centre search | Same form, all centres | `search-app` | `drupal_get_form` → `dh_manageapp_search_form` | `access all centres` |
| Daily activity | Filter log table | `daily-activity/%centre_id` | `drupal_get_form` → `dh_daily_activity_form` | `access daily activity` |
| SMS report | Centre SMS usage | `centre/%centre_id/sms-report` | `dh_center_sms_report` | `view sms report` |
| Course report | Summary form + Excel | `centre/%centre_id/course-report` | `drupal_get_form` → `dh_center_course_report_form` | `view course report` |
| Bulk mail schedule | List/edit/mute campaigns | `centre/%centre_id/bulk-mail-schedule` | `dh_show_bulk_mail_schedule` | `mass mail` |
| Bulk mail actions | delete / edit / show-log / get-log / mute / unmute | `bulk-mail/%centre_id/%bulk_mail_id/{delete,edit,show-log,get-log,mute,unmute}` | `dh_delete_*` / `dh_edit_*` / `dh_show_log_*` / `dh_get_log_*` / `dh_mute_*` / `dh_unmute_*` | `mass mail` |
| Logout | Session end | `user-logout` | `get_user_logout` | `access callback` = true |

Dashboard module links (fact, `dh_manage_centre` `$modules`, each shown only if `drupal_valid_path`): Centre Settings, Manage Courses, Manage Letters, Search, Daily Activity, AT Schedule, Referral List, Center Referral List, SMS Report, Course Report, Bulk Mail Schedule.

#### 1.2 Course

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **Course dashboard** | Link grid + `course_summary` counts | `course/%centre_id/%course_id` | `dh_manage_courses_main` → `dh_course_dashboard` | `access course` |
| **Search-course worklist** | Status/gender/old/type filtered DataTable | `search-course/%centre_id/%course_id` | `_search_course` → `dh_manageapp_search_results` | `access manageapp` |
| Add application | Full applicant form | `app/add/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_ma_applicant_form` | `add application` |
| Add application (modal) | Same form via ctools | `app/add/%centre_id/%course_id/%ctools_js` | `dh_manage_day_zero` | `access zero day` |
| Assign teacher | AT apps + type/group/status | `assign-teacher/%centre_id/%course_id` | `dh_assign_teacher` | `at scheduling` |
| Day 0 list (print) | Attendance list PDF/HTML | `day0-list/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_zero_day_list` | `access zero day` |
| **Zero Day** | Unattended + attended + mark-attended dialog | `zero-day/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_zero_main` | `access zero day` |
| Zero Day pickers | Centre then course | `zero-day`, `zero-day/%centre_id` | `dh_manage_day_zero` | `access zero day` |
| Teacher / manager / cell / laundry / valuable lists | Generate/print lists | `teacher-list|manager-list|cell-list|laundry-list|valuable-list/%centre_id/%course_id` | `dh_manage_day_zero` → matching `dh_generate_*` / `dh_*_list` | `view teachers list` (teacher-list only); else `access zero day` |
| **Seating plan** | Hall grid; `?r=1` regenerates | `seating/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_generate_seating_plan` | `access zero day` |
| Seating update | AJAX save drag-drop | `seating-update/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_update_seating` | `access zero day` |
| Group-wise seating | Group columns; `?r=1` regen | `group-seating/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_generate_group_seating_plan` | `access zero day` |
| Group seating update | AJAX save | `group-seating-update/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_update_group_seating` | `access zero day` |
| Seating2 | Alternate INI layout (`seating.inc`) | `seating2/%centre_id/%course_id` | `dh_manage_day_zero` → `dh_generate_seating_plan2` | `access zero day` |
| Student chit / checking slip | Print slips; `?seating=1` | `student-chit|checking-slip/%centre_id/%course_id` | `dh_student_chit` / `dh_checking_slip` | `access zero day` |
| Day 11 report | PDF | `report-day11/%centre_id/%course_id` | `course_day11_report` | `access zero day` |
| Male / female course PDF | Generate; `?seating=1` sort | `course-pdf-m|course-pdf-f/%centre_id/%course_id` | `course_pdf` | `access male` / `access female` |

Course dashboard `$modules` (fact, `dh_course_dashboard`): View Applications (`search-course` with `s=&t=&g=&d=a`), Add Application, Assign Teacher, Day 0 List, Zero Day, Teachers List, Manager List, Cell List, Seating Plan, Group-wise Seating, Laundry List, Valuable List, Student Chit, Checking Slip, Course Summary Report, Generate Male/Female PDF. Cell/Seating/Group links also offer **Regenerate** (`?r=1`).

#### 1.3 Applicant / worklist actions

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **Change status** | JSON write `s/l/c` | `change-status/%app_id` | `_change_status` | `change status` |
| Edit application | Full form | `app/%app_id/edit` | `dh_manage_day_zero` → `dh_ma_applicant_form` | `edit application` |
| Edit (modal) | Same | `app/%app_id/edit/%ctools_js` | `dh_manage_day_zero` | `edit application` |
| Delete application | JSON delete | `app/%app_id/delete` | `_delete_app` | `delete application` |
| Send AT review email | LC email | `app/%app_id/send-at-email` | `send_at_email` | `edit application` |
| Mark attended | Day0 POST | `app-update-attended/%app_id` | `dh_app_update_attended` | `access zero day` |
| Transfer course | JSON move | `move-to-course/%app_id/%course_id/%centre_id` | `_move_to_centre_course` | `access course` |
| Courses for applicant | Child table HTML | `app-courses/%app_id` | `_search_student` | `access manageapp` |
| Clarifications | Child table HTML | `app-clarifications/%app_id` | `_get_clarifications` | `access manageapp` |
| Activity log | Child table HTML | `app-activity/%app_id` | `_get_activity` | `access manageapp` |
| Application PDF | Stream PDF | `show-application/%app_id` | `show_application_pdf` | `access application pdf` |
| Applicant photo | Stream image | `show-photo/%app_id` | `show_application_photo` | `access manageapp` |
| Clarification file | Stream | `show-clarification/%app_id/%clarification_id` | `show_clarification` | `access manageapp` |
| Transfer course list | JSON courses | `get-courses/%` | `_get_courses` | `transfer course` |
| Finalize / cancel counts | JSON | `app-student-count-finalize/%course_id`, `app-student-count-cancel/%course_arr` | `_get_student_finalize`, `_get_student_cancel_count` | `access manageapp` |
| Deep-link one app in search | Show one row | `search-app/%centre_id/%bulk_mail_id/%app_id` | `dh_manageapp_search_show_app` | `access manageapp` |

#### 1.4 Letters

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **Letter list** | DataTable + deleted restore | `letters/%centre_id` | `dh_manage_letters` → `dh_letters_listing` | `manage letters` |
| Add / edit / copy | Template form | `letters/%centre_id/{add,edit,copy}/%letter_id` | `dh_manage_letters` → `dh_letters_form` | `manage letters` |
| Delete / restore / del attach | Soft-delete writes | `letters/%centre_id/{delete,restore,delattach}/%letter_id` | `dh_manage_letters` | `manage letters` |
| Letter fields | Custom merge fields editor | `letter-fields/%centre_id` | `dh_manage_letter_fields` | `manage letters` |
| Letter fields AJAX | Editor POST | `letter-fields/handler/%centre_id` | `dh_manage_letter_fields_handler` | `manage letters` |

#### 1.5 AT scheduling

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **AT schedule** | Recent Received + upcoming/completed course tables | `at-schedule/%centre_id` | `dh_manage_at_schedule` | `at scheduling` |
| Assign teacher (per course) | Add AT + change status/type/group | `assign-teacher/%centre_id/%course_id` | `dh_assign_teacher` | `at scheduling` |
| AT status / type / group | AJAX | `at-schedule/change-{status,type,group}/%atappid` | `at_app_change` | `at scheduling` |
| Delete trainee | Write | `at-schedule/del-trainee-teacher/%centre_id/%course_id/%atappid` | `del_trainee_teacher` | `at scheduling` |
| AT info popup | Read | `at-schedule/get-at-info/%` | `get_at_info` | `access at details` |
| AT activity | Child HTML | `at-app-activity/%atappid` | `_get_at_app_activity` | `at scheduling` |
| Trainee autocomplete | Lookup | `autocomplete/get-trainee-teacher` | `_get_trainee_teacher` | `_access_teacher` (`access zero day` **or** `at scheduling`) |

#### 1.6 Referral

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **Referral list** (AT-listed, `r_center=0`) | DataTable + expand + delete/readonly | `referral/%centre_id` | `dh_referral_results` | `view referral list` |
| Add / edit referral | Long form | `referral/%centre_id/{add,edit/%ref_id}` | `dh_addedit_referral` → `dh_referral_form` | `manage referral list` |
| Delete referral | Write | `referral/%centre_id/%ref_id/delete` | `referral_delete` | `delete referral list` |
| Readonly flag | JSON | `referral/read-only/%ref_id` | `referral_readonly` | `delete referral list` |
| Referral activity | Child HTML | `referral/get-activity/%` | `get_referral_activity` | `view referral list` |
| **Center referral list** (`r_center=cid`) | Same UI, centre-owned | `center-referral/%centre_id` | `dh_referral_results` | `view center referral list` |
| Add / edit / delete center referral | Same form | `center-referral/%centre_id/{add,edit/%ref_id, %ref_id/delete}` | `dh_addedit_referral` / `referral_delete` | `manage center referral list` / `delete center referral list` |
| Global referral (no centre) | Same list | `referral` | `dh_referral_results` | `view referral list` |

#### 1.7 Autocomplete / system (desk-adjacent)

`autocomplete/get-{country,state,city}`, `get-location-from-pincode` — `access zero day`. `autocomplete/get-teacher` — `_access_teacher`. Used by applicant/referral/centre forms, not standalone pages.

---

### 2. Registrar-critical pages — regions, filters/tables, read vs write

#### 2.1 Centre dashboard — `centre/%centre_id` — `dh_manage_centre` + `dashboard()`

**Regions (fact)**
1. Red `important_notice` variable (site-wide banner).
2. **3-column link list** (`ul.multi-column`) from `$modules` (permission-gated via `drupal_valid_path`).
3. **Course picker** — `dh_zero_select_course($with_header=false)`: Select2 of courses with `c_start >= max_old_courses` (default 6 months). Change → `location.href = /course/{cid}/{courseId}`.
4. **Received/Response Applications** — only if `received applications block`. Table: Course, New/Old/Total Male, spacer, New/Old/Total Female. Counts = `Received` + `ClarificationResponse` (keys from `COURSE-SYSTEM-STATUS`). Each cell links to `search-course/{cid}/{courseId}?s=Received,ClarificationResponse&t=&g=` (and `t=0|1`, `g=M|F`).
5. **Upcoming Courses** — only if `upcoming courses block`. Next **4** un-deleted future courses (`c_start >= today`). Renders `course_summary()` blocks.

**Filters:** none on the page itself (picker + count links).

**Read vs write:** **read**. Writes live on linked pages (settings, course editor, letters, …).

#### 2.2 Course dashboard — `course/%centre_id/%course_id` — `dh_course_dashboard`

**Regions (fact)**
1. Course name + **Edit** → `manage-course/{cid}?c={courseId}` (opens editor on that row).
2. Back to Dashboard.
3. **3-column action links** (`$modules` above). View Applications always shown (not `drupal_valid_path`-gated). Others gated.
4. **Course Summary** — `course_summary($cid, "where c_id=…")`. Per-status rows: NM, OM, Total, SM, spacer, NF, OF, Total, SF. Status list = `COURSE-SYSTEM-STATUS` + `COURSE-STATUS`; LC courses add `COURSE-LC-STATUS` if user has `r-at review` or `a-at review`. Cells link to `search-course` with `s`, `t` (0 new / 1 old), `g`, `at=a|s`.

**Read vs write:** **read** hub. Edit course is write on another route.

#### 2.3 Search-course worklist — `search-course/%centre_id/%course_id` — `_search_course`

**Query params (fact):** `s` status (comma-separated), `t` old (`0`/`1`/empty), `g` gender (`M`/`F`/empty; forced if user lacks both `access male` and `access female`), `d` dataset (`a` applicant default, `s` student), `at` type (`s` Sevak / else Student).

**Regions (fact)**
1. H3 filter summary + Back to Course + Back to Dashboard + **Add Applicant**.
2. Empty `#table-applicants` filled by JS `var dataset = …` (`json_encode($rows)` from `dh_manageapp_search_results`).
3. Footer back links.

**Visible DataTable columns (fact):** Detail (expand), Applicant Name, Edu/Occ/Comp/Desig (`eocd`), Status, Type (`o_n` = Old/New + gender), Age, ChangeStatus, Action. Mobile + Email columns exist but `visible:false` and remain searchable. Columns 10–38 hidden (Excel export only).

**Expand child (`format(d)`):** clarification AJAX (`/app-clarifications/{aid}`), referral block if `referral>0`, LC RecoAT/AreaT if `long_course`, address+contact, course-count grid, first/last/practice, photo (`show-photo/{aid}`), physical/other/dept, mental/medication/intox/left_reason, internal note/extra/willing-to-serve/discourse-lang/pregnant, friend-family, applied-courses AJAX (`/app-courses/{aid}`), activity AJAX (`/app-activity/{aid}`).

**Filters:** URL query only (no on-page filter form). DataTables search box is client-side over `dataset`. Pagination position from `cs_pagination` (top/bottom/both).

**Writes (fact, same page)**
- **Change status:** `.update-status` → `GET /change-status/{aid}?s=&l=&c=` (see 2.4).
- **Action select** (permission-gated): Transfer (`/move-to-course/…` + `/get-courses/{cid}`), Add (copy-to-course dialog), Add Referral / Add Center Referral (navigate with `?aid=`), Delete (`/app/{id}/delete`), Send Review Email (`/app/{id}/send-at-email`, LC only).
- **Excel export** if `export data` and not iOS.

**Read vs write:** **read + write**. This is the registrar’s main worklist.

**Guess:** Android “Today” is this page with `s=&t=&g=&d=a` (all statuses), not the advanced `search-app` form. App docs state that explicitly.

#### 2.4 Change-status — `change-status/%app_id` — `_change_status`

**Not a page.** JSON endpoint. Desk UI is the worklist **ChangeStatus** column + jquery-confirm letter picker.

**Request (fact):** `GET /change-status/{app_id}` with `s` status, `l` letter id (0 if none), `c` comment. JS: `$.getJSON("/change-status/"+id, { s, l: letter, c: comment })`.

**Response keys (fact):** `status` (`OK`|`Failed`), `msg`, `confno`, `newstatus`.

**Server branches (fact)**
- `s == Approved` or `s == R-ATReview`: LC workflow. If current `R-ATReview` and Area AT set → force `A-ATReview`; if no Area AT → **Failed** + `Please Edit application and choose Area teacher before approving!`. If current `R-ATTransfer` and recommending set → `R-ATReview`; else Failed + recommending-teacher message. Else treat as Area-AT approve → `Received`. Calls `update_status_external`. **Does not send a letter in this branch.**
- `Rejected-R-AT` / `Rejected-A-AT`: set LC approve field Rejected; `newstatus=Rejected`.
- Else: `_update_status($app_id, $status)` then `dh_send_letter('applicant', $app_id, $status, $letter, $comment)`. Conf no minted in `_update_status` when moving to `STATUS-CONFIRMED` (unless already ReConfirmation/Error with existing conf). Custom status returns empty conf and skips some updates.

**Desk status dropdown (fact, JS in `search.inc`)**
- Finalized course → `NA`.
- `R-ATReview` / `A-ATReview` → Approved, Rejected-R-AT or Rejected-A-AT, Cancelled, Custom.
- `R-ATTransfer` → R-AT Review, Cancelled, Custom.
- LC and reco/area not approved → `NA`.
- Else: `COURSE-STATUS` keys + Confirmed + Cancelled + Custom; if current Confirmed/ReConfirmation also offer ReConfirmation + Expected.
- If multiple letters exist for the chosen event, a modal forces letter + optional comment before Update.

**Read vs write:** **write**. Letter send is a side effect of the non-LC-approve branch.

**Guess vs Android:** App docs say never send `Approved` and always `l=0`. Desk **does** offer Approved for LC AT-review rows and **does** pick a letter when several exist.

#### 2.5 Day 0 / Zero Day — `zero-day/%centre_id/%course_id` — `dh_zero_main`

**Gate (fact):** if `c_finalized` → error and goto course dashboard.

**Regions (fact)**
1. Back to Course | Back to Dashboard; course name; **Add New Applicant** (modal link).
2. `#applicant-list` — **Unattended Applicants** (`dh_manageapp_list`): DataTable `#table-applicants`. Columns: Update button, ConfNo, Name (+PDF), Gender, City, Age. Rows = `a_attended=0`.
3. `#dialog-div` — jQuery UI “Student Attended” dialog: Attending checkbox, Room Section, Room No (unallocated rooms only), Laundry, Valuable, CellNo, Cell Fixed, Group 1–9, Special Seating (None/Chowky/Chair/BackRest), Comments.
4. `#attended-list` — `dh_manageapp_attended`:
   - Link row: Assign Teacher | Teacher List | Seating Plan (+ Re-Gen) | Group-wise Seating.
   - `#day-summary`: Confirmed table (Old/New/Total/Server × M/F), Attended table (same), Special seating (Chowky/Chair/Backrest × M/F, old+new), optional Group table (Group No, Teachers, NM/OM/Total/SM, NF/OF/Total/SF) if >1 group.
   - `#table-attending`: Update, ConfNo, Name, Gender, Type, Age, Teen/10D/STP, LC, RoomNo, Laundry, Valuable, Chowky, Chair, BackRest, Group, hidden H (cell\|\|fixed\|\|comment).

**Write (fact):** dialog Update → `POST /app-update-attended/{app_id}` with `s,r,g,l,v,c,cf,chow,chai,back,comment,a`. Response replaces both lists + acco/alloted JSON.

**Read vs write:** **write** (attendance + room/cell/laundry/valuable/group/special seat). Print lists from course dashboard are mostly **read** (except seating regen).

#### 2.6 Seating — `seating/%centre_id/%course_id` — `dh_generate_seating_plan`

**Regions (fact):** standalone HTML (not Drupal chrome). Reads `cs_seat_config` INI + `cs_seat_naming_conv`. LEFT/RIGHT hall grids; seat cells show acco, name, old/new course counts, age, backrest, chowky/chair, cell. Combined seating if `c_combined_seat_course > 0`. `?r=1` → `dh_auto_allocate_seats` then render. Drag-drop save → `seating-update`. Invalid INI → error page pointing at Centre Settings.

**Group-wise seating:** separate grid (`dh_generate_group_seating_plan`); does not rewrite main hall seats.

**Seating2:** alternate drag UI in `seating.inc` (`dh_generate_seating_plan2`); help text + Print. **Guess:** less used; course dashboard links `seating` not `seating2`.

**Read vs write:** **read** by default; **write** on regen (`?r=1`) and drag-save.

#### 2.7 Letters — `letters/%centre_id` — `dh_letters_listing` / `dh_letters_form`

**List regions:** Add Letter | Manage Fields | Back to Dashboard. DataTable: Letter Name, Status (event), Course Type, Subject, Edit, Copy, Delete. Second table: Deleted Letters + Restore.

**Form fields (fact):** Letter Name, Status (`l_event` = system + custom + LC statuses), Course Type (or All), Subject, Body (filtered_html), SMS Text, Attachment. Collapsed merge-field cheat sheet from `_get_letter_fields`.

**Read vs write:** **write** (CRUD templates). Sending happens inside `_change_status` / crons / bulk-mail, not this page.

#### 2.8 AT — `at-schedule/%centre_id` + `assign-teacher/%centre_id/%course_id`

**AT schedule regions (fact)**
1. Back to Dashboard.
2. **Recent Received AT Applications** — DataTable: Detail, Course-Name, AT-Name, Gender, Current-Status, Change-Status (Received → Confirmed/Cancelled). Expand loads `/at-app-activity/{ct_id}`. Status change confirm → `at-schedule/change-status/{ct_id}`.
3. **Upcoming Courses** — course link, AT required M|F, nested teacher table (name, type+gender, status).
4. **Completed Courses** — year select, same columns, Confirmed-only teachers.

**Assign teacher regions (fact):** Back to Course | Back to AT Schedule or Zero Day. Required M/F counts. Add-teacher form (`dh_assign_teacher_form`). Teacher Applications table: status/type/group selects (`Confirmed`/`Cancelled`, `Conducting`/`Assisting`). Separate trainee-teacher form. Finalized course blocked.

**Read vs write:** **write** (AT application status/type/group).

#### 2.9 Referral — `referral/%centre_id` and `center-referral/%centre_id` — `dh_referral_results`

**Regions (fact):** Back to Dashboard | Add Referral (or Add Center Referral). DataTable `#table-applicants`: Detail, Applicant Name, Gender, Age, Email (`contact` HTML), Referred By, Start Date, End Date, Delete (+ r-only checkbox on AT list). Expand: address, contact, **Pancard / Aadhar / National ID / Passport**, occ/edu/company, listed-for grid (10d…longseva), reason, referring AT/centre, course counts, referral-course, activity AJAX.

**Filter:** `r_center=0` (AT list) vs `r_center={cid}` (centre list); `r_deleted=0`.

**Form (`dh_referral_form`):** Personal Information (name, gender, DOB, phones, email, education, occupation, …) plus listing flags/dates/reason (long form ~690 lines). NPI fields exist on the student/referral record (`s_aadhar`, `s_passport`, `s_voter_id`, `s_pancard`).

**Read vs write:** list is **read + delete/readonly**; add/edit is **write**.

---

### 3. Dataset / card field keys — `dh_manageapp_search_results` `$rs`

Built in `inc/search.inc` (~278–521). Encoded as `var dataset`. **Names only. No sample values.**

Applicant SQL also selects `a_passport`, `a_voter_id`, `a_pancard`, `a_aadhar` and maps them onto `$rs`. Student SQL path does **not** select those four columns (those `$rs` keys would be empty/undefined on `d=s`).

#### 3.1 Public / operational keys (always set on applicant path)

`allowtransfer`, `type`, `gender`, `o_n`, `courseid`, `centreid`, `finalized`, `aid`, `changestatus`, `name`, `course`, `status`, `app_status`, `confno`, `location`, `city`, `state`, `country`, `pin`, `dob`, `age`, `address`, `contact`, `contact_home`, `contact_mobile`, `contact_office`, `contact_email`, `occupation`, `designation`, `company`, `emergency_name`, `emergency_relation`, `emergency_num`, `lang_discourse`, `friend_family`, `note`, `willingtoserve`, `extra`, `monk`, `eocd`, `course_teen`, `course_10d`, `course_stp`, `course_spl`, `course_20d`, `course_30d`, `course_45d`, `course_60d`, `course_tsc`, `course_seva`, `left_reason`, `section`, `acc`, `first_course`, `last_course`, `course_others`, `practice_details` (old students only), `Education`, `Company`, `Dept`, `Occ`, `Designation`, `lang`, `alist`, `photo`, `long_course`, `reco_t`, `reco_status`, `area_t`, `area_status`, `app_created`, `referral`

#### 3.2 Medical / extra keys (not the four NPI IDs; still sensitive)

`physical`, `mental`, `medication`, `addiction`, `othertechnique`, `pregnant`, `id_issued_date`, `id_issued_by`, `nationality`

These are mapped from `ae_*` / `aa_*` columns. Android docs treat `ae_*` as NPI-adjacent and must not persist.

#### 3.3 NPI keys (do not persist / log)

| `$rs` key | Source column (applicant query) |
|---|---|
| `aadhar` | `a_aadhar` |
| `passport` | `a_passport` |
| `voterid` | `a_voter_id` |
| `pancard` | `a_pancard` |

Excel export column 18 = ID type label (Passport/Voter ID/Pan card/Aadhar); column 19 = the raw ID number(s).

#### 3.4 Conditional referral keys (only if `a_referral` matches an in-date listing)

`ref_reason`, `ref_start`, `ref_end`, `ref_listed_by`, `ref_listed_title`, `special_list`, `ref_listed_for`

If listing expired or course-level flags do not match, `referral` is forced back to `0`.

#### 3.5 What the desk card actually shows vs `$rs`

**Collapsed row (fact):** name (edit link + PDF + Sevak/AT suffix), `eocd` (edu/occ + course + Phy/Mental/Intox/Oth-Med/Preg/A-List/Monk/Referral chips), `status` (+ conf no), `o_n`, `age`, change-status widget, action widget.

**Expanded card (fact):** address, phones, email, course counts, first/last, practice, photo, medical, note, extra, discourse lang, pregnant, friend-family, referral block, LC teachers, plus AJAX clarifications / other courses / activity.

**Guess:** Android public card is a **subset** of `$rs` (name, status, confno, age, gender, monk, location, phones, email, dob, `app_created`, old-student history, photo URL). Desk card is richer and includes medical + NPI-in-export + street address.

---

### 4. Gap table vs existing Android Vertical 1

Legend: **have** = live desk path already used; **stub** = screen exists but not live/complete; **out of scope** = not in v1 product (per app docs).

#### 4.1 Android screens → desk

| Android v1 screen | Desk counterpart | Status | Notes (fact unless marked guess) |
|---|---|---|---|
| Login | Drupal `user/login` (not in this module) | **have** | App parses HTML login. Module only has `home` redirect + `user-logout`. |
| Courses | `centre/%centre_id` course picker + upcoming `course_summary` links | **have** | App parses upcoming links from `GET /centre/{cid}`. Does not use Manage Courses editor. |
| Today worklist | `search-course/%/%?s=&t=&g=&d=a` + `var dataset` | **have** | App must not persist NPI keys. Desk also has advanced `search-app` (not used). |
| Applicant card | worklist expand `format(d)` + public `$rs` | **have** | Android card is narrower (no medical, no street, no edit, no NPI). |
| Change-status sheet | worklist ChangeStatus → `GET /change-status/{id}?s=&l=0&c=` | **have** | Desk may prompt for letter; app always `l=0`. Desk offers `Approved` on LC rows; app must not send it. |
| Day 0 summary | `#day-summary` inside `dh_zero_main` / `dh_manageapp_attended` | **stub** | App computes Expected/Arrived from worklist; seating request table is dashes. No `app-update-attended`. |
| Photo review | `show-photo/%app_id` (read) only | **stub** | No live photo-upload route in `hook_menu`. `a_photo` is displayed; upload is on the full applicant form. |
| Settings | — (no desk settings page for theme/remember-me) | **have** (app-only) | Desk equivalent is Drupal user + `centre/{id}/edit` (centre config, not app chrome). |

#### 4.2 Desk page / action → Android v1

| Desk page / action | Android v1 |
|---|---|
| Centre picker / dashboard links | **have** (courses only; other dashboard links unused) |
| Received/upcoming count tables | **out of scope** (guess: counts exist only as Today chips) |
| Manage Courses editor | **out of scope** |
| Centre settings / acco / hall INI | **out of scope** |
| Advanced Search `search-app` | **out of scope** |
| Search-course worklist | **have** |
| Worklist expand (full desk card) | **stub** (public subset only) |
| Change-status + letter picker | **have** (status+comment; letter picker **out of scope**) |
| Transfer / Add / Delete / Send Review Email | **out of scope** |
| Add / edit application form | **out of scope** |
| Application PDF / clarification files | **out of scope** |
| Zero Day mark-attended + room/cell/laundry | **out of scope** |
| Day 0 summary numbers | **stub** (read-only, no attendance write) |
| Seating / group-seating / regen / drag-save | **out of scope** (summary seating table is stub dashes) |
| Teacher / manager / cell / laundry / valuable / chit / slip / D11 / course PDF | **out of scope** |
| Letters CRUD + merge fields | **out of scope** (letter send is black box behind `_change_status`) |
| Bulk mail / SMS report / course report / daily activity | **out of scope** |
| AT schedule + assign teacher | **out of scope** |
| Referral + center-referral CRUD | **out of scope** (referral **flag** may appear on card if `referral` in dataset; guess) |
| Photo upload / geometry review | **stub** (mock only) |
| LC AT-review Approved path | **out of scope** (app never sends `Approved`) |
| Logout / erase local | **have** |

---

### 5. Open design questions (do not pick)

These are questions only. No recommendation.

1. **v1 vs later desk pages.** Which of the centre-dashboard links (Letters, AT Schedule, Referral, Daily Activity, Reports, Bulk Mail, Centre Settings, Manage Courses) belong in a later vertical versus staying out of the phone entirely?
2. **Today vs full worklist.** Desk `search-course` is the whole course, all statuses, with DataTables search. Android Today is “find + change status” with chips. Should v1 stay chip+q only, or grow toward desk filters (`t` old/new, `g`, `at` Sevak, advanced `search-app` fields)?
3. **Card depth.** Desk expand shows medical, street address, emergency, LC teachers, referral reason, clarifications, activity, other courses, PDF. Android card deliberately omits ID/medical/street/edit/attendance. Which desk expand blocks (if any) should a later card add without pulling NPI?
4. **Photo review — keep or drop?** Live desk has **no** photo-review/upload API; only `show-photo/{id}` and the full edit form. Should the mock Photo review screen stay in v1 chrome, move to a later vertical that invents an upload path, or be removed so the tablet matches the desk?
5. **Day 0 — stay stub or grow?** Desk Zero Day is a write surface (mark attended, room, cell, laundry, valuable, group, special seat) plus print lists. Android Day 0 is a read-only summary with dashed seating. Should it stay a glance page, grow to the `#day-summary` tables (Confirmed/Attended/Special/Group), or become the full mark-attended dialog?
6. **Seating.** Desk seating is a full-hall drag grid driven by `cs_seat_config`. The Android mock footer had seating/teacher buttons that v1 was told not to build. Is seating a later vertical, a print-only WebView, or never-on-phone?
7. **Change-status chrome.** Desk: inline `<select>` + Update + optional letter modal + comment. Android: bottom sheet radios + comment + “server may send a letter”. Which desk behaviors to copy later: letter picker, LC Approved/Rejected, ReConfirmation/Expected extras, `NA` when finalized/LC-gated?
8. **Desk chrome to copy.** Centre dashboard is a 3-column link list + picker + count tables. Course dashboard is the same pattern. Worklist is a dense DataTable. Android is list–detail Material. Which desk chrome (link grid, count crosstab, expand-row, status-in-row) should a later tablet layout echo?
9. **Referral on the card.** Dataset can set `referral` + reason/dates. Desk paints the row and expand in referral styling. Android audit/card does not currently treat this as a first-class block. Surface a flag only, a read-only block, or leave it?
10. **Conf no + letters.** `_update_status` may mint `confno`; `_change_status` may send a letter. Android already shows minted conf no and a generic letter notice. Is that enough, or does the registrar need to see *which* letter went (desk shows letter name under the select)?
11. **Course dashboard as a hub.** After picking a course, desk lands on `course/{cid}/{id}` (actions + summary), not directly on the worklist. Android goes Courses → Today. Should a later version insert a course hub (Day 0, seating, PDFs, AT) or keep Today as home?
12. **Permissions.** Desk hides links via `drupal_valid_path` / `user_access`. App rule is no client ACL. If a later vertical adds Letters/AT/Referral, does the tablet still show every entry and let the server 403, or start reflecting missing dashboard links?

---

### Source index (fact)

| File | Used for |
|---|---|
| `dh_manageapp.module` | `dh_manageapp_permission`, `dh_manageapp_menu`, `_change_status`, `_update_status`, `_search_course`, `_get_clarifications`, `_get_activity`, `dh_manageapp_home` |
| `inc/centre.inc` | `dh_manage_centre`, `dashboard`, `received_applications`, `upcoming_courses`, `dh_ma_centre_form` |
| `inc/course.inc` | `dh_course_dashboard`, `dh_manage_courses`, `course_summary` |
| `inc/search.inc` | `dh_manageapp_search_results` `$rs` + DataTable + change-status JS; `dh_manageapp_search_form` |
| `inc/zero-day.inc` | `dh_manage_day_zero` router, `dh_zero_main`, `dh_manageapp_list`, `dh_manageapp_attended`, seating/lists |
| `inc/seating.inc` | `dh_generate_seating_plan2` only |
| `inc/letters.inc` | `dh_manage_letters`, `dh_letters_listing`, `dh_letters_form` |
| `inc/at-schedule.inc` | `dh_manage_at_schedule`, `dh_assign_teacher`, `at_app_change` |
| `inc/referral.inc` | `dh_referral_results`, `dh_referral_form` |
| `js/` | shared widgets; worklist/zero-day/AT/referral logic is inline in the `.inc` files |
| `dipi-app/AGENTS.md` | Android screen list for the gap table only |

**Menu quirk (fact):** `$items['search-lc']` is assigned twice in `dh_manageapp_menu()`; the second (`a-at review`) overwrites the first (`r-at review`).


---

## Part 3 — Server memory map (dipi-web)

Built 2026-08-13 from custom DIPI code only (Drupal 7 core and contrib excluded).
Graphify graph: `graphify-out/graph.html` (284 nodes, 310 edges, 55 communities).
AST cannot see Drupal `.module` files natively; those were staged as `.php` for extraction.

This map is the working model for a **centre-staff Android client**. It is not a student-apply app.

---

### 1. What this repo is

A Drupal 7 LAMP application that runs Vipassana centre registration worldwide.

Custom product code lives in:

| Path | Role |
|------|------|
| `sites/all/modules/dh_manageapp/` | Registrar desk (the product) |
| `sites/all/modules/dh_atportal/` | Assistant Teacher self-service |
| `sites/all/modules/dh_patrika/` | Physical newsletter |
| `sites/all/modules/dipi_api/` | Machine API (Services) |
| `sites/all/modules/find_people/` | Admin user search |
| `sites/all/modules/user_created_by/` | Who created a Drupal user |
| `sites/all/themes/tweme/` | Bootstrap skin |
| `cron-*.php`, `status-trigger.php`, `unfinalized.php` | CLI |

Everything else is stock Drupal 7 or contrib (Views, Services, CTools, Mailgun module).

---

### 2. Runtime shape

```
Browser registrar UI          AT portal UI           External machines
 (DataTables + Drupal forms)   (/at-portal)           (mobile / IVR / SMS / Mitra)
           \                         |                         |
            \                        |                         |
             +-------- Drupal 7 bootstrap + session/cookie ----+
                                 |
                    dh_* tables in MySQL
                                 |
          Mailgun | 360Dialog | S3 | RabbitMQ | SMS vendors | pdftk
```

There is **no registrar REST**. The desk UI is HTML pages plus a handful of JSON callbacks. The REST layer (`/api`, `/zeroday`, `/ivr`, `/sms`, `/mitra`) was built for other clients, not for the registrar.

---

### 3. Tenancy and identity

- Every operational row is **centre-scoped** (`a_center`, `c_center`, `l_center`).
- Staff access is `users` + `dh_user_center` (uid ↔ centre). `_manageapp_check_access()` counts that mapping.
- Gender split is a first-class permission: `access male` / `access female`.
- AT login username is `t_code.t_gender` (example `DHI001.M`), stored in both `dh_teacher` and `users`.
- Application confirmation number is generated on Confirm: `{N|O|S}{M|F}` + per-course sequence (`generate_conf_no()` via MySQL `nextval1`).
- Status labels are **not hardcoded forever**. Canonical values live in `dh_type_detail` (`COURSE-SYSTEM-STATUS`, `COURSE-STATUS`, `COURSE-LC-STATUS`). Code still has string literals (`Received`, `WaitList`, `R-ATReview`) in many branches.

Drupal Services auth for `/api`:

1. `POST /api/user/login` → session + token
2. Subsequent `POST /api/dipi/{action}/...`
3. AT extra: Sodium sealed box `username|||password` on `check-cred`

Role **APP API** already exists: `get all centres`, `get all courses`, `post application`, `get application status`, `at portal access`.

---

### 4. Applicant record (the atomic object)

Split across five tables, always keyed by `a_id`:

| Table | Prefix | Contents |
|-------|--------|----------|
| `dh_applicant` | `a_` | Identity, contact, status, centre, course, conf no, type, old/new, flags |
| `dh_applicant_course` | `ac_` | Course-count history (10d/20d/30d/…), first/last course |
| `dh_applicant_extra` | `ae_` | Medical, ID, pregnancy, parents (teen/child) — NPI |
| `dh_applicant_lc` | `al_` | Long-course eligibility + Reco/Area AT + approvals |
| `dh_applicant_attended` | `aa_` | Day-0: room, cell, laundry, valuables, group, seat, left |

After finalize, attended students are copied to `dh_student` / `dh_student_course` / `dh_student_course_input`. That is the permanent person record. Applications remain historical.

Form groups in `application.inc` (the registrar add/edit screen):

1. Personal (name, gender, DOB, phones, address, old/new, Student/Sevak, A-list, monk)
2. Work / education / department
3. ID + nationality + languages (3 + discourse)
4. Medical / emergency / internal note / friend-family
5. Course history counts (if old student)
6. LC screening (if LC course)
7. Teen/child extras (if teen/child course)
8. Day-0 allotment (section, room, cell, laundry, group, left)

Cannot add/edit if `c_finalized = 1`.

---

### 5. Status machine

System statuses (from `dh_type_detail` + code):

```
Received
  → Clarification → ClarificationResponse
  → Confirmed  → Expected → ReConfirmation → Attended
                                            ↘ Left
  → Cancelled | Rejected | Errors | WaitList | Duplicate | Custom | Deceased
```

Long course extra:

```
R-ATReview → A-ATReview → Received (approved)
           ↘ Rejected-R-AT
A-ATReview → Rejected-A-AT
R-ATTransfer → (registrar picks Reco AT) → R-ATReview
```

**Write path for a status change (desk):**

`GET/POST /change-status/{app_id}?s=&l=&c=` → `_change_status()` → `_update_status()` and/or `update_status_external()` → optional `dh_send_letter()`.

`update_status_external()` is the **god function** of the lifecycle:

- On `Received`: `set_referral()`, `dh_course_status_check()`, maybe force `WaitList`
- On Confirm: `generate_conf_no()` unless already Expected→Confirmed
- Rebuilds application PDF
- Syncs course Full/Waitlist from centre INI (`cs_course_config`)
- Duplicate is excluded from capacity counts (2026 change)

Letters fire on most non-LC status changes. Confirm/Expected attach the application PDF from S3.

Graphify confirms this: `update_status_external()`, `_update_status()`, `_change_status()`, `dh_atportal_review_form_submit()`, `_process_lc_application()` sit in one community (“Status Transition Core”).

---

### 6. HTTP surface a native client must replace

#### 6.1 Drupal pages (HTML, session cookie)

Centre home: `centre/{cid}`

Courses: `manage-course/{cid}`, `course/{cid}/{course}`, `course/handler/{cid}` (DataTables Editor CRUD on `dh_course`)

Applicants: `search-app/{cid}`, `app/add/{cid}/{course}`, `app/{id}/edit`, `app/{id}/delete`, `change-status/{id}`, `app-courses/{id}`, `app-activity/{id}`, `app-clarifications/{id}`, `show-application/{id}`, `show-photo/{id}`

Day-0: `zero-day/{cid}/{course}`, `teacher-list`, `manager-list`, `cell-list`, `laundry-list`, `valuable-list`, `seating`, `group-seating`, `day0-list`, `student-chit`, `checking-slip`, `report-day11`

Letters: `letters/{cid}`, `letter-fields/{cid}`

AT schedule: `at-schedule/{cid}`, `assign-teacher/{cid}/{course}`, `at-schedule/change-{status,type,group}/{atappid}`

Referrals: `referral/{cid}`, `center-referral/{cid}`

Comms: `centre/{cid}/bulk-mail-schedule`, `centre/{cid}/sms-report`, `centre/{cid}/course-report`

Dashboards: `center-dashboard-page`, `vri-management`

Webhooks (permission `access webhook`): `wa-hook`, `wa-hook-bulk`, `webhook/mailgun`, `webhook3/mailgun`

#### 6.2 JSON / AJAX already on the desk (not REST)

| URL | Job |
|-----|-----|
| `/change-status/{app_id}` | Status + letter |
| `/course/handler/{cid}` | DataTables Editor on courses |
| `/user-mapping/handler` | User↔centre mapping |
| `/letter-fields/handler/{cid}` | Custom merge fields |
| `/centre/{cid}/acco-handler` | Room inventory |
| `/autocomplete/get-{country,state,city,teacher}` | Typeahead |
| `/get-location-from-pincode` | Zip lookup |
| `/get-courses/{cid}` | Transfer target list |
| `/app-student-count-finalize/{course}` | Finalize preview counts |
| `/app-update-attended/{app}` | Toggle attended |
| `/move-to-course/{app}/{course}/{cid}` | Transfer |

These are the **real registrar API**, just not designed as one.

#### 6.3 Drupal Services (existing, wrong shape for registrar)

| Mount | Auth | What it can do |
|-------|------|----------------|
| `/api` | session | Incremental catalogues, `post-application`, app status, AT LC + AT-seva, `check-cred` |
| `/zeroday` | session | Lookup / mark attended / stats **by confirmation number only** |
| `/ivr` | session | Phone-keyed confirm/cancel/status |
| `/sms` | API key | Keyword router |
| `/mitra` | `mitra upakram` | Partner delta sync |

Missing from all of these: search, letter send, edit application, rooms/cells/seating, finalize, bulk mail, referrals, centre settings, course Editor.

---

### 7. Day-0 (physical course start)

Entry: `dh_manage_day_zero()` routes on URL.

Lists generated from `dh_applicant` + `dh_applicant_attended` + seating INI (`cs_seat_config`) + room INI (`dh_center_setting_acco`):

- Teacher list / manager list (optional confirmation number, cell batch)
- Cell list, laundry, valuables
- Hall seating + **group-wise seating** (`aa_group_seat_row/col`, `GROUP<n>-` keys)
- Day-0 arrival list (order by conf no; strip contacts option)
- Student chit / checking slip
- Day-11 report PDF

Mark attended: `dh_app_update_attended` and `/zeroday` `mark-attended-by-conf-num`.

Finalize (`finalize_course`): course end date must be past; attended → `Attended`; snapshot `dh_course_stat`; copy to `dh_student*`; move PDFs/photos; `c_finalized=1`. Reverse is CLI `unfinalized.php`.

---

### 8. Letters and outbound comms

`dh_letter` is per-centre, per-event templates (email subject/body + SMS). Merge via `dh_get_letter()`. Send via `dh_send_letter()`:

- Email: Mailgun (`mailgun_key`, centre from-name / reply-to)
- WhatsApp: 360Dialog templates + optional PDF
- SMS: Textlocal / Bhash / Rudra / SA aggregator
- `mode_test=1` reroutes email to `mode_test_emails`

Bulk mail: search builds a WHERE clause → `dh_bulk_mail` row → `cron-bulk-mail.php` → RabbitMQ.

Inbound: Mailgun webhooks set `Errors` on bounce; WhatsApp `/wa-hook` → RabbitMQ `whatsapp` queue → same keywords as SMS.

---

### 9. AT portal vs registrar AT schedule

Two different jobs:

| | Registrar (`at-schedule.inc`) | AT (`dh_atportal` + `/api`) |
|--|-------------------------------|-----------------------------|
| Who | Centre staff | The teacher |
| Job | Confirm/cancel ATs, type, group, quotas | Apply for seva, review LC apps, edit own profile |
| Quotas | `c_at_m_count` / `c_at_f_count` vs confirmed | Self-apply + status view |
| LC | Sees Reco/Area columns | Reco AT / Area AT review forms |

Address book (`address-book.inc`) is a large AT directory: CT/ACT centres, CAT/ACAT areas, roles, deceased, search.

---

### 10. Tables that matter (46 used in custom SQL)

**Core loop:** `dh_applicant`, `dh_applicant_course`, `dh_applicant_extra`, `dh_applicant_lc`, `dh_applicant_attended`, `dh_applicant_clarification`

**Org:** `dh_center`, `dh_center_setting`, `dh_center_setting_acco`, `dh_user_center`, `dh_course`, `dh_course_teacher`, `dh_course_stat`, `dh_center_course_template`, `dh_lc_admin`

**People after finalize:** `dh_student`, `dh_student_course`, `dh_student_course_input`

**Teachers:** `dh_teacher`, `dh_teacher_role`, `dh_teacher_center`, `dh_teacher_area`, `dh_teacher_area_cluster`, `dh_teacher_log`

**Comms:** `dh_letter`, `dh_letter_fields`, `dh_bulk_mail`, `dh_bulk_mail_log`, `dh_bulk_mail_unsubscribe`, `dh_center_sms`, `dh_sms_log`, `dh_ivr_log`

**Referral:** `dh_referral`, `dh_referral_log`

**Geo:** `dh_country`, `dh_state`, `dh_city`, `dh_pin_code`, `dh_languages`

**Config/audit:** `dh_type_detail`, `dh_log`, `dh_json`

**Patrika:** `dh_patrika`, `dh_patrika_payment`, `dh_patrika_note`, `dh_patrika_log`

---

### 11. Permissions a staff app must honor

From `dh_manageapp_permission()` (subset):

`access manageapp`, `access centre`, `access course`, `manage course`, `add/edit/delete application`, `change status`, `transfer course`, `access zero day`, `access male`, `access female`, `manage letters`, `mass mail`, `at scheduling`, `view/manage/delete referral list`, `r-at review`, `a-at review`, `export data`, `view teachers list`, `view sms/course report`, `access application pdf/photos`, `access center dashboard`, `access all centres`

AT portal: `access at portal`, `access at profile`, `access at address book`, `add assistant teacher`, `at portal superadmin`, `approve applications`.

A native client **cannot** flatten this to “logged-in user sees all applicants”.

---

### 12. Integrations

| System | Where | Notes |
|--------|-------|--------|
| Mailgun | `letters.inc` | Transactional + webhooks |
| 360Dialog | `dipi_api.module` | Templates, WAID check, PDF upload |
| AWS S3 | `dana-s3.inc` | Photos + PDFs; creds in `/dhamma/htpasswd/s3-env.ini` |
| RabbitMQ | bulk mail + WhatsApp | `mq_*` Drupal variables |
| pdftk / pdfcpu | `pdf.inc` | Form-fill application + D11 |
| SMS vendors | `send_sms*` | Country-routed |

Test switch: `variable_get('mode_test')`.

---

### 13. Graphify findings (what the graph is good for)

God nodes (highest degree): `_api_handle_sms`, `handle_whatsapp_msg`, `update_status_external`, `_update_status`, `send_sms`, `logit`.

That matches the source: **status change + outbound message** is the system spine. SMS/WhatsApp look more “connected” than search/zero-day because those live in `.inc` files that PHP AST barely cross-links (Drupal `include` is not an import). Isolated communities for `search.inc`, `zero-day.inc`, `course.inc` are an extractor limit, not a modular design.

True architectural fact the graph still shows: **`update_status_external` is the bridge** between AT portal review, registrar `_change_status`, and API `_process_lc_application`.

Health: 4 dangling-endpoint edges after merge; graph is usable.

---

### 14. Implications for an Android registrar client

1. Do not wrap the Drupal pages. They are DataTables Editor + giant forms.
2. Do not pretend `/api` is the registrar API. It is apply + AT + catalogues.
3. The first native vertical that pays off is **today’s course**: incoming Received, status change + letter, search, mark attended. That is `_change_status` + search + day-0 attended.
4. Next: day-0 lists and seating (local, tablet, offline-tolerant).
5. Last: letters admin, bulk mail, finalize, centre settings (desktop is fine).
6. Auth should be Drupal staff session (or a new token issued for that user), **not** the APP API service account (that account can already IDOR `get-app-detail`).
7. Treat medical + ID fields as NPI. No local photo cache without encryption. No logging of letter bodies.


---

## Part 4 — Sheet routes and markup skeletons (HAR sweep 2026-09-02)

Derived from a browser trace of the live desk during a **zero-day activity sweep**
for a course starting that day, captured 2026-09-02, 178 entries. Absorbed here
from the retired `version-5/HAR-ROUTES.md` handover when that folder was folded
into the repo docs (2026-09-05).

**What was removed, deliberately:** the HAR itself, every request and response
header (so no `Cookie`, no `Set-Cookie`, no CSRF or `form_token`), every request
body, and every response body. No student row, name, phone, email, Aadhaar / PAN /
passport / voter ID, emergency contact or health disclosure appears here. What
survives is method, path, query parameter *names*, response status, content type,
body size and hit count — plus the CSS class and table-header skeletons of the
sheet markup, which are server template structure, not data. The source HAR is not
in git and never will be (AGENTS.md hard rule 10).

Centre `63`, course `66884` in the capture. Read them as `{cid}` and `{courseId}`.


### Desk pages the app already consumes

| Method | Path | Status | Content-Type | Size | Feeds |
|---|---|---|---|---|---|
| GET | `/zero-day/{cid}/{courseId}` | 200 | `text/html` | 67.6 KB | **Day 0 summary** (`#day-summary` fragment) and **room merge** (`#table-attending`) |
| GET | `/day0-list/{cid}/{courseId}` | 200 | `text/html` | 55.0 KB | Day 0 list |
| GET | `/teacher-list/{cid}/{courseId}` | 200 | `text/html` | 13.9 KB | Teacher list |
| GET | `/manager-list/{cid}/{courseId}` | 200 | `text/html` | 8.8 KB | Manager list |
| GET | `/student-chit/{cid}/{courseId}` | 200 | `text/html` | 4.0 KB | Student chit |
| GET | `/seating/{cid}/{courseId}` | 200 | `text/html` | 33.8 KB | Seating plan |
| GET | `/laundry-list/{cid}/{courseId}` | 200 | `application/vnd.ms-excel` | streamed | Laundry list → `cacheDir/sheets` |
| GET | `/valuable-list/{cid}/{courseId}` | 200 | `application/vnd.ms-excel` | streamed | Valuable list → `cacheDir/sheets` |
| GET | `/course/{cid}/{courseId}` | 200 | `text/html` | 20.9 KB | Course page — the desk's own hub; source of the per-course `Course Summary` matrix |

Not exercised in this capture but wired in the app and unchanged: `GET /checking-slip/{cid}/{courseId}` (HTML), `GET /course-pdf-m|course-pdf-f/{cid}/{courseId}` (PDF), `GET /report-day11/{cid}/{courseId}` (PDF), `GET /centre/{cid}/acco-handler` (room config, read-only), `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` (worklist `var dataset`), `GET /change-status/{id}?s=&l=0&c=` (the one status write).

### Routes present on the desk but **not** used by the app

| Method | Path | Status | Content-Type | Size | Note |
|---|---|---|---|---|---|
| GET | `/group-seating/{cid}/{courseId}` | 200 | `text/html` | 26.9 KB | Group-wise seating. Same markup family as `/seating`. Not wired into the app. Same markup family as `/seating`; do not add a chip for it |
| GET | `/cell-list/{cid}/{courseId}` | — | — | — | Linked from the course page only. Out of scope |

### Query parameters

| Param | Seen on | Verdict |
|---|---|---|
| `?conf=1` | `day0-list` | **Safe.** Sorts by confirmation number instead of name. Use it for the native sort control |
| `?seating=1` | `teacher-list`, `student-chit`, `course-pdf-m`, `course-pdf-f` | **Safe.** Orders rows/chits by seating plan. Use it for the native order control |
| `?r=1` | `seating`, `group-seating`, `cell-list` — the desk's own "Re-Gen" links | **FORBIDDEN.** Presence triggers server-side bulk seat auto-allocation. Never send it, never draw a control that implies it |
| `?tkp923`, `?v=…` | static CSS/JS | Drupal cache-busters, irrelevant |

### Sheet markup skeletons

Server template structure only — no cell values.

#### `day0-list` → `day0-list.css`

`.header-day0` (`.title`, `.title-head`) · `.day0-toolbar.no-print` → `.grp` × 3: back link + `Print` (`onclick="window.print()"`), `Sort: Name | Confirmation no.` (the second is `?conf=1`), `Columns:` three `<button class="col-toggle" data-toggle-col="occ|contact|comments">` · then four `.day0-block` groups, each `.day0-scroll > table.table-day0-list` with `<colgroup>` classes `c-sr d0-sr`, `c-conf d0-conf`, `c-student d0-student`, `c-courses d0-courses`, `c-edu d0-edu`, `c-age d0-age`, `c-city d0-city`, `c-occ d0-occ`, `c-contact d0-contact`, `c-comments d0-comments`.

Group headers, in order: `Male|Old students|n total`, `Male|New students|n total`, `Female|Old students|n total`, `Female|New students|n total` (the `|` are `.sep` spans, the count a `.cnt` span). Column headers: `Sr · Conf No · Student · Crs · Education · Age · City · Occupation · Contact Details · Recom. / Comments`. `.day0-break` marks the print page break.

#### `teacher-list` → `teacher-list-v2.css`

`.header-teacher` · `.tl-toolbar.no-print` → back + `Print`, `Order: Seniority | Seating plan` (`?seating=1`), `Columns:` `col-toggle` for `cell|langs|comments` · `.tl-block > .tl-scroll > table.table-teacher-list`, colgroup `tl-sn tl-student tl-room tl-age tl-city tl-courses tl-cell tl-seat tl-occ tl-edu tl-langs tl-comments`. Group band is a `<th class="tl-groupinfo" colspan>` reading `AT: {teacher} [{code}] | {gender} | {cohort} | Group n | n total`. Headers: `S/N · Student · Room · Age · City · Courses · Cell · Seat · Occupation · Education · Languages · Comments`.

#### `manager-list` → `manager-list-v2.css`

`.header-manager` · `.ml-toolbar.no-print` → back + `Print`, `Columns:` one `col-toggle` for `cell` · `table.table-manager-list`, colgroup `ml-sn ml-student ml-age ml-room ml-ename ml-enum ml-cell ml-seat ml-conf ml-set`. Two stacked `<th class="ml-groupinfo">` bands: `{gender} | n total`, then `.ml-teachers` (`.lbl` "Teachers" + names with `.code`). Headers: `S/N · Student · Age · Room · Emergency Contact · Emergency No. · Cell · Seat · Conf No · Set`.

#### `student-chit` → `student-chit.css`

`.header-day0` (`.title` "Student Chit", `.title-head` course line) · two dead buttons `.no-print.remove-seat` / `.no-print.remove-cell` · a `.no-print` link to `?seating=1` · `.main-div` → repeated `.table-student-chit`, each containing exactly four divs: `.seat` (`"Seat: E4"` / `"Chowky: CW-A1"` / `"Seat: "` when unassigned), `.name` (given `<br>` family), `.cell` (`"Cell: "` — the mobile column, empty at this centre), `.room` (`"Room No.:Mbk-27"`, no space after the colon). The `.cell` + `.room` adjacency is the `Cell: Room No.:` redundancy the owner flagged.

#### `checking-slip`

Same `.header-day0` family (title still reads "Student Chit" on the live page). Per-student card: `{name} ({seat})`, room, empty `Cell:`, then the fixed bilingual meet-the-teacher note (English then Hindi) with two hand-filled blanks — a time and a location. A `.no-print` form offers `English-Text:` / `Hindi-Text:` + `Replace empty location with provided text`; it is JavaScript and dead in our viewer.

#### `seating` and `group-seating` → `seating.css`

`.helptext` (the large instruction panel) · `.no-print` buttons `Print Seating Plan`, `.store-seat-changes` · `.seat-legend` (`.legend-bar`, `.legend-cell`, `.legend-swatch`, `.legend-note`) · `.plan-header` / `.plan-header-title` (`Seating Plan - MALE` / `- FEMALE` + centre/type/year/dates) · `.sortable-m` / `.sortable-f` grids on `.bg-table-m` / `.bg-table-f`, cells carrying `.s-seat-no`, `.s-name`, `.s-age`, `.s-acco`, `.s-cell`, `.s-app-id`, `.s-old-student`, `.s-backrest` / `.s-backrest-yes`, `.teacher-seat`, `.chowky-div`, `.cwch` · per-column `.dh-add-col`, `.dh-blank-col`, `.dh-del-col`, `.add-row` · `.dh-page-sep` between the male and female pages. Every cell carries a jQuery-UI `.ui-state-default` drag handle. **All of the editing affordances require JavaScript, which the app's WebView has disabled.**

#### `zero-day` → `#day-summary`

Three tables inside the fragment:

| Table | Columns | Rows |
|---|---|---|
| `#table-conf` | (label) · `Old` · `New` · `Total` · `Server` | `Confirmed Male`, `Confirmed Female`, `Total` |
| `#table-totals` | (label) · `Old` · `New` · `Total` · `Server` | `Attended Male`, `Attended Female`, `Total` |
| `#table-special` | (label) · `Chowky` · `Chair` · `Backrest` | `Male`, `Female`, `Total` — cells are strings like `1 (O) + 1 (N)` |

The fragment arrives with **no stylesheet**, which is why the in-app view is browser default. `Total` cells rely on an unclosed `<b>` tag.

Also on the same page and already consumed by the app: `#table-attending`, whose columns are `ConfNo · Name · Gender · Type · Age · Teen/10D/STP · LC · RoomNo · Laundry · Valuable · Chowky · Chair · BackRest · Group · H`. The app takes `a_id` plus room/seat/laundry/valuable/group only — never names, never the hidden comment column. The row's update action posts to `/app-update-attended/{id}` (the existing, separately-authorised allocation sync).

#### `course/{cid}/{courseId}` → `.summary-block`

`<h2>Course Summary</h2>` then `.summary-block` → `.table-heading` (course identity, linked) → a table with headers `(label) · NM · OM · Total · SM · (spacer) · NF · OF · Total · SF`. Row labels are statuses (`Received`, `Confirmed`, `Cancelled`, `Clarification-Response`, `ReConfirmation`, `Expected`, …), each cell an anchor drilling into `/search-course/{cid}/{courseId}?s={status}&t={0|1}&g={M|F}&at={a|s}`. This is the same matrix the centre dashboard already paints natively — useful as the idiom for the new Course report table, not as a new fetch.

### Course report transport (already implemented)

`GET /centre/{cid}/course-report` returns a Drupal form, `dh_center_course_report_form`. The app scrapes `form_build_id`, `form_token` and the two date fields, then POSTs to the form's own action with `op = "Download Course Report"`; the reply streams `text/csv`.

Form fields: `report_from_date[date]`, `report_to_date[date]` (defaults last year → today), `form_build_id`, `form_token`, `form_id`, `op`.

CSV columns: `Course, NewMale, NewFemale, NewTotal, OldMale, OldFemale, OldTotal, StudentTotal, SevakMale, SevakFemale, SevakTotal, ConductingTeachers, AssistingTeachers, TrainingTeachers` — one row per course in range plus a grand-total row. `Course` is a single string (`{centre} / {type} / {year} / {dates}`); teacher names wrap onto continuation lines, so a parser must not assume one physical line per record.

---

## How to refresh this map

```bash
cd dipi-web
graphify update .     # incremental AST; keep graphify-out/cache
open graphify-out/graph.html
```

`.graphifyignore` keeps Drupal core/contrib out. Re-stage `*.module` → `graphify-out/_modules/*.php` if you rebuild from scratch (Graphify does not treat `.module` as PHP).

