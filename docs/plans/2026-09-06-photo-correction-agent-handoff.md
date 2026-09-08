# Applicant Photo Correction Implementation Plan

> **For agentic workers:** Use the installed executing-plans skill to implement this plan task by task. If the owner explicitly requests delegation, subagent-driven-development is an alternative. Steps use checkboxes. This document is a handoff, not permission to change the backend or run live applicant writes.

**Goal:** Replace Android's preview-only photo screen with reliable native rotation, crop, on-device suggestions, review and export; add shared server corrections only through an explicitly approved photo-only backend operation.

**Architecture:** Track A is an independently shippable Android feature that reads existing photos and stores only local correction metadata. Track B is a conditional backend feature that transforms the server's source photo and changes only its photo reference; it never replays the applicant form. Track C connects the approved backend capability to Android with explicit per-photo outcomes and recovery.

**Tech Stack:** Kotlin, Jetpack Compose, coroutines, Android Bitmap/Canvas, Android Keystore-backed metadata storage, bundled ML Kit face detection, OkHttp, Jsoup, JUnit/Robolectric. Conditional backend: Drupal 7 Form API, PHP image toolkit, existing private object storage, transactional database writes and durable operation receipts.

**Spec:** [Native design](2026-09-06-native-photo-correction-design.md), updated by [backend research](2026-09-06-photo-backend-research.md). The research supersedes the original proposal to upload through the full applicant form.

## 1. Read this before doing anything

The owner originally requested a native port of the working Chrome/Tampermonkey photo correction tool. The backend was explicitly declared immutable. Research then demonstrated that the extension's upload mechanism is not a photo-only operation. The latest request is to write this detailed handoff plan.

Do not interpret the existence of Track B as approval to execute it. Do not interpret permission to edit photos as permission to change attendance, status, health, identity documents or names.

| Boundary | State at handoff | Executor action |
| --- | --- | --- |
| Research and write this plan | Authorized and completed | Read the evidence; avoid repeating broad exploration |
| Execute Track A design | Proposed, not yet implemented | Proceed when the owner instructs implementation of this plan/local scope; incorporate any design feedback |
| Modify Drupal or add routes | Explicitly prohibited unless the owner grants an exception | Keep Track B read-only until approval specifically includes the photo-only backend feature and legacy writer coordination |
| Deploy backend code or database migrations | Not authorized by this planning request | Prepare tested changes for review first; require deployment authorization |
| Write to a live applicant record | Not authorized by this planning request | Use synthetic tests, then an explicitly identified and approved controlled application |
| Merge/release/install | Established project workflow, subject to the active implementation request | Respect existing authorization; do not infer approval to deploy a backend from Android release approval |
| Cross-course face matching | Outside the proposed scope | Do not port face embeddings or the extension's identity-matching database |

A user instruction to implement this plan is sufficient design direction for Track A unless they specify changes. It does not silently override the explicit backend prohibition. Ask only for an approval or factual input that is missing and required for the next dependent action.

If backend approval is withheld, complete and label the local feature honestly. Do not claim that corrected images have reached DIPI, other tablets or server-generated PDFs.

## 2. Current state and source map

Paths and versions below were verified on 2026-09-06. Recheck them before modifying a checkout.

| Repository | Location | Observed state |
| --- | --- | --- |
| Android | /Users/wizops/DIPI/dipi-app | main at cf2b4dfdcee98640f1447750dee23303cb8e185f; version 1.44.0 / code 87 |
| Backend reference | /Users/wizops/DIPI/dipi-web | d02296f79849dffa9b976f2e7b026a075aa1b538; existing local changes and commits ahead of origin |
| Working extension | /Users/wizops/DIPI/callconfirm/photo-review | README.md, review.js, facematch.js, userscript.user.js |
| Reference screenshot | /var/folders/n_/40h75cl532n2rvjgqlcpvctm0000gn/T/codex-clipboard-2bb4c737-97e6-4cd2-b0e9-5381b4fa8f28.png | Real applicant photos; inspect locally if available, never commit or include in a distributable fixture |

Read Android AGENTS.md, docs/LIVE-DESK.md and docs/DESIGN.md first. The existing design authority is docs/design/DIPI-Staff.dc.html. Preserve the shipped DIPI styling and navigation.

### Android code that exists today

| File, relative to Android root | What matters |
| --- | --- |
| core/model/src/main/kotlin/org/dhamma/dipi/staff/model/Models.kt | PhotoEdit currently contains rotate, cropped, done, uploaded booleans; it cannot represent a real crop |
| feature/photos/src/main/kotlin/org/dhamma/dipi/staff/photos/PhotoReviewScreen.kt | Tiny-photo list; visual rotation and centre-crop toggle, no pixel correction |
| app/src/main/kotlin/org/dhamma/dipi/staff/data/PhotoEditStore.kt | Plain DataStore map keyed only by applicant ID; insufficient scope/source identity |
| core/network/src/main/kotlin/org/dhamma/dipi/staff/network/PhotoLoader.kt | Authenticated read-only source, six concurrent fetches, 32 MiB memory LRU; needs invalidation/session cleanup |
| app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt | photoReview returns no live suggestions; uploadPhotos rejects live uploads |
| app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt | openPhotos, loadPhoto, rotatePhoto, cropPhoto, markPhotoDone, uploadPhotos, photoNote and cleanup |
| app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt | PhotoReviewScreen routing and callbacks |
| core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/WhatsAppStore.kt | Existing encrypted, injectable SharedPreferences pattern; reuse the pattern, not its files or keys |
| app/src/main/AndroidManifest.xml | allowBackup=false; existing narrow FileProvider |
| app/src/main/res/xml/file_paths.xml | Only cache/sheets is shared today; do not widen this to the whole cache/files directory |
| app/src/test/kotlin/org/dhamma/dipi/staff/PhotoPanesTest.kt | Existing Compose/photo tests |
| core/network/src/test/kotlin/org/dhamma/dipi/staff/network/PhotoBoundedLoaderTest.kt | Cache/concurrency tests |
| app/src/test/kotlin/org/dhamma/dipi/staff/TestStores.kt | Test VM/store construction |

The present VM upload callback marks every pending edit uploaded if the repository returns any positive success count. Remove this aggregate-success behavior before introducing real writes. Only a verified result for a particular operation/applicant may mark that photo committed.

### Backend evidence that must not be rediscovered by live writes

Relative to the backend root:

- sites/all/modules/dh_manageapp/dh_manageapp.module:352 defines the normal edit route; :357 defines the modal route. Both use the same form.
- sites/all/modules/dh_manageapp/inc/zero-day.inc:137 loads applicant plus related records and invokes dh_ma_applicant_form.
- sites/all/modules/dh_manageapp/inc/application.inc:877 is the full submit handler. Seating flags are reset at :1245; photo storage occurs at :1274 onward.
- sites/all/modules/dh_manageapp/inc/dana-s3.inc:4 is a server-internal storage writer, not an authenticated client API.
- sites/all/modules/dh_manageapp/dh_manageapp.module:2526 implements GET /show-photo/{aid}.
- sites/all/modules/dipi_api/dipi_api.module:810 implements application creation, not photo replacement.
- sites/all/modules/dh_manageapp/inc/course.inc:914 implements finalize_course and copies photos into student archives.
- sites/all/modules/dh_manageapp/inc/pdf.inc:362 embeds the photo in an application PDF and later writes a_uri.

Source lines may drift. Search by function name before editing.

## 3. Global constraints

