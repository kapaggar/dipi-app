# Desk performance - 2.0.0 / 98

Implemented on `main` from 1.46.2 / 96, following the owner-approved analysis.
Owner requested a final 2.0 GitHub release after reviewing the implementation.
Version 2.0.0 / 98 supersedes the unpublished 1.46.3 / 97 preparation.
Work remains on main, with no v7 merge/UI port. The later installation request
authorized the Pixel C update. See the [device follow-up and next implementation
plan](../reports/2026-09-11-android-performance-follow-up.md) for confirmed
installation, partial after-measurements, mixed CPU results and the subsequent
Wi-Fi ADB disconnect. The remaining paths still need measurement.

The full [baseline inventory, measurements and ranked recommendations](../reports/2026-09-10-android-performance-baseline.md)
are archived separately with source links pinned to the analyzed 1.46.2 commit.

## Changes

1. Existing worklist, centre, attended-table and teacher-roll HTML processing,
   DTO serialization/mapping and both existing roll-audit passes run on the CPU
   dispatcher. Course ops encrypted roll writes/cache reads run on IO. Requests
   retain their order; audit rules and output are retained. Session generations,
   cancellation propagation, immutable disclosure inputs and course checks keep
   delayed processing from repopulating expired-session data or another course's
   UI. An old course completion cannot launch the new course's Zero Day request.
2. Reuse the same tag, whitespace and role-suffix regexes in the worklist parser,
   including the health-noise whitespace regex. No new parsing format.
3. The minute clock subscribes only while STARTED and interactive. Screen-state
   broadcasts cancel the pending minute delay; lifecycle stop also unregisters
   the receiver. Resume emits current time immediately. No polling added.
4. Day 0 summary skips its Zero Day GET when either the course state or worklist
   rows say finalized. The normal active-course summary still fetches once.

The changes were initially prepared as PATCH 1.46.3 / 97: existing behavior
corrected and computation relocated, with no new fetch or freshness model.
The owner explicitly chose MAJOR 2.0.0 / 98 for publication; this is a release
numbering decision and does not imply a UI or protocol rewrite.
Photo review/ML Kit remains default off. Server messages,
status vocabulary, no-`r` sheet allowlist, allocation fields and native photo
display are unchanged.

## Implementation details and ownership

| Concern | Implementation | Validation |
|---|---|---|
| CPU dispatch | `DeskDispatchers.computation` defaults to `Dispatchers.Default`. `StaffRepository` moves response-body conversion, worklist/centre/attended/teacher parsing, DTO serialization and observed-row mapping/auditing off the caller. Room retains its existing database executor. | A paused computation dispatcher lets the caller continue while processing is pending. Synthetic parsed rows and merged audit flags match the previous parser/rules. |
| Blocking course cache work | Course ops roll writes and VM cache reads use the IO dispatcher. Roll/card writes and cache erasure synchronize on the existing store when they can overlap. | Existing Course ops prefetch/cache tests stay green; no new request or cache format. |
| Session ownership | Repository operations capture a session generation. Login, logout, expiry and erase invalidate earlier generations. Checks around newly suspended computation prevent those results from being published. Cancellation remains cancellation through `toApi()` and VM handlers. | Expiry during paused parsing cannot repopulate rows or the in-memory disclosure map. |
| Course ownership | VM completion checks compare course and session before publishing, merging rooms, or proceeding to the next course-open step. Changing courses clears the room-pull busy state. | A held old-course response cannot publish old rows or launch the next course's Zero Day. A current-session 403 still returns to Sign-in after a course switch. |
| Disclosure snapshot equality | The observed roll combines with immutable disclosure snapshots, then computes the audit away from Main. The final comparison uses value equality, matching `distinctUntilChanged`. | A refreshed public row still appears when its disclosure values are unchanged but its map instance differs. An identity-comparison negative control failed this test. |
| Regex reuse | Existing tag, whitespace, role-suffix and health-noise patterns are compiled once. Pattern text, flags, entity decoding and trimming are unchanged. | Network parser fixtures pass. The synthetic allocation probe checked 512 output pairs before measuring. |
| Minute clock | `interactiveScreen` uses screen on/off broadcasts and `PowerManager.isInteractive`; `deskClockTicks` is lifecycle-gated at STARTED. Invisible states cancel the minute producer; lifecycle stop unregisters its receiver. | Virtual-time tests check no clock reads while screen-off/backgrounded, upstream unsubscribe, current-time resume and cancellation cleanup. |
| Finalized summary | `openSheet` checks both existing course finalization state and finalized worklist rows before dispatching Day 0 summary. | Both finalized signals produce zero summary requests; an active course produces its existing single request. SheetRouteSafetyTest remains green. |

