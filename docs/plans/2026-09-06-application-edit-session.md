# Application edit session repair

## Diagnosis

The tablet showed a Drupal Page not found response and anonymous login block after
Save in the application edit viewer. The initial HTML is fetched through the app’s
authenticated OkHttp client, then loaded into a display-only WebView. The WebView
has no app cookies, but the HTML form’s controls and POST remain active.

The unchanged Drupal app_id_load checks the current user’s centre and gender
permissions. An anonymous request fails the route loader and produces 404 before
form submission. The screenshot is consistent with this deterministic session
split; app-cookie expiration is not required.

## Fix

- Replace the embedded Edit form with a labelled browser handoff.
- Open user/login with a relative destination of app/{id}/edit. Drupal preserves
  this destination after sign-in; its logged-in redirect also honours destination.
  An anonymous GET against the deployed login route returned HTTP 200 and retained
  the destination in the login form action. No application POST was made.
- Use the browser’s own session. Never transfer app cookies or credentials.
- Refresh the current worklist once after leaving for Edit and returning. Preserve
  the selected application and recompute audit from the refreshed repository data.
- Scope return handling to the same session/course; cancel it on logout or erase.
- Keep the backend unchanged and do not submit a real application during testing.

## Validation

Regression coverage: actual Edit tap bypasses the sheet viewer, destination URL and
credential-free browser intent, missing browser, refresh only after return, and no
refresh after switching course/session. Run the required JVM/debug-unit suite and
release build; install on Pixel C and inspect the browser handoff without saving.

Final validation: 736 tests passed with zero failures, errors or skips. The signed
1.43.5 release APK (versionCode 86) was installed on the Pixel C. Following the
original course’s honorific audit finding and tapping Edit opened Firefox’s real
Drupal login form with the correct application destination, rather than Page not
found. Returning to DIPI preserved the selected application. The browser requires
its own sign-in and was left on that page for the owner. No credentials were copied
and no application Save was submitted.
