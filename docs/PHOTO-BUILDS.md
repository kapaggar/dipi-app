# Photo capability builds

Photo review and corrected-photo upload are build-time options. The default is disabled. Both alternatives retain ordinary applicant photo display, the existing package `org.dhamma.dipi.staff`, signing configuration and Android 8.0 minimum.

## Build

From the repository root, the default compact release is:

```bash
./gradlew :app:assembleRelease
```

Explicitly select the same compact build:

```bash
./gradlew :app:assembleRelease -Pdipi.photoReview=false
```

Opt in to photo correction, review and the existing explicit live update workflow:

```bash
./gradlew :app:assembleRelease -Pdipi.photoReview=true
```

The property accepts only `true` or `false`. It is not read from `local.properties`. Enabled builds bundle the existing ML Kit face detector; compact builds exclude its dependency, models and native libraries. Neither build downloads replacement code or detector models.

All commands write `app/build/outputs/apk/release/app-release.apk`. Preserve or rename an APK before building the other alternative.

## Install or switch

Changing the flag requires building and installing another APK. There is no runtime toggle. Because both alternatives use the same package and signing, installing one updates the existing app; they cannot be installed side by side.

## Behavior and retained data

Compact builds hide correction/review entry points and reject controller actions, review navigation and repository photo uploads before network writes. Ordinary applicant photos continue to load; stored corrected drafts are not used for compact-build display. Course ops remains read-only in both alternatives.

Enabled builds retain source-bound encrypted drafts, local rotate/crop/review/export, and the explicit approved-photo update through the existing applicant edit form. Enabling the build does not upload anything automatically.

Installing the compact alternative is not an erase operation. Existing encrypted drafts retain their established scope and cleanup behavior. Logout, session-expiry handling and Settings **Erase all local data** keep their existing cleanup paths; use **Erase all local data** when intentionally removing retained local data.