- Android package: org.dhamma.dipi.staff.
- compileSdk/targetSdk 35, minSdk 26, JVM target 17, Gradle 8.9. The observed Mac JDK is 20; do not assume a local JDK 17 installation.
- Live base URL is https://dipi.vridhamma.org. No live /staff API, APP API or get-app-detail usage.
- Never submit the full applicant form for photo correction. Never call the status, attendance, letter, course-finalization or referral routines as a correction shortcut.
- Never send Approved. Preserve server messages verbatim.
- Do not add backend permission logic to Kotlin. The server enforces access. Local protocol/source validation is required and is not an invented role system.
- No applicant form values, NPI, credentials, photo bytes or face descriptors in Room, ordinary preferences, logs, crash context, backups or committed fixtures.
- Photo sources and working images stay in bounded memory. Explicit export is a user-selected exception.
- Persist encrypted geometry/review/operation metadata only, scoped by server + centre + course + applicant and bound to source bytes.
- Retain browser isolation for Edit on desk site. Never copy app cookies or passwords into a browser/WebView.
- No em dashes in app-owned copy. Use concise labels, existing theme tokens and accessible touch controls.
- Course ops stays read-only. Photo editing belongs to the desk workflow.
- Never add an r query parameter to a sheet request.
- Do not change the backend checkout during Track A. Do not stash, reset, merge, clean or delete another developer's local work.
- No attribution/watermark/trailers in commits or PRs.
- SemVer before any shippable APK. Expected next local feature release is 1.45.0 / code 88 only if the baseline has not advanced.
- Run the documented debug-unit test suite. Never run ./gradlew test or :app:test; their release Robolectric variant is not configured correctly.

## 4. Deliverables and execution order

1. Task 0: confirm source and approval boundaries.
2. Track A: source-bound native editing, scanning, review and export. Independently shippable.
3. Gate B: obtain a specific backend exception or stop shared-write work.
4. Track B: isolated photo-only backend operation and coordinated legacy writers, tested in isolation/staging.
5. Track C: Android capability parsing, per-photo submission/recovery and controlled integration pilot.
6. Release task: build/install/publish only the scope that has passed its gates.

Do not create a new user-visible task or spawn agents merely because this is a handoff. If delegation is requested later, assign files with clear ownership and review each dependency before integration.

## 5. Product behavior

### Review grid

- Keep current course context and existing entry points. Opening from a selected application focuses it; returning retains its selection.
- Use an adaptive grid: about four columns on Pixel C, two on a normal phone, one when width/font scaling requires it. Minimum usable card width is 156dp; no fixed column count.
- The 13:14 image frame shows the complete source using Fit until the user applies a correction. Do not accidentally crop in the grid.
- Display name, confirmation number, status, and a concise text-backed review state. No green Good badge before scanning/review.
- Primary actions for Track A: Scan photos, Review fixes, Export (N). Track C changes the final action to Upload (N) only after a verified supported capability; export remains available.
- Filters: All, Needs review, Ready. Uploaded appears only for shared correction. Add existing gender/new-old filters and name/confirmation search.
- Card actions have labels/content descriptions and 48dp targets. Tap image/Edit for the editor. Do not copy the extension's tightly packed desktop icon toolbar.
- Clear this course's edits is scoped and explicit. It cannot undo a shared correction.

### Editor

- Large working preview, Original/Corrected comparison, rotate left/right/180, crop, Undo, Reset.
- Use a 13:14 touch crop frame with pan/zoom and generous handles. Crop state is relative to the rotated source, never the screen's letterbox margins.
- Pinch changes the crop selection/viewport consistently; do not maintain two independent zoom values that produce a different export.
- Keep & next approves and advances. Save draft saves without approval. Further edits clear approval.
- Cancel abandons unsaved editor changes; saved drafts remain.
- An unchanged reviewed source can be Approved but is never included in export/upload correction counts.

### Suggestions

- Scan current filtered applicants with available photos. Show completed/total and Cancel.
- Apply safe suggestions is a separate local action, reachable from the scan results. It creates drafts, never approval or upload.
- Confidence is evidence-based: upright facial landmarks, a single unambiguous orientation, one face and safe head margins. Do not invent a percentage confidence from an API that does not provide one.
- Ambiguous, multiple-face or no-face results remain for manual review. Never choose one of several people's faces automatically.
- No cross-course identity matching, face embeddings, beautification, replacement faces or generative image edits.

### Shared batch, only after Track B

- Review before/after previews and selected applicants, then confirm one explicit batch.
- Process one photo at a time. Stop after current is always visible. Stop immediately after a failure, conflict or unknown outcome.
- Other courses' previous batches do not block a new local review; a currently executing writer still serializes writes.
- Do not discard an uncertain operation into an automatically retryable state.
- After a verified commit, reset geometry relative to the new source and invalidate all affected image caches.

## 6. Canonical data and image contract

The following proposed APIs are the contract for the tasks. Keep names consistent across files and tests. Types live in the new model file unless explicitly assigned elsewhere.

    @Serializable
    data class PhotoScope(
        val origin: String,
        val centreId: Int,
        val courseId: Int
    )

    @Serializable
    data class PhotoKey(val scope: PhotoScope, val applicantId: Int)

    @Serializable
    data class PhotoStamp(
        val sha256: String,
        val width: Int,
        val height: Int
    )

    @Serializable
    data class PhotoCrop(val x: Int, val y: Int, val width: Int, val height: Int)

    @Serializable
    data class PhotoRecipe(val clockwise: Int = 0, val crop: PhotoCrop? = null)

    @Serializable
    enum class PhotoReviewState { UNREVIEWED, DRAFT, APPROVED, SOURCE_CHANGED }

    @Serializable
    enum class PhotoWriteState {
        NOT_STARTED, PREPARING, SUBMITTING, VERIFYING,
        COMMITTED, FAILED, UNKNOWN, SUPERSEDED
    }

    @Serializable
    data class PhotoDraft(
        val key: PhotoKey,
        val source: PhotoStamp,
        val recipe: PhotoRecipe,
        val review: PhotoReviewState = PhotoReviewState.UNREVIEWED,
        val editRevision: Long = 0,
        val write: PhotoWriteState = PhotoWriteState.NOT_STARTED,
        val operationId: String? = null,
        val output: PhotoStamp? = null
    )

Additional rules:

- Canonicalize origin with the URL parser: scheme + host + effective non-default port + configured deployment base path if any. Never use only a centre ID as an encryption/store/cache scope.
- Applicant IDs and centre/course IDs must be positive; SHA-256 is 64 lowercase hex characters.
- Hash original encoded source bytes, before resizing, EXIF handling, rotation or JPEG export. Do not use a perceptual hash as source identity.
- For v1, geometry is defined on stored decoded pixels with no implicit EXIF rotation. Both renderers must follow this rule. Auto-scan/manual rotation can correct orientation. Changing EXIF policy requires a protocol version change and fixtures.
- The editor may use normalized floating-point coordinates internally. On save, convert once to a clamped integer crop in the post-rotation source dimensions; persist and transmit that exact crop. This refines the original normalized-storage proposal and avoids Android/PHP rounding drift.
- A crop uses half-open pixel bounds: x <= column < x + width, y <= row < y + height.
- Only 0, 90, 180 and 270 clockwise degrees are accepted at a transport boundary. UI delta actions normalize before constructing a recipe.
- Full-frame crop is canonicalized to null. Identity means 0 degrees and no crop.
- The UI snaps portrait crops to 13:14 when the image has enough pixels. Geometry validation itself supports any positive in-bounds rectangle, which allows small synthetic fixtures and faithful migration.
- Rotation after an existing crop must be predictable: transform its rectangle into the new rotated coordinate space or explicitly reset crop and show the preview. Implement the rectangle transform; never silently keep coordinates for the old orientation.
- Any edit increments editRevision and clears APPROVED. A stale asynchronous scan/load result may not overwrite a newer editRevision.
- Reviewed correction becomes Ready only if the fetched source stamp matches, the recipe is non-identity and there is no unresolved write.

