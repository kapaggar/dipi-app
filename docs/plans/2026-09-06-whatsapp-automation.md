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

## Implementation status - prerequisite work only

Branch: `codex/whatsapp-automation`. No runtime feature or release is enabled.

- Added metadata-only `ManagedLetter` and a non-serializable, redacted
  `RenderedLetter` holding personalised text in memory.
- Added `LetterLinkCipher`: matches Drupal's hexadecimal SHA-256 derivation,
  OpenSSL AES-256-CBC padding, and double Base64 using synthetic PHP vectors.
  It takes caller-provided secrets; it neither loads nor provisions real secrets.
- Added `ManagedLetterParser`: selects only `table-letters`, binds letter links
  to the requested origin/centre, rejects ambiguous identities, and extracts only
  the applicant viewer's `div.container > div.main > pre` body. Preserves Unicode,
  paragraph/list boundaries and full HTTP(S) links. Rejects empty/unresolved,
  oversized, interactive or unsupported content. It performs no network requests.
- Added jsoup 1.23.2 and NIO core-library desugaring for Android compatibility.

These components are not connected to Calling or Settings. Profile storage,
provisioning UI, authenticated fetching, batch selection/recovery and the
accessibility sender remain pending behind the pilot gate.

## Pixel C pilot - 2026-09-06

Read-only inspection on Pixel C, Android 8.1, WhatsApp 2.26.34.81:

- The official Android CLI layout probe returned an unrecognized instrumentation
  response. ADB UI Automator could inspect the accessibility hierarchy instead.
- The currently open chat exposes exactly one numeric
  `com.whatsapp:id/conversation_contact_name`, one full-text `entry`, and one
  enabled/clickable `send` node. The tablet uses a split conversation layout.
- This is evidence for the current unsaved-contact screen only, not proof of
  recipient verification for saved contacts, sending, or post-send observation.
- No Send button was pressed. No student was messaged. No live applicant letter
  was retrieved. Personal UI text and phone numbers are excluded from this file.

**Pending input:** a WhatsApp number controlled by the owner, explicitly designated
to receive a few labelled pilot messages. Do not substitute a student number.

## Pilot acceptance and remaining implementation

1. On the designated recipient, prove both numeric identity verification and exact
   composer text before sending. A saved contact requires verified phone identity;
   a display name or successful deep link alone is insufficient.
2. Send a unique labelled test message once, using the UI node action. Observe a
   newly added outgoing message with that exact text. Distinguish old matching
   messages and incoming messages. Record submission observed, never delivered.
3. Test lock, app switching, dialogs, unsupported hierarchy, disconnection and
   interruption after Send. Ambiguous send results must stop as outcome unknown
   without an automatic retry. If these observations are unreliable, stop the
   unattended implementation.
4. After the gate passes, implement a package-restricted accessibility service,
   centre-scoped protected profiles, memory-only letter fetching/preview, explicit
   batch selection and encrypted metadata recovery. Never resume on startup or
   logout. Messaging outcomes remain separate from calls and server status.
5. Verify the authenticated letter listing and applicant viewer against deployed
   behaviour before enabling real batches. Confirm active membership again before
   rendering; explicit-ID rendering does not itself exclude inactive letters.
6. Test centre isolation, duplicate/invalid numbers, disabled letters, Unicode,
   interruption and process death. Amend the user-facing design ledger only when
   UI exists. Bump a minor version and ship only after the successful pilot.

## Validation commands

```bash
./gradlew :core:network:testDebugUnitTest --tests '*ManagedLetterParserTest' --tests '*LetterLinkCipherTest'
```

The 10 targeted tests pass with synthetic data. The full required regression
command also passes: 693 tests across model (127), audit (27), network (150),
datastore (13), and app (376), with zero failures, errors or skipped tests.
This includes Android app compilation with the desugaring configuration. It does
not constitute the live pilot or verification of deployed routes.
