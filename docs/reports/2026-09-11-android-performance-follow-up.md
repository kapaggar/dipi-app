# Android performance follow-up - 2.0.0 / 98

Device work on 2026-09-11 UTC (2026-09-10 Pacific). This follows the
[1.46.2 baseline](2026-09-10-android-performance-baseline.md) and
[2.0.0 implementation record](../plans/2026-09-10-desk-performance.md).

## Installation

The owner explicitly authorized installation. GitHub latest was verified as
final v2.0.0. Installed the published-equivalent release APK on Pixel C using an
in-place `adb install -r`; package manager returned Success and reported
`org.dhamma.dipi.staff`, versionName 2.0.0, versionCode 98. No app data clear,
logout, uninstall or signing-key change was performed.

APK SHA-256: `c00681a9715ed0cd57fe0673ba77b0859c77d084f9096f56a412b84ad3b6be63`.
This matches both GitHub release asset digests. Photo review remains disabled.
The app opened at Sign-in with Remember me fields populated; submitting those
existing fields restored the centre dashboard. No credential values were read
into diagnostics. The previous authenticated session did not survive to this
run; the cause was not isolated. Do not attribute that to the update or claim a
long-pause session-survival pass.

The first launch overlapped installation and sleep/keyguard transitions and is
excluded from timing conclusions. Simulate offline was initially enabled and
was temporarily disabled for live measurements. Initial screen timeout was
600,000 ms; rotation settings were accelerometer 1/user 0. Battery was 14%,
discharging, at 28.8 C when settled centre measurements began. The battery-saver
setting returned null; no battery-mode change was made.

## Device results

CPU is cumulative process/Main CPU at 10 ms resolution;
UID network bytes do not prove HTTP request counts. These are release-build
observations, not method traces or endpoint timings. Wi-Fi ADB, battery state,
cache warmth and changing live data limit comparisons with the previous day.

| Path | Logical requests from source | Window ms | Process CPU ms | Main CPU ms | App RX/TX bytes |
|---|---|---:|---:|---:|---:|
| Remember me sign-in to centre | Login/form/session discovery plus centre/config; wire count unmeasured | 18,188 | 13,430 | 5,170 | 33,791 / 3,887 |
| Centre, screen on, untouched | No due fetch | 120,984 | 0 | 0 | 0 / 0 |
| Centre, screen off | No due fetch | 124,210 | 260 | 30 | 0 / 135 |
| First upcoming-course open after install | Worklist then Zero Day: 2 | 15,960 | 37,980 | 3,920 | 107,612 / 4,866 |
| Warm reopen of the same course | Worklist then Zero Day: 2 | 15,829 | 32,200 | 1,550 | 99,912 / 4,058 |
| Remaining paths | Captured after reconnect; see the separate table below | - | - | - | - |

The centre run totals 245,194 ms, with 260 ms process CPU and 30 ms Main CPU.
The final screen-off minute used 10 ms process CPU, zero Main CPU and zero app
bytes. The two transmitted packets earlier in screen-off do not identify an
HTTP request. The four-minute window does not test the 20-minute keep-alive.

The current live course has **210 applicants / 123 on the roll / 33 audit rows**;
the baseline had 208 / 118 / 31. The previous local check-in remained visible.
At the first post-install course observation, the header said synced and the
applicant count was populated while the Board still showed zero roll/audit
counts. A later observation showed the populated roll and audit. Exact time to
useful roll and Zero Day completion was not instrumented. This is evidence to
review freshness/loading presentation, not proof of data loss.

| Warm course comparison | Baseline 1.46.2 | Installed 2.0.0 |
|---|---:|---:|
| Observation window ms | 13,070 | 15,829 |
| Main CPU ms | 5,540 | 1,550 |
| Process CPU ms | 8,090 | 32,200 |
| Frames / janky frames | 6 / 5 | 211 / 211 |
| Gfxinfo p50 / p99 ms | 117 / 2,600 | 38 / 113 |

