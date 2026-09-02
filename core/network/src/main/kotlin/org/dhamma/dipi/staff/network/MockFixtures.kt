package org.dhamma.dipi.staff.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val jsonPretty = Json { encodeDefaults = true; ignoreUnknownKeys = true }

internal object MockFixtures {
    const val CENTRE_ID = 1
    const val COURSE_10D = 10
    const val RAKESH_ID = 2
    const val MEERA_ID = 1

    /**
     * Sheet permissions differ per export on the live desk (access male/
     * female, view teachers list, access zero day, view course report):
     * any sheet fetch against this centre id refuses with the Drupal 403
     * page so tests can assert the verbatim NotAvailable path.
     */
    const val FORBIDDEN_CENTRE = 99

    val session = SessionDto(
        uid = 42,
        name = "sudha.user",
        displayName = "sudha.user",
        centres = listOf(CentreDto(CENTRE_ID, "Dhamma Sudha")),
        modeTest = true,
    )

    val courses = listOf(
        CourseDto(COURSE_10D, CENTRE_ID, "10-Day", "2026-08-20", "2026-08-31"),
        CourseDto(11, CENTRE_ID, "Satipatthana", "2026-09-03", "2026-09-12"),
        CourseDto(12, CENTRE_ID, "10-Day", "2026-09-16", "2026-09-27"),
    )

    /** Courses that have already started — the Select Course older list. */
    val olderCourses = listOf(
        CourseDto(8, CENTRE_ID, "Dhamma Sudha / 10 Day / 2026 / 6th-Aug to 17th-Aug", "2026-08-06", "2026-08-17"),
        CourseDto(7, CENTRE_ID, "Dhamma Sudha / STP / 2026 / 23rd-Jul to 31st-Jul", "2026-07-23", "2026-07-31"),
    )

    /**
     * DataTables Editor GET payload for `/centre/{cid}/acco-handler`
     * (`dh_center_setting_acco`) — the shape the live desk serves.
     */
    val accoHandlerJson = """
        {"data":[
         {"DT_RowId":"row_1","dh_center_setting_acco":{"csa_id":"1","csa_center":"1","csa_gender":"F","csa_section":"Fbk","csa_room":"1:6W","csa_deleted":"0"}},
         {"DT_RowId":"row_2","dh_center_setting_acco":{"csa_id":"2","csa_center":"1","csa_gender":"M","csa_section":"Mbk","csa_room":"1:8, 9IC, 10W","csa_deleted":"0"}}
        ],"options":{"dh_center_setting_acco.csa_gender":[{"label":"Male","value":"M"},{"label":"Female","value":"F"}]},"files":[]}
    """.trimIndent()