### Golden image vectors

Use a synthetic 2 by 3 source, left-to-right/top-to-bottom pixels:

    A B
    C D
    E F

Expected clockwise outputs:

    0:   A B      90:  E C A      180: F E      270: B D F
         C D           F D B           D C           A C E
         E F                           B A

After 90 degrees, crop x=1,y=0,width=2,height=2 produces:

    C A
    D B

Represent A..F as distinct opaque colors for actual renderer tests. Geometry tests can use their letters. Add transparent PNG and JPEG fixtures generated from synthetic patterns, never student photographs.

### Memory and file boundaries

- Keep the existing 32 MiB thumbnail LRU. Limit original correction work to one source at a time.
- Source download limit: 10 MiB, enforced while streaming even without Content-Length.
- Full correction decode limit: 8,000,000 pixels, checked from image bounds before allocation. Oversize/invalid images show a clear unsupported-image result; they do not crash or silently downsample source coordinates.
- Do not upscale exports. JPEG output quality is 92; flatten alpha onto white explicitly. Recycle owned temporary bitmaps after consumers finish, not while Compose or another coroutine can reference them.
- Aim for under 128 MiB additional peak working memory on Pixel C during scanning/editing. Profile the actual release build; if exceeded, reduce simultaneous working copies.
- Export through Android's document picker directly to its returned URI. For batch export, stream a ZIP of approved JPEGs one at a time. Cancellation produces an explicit incomplete export result, never Uploaded.
- Use filenames based on applicant ID, for example photo-999999-corrected.jpg, without names or other personal details.
- Avoid adding a photo FileProvider/share cache in the first implementation; the document picker covers export without expanding the existing provider surface.

## 7. File ownership map for implementation

All Android paths below are relative to /Users/wizops/DIPI/dipi-app. New files are proposed, not already present.

| Task | Create | Modify |
| --- | --- | --- |
| A1 | core/model/src/main/kotlin/org/dhamma/dipi/staff/model/PhotoCorrection.kt; matching test PhotoGeometryTest.kt | Models.kt only to retire/migrate legacy photo types after callers move |
| A2 | core/network/src/main/kotlin/org/dhamma/dipi/staff/network/PhotoSource.kt; PhotoSourceTest.kt | PhotoLoader.kt; PhotoBoundedLoaderTest.kt |
| A3 | app/src/main/kotlin/org/dhamma/dipi/staff/photos/PhotoRenderer.kt; app test PhotoRendererTest.kt | app build dependencies only if required |
| A4 | core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/PhotoCorrectionStore.kt; corresponding test | app/data/PhotoEditStore.kt migration/retirement; cleanup wiring |
| A5 | app/photos/PhotoScanner.kt; core/model/.../PhotoSuggestionPolicy.kt; tests | app/build.gradle.kts for the pinned detector |
| A6 | feature/photos/.../PhotoEditor.kt; PhotoReviewUiState.kt; PhotoReviewActions.kt; app/photos/PhotoReviewController.kt | PhotoReviewScreen.kt, DeskViewModel.kt, DipiAppUi.kt |
| A7 | app/photos/PhotoExport.kt; app tests PhotoExportTest.kt, PhotoCorrectionFlowTest.kt | photo action wiring, photoNote and photo display callbacks |
| B1-B5 | Backend sites/all/modules/dh_photocorrection with .info/.module/.install, inc/photo-correction.inc, inc/photo-storage.inc and tests | Only after approval: existing applicant photo writer and course-finalization media coordination |
| C1 | core/network/.../PhotoCorrectionClient.kt; corresponding tests | Injection/wiring only; do not reuse the mock StaffApi upload endpoint |
| C2 | app/photos/PhotoCorrectionBatch.kt; app tests PhotoCorrectionBatchTest.kt | Controller/store/UI integration |
| R | Release evidence document | app/build.gradle.kts, AGENTS.md, docs/LIVE-DESK.md, docs/DESIGN.md |

In rows using app/photos or feature/photos/..., expand against the full package paths above. Do not create a new Gradle module solely to hold a few classes. Rendering/scanning/controller code belongs in the app package; reusable pure geometry/state belongs in core:model; Compose belongs in feature:photos.

## Task 0: Establish the execution baseline

**Produces:** a recorded source snapshot and an explicit selection of executable tracks.

- [ ] Read this plan, both linked specifications and current AGENTS.md files. Record any actual conflict with current owner instructions.
- [ ] Inspect checkouts and branches without resetting or cleaning them:

        git -C /Users/wizops/DIPI/dipi-app status --short --branch
        git -C /Users/wizops/DIPI/dipi-app log -1 --format='%H %s'
        git -C /Users/wizops/DIPI/dipi-web status --short --branch
        git -C /Users/wizops/DIPI/dipi-web log -1 --format='%H %s'

- [ ] Run the portable offline characterization from this handoff:

        php docs/plans/photo-correction-handoff/characterize-applicant-submit.php /Users/wizops/DIPI/dipi-web

  Expected: six true assertions, synthetic_only=true, live_requests=0. Exit 2 means the reviewed source hash differs; inspect source before updating the probe. Never bypass the hash guard just to make it run.

- [ ] Record that the probe stubs storage, database, PDF and referral calls. It is not evidence of successful deployment or live photo upload.
- [ ] For Track A implementation, create a codex/photo-correction branch or an isolated worktree after protecting existing changes. Do not apply historical stashes.
- [ ] Run the baseline full debug-unit suite from section 13. Address a real baseline failure separately before attributing it to this feature.

## Track A: Android local correction

### Task A1: Pure geometry and source-bound edit state

**Files:** PhotoCorrection.kt, PhotoSuggestionPolicy.kt only when A5 starts; core:model test PhotoGeometryTest.kt.

**Produces:** PhotoGeometry.rotatedSize(width,height,clockwise): Pair<Int,Int>; PhotoGeometry.validate(stamp,recipe): PhotoRecipe; PhotoGeometry.rotateCrop(crop,oldWidth,oldHeight,delta): PhotoCrop; PhotoGeometry.isIdentity(recipe): Boolean. State types are defined in section 6.

- [ ] Write failing tests for rotation dimensions, all four golden vectors, crop bounds, identity normalization, crop mapping after rotation, integer overflow and invalid rotations.
- [ ] Include this boundary test using actual proposed APIs:

        @Test fun rejectsCropOutsideRotatedSource() {
            val source = PhotoStamp("0".repeat(64), 2, 3)
            val bad = PhotoRecipe(90, PhotoCrop(2, 0, 2, 2))
            assertThrows(IllegalArgumentException::class.java) {
                PhotoGeometry.validate(source, bad)
            }
        }

- [ ] Implement bounds using Long arithmetic before comparing or adding dimensions. For a clockwise 90-degree change, map a rectangle with x'=oldHeight-(y+height), y'=x, width'=height, height'=width. Compose this mapping for 180/270.
- [ ] Define editing/review helpers that reject source mismatch, increment editRevision and clear approval after every geometry change. Identity approval must not increase Ready count.
- [ ] Run:

        ./gradlew :core:model:test --tests '*PhotoGeometryTest'

  Expected: all vectors and rejected-input assertions pass.
