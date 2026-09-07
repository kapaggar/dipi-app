# Login sequencing and finalized Room Chart

Owner request: fix the first-attempt native sign-in error; recover finalized-course rooms from the existing worklist; make historical allocation read-only; rename Rooms & seats to Room Chart and show age and old/new.

## Investigation

- `DeskViewModel` started restoration in an untracked coroutine. Manual sign-in could run concurrently, and duplicate taps had no function-level guard. An expired restoration calls `sessionExpired`, clearing the same cookie jar a new login uses.
- Restoration also forwarded its already-handled auth failure to `handleAuth`, launching another asynchronous cookie cleanup. Login enabled the form again before loading the centre's courses.
- A controlled delayed-403 regression reproduced two login POSTs while restoration remained unresolved. This demonstrates the race; the user's original transient error was not captured on the tablet.
- Backend `dh_manageapp/inc/search.inc` selects `c_finalized`, joins `dh_applicant_attended`, and emits `finalized`, `section`, and `acc` in `var dataset`. Attended and Left filters use the same dataset as the existing unfiltered `search-course` route. No extra endpoints or PHP changes are needed.
- Zero Day deliberately redirects finalized courses. Its missing table is not a reason to erase historical rooms or infer that the staff session expired.

## Implementation

- Manual sign-in waits for startup restoration and pending auth cleanup; duplicate taps share one attempt. Keep loading through centre discovery. An expired saved session returns to sign-in without reporting a manual login failure.
- Preserve server `finalized` and room section/number in the existing applicant cache. Missing/NA room parts remain empty.
- Finalized Attended records derive room occupancy from that snapshot, overriding stale local allocations. Left records never occupy a room. Other finalized statuses never occupy a room.
- Finalized Check-in is read-only; editing handlers and room-sync dispatch are also guarded. Automatic room pulls skip Zero Day; manual Pull refreshes the worklist. Current-course sync excludes other courses and Left applicants.
- Room Chart shows name, age, OLD/NEW text cues and all occupants of a shared room. Historical rooms absent from today's room inventory are included. Finalized counts say assigned/unassigned; the page says Read only.
- The finalized flag comes from server data, never inferred from course age. An empty worklist has no applicant allocation controls to edit.

## Validation

Synthetic regression tests cover delayed expired restoration, duplicate taps, successful sign-in, historical allocation overriding unsynced data, Left exclusion, missing room fields, disabled finalized writes/Zero Day retrieval, age/seniority cues, and rooms removed from current inventory. Existing room-layout and active-course tests remain in the full suite.

Release and device validation results are recorded below after execution. No applicant edits, attendance/status writes, or WhatsApp sends are part of validation.

### Release validation

- Full supported suite: **743 tests, zero failures/errors/skips**. Release APK built successfully.
- Pixel C installed **1.44.0 / versionCode 87** with data preserved. Native startup restored the session to the centre dashboard without an error.
- Live August 19-30 finalized course: server data activated Read only; female room block showed 16 historical assignments. Names, age and OLD/NEW cues displayed on occupied cells. Check-in showed Read only and explicit Left labels. No writes submitted.
- Tablet left on the historical Room Chart with occupied cells visible.
- The original transient login error was not reproduced live; its concurrency path was reproduced and fixed in the controlled delayed-403 test.
