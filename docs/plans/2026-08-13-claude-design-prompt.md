# Claude Design prompt — DIPI Staff Android, Vertical 1 screens

Paste everything below the line into Claude Design. Output feeds the Compose
implementation after Vertical 1 approval.

---

Design high-fidelity mobile app mockups for **DIPI Staff**, a native Android app
(Material 3, Jetpack Compose) used by a **course registrar at a Vipassana
meditation centre** to manage course applicants from a phone or tablet, replacing
a desktop web dashboard.

## Who uses it and where

One registrar at a busy registration desk. Interruptions are constant; sessions
are short (30 seconds to 2 minutes). They look up an applicant, change a status,
and put the device down. Large touch targets, high scan-ability, minimal chrome.
The tone is calm and utilitarian — this is for a meditation centre: quiet
colours, no gamification, no decoration. Think "clinical but warm".

## Platforms

Design every screen twice:
- **Phone** (portrait, ~412dp wide): stacked navigation, one screen at a time.
- **Tablet** (landscape, ~1280dp wide): list-detail layout — worklist on the
  left pane, applicant card on the right pane.

Use Material 3 components (top app bar, filter chips, list items, bottom sheet,
snackbar, outlined text fields). Light theme primary; include one dark-theme
variant of the Today screen.

## Screens (6)

### 1. Login
Three fields: **Server URL** (pre-filled `https://`), **Username**, **Password**.
One primary button "Sign in". App logo/wordmark "DIPI Staff" above. Show an
error state variant: a server-returned message rendered verbatim under the form
(e.g. "Wrong username or password."). No sign-up, no forgot-password.

### 2. Centre & course picker
After login. A dropdown/selector for **Centre** (most registrars have exactly
one — show it pre-selected: "Dhamma Giri") and a list of **upcoming courses**,
each row showing course name, date range, and days-until-start. Example rows:
- "10-Day Course · 20 Aug – 31 Aug 2026 · starts in 7 days"
- "Satipatthana Course · 3 Sep – 12 Sep 2026 · starts in 21 days"
- "10-Day Course · 16 Sep – 27 Sep 2026"
Finalized/past courses do not appear at all. Tapping a course goes to Today.

### 3. Today (the home screen — spend the most effort here)
The worklist for the selected course. From top:
- Compact header: course name + date range, centre name small above it.
- A prominent **search field** ("Name, conf no, phone…").
- A horizontally scrolling row of **status filter chips with counts**:
  All (214) · Pending (61) · Received (48) · Confirmed (72) · Expected (18) ·
  Cancelled (9) · Rejected (6). Chips are multi-select filters.
- A **dense applicant list**, hundreds of rows, each row showing:
  - Name (primary), e.g. "Meera Deshpande"
  - Status as a small tonal badge, colour-coded (Confirmed = green tone,
    Pending = neutral, Cancelled/Rejected = muted red tone)
  - Conf number when present, monospaced, e.g. "NF128" or "OM42"
    (format: N=new/O=old student or S=server, then M/F, then a number)
  - Secondary line: "Student · Old · Pune, India" or "Sevak · New · Jaipur"
- Gender is shown per row (M/F) but there is **no gender or centre filter UI**
  beyond the status chips — the server already scopes the list.
Include a variant with **2 queued offline changes**: a thin banner or badge
"2 changes waiting to sync" (the app queues status changes when offline).

### 4. Applicant card (public card)
Detail view for one applicant. Tablet: right pane. Phone: full screen, back
arrow. Content:
- Name large, status badge, conf number if any.
- Facts as a scannable group: Student/Sevak, Old/New student, gender, age,
  city/state/country, mobile, email, home phone, date of birth, application
  date. A small "monk/nun" tag when applicable.
- **Deliberately absent** (do not invent them): no ID numbers, no medical
  info, no address street lines, no photo, no edit button, no attendance
  toggle. This card is intentionally thin.
- One primary action: **"Change status"** button, prominent, bottom-anchored
  on phone.
- Contact actions: tap-to-call / tap-to-email affordances on mobile & email.

### 5. Change-status bottom sheet
Opens from the applicant card. Contains:
- Current status shown as "Confirmed → " leading a **new-status selector**
  (list of choices: Pending, Received, Confirmed, Expected, Reconfirmation,
  Cancelled, Rejected, Custom — note: **no "Approve" option exists**).
- An optional **comment** text field.
- A quiet but persistent notice line with a mail icon:
  "The server may send the applicant a letter for this change."
- Confirm button. Show two result variants as toasts/snackbars:
  - Success: "Status updated · conf no NF129"
  - Server refusal, message verbatim: "Please Edit application and choose
    Area teacher before approving!" (rendered as-is in an error snackbar).

### 6. Settings
Minimal: signed-in user + centre, server URL (read-only), a **"TEST MODE"**
banner treatment shown when the server is in test mode (design this banner —
it should also appear as a persistent thin strip on all screens in test mode),
sync status ("Last synced 2 min ago"), and Logout.

## Global states to include
- Offline: Today list served from cache, subtle "offline — showing cached
  list" indicator, queued-changes badge.
- Loading: skeleton rows on Today.
- Server error: snackbar always shows the server's message text verbatim;
  the app never rewords or suppresses it.

## Hard constraints
- No bottom navigation bar — the app is a single flow (course → list → card).
- No admin features: no editing applications, no letters/mail UI, no seating
  or rooms, no attendance marking, no finalize.
- No "Approved" status anywhere.
- Sample data: Indian names and cities, mixed M/F, ~10 visible list rows.
- Material 3, Roboto or default type scale, restrained palette (suggest a
  deep neutral primary — slate/indigo family), AA contrast.

Deliver: 6 phone screens + tablet list-detail composite + dark Today variant
+ the bottom sheet + error/offline states listed above.
