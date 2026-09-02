# Status vocabulary from the parsed select — spec (T3)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** repo scan 2026-08-31 — `SearchPageParser.parse` extracts the desk's
authoritative status list from the `edit-app-status` `<select>`
(`SearchPageParser.kt:49-51`, already covered by `SearchPageParserTest`), but
`StaffRepository.refreshApplicants` throws it away and instead derives
`lastStatuses` from whatever statuses happen to appear in the current roster
(`StaffRepository.kt:294`). A course whose roster shows only three statuses
offers only those three in the status sheet, even though the desk's select
lists the full vocabulary.

## Decision — prefer the parsed select, fall back to roster counts

Extract a pure function so the rule is unit-testable without the network stack
(house style: `SyncBanners`, `deskSaveSnack`):

`app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt` — add a
top-level internal function in the same file:

```kotlin
/**
 * The status vocabulary offered in the sheet: the desk's own select when the
 * page carried one, else whatever statuses the roster shows (the pre-1.28
 * behaviour, kept as the fallback for filtered fetches whose HTML fragment
 * has no form). Never invents entries; "Approved" is excluded downstream by
 * ApplicantStatus.mergeChoices, not here.
 */
internal fun deriveStatuses(parsed: List<String>, counts: Map<String, Int>): List<String> =
    parsed.ifEmpty { counts.keys.filter { it != "All" } }
```

In `refreshApplicants`, replace line 294:

```kotlin
// was: if (counts.keys.size > 1) lastStatuses = counts.keys.filter { it != "All" }
val derived = deriveStatuses(result.statuses, counts)
if (derived.isNotEmpty()) lastStatuses = derived
```

Everything downstream is already correct and unchanged: `loadStatuses()`
(`:240-243`) returns `lastStatuses`; `DeskViewModel:1251-1252` merges via
`ApplicantStatus.mergeChoices`, which already drops blanks and **filters
"Approved" case-insensitively** (`ApplicantStatus.kt:42-52`) — the
never-send-`Approved` rule needs no new code, but the tests below pin it.

## Tests

New file `app/src/test/kotlin/org/dhamma/dipi/staff/StatusVocabularyTest.kt`
(plain JUnit, no Robolectric — pure functions only):

```kotlin
class StatusVocabularyTest {
    @Test fun parsedSelectWins() {
        val out = deriveStatuses(
            parsed = listOf("Received", "Confirmed", "Cancelled", "Waiting List"),
            counts = mapOf("All" to 5, "Received" to 5),
        )
        assertEquals(listOf("Received", "Confirmed", "Cancelled", "Waiting List"), out)
    }

    @Test fun emptySelectFallsBackToRosterCounts() {
        val out = deriveStatuses(parsed = emptyList(), counts = mapOf("All" to 3, "Received" to 2, "Cancelled" to 1))
        assertEquals(listOf("Received", "Cancelled"), out)
    }

    @Test fun approvedFromServerNeverReachesTheSheet() {
        // The select may legitimately carry Approved; mergeChoices must drop it.
        val merged = ApplicantStatus.mergeChoices(deriveStatuses(listOf("Received", "Approved", "Confirmed"), emptyMap()))
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
    }
}
```

(If `deriveStatuses` is not visible from the test package, mark it
`@VisibleForTesting internal` — do not make it public.)

Never touched: `SearchPageParserTest` (already asserts the select parse),
`ApplicantStatusTest`, `StatusSheetTest`, `mergeChoices` itself,
the `sensitive.clear()` / `persist(rows)` logic surrounding line 294.

## Constraints

No status engine in Kotlin — the app still only displays and sends strings; the
client must never send `Approved` (unchanged, pinned by the new test); no change
to any request; no agent trailers; never bare `./gradlew test`.
