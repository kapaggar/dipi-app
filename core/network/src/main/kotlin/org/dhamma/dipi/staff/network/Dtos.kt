package org.dhamma.dipi.staff.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantHistory
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantListPage
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseCount
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.StatusChangeResult
import org.dhamma.dipi.staff.model.StatusWrite

@Serializable
data class LoginBody(val username: String, val password: String)

@Serializable
data class LoginDto(
    val sessid: String = "",
    val session_name: String = "",
    val token: String = "",
    val user: LoginUserDto = LoginUserDto(),
)

@Serializable
data class LoginUserDto(
    @Serializable(with = FlexibleIntSerializer::class)
    val uid: Int = 0,
    val name: String = "",
)

@Serializable
data class SessionDto(
    val uid: Int,
    val name: String,
    val displayName: String,
    val centres: List<CentreDto>,
    val modeTest: Boolean,
) {
    fun toModel() = Session(
        uid = uid,
        name = name,
        displayName = displayName,
        centres = centres.map { Centre(CentreId(it.id), it.name) },
        modeTest = modeTest,
    )
}

@Serializable
data class CentreDto(val id: Int, val name: String)

@Serializable
data class CourseDto(
    val id: Int,
    val centreId: Int,
    val name: String,
    val start: String,
    val end: String,
    val typeKey: String = "",
) {
    fun toModel() = Course(CourseId(id), CentreId(centreId), name, start, end, typeKey)
}

@Serializable
data class CourseListDto(val items: List<CourseDto>)

@Serializable
data class ApplicantListDto(
    val items: List<ApplicantDto>,
    val counts: Map<String, Int>,
    val nextCursor: String? = null,
) {
    fun toModel() = ApplicantListPage(items.map { it.toModel() }, counts, nextCursor)
}

@Serializable
data class HistoryDto(
    val first: String? = null,
    val recent: String? = null,
    val counts: List<CountDto> = emptyList(),
)

@Serializable
data class CountDto(val label: String, val n: Int)

@Serializable
data class FlagDto(
    val severity: String,
    val label: String,
    val detail: String,
    val ruleId: String,
)

@Serializable
data class ApplicantDto(
    val id: Int,
    val centreId: Int,
    val courseId: Int,
    val givenName: String,
    val middleName: String = "",
    val familyName: String,
    val gender: String,
    val status: String,
    val type: String,
    val oldStudent: Boolean,
    val attended: Boolean,
    val confNo: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val phoneHome: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val monk: Boolean = false,
    val createdAt: String? = null,
    val photoUrl: String? = null,
    val emergencyPresent: Boolean? = null,
    val idPresent: Boolean? = null,
    val emergencyNamePresent: Boolean? = null,
    val emergencyEqSelf: Boolean? = null,
    val history: HistoryDto? = null,
    val flags: List<FlagDto> = emptyList(),
) {
    fun toModel() = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(centreId),
        courseId = CourseId(courseId),
        givenName = givenName,
        middleName = middleName,
        familyName = familyName,
        gender = if (gender.equals("M", true)) Gender.M else Gender.F,
        status = ApplicantStatus(status),
        type = if (type.equals("Sevak", true)) ApplicantType.Sevak else ApplicantType.Student,
        oldStudent = oldStudent,
        attended = attended,
        confNo = ConfNo.parseOrNull(confNo),
        email = email,
        mobile = mobile,
        phoneHome = phoneHome,
        city = city,
        state = state,
        country = country,
        dob = dob,
        age = age,
        monk = monk,
        createdAt = createdAt,
        photoUrl = photoUrl,
        emergencyPresent = emergencyPresent,
        idPresent = idPresent,
        emergencyNamePresent = emergencyNamePresent,
        emergencyEqSelf = emergencyEqSelf,
        history = history?.let {
            ApplicantHistory(it.first, it.recent, it.counts.map { c -> CourseCount(c.label, c.n) })
        },
        flags = flags.map {
            AuditFlag(
                severity = runCatching { AuditSeverity.valueOf(it.severity.uppercase()) }
                    .getOrDefault(AuditSeverity.SOFT),
                label = it.label,
                detail = it.detail,
                ruleId = it.ruleId,
            )
        },
    )
}

@Serializable
data class StatusesDto(val items: List<StatusItemDto>)

@Serializable
data class StatusItemDto(val key: String? = null, val value: String)

/**
 * `POST /app-update-attended/{id}` reply — only the verdict. The live JSON
 * also carries `applicant`/`attended` HTML lists plus `acco`/`alloted` maps;
 * the converter's ignoreUnknownKeys drops them, they are never parsed.
 */
@Serializable
data class AttendedUpdateDto(
    val status: Boolean = false,
    val msg: String = "",
)

@Serializable
data class ChangeStatusDto(
    val status: String = "",
    val msg: String = "",
    val confno: String = "",
    val newstatus: String = "",
) {
    fun toModel() = StatusWrite.parseResult(status, msg, confno, newstatus)
}

@Serializable
data class PhotoReviewListDto(val items: List<PhotoReviewDto>)

@Serializable
data class PhotoReviewDto(
    val applicantId: Int,
    val kind: String,
    val badge: String,
    val suggestedRotate: Int = 0,
    val suggestedCrop: Boolean = false,
) {
    fun toModel() = PhotoReviewItem(ApplicantId(applicantId), kind, badge, suggestedRotate, suggestedCrop)
}

@Serializable
data class PhotoUploadBody(val rotate: Int, val crop: CropDto? = null)

@Serializable
data class CropDto(val l: Float = 0f, val t: Float = 0f, val r: Float = 1f, val b: Float = 1f)

@Serializable
data class PhotoUploadResultDto(val ok: Boolean, val driftedFields: List<String> = emptyList(), val message: String = "")
