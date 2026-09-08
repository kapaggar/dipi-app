# Native photo correction

Status: design proposal, updated after backend research. The full-form upload recommendation is withdrawn. See [backend findings and the safe correction boundary](2026-09-06-photo-backend-research.md). No backend change is approved.

## Recommended scope

Replace the existing preview-only Photo review screen with native correction, on-device scan suggestions, explicit review and export. Under the unchanged-backend rule, corrections remain local. Shared photo correction requires the isolated server operation described in the research report; do not resubmit the full applicant edit form. Preserve the general Edit on desk site browser handoff.

Three possible approaches:

| Approach | Benefit | Limitation |
| --- | --- | --- |
| Native correction and export (available under current rules) | Touch editing with no applicant writes | Corrections stay local; normal desk-site upload still has full-save side effects |
| Native correction and a dedicated photo-only server operation (recommended for shared corrections) | Shared corrected photos without applicant-form replay | Requires an explicit backend exception and implementation of the researched contract |
| Embed the browser extension | Closest initial visual match | Browser globals, desktop crop controls and WASM assets do not map cleanly to the native app; expands the authenticated WebView surface |

Cross-course face matching in `facematch.js` is a separate subsystem, not required for rotation/crop correction. It is outside this proposed first release. No face embeddings or identity matching are introduced. This scope choice is visible for owner review, not an implicit claim of complete extension feature parity.

## What the current implementations actually do

- Android displays a rotated image and a centre-crop toggle. It cannot yet render a precise corrected JPEG. Live suggestions are empty and live uploads return an unavailable message.
- The extension rotates actual pixels, keeps a normalized crop rectangle in post-rotation coordinates, and exports JPEGs. Its crop ratio is 260:280 (13:14).
- Its scanner evaluates four orientations using facial landmarks. Confident suggestions can be applied locally, but approval and upload remain separate actions.
- The extension submits the complete `dh_ma_applicant_form` from `GET /app/{aid}/edit`, replacing `files[upload_photo]`, to `POST /app/{aid}/edit` with `op=Update` and fresh Drupal tokens.
- The extension's batch path does not perform the second staleness check used by its single-photo preview. The Android implementation must instead use the dedicated photo protocol's source-revision checks; it must not port either full-form path.

## Upload boundary requiring an owner decision

The current server has no photo-only endpoint. Retaining all submitted control values is necessary, but does not guarantee that only the photo changes in the database.

The local read-only backend source shows that the full-form submit handler:

- Normalizes first and last names, trims extra fields, derives health flags, and updates timestamps.
- Processes attendance, room-related fields, special seating and confirmation numbers. These can change even when the submitted form controls match their initial values. A concrete example: the form represents chowky/chair/backrest as one radio selection, then submission resets all three flags and restores only that selection. A record with both chair and backrest cannot round-trip through this form without losing one flag.
- Updates course-history/long-course records, calls referral handling, and regenerates the application PDF. Certain long-course changes can trigger teacher letters.
- Attempts the photo storage write, but logs storage failure and still updates the photo path. HTTP success alone therefore cannot establish successful replacement.

A before/after comparison can detect exposed differences after saving; it cannot prevent the server's normalization, inspect every hidden database field, roll back changes, or close the race between the final GET and POST. The UI must not claim "all other fields preserved" as an absolute guarantee.

The original proposal to permit full-form replay is withdrawn after the offline characterization reproduced these side effects. Owner approval of a UI design is not approval to resubmit sensitive applicant forms. A dedicated photo-only operation would require a separate, explicit exception to the unchanged-backend rule. See the research report for its bounded inputs, write set and recovery contract.

No live applicant upload is performed during implementation until a controlled test application is identified and its upload authorized.

## Native interaction design

Use the existing DIPI theme, typography, course context and navigation. Photo review remains a course-level desk destination, reachable from the existing entry points; opening it from an applicant focuses that applicant. Course ops remains read-only.

### Review grid

- A responsive grid replaces the tiny-photo list: approximately four columns in the Pixel C's remaining desk pane and two on phones, adapting to available width and font size.
- Each card gives the image most of its area, with a 13:14 viewing frame. The full image remains visible unless a correction is being previewed; the grid never silently crops a source.
- Below the image: name, confirmation number, server status, and one text-backed review badge. Tap the photo or Edit to enter the editor. Rotate and Reviewed have labelled 48dp touch targets. Long names wrap.
- Header actions: **Scan photos**, **Review fixes**, **Upload (N)**. Export and **Clear this course's edits** live in an overflow menu. No crowded row of eight commands.
- Search and existing gender/new-old filtering remain available. Review filters: **All**, **Needs review**, **Ready**, **Uploaded**. Detailed reasons such as No face, Multiple faces, Source changed and Upload uncertain appear on the relevant card.
- An unscanned photo is Unreviewed, never labelled Good. Reviewed unchanged photos do not enter the upload count.
- Scanning has completed/total progress and Cancel. It must not block scrolling or manual editing.

### Photo editor

- On tablets, a large corrected preview and an Original/Corrected comparison with controls beside it. On phones, one large preview with an Original toggle and bottom controls.
- Rotate left, rotate right and 180 degrees affect real pixel geometry. Undo and Reset restore reversible local edits.
- Crop uses a movable/resizable 13:14 frame, pinch zoom and pan, with face/head margin guides. The editor uses normalized coordinates in the rotated source, then saves an exact integer pixel rectangle bound to that source's dimensions, as specified in the agent handoff. Controls and handles remain usable with touch and large fonts.
- A suggested correction shows its resulting preview before acceptance. Ambiguous/no-face/multiple-face results require manual review; they never select a person's face automatically.
- **Keep & next** approves the displayed result and moves to the next unreviewed photo. **Save draft** retains changes without approving them. Any later edit clears approval.

