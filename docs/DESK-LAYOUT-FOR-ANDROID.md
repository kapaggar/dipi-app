# DIPI registrar desk — local PHP inventory (Android design input)

**Scope (observed):** read-only scan of `/Users/wizops/DIPI/dipi-web/sites/all/modules/dh_manageapp`. No website login. No PHP edits. No credentials, cookies, tokens, or student values.

**Android v1 baseline (observed from local app docs, not PHP):** `dipi-app` Vertical 1 **1.4.1** on `feat/vertical-1`. Screens: Login, Courses, Today worklist, Applicant card, Change-status sheet, Day 0 summary (read-only stub seating), Photo review (mock / no live upload), Settings.

**How to read this file**
- **Fact** = quoted from PHP (`dh_manageapp_menu`, callbacks, `$rs` keys, HTML regions).
- **Guess** = inference for Android design, not proven by PHP.
- Login wall is omitted (Drupal core `user/login`, not in this module’s `hook_menu`).
- `templates/tpl/js` does **not** exist. Desk JS lives in `js/` (`manageapp.js`, `location-common.js`, select2, jquery-confirm) plus large **inline** scripts in `search.inc`, `zero-day.inc`, `at-schedule.inc`, `referral.inc`. `tpl/` only has `center-dashboard-page.tpl.php` and `vri-management-page.tpl.php` (teacher/VRI dashboards, not the registrar desk).

---

## 1. Hierarchical map — authenticated desk pages a centre registrar uses

Source: `dh_manageapp_menu()` in `dh_manageapp.module` (lines 96–692). Access is Drupal permission strings in `access arguments` unless noted.

**Entry (fact):** `dh_manageapp_home()` on `home` (`access content`) redirects: `access centre` → `centre`; else `access at portal` → `at-portal`. A single-centre user in `dh_zero_select_centre()` is auto-sent to `centre/{cid}`.

**Not registrar desk (listed so they are not mistaken for v1):** `admin/dh_manageapp` (site admin), `user-mapping` / `users`, `center-dashboard` / `center-dashboard-page` / `vri-management` (teacher/VRI), `search-lc` (AT LC review; **menu key registered twice**, second wins with `a-at review`), `at-portal/referral/add`, `webhook/*`, `wa-hook*`.

### 1.1 Centre

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

### 1.2 Course

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

### 1.3 Applicant / worklist actions

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

### 1.4 Letters

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **Letter list** | DataTable + deleted restore | `letters/%centre_id` | `dh_manage_letters` → `dh_letters_listing` | `manage letters` |
| Add / edit / copy | Template form | `letters/%centre_id/{add,edit,copy}/%letter_id` | `dh_manage_letters` → `dh_letters_form` | `manage letters` |
| Delete / restore / del attach | Soft-delete writes | `letters/%centre_id/{delete,restore,delattach}/%letter_id` | `dh_manage_letters` | `manage letters` |
| Letter fields | Custom merge fields editor | `letter-fields/%centre_id` | `dh_manage_letter_fields` | `manage letters` |
| Letter fields AJAX | Editor POST | `letter-fields/handler/%centre_id` | `dh_manage_letter_fields_handler` | `manage letters` |

### 1.5 AT scheduling

| Page | Main actions | Route | PHP callback | Access argument |
|---|---|---|---|---|
| **AT schedule** | Recent Received + upcoming/completed course tables | `at-schedule/%centre_id` | `dh_manage_at_schedule` | `at scheduling` |
| Assign teacher (per course) | Add AT + change status/type/group | `assign-teacher/%centre_id/%course_id` | `dh_assign_teacher` | `at scheduling` |
| AT status / type / group | AJAX | `at-schedule/change-{status,type,group}/%atappid` | `at_app_change` | `at scheduling` |
| Delete trainee | Write | `at-schedule/del-trainee-teacher/%centre_id/%course_id/%atappid` | `del_trainee_teacher` | `at scheduling` |
| AT info popup | Read | `at-schedule/get-at-info/%` | `get_at_info` | `access at details` |
| AT activity | Child HTML | `at-app-activity/%atappid` | `_get_at_app_activity` | `at scheduling` |
| Trainee autocomplete | Lookup | `autocomplete/get-trainee-teacher` | `_get_trainee_teacher` | `_access_teacher` (`access zero day` **or** `at scheduling`) |

### 1.6 Referral

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

### 1.7 Autocomplete / system (desk-adjacent)

`autocomplete/get-{country,state,city}`, `get-location-from-pincode` — `access zero day`. `autocomplete/get-teacher` — `_access_teacher`. Used by applicant/referral/centre forms, not standalone pages.

---

## 2. Registrar-critical pages — regions, filters/tables, read vs write

### 2.1 Centre dashboard — `centre/%centre_id` — `dh_manage_centre` + `dashboard()`

