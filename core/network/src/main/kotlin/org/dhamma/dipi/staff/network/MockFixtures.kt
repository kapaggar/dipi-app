package org.dhamma.dipi.staff.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val jsonPretty = Json { encodeDefaults = true; ignoreUnknownKeys = true }

internal object MockFixtures {
    const val CENTRE_ID = 1
    const val COURSE_10D = 10
    const val RAKESH_ID = 2
    const val MEERA_ID = 1

    val session = SessionDto(
        uid = 42,
        name = "sudha.user",
        displayName = "sudha.user",
        centres = listOf(CentreDto(CENTRE_ID, "Dhamma Sudha")),
        modeTest = true,
    )

    val courses = listOf(
        CourseDto(COURSE_10D, CENTRE_ID, "10-Day", "2026-08-20", "2026-08-31", "10d"),
        CourseDto(11, CENTRE_ID, "Satipatthana", "2026-09-03", "2026-09-12", "stp"),
        CourseDto(12, CENTRE_ID, "10-Day", "2026-09-16", "2026-09-27", "10d"),
    )

    val counts = mapOf(
        "All" to 214,
        "Pending" to 61,
        "Received" to 48,
        "Confirmed" to 72,
        "Expected" to 18,
        "Cancelled" to 9,
        "Rejected" to 6,
    )

    val people: List<ApplicantDto> = listOf(
        person(
            MEERA_ID, "Meera", "Deshpande", "F", "Confirmed", "NF128",
            old = true, city = "Pune", state = "Maharashtra", age = 34,
            mobile = "+91 98220 41783", email = "meera.deshpande@gmail.com",
            home = "+91 20 2567 1120", dob = "11 Mar 1992", applied = "2 Jul 2026",
            photo = "good",
            history = HistoryDto(
                "10 Jul 2018 · Dhamma Sudha · Pundalik Ahire",
                "1 Apr 2026 · Dhamma Pattana · Bhumidhar",
                listOf(CountDto("10-day", 4), CountDto("Satipatthana", 1), CountDto("Dhamma service", 2)),
            ),
        ),
        person(
            RAKESH_ID, "Rakesh", "Iyer", "M", "Pending", null,
            city = "Chennai", state = "Tamil Nadu", age = 28,
            mobile = "+91 50031 55402", email = "r.iyer@outlook.com",
            dob = "5 Sep 1997", applied = "28 Jul 2026", photo = "rot90",
            emergency = false,
            flags = listOf(
                FlagDto("HARD", "Mobile number cannot be an Indian number", "phone_prefix_invalid · +91 50031 55402", "phone_prefix_invalid"),
                FlagDto("HARD", "Emergency contact number is blank", "missing_field · 'Emergency Contact No'", "missing_field"),
                FlagDto("HARD", "No ID type or ID number on the application", "id_missing · ID Type —, ID No —", "id_missing"),
            ),
        ),
        person(
            3, "Ananya", "Bhosale", "F", "Received", "NF131",
            city = "Nashik", state = "Maharashtra", age = 22,
            mobile = "+91 88888 20114", email = "ananya.b@gmail.com",
            dob = "19 Jan 2004", applied = "21 Jul 2026", photo = "crop",
            flags = listOf(FlagDto("HARD", "Aadhar number is masked", "aadhar_masked · XXXX XXXX 4417", "aadhar_masked")),
        ),
        person(
            4, "Suresh", "Nair", "M", "Confirmed", "OM42",
            type = "Sevak", old = true, city = "Kochi", state = "Kerala", age = 51,
            mobile = "+91 94470 33218", email = "suresh.nair@dhamma.net",
            home = "+91 484 2334 771", dob = "2 Feb 1975", applied = "14 Jun 2026",
            history = HistoryDto(
                "2 Feb 2001 · Dhamma Ketana · S. Ramanathan",
                "12 Dec 2025 · Dhamma Sudha · Bhumidhar",
                listOf(CountDto("10-day", 11), CountDto("20-day", 1), CountDto("Satipatthana", 3), CountDto("Dhamma service", 9)),
            ),
            flags = listOf(FlagDto("SAFETY", "Emergency contact is their own mobile", "emergency_eq_self · 9447033218", "emergency_eq_self")),
        ),
        person(
            5, "Priya", "Chandrasekhar", "F", "Expected", "NF140",
            old = true, city = "Bengaluru", state = "Karnataka", age = 39,
            mobile = "+91 99001 77620", email = "priyac@fastmail.com",
            dob = "30 Oct 1984", applied = "3 Aug 2026",
            history = HistoryDto(
                "19 Mar 2015 · Dhamma Paphulla · Uma Rangan",
                "8 Nov 2025 · Dhamma Paphulla · Uma Rangan",
                listOf(CountDto("10-day", 3), CountDto("Dhamma service", 1)),
            ),
            flags = listOf(FlagDto("HARD", "Listed age does not match date of birth", "age_dob_mismatch · listed 39, DOB gives 41", "age_dob_mismatch")),
        ),
        person(
            6, "Vikram", "Joshi", "M", "Cancelled", "OM17",
            old = true, city = "Indore", state = "Madhya Pradesh", age = 46,
            mobile = "+91 93000 12456", email = "vikram.joshi@gmail.com",
            dob = "7 Dec 1979", applied = "11 Jun 2026",
        ),
        person(
            7, "Fatima", "Sheikh", "F", "Confirmed", "NF133",
            city = "Hyderabad", state = "Telangana", age = 31,
            mobile = "+91 70930 55811", email = "fatima.sheikh@gmail.com",
            dob = "24 Apr 1995", applied = "19 Jul 2026", photo = "noface",
            flags = listOf(
                FlagDto(
                    "HARD",
                    "Also active in another course",
                    "cross_course_duplicate · Satipatthana 3 Sep, Confirmed NF061 · matched by phone, name+DOB",
                    "cross_course_duplicate",
                ),
            ),
        ),
        person(
            8, "Devendra", "Kulkarni", "M", "Pending", null,
            type = "Sevak", city = "Jaipur", state = "Rajasthan", age = 26,
            mobile = "+91 82330 90417", email = "dev.kulkarni@gmail.com",
            dob = "16 Aug 1999", applied = "30 Jul 2026",
            flags = listOf(FlagDto("SOFT", "Mobile shared with another applicant", "shared_mobile · 8233090417 · also Rekha Kulkarni", "shared_mobile")),
        ),
        person(
            9, "Lakshmi", "Menon", "F", "Received", "NF136",
            old = true, city = "Thrissur", state = "Kerala", age = 58,
            mobile = "+91 97440 21008", email = "lakshmi.menon@gmail.com",
            home = "+91 487 2331 004", dob = "9 May 1968", applied = "25 Jul 2026", photo = "rot180",
            history = HistoryDto(
                "14 Jan 1999 · Dhamma Ketana · S. Ramanathan",
                "20 Jun 2025 · Dhamma Ketana · Uma Rangan",
                listOf(CountDto("10-day", 14), CountDto("30-day", 1), CountDto("Satipatthana", 4), CountDto("Dhamma service", 12)),
            ),
        ),
        person(
            10, "Arjun", "Patel", "M", "Rejected", null,
            city = "Ahmedabad", state = "Gujarat", age = 19,
            mobile = "+91 76000 44219", email = "arjun.patel04@gmail.com",
            dob = "2 Feb 2007", applied = "1 Aug 2026",
        ),
        person(
            11, "Sister", "Uma Rangan", "F", "Confirmed", "NF102",
            old = true, city = "Chennai", state = "Tamil Nadu", age = 62, monk = true,
            mobile = "+91 90250 78003", email = "uma.rangan@dhamma.net",
            dob = "13 Jul 1964", applied = "2 Jun 2026",
            history = HistoryDto(
                "3 Mar 1994 · Dhamma Sudha · Goenkaji",
                "5 May 2026 · Dhamma Sudha · Bhumidhar",
                listOf(CountDto("10-day", 22), CountDto("45-day", 1), CountDto("Teacher self course", 3), CountDto("Dhamma service", 18)),
            ),
            flags = listOf(FlagDto("HARD", "Honorific left in the name field", "name_title_prefix · 'Sister'", "name_title_prefix")),
        ),
        person(
            12, "Nikhil", "Rane", "M", "Expected", "NF144",
            city = "Nagpur", state = "Maharashtra", age = 35,
            mobile = "+91 89560 10077", email = "nikhil.rane@gmail.com",
            dob = "21 Nov 1990", applied = "6 Aug 2026",
        ),
    )

