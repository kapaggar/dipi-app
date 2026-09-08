# Photo correction: backend research

Research date: 2026-09-06. Backend examined read-only at commit `d02296f79849dffa9b976f2e7b026a075aa1b538` in `/Users/wizops/DIPI/dipi-web`.

## Decision

There is no existing photo-only applicant write path in the checked source. Do not port the extension's full-form upload as a safe photo replacement. Its input-preservation checks cannot prevent side effects inside Drupal's submit handler.

Under the owner's unchanged-backend rule, Android can safely provide reversible local rotation/crop, reviewed previews and explicit JPEG export. Shared corrections on the DIPI server require an approved, isolated photo-only server operation. That operation is proposed below; it does not exist yet and was not implemented or probed live.

This conclusion supersedes the guarded full-form upload recommendation in the initial native photo correction proposal.

## Existing routes and alternatives checked

All source locations below are relative to the backend checkout.

| Path or mechanism | Source | Finding |
| --- | --- | --- |
| `GET /show-photo/{aid}` | `sites/all/modules/dh_manageapp/dh_manageapp.module:680`, callback at 2526 | Reads `a_photo` and streams a private storage object. No write branch or conditional photo revision contract. |
| `/app/{aid}/edit` | `dh_manageapp.module:352`; `inc/zero-day.inc:137`; `inc/application.inc:877` | Full applicant form, validation and submit. The photo is handled near the end, after unrelated writes. |
| `/app/{aid}/edit/{ctools_js}` | `dh_manageapp.module:357`; `inc/zero-day.inc:173` | Modal transport for the same form and submit handler. Not a photo-only shortcut. |
| Form with fewer fields or another `op` | `inc/application.inc:674`, 821, 877; `includes/form.inc:1099`, 1505 | Only an Update submit control is defined for editing. No dedicated photo submit handler. Partial fields are rejected by form validation or reach a handler that derives values from missing input. Client parameters cannot safely select a nonexistent photo-only handler. |
| Services `post-application` photo data | `sites/all/modules/dipi_api/dipi_api.module:106`, 810, 1491 | Creates a new applicant, sends letters, creates related records, then saves its photo. It does not update an existing applicant's photo. Not a supported Android desk route. |
| Generic Services file creation | `sites/all/modules/services/resources/file_resource.inc:161` | Creates a Drupal-managed file. No applicant photo association or call to DIPI's private-object storage writer. Not a replacement for `a_photo`. |
| `s3_put_file` | `inc/dana-s3.inc:4` | Internal PHP helper using server-held credentials. No desk-authenticated URL or presigned upload route was found. Direct tablet storage credentials are not an acceptable substitute. |
| Course finalization photo copies | `inc/course.inc:1007` | Copies applicant photos into finalized student archives while finalizing course records. Not a correction endpoint and must not be invoked for this task. |

The search covered photo references and storage-write call sites across the checked PHP/module/include source, not only filenames containing "photo". No alternate photo submission hook was found in the supplied module set.

## Confirmed full-form side effects

The source establishes the following:

1. **Combined seating flags are lossy.** The form collapses chowky, chair and backrest into one `special` radio, with backrest taking precedence (`inc/application.inc:636`). Submission zeroes all three flags and restores only the chosen one (`:1245`). Sending unchanged controls can therefore clear a valid chair/backrest combination.
2. **Names and extra data are transformed.** First/last names are lowercased then title-cased (`:1003`); `ae_*` values are trimmed (`:895`); health flags are recomputed from text (`:902`). These are not conditional on changing the photo versus changing the rest of the application.
3. **Other records are processed.** Attendance/status/confirmation logic occurs before photo handling (`:977`, `:1014`, `:1209`), and referral handling and related-record writes are part of the same submit. Certain long-course branches can send teacher letters (`:1179`, `:1300`).
4. **Storage failure is not surfaced as a failed applicant save.** The handler logs `s3_put_file` failure but still updates `a_photo` (`:1288`). It then calls PDF regeneration and continues to its normal redirect. A redirect or HTTP success is not proof of photo replacement.
5. **A full-form readback cannot prove all database fields were preserved.** Some fields are derived or represented lossily. It also cannot undo side effects already committed, recover overwritten image bytes, or eliminate the GET-to-POST race.

### Offline characterization

Executed the actual `dh_ma_applicant_form_submit` function extracted from the checked source, plus its original special-seating selection block, with synthetic data. Database, storage, referral, PDF and redirect calls were replaced with local test doubles. Drupal was not bootstrapped, no credentials were read, and no network or database connection was made.

Six assertions reproduced:

| Assertion | Result |
| --- | --- |
| An unchanged chair + backrest form becomes backrest only | Reproduced |
| Synthetic first name `McDONALD` becomes `Mcdonald` | Reproduced |
| Unchanged extra text is trimmed | Reproduced |
| Failed storage still leads to an `a_photo` update | Reproduced |
| Failed storage still reaches the normal redirect after the PDF call | Reproduced |
| Full save calls referral and PDF routines | Reproduced |

The test captured updates to `dh_applicant`, `dh_applicant_extra`, and `dh_applicant_attended`. It deliberately stubbed, rather than executed, the bodies of referral/PDF/storage routines. It is a submit-handler characterization, not a deployed end-to-end upload test. Runtime: local PHP 8.5.9; the deployed PHP version was not checked.

