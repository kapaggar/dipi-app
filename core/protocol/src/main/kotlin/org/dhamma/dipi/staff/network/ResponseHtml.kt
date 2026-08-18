package org.dhamma.dipi.staff.network

import okhttp3.ResponseBody
import retrofit2.Response

/** Drupal often returns the login form as 403; Retrofit then puts HTML in errorBody(). */
fun Response<ResponseBody>.html(): String =
    (body() ?: errorBody())?.string().orEmpty()
