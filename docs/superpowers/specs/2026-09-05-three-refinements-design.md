# Three approved desk refinements

**Date:** 2026-09-05  
**Release:** 1.41.0 (`versionCode` 68)

## Scope

Implement the three owner-approved candidates against the current shipped behavior. The downloaded `DIPI Sheets v5.dc.html` and `DIPI Course ops v6.dc.html` files are visual references only. Current `AGENTS.md`, `docs/DESIGN.md`, and `docs/DECISIONS.md` remain the product and transport authority.

## 1. Truthful cached-roll status

A successful teacher-list fetch may show the current `sheetClock()` value because the process observed that fetch. A roll restored from encrypted storage after a failed GET has no persisted fetch timestamp, so the UI must receive `teacherRollCachedAt = null` and use its existing generic “showing cached list” copy. The implementation must not infer cache age from process time and must not change cache schema, fetching, polling, or wiping behavior.

## 2. Closed desk-site browser handoffs

Two owner-approved controls become real system-browser handoffs:

- Advanced Search: `${BuildConfig.BASE_URL}/search-app/{centreId}`
- Add Application: `${BuildConfig.BASE_URL}/app/add/{centreId}/{courseId}`

Only these destinations are supported. URLs are derived from typed IDs already in app state and the trimmed build base URL. Opening uses Android `ACTION_VIEW` in the system browser. App session cookies and credentials are never copied. If no browser resolves the intent, the app shows the local error `No browser can open the desk site`.

The native cached Advanced Search stays unchanged. The two controls carry the approved trailing `↗` treatment. No StaffApi method, WebView, parser, repository call, `POST /search-app`, or generic arbitrary-route launcher is added. Existing sheet and retired desk-site destinations remain unchanged.

## 3. Desk snackbar error token

`DeskSnackbar` reads the active `LocalDipi.snackError` token for errors and keeps `Industry.accent800` for success. This aligns the desk component with the existing phone snackbar token while preserving the current default appearance.

## Validation

Each slice follows red-green-refactor with a test that fails against current production behavior. The final branch runs the repository’s five-module JVM suite, assembles debug and release APKs, and installs/launches the debug APK on the Pixel C because 1.41.0 is tablet-facing.

