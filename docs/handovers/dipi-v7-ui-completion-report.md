# DIPI v7 UI completion - 1.48.0 / 99

Implementation on the existing `feat/dipi-v7` working tree. The original uncommitted work was retained. No branch reset, deletion, commit, push or GitHub release was performed. A verified 806-file recovery snapshot is at `/private/tmp/dipi-ui-repair-baseline-2026-09-09/working-tree.zip`.

## Visible changes

- Centre dashboard: three primary actions on one tablet row; narrow layouts wrap. Centre settings: separate male/female hall cards.
- Room Chart: pinned title, actions, scope, blocks and jump above the scrolling grid. Valid Jump/Next dismiss the keyboard; an invalid query retains focus for correction. Jump and Next occupied reveal the requested cell, including repeated requests for the same room. Large refusal lists stay bounded. Occupant names can grow, with existing room geometry, age and old/new cues retained.
- Audit: pinned heading with independently scrolling evidence, identified matching applicants and separate Open actions. Existing per-applicant selection and Back context remain.
- Applications, Calling and Check-in: wrapping controls and useful bounded detail/list regions on phones. Long email and applicant content remain readable.
- Course report: wrapping date/preset controls and one horizontally scrolling table/totals region. Presets still fill dates; Run initiates the request.
- Sheets: responsive title, format and action regions; phone column controls remain horizontally scrollable. Fit/Readable affects screen layout only.
- Course ops: one loaded-phone student-card scroll, independent tablet columns, wrapping teacher names, a finite horizontally scrolling teacher table, and bounded unseated facts. Teacher-at-bottom hall geometry and course-ops read-only rules remain.
- Theme: composition-aware light/dark colors and readable shared card surfaces, including Day Summary and Settings. Dark mode uses Steel night; the selected light skin is retained.
- Print: native Android verification exposed nine-up student chits. Minimum heights now leave A4 rounding clearance: 69mm chits and 138mm slips. Content can grow instead of being clipped.

The earlier compatible v7 work is preserved: exact health-answer classification/history provenance, defined desk populations, audit evidence/navigation context, unknown freshness timestamps, report presets, and screen-only sheet width. Prototype-only controls and policy changes excluded by the design conflict register remain excluded.

## Optional photo capability

Photo correction, review and live corrected-photo update are disabled by default at build time. Ordinary applicant photo display remains available. The compact APK excludes ML Kit face detection models/native binaries and hides or rejects correction and upload entry points. Repository upload guards prevent a hidden UI route from bypassing the option.

`./gradlew :app:assembleRelease -Pdipi.photoReview=true` builds the enabled alternative. It retains encrypted, source-bound drafts, rotate/crop/scan/review/export and the existing explicit applicant-edit-form update. This is an APK choice, not a runtime switch. Both alternatives use the same package/signing and update the existing installation. Enabling it does not automatically upload anything.

No live student photo upload or message sending was performed during verification. Existing mocked network/workflow tests cover photo update behavior. See [PHOTO-BUILDS](../PHOTO-BUILDS.md).

## Verification

- Full supported JVM suite: **904 passed**, zero failures/errors/skips: model 167, audit 36, network 188, datastore 29, app 484.
- `:app:lintDebug`, debug/test APK assembly and optimized compact release assembly passed.
- After the native print defect was fixed, all **27 focused sheet/layout tests passed**, and compact debug/test/release APKs rebuilt successfully.
- Enabled alternative: **46 focused photo/hub/scanner/sheet/layout tests passed** after the print correction, together with lintDebug and optimized release assembly.
- Pixel C: Android 8.1/API 27, 2560x1800, density 320. Fixtures render at normal tablet width and constrained 412dp with actual font scale 1.3 for the phone student card. They construct no repository and use generated records only.
- First native visual run: 16/16 passed. Refreshed Blossom runs revealed the room target was visible while the test activity keyboard panned the header offscreen. The fixture now matches MainActivity adjustResize; valid Jump and Next clear focus/hide the keyboard, while invalid Jump retains focus. **Final native run: 16/16 passed** in 98.8 seconds; the Room Chart title and target 80 are both visible. Final keyboard-focused regression suite: **22/22 passed**.
- Native Android PrintManager PDF acceptance passed: **25 chits = 12/12/1** after the fix (previously 9/9/7), and **five slips = 2/2/1**. Both saves completed through the tablet system print UI, with page counts extracted from those PDFs and first pages visually inspected.

