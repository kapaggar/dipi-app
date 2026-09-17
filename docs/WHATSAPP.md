# Centre-specific WhatsApp automation

## Approved boundaries

Android only; no Drupal or applicant-portal changes. Opt in per server and centre,
with separate administrator provisioning of the shared encryption material.
The owner accepted that a compromised provisioned tablet can expose a server key
that is not scoped to its centre. No real key belongs in the APK, repository,
logs, backups, test fixtures or exports.

Staff choose recipients from the current course's Expected/Confirmed population
and one active letter per batch. Review the recipients, numbers and personalised
sample before starting. Resolve duplicate numbers explicitly. The unlocked tablet
is dedicated to visible WhatsApp UI automation for the duration of the batch.

Read the existing active-letter listing and let Drupal render substitutions.
Do not invent an endpoint, change status, call server delivery functions, or visit
confirm/cancel links. Fetching the existing applicant letter viewer is not strictly
read-only: `dh_get_letter` can initialise missing applicant login/auth-code fields.

## Implementation

Implemented in Android; the backend is unchanged.

- Centre/server-scoped, disabled-by-default profile and separately provisioned
  shared key in Keystore-backed encrypted preferences. Erase-all removes them.
- Active managed-letter discovery through the authenticated desk session, with
  centre/origin validation and active membership rechecked before every render.
- PHP-compatible applicant-link encryption verified with synthetic keys. The
  public applicant viewer uses a separate client with no desk cookies, redirects,
  caching or request logging. HTML and personalised text stay in memory.
- Calling selection follows its existing filters and Expected/Confirmed scope.
  Invalid numbers cannot be selected. Shared numbers require explicit consent
  to separate messages, or staff can deselect individual applicants.
- A selected active letter, recipient/number review and personalised sample are
  required before Start. Existing manual messaging remains available when off.
- Accessibility sending verifies the full numeric recipient and exact composer,
  persists SendStarted before one click, then requires a new outgoing message.
  A saved contact name alone fails closed. No fixed coordinates are used.
- The body is written through the composer accessibility action, not the launch
  URL, to avoid putting personalised text or bearer links in activity logs.
- Pause/Stop controls, interruption detection and encrypted metadata recovery.
  Each course has independent encrypted batch progress.
  Unknown outcomes never retry automatically; recovery is always explicitly
  initiated. Messaging progress does not change calls, attendance or desk status.
- A labelled Message yourself check gates enablement for the installed WhatsApp
  version. Updating WhatsApp invalidates readiness until another successful check.

## Validation

The required regression command passes: 723 tests, zero failures/errors/skips
(model 131, audit 27, network 155, datastore 17, app 393). Coverage includes
synthetic PHP vectors, Unicode/links, unresolved templates, active-letter and
origin checks, invalid/duplicate phones, filtered Calling selection, encrypted
scope isolation, restart/logout/course changes and interruption after Send.

Android 8 uses a stricter regex parser: literal closing brackets/braces are
escaped. Applicant HTML reads use bounded buffered reads instead of Java's newer
InputStream.readNBytes. Release APKs are tested on the actual Pixel C.

## Pixel C pilot - 2026-09-06

Device: Pixel C, Android 8.1, WhatsApp 2.26.34.81. Only labelled messages to the
owner's Message yourself conversation were sent. No applicant was messaged.

The actual accessibility service verified self-chat identity, set and re-read the
exact composer text (Hindi, paragraphs and a complete query-string URL), pressed
Send once and observed the new outgoing message. The successful check is stored
against the installed WhatsApp version. Existing numeric unsaved-contact headers
were inspected without sending. Saved-name-only chats remain unsupported and stop
for manual review; a display name is never treated as phone-number proof.

Android 8 cached recycled WhatsApp text nodes after Send. The observer now refreshes
row/child nodes and includes non-important container views before comparing text
and outgoing status evidence. UI Automator inspection disconnects accessibility
services, so no UI Automator probe runs during the successful self-test. Earlier
interrupted tests correctly stopped without automatic retries.

## Single-code provisioning (owner refinement)

Eligible desk admins receive one constant `DIPI-WA1` provisioning code separately.
It packages the existing effective AES key and IV, with a version and typo checksum;
it does not invent a replacement server key or change PHP. Two distinct secrets
cannot be replaced by hashing an arbitrary new password without changing the server.
The existing two-field pilot storage migrates when first used, removing its old
entries. Neither the code nor its decoded material belongs in an APK or Git commit.

An authorised administrator can create an owner-only file outside the repository:

```bash
python3 scripts/create-whatsapp-provisioning-code.py --output /private/tmp/dipi-whatsapp-code.txt
```

The helper prompts privately for the existing values. It can instead read a trusted
local backend source with `--legacy-php /path/to/dh_manageapp.module`, without
executing or changing it. It never prints the code or sends email. Distribution
is handled separately by the administrator; possession grants the same shared-key
access as the previous key/IV pair. The Android app has no code export function.

## Deployed route verification

The Pixel C loaded 45 active letters from its own centre listing, selected that
centre's existing confirmation/cancellation letter and rendered one eligible
applicant's personalised sample with its complete HTTP link. The review showed
one recipient; Start was never pressed. The sample was closed and discarded.
No applicant identifiers, phone numbers, body text or bearer URLs are recorded here.