Source files: [StaffRepository](../../app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt),
[DeskDispatchers](../../app/src/main/kotlin/org/dhamma/dipi/staff/data/DeskDispatchers.kt),
[DeskViewModel](../../app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt),
[DeskClock](../../app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskClock.kt),
[SearchPageParser](../../core/network/src/main/kotlin/org/dhamma/dipi/staff/network/SearchPageParser.kt).

The dispatch change deliberately retains both existing audit passes and the
worklist-to-Zero-Day dependency. It does not establish that all remaining UI work
is off Main, consolidate audits, redesign all cache/session races, or cancel the
existing Course ops prefetch on background. Those require separate evidence.

## Network and scheduler inventory delta

| Path / job | 1.46.2 | 2.0.0 |
|---|---|---|
| Saved-session start through centre | 3 logical GETs; redirects additional | Same |
| Centre load | Dashboard then acco-handler, 2 GETs | Same |
| Active course open | Worklist then Zero Day, 2 GETs | Same |
| Finalized course open | Worklist only, 1 GET | Same |
| Compact pull-to-refresh | Worklist only, 1 GET | Same |
| Ordinary sheet open | 1 sheet request plus possible WebView assets | Same |
| Finalized Day 0 summary | Unguarded Zero Day request | 0 requests |
| Course ops entry | 1 teacher-list, optional worklist, missing application cards at <=4 concurrent | Same |
| Course ops filter/destination navigation | 0 GETs | Same |
| Normal resume | 0 fetches, except pending browser Edit handoff | Same |
| Session keep-alive | One 20-minute token + centre coroutine; process-local | Same |
| Connectivity | Callback; recovery flushes existing outbox | Same |
| Desk minute clock | Composition lifetime, including background | STARTED + interactive only |
| Query/snackbar timers | 300 ms debounce / 4.2 s one-shot | Same |
| Photo / WhatsApp / library startup | On-demand photo; opt-in WA only; one-shot ProfileInstaller | Same |

Counts are source-derived logical calls, not captured wire counts. No new GET,
transport parallelism, keep-alive interval change, teacher-list refetch, photo
prefetch, cache format, or persistence of NPI was introduced.

## Baseline - Pixel C, Android 8.1, installed release 1.46.2 / 96

Device `10.0.0.144:5555`, Wi-Fi ADB. Main CPU from `/proc/PID/task/PID/stat`,
10 ms resolution; byte deltas from UID 10145 xt_qtaguid counters. CPU is not
blocked time or method-exclusive profiling. Single trials, with UI observation
overhead except the warm control. Exact per-endpoint duration/count unavailable
on the non-debuggable installed release (`am profile`: Process not debuggable).