**Regions (fact)**
1. Red `important_notice` variable (site-wide banner).
2. **3-column link list** (`ul.multi-column`) from `$modules` (permission-gated via `drupal_valid_path`).
3. **Course picker** — `dh_zero_select_course($with_header=false)`: Select2 of courses with `c_start >= max_old_courses` (default 6 months). Change → `location.href = /course/{cid}/{courseId}`.
4. **Received/Response Applications** — only if `received applications block`. Table: Course, New/Old/Total Male, spacer, New/Old/Total Female. Counts = `Received` + `ClarificationResponse` (keys from `COURSE-SYSTEM-STATUS`). Each cell links to `search-course/{cid}/{courseId}?s=Received,ClarificationResponse&t=&g=` (and `t=0|1`, `g=M|F`).
5. **Upcoming Courses** — only if `upcoming courses block`. Next **4** un-deleted future courses (`c_start >= today`). Renders `course_summary()` blocks.

**Filters:** none on the page itself (picker + count links).

**Read vs write:** **read**. Writes live on linked pages (settings, course editor, letters, …).

### 2.2 Course dashboard — `course/%centre_id/%course_id` — `dh_course_dashboard`

**Regions (fact)**
1. Course name + **Edit** → `manage-course/{cid}?c={courseId}` (opens editor on that row).
2. Back to Dashboard.
3. **3-column action links** (`$modules` above). View Applications always shown (not `drupal_valid_path`-gated). Others gated.
4. **Course Summary** — `course_summary($cid, "where c_id=…")`. Per-status rows: NM, OM, Total, SM, spacer, NF, OF, Total, SF. Status list = `COURSE-SYSTEM-STATUS` + `COURSE-STATUS`; LC courses add `COURSE-LC-STATUS` if user has `r-at review` or `a-at review`. Cells link to `search-course` with `s`, `t` (0 new / 1 old), `g`, `at=a|s`.

**Read vs write:** **read** hub. Edit course is write on another route.

### 2.3 Search-course worklist — `search-course/%centre_id/%course_id` — `_search_course`

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

### 2.4 Change-status — `change-status/%app_id` — `_change_status`

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

### 2.5 Day 0 / Zero Day — `zero-day/%centre_id/%course_id` — `dh_zero_main`

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

### 2.6 Seating — `seating/%centre_id/%course_id` — `dh_generate_seating_plan`

**Regions (fact):** standalone HTML (not Drupal chrome). Reads `cs_seat_config` INI + `cs_seat_naming_conv`. LEFT/RIGHT hall grids; seat cells show acco, name, old/new course counts, age, backrest, chowky/chair, cell. Combined seating if `c_combined_seat_course > 0`. `?r=1` → `dh_auto_allocate_seats` then render. Drag-drop save → `seating-update`. Invalid INI → error page pointing at Centre Settings.

**Group-wise seating:** separate grid (`dh_generate_group_seating_plan`); does not rewrite main hall seats.

**Seating2:** alternate drag UI in `seating.inc` (`dh_generate_seating_plan2`); help text + Print. **Guess:** less used; course dashboard links `seating` not `seating2`.

**Read vs write:** **read** by default; **write** on regen (`?r=1`) and drag-save.

### 2.7 Letters — `letters/%centre_id` — `dh_letters_listing` / `dh_letters_form`

**List regions:** Add Letter | Manage Fields | Back to Dashboard. DataTable: Letter Name, Status (event), Course Type, Subject, Edit, Copy, Delete. Second table: Deleted Letters + Restore.

**Form fields (fact):** Letter Name, Status (`l_event` = system + custom + LC statuses), Course Type (or All), Subject, Body (filtered_html), SMS Text, Attachment. Collapsed merge-field cheat sheet from `_get_letter_fields`.

**Read vs write:** **write** (CRUD templates). Sending happens inside `_change_status` / crons / bulk-mail, not this page.

### 2.8 AT — `at-schedule/%centre_id` + `assign-teacher/%centre_id/%course_id`

**AT schedule regions (fact)**
1. Back to Dashboard.
2. **Recent Received AT Applications** — DataTable: Detail, Course-Name, AT-Name, Gender, Current-Status, Change-Status (Received → Confirmed/Cancelled). Expand loads `/at-app-activity/{ct_id}`. Status change confirm → `at-schedule/change-status/{ct_id}`.
3. **Upcoming Courses** — course link, AT required M|F, nested teacher table (name, type+gender, status).
4. **Completed Courses** — year select, same columns, Confirmed-only teachers.

**Assign teacher regions (fact):** Back to Course | Back to AT Schedule or Zero Day. Required M/F counts. Add-teacher form (`dh_assign_teacher_form`). Teacher Applications table: status/type/group selects (`Confirmed`/`Cancelled`, `Conducting`/`Assisting`). Separate trainee-teacher form. Finalized course blocked.

**Read vs write:** **write** (AT application status/type/group).

### 2.9 Referral — `referral/%centre_id` and `center-referral/%centre_id` — `dh_referral_results`

