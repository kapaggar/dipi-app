package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.SheetStylesheet
import org.dhamma.dipi.staff.desk.seatingPlanPrintHtml
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetScreenWidth
import org.dhamma.dipi.staff.model.TeacherRoll
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Writes synthetic print HTML for the out-of-git evidence folder.
 * Active only when DIPI_V7_EVIDENCE is set. Names are invented markers.
 */
class V7EvidenceDumpTest {

    @Test
    fun dumpSyntheticPrintHtml() {
        val root = System.getenv("DIPI_V7_EVIDENCE")?.let(::File) ?: return
        val htmlDir = File(root, "html").apply { mkdirs() }

        val chitBody = buildString {
            append("<div class=\"main-div\">")
            repeat(25) { i ->
                val n = i + 1
                append(
                    """<div class="table-student-chit"><div class="seat">A$n</div>""" +
                        """<div class="room">Syn $n</div>""" +
                        """<div class="name">Synthetic Chit ${n.toString().padStart(2, '0')}</div>""" +
                        """<div class="cell"></div></div>""",
                )
            }
            append("</div>")
        }
        htmlDir.resolve("student-chits-25.html").writeText(
            SheetStylesheet.render(chitBody, emptySet(), SheetExport.StudentChit),
        )

        val slipBody = buildString {
            append("<div class=\"main-div\">")
            repeat(5) { i ->
                val n = i + 1
                append(
                    """<div class="table-student-chit"><div class="seat">B$n</div>""" +
                        """<div class="room">Slip $n</div>""" +
                        """<div class="name">Synthetic Slip ${n.toString().padStart(2, '0')}</div>""" +
                        """<div class="cell"></div></div>""",
                )
            }
            append("</div>")
        }
        htmlDir.resolve("checking-slips-5.html").writeText(
            SheetStylesheet.render(slipBody, emptySet(), SheetExport.CheckingSlip),
        )

        htmlDir.resolve("seating-landscape.html").writeText(seatingPlanPrintHtml(syntheticRoll()) { HallGrid() })

        val day0 = """
            <table>
              <tr>
                <td class="d0-name">Synthetic Applicant 01</td>
                <td class="d0-contact">SYNTHETIC-DAY0-CONTACT</td>
                <td class="d0-comments">Synthetic comment that must remain complete on paper.</td>
              </tr>
            </table>
        """.trimIndent()
        htmlDir.resolve("day0-fit.html").writeText(
            SheetStylesheet.render(day0, emptySet(), SheetExport.Day0List, SheetScreenWidth.FIT),
        )
        htmlDir.resolve("day0-readable.html").writeText(
            SheetStylesheet.render(day0, emptySet(), SheetExport.Day0List, SheetScreenWidth.READABLE),
        )

        val manager = """
            <table>
              <tr>
                <td class="ml-name">Synthetic Manager 01</td>
                <td class="ml-cell">SYNTHETIC-MANAGER-CELL</td>
              </tr>
            </table>
        """.trimIndent()
        htmlDir.resolve("manager-fit.html").writeText(
            SheetStylesheet.render(manager, emptySet(), SheetExport.ManagerList, SheetScreenWidth.FIT),
        )
        htmlDir.resolve("manager-readable.html").writeText(
            SheetStylesheet.render(manager, emptySet(), SheetExport.ManagerList, SheetScreenWidth.READABLE),
        )

        val day0Fit = htmlDir.resolve("day0-fit.html").readText()
        val day0Readable = htmlDir.resolve("day0-readable.html").readText()
        assertTrue(day0Fit.contains("dipi-fit"))
        assertTrue(day0Readable.contains("dipi-readable"))
        assertTrue(day0Fit.contains("@media print"))
        assertTrue(day0Readable.contains("max-width: none !important"))
        assertTrue(day0Fit.contains(".d0-contact{display:none!important}"))
        assertTrue(day0Fit.contains("SYNTHETIC-DAY0-CONTACT"))
        assertTrue(!day0Fit.contains("dipi-hide-ml-cell") || day0Fit.contains("ml-cell"))
    }

    private fun row(sn: Int, name: String, seat: String, room: String, genderPrefix: String) = RollRow(
        sn = sn,
        name = name,
        roleTag = null,
        room = room,
        age = "40",
        city = "Synthetic City",
        courses = emptyList(),
        cell = "",
        seat = seat,
        seatKind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        },
        backrest = seat.startsWith("A"),
        occupation = "",
        education = "",
        languages = "",
    )

    private fun syntheticRoll(): TeacherRoll {
        val male = (1..8).map { n ->
            row(
                n,
                "Synthetic Male ${n.toString().padStart(2, '0')}",
                if (n == 8) "CW-A1" else if (n == 7) "CH-A1" else "A$n",
                "Mbk $n",
                "M",
            )
        }
        val female = (1..6).map { n ->
            row(
                n + 10,
                "Synthetic Female ${n.toString().padStart(2, '0')}",
                "A$n",
                "Fbk $n",
                "F",
            )
        }
        return TeacherRoll(
            groups = listOf(
                RollGroup("Synthetic AT M", "SAM", Gender.M, RollSeniority.OLD, "1", male.size, male),
                RollGroup("Synthetic AT F", "SAF", Gender.F, RollSeniority.NEW, "1", female.size, female),
            ),
        )
    }
}