- [ ] Commit only geometry/state files and tests with message: Add source-bound photo correction geometry.

### Task A2: Bounded original sources and cache invalidation

**Files:** PhotoSource.kt, PhotoLoader.kt, PhotoSourceTest.kt, PhotoBoundedLoaderTest.kt.

**Interfaces:** Keep PhotoLoader.load(applicantId): Bitmap? for existing callers during migration. Add loadSource(key: PhotoKey, forceRefresh: Boolean): PhotoSourceResult; invalidate(applicantId): Unit; clear(): Unit.

PhotoSourceResult is a sealed result with Ready(PhotoSource), Missing, AuthenticationRequired, UnsupportedImage, TooLarge and Unavailable. PhotoSource owns the original decoded Bitmap plus PhotoStamp; byte arrays used to calculate SHA-256 are released after decode. Avoid data-class toString methods that dump bytes.

- [ ] Add MockWebServer tests for an image, login HTML returned with 200, 403, 404, malformed image, oversized Content-Length, chunked content over 10 MiB and redirects outside the configured origin.
- [ ] Verify that failure is not cached, same-source requests coalesce, and clearing while a fetch is in flight cannot repopulate the previous session's cache.
- [ ] Use a generation counter and canceled jobs to implement clear. Each completion checks its captured generation before cache insertion or emission.
- [ ] Enforce redirect safety before following a redirect. Do not send cookies to another origin or accept arbitrary image URLs from a page.
- [ ] Distinguish thumbnail reads from the single full-resolution correction slot. Preserve existing photo-only GET behavior; do not add application-view or full-form fetches.
- [ ] Test with this required race:

        @Test fun clearPreventsOldSessionCacheInsertion() = runBlocking {
            val first = CompletableDeferred<Unit>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val loader = BoundedLoader<Int, String>(
                1, 1024, scope, { 1L }
            ) { first.await(); "old-session" }
            val pending = async { runCatching { loader.get(1) } }
            yield()
            loader.clear()
            first.complete(Unit)
            pending.await()
            assertEquals(0L, loader.cachedBytes())
            scope.cancel()
        }

  Implement BoundedLoader.clear as part of this task. Strengthen synchronization in the test with an entered-fetch latch so it does not rely on scheduling luck.
- [ ] Run the targeted network tests and existing PhotoBoundedLoaderTest. Commit with message: Add bounded photo sources and session-safe invalidation.

### Task A3: Render actual corrected pixels

**Files:** app/photos/PhotoRenderer.kt and app test PhotoRendererTest.kt.

**Interfaces:**

        class PhotoRenderer {
            suspend fun render(source: Bitmap, recipe: PhotoRecipe): Bitmap
            suspend fun writeJpeg(
                source: Bitmap,
                recipe: PhotoRecipe,
                destination: OutputStream
            )
        }

The caller owns the source and stream; the renderer owns and releases intermediate copies. Returned Bitmap ownership is documented and transferred to the caller.

- [ ] Create the 2 by 3 colored bitmap and assert that the 90-degree/crop output is exactly C,A,D,B in row-major order.
- [ ] Test transparent input becomes white-backed JPEG, identity rendering preserves dimensions, no upscaling occurs, and source pixels remain unchanged.
- [ ] Use integer right-angle mappings or an Android Matrix whose behavior is locked by golden tests. Do not use display-only graphicsLayer for the editor's corrected result.
- [ ] Run expensive work on Dispatchers.Default under one correction-work semaphore. Propagate cancellation; never convert cancellation into a Good/Committed result.
- [ ] Use the same recipe/render implementation for preview and JPEG export. A lower-resolution display may scale the already-correct result; it must not recalculate the crop differently.
- [ ] Run:

        ./gradlew :app:testDebugUnitTest --tests '*PhotoRendererTest'

- [ ] Commit with message: Render rotated and cropped photo pixels.

### Task A4: Encrypted, scoped drafts and safe migration

**Files:** core/datastore PhotoCorrectionStore.kt and PhotoCorrectionStoreTest.kt; app PhotoEditStore.kt and cleanup callers.

**Interfaces:** drafts(scope): List<PhotoDraft>; save(draft): Unit; clearCourse(scope): Unit; wipeAll(): Unit. Store methods perform atomic read-modify-write under a mutex/synchronization and report write failure to the caller.

- [ ] Follow the injectable encrypted-preferences pattern in WhatsAppStore, using a separate file dipi_photo_corrections. Do not share WhatsApp keys/profile state.
- [ ] Use an injectable SharedPreferences provider in JVM tests instead of requiring a working Android Keystore. Retain the project's existing encrypted-store approach for compatibility; check current Android guidance before adding a new cryptographic dependency or changing its key lifecycle.
- [ ] Test same applicant ID across two origins, two centres and two courses. Clearing one scope must preserve every other scope.
- [ ] Test that approved geometry, operation IDs and source stamps round-trip; corrupted ciphertext/JSON produces an explicit recovery/reset state, not a fabricated successful upload.
- [ ] Drop legacy unscoped flags rather than guessing a source identity. The old cropped boolean is not a reproducible rectangle and old uploaded booleans are not verified operation receipts. Do not migrate them as approved/committed.
- [ ] Delete the old preference payload only after the new store migration marker is committed. Show one short notice if nonempty legacy drafts were discarded: Review photo edits again.
- [ ] Logout and Erase all clear drafts. Session expiry cancels work and clears photo pixels while preserving only the encrypted recovery metadata permitted by current project rules; document this distinction and test it.
- [ ] Restoration maps SUBMITTING/VERIFYING to UNKNOWN. It never resumes a request.
- [ ] Run:

        ./gradlew :core:datastore:testDebugUnitTest --tests '*PhotoCorrectionStoreTest'

- [ ] Commit with message: Scope and encrypt photo correction drafts.

### Task A5: Conservative on-device scanning

**Files:** app/photos/PhotoScanner.kt, core:model PhotoSuggestionPolicy.kt, app/build.gradle.kts, PhotoScannerTest.kt, core:model PhotoSuggestionPolicyTest.kt.

**Interfaces:** PhotoScanner.scan(source: Bitmap): List<PhotoOrientationEvidence>; PhotoSuggestionPolicy.suggest(evidence,stamp): PhotoSuggestion.

Define PhotoPoint(x:Double,y:Double) and PhotoBox(x:Double,y:Double,width:Double,height:Double). PhotoOrientationEvidence contains clockwise:Int and faces:List<FaceEvidence>. FaceEvidence contains box:PhotoBox and nullable leftEye/rightEye/nose/mouth:PhotoPoint, all normalized within that rotated image. PhotoSuggestion contains recipe:PhotoRecipe?, reason:PhotoSuggestionReason and safeToApply:Boolean. Define reasons NONE, ROTATE, CROP, ROTATE_AND_CROP, NO_FACE, MULTIPLE_FACES and AMBIGUOUS. It contains no identity/embedding.

