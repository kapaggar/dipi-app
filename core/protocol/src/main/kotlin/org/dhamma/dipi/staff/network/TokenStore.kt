package org.dhamma.dipi.staff.network

interface TokenStore {
    suspend fun sessionCookie(): String?
    suspend fun csrf(): String?
    suspend fun saveSession(cookie: String?, csrf: String?)
    suspend fun clear()
}
