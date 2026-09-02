package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.data.mapRollApplicantIds
import org.dhamma.dipi.staff.data.rollNameKey
import org.dhamma.dipi.staff.data.rollRoomKey
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec 2d S2 — the applicant-id join (the Wave-1 BLOCKED-check ruling: the
 * teacher-list markup carries NO id). Primary join: group-band gender +
 * normalized full name, unique within the course worklist; duplicates
 * disambiguate by room (zero-day merge) then age; anything still ambiguous
 * resolves to NO id — never a guess.
 */
class TeacherIdMappingTest {

    private fun applicant(
        id: Int,
        given: String,
        family: String,
        gender: Gender = Gender.M,
        age: Int? = 40,
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = gender,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = true,
        age = age,
    )

    private fun rollRow(sn: Int, name: String, room: String = "Mbk-8", age: String = "40") = RollRow(
        sn = sn, name = name, roleTag = null, room = room, age = age, city = "Pune",
        courses = emptyList(), cell = "", seat = "", seatKind = SeatKind.FLOOR,
        backrest = false, occupation = "", education = "", languages = "",
    )

    private fun roll(gender: Gender, vararg rows: RollRow) = TeacherRoll(
        listOf(
            RollGroup(
                at = "(unassigned)", code = null, gender = gender,
                seniority = RollSeniority.OLD, group = "1", total = rows.size,
                rows = rows.toList(),
            ),
        ),
    )

    private fun ids(mapped: TeacherRoll): List<Int?> =
        mapped.groups.single().rows.map { it.applicantId?.value }

    @Test
    fun uniqueNormalizedNamePlusGenderIsThePrimaryJoin() {
        val mapped = mapRollApplicantIds(
            roll(Gender.M, rollRow(1, "Suresh  NAIR"), rollRow(2, "Unknown Person")),
            listOf(applicant(4, "Suresh", "Nair"), applicant(9, "Meera", "Deshpande", Gender.F)),
            emptyMap(),
        )
        assertEquals(listOf(4, null), ids(mapped))
    }

    @Test
    fun genderComesFromTheBandAndMustMatch() {
        // Same name on the female worklist row: a male group never adopts it.
        val mapped = mapRollApplicantIds(
            roll(Gender.M, rollRow(1, "Kiran Patel")),
            listOf(applicant(7, "Kiran", "Patel", Gender.F)),
            emptyMap(),
        )
        assertEquals(listOf(null), ids(mapped))
    }

    @Test
    fun duplicateNamesDisambiguateByZeroDayRoom() {
        val worklist = listOf(
            applicant(21, "Ram", "Sharma", age = 40),
            applicant(22, "Ram", "Sharma", age = 40),
        )
        val checkIns = mapOf(
            21 to CheckInRecord(checkedIn = true, room = "Mbk 8"),
            22 to CheckInRecord(checkedIn = true, room = "Mbk 11"),
        )
        val mapped = mapRollApplicantIds(
            roll(Gender.M, rollRow(1, "Ram Sharma", room = "Mbk-11")),
            worklist,
            checkIns,
        )
        // "Mbk-11" (roll) == "Mbk 11" (zero-day merge) through the room key.
        assertEquals(listOf(22), ids(mapped))
    }

    @Test
    fun duplicateNamesFallBackToAgeThenRefuseToGuess() {
        val worklist = listOf(
            applicant(31, "Ram", "Sharma", age = 40),
            applicant(32, "Ram", "Sharma", age = 62),
        )
        val byAge = mapRollApplicantIds(
            roll(Gender.M, rollRow(1, "Ram Sharma", age = "62")),
            worklist,
            emptyMap(),
        )
        assertEquals(listOf(32), ids(byAge))

        // Same name, same age, no room evidence: ambiguous stays unmapped.
        val ambiguous = mapRollApplicantIds(
            roll(Gender.M, rollRow(1, "Ram Sharma", age = "40")),
            listOf(applicant(31, "Ram", "Sharma", age = 40), applicant(32, "Ram", "Sharma", age = 40)),
            emptyMap(),
        )
        assertNull(ids(ambiguous).single())
    }

    @Test
    fun emptyWorklistLeavesTheRollUntouched() {
        val r = roll(Gender.M, rollRow(1, "Suresh Nair"))
        assertEquals(r, mapRollApplicantIds(r, emptyList(), emptyMap()))
    }

    @Test
    fun keysNormalizeCaseWhitespaceAndSeparators() {
        assertEquals(rollNameKey("Suresh Nair"), rollNameKey("  suresh   NAIR "))
        assertEquals(rollRoomKey("Mbk-8"), rollRoomKey("Mbk 8"))
        assertEquals(rollRoomKey("MBK8"), rollRoomKey("mbk-8"))
    }
}