Portable probe: [characterize-applicant-submit.php](photo-correction-handoff/characterize-applicant-submit.php). It verifies the reviewed source hash before evaluating the extracted function. [Recorded synthetic results](photo-correction-handoff/characterization-result.json) travel with the handoff.

```bash
php docs/plans/photo-correction-handoff/characterize-applicant-submit.php /Users/wizops/DIPI/dipi-web
```

## Safe implementation with the backend unchanged

- Android fetches existing photos through `/show-photo/{aid}` and applies reversible local geometry for its own photo displays.
- The editor and approved preview use one rotation/crop renderer. The source remains intact. Store only source-bound, server/centre/course-scoped correction metadata; no full applicant form or face descriptors.
- Staff can explicitly export a corrected JPEG. Export does not update the desk site, other tablets, server-rendered sheets or existing application PDFs.
- Do not describe a browser upload as a photo-only safety fallback: the normal Edit page invokes the same full-save handler. It may remain an explicit manual operation, with the same known limitations.
- Do not add hidden full-form uploads, restore-after-save routines, alternate status calls, API creation requests, or direct storage access.

This meets local photo correction, not server-wide replacement. That distinction must be visible in the UI and release notes.

## Proposed safe shared correction path, requiring a backend exception

The narrowest useful addition is an independent photo-correction form/handler, for example `/app/{aid}/photo`. This is a proposed route, not a discovered endpoint. It must never call `dh_ma_applicant_form_submit`.

For rotation and crop, Android can submit the **correction recipe** instead of the entire form or a replacement application: right-angle rotation, crop rectangle, source revision and an operation ID. Drupal already includes `image_rotate` and `image_crop`; the existing server can transform its own stored image. Native and PHP renderers must agree on clockwise rotation, post-rotation crop coordinates and pixel rounding. [Drupal image API](https://api.drupal.org/api/drupal/includes%21image.inc/7.x).

Required contract:

1. **Server authority:** existing authenticated desk session, CSRF protection, applicant-centre/gender access checks, edit permission and finalized-course checks on every request. Reuse the existing applicant loader where applicable, and revalidate before committing. Android does not implement a parallel permission system.
2. **Narrow inputs:** accept only the operation ID, source revision, 0/90/180/270 rotation and a validated crop rectangle, plus form tokens. Server derives the applicant, centre, course, source object and destination key. No names, health, documents, attendance, status, arbitrary file paths or external image URLs are accepted.
3. **Validate before writing:** decode only supported image formats, bound bytes and pixel count, check crop bounds after rotation, and reject a changed source. No-op corrections do not rewrite the image.
4. **Preserve the source:** create a new private revision object instead of overwriting the current image. Check every image operation and storage result, and verify the new object before changing the active pointer. A storage failure leaves the current photo unchanged.
5. **Commit only the photo:** update `dh_applicant.a_photo` with a version/precondition check. Record a narrowly scoped operation receipt/audit entry without applicant form values or pixels. Do not write attendance, health, names, course history, referral or status.
6. **Concurrency and recovery:** serialize photo mutations and make operation IDs idempotent so a timeout/restart cannot apply rotation twice. Durable receipts distinguish failed, committed and pending outcomes. A failed conditional update leaves the previous pointer intact; an unreferenced new object is cleaned up safely.
7. **Legacy writers matter:** the existing edit handler overwrites a fixed object key. A comparison of `a_photo` alone cannot detect an in-place byte replacement. Robust guarantees across both workflows require routing legacy photo writes through the same revision/locking helper, or disabling that legacy writer for the correction rollout. A lock used only by the new route does not protect against a writer that ignores it. Storage conditional writes can enforce object preconditions, but do not replace the database pointer protocol. [AWS conditional-write documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html).
8. **Readback:** return the resulting photo revision/checksum, and let Android fetch the committed image before clearing its local geometry. Unknown results use read-only receipt/revision lookup, never a blind retry.
9. **PDF policy is separate:** the existing PDF generator writes `a_uri` and its storage object (`inc/pdf.inc:362`). A photo-only change leaves an already generated PDF stale unless regeneration is explicitly included. If included, use a separate controlled operation with its own result; never call the full applicant submit just to regenerate the PDF. No teacher/applicant letters should be sent by correction.

This is an isolated backend feature, not merely an Android request-format adjustment. New route, shared photo writer and durable recovery semantics require review and synthetic tests before deployment. The existing server-held storage credentials remain on the server.

## Acceptance evidence before enabling shared edits

- Exact before/after comparison of every non-photo field and related record for synthetic cases: new/old student, attended, left, chair+backrest, long-course, unusual name casing and multiline text.
- No calls to status transitions, attendance writers, referral updates or delivery functions from the correction handler.
- Changed photo, simultaneous corrections, legacy upload races, duplicate operation IDs, failed image decoding, failed storage, failed database commit, expired session, invalid CSRF, wrong centre, finalized course, and response loss after commit.
- Pixel C preview/server-image parity using asymmetric synthetic images and all supported rotations/crops.
- A specifically authorized controlled-record pilot before any real applicant batch.

## Research limits and repository state

No backend files were edited and no live write endpoints were called. The backend checkout has existing uncommitted files and local commits ahead of its configured origin; this investigation preserved them. The local source and extension behavior are evidence, not proof that the deployed site's commit, enabled modules, storage SDK, bucket policy/versioning or permissions match this checkout. Those details need read-only deployment verification before a shared correction feature is enabled.