| Path | Requests (source) | Wall ms | Main CPU ms | Observation |
|---|---:|---:|---:|---|
| Saved-session start -> centre | 3 + redirects | Data visible in (2,895, 6,072] | 2,540 | Activity launch 1,021 ms |
| Course open, cached Room roll | 2 | Cached Board by 5,265; fresh label in (7,987, 9,995] | 7,020 | 208 applicants; exact Zero Day completion unknown |
| Warm course, no UI polling | 2 | 13,070 observation window | 5,540 | Process CPU 8,090 ms; 5/6 frames janky |
| Warm Day 0 list sheet | 1 + assets | Fetched indicator by 3,383 | 440 | Actual WebView first-data paint not instrumented |
| Compact pull-to-refresh | 1 | 8,446 observation window | 3,570 | Existing rows visible; no Zero Day merge |
| Course ops entry | 1 + optional worklist + missing card count | Populated roll in (6,938, 16,728] | 5,710 | 71-person roll; traffic settled by 20,781 ms |
| Course ops filter / destinations | 0 | 12,494 action window | 540 | 0 app bytes |
| Home for 90 s | No due fetch | 91,040 | 60 | RX/TX 391/520 B, quiet final 30 s |
| Return | 0 | Activity 129; Board observed by 2,457 | 50 | 0 app bytes |
| Centre idle, screen on | No due fetch | 120,430 | 0 | Process CPU 60 ms; RX/TX 135/104 B |
| Centre idle, screen off | No due fetch | 121,493 | 10 | Process CPU 80 ms, includes transition; 0 app bytes |
| Sign-in idle / fresh credential login | Unmeasured | Unmeasured | Unmeasured | Owner required preserving session |

The four-minute centre run rendered zero frames. No app wakelock, current alarm
or job, bound WhatsApp service, or ML Kit thread was observed. APK inspection
confirmed photo-review default false and ML Kit absent. Whole-device battery/radio
accounting is confounded by Wi-Fi ADB and delayed startup attribution; no reliable
mAh estimate. The 20-minute tick/typical long registrar pause was not measured.

## After measurements and validation

At publication, the tablet remained on 1.46.2 and device after-measurements were
pending. The [later device follow-up](../reports/2026-09-11-android-performance-follow-up.md)
records the authorized 2.0.0 installation and available after table. Those results
are mixed and incomplete; no overall speedup or battery saving is claimed.

Local synthetic `stripTags` probe: Mac aarch64, JDK 20.0.1; 512 distinct synthetic
inputs, output equality checked, eight warm-up pairs, eleven alternating measured
pairs of 50,000 calls. Baseline reproduces the original Kotlin regex/trim operations;
after calls the compiled optimized parser. Median per call:

| Metric | Before | After | Interpretation |
|---|---:|---:|---|
| Allocated bytes | 4,015.7 | 2,607.7 | 35.1% less allocation |
| CPU ns | 1,116.8 | 1,153.6 | No CPU improvement in this probe |
| Wall ns | 1,128.4 | 1,162.9 | Not a device or end-to-end result |

Targeted tests verify dispatcher/caller separation, identical parsed/audited rows,
non-persistence of synthetic disclosure values, session expiry during processing,
course switching, refresh with equal disclosure values and changed public rows,
current-session 403 after switching courses, screen-off/background clock cancellation
and current-time resume, and both finalized signals plus the active-summary path.
Four negative controls failed as
expected with session protection, lifecycle gating, the finalized-summary guard,
or value-based disclosure comparison temporarily disabled; all protections were
then restored.

The unpublished 1.46.3 preparation passed **853 tests, zero failures/errors/skips**:
model 150, audit 31, network 188, datastore 26, app 458. SheetRouteSafetyTest
passed all three tests. The final 2.0.0 suite and artifact checks are recorded
below after the version-specific run.

```bash
./gradlew :core:model:test --rerun :core:audit:test --rerun \
  :core:network:testDebugUnitTest --rerun \
  :core:datastore:testDebugUnitTest --rerun \
  :app:testDebugUnitTest --rerun --console=plain
./gradlew :app:assembleRelease -Pdipi.photoReview=false --console=plain
```

## Final release validation

Final 2.0.0 / 98 validation completed on 2026-09-10:

| Check | Result |
|---|---|
| Required full unit suite | **853 passed; zero failures, errors or skips**, 1m 58s |
| Module totals | model 150; audit 31; network 188; datastore 26; app 458 |
| SheetRouteSafetyTest | 3 passed |
| Release assembly | Passed in 1m 10s; R8, resource shrinking and release lint completed |
| APK identity | `org.dhamma.dipi.staff`, **2.0.0 / 98**, non-debuggable, arm64-v8a |
| Signature | Verified; existing Android debug signing identity retained for desk distribution |
| Default features | Live Drupal; mock false; photo review false |
| ML Kit | No ML Kit/DataTransport classes in DEX names or release R8 mapping |
| Manifest | Service/provider/receiver inventory matches 1.46.2; no wake-lock, location or sensor permissions |
| Keep-alive | Repository and VM keep-alive blocks byte-for-byte unchanged from baseline |
| Documentation | 106 baseline source links checked against the pinned commit; local document links resolve |
| Device after-measurements | Pending; no installation performed in this release task |