Main CPU and the observed frame-duration tail were lower, but **total CPU was
higher and all 211 recorded warm-run frames were classified janky**. This is not
a controlled before/after experiment: windows differ, live rows changed, and
the new APK's compilation/cache state and device power state differ. More
rendered loading frames after unblocking Main also change the frame sample.
There is no defensible overall speedup or active-battery-saving claim yet.
Profile audit/parse cancellation and loading/render work before deciding which
remaining computation to remove.

After the warm course run, Wi-Fi ADB changed to offline and reconnecting to the
documented address timed out. A thread-cost follow-up and sheet measurement
could not start; these failures are excluded from timing tables. This does not
establish an app crash. At disconnection the last confirmed state was the
populated course Board, Desk mode, **Simulate offline disabled**. The owner was
asked to charge the tablet and reconnect it to the same Wi-Fi, then explicitly
requested a retry when the tablet was back.

### Reconnected release run

The retry reconnected to the same Pixel C and confirmed 2.0.0 / 98 without
reinstalling. The tablet was AC powered at 30% and 32.6 C; the DIPI process was
not running. Launch again reached Sign-in with populated Remember me fields.
Submitting those existing fields restored the centre; no logout or app data
clear was performed. These observations do not isolate why session restoration
failed. Charging and the fresh process further limit cross-run comparisons.

The first Board sample after course entry still consumed 7,920 ms process CPU
over 5,567 ms, with zero Main CPU and zero UID bytes. A later 13,317 ms thread
sample used 70 ms process CPU, zero Main CPU and zero UID bytes. Only ART's
Profile Saver accumulated nonzero CPU in the separate per-thread samples
(80 ms). Process and thread intervals are not identical and tick rounding
prevents exact reconciliation. The early interval was therefore not a settled
idle baseline; the later interval does not show a sustained worker loop.

Runtime inspection found no DIPI alarm, registered job, active service, app
wakelock or bound accessibility service. The job scheduler's `u0a145: 40` entry
was a UID priority entry, not a scheduled job. Connectivity had one DIPI LISTEN
callback. None of the 32 thread names sampled at initial launch matched ML Kit,
Firebase, vision or transport-runtime; names alone are not a complete library
inspection, but the release DEX/dependency inspection also found ML Kit absent.
Another app, AccuBattery, held a charging wakelock, and SD Maid's accessibility
service was enabled. Whole-device power cannot be attributed to DIPI alone.

| Retry path | Logical requests from source | Window ms | Process CPU ms | Main CPU ms | App RX/TX bytes |
|---|---|---:|---:|---:|---:|
| First Day 0 list sheet in this process | 1 sheet GET plus WebView assets | 7,852 | 7,120 | 1,670 | 10,673 / 1,766 |
| Warm Day 0 list sheet | 1 sheet GET plus WebView assets | 7,718 | 5,340 | 1,210 | 11,587 / 2,593 |
| Compact pull-to-refresh | 1 worklist GET; no Zero Day merge | 11,270 | 14,610 | 1,780 | 84,946 / 4,896 |
| One deliberate Course ops entry | 1 teacher-list GET; optional worklist and missing cards, actual wire count unmeasured | 25,021 | 11,620 | 1,810 | 14,639 / 3,550 |
| Course ops, settled | No due fetch | 10,450 | 160 | 0 | 0 / 0 |
| Course ops group filter on/off, Seating plan, Teacher list | 0 | 12,274 | 4,840 | 1,860 | 0 / 0 |
| Home, untouched for 90 seconds | No due fetch | 90,568 | 560 | 60 | 0 / 0 |
| Return to the existing Board | 0 | 5,673 | 550 | 150 | 0 / 0 |

These are fixed observation windows including ADB input overhead, not request
durations or first-useful-pixel measurements. Sheet controls and the print action
were present on subsequent inspection. The process survived the entire
background/return trial; no UID traffic was observed in either window. This is
only a short-pause result, not a 20-minute keep-alive or long-pause test.

Gfxinfo for the first sheet recorded 75/75 janky frames, p50 46 ms and p99
4,950 ms; the warm sheet recorded 89/89, p50 46 ms and p99 300 ms. These counters
include the subsequent UI inspection before capture and do not have exactly the
same boundary as the CPU window. Baseline warm-sheet Main CPU was 440 ms and
p99 was 89 ms over a shorter observed opening. This retry does not demonstrate
a sheet speedup; WebView startup/assets and rendering need separate attribution.

