package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.SheetExport

/**
 * The desk-sheet stylesheet (v5 frames `5a` / `5t`) — the one piece of the
 * design file that is real CSS rather than a picture of Compose.
 *
 * It is injected **in the UI layer**, at render time, and never in the
 * transport: `SheetPayload.Html.html` still carries byte-for-byte what the
 * server said, which is what `injectedStylesheetIsNotInTheTransportPayload`
 * pins. Nothing here edits server content — it is presentation only:
 *
 * 1. Hides furniture that cannot work in our viewer. JavaScript is off, so
 *    every `Columns:` pill, `Print` link, drag handle and seat-editor button
 *    on these pages is a control drawn at full weight that does nothing. A
 *    dead control is worse than a missing one.
 * 2. Restyles the tables onto the Industry vocabulary of frame `5t`.
 * 3. Carries the column-visibility rules the native chips toggle, so a chip
 *    flips a class on `<html>` instead of costing a refetch.
 * 4. Sets `@media print` to A4 with a 10mm margin, and — per the plan's
 *    "do not truncate a comment or a health disclosure on print" — releases
 *    every on-screen line clamp when printing.
 *
 * In-sheet anchors get `pointer-events:none` and inherit body colour: they
 * all land on a 403 in this viewer, so nothing that cannot be tapped is
 * allowed to look tappable.
 */
object SheetStylesheet {

    /** CSS classes the native column chips toggle on the document element. */
    enum class Column(val cssClass: String, val label: String, val onByDefault: Boolean) {
        Day0Occupation("d0-occ", "Occupation", true),

        /**
         * Contact Details is **off by default and never prints** — the Day 0
         * clipboard sits on an open desk all day and these are phone numbers.
         */
        Day0Contact("d0-contact", "Contact", false),
        Day0Comments("d0-comments", "Comments", true),
        TeacherCity("tl-city", "City", false),
        TeacherCell("tl-cell", "Cell", false),
        TeacherEducation("tl-edu", "Education", false),
        TeacherLanguages("tl-langs", "Languages", false),
        TeacherComments("tl-comments", "Comments", true),
        ManagerCell("ml-cell", "Cell", false),
        ;

        /** The class added to `<html>` when this column is hidden. */
        val hideClass: String get() = "dipi-hide-$cssClass"
    }

    /** The chips a given sheet offers, in the order the columns appear. */
    fun columnsFor(export: SheetExport): List<Column> = when (export) {
        SheetExport.Day0List -> listOf(Column.Day0Occupation, Column.Day0Contact, Column.Day0Comments)
        SheetExport.TeacherList ->
            listOf(
                Column.TeacherCity,
                Column.TeacherCell,
                Column.TeacherEducation,
                Column.TeacherLanguages,
                Column.TeacherComments,
            )
        SheetExport.ManagerList -> listOf(Column.ManagerCell)
        else -> emptyList()
    }

