package com.nexalarm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 集中式 HTTP 工具
 * - 自動帶入 Authorization Bearer token（token 非 null 時）
 * - 網路 IOException 時自動重試（最多 3 次，指數退避：1s / 2s / 4s）
 * - HTTP 4xx 不重試，HTTP 5xx 重試
 */
object ApiClient {

    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT    = 15_000
    private const val MAX_RETRIES     = 3

    data class Response(val code: Int, val body: String)

    /** POST JSON，token 非 null 時自動帶入 Authorization header */
    suspend fun post(url: String, body: JSONObject, token: String? = null): Response =
        withContext(Dispatchers.IO) {
            withRetry {
                val conn = open(url, "POST", token)
                try {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    read(conn)
                } finally {
                    conn.disconnect()
                }
            }
        }

    /** GET，token 非 null 時自動帶入 Authorization header */
    suspend fun get(url: String, token: String? = null): Response =
        withContext(Dispatchers.IO) {
            withRetry {
                val conn = open(url, "GET", token)
                try {
                    read(conn)
                } finally {
                    conn.disconnect()
                }
            }
        }

    private fun open(url: String, method: String, token: String?): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = CONNECT_TIMEOUT
            readTimeout    = READ_TIMEOUT
        }

    private fun read(conn: HttpURLConnection): Response {
        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
        }
        return Response(code, body)
    }

    /**
     * 重試包裝：
     * - IOException（網路斷線/逾時）→ 指數退避後重試
     * - HTTP 4xx → 不重試（用戶端錯誤，重試無意義）
     * - HTTP 5xx → 允許重試（伺服器暫時問題）
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastEx: Exception? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                val result = block()
                // 如果是 Response，4xx 不重試
                if (result is Response && result.code in 400..499) return result
                return result
            } catch (e: IOException) {
                lastEx = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(1_000L * (1 shl attempt)) // 1s → 2s → 4s
                }
            }
        }
        throw lastEx!!
    }
}