Portrait reached the existing compact Applications screen. Its initial entry
was allowed to load before the measured pull gesture; rotation/entry work is
excluded from the refresh window. The list remained present afterwards. Source
inspection still gives one worklist request and no Zero Day merge for refresh;
the UID byte measurement does not independently count those requests.
Refresh gfxinfo recorded 266/266 janky frames, p50 21 ms and p99 61 ms. Main CPU
was lower than the earlier 3,570 ms sample, but the windows differ and the
14,610 ms process CPU leaves active-work savings unproven.

Exactly one Course ops entry was performed during the retry, with no repeated
teacher-list benchmark. Its 71-person roll was visible at the subsequent UI
inspection. Existing Room/encrypted card caches were left intact, so this must
not be interpreted as an empty-cache application-buffer benchmark. No applicant
cards were opened, no Comments column was inspected and no letter/WhatsApp
workflow was started. The entry window includes leaving Settings and 20 seconds
of waiting; it is not teacher-list latency.
Entry gfxinfo recorded 14/14 janky frames, p50 150 ms and p99 950 ms.
Toggling one group filter on/off and switching to Seating plan and back to
Teacher list used zero UID bytes across all four actions. That supports the
existing no-refetch behavior for these interactions; it does not prove all
possible navigation or lifecycle sequences.
Local-action gfxinfo recorded 33/33 janky frames, p50 44 ms and p99 900 ms.

### Restoration and validation

After the retry, the existing device PIN opened Settings successfully. Verified
Desk mode selected, Course ops unselected, **Simulate offline enabled**, automatic
rotation 1, user rotation 0 and screen timeout 600,000 ms. Returned to the centre
dashboard and put the screen to sleep; `dumpsys power` reported Asleep. Package
manager again confirmed 2.0.0 / 98. Local desk data and the PIN were preserved;
no second APK was installed during this retry.

This follow-up changes documentation only. Release validation remains the
recorded **853 passing tests**, including all three SheetRouteSafetyTest cases,
and the verified release APK. The suite was not rerun for these documentation
edits; documentation whitespace and measurement transcription were checked.
There is no new version bump or release artifact.

## Next implementation, in order

