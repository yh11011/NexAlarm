package com.nexalarm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: Int,
    val username: String?,
    val email: String?,
    val displayName: String?,
    val token: String
)

object AuthRepository {
    // alarm.nex11.me/auth/* proxy 轉發到 auth 服務（port 9000）
    private const val BASE_URL = "https://alarm.nex11.me/auth"

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("username_or_email", usernameOrEmail)
                    put("password", password)
                }
                postJson("$BASE_URL/login", body)
            }
        }

    suspend fun register(
        usernameOrEmail: String,
        password: String,
        displayName: String?
    ): Result<AuthUser> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("username_or_email", usernameOrEmail)
                put("password", password)
                if (!displayName.isNullOrBlank()) put("display_name", displayName)
            }
            postJson("$BASE_URL/register", body)
        }
    }

    /** 向伺服器驗證優惠碼，成功返回 true，無效返回 false，網路錯誤 throws */
    suspend fun validatePromoCode(code: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply { put("code", code) }
                val conn = (URL("$BASE_URL/promo/validate").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                try {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    conn.responseCode == 200
                } finally {
                    conn.disconnect()
                }
            }
        }

    private fun postJson(url: String, body: JSONObject): AuthUser {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            } else {
                val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: "HTTP $responseCode"
                val detail = runCatching {
                    JSONObject(errorText).optString("detail").takeIf { it.isNotBlank() }
                }.getOrNull() ?: errorText
                throw Exception(detail)
            }

            val json = JSONObject(responseBody)
            val token = json.getString("access_token")
            val user = json.getJSONObject("user")
            return AuthUser(
                id = user.getInt("id"),
                username = user.optString("username").takeIf { it.isNotEmpty() && it != "null" },
                email = user.optString("email").takeIf { it.isNotEmpty() && it != "null" },
                displayName = user.optString("display_name").takeIf { it.isNotEmpty() && it != "null" },
                token = token
            )
        } finally {
            conn.disconnect()
        }
    }
}