The desk can close an idle pooled connection before the next GET. Letter clients
use their own fresh-connection pools, with the existing desk timeout budget, so
this does not require automatically replaying a letter request. Certificate and
hostname verification remain enabled. No desk cookies enter the applicant client.

## Release validation

DIPI Staff 1.43.1 (`versionCode` 80), signed arm64 release APK. All 723 required
regression tests pass with no failures, errors or skips. The release build passed
its controlled self-test on Pixel C / Android 8.1 / WhatsApp 2.26.34.81. Screen lock
and switching apps stopped the run and invalidated the test until staff explicitly
started another successful check. The successful device check survived a cold
start, and automation remained off.

The first live letter preview succeeded with fresh connections. No applicant was
messaged. The separately provisioned code and both original secret values were
checked to be absent from the APK. The release assets `dipi-staff-1.43.1.apk` and
`dipi-staff.apk` are identical, with SHA-256:

`1348b5272a15c47a4f9533e0aa7d46a6ed193892056b523067a1b2e12b9fbf8f`

## QA follow-up - 1.43.2

- Setup: one enable switch, visible code/permission/test readiness, and collapsed
  setup controls after provisioning. Android permission status refreshes on return.
- Calling: numbered recipient/message/review steps, remembered letter behind
  Change letter, and an explicit completion/progress screen without a second
  preparation form underneath an unfinished batch.
- Root-cause finding: neither the original self-test nor batch runner explicitly
  returned to the desk activity. Completion now persists progress, clears the
  accessibility observer, then returns to the existing DIPI activity if WhatsApp
  is still visible. It never takes focus from an unrelated app or locked screen.
- Refresh the root and composer/header nodes as well as outgoing message nodes;
  exact text verification remains required. A timeout reports only matching-row
  count and whether the composer cleared, never message text or URLs.
- Pixel C pilot: a longer synthetic self-chat message with Hindi, paragraphs and
  a complete link was observed successfully, and DIPI automatically regained
  focus and displayed the passed result. No applicant was messaged. Saved letter
  expansion/collapse was exercised on the tablet.
- Recovery regression tests distinguish completed submissions/skips from unknown
  outcomes after interruption. Unknown attempts remain ineligible for retry.
- The owner requested the existing provisioning string for other tablets; generated
  an owner-only file outside Git using the existing administrator helper. No email
  was sent and no backend source was modified.

Validation for this follow-up: 725 tests, zero failures/errors/skips, using the
required model/audit/network/datastore/app debug-unit suite.

## Long-letter timeout diagnosis - 1.43.3

The owner reproduced a timeout after delivery on the Pixel C. DIPI reported a clear
composer and zero matching outgoing rows. Inspection of the existing sent message,
without resending or saving edits, established a 4,139-character raw message with
12 bold delimiters, a 779-character collapsed accessibility preview, and a
4,127-character expanded display. Short/plain self-tests did not cover this case.

The observer now accepts exact display text with paired bold markers hidden, while
preserving all words, whitespace and complete URLs. A collapsed prefix is only a
candidate for expansion, never evidence of submission. After Send, a unique new
outgoing candidate may have its exact Read more accessibility control clicked.
The observer retains that row identity and requires its full expanded text and a
clear composer before recording submission. This preserves outgoing evidence when
the status icon leaves the viewport on expansion. Existing similar collapsed rows
make identity ambiguous and remain unknown; there is no automatic retry.

Synthetic regressions first failed against the original exact-string observation,
then passed for formatted Hindi, paragraphs, complete URLs, collapsed previews and
ambiguous/old candidates. The 3,806-character formatted Pixel C self-test passed
with automatic expansion, full observation and return to DIPI. No applicant was
sent another message; the owner's existing uncertain attempt remains for review.
The built-in test now uses 30 paragraphs to cover messages longer than this failure.

Final release validation: 729 tests passed with zero failures/errors/skips. The
4,706-character, 30-paragraph bold/Hindi/link self-test passed on the installed
1.43.3 release APK (versionCode 84), with automatic return and Submission observed.

## Course isolation and copy follow-up - 1.43.4

The open tablet showed the old centre-wide guard blocking a batch because another
course had unfinished progress. Batch keys now include course ID as well as the
hashed server/centre scope. The single legacy slot is moved atomically into its
recorded course before reading or writing any course batch. Existing uncertain
attempts stay uncertain; no batch resumes automatically. Discard removes only the
current course's progress, while Remove centre setup and Erase all still remove all
applicable batches.

The current batch's Discard control is above its bounded recipient list. Other
courses open directly in batch preparation. Regression tests cover the original
blocker, legacy migration when another course opens first, independent recovery,
targeted discard, origin/centre isolation and erase-all.

The owner also requested concise copy and hyphens throughout app-authored UI. Routine
descriptions were shortened or removed across desk, Calling, settings, reports,
teacher/student screens, photos and WhatsApp; consequential warnings remain brief.
Server responses and applicant content are unchanged. Backend code is unchanged.

Final validation: 732 tests passed with zero failures/errors/skips, and the signed
1.43.4 release APK (versionCode 85) was installed on the Pixel C. The September 16
course now opens recipient selection and the saved letter directly, without the
other-course unfinished-batch warning. Accessibility automation remains enabled.
No applicant messages were sent and no saved batches were discarded during this check.