- [ ] Verify official ML Kit dependency guidance at execution time; the reviewed bundled artifact is com.google.mlkit:face-detection:16.1.7. Pin the chosen version. Do not introduce remotely downloaded code/model assets from the extension.
- [ ] Add evidence-only tests: no face, multiple faces, upright zero degrees, one clearly upright alternate angle, ambiguous alternates, upside-down without mouth/nose evidence, edge-clipped head and small centred face.
- [ ] In policy, confirm upright geometry when both eyes are sufficiently level, eye midpoint is above nose/mouth and the landmarks lie plausibly within the box. Use the extension's eye-level tolerance 0.35 as an initial tunable constant with tests.
- [ ] Keep a credible upright zero-degree result. Auto-apply a different orientation only when a single-face upright candidate is unambiguous. Do not infer upright from largest face area alone.
- [ ] Suggest portrait crop when face area is below 0.20. Use 0.6 face-height head/chin margins and fit a 13:14 crop within the image. Apply locally only when one face and all required margins fit; otherwise show a suggestion for manual adjustment.
- [ ] Process orientations sequentially. Scan a 640px working copy, retry up to 1024px only if no face is found. Scale evidence back to source dimensions before constructing the integer recipe.
- [ ] Cancel promptly between detector calls and close the detector on controller cleanup. Late results must match the captured source stamp and editRevision.
- [ ] Run policy tests without ML Kit and a controlled device detector smoke test separately. A device detector failure leaves manual editing functional.
- [ ] Commit with message: Add on-device photo correction suggestions.

### Task A6: Native grid, editor and review controller

**Files:** feature:photos UI files; app/photos/PhotoReviewController.kt; DeskViewModel.kt; DipiAppUi.kt; app tests PhotoEditorTest.kt and PhotoReviewScreenTest.kt.

**Interfaces:** PhotoReviewController exposes StateFlow<PhotoReviewUiState> and accepts PhotoReviewAction. UI has no repository/store access.

Define actions Open(scope,focusApplicantId), SetSearch, SetReviewFilter, SelectApplicant, Rotate, SetCrop, Undo, Reset, SaveDraft, Approve, ApproveSelected, Scan, CancelScan, ApplySafeSuggestions, ClearCourse and Close. Add shared-write actions only in C2.

PhotoReviewUiState includes the active scope, applicant list, draft map, filter/search values, selected applicant, editor draft, loading/error IDs, scan progress and local export progress. Keep Bitmaps in a separate bounded source/preview owner; never in a SavedStateHandle or a serializable UI-state snapshot.

- [ ] Replace the old photo screen callbacks with the controller state/actions. Keep navigation/state responsibilities in DeskViewModel small rather than adding the entire editor there.
- [ ] Implement responsive grid and controls from section 5 using LocalDipi theme tokens and existing typography. At narrow width, wrap/scroll action rows rather than clipping them.
- [ ] Build the crop interaction against the actual fitted image rectangle, excluding letterbox margins. Map touch deltas through pan/zoom back to source coordinates, then validate with PhotoGeometry.
- [ ] Add Compose tests for unreviewed labels, selecting a card, rotating then approving, editing after approval, Keep & next order and missing-image placeholders.
- [ ] Include this interaction assertion against the actual UI:

        rule.onNodeWithContentDescription("Rotate right").performClick()
        rule.onNodeWithText("Keep & next").performClick()
        rule.onNodeWithText("Ready 1").assertIsDisplayed()
        rule.onNodeWithText("Uploaded").assertDoesNotExist()

  The test supplies synthetic applicants and a controlled controller/fake source. No network or real photos.
- [ ] Test crop gestures at 1280dp landscape, 360dp portrait and increased font scale. All actions must remain reachable.
- [ ] Remove automatic seeding of suggested geometry into approved state and the false Photo looks fine fallback in photoNote.
- [ ] Run existing PhotoPanesTest and relevant navigation/card tests along with the new tests.
- [ ] Commit with message: Add native photo correction and review workflow.

### Task A7: Explicit export and local display integration

**Files:** app/photos/PhotoExport.kt, PhotoExportTest.kt, PhotoCorrectionFlowTest.kt, controller/UI callbacks and affected photo displays.

**Interfaces:** PhotoExport.writeOne(source,recipe,output): Unit; writeZip(entries,openSource,output,onProgress): ExportResult. ExportResult distinguishes Completed, Cancelled and Failed with a completed-file count. Inputs use PhotoKey/PhotoRecipe/PhotoStamp, not applicant names.

- [ ] Request ACTION_CREATE_DOCUMENT for image/jpeg or application/zip from the UI. Freeze the approved selection before opening the picker. Do not export a different course after the user changes context.
- [ ] On receiving a URI, refetch/verify source stamps, render one image at a time, and stream to ContentResolver output. Do not add storage permission or write directly into Downloads without the picker.
- [ ] Verify ZIP entry bytes with synthetic images and deterministic filenames. Test cancellation, permission denial, null output stream, mid-write failure and changed source.
- [ ] Show Saved locally after export. Export must not mutate PhotoWriteState to COMMITTED.
- [ ] Use approved source-matching local corrections in desk photo displays through an explicit display-photo loader. The editor's Original loader always returns raw source pixels. Never feed an already corrected preview back into the renderer as its source.
- [ ] Changing course/logout invalidates displayed corrected previews. Course ops remains read-only and must not inherit edit controls.
- [ ] Update the old mock upload path and aggregate pending counters so Track A exposes no misleading Queue upload control. Keep unrelated mock API fixtures intact until their callers are deliberately retired.
- [ ] Test that an approved correction is visible in the desk detail and Original remains unchanged in the editor. Test server sheets/PDFs are not relabelled as corrected.
- [ ] Run the full debug-unit suite. Commit with message: Export reviewed photo corrections and refresh local displays.

### Task A8: Local feature documentation and device gate

**Files:** AGENTS.md, docs/DESIGN.md, docs/LIVE-DESK.md and a dated implementation evidence note.

- [ ] Describe local correction/export accurately. Explicitly say shared DIPI photos and server PDFs are unchanged in Track A.
- [ ] Record bundled detector version, source limits, migration behavior, test commands/results and unresolved device-specific issues.
- [ ] Verify on Pixel C using synthetic images in a test/debug path that cannot send live writes: grid sizing, crop handles, four rotations, portrait crop, before/after, scan cancellation, export picker, process restart and session cleanup.
- [ ] Remove or keep synthetic fixture injection behind test/mock-only build configuration; release must use the live read route and no hidden test applicants.
- [ ] Do not mark the original server-wide replacement goal complete when shipping only Track A.

## Gate B: Owner approval before backend implementation

The necessary exception must cover:

- A new isolated dh_photocorrection module/routes and operation-receipt schema.
- Replacing the photo-storage portion of the existing applicant edit handler with the shared safe photo writer.
- Coordinating photo writes with course finalization/media copying.
- Read-only deployment verification and separately authorized staging/deployment.
- A decision about PDF regeneration. Default proposal for the first shared correction release: do not regenerate existing application PDFs automatically; state that limitation.

Without this exception, stop at the shared-write boundary and continue authorized local work. Never fall back to full-form replay.

## Track B: Dedicated photo-only backend operation

The names below are proposed. They do not describe existing live routes.

### Task B1: Isolated module and strict protocol

**Files, relative to backend root:** create sites/all/modules/dh_photocorrection/dh_photocorrection.info, dh_photocorrection.module, dh_photocorrection.install, inc/photo-correction.inc; tests/contract.php.

Use a separate module so the new operation/schema is isolated from the large existing module and does not guess an unused update number in a module that currently has no .install file. Depend on dh_manageapp.

**Routes:**