    val photoReview = listOf(
        PhotoReviewDto(RAKESH_ID, "suggest", "suggest ↻90°", 90, false),
        PhotoReviewDto(9, "auto", "✨ auto ↻180° — ✓ keep", 180, false),
        PhotoReviewDto(3, "suggest", "suggest ✂ zoom", 0, true),
        PhotoReviewDto(7, "nofel", "no face found", 0, false),
    )

    fun person(
        id: Int,
        given: String,
        family: String,
        g: String,
        status: String,
        conf: String?,
        type: String = "Student",
        old: Boolean = false,
        city: String,
        state: String,
        age: Int,
        mobile: String,
        email: String,
        home: String? = null,
        dob: String,
        applied: String,
        photo: String = "good",
        monk: Boolean = false,
        emergency: Boolean? = true,
        history: HistoryDto? = null,
        flags: List<FlagDto> = emptyList(),
    ) = ApplicantDto(
        id = id,
        centreId = CENTRE_ID,
        courseId = COURSE_10D,
        givenName = given,
        familyName = family,
        gender = g,
        status = status,
        type = type,
        oldStudent = old,
        attended = false,
        confNo = conf,
        email = email,
        mobile = mobile,
        phoneHome = home,
        city = city,
        state = state,
        country = "India",
        dob = dob,
        age = age,
        monk = monk,
        createdAt = applied,
        photoUrl = photo,
        emergencyPresent = emergency,
        history = history,
        flags = flags,
    )

    }
