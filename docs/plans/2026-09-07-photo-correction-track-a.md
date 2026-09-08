# Photo correction Track A + desk write (1.45.1 / 89)

Track A local Android correction is kept. Owner **2026-09-07**: a corrected photo
must update the remote DIPI application. There is no photo-only backend write.
Full-form replay of `GET`+`POST /app/{id}/edit` is the only live write path.
PHP is unchanged. No `/app/{aid}/photo`. No live `/staff/*`.

## Authorized vs gated

- **Allowed:** Track A source-bound rotate/crop, encrypted drafts, on-device suggestions, review UI, document-picker export, local desk display of approved corrections, and **full-form replay for photo write only**.
- **Gated:** Track B (`dh_photocorrection`, `/app/{aid}/photo`), photo-only submit/recovery, mock `/staff/*` upload as a real path.

Offline characterization (`characterize-applicant-submit.php`) still reports six true full-form side effects, `synthetic_only=true`, `live_requests=0`. Replay **echoes the GET** and does not claim photo-only preservation.

## Implementation

| Task | Status |
| --- | --- |
| A1-A8 local correction | Done (1.45.0) |
| Form scrape `ApplicantEditFormParser` | Done |
| `ApplicantPhotoWriter` GET+multipart POST | Done |
| Review UI Update on desk | Done |
| Course ops blocked | Done |
| B1-B5 photo-only backend | Not started |

Write path:

1. `GET /app/{id}/edit` (themed `dh_ma_applicant_form`).
2. Parse every current control. Require `form_build_id`, `form_token`, `form_id`, file input, submit `op=Update`.
3. Fail closed if those are missing, the action is off-origin, the action has `?r=`, or a status control is `Approved`.
4. `POST` multipart to the form action: echoed fields + JPEG on the form's file part (live: `files[upload_photo]`).
5. On success, invalidate `PhotoLoader` for that id and reload `GET /show-photo/{id}`.
6. NPI stays in memory for that POST only. Logout / erase-all wipe drafts and the held form.

## Limits

- Source download 10 MiB streamed. Full decode 8,000,000 pixels. Thumbnail LRU 32 MiB.
- Geometry is on stored decoded pixels; no implicit EXIF rotation.
- Drafts live in `dipi_photo_corrections` (EncryptedSharedPreferences). Logout and Erase-all wipe them. Session expiry clears pixels and the in-memory form map.
- Legacy `dipi_photo_edits` flags are discarded, not migrated. Notice: `Review photo edits again`.
- Export filenames: `photo-{id}-corrected.jpg`.
- Course ops uses raw `show-photo` thumbnails and has no update controls.

## Residual side-effect risk

Echoing the GET is preservation of **what the form currently shows**, not a photo-only database write. The submit handler can still:

- collapse chair+backrest into one `special` radio
- title-case names and trim extras
- recompute health flags from text
- derive status from attending/left
- call referral and regenerate the application PDF
- update `a_photo` even if storage fails

HTTP success is not proof the new bytes landed in object storage.

## Tests

```
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

Never `./gradlew test`.