    /**
     * Wraps [serverHtml] with the injected `<style>` block and the hide
     * classes for [hidden]. The server body is passed through untouched —
     * appended after the style, never rewritten — so a change on the desk
     * shows up in the app the same day it ships.
     */
    fun render(serverHtml: String, hidden: Set<Column>, export: SheetExport? = null): String {
        val extra = when (export) {
            SheetExport.StudentChit -> " dipi-student-chit"
            SheetExport.CheckingSlip -> " dipi-checking-slip"
            else -> ""
        }
        val classes = hidden.joinToString(" ") { it.hideClass }
        return buildString {
            append("<!doctype html><html class=\"dipi-sheet")
            append(extra)
            append(" ")
            append(classes)
            append("\"><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            append("<style>")
            append(CSS)
            append(hiddenColumnRules())
            append("</style></head><body>")
            append(serverHtml)
            append("</body></html>")
        }
    }

    /**
     * One rule per column: `html.dipi-hide-d0-contact .d0-contact { display:none }`
     * covers the `<col>`, the `<th>` and every `<td>`, because the desk puts
     * the same class on all three (see `version-5/HAR-ROUTES.md`).
     */
    private fun hiddenColumnRules(): String = Column.entries.joinToString("") { col ->
        "html.${col.hideClass} .${col.cssClass}{display:none!important}"
    } + "@media print{.d0-contact{display:none!important}}"

    /**
     * Frame `5a`'s dead-furniture list and frame `5t`'s table vocabulary.
     * Selectors are the desk's own classes, catalogued in HAR-ROUTES.md §
     * "Per-sheet DOM".
     */
    private val CSS = """
        :root{
          --n100:#F5F5F8;--n200:#EDEDF1;--n300:#E0E0E3;--n400:#D4D4D7;
          --n500:#8A8A90;--n600:#6B6B72;--n700:#4A4A51;--n800:#2E2E34;
          --a100:#EDF3EE;--a300:#B9CFBE;--a400:#9BBFA3;--a800:#2F5D3A;
        }
        html,body{margin:0;padding:0;background:#FFF;color:#2E2E34;
          font-family:Roboto,'Helvetica Neue',Arial,sans-serif;font-size:13px;}

        /* 1 · Dead furniture. JavaScript is off: none of this can work. */
        .no-print,.helptext,.day0-toolbar,.tl-toolbar,.ml-toolbar,
        .col-toggle,.remove-seat,.remove-cell,.store-seat-changes,
        .dh-add-col,.dh-blank-col,.dh-del-col,.add-row,.drag-img,
        .ui-draggable-handle,.ui-sortable-handle,.seat-instructions,
        .header-day0 .title,.header-teacher .title,.header-manager .title,
        .header-day0 .title-head,.header-teacher .title-head,
        .header-manager .title-head
          {display:none!important}

        /* Anchors all land on a 403 here, so none of them may look tappable. */
        a,a:link,a:visited{color:inherit!important;text-decoration:none!important;
          pointer-events:none!important;cursor:default!important}

        /* 2 · Table vocabulary, frame 5t. No zebra: the fifth-row rule
           carries the eye instead, and zebra fights the group bands. */
        table{width:100%;border-collapse:collapse;table-layout:fixed}
        th,td{padding:0 10px;vertical-align:middle;overflow-wrap:break-word}
        thead th{height:28px;font-family:'IBM Plex Mono',ui-monospace,monospace;
          font-weight:500;font-size:9px;letter-spacing:1.4px;text-transform:uppercase;
          color:#8A8A90;text-align:left;border-bottom:1px solid var(--n400);
          background:#FFF;position:sticky;top:36px;z-index:1}
        .day0-blockinfo,.day0-groupinfo,.tl-groupinfo,.ml-groupinfo{height:36px;background:var(--a100);
          border:1px solid var(--a300);border-radius:6px;color:var(--a800);
          font-family:'IBM Plex Mono',ui-monospace,monospace;font-weight:500;
          font-size:10px;letter-spacing:1.4px;text-transform:uppercase;
          text-align:left;position:sticky;top:0;z-index:2}
        tbody tr{height:44px;border-bottom:1px solid var(--n200)}
        tbody tr:nth-child(5n){border-bottom:1px solid var(--n400)}
        tbody tr:nth-child(odd),tbody tr:nth-child(even){background:transparent}
        .d0-student,.tl-student,.ml-student{font-weight:500;font-size:15px;color:#2E2E34}
        .d0-sr,.d0-conf,.d0-age,.tl-sn,.tl-age,.tl-seat,.ml-sn,.ml-age,
        .ml-seat,.ml-conf,.ml-enum,.tl-cell,.ml-cell
          {font-family:'IBM Plex Mono',ui-monospace,monospace;text-align:right;
           font-variant-numeric:tabular-nums}
        .d0-courses,.d0-edu,.d0-city,.d0-occ,.tl-courses,.tl-occ,.tl-edu,
        .tl-langs,.ml-ename
          {font-size:13px;color:var(--n600)}
        .tl-courses{font-family:'IBM Plex Mono',ui-monospace,monospace;
          font-weight:500;font-size:13.5px;color:#2E2E34}
        .ml-enum{font-weight:500;font-size:18px;color:#2E2E34}
        /* Free text clamps on screen only — see @media print below. */
        .d0-comments,.tl-comments{display:-webkit-box;-webkit-line-clamp:2;
          -webkit-box-orient:vertical;overflow:hidden;font-size:13px;color:var(--n600)}

        /* Student chit: butted hairlines, seat then room then name. */
        .table-student-chit{border:0.25pt solid #2E2E34;page-break-inside:avoid}
        .table-student-chit .seat{font-family:'IBM Plex Mono',ui-monospace,monospace;
          font-weight:600;font-size:15px}
        .table-student-chit .name{font-size:15px;font-weight:500}
        .table-student-chit .cell:empty{display:none}

        /* 4 · Print. A4, 10mm, chits 9-up (63.3×92.3mm), contact off,
           nothing truncated. The desk @imports student-chit.css (197×110
           float, 18-up / 3×6 page breaks) after this block, so every
           geometry rule here is !important or the server layout wins. */
        @page{size:A4;margin:10mm}
        @media print{
          .d0-comments,.tl-comments{display:block;-webkit-line-clamp:unset;overflow:visible}
          thead th,.day0-blockinfo,.day0-groupinfo,.tl-groupinfo,.ml-groupinfo{position:static}
          .day0-break,.dh-page-sep{page-break-after:always}
          tbody tr{page-break-inside:avoid}
          body{font-size:9pt}
          thead th{font-size:8pt}
          html.dipi-student-chit .header-day0{display:none!important}
          html.dipi-student-chit .main-div{
            display:flex!important;flex-wrap:wrap!important;
            width:190mm!important;max-width:100%!important;margin:0!important}
          html.dipi-student-chit .table-student-chit{
            float:none!important;display:block!important;
            width:63.3mm!important;height:92.3mm!important;
            max-width:63.3mm!important;max-height:92.3mm!important;
            margin:0!important;padding:4mm!important;
            box-sizing:border-box!important;
            page-break-inside:avoid!important;break-inside:avoid!important}
          html.dipi-checking-slip .main-div{
            display:flex!important;flex-wrap:wrap!important;
            width:190mm!important;margin:0!important}
          html.dipi-checking-slip .table-student-chit{
            float:none!important;width:50%!important;height:130mm!important;
            margin:0!important;box-sizing:border-box!important}
        }
    """.trimIndent()
}
