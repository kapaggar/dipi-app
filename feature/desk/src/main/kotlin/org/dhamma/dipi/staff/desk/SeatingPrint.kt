package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.HallPlan
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.UNSEATED_NO_REASON
import org.dhamma.dipi.staff.model.hallLayout

/**
 * Print-only HTML for the native 5h hall (frame 5i, `READ & PRINT`).
 *
 * The board seating surface never `GET`s `/seating` (that page is the desk's
 * drag-drop editor and carries the dangerous `?r=` auto-allocation), so paper
 * is rendered from the in-memory roll instead — through the *same* pure
 * [hallLayout] the screen draws, so print and screen can never disagree. One
 * gender per A4 page, depth descending so row 1 sits at the Dhamma seat, the
 * chowky/chair rail and the visible unseated list beneath. Monochrome by
 * construction; no accent fills reach paper.
 */
fun seatingPlanPrintHtml(
    roll: TeacherRoll,
    gridFor: (Gender) -> HallGrid,
): String {
    val halls = listOf(Gender.M, Gender.F).mapNotNull { gender ->
        val groups = roll.groups.filter { it.gender == gender }
        if (groups.isEmpty()) null else gender to hallLayout(groups, gridFor(gender))
    }

    val sections = halls.mapIndexed { index, (gender, plan) ->
        val last = index == halls.lastIndex
        hallSection(gender, plan, breakAfter = !last)
    }

    return """
        <!doctype html><html><head><meta charset="utf-8">
        <style>
          @page{size:A4;margin:10mm}
          body{font:10pt/1.3 sans-serif;color:#111;margin:0}
          h1{font-size:15pt;margin:0 0 2px}
          .sub{font-size:9pt;color:#555;margin:0 0 8px}
          table.grid{width:100%;border-collapse:collapse;table-layout:fixed}
          table.grid td{border:0.5pt solid #333;height:52px;padding:3px 4px;
            vertical-align:top;font-size:8pt;overflow:hidden}
          td.mt{border:0.5pt dashed #bbb;color:#999}
          td .id{display:block;font-size:7pt;color:#666;letter-spacing:0.04em}
          td .nm{display:block;font-weight:600;font-size:9pt;line-height:1.1}
          td .on{display:block;font-size:6.5pt;color:#666}
          tr.axis td{border:0;height:auto;text-align:center;font-size:8pt;
            color:#666;padding:2px 0}
          .teacher{border:0.5pt solid #333;background:#f0f0f0;text-align:center;
            font-size:8pt;letter-spacing:0.14em;color:#333;padding:5px;margin-top:2px}
          h2{font-size:10pt;margin:12px 0 4px}
          ul.list{margin:0;padding-left:16px;font-size:9pt}
          ul.list li{margin:1px 0}
        </style></head><body>
        ${sections.joinToString("\n")}
        </body></html>
    """.trimIndent()
}

private fun hallSection(gender: Gender, plan: HallPlan, breakAfter: Boolean): String {
    val genderWord = if (gender == Gender.F) "Female" else "Male"
    val style = if (breakAfter) " style=\"page-break-after:always\"" else ""

    val gridRows = buildString {
        // Depth descending: highest depth at the top, depth row 1 at the bottom
        // (directly above the teacher marker), matching the screen.
        for (d in plan.cells.indices.reversed()) {
            append("<tr>")
            for (cell in plan.cells[d]) {
                val seated = cell.seated
                if (seated != null) {
                    val oldNew = if (seated.old) "OLD" else "NEW"
                    append(
                        "<td><span class=\"id\">${esc(cell.id)}</span>" +
                            "<span class=\"nm\">${esc(seated.row.name)}</span>" +
                            "<span class=\"on\">$oldNew</span></td>",
                    )
                } else {
                    append("<td class=\"mt\"><span class=\"id\">${esc(cell.id)}</span></td>")
                }
            }
            append("</tr>")
        }
        append("<tr class=\"axis\">")
        plan.columnLetters.forEach { append("<td>${esc(it)}</td>") }
        append("</tr>")
    }

    val rail = if (plan.chowkyChair.isEmpty()) {
        ""
    } else {
        val items = plan.chowkyChair.joinToString("") { s ->
            val oldNew = if (s.old) "OLD" else "NEW"
            "<li>${esc(s.row.seat.trim())} — ${esc(s.row.name)} ($oldNew)</li>"
        }
        "<h2>Chowky / Chair</h2><ul class=\"list\">$items</ul>"
    }

    val unseated = if (plan.unseatedVisible.isEmpty()) {
        ""
    } else {
        val items = plan.unseatedVisible.joinToString("") { u ->
            val reason = if (u.reason == UNSEATED_NO_REASON) "" else " — ${esc(u.reason)}"
            "<li>${esc(u.row.name)}$reason</li>"
        }
        "<h2>Unseated</h2><ul class=\"list\">$items</ul>"
    }

    return """
        <section$style>
          <h1>$genderWord hall</h1>
          <p class="sub">${plan.seatedCount} seated · ${plan.oldCount} old, ${plan.newCount} new</p>
          <table class="grid"><tbody>$gridRows</tbody></table>
          <div class="teacher">TEACHER · DHAMMA SEAT</div>
          $rail
          $unseated
        </section>
    """.trimIndent()
}

private fun esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