| Method | Proposed path | Purpose |
| --- | --- | --- |
| GET | /app/{aid}/photo | Small photo-only Drupal form and protocol/source metadata |
| POST | /app/{aid}/photo | Apply one approved correction recipe |
| GET | /app/{aid}/photo-result/{operationId} | Read-only durable result lookup |

**Produces in B1:** dh_pc_validate_recipe(input,sourceInfo): array, returning either ok=true with the canonical recipe or ok=false with a bounded code. B2 consumes this validator. Define sourceInfo with width, height and sha256; validate the source fields in the form against it.

The GET form includes read-only data attributes for protocol, applicant ID, centre ID and course ID. They are parsed by Android for context matching, not submitted as overrides. Define source_revision as a server-computed SHA-256 of the internal current URI plus a newline plus the encoded-source SHA-256. It exposes no storage path. Task B3 makes referenced objects immutable so a pointer/version check has a meaningful concurrency guarantee.

**Successful form fields:**

    form_id=dh_photocorrection_form
    form_build_id=<Drupal generated>
    form_token=<Drupal generated>
    protocol=1
    operation_id=<canonical UUID>
    source_revision=<opaque revision from server>
    source_sha256=<64 lowercase hex>
    source_width=<positive integer>
    source_height=<positive integer>
    clockwise=0|90|180|270
    crop_enabled=0|1
    crop_x=<nonnegative integer when enabled>
    crop_y=<nonnegative integer when enabled>
    crop_width=<positive integer when enabled>
    crop_height=<positive integer when enabled>
    op=Save photo

No file upload, a_*, ae_*, aa_*, ac_*, al_*, status, centre override, course override, object path or external URL fields are accepted. Route identity and server records determine scope. Reject unexpected domain fields instead of silently ignoring an attacker/user mistake. Allow only the known Drupal transport controls required by the rendered form.

**Result HTML contract:**

    <section id="photo-correction-result"
      data-protocol="1"
      data-applicant-id="999999"
      data-operation-id="11111111-1111-4111-8111-111111111111"
      data-state="committed"
      data-output-revision="opaque-server-revision"
      data-output-sha256="0000000000000000000000000000000000000000000000000000000000000000"
      data-width="260"
      data-height="280">
      <p class="message">Photo saved.</p>
    </section>

The zero hash is a syntactic example; test/server results use the actual output hash. Output metadata is present only for a verified committed/no-op result. Other states are pending, failed or conflict with a short machine code and the normal server message. HTTP status alone does not mean committed.

- [ ] Implement route access using the existing %app_id loader, edit application permission and fresh course finalization checks. Validate access on result lookup too, including the operation's applicant/actor context.
- [ ] Let Drupal generate/validate form tokens. A GET must never initialize credentials or write application/photo records.
- [ ] Parse recipe fields into typed integers with size/range checks. Reject arrays, scientific notation, overflow, negative crop sizes and unsupported protocol versions.
- [ ] Add offline contract assertions:

        function expectRejected($changes, $expectedCode) {
            $hash = str_repeat('0', 64);
            $source = array('width'=>260, 'height'=>280, 'sha256'=>$hash);
            $valid = array(
                'form_id'=>'dh_photocorrection_form',
                'form_build_id'=>'synthetic-build', 'form_token'=>'synthetic-token',
                'protocol'=>'1',
                'operation_id'=>'11111111-1111-4111-8111-111111111111',
                'source_revision'=>$hash, 'source_sha256'=>$hash,
                'source_width'=>'260', 'source_height'=>'280',
                'clockwise'=>'0', 'crop_enabled'=>'1',
                'crop_x'=>'0', 'crop_y'=>'0', 'crop_width'=>'260', 'crop_height'=>'280',
                'op'=>'Save photo'
            );
            $result = dh_pc_validate_recipe(array_replace($valid, $changes), $source);
            if ($result['ok'] !== false || $result['code'] !== $expectedCode) {
                throw new RuntimeException('Expected rejection: '.$expectedCode);
            }
        }
        expectRejected(array('clockwise' => 45), 'invalid_rotation');
        expectRejected(array('a_status' => 'Confirmed'), 'unexpected_field');
        expectRejected(array('photo_url' => 'https://example.invalid/x'), 'unexpected_field');
        expectRejected(array('crop_width' => -1), 'invalid_crop');

  The pure validator checks field shape and values; separate Drupal integration tests check the authenticity of the tokens. No Drupal bootstrap or live DB connection is used in this contract test.
- [ ] Add staging integration cases for login/permission/CSRF/finalized rejection and confirm zero writes on each rejection.
- [ ] Commit with message: Add isolated photo correction form contract.

### Task B2: Durable operations and immutable photo storage

**Files:** dh_photocorrection.install, inc/photo-storage.inc, tests/storage.php, tests/operations.php.

**Consumes:** dh_pc_validate_recipe from B1. **Produces:** dh_pc_prepare_operation(applicantId,actorId,operationId,recipe,sourceRevision): array; dh_pc_commit_operation(operationId): array; dh_pc_read_operation(applicantId,actorId,operationId): array.

These functions call injectable/testable repository and storage adapters. They never call the applicant form submit handler.

Create a dh_photo_operation table with:

| Column | Meaning |
| --- | --- |
| operation_id | Primary key, UUID |
| applicant_id, actor_id, centre_id, course_id | Server-derived context |
| request_hash | SHA-256 of canonical protocol/source/recipe values, excluding expiring form tokens |
| expected_uri, expected_sha256 | Internal source identity; never returned as a storage URL |
| recipe_json | Only protocol, rotation and crop integers |
| state | preparing, staged, committed, failed or conflict |
| result_uri, result_sha256, result_width, result_height | Private result metadata |
| error_code | Bounded machine code; no applicant data |
| created_at, updated_at | Operation timestamps |

Use a unique operation ID and compare request_hash plus actor/applicant identity on reuse. Same ID with different input is a conflict. Do not store full HTTP bodies, session tokens, image bytes or health/ID fields.

- [ ] Write adapter-based tests for storage failure, image failure, duplicate operations, changed source, database failure after staging and response loss after commit.
- [ ] Stream/decode the server-owned source with the same 10 MiB/8 MP limits, recipe coordinates and no implicit EXIF policy as Android. Check each rotate/crop/save return value.
- [ ] Preserve the current object. Write a new private immutable key under a revision prefix derived by the server, not a user path. Verify stored checksum and image dimensions before pointer changes.
- [ ] Persist preparing/staged outcomes so abandoned operations can be reconciled. An abandoned preparing operation may only be marked failed if it provably did not commit; otherwise inspect its durable receipt/pointer.
- [ ] Use a short database transaction for finalization check, current source-pointer comparison, a_photo update and committed receipt. No normal applicant-form updates occur.
- [ ] Preserve this ordering:

        validate request and permissions
        claim/reuse durable operation ID
        fetch and verify original source
        render and stage a new private object
        verify staged object
        begin transaction
        revalidate course/access and compare source pointer/revision
        update only dh_applicant.a_photo
        mark operation committed
        commit transaction
        return committed receipt

- [ ] Keep S3 writes outside the short final database transaction. Conditional creation prevents overwriting a revision object; the database comparison prevents publishing a stale result. Current-object immutability depends on Task B3, not on a pointer check alone.
- [ ] On final transaction failure, retain the current pointer and mark/reconcile the staged orphan. Do not delete a result that may already be referenced.
- [ ] Test that only a_photo and operation/audit records change. Normal applicant timestamps are also outside the permitted write set unless separately approved.
- [ ] Test no-op corrections: no image rewrite and no accidental repeated JPEG degradation. Return a verified no-op/committed result for the same source with a receipt.
- [ ] Commit with message: Add recoverable photo-only storage commits.

