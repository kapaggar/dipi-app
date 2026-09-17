package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.CentreHallSettings
import org.dhamma.dipi.staff.model.HallSeatPlan

/**
 * Read-only Hall Settings from `GET /centre/{cid}/edit`.
 *
 * Live visual editor (HAR 2026-09-16, Dhamma Sudha `/centre/63/edit`,
 * upstream `dh_manageapp` `seat-visual.js`):
 * `cs_hall_combined`, `cs_seat_naming_conv`, and per-gender Main Plan
 * `seatcfg_{male|female}_{spr,sprc,dir,pos,empty,empty_cho}`. Group slots
 * `seatcfg_*_gN_*` exist on the page; this parser reads Main Plan only.
 * The INI `cs_seat_config` is assembled on POST and is not in the live GET
 * HTML; an older textarea is accepted as fallback. Never POST. No `?r=`.
 */
object CentreHallParser {

    fun settingsOrNull(html: String): CentreHallSettings? {
        if (html.isBlank()) return null
        if (!looksLikeHallEdit(html)) return null
        val combined = HtmlForms.yesNo(html, "cs_hall_combined")
        val naming = HtmlForms.radioValue(html, "cs_seat_naming_conv")?.toIntOrNull()
        val male = planFromFields(html, "male") ?: planFromIni(html, "RIGHT")
        val female = planFromFields(html, "female") ?: planFromIni(html, "LEFT")
        if (combined == null && naming == null && male == null && female == null) return null
        val malePlan = male ?: HallSeatPlan()
        val femalePlan = female ?: HallSeatPlan()
        return CentreHallSettings(
            combinedHall = combined,
            seatNaming = naming,
            maleSeatsPerRow = malePlan.columns,
            femaleSeatsPerRow = femalePlan.columns,
            malePlan = malePlan,
            femalePlan = femalePlan,
        )
    }

    private fun looksLikeHallEdit(html: String): Boolean =
        html.contains("cs_hall_combined") ||
            html.contains("seatcfg_male_spr") ||
            html.contains("cs_seat_config") ||
            html.contains("cs_seat_naming_conv")

    private fun planFromFields(html: String, gender: String): HallSeatPlan? {
        val prefix = "seatcfg_${gender}_"
        if (!html.contains("${prefix}spr")) return null
        val columns = HtmlForms.inputValue(html, "${prefix}spr")?.trim()?.toIntOrNull()
        val chowky = HtmlForms.inputValue(html, "${prefix}sprc")?.trim()?.toIntOrNull()
        val direction = HtmlForms.selectValue(html, "${prefix}dir")
        val position = HtmlForms.selectValue(html, "${prefix}pos")
        val empty = HtmlForms.inputValue(html, "${prefix}empty")?.trim()
        val emptyCho = HtmlForms.inputValue(html, "${prefix}empty_cho")?.trim()
        val plan = HallSeatPlan(
            columns = columns,
            chowkyColumns = chowky,
            direction = direction,
            chowkyPosition = position,
            emptySeats = empty,
            emptyChowky = emptyCho,
        )
        return plan.takeIf { it.isPresent() }
    }

    /**
     * Older desks still render the raw INI textarea. Live GET (16 Sep 2026)
     * does not. Main-section keys only; `GROUP<n>-` lines are ignored.
     */
    private fun planFromIni(html: String, section: String): HallSeatPlan? {
        val raw = HtmlForms.textarea(html, "cs_seat_config") ?: return null
        val block = Regex(
            """\[$section\](.*?)(?=\[[A-Za-z]+\]|\z)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(raw)?.groupValues?.get(1) ?: return null
        fun key(name: String): String? {
            val m = Regex(
                """^${Regex.escape(name)}\s*=\s*(.*)$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
            ).find(block) ?: return null
            return m.groupValues[1].trim()
        }
        val plan = HallSeatPlan(
            columns = key("SeatsPerRow")?.toIntOrNull(),
            chowkyColumns = key("SeatsPerRowChowky")?.toIntOrNull(),
            direction = key("SeatDirection")?.lowercase(),
            chowkyPosition = key("ChowkyPosition")?.lowercase(),
            emptySeats = key("EmptySeats"),
            emptyChowky = key("EmptyChowky"),
        )
        return plan.takeIf { it.isPresent() }
    }
}