Initial test failures and review findings were fixed rather than ignored: missing imports, finite teacher-table constraints, below-fold Settings assertions, stateful sheet test callbacks, nested phone scrolling, dark card contrast, repeated room targeting and print height overflow. The first native run failed when the tablet was asleep; waking the device allowed the instrumented activities to run. One print attempt timed out while an unrelated Bluetooth dialog was being dismissed; the later saved PDF came from a successful native print test.

## Reproduce the checks

Run from the repository root:

```bash
./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease
./gradlew :app:testDebugUnitTest --tests '*PhotoCorrectionFlowTest' --tests '*PhotoReviewScreenTest' --tests '*PhotoScannerTest' --tests '*CourseHubScreenTest' -Pdipi.photoReview=true
```

Install the compact debug APK and its Android test APK on an awake/unlocked Pixel C, then run:

```bash
adb -s 10.0.0.144:5555 shell am instrument -w -e class org.dhamma.dipi.staff.V7DeviceVisualTest org.dhamma.dipi.staff.test/androidx.test.runner.AndroidJUnitRunner
adb -s 10.0.0.144:5555 shell am instrument -w -e class org.dhamma.dipi.staff.V7DevicePrintTest -e printFixture chits org.dhamma.dipi.staff.test/androidx.test.runner.AndroidJUnitRunner
```

The opt-in print test opens Android PrintManager. Save its synthetic PDF through the system UI within 120 seconds; repeat with `printFixture slips`. Verify actual per-page record counts in those PDFs. The print test is skipped when no fixture argument is supplied. Visual fixtures write internal `files/v7-synthetic-evidence`; pull them while the compact debug APK is installed, then restore the compact release. Do not use production applicants as fixtures.

## Artifacts and installation

Artifacts are in `app/build/outputs/dipi-1.48.0/` (ignored build outputs). Both alternatives are version 1.48.0 / 99, arm64 optimized release APKs.

| APK | Bytes | Decimal MB |
| --- | ---: | ---: |
| [dipi-staff-1.48.0.apk](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/dipi-staff-1.48.0.apk) | 6,978,987 | 6.98 |
| [dipi-staff-1.48.0-photos.apk](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/dipi-staff-1.48.0-photos.apk) | 20,292,065 | 20.29 |

The compact release was installed in place and launched successfully on the Pixel C. Package version 1.48.0 / 99 and a running process were verified; the installed base APK SHA-256 exactly matches the compact artifact. Existing app data was retained. Only the generated device fixtures and the test APK were removed after evidence capture. No GitHub publication was performed.

SHA-256:

```text
065f538f51fbb6186a995e263db3e1744a161dd661ffec68f7f66831542220a2  dipi-staff-1.48.0.apk
c562096f69fba01de95efdc6735ae7caf9d7a5b499d8b1c44611b354fc0e3e3a  dipi-staff-1.48.0-photos.apk
```

 This report and the build documentation remain in the source tree; generated screenshots/PDFs contain synthetic data only.

Representative native screenshots (absolute local links):

- [Centre dashboard](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/c1-centre.png)
- [Teacher list](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/t1-teacher-list.png)
- [Dark student card](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/t3-student-card-dark.png)
- [Phone card at font scale 1.3](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/t3-student-card-phone-1.3.png)
- [Loaded phone sheet](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/d7-sheet-phone.png)
- [Room80 revealed](/Users/wizops/DIPI/dipi-app/app/build/outputs/dipi-1.48.0/screenshots/d6-room-focused.png)

The final scoped source review found no concrete remaining regression in room keyboard handling, repeated navigation, dark Day Summary colors or chit/slip print geometry.

Verification is scoped to the tested native screens, regression suite and existing workflow tests. It does not claim a new live-server photo-upload acceptance run or universal coverage of every possible room configuration, disclosure length or font scale.