### Task B3: Coordinate legacy writers and finalization

**Files:** existing dh_manageapp/inc/application.inc photo block; dh_manageapp/inc/course.inc finalization media path; new module's storage adapter and tests/concurrency.php.

This is an activation gate, not optional hardening. The existing writer uses a fixed object key and can change bytes while a_photo stays identical.

- [ ] Enumerate all active writers again, including application creation, legacy edit, finalization copies, scheduled jobs and any external storage maintenance. Do not enable shared correction if an active writer can overwrite the same revision objects outside this protocol.
- [ ] Replace only the legacy edit handler's photo storage block with the shared immutable-object/pointer writer while the module is enabled. Preserve the general form's unrelated behavior; the new photo route never calls it.
- [ ] Legacy photo replacement receives a server-generated operation ID and stages a new immutable object. It must not overwrite the existing fixed key. Surface storage failure instead of claiming photo success.
- [ ] Initial application creation may keep an initial key only if it is never subsequently overwritten; route later replacements through the shared writer.
- [ ] Serialize the publication of photo revisions with finalization's media snapshot. Both operations must participate in the same course-level coordination; a flag checked only once before processing is insufficient.
- [ ] Prefer the deployed database's connection-scoped advisory lock for course media coordination if supported and verified. Acquire the course lock before the applicant operation, release in finally, use a bounded wait, and fail with course_busy when another operation owns it. Do not rely on an unrenewed expiring Drupal lock during long storage/finalization work.
- [ ] If the database lacks a proven advisory-lock mechanism, implement a tested shared fencing/lease protocol before activation. Do not silently replace it with a local-process mutex. Record this as a deployment prerequisite if support cannot be verified.
- [ ] Concurrency tests must run two independent workers/connections: two corrections; correction versus legacy replacement; correction versus finalization; worker death while preparing; worker death after transaction commit.
- [ ] Assert that an older source never wins after a newer photo is published and that finalized student archives cannot capture a half-published correction. If these assertions fail, keep the capability off.
- [ ] Do not invoke finalization in production to test the lock. Use a disposable staging course.
- [ ] Commit with message: Coordinate photo writers and finalization snapshots.

### Task B4: Backend invariants and deployment evidence

**Files:** tests/invariants.php, tests/fixtures.php and a backend deployment/runbook note.

- [ ] Build synthetic records covering New/Old, Expected/Confirmed, Attended/Left, chair+backrest, long-course fields, unusual name casing, non-ASCII/multiline values, legacy empty fields and finalized course.
- [ ] Snapshot every non-photo applicant field and all related-record tables. Execute only the new correction handler. Compare exactly afterward; exclude only a_photo and the new operation/audit records.
- [ ] Assert zero calls to dh_ma_applicant_form_submit, set_referral, status handlers, attendance mutation helpers and dh_send_letter.
- [ ] Prove storage failure does not change a_photo; DB failure does not publish an orphan; identical operation replay returns the original result; changed payload with the same ID conflicts.
- [ ] Verify deployment facts read-only: deployed commit, PHP version/extensions, image toolkit, private storage SDK, checksum/conditional-write support, database transactions/advisory locks, route access and schema installation.
- [ ] Keep a module/config capability switch disabled until route, storage, concurrency and invariant tests pass in staging. Do not expose Enable on Android as a way to bypass this gate.
- [ ] Prepare exact migration/deployment/rollback commands for the actual deployment tooling discovered. Do not invent drush or SSH credentials/paths.
- [ ] Rollback disables the capability first, drains/reconciles operations and removes routes only after writers are idle. Preserve revision objects, a_photo pointers and operation receipts. Do not automatically restore old photos or drop the receipt table.
- [ ] Commit reviewed code and runbook. Request deployment authorization only after those concrete artifacts exist.

### Task B5: PDF behavior

Default first-release behavior is photo replacement only. Existing server-generated application PDFs may still contain the older photo.

- [ ] State this in the shared-edit result/help and release notes without implying server PDFs refreshed.
- [ ] Do not call create_application_pdf automatically from the correction operation.
- [ ] If the owner separately requests PDF refresh, make it a separate tracked operation permitted to update a_uri only, with immutable PDF storage and its own outcome. Review the existing PDF helper's failure behavior first; it also updates its reference after attempted storage.
- [ ] A PDF refresh failure must not make Android reapply a photo correction.

## Track C: Android shared correction, after backend activation

### Task C1: Narrow capability/form/result client

**Files:** core/network PhotoCorrectionClient.kt and PhotoCorrectionClientTest.kt.

**Interfaces:**

        interface PhotoCorrectionClient {
            suspend fun prepare(key: PhotoKey): PhotoCorrectionForm
            suspend fun submit(
                form: PhotoCorrectionForm,
                draft: PhotoDraft,
                operationId: String
            ): PhotoOperationResult
            suspend fun result(
                key: PhotoKey,
                operationId: String
            ): PhotoOperationResult
        }

Define PhotoCorrectionForm as a non-serializable in-memory object containing matched context, protocol/source stamp/revision, same-origin action and Drupal form tokens. Define PhotoOperationResult with Pending, Committed(output stamp/revision), Failed(code,message) and Conflict(code,message). HTTP ambiguity throws/returns a transport outcome that the controller maps to UNKNOWN.

- [ ] Add MockWebServer fixtures for the exact protocol in B1. Validate route/applicant/centre/course context, protocol version, source hash and action origin.
- [ ] Construct the body only from the allowlisted photo fields. Never serialize an arbitrary HTML form map or reuse the generic applicant-form parser.
- [ ] Reject unknown protocol, changed source, missing tokens, duplicated critical form controls, external action, login HTML and an unrelated success page.
- [ ] Disable automatic POST retry and redirect replay. Follow only expected same-origin read-only result locations; reject 307/308 replay to any other path.
- [ ] Test that the request body contains no a_*, ae_*, aa_*, ac_*, al_* fields. Assert that no /app/{aid}/edit or /staff request is emitted on any error path.
- [ ] Missing capability/404 keeps Export available and explains that shared correction is unavailable. It never triggers the extension's fallback.
- [ ] Run:

        ./gradlew :core:network:testDebugUnitTest --tests '*PhotoCorrectionClientTest'

- [ ] Commit with message: Connect Android to the photo-only correction protocol.

### Task C2: Per-photo batch and explicit recovery

**Files:** app/photos/PhotoCorrectionBatch.kt, PhotoCorrectionBatchTest.kt; controller, store and UI integration.

**Interfaces:** start(drafts: List<PhotoDraft>): Unit; stopAfterCurrent(): Unit; verifyUnknown(key,operationId): Unit. Expose a StateFlow of per-photo outcomes and completed/total counts.

- [ ] Freeze scope/source/recipe/editRevision at explicit Start. Reject mixed scopes, duplicate applicant IDs, identity recipes, unapproved drafts and unresolved operations.
- [ ] Persist operationId and SUBMITTING before the request can leave the device. If that persistence fails, send nothing.
- [ ] Sequence each item:

        APPROVED -> PREPARING
        fresh form and matching source -> persist SUBMITTING
        submit once -> VERIFYING
        committed receipt plus fetched matching photo -> COMMITTED