### Review and shared correction (conditional on an approved photo-only server operation)

If the backend remains unchanged, expose local approval and export instead of Upload. The following shared-correction flow applies only once the dedicated server operation is implemented and verified.

- Review fixes shows before/after pairs for changed photos. **Approve selected** is explicit, and separate from uploading.
- Upload opens a review sheet listing the course, selected applicants, correction previews and total. The request contains only the approved correction recipe and source revision, plus operation/form tokens.
- The foreground batch uploads sequentially and exposes progress, **Stop after current**, and per-photo results. Navigating away does not silently lose the outcome. No background or automatic restart.
- Uploaded, Validation failed, Source changed, Application changed, and Outcome unknown are distinct. Server validation text remains verbatim.
- A timeout or process death after POST starts is Outcome unknown. Reopening the feature offers read-only verification, never automatic retry.
- Successful verification resets local geometry and refreshes the image cache, preventing a second rotation/crop of already corrected pixels.
- Clearing local edits affects only this server/centre/course. It never reverses an uploaded photo.

## Processing and storage

- A focused geometry component handles right-angle rotation, crop mapping, bounds checking and JPEG rendering. Preview and export share this implementation so their results match.
- Use bundled Android ML Kit face detection for local landmark/box suggestions, subject to Pixel C validation. The bundled model is available immediately and adds roughly 6.9 MB according to Google's documentation. Evaluate all four orientations, preserve uncertain cases, and use conservative head/chin margins. No beautification, face reconstruction or generative edits.
- Scanner evidence stays in memory. Persist only encrypted course-scoped correction metadata: applicant ID, source-bound integer geometry, source fingerprint/dimensions, review state and upload outcome. No applicant form values, photo pixels or face descriptors in that store.
- Bind every correction to its source fingerprint. A replacement photo invalidates old geometry and review approval. Migrate legacy unscoped edit flags conservatively as unapproved drafts only when scope and source can be established; otherwise discard the obsolete flags.
- Photos and corrected export bytes stay in bounded memory. Explicit exports stream through Android's document picker; the user chooses the destination. The first implementation does not add photo share-cache files.
- Clear in-memory photo-form tokens and photo caches on logout/session expiry/erase. The correction feature never loads the full applicant form. Cancel in-flight loads so an old request cannot repopulate a cleared cache. Existing local-data rules continue to apply.

## Transport boundary

The app-only implementation uses the existing read-only photo route and local geometry. Do not fetch or resubmit applicant edit forms for correction.

Shared edits follow the dedicated photo-only contract in [the research report](2026-09-06-photo-backend-research.md): session and CSRF protection; narrow correction inputs; server-enforced access/finalization; source revision checks; preserved source objects; photo-only writes; coordinated legacy writers; durable operation outcomes; and verified image readback. This contract is proposed, not currently available on the server.

## Components and tests

| Component | Responsibility | Validation |
| --- | --- | --- |
| Photo geometry/renderer | Rotation, source-bound pixel crop and corrected JPEG | Asymmetric synthetic images; all four rotations; crop bounds; preview/export parity; no double application |
| Local scan adapter | Face boxes/landmarks and conservative suggestions | Injected detector results; ambiguity/multiple faces; cancellation; controlled Pixel C samples |
| Scoped edit store | Draft/review/source/outcome metadata | Server/centre/course isolation; migration; source replacement; logout/erase and interrupted upload recovery |
| Photo correction transport (conditional) | Dedicated photo-only operation and result verification | Narrow inputs; source revisions; tokens; drift; login redirects; storage failure; duplicate operations; timeout after commit; no applicant-form submission |
| Native screen/editor | Grid, gestures, review and progress | Robolectric user flows; narrow/large-font layout; Pixel C crop controls; button state and outcome rendering |

Use synthetic applicant fields and synthetic images in committed fixtures. Do not commit real photos, application HTML, health data, IDs, credentials or tablet dumps.

## Rollout

After design approval: implement the authorized local scope in a `codex/` branch, complete the existing debug-unit suite plus focused tests, and validate the editor and scan on Pixel C. Shared correction additionally requires approval and implementation of the researched backend exception, then a specifically authorized controlled-record pilot. A failed preservation or photo verification pilot keeps live writes unavailable; local correction/export can still ship with the limitation stated.

The next user-visible feature release is expected to be 1.45.0 / versionCode 88, subject to checking the repository again before assembly. Follow existing install/release rules and publish both versioned and stable APK filenames when the release is ready.

## Evidence

- Extension: `/Users/wizops/DIPI/callconfirm/photo-review/README.md`, `review.js` (geometry and upload helpers), `facematch.js` (separate identity-matching system).
- Android: `feature/photos/.../PhotoReviewScreen.kt`, `app/.../PhotoEditStore.kt`, `core/network/.../PhotoLoader.kt`, `StaffRepository.uploadPhotos`.
- Backend, read only: `/Users/wizops/DIPI/dipi-web/sites/all/modules/dh_manageapp/inc/application.inc`, form at line 4, validation at 821, submission at 877, photo storage at 1274, downstream PDF/letter behavior around 1300.
- Official Android detector documentation: https://developers.google.com/ml-kit/vision/face-detection/android (checked 2026-09-06). Bundled dependency currently documented as `com.google.mlkit:face-detection:16.1.7`; Pixel C support and performance still require actual validation.