| Priority | Implementation | Evidence and acceptance criteria | Human decision |
|---|---|---|---|
| 1 - correctness before more caching | Scope the native hall roll, application-card publications and freshness by session/centre/course; retain actual or unknown fetch time. Clear or reject incompatible display state on course changes. | `deskOpenCourse` leaves `teacherRoll`; `openNativeBoardSeating` reuses it and stamps the current clock. Prefetch callbacks still publish cards/progress without a course check, and a late `saveCard` can switch the store's active course. Test A-to-B navigation during a held fetch: no wrong-course roll/cards, no misleading new timestamp, and no extra teacher-list request. | No new product policy is needed to stop wrong-course display and invented timestamps. Offline presentation beyond existing cached/unknown states needs a separate UX decision. |
| 2 - remove repeated requests | Coalesce only identical, concurrent worklist/centre requests, keyed by session, centre, course and server filter. Stop the redundant compact text-query GET where the live transport ignores `q`, retaining its existing local filtering. | `refresh()` launches on every call; `ensureWorklist` guards the observer but not each refresh launch. `onQuery` schedules a GET after 300 ms despite live `searchCourse` not using the text. Tests: rapid duplicate refresh taps cause one in-flight request; a later explicit refresh still fetches; different filters never share results; cancelled/stale results never publish. | No new transport decision needed for identical in-flight reads. Registrar acceptance should confirm refresh feedback and local search behavior. Keep explicit status-filter GETs and server vocabulary. |
| 3 - reduce total computation | Build course-wide duplicate/audit indexes once per immutable roll and reuse them; evaluate whether the two existing audit passes can share one equivalent result. Add cooperative cancellation between bounded CPU work units so obsolete mapLatest computation can stop promptly. | `ClientAudit.evaluate` filters course mates per row and runs twice in the observed-roll path. Moving it off Main did not remove the total work; the warm device run used more process CPU. Require synthetic flag-for-flag equivalence, including sensitive-memory checks, plus CPU/allocation attribution and device remeasurement. Measure loading/rendering work too; do not attribute all CPU to audits without a trace. | Engineering can prepare equivalence tests. Registrar validation of representative audit outcomes is useful before shipping; audit rules must not change. |
| 4 - speed centre presentation | Measure centre landing/dashboard/config stages; reuse already-fetched authenticated dashboard HTML where the same response is available. Consider publishing courses before independent room-config processing only after dependencies are explicit. | Current centre path awaits `acco-handler` before publishing courses and may fetch dashboard HTML after an authenticated landing already redirected there. Require redirect/login-form/403 tests and exact request timing. Do not speculate an additional GET. | If room configuration will still be loading when courses become usable, the owner must accept that desk interaction and its honest loading state. |
| 5 - stop abandoned Course ops work | Retain a course-entry job, reject stale card/progress writes, and design pause/resume for unfinished application-card buffering using the current teacher roll and existing cache. | Existing prefetch is bounded at four but can outlive screen/mode changes. A resumed buffer must never fetch teacher-list again. Partial/missing answers must remain visibly unknown rather than treated as checked. | Choose whether an interrupted entry should finish offline preparation or pause while backgrounded/screen-off. Eager full-roll buffering is an existing owner decision; changing it requires an explicit choice. |
| 6 - targeted secondary work | Measure image sampling/cancellation, WebView asset loads/disposal, cookie/token storage overhead and remaining Compose calculations. Optimize only measured costs. | Photos use a shared loader and 1024 px target rather than each view's bounds; leaving a cell does not necessarily cancel a shared transfer. WebView filters may reload assets. No measured whole-course photo storm or idle radio problem justifies broad changes. | Usually engineering-only once measured. No whole-course photo prefetch, ML Kit activation or image/product redesign. |

`ApplicantDao` still upserts returned rows without a course-level replacement,
and lastSync remains global. Before changing retention, establish whether each
unfiltered response is a complete authoritative roll and how local check-ins and
pending work must survive removal from that response. Never delete local desk
work as a cache optimization. This requires an explicit retention decision.

## Human input that is actually needed

**Device dependency resolved:** the owner reconnected Pixel C and the interrupted
release measurements resumed. Installation itself was already complete. Exact
endpoint timing and CPU method attribution remain separate profiling work.

1. **Profiling build and test window:** authorize a separate debuggable diagnostic
   APK if exact Retrofit durations and parse/merge/audit method attribution are
   required. The requested installed release cannot provide those traces. Use
   only route templates, counts, status codes and monotonic durations; no bodies,
   identifiers, credentials or NPI. Remove diagnostic hooks before any shipment.
2. **Registrar pause scenario:** specify the typical/longest pause to validate
   session survival. Keep both 20-minute requests until that experiment succeeds;
   no keep-alive redesign is proposed now.
3. **Course ops interruption policy:** decide pause versus complete the currently
   authorized application-card buffer, and approve the presentation of unknown
   answers if partial preparation is exposed.
4. **Cache retention and desk acceptance:** agree what happens to applicants no
   longer present in an authoritative roll while preserving local desk records;
   confirm existing cached/unknown timestamp wording is adequate. Test the desk's
   refresh, filtering, course switching and audit outcomes before another release.
5. **WhatsApp boundary:** any change tying OS accessibility binding to centre
   opt-in belongs to a separate product review. Default runtime binding is checked
   in the device results; no accessibility activation or letter preview is part
   of performance work.

The next bounded engineering pass should address priorities 1 and 2, followed by
the measured portion of 3. The rest should stay staged behind their evidence or
product decisions. No implementation of this next pass is included in this
installation task.

Never add speculative GETs, shorten/duplicate keep-alive, refetch teacher-list on
filters/theme/navigation/idle, parallelize Zero Day before finalization is known,
send `r=`, store NPI/letter bodies, or change backend PHP to claim speed.

The coroutine ownership and cancellation approach follows Android's
[coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).