**Regions (fact):** Back to Dashboard | Add Referral (or Add Center Referral). DataTable `#table-applicants`: Detail, Applicant Name, Gender, Age, Email (`contact` HTML), Referred By, Start Date, End Date, Delete (+ r-only checkbox on AT list). Expand: address, contact, **Pancard / Aadhar / National ID / Passport**, occ/edu/company, listed-for grid (10d…longseva), reason, referring AT/centre, course counts, referral-course, activity AJAX.

**Filter:** `r_center=0` (AT list) vs `r_center={cid}` (centre list); `r_deleted=0`.

**Form (`dh_referral_form`):** Personal Information (name, gender, DOB, phones, email, education, occupation, …) plus listing flags/dates/reason (long form ~690 lines). NPI fields exist on the student/referral record (`s_aadhar`, `s_passport`, `s_voter_id`, `s_pancard`).

**Read vs write:** list is **read + delete/readonly**; add/edit is **write**.

---

## 3. Dataset / card field keys — `dh_manageapp_search_results` `$rs`

Built in `inc/search.inc` (~278–521). Encoded as `var dataset`. **Names only. No sample values.**

Applicant SQL also selects `a_passport`, `a_voter_id`, `a_pancard`, `a_aadhar` and maps them onto `$rs`. Student SQL path does **not** select those four columns (those `$rs` keys would be empty/undefined on `d=s`).

### 3.1 Public / operational keys (always set on applicant path)

`allowtransfer`, `type`, `gender`, `o_n`, `courseid`, `centreid`, `finalized`, `aid`, `changestatus`, `name`, `course`, `status`, `app_status`, `confno`, `location`, `city`, `state`, `country`, `pin`, `dob`, `age`, `address`, `contact`, `contact_home`, `contact_mobile`, `contact_office`, `contact_email`, `occupation`, `designation`, `company`, `emergency_name`, `emergency_relation`, `emergency_num`, `lang_discourse`, `friend_family`, `note`, `willingtoserve`, `extra`, `monk`, `eocd`, `course_teen`, `course_10d`, `course_stp`, `course_spl`, `course_20d`, `course_30d`, `course_45d`, `course_60d`, `course_tsc`, `course_seva`, `left_reason`, `section`, `acc`, `first_course`, `last_course`, `course_others`, `practice_details` (old students only), `Education`, `Company`, `Dept`, `Occ`, `Designation`, `lang`, `alist`, `photo`, `long_course`, `reco_t`, `reco_status`, `area_t`, `area_status`, `app_created`, `referral`

### 3.2 Medical / extra keys (not the four NPI IDs; still sensitive)

`physical`, `mental`, `medication`, `addiction`, `othertechnique`, `pregnant`, `id_issued_date`, `id_issued_by`, `nationality`

These are mapped from `ae_*` / `aa_*` columns. Android docs treat `ae_*` as NPI-adjacent and must not persist.

### 3.3 NPI keys (do not persist / log)

| `$rs` key | Source column (applicant query) |
|---|---|
| `aadhar` | `a_aadhar` |
| `passport` | `a_passport` |
| `voterid` | `a_voter_id` |
| `pancard` | `a_pancard` |

Excel export column 18 = ID type label (Passport/Voter ID/Pan card/Aadhar); column 19 = the raw ID number(s).

### 3.4 Conditional referral keys (only if `a_referral` matches an in-date listing)

`ref_reason`, `ref_start`, `ref_end`, `ref_listed_by`, `ref_listed_title`, `special_list`, `ref_listed_for`

If listing expired or course-level flags do not match, `referral` is forced back to `0`.

### 3.5 What the desk card actually shows vs `$rs`

**Collapsed row (fact):** name (edit link + PDF + Sevak/AT suffix), `eocd` (edu/occ + course + Phy/Mental/Intox/Oth-Med/Preg/A-List/Monk/Referral chips), `status` (+ conf no), `o_n`, `age`, change-status widget, action widget.

**Expanded card (fact):** address, phones, email, course counts, first/last, practice, photo, medical, note, extra, discourse lang, pregnant, friend-family, referral block, LC teachers, plus AJAX clarifications / other courses / activity.

**Guess:** Android public card is a **subset** of `$rs` (name, status, confno, age, gender, monk, location, phones, email, dob, `app_created`, old-student history, photo URL). Desk card is richer and includes medical + NPI-in-export + street address.

---

## 4. Gap table vs existing Android Vertical 1

Legend: **have** = live desk path already used; **stub** = screen exists but not live/complete; **out of scope** = not in v1 product (per app docs).

### 4.1 Android screens → desk

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

### 4.2 Desk page / action → Android v1

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

## 5. Open design questions (do not pick)

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

## Source index (fact)

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
| `dipi-app/AGENTS.md`, `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` | Android v1 screen list for the gap table only |

**Menu quirk (fact):** `$items['search-lc']` is assigned twice in `dh_manageapp_menu()`; the second (`a-at review`) overwrites the first (`r-at review`).