    /**
     * Rooms the desk already allotted (applicant id → section, room no).
     * Posting either room for anyone else refuses like the live handler.
     */
    val allotedSeed = mapOf(
        4 to ("Mbk" to "8"), // Suresh Nair holds Mbk 8
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
            dob = "5 Sep 1997", applied = "28 Jul 2026",
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
            dob = "19 Jan 2004", applied = "21 Jul 2026",
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
            dob = "24 Apr 1995", applied = "19 Jul 2026",
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
            home = "+91 487 2331 004", dob = "9 May 1968", applied = "25 Jul 2026",
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

    // --- Board "Sheets & exports" fixtures (mock flag only) ---

    /** Minimal but structurally valid PDF blob for `/course-pdf-{m,f}`. */
    val pdfBytes: ByteArray = (
        "%PDF-1.4\n" +
            "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
            "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\n" +
            "trailer<</Root 1 0 R>>\n" +
            "%%EOF\n"
        ).toByteArray(Charsets.US_ASCII)

    /** OLE2 magic + filler for the laundry/valuable `.xls` streams. */
    val xlsBytes: ByteArray =
        byteArrayOf(
            0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(),
            0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(),
        ) + "DIPI mock spreadsheet".toByteArray(Charsets.US_ASCII)

    /**
     * Distinctive health free-text planted in the fixture's Comments cells.
     * The teacher-list Comments column is unlabelled health text the client
     * must NEVER parse or store — tests assert this string appears nowhere
     * in any parsed output.
     */
    const val TEACHER_HEALTH_NOISE = "Insulin-dependent diabetes, takes olanzapine nightly"

    /**
     * `GET /teacher-list/{cid}/{courseId}` as `dh_generate_teacher_list`
     * renders it (inc/zero-day.inc:877-1072): an UNTHEMED fragment starting
     * `<style>`, a header + screen-only toolbar, then one
     * `table-teacher-list` block per (gender, old/new, group) with the
     * pipe-separated `tl-groupinfo` band and 12 cells per row. S/N restarts
     * per block; langs/comments wrapped in `span.tlc`; the trailing
     * column-toggle script is part of the live shape too.
     */
    fun teacherListHtml(cid: Int, courseId: Int): String {
        fun row(
            sn: Int,
            student: String,
            room: String,
            age: String,
            city: String,
            courses: String,
            cell: String,
            seat: String,
            occ: String,
            edu: String,
            langs: String,
            comments: String,
        ) = "<tr><td class=\"tl-sn\">$sn</td><td class=\"tl-student\">$student</td>" +
            "<td class=\"tl-room\">$room</td><td class=\"tl-age\">$age</td>" +
            "<td class=\"tl-city\">$city</td><td class=\"tl-courses\">$courses</td>" +
            "<td class=\"tl-cell\">$cell</td><td class=\"tl-seat\">$seat</td>" +
            "<td class=\"tl-occ\">$occ</td><td class=\"tl-edu\">$edu</td>" +
            "<td class=\"tl-langs\"><span class=\"tlc\">$langs</span></td>" +
            "<td class=\"tl-comments\"><span class=\"tlc\">$comments</span></td></tr>"

        fun band(at: String, gender: String, type: String, group: Int, total: Int) =
            "<tr><th class=\"tl-groupinfo\" colspan=\"12\">" +
                "<span class=\"at\">AT: $at</span><span class=\"sep\">|</span>" +
                "$gender<span class=\"sep\">|</span>$type<span class=\"sep\">|</span>" +
                "Group $group<span class=\"sep\">|</span><span class=\"cnt\">$total total</span></th></tr>"

        val headRow = "<tr><th class=\"tl-sn\">S/N</th><th class=\"tl-student\">Student</th>" +
            "<th class=\"tl-room\">Room</th><th class=\"tl-age\">Age</th><th class=\"tl-city\">City</th>" +
            "<th class=\"tl-courses\">Courses</th><th class=\"tl-cell\">Cell</th><th class=\"tl-seat\">Seat</th>" +
            "<th class=\"tl-occ\">Occupation</th><th class=\"tl-edu\">Education</th>" +
            "<th class=\"tl-langs\"><span class=\"tlc\">Languages</span></th>" +
            "<th class=\"tl-comments\"><span class=\"tlc\">Comments</span></th></tr>"

        val colgroup = "<colgroup><col class=\"tl-sn\"><col class=\"tl-student\"><col class=\"tl-room\">" +
            "<col class=\"tl-age\"><col class=\"tl-city\"><col class=\"tl-courses\"><col class=\"tl-cell\">" +
            "<col class=\"tl-seat\"><col class=\"tl-occ\"><col class=\"tl-edu\"><col class=\"tl-langs\">" +
            "<col class=\"tl-comments\"></colgroup>"

        fun block(brk: Boolean, bandRow: String, vararg rows: String) =
            "<div class=\"tl-block${if (brk) " tl-break" else ""}\"><div class=\"tl-scroll\">" +
                "<table class=\"table-teacher-list\">$colgroup<thead>$bandRow$headRow</thead>" +
                "<tbody>${rows.joinToString("")}</tbody></table></div></div>"

        return "<style>@import url(\"/sites/all/modules/dh_manageapp/css/teacher-list-v2.css\");</style>" +
            "<div class=\"header-teacher\"><div class=\"title\">Teacher List</div>" +
            "<div class=\"title-head\">Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug</div></div>" +
            "<div class=\"tl-toolbar no-print\"><span class=\"grp\"><b>Order:</b> " +
            "<a class=\"active\" href=\"/teacher-list/$cid/$courseId\">Seniority</a> &middot; " +
            "<a class=\"\" href=\"/teacher-list/$cid/$courseId?seating=1\">Seating plan</a></span>" +
            "<span class=\"grp\"><b>Columns:</b> " +
            "<button type=\"button\" class=\"col-toggle\" data-toggle-col=\"langs\">Languages</button> " +
            "<button type=\"button\" class=\"col-toggle\" data-toggle-col=\"comments\">Comments</button></span></div>" +
            block(
                false,
                band("Trainee-A-M Teacher [TAM]", "Male", "Old", 1, 3),
                row(
                    1, "Suresh Nair <b>(Sevak)</b>", "Mbk-8", "51", "Kochi",
                    "<b>10D:</b>11 <b>STP:</b>3 <b>SPL:</b>1", "", "",
                    "Retired Teacher", "B.Ed", "Malayalam, English", TEACHER_HEALTH_NOISE,
                ),
                row(
                    2, "Vikram Joshi <b>(AT-2010)</b>", "Mbk-2", "46", "Indore",
                    "<b>10D:</b>6 <b>TSC:</b>1", "C2 (2)",
                    "CW-A3<span class=\"tl-br\" title=\"Backrest\">BR</span>",
                    "Engineer", "B.E.", "Hindi, English", "",
                ),
                row(
                    3, "Nikhil Rane", "Mbk-11", "35", "Nagpur",
                    "<b>10D:</b>4", "", "A8",
                    "Accountant", "M.Com", "Marathi, Hindi", "",
                ),
            ) +
            block(
                true,
                band("(unassigned)", "Male", "New", 1, 2),
                row(
                    1, "Rakesh Iyer", "Mbk-14", "28", "Chennai",
                    "", "", "CH-12<span class=\"tl-br\" title=\"Backrest\">BR</span>",
                    "Software Developer", "B.Tech", "Tamil, English", TEACHER_HEALTH_NOISE,
                ),
                row(
                    2, "Arjun Patel", "Mbk-15", "19", "Ahmedabad",
                    "", "", "14", "—", "—", "Gujarati", "",
                ),
            ) +
            block(
                true,
                band("Uma Rangan [URN]", "Female", "Old", 1, 1),
                row(
                    1, "Meera Deshpande <b>(SAT-2011)</b>", "Fbk-1", "34", "Pune",
                    "<b>10D:</b>4 <b>STP:</b>1", "C5", "CW-B1",
                    "Doctor", "MBBS", "Marathi, English, Hindi", "",
                ),
            ) +
            "<script>document.addEventListener(\"click\", function(e){});</script>"
    }

    /** Print-styled desk sheet page (day0-list, manager-list, …). */
    fun sheetHtml(slug: String, cid: Int, courseId: Int) = """
        <html><head>
        <style>@import url("/sites/all/modules/dh_manageapp/css/teacher-list.css");</style>
        </head><body>
        <h2>$slug · centre $cid · course $courseId</h2>
        <table id="table-$slug"><tr><th>#</th><th>Name</th><th>Room</th></tr>
        <tr><td>1</td><td>Meera Deshpande</td><td>Fbk-1</td></tr>
        <tr><td>2</td><td>Suresh Nair</td><td>Mbk-8</td></tr></table>
        </body></html>
    """.trimIndent()

    private fun daySummaryBlock() = """
        <div id="day-summary"><table id="table-conf"><tr><th>Conf</th></tr><tr><td>72</td></tr></table>
        <table id="table-totals"><tr><th>M</th><th>F</th></tr><tr><td><b>6</b></td><td><b>6</b></td></tr></table>
        <table id="table-special"><tr><th>Teen</th></tr><tr><td>0</td></tr></table></div><br />
    """.trimIndent()

    private fun attendingRow(
        id: Int,
        conf: String,
        name: String,
        gender: String,
        type: String,
        age: Int,
        room: String,
        laundry: String = "",
        valuable: String = "",
        chowky: String = "No",
        chair: String = "No",
        backrest: String = "No",
        group: String = "1",
    ) = "<tr><td>$id</td><td>$conf</td><td><a href=\"/app/$id/edit\" appid=\"$id\">$name</a></td>" +
        "<td>$gender</td><td>$type</td><td>$age</td><td>0</td><td>0</td>" +
        "<td>$room</td><td>$laundry</td><td>$valuable</td>" +
        "<td>$chowky</td><td>$chair</td><td>$backrest</td><td>$group</td><td>||||</td></tr>"

    /** The zero-day page: `#day-summary` first, then a real attended table. */
    fun zeroDayHtml(cid: Int, courseId: Int) = """
        <html><head><title>Day Zero | centre $cid</title></head><body>
        <h1>Day Zero · course $courseId</h1>
        ${daySummaryBlock()}
        <br><h2>Attended Applicants</h2>
        <table id="table-attending">
        <thead><tr><th>Update</th><th>ConfNo</th><th>Name</th><th>Gender</th><th>Type</th><th>Age</th><th>Teen/10D/STP</th><th>LC</th><th>RoomNo</th><th>Laundry</th><th>Valuable</th><th>Chowky</th><th>Chair</th><th>BackRest</th><th>Group</th><th>H</th></tr></thead>
        <tbody>
        ${attendingRow(MEERA_ID, "OF128", "Meera Deshpande", "Female", "Student", 34, "Fbk-1")}
        ${attendingRow(4, "OM42", "Suresh Nair", "Male", "Sevak", 51, "Mbk-8", chowky = "Yes", group = "2")}
        </tbody></table>
        </body></html>
    """.trimIndent()

    /** Same page with `#table-attending-empty` — parser tests and empty-desk cases. */
    fun zeroDayEmptyAttendingHtml(cid: Int, courseId: Int) = """
        <html><head><title>Day Zero | centre $cid</title></head><body>
        <h1>Day Zero · course $courseId</h1>
        ${daySummaryBlock()}
        <br><h2>Attended Applicants</h2>
        <table id="table-attending-empty"><tr><td>none yet</td></tr></table>
        </body></html>
    """.trimIndent()

    /** Drupal 403 page, served verbatim on permission refusals. */
    val accessDeniedHtml = """
        <html><head><title>Access denied | Dhamma.org</title></head><body>
        <h1>Access denied</h1><p>You are not authorized to access this page.</p>
        </body></html>
    """.trimIndent()

    /** `dh_center_course_report_form` as drupal_get_form renders it. */
    fun courseReportFormHtml(cid: Int) = """
        <html><body>
        <form action="/centre/$cid/course-report" method="post" id="dh-center-course-report-form" accept-charset="UTF-8">
        <h2>Get Report for Courses Having Start Date:</h2>
        <input type="text" id="edit-report-from-date-datepicker-popup-0" name="report_from_date[date]" value="2025-08-16" size="12" />
        <input type="text" id="edit-report-to-date-datepicker-popup-0" name="report_to_date[date]" value="2026-08-16" size="12" />
        <input type="hidden" name="form_build_id" value="form-MoCkBuIlDiD" />
        <input type="hidden" name="form_token" value="mock-form-token" />
        <input type="hidden" name="form_id" value="dh_center_course_report_form" />
        <input type="submit" id="edit-sub" name="op" value="Download Course Report" />
        </form></body></html>
    """.trimIndent()

    /** CSV the course-report form submit streams back. */
    val courseReportCsv =
        "Course,NewMale,NewFemale,NewTotal,OldMale,OldFemale,OldTotal,StudentTotal,SevakMale,SevakFemale,SevakTotal\n" +
            "10-Day,4,5,9,2,1,3,12,1,1,2\n" +
            "Total,4,5,9,2,1,3,12,1,1,2\n"

    /** The desk's application edit form — display-only, never persisted. */
    fun appEditHtml(id: Int) = """
        <html><body>
        <form action="/app/$id/edit" method="post" id="dh-zero-app-form" accept-charset="UTF-8">
        <input type="text" name="a_f_name" value="Rakesh" />
        <input type="text" name="a_l_name" value="Iyer" />
        <input type="hidden" name="form_build_id" value="form-AppEdItBuIlD" />
        <input type="hidden" name="form_id" value="dh_zero_app_form" />
        <input type="submit" name="op" value="Save" />
        </form></body></html>
    """.trimIndent()

    fun appCoursesHtml(id: Int) = """
        <h4>Applicant Courses</h4>
        <table>
        <thead><tr><th>Course</th><th>Type</th><th>Status</th><th>Attended</th><th>Address</th></tr></thead>
        <tbody>
        <tr><td>10-Day · Aug 2026</td><td>Student</td><td>Confirmed</td><td>False</td><td>Pune</td></tr>
        <tr><td>Satipatthana · Apr 2026</td><td>Student</td><td>Attended</td><td>True</td><td>Pune</td></tr>
        </tbody></table>
    """.trimIndent()

    fun appActivityHtml(id: Int) = """
        <h4>Activity Log</h4>
        <table>
        <thead><tr><th>DateTime</th><th>Activity</th><th>User</th></tr></thead>
        <tbody>
        <tr><td>2026-08-16 10:22:00</td><td>Status Change · Confirmed</td><td>sudha.user</td></tr>
        <tr><td>2026-08-10 08:00:00</td><td>Letter · Received</td><td>System</td></tr>
        </tbody></table>
    """.trimIndent()

    fun appClarificationsHtml(id: Int) = """
        <table>
        <thead><tr><th>DateTime</th><th>Message</th><th>File</th></tr></thead>
        <tbody>
        <tr><td>2026-08-12 11:00:00</td><td>Please confirm travel date</td><td><a href="/show-clarification/$id/3">View</a></td></tr>
        <tr><td>2026-08-08 09:30:00</td><td>Need a doctor's note</td><td>No Upload</td></tr>
        </tbody></table>
    """.trimIndent()

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
        emergencyPresent = emergency,
        history = history,
        flags = flags,
    )

    }