APK size: **6,929,827 bytes**. SHA-256 for both release asset names:

```text
c00681a9715ed0cd57fe0673ba77b0859c77d084f9096f56a412b84ad3b6be63
```

GitHub release: [v2.0.0](https://github.com/kapaggar/dipi-app/releases/tag/v2.0.0).
Release assets are `dipi-staff-2.0.0.apk` and `dipi-staff.apk`, byte-identical copies
of the verified release APK. Publication marks v2.0.0 latest, preserving the
[stable APK link](https://github.com/kapaggar/dipi-app/releases/latest/download/dipi-staff.apk).
No temporary profiling hooks, real student data, raw diagnostics, handoff archives
or signing keys are included in the commit.

## Remaining device measurements

The 1.46.2 baseline has no per-Retrofit duration trace and no method-exclusive
parse/merge profile. The 2.0.0 release is non-debuggable. Its first tablet run can
repeat process/Main CPU, UID byte counters, `gfxinfo`, and idle measurements;
exact endpoint and method attribution needs a separately authorized profiling
build. Publishing this release does not fill those gaps.

1. After installation is authorized, confirm the exact APK version, signature,
   session, Desk mode, centre/course, Simulate offline state and rotation. Preserve
   the session and local desk data; do not log out or clear app storage. Preserve
   the original preferences after each trial.
2. Repeat the baseline saved-session start, the same upcoming course, one warm
   sheet, compact pull-to-refresh, one Course ops entry, its local filters and
   navigation, and Home for 90 seconds followed by return. Keep the one mutating
   teacher-list GET confined to each deliberate Course ops entry. Do not use
   repeated teacher-list requests as a microbenchmark.
3. Record the same cumulative `/proc` CPU and UID bytes over matched windows.
   Reset `gfxinfo` before the course action. Record cached useful pixels, fresh
   worklist presentation and completion of Zero Day separately; a global freshness
   label is not a Zero Day completion marker.
4. Reset battery statistics, leave the centre untouched for two minutes with the
   screen on, then two minutes screen-off. Inspect app-scoped alarm, job,
   connectivity, thread and wakelock state. Avoid rapid UI snapshots during CPU
   control windows. Keep Wi-Fi ADB's energy/accounting limitations explicit.
5. A profiling build must emit only route templates, counts, status codes and
   monotonic durations. Never record applicant IDs, query strings, cookies,
   credentials, bodies or NPI. Time parsing, merging and auditing separately,
   and remove all temporary instrumentation before shipping.
6. Test the unchanged 20-minute pair through a representative registrar pause
   before proposing any transport reduction or background deferral. A four-minute
   idle window or 90-second return is insufficient evidence for that decision.

No post-release speedup, radio saving or mAh estimate is asserted until these
measurements exist. Sign-in idle remains unmeasured at the owner's instruction.

## Deferred findings and risk

Keep-alive stays the required two-request, process-local 20-minute coroutine,
including while backgrounded. No cache-honesty model changed. Native hall cache
scoping/new timestamps, global worklist freshness, retained Room rows, duplicate
in-flight requests, audit consolidation, Course ops prefetch lifecycle, image
sizing/cancellation and WebView assets remain deferred. Existing accessibility
binding is not structurally tied to centre opt-in; the measured default is
off/unbound and WhatsApp product changes remain out of scope. Never trade a new
GET, shorter keep-alive, teacher-list refetch, `r=`, NPI persistence, backend
change, ML Kit activation or v7 UI for apparent speed.

Coroutine implementation references: [main-safe functions and injected dispatchers](https://developer.android.com/kotlin/coroutines/coroutines-best-practices),
[lifecycle-scoped coroutines](https://developer.android.com/topic/libraries/architecture/coroutines).
