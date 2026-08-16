package org.dhamma.dipi.staff.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface DrupalAuthApi {
    @POST("/api/user/login")
    suspend fun login(@Body body: LoginBody): LoginDto

    @POST("/api/user/logout")
    suspend fun logout()

    @GET("/services/session/token")
    suspend fun csrfToken(): ResponseBody
}

interface StaffApi {
    @GET("/staff/session")
    suspend fun session(): SessionDto

    @GET("/staff/centres/{cid}/courses")
    suspend fun courses(
        @Path("cid") centreId: Int,
        @Query("upcoming") upcoming: Int = 1,
    ): CourseListDto

    @GET("/staff/courses/{id}/applicants")
    suspend fun applicants(
        @Path("id") courseId: Int,
        @Query("status") status: String? = null,
        @Query("q") q: String? = null,
        @Query("cursor") cursor: String? = null,
    ): ApplicantListDto

    @GET("/staff/applicants/{id}")
    suspend fun applicant(@Path("id") id: Int): ApplicantDto

    @GET("/staff/meta/statuses")
    suspend fun statuses(): StatusesDto

    @GET("/staff/courses/{id}/photo-review")
    suspend fun photoReview(@Path("id") courseId: Int): PhotoReviewListDto

    @POST("/staff/applicants/{id}/photo")
    suspend fun uploadPhoto(@Path("id") id: Int, @Body body: PhotoUploadBody): PhotoUploadResultDto

    @GET("/change-status/{id}")
    suspend fun changeStatusGet(
        @Path("id") id: Int,
        @Query("s") status: String,
        @Query("l") letterId: Int = 0,
        @Query("c") comment: String = "",
    ): ChangeStatusDto

    /** Canonical v1 write. Always send l=0. Never send s=Approved from the sheet. */
    @POST("/change-status/{id}")
    suspend fun changeStatus(
        @Path("id") id: Int,
        @Query("s") status: String,
        @Query("l") letterId: Int = 0,
        @Query("c") comment: String = "",
    ): ChangeStatusDto

    /**
     * The worklist dialog's own allocation write (owner amendment 2026-08-16):
     * `dh_app_update_attended` — plain menu callback, session cookie only, no
     * form token. Fields are exactly the dialog's `s,r,g,l,v,c,cf,chow,chai,
     * back,comment,a` (build via RoomAllocSync.params). No status, no NPI.
     */
    @FormUrlEncoded
    @POST("/app-update-attended/{id}")
    suspend fun updateAttended(
        @Path("id") id: Int,
        @FieldMap fields: Map<String, String>,
    ): AttendedUpdateDto

    /** Live desk: Drupal form that embeds `var dataset`. */
    @GET("/search-app")
    suspend fun searchAppLanding(): Response<ResponseBody>

    @GET("/search-app/{cid}")
    suspend fun searchAppForm(@Path("cid") centreId: Int): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/search-app/{cid}")
    suspend fun searchAppSubmit(
        @Path("cid") centreId: Int,
        @FieldMap fields: Map<String, String>,
    ): Response<ResponseBody>

    /** Live desk JSON. Permission: transfer course. Optional. */
    @GET("/get-courses/{cid}")
    suspend fun getCourses(@Path("cid") centreId: Int): List<LiveCourseDto>

    /** HAR: anonymous GET / is 403 but includes user_login_block. */
    @GET("/")
    suspend fun siteRoot(): Response<ResponseBody>

    /** 200 HTML (unlike GET / which is 403). Use when the desk 403 page has no form. */
    @GET("/user/login")
    suspend fun userLogin(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/home")
    suspend fun loginBlock(
        @Query("destination") destination: String = "home",
        @Field("name") name: String,
        @Field("pass") pass: String,
        @Field("form_build_id") formBuildId: String,
        @Field("form_id") formId: String = "user_login_block",
        @Field("op") op: String = "Log in",
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun submitLogin(
        @Url action: String,
        @Field("name") name: String,
        @Field("pass") pass: String,
        @Field("form_build_id") formBuildId: String,
        @Field("form_id") formId: String,
        @Field("op") op: String = "Log in",
    ): Response<ResponseBody>

    @GET("/user/logout")
    suspend fun logoutGet(): Response<ResponseBody>

    @GET("/centre")
    suspend fun centreLanding(): Response<ResponseBody>

    @GET("/centre/{cid}")
    suspend fun centreDashboard(@Path("cid") centreId: Int): Response<ResponseBody>

    /**
     * Centre room config: the DataTables source the browser loads for
     * `/centre/{cid}/edit`'s Accommodation table. GET only — read-only here.
     */
    @GET("/centre/{cid}/acco-handler")
    suspend fun accoHandler(@Path("cid") centreId: Int): Response<ResponseBody>

    @GET("/search-course/{cid}/{courseId}")
    suspend fun searchCourse(
        @Path("cid") centreId: Int,
        @Path("courseId") courseId: Int,
        @Query("s") status: String = "",
        @Query("t") old: String = "",
        @Query("g") gender: String = "",
        @Query("d") db: String = "a",
    ): Response<ResponseBody>
}
