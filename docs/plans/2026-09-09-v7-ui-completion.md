# v7 UI completion and optional photo build

Owner request: implement missing v7 UI and fix reviewed defects; photo review/live photo update optional and disabled by default to reduce the release APK. Work on the existing feat/dipi-v7 tree without deleting previous work. Recovery snapshot: /private/tmp/dipi-ui-repair-baseline-2026-09-09.

## Design and acceptance

Complete the compatible native v7 plan in docs/handovers/dipi-v7-implementation-2026-09-09/IMPLEMENTATION_PLAN.md. The original design's prototype controls, invented data and optional policy changes remain excluded. Preserve existing endpoints, status strings, course-ops read-only behavior, configured hall/room geometry, print safety and encrypted cleanup rules. The reviewed faults are detailed in /private/tmp/dipi-v7-implementation-review-2026-09-09.md.

Photo capability is a build-time opt-in (`-Pdipi.photoReview=true`), false by default. The compact build keeps applicant photo display but hides and rejects correction/review/upload entry points, excludes bundled face detection, and cannot upload corrected photos. The enabled build retains the existing source-bound editor, explicit live update workflow and encryption. No dynamic code/model download. Measure both optimized APKs; aim for approximately 6 MB for compact but report measured bytes, not an assumed guarantee. A runtime switch alone cannot remove APK bytes. Both alternatives use the existing application ID and signing configuration.

## Work items

1. Course ops and theme: fix nested phone scrolling; wrapping teacher names; readable consistent light/dark text; bounded header/controls; preserve hall geometry and card navigation. Add meaningful narrow/large-font/long-name tests.
2. Desk and centre UI: responsive centre actions, separate hall setting cards, Room Chart pinned navigation and actual bring-into-view, complete Audit comparison with related-record actions and pinned heading, responsive sheet header/format band, responsive report range/table, wrapping Calling/Check-in/Applications controls and long content. Preserve existing semantics and callbacks. Add regression tests for reviewed defects and interactions.
3. Optional photo capability: conditional ML dependency/source implementation, build constant, hidden and guarded review/upload routes, optional Settings explanation, default/off and enabled verification, no changes to normal photo display or data cleanup. Version 1.48.0 / 99 for this user-visible capability.
4. Integrate and review: no accidental loss of earlier dirty edits; reproduce and fix meaningful test failures; full supported suite and lint; compact and enabled release builds with dependency/APK inspection. Capture synthetic native visual evidence, install compact on Pixel C and verify version/startup. Do not upload student photos or send messages during verification.

## Status

- Baseline snapshot: complete; no reset, clean, commit or branch deletion.
- Course ops/theme: implemented and reviewed; loaded phone scrolling, readable dark card surfaces and teacher wrapping covered by regression tests.
- Desk/centre: implemented and reviewed; responsive bounds, paired Audit Open and repeat room reveal covered by regression tests. Native print revealed nine-up chits; corrected geometry passed actual Android12/12/1 chit and2/2/1 slip PDF acceptance.
- Optional photo: implemented. Default compact and opt-in enabled builds both assemble; enabled photo/sheet/layout tests46/46 passed. Final compact6,978,987 bytes / enabled20,292,065 bytes; APK archives checked for detector exclusion in compact.
- Review/tests/builds/device evidence: supported JVM suite 904/904 passed and lintDebug passed. First native visual run16/16 passed; updated Blossom runs showed native keyboard panning hiding the header, not the target room. The fixture now matches MainActivity adjustResize; valid navigation dismisses the keyboard, invalid Jump retains focus. Focused room regression suite passed. Native print acceptance complete; final native visual run16/16 and focused room suite22/22 passed. Compact release installed and launched on Pixel C as1.48.0/99; installed APK hash matches the compact artifact. Final results: docs/handovers/dipi-v7-ui-completion-report.md.

Do not mark screen acceptance complete based solely on source-token tests or presence of labels. Check useful bounds, scrolling, text layout, loaded phone state and genuine font scale 1.3. Use generated records for screenshots. Native print acceptance must be distinguished from host-browser output.
