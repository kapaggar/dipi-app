# TODO(server) — mocked `/staff` routes

Every row is served by the debug mock until PHP lands. Status writes already exist.

| Method | Path | PHP to implement |
|---|---|---|
| GET | `/staff/session` | `users` + `dh_user_center` + `variable_get('mode_test')` |
| GET | `/staff/centres/{cid}/courses?upcoming=1` | `dh_course` where `c_finalized=0` and `c_deleted=0` |
| GET | `/staff/courses/{id}/applicants` | `search.inc` WHERE + **whitelist SELECT** (no `ae_*`, no national IDs) |
| GET | `/staff/applicants/{id}` | public `dh_applicant` + `dh_applicant_course` history + server flags |
| GET | `/staff/meta/statuses` | `dh_type_detail` COURSE-SYSTEM-STATUS / COURSE-STATUS |
| GET | `/staff/applicants/{id}/photo` | `a_photo` S3 stream |
| GET | `/staff/courses/{id}/photo-review` | new suggestion classes |
| POST | `/staff/applicants/{id}/photo` | resubmit application with swapped photo; return drift |

**Existing (do not wrap):**

| Method | Path | PHP |
|---|---|---|
| POST | `/api/user/login` | Services |
| GET | `/services/session/token` | Services |
| GET/POST | `/change-status/{id}?s=&l=&c=` | `_change_status` |

**Do not implement in v1:** `POST /staff/applicants/{id}/attended`, `/app-update-attended` from this app.
