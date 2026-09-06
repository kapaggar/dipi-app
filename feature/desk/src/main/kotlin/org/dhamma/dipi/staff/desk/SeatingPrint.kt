package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.BACKREST_GLYPH
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.HallPlan
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.UNSEATED_NO_REASON
import org.dhamma.dipi.staff.model.backrestSeatLabel
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
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
          @page{size:A4 landscape;margin:6mm}
          *{box-sizing:border-box}
          html,body{width:100%;height:100%;margin:0}
          body{font:10pt/1.15 Arial,sans-serif;color:#111}
          section.hall{height:198mm;display:flex;flex-direction:column;overflow:hidden}
          .header{display:flex;align-items:flex-end;justify-content:space-between;
            gap:4mm;margin-bottom:2mm}
          h1{font-size:16pt;line-height:1;margin:0}
          .sub{font-size:8.5pt;color:#444;margin:0;text-align:right}
          .grid-wrap{flex:1;min-height:0;display:flex}
          table.grid{width:100%;height:100%;border-collapse:collapse;table-layout:fixed}
          table.grid tr.seat-row{height:auto}
          table.grid td{border:0.6pt solid #222;padding:1.5mm 1.7mm;
            vertical-align:top;overflow:hidden}
          td.mt{border:0.5pt dashed #aaa;color:#777}
          .id{display:block;font-size:8.5pt;font-weight:700;color:#333;
            letter-spacing:0.035em;white-space:nowrap}
          .nm{display:block;font-weight:700;font-size:10.5pt;line-height:1.08;
            margin-top:0.8mm}
          .meta{display:block;font-size:7.8pt;font-weight:600;color:#333;
            line-height:1.15;margin-top:1mm}
          .on{display:block;font-size:7pt;font-weight:700;color:#555;
            letter-spacing:0.08em;margin-top:0.7mm}
          tr.axis{height:4mm}
          tr.axis td{border:0;height:4mm;text-align:center;font-size:7.5pt;
            color:#555;padding:0.8mm 0 0}
          .teacher{border:0.5pt solid #333;background:#f0f0f0;text-align:center;
            font-size:8pt;font-weight:700;letter-spacing:0.16em;color:#222;
            padding:1.5mm;margin-top:0.8mm}
          .lower{flex:none;margin-top:1.5mm}
          h2{font-size:8pt;line-height:1;margin:0 0 0.8mm;letter-spacing:0.08em}
          .cards{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:0.8mm}
          .card{border:0.5pt solid #555;padding:1mm 1.2mm;min-width:0}
          .card .id{font-size:7.5pt}
          .card .nm{font-size:8.5pt;margin-top:0.4mm;white-space:nowrap;
            overflow:hidden;text-overflow:ellipsis}
          .card .meta{font-size:7pt;margin-top:0.5mm}
          .card .on{font-size:6.5pt;margin-top:0.4mm}
          .lower+.lower{margin-top:1.5mm}
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
            append("<tr class=\"seat-row\">")
            for (cell in plan.cells[d]) {
                val seated = cell.seated
                if (seated != null) {
                    val oldNew = if (seated.old) "OLD" else "NEW"
                    append(
                        "<td>${personBlock(
                            backrestSeatLabel(cell.id, seated.row.backrest),
                            seated.row.name,
                            seated.row.room,
                            seated.row.age,
                            oldNew,
                        )}</td>",
                    )
                } else {
                    append("<td class=\"mt\"><span class=\"id\">SEAT ${esc(cell.id)}</span></td>")
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
            "<div class=\"card\">${personBlock(
                backrestSeatLabel(s.row.seat.trim(), s.row.backrest),
                s.row.name,
                s.row.room,
                s.row.age,
                oldNew,
            )}</div>"
        }
        "<div class=\"lower\"><h2>CHOWKY / CHAIR</h2><div class=\"cards\">$items</div></div>"
    }

    val unseated = if (plan.unseatedVisible.isEmpty()) {
        ""
    } else {
        val items = plan.unseatedVisible.joinToString("") { u ->
            val reason = if (u.reason == UNSEATED_NO_REASON) "" else " — ${esc(u.reason)}"
            "<div class=\"card\"><span class=\"id\">UNSEATED$reason</span>" +
                "<span class=\"nm\">${esc(u.row.name)}</span>" +
                "<span class=\"meta\">${roomAge(u.row.room, u.row.age)}</span></div>"
        }
        "<div class=\"lower\"><h2>UNSEATED</h2><div class=\"cards\">$items</div></div>"
    }

    // The legend only earns its line when a drawn seat (hall cell or rail)
    // actually carries the glyph on this page.
    val hasBackrest = plan.cells.any { r -> r.any { it.seated?.row?.backrest == true } } ||
        plan.chowkyChair.any { it.row.backrest }
    val legend =
        if (hasBackrest) " · ${esc(BACKREST_GLYPH)} = backrest" else ""

    return """
        <section class="hall"$style>
          <div class="header">
            <h1>$genderWord hall</h1>
            <p class="sub">${plan.seatedCount} seated · ${plan.oldCount} old, ${plan.newCount} new$legend</p>
          </div>
          <div class="grid-wrap"><table class="grid"><tbody>$gridRows</tbody></table></div>
          <div class="teacher">TEACHER · DHAMMA SEAT</div>
          $rail
          $unseated
        </section>
    """.trimIndent()
}

private fun personBlock(
    seat: String,
    name: String,
    room: String,
    age: String,
    oldNew: String,
): String =
    "<span class=\"id\">SEAT ${esc(seat)}</span>" +
        "<span class=\"nm\">${esc(name)}</span>" +
        "<span class=\"meta\">${roomAge(room, age)}</span>" +
        "<span class=\"on\">$oldNew</span>"

private fun roomAge(room: String, age: String): String =
    "ROOM ${printable(room)} · AGE ${printable(age)}"

private fun printable(value: String): String = esc(value.trim().ifEmpty { "—" })

private fun esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
