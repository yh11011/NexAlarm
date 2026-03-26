package com.nexalarm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
                parseAuthResponse(ApiClient.post("$BASE_URL/login", body))
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
            parseAuthResponse(ApiClient.post("$BASE_URL/register", body))
        }
    }

    /** 通知後端登出（stateless JWT；忽略網路失敗，本地已清除 token） */
    suspend fun logout(token: String) = withContext(Dispatchers.IO) {
        runCatching { ApiClient.post("$BASE_URL/logout", JSONObject(), token) }
    }

    /** 向伺服器驗證優惠碼，成功返回 true，無效返回 false，網路錯誤 throws */
    suspend fun validatePromoCode(code: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply { put("code", code) }
                val resp = ApiClient.post("$BASE_URL/promo/validate", body)
                resp.code == 200
            }
        }

    private fun parseAuthResponse(resp: ApiClient.Response): AuthUser {
        if (resp.code !in 200..299) {
            val raw = runCatching {
                JSONObject(resp.body).optString("detail").takeIf { it.isNotBlank() }
            }.getOrNull() ?: resp.body
            throw Exception(toFriendlyMessage(resp.code, raw))
        }
        val json  = JSONObject(resp.body)
        val token = json.getString("access_token")
        val user  = json.getJSONObject("user")
        return AuthUser(
            id          = user.getInt("id"),
            username    = user.optString("username").takeIf    { it.isNotEmpty() && it != "null" },
            email       = user.optString("email").takeIf       { it.isNotEmpty() && it != "null" },
            displayName = user.optString("display_name").takeIf { it.isNotEmpty() && it != "null" },
            token       = token
        )
    }

    private fun toFriendlyMessage(code: Int, raw: String): String = when {
        code == 401 || raw.contains("Incorrect", ignoreCase = true)
            || raw.contains("Invalid",   ignoreCase = true) -> "帳號或密碼錯誤"
        code == 409 || raw.contains("already", ignoreCase = true)
            || raw.contains("exist",    ignoreCase = true)  -> "此帳號已被註冊，請直接登入"
        code == 422 || raw.contains("validation", ignoreCase = true) -> "請填寫正確的帳號和密碼"
        code == 429                                          -> "請求過於頻繁，請稍後再試"
        code >= 500                                          -> "伺服器暫時無回應，請稍後再試"
        else                                                 -> "操作失敗，請重試"
    }
}
