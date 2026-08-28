package org.dhamma.dipi.staff.course

/** Native-screen dispatch for a desk tile, in place of the fragile title-string match. */
enum class DeskTileAction { CentreOps, AppSettings, AdvancedSearch }

data class DeskTileSpec(
    val title: String,
    val route: String,
    val action: DeskTileAction? = null,
)

enum class CourseHubLive { Applications, Summary, Photos, Audit, Calling, ZeroDay, CentreOps }

data class CourseHubTile(
    val title: String,
    val route: String,
    val live: CourseHubLive? = null,
    /** Header glyph on the phone hub cards (native tiles only). */
    val glyph: String = "",
)

/**
 * Native tiles lead — they are what the registrar touches — then the
 * desk-site links in their existing order (owner feedback 2026-08-27). The
 * Centre Settings desk-site route is kept for reference but unused once
 * `action` is set; the native screen replaces it rather than duplicating it.
 */
fun centreDeskTiles(centreId: Int): List<DeskTileSpec> = listOf(
    DeskTileSpec("Centre Settings", "centre/$centreId/edit", DeskTileAction.CentreOps),
    DeskTileSpec("Advanced Search", "search-app/$centreId", DeskTileAction.AdvancedSearch),
    DeskTileSpec("App Settings", "", DeskTileAction.AppSettings),
    DeskTileSpec("Manage Courses", "manage-course/$centreId"),
    DeskTileSpec("Daily Activity", "daily-activity/$centreId"),
    DeskTileSpec("SMS Report", "centre/$centreId/sms-report"),
    DeskTileSpec("Course Report", "centre/$centreId/course-report"),
    DeskTileSpec("Bulk Mail", "centre/$centreId/bulk-mail-schedule"),
)

/**
 * The phone course-hub catalogue (owner feedback 2026-08-16): native flows
 * first, in day-flow order, each with its card glyph; the desk-site links
 * (`live == null`) follow and render only inside the ⋯ overflow menu — the
 * phone opens straight into native screens, never the desk site.
 */
fun courseHubTiles(centreId: Int, courseId: Int): List<CourseHubTile> = listOf(
    CourseHubTile("View Applications", "search-course/$centreId/$courseId?s=&t=&g=&d=a", CourseHubLive.Applications, "▤"),
    CourseHubTile("Zero Day", "zero-day/$centreId/$courseId", CourseHubLive.ZeroDay, "✓"),
    CourseHubTile("Day 0 summary", "Day 0 summary", CourseHubLive.Summary, "≡"),
    CourseHubTile("Photo review", "Photo review", CourseHubLive.Photos, "◎"),
    CourseHubTile("Audit applications", "audit/$centreId/$courseId", CourseHubLive.Audit, "△"),
    CourseHubTile("Calling students", "calling/$centreId/$courseId", CourseHubLive.Calling, "✆"),
    CourseHubTile("Centre Settings", "centre/$centreId/edit", CourseHubLive.CentreOps, "⚙"),
    CourseHubTile("Add Application", "app/add/$centreId/$courseId"),
    CourseHubTile("Day 0 List", "day0-list/$centreId/$courseId"),
    CourseHubTile("Seating Plan", "seating/$centreId/$courseId"),
    CourseHubTile("Student Chit", "student-chit/$centreId/$courseId"),
    CourseHubTile("Checking Slip", "checking-slip/$centreId/$courseId"),
    CourseHubTile("Male PDF", "course-pdf-m/$centreId/$courseId"),
    CourseHubTile("Female PDF", "course-pdf-f/$centreId/$courseId"),
    CourseHubTile("Teachers List", "teacher-list/$centreId/$courseId"),
    CourseHubTile("Laundry List", "laundry-list/$centreId/$courseId"),
    CourseHubTile("Valuable List", "valuable-list/$centreId/$courseId"),
    CourseHubTile("Course Summary Report", "report-day11/$centreId/$courseId"),
)

/** The native-flow cards on the phone hub. */
fun courseHubLiveTiles(centreId: Int, courseId: Int): List<CourseHubTile> =
    courseHubTiles(centreId, courseId).filter { it.live != null }

/** Desk-site destinations, tucked into the hub's ⋯ overflow menu. */
fun courseHubDeskTiles(centreId: Int, courseId: Int): List<CourseHubTile> =
    courseHubTiles(centreId, courseId).filter { it.live == null }
