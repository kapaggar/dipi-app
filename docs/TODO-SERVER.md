# Mock `/staff` routes — not live PHP

The live host `https://dipi.vridhamma.org` is **immutable**. Do **not** implement these routes in `dipi-web`. They exist only on MockWebServer when `-Pdipi.useMock=true`.

The shipping client talks to the existing desk instead (see `LIVE-DESK-HAR.md`).

| Method | Path | Used when |
|---|---|---|
| GET | `/staff/session` | mock session |
| GET | `/staff/centres/{cid}/courses?upcoming=1` | mock course list |
| GET | `/staff/courses/{id}/applicants` | mock worklist |
| GET | `/staff/applicants/{id}` | mock card |
| GET | `/staff/meta/statuses` | mock status chips |
| GET | `/staff/courses/{id}/photo-review` | mock photo review |
| POST | `/staff/applicants/{id}/photo` | mock photo upload |

**Existing on live (do not wrap, do not invent replacements):**

| Method | Path | PHP |
|---|---|---|
| GET | `/user/login`, `/`, `/centre`, `/centre/{cid}` | desk HTML |
| POST | `/user/login` or `/home?destination=home` | `user_login` / `user_login_block` |
| GET | `/search-course/{cid}/{courseId}` | `var dataset` |
| GET | `/change-status/{id}?s=&l=&c=` | `_change_status` |
| GET | `/services/session/token` | CSRF keep-alive (optional) |
| GET | `/user/logout` | logout |

**Do not implement in v1:** `POST /staff/applicants/{id}/attended`, `/app-update-attended` from this app. Photo upload is not exposed on the live desk.