- [ ] Persist UNKNOWN on interruption after submission starts. Preserve the same operation ID. Never issue a second correction automatically.
- [ ] Read-only result lookup can recover a committed receipt. Fetch the latest source and compare the returned SHA-256/dimensions with the receipt before rebasing geometry.
- [ ] If the receipt committed but the current source is newer, mark SUPERSEDED and refresh display; do not overwrite the new source or reapply old geometry.
- [ ] A 404 result lookup after an uncertain POST is not proof that no write occurred. Keep UNKNOWN unless the server's durable protocol establishes a terminal failure.
- [ ] Mark only the current matching operation committed. Do not use the old positive-count loop to mark the rest of a batch uploaded.
- [ ] Stop after current completes its known outcome; untouched items stay Ready. Screen lock/app background interrupts new submissions. Do not automatically resume on foreground, restart, course change or login.
- [ ] Tests use a fake PhotoCorrectionClient and source loader with controlled latches. Required scenario: two approved photos, first commit succeeds, second connection drops after POST; assert first COMMITTED, second UNKNOWN and exactly one submit per ID.
- [ ] Test logout while awaiting a response: clear pixels, prevent old-session UI/cache insertion, and preserve only the recovery metadata allowed by the store policy.
- [ ] Commit with message: Track verified photo corrections and recover uncertain outcomes.

### Task C3: Controlled end-to-end pilot

- [ ] Obtain one explicitly approved staging/test applicant ID and a synthetic source photo. Record centre/course, expected source checksum and approved recipe.
- [ ] Snapshot non-photo fields/related records in staging. Run Android preview -> explicit correction -> readback. Compare server pixels/geometry and unchanged fields.
- [ ] Repeat with a connection drop immediately after commit, process death, source replacement by a second client, stop-after-current and expired login.
- [ ] Confirm that the tablet returns to an actionable result view and no subsequent photo starts after failure/unknown.
- [ ] Verify course/centre isolation using separate synthetic cases. Do not switch to real students to solve a fixture problem.
- [ ] Record app/backend versions, device/OS, request counts, outcome counts and checksums only. No full applicant forms or real photos in committed evidence.
- [ ] Keep shared correction unavailable if any preservation, concurrency or readback assertion fails.

## 13. Verification and release commands

### Standard Android suite

Run from the Android checkout:

    ./gradlew :core:model:test :core:audit:test \
              :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
              :app:testDebugUnitTest

Use targeted test filters during each task, then the complete suite once the implementation is integrated. Do not rerun unchanged broad suites repeatedly without a new failure/change.

### Backend tests

The proposed test files must run without a production bootstrap:

    php sites/all/modules/dh_photocorrection/tests/contract.php
    php sites/all/modules/dh_photocorrection/tests/storage.php
    php sites/all/modules/dh_photocorrection/tests/operations.php
    php sites/all/modules/dh_photocorrection/tests/invariants.php

Database-concurrency and full Drupal form integration tests require an explicitly configured disposable environment; refuse to run when its database identity matches production or no test-environment marker is configured. Document the exact environment invocation when it exists.

### Version, assemble and install

- [ ] Recheck current versionName/versionCode and published release before choosing the next version. Do not reuse an installed versionCode.
- [ ] Update AGENTS.md and release notes around the actual shipped scope.
- [ ] Assemble:

        ./gradlew :app:assembleRelease

- [ ] Verify the APK package/version, signing identity and checksum using the installed Android build-tools. Confirm USE_MOCK=false and live base URL in release configuration.
- [ ] Check/reconnect the tablet, then install without clearing its data:

        /Users/wizops/Android/Sdk/platform-tools/adb devices -l
        /Users/wizops/Android/Sdk/platform-tools/adb connect 10.0.0.144:5555
        /Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 install -r app/build/outputs/apk/release/app-release.apk
        /Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity

  Pixel C USB serial: 5C01001294, Android 8.1. Re-discover the address if DHCP has changed. Do not use a destructive reinstall to bypass a signing/version mismatch.

- [ ] Run the device checks for the shipped track. Do not capture/log unrelated student health or identity fields.
- [ ] Merge reviewed implementation into main and push only when authorized by the active request. Never delete unmerged work during cleanup.
- [ ] Publish a GitHub release to https://github.com/kapaggar/dipi-app/releases with both dipi-staff-<version>.apk and dipi-staff.apk containing identical bytes; mark it latest.
- [ ] Verify the published assets/download, not merely the local APK. Permanent link: https://github.com/kapaggar/dipi-app/releases/latest/download/dipi-staff.apk.

No signing key, local.properties, provisioning code, app cookie, real applicant data or screenshot belongs in the commit/release.

## 14. Completion checklist

### Track A complete means

- [ ] Real pixel rotation/crop and matching export, not a display transform.
- [ ] Usable tablet/phone editor and concise grid.
- [ ] Conservative on-device suggestions, cancellation and explicit approval.
- [ ] Source-bound encrypted drafts, isolation and cleanup.
- [ ] Local display integration without double correction.
- [ ] Export works and never reports Uploaded.
- [ ] Required tests and Pixel C validation pass.
- [ ] Release notes state local-only scope if no shared backend exists.

### Shared correction complete additionally means

- [ ] Explicit backend/deployment/pilot authorization is recorded.
- [ ] Photo-only handler accepts no applicant-form fields.
- [ ] Original images survive failed writes and are retained for recovery.
- [ ] Durable per-operation results and source conflict checks work.
- [ ] Legacy writers and finalization participate in the same safe protocol.
- [ ] No non-photo fields or related records change in invariant tests.
- [ ] Android readback and unknown-outcome recovery work without automatic resubmission.
- [ ] Controlled pilot passes; deployment capabilities are verified.
- [ ] PDF behavior is stated accurately.
- [ ] APK installed/published and assets verified for the approved scope.

## 15. Handoff status and first response expected from the next agent

At document creation, only research, design and planning artifacts exist. No photo feature implementation, backend change, live upload or release was performed for this task.

The next agent should start with a concise status statement naming:

1. The current Android/backend commits and any changed assumptions.
2. Which track is authorized.
3. The first task they will execute.
4. Any genuinely blocking approval/input, without repeating already granted permission.

Suggested owner prompt to accompany this handoff:

    Read docs/plans/2026-09-06-photo-correction-agent-handoff.md and its linked
    evidence. Implement the authorized local Android photo-correction scope
    task by task. Keep Drupal unchanged. Do not use full applicant-form uploads.
    Report the backend exception required before attempting shared corrections.

For a backend-enabled engagement, the owner must replace the unchanged-Drupal instruction with explicit approval covering Gate B. Merely handing over this document is not that approval.

## 16. Evidence bundle and official references

- [Native design](2026-09-06-native-photo-correction-design.md).
- [Backend research and route inventory](2026-09-06-photo-backend-research.md).
- [Portable offline characterization](photo-correction-handoff/characterize-applicant-submit.php).
- [Recorded synthetic results](photo-correction-handoff/characterization-result.json).
- [ML Kit Android face detection](https://developers.google.com/ml-kit/vision/face-detection/android).
- [Drupal image operations](https://api.drupal.org/api/drupal/includes%21image.inc/7.x).
- [Drupal Form API submit-handler selection](https://api.drupal.org/api/drupal/includes%21form.inc/function/form_execute_handlers/7.x).
- [AWS conditional object writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html).

Official references explain platform capabilities; they do not prove the deployed backend configuration. The offline probe is pinned to application.inc SHA-256 3c5587bd151e2311d491743a97b7fa6bce75c12793b724f5eee75762a62444a7 and exits before evaluating changed source.
