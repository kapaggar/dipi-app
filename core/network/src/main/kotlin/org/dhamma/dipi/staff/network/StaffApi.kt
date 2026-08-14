package org.dhamma.dipi.staff.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
}
