package com.nexalarm.app.data

import org.json.JSONObject

/**
 * AI 服務綁定 & Chat 倉庫
 */
class AiRepository {
    companion object {
        private const val BASE_URL = "https://alarm.nex11.me/auth"

        /**
         * 綁定 AI API key
         * @return { success: Boolean, provider: String, key_preview: String } 或錯誤訊息
         */
        suspend fun bindAiKey(provider: String, apiKey: String, token: String): Result<JSONObject> {
            return try {
                val body = JSONObject()
                body.put("provider", provider.lowercase())
                body.put("api_key", apiKey)

                val response = ApiClient.post("$BASE_URL/ai-key/bind", body, token)
                if (response.code == 200) {
                    Result.success(JSONObject(response.body))
                } else {
                    val errorObj = JSONObject(response.body)
                    val message = errorObj.optString("detail", "Failed to bind AI key")
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * 查詢已綁定的 AI 服務列表
         * @return { bindings: [{ provider: String, key_preview: String }] }
         */
        suspend fun getAiKeyStatus(token: String): Result<JSONObject> {
            return try {
                val response = ApiClient.get("$BASE_URL/ai-key/status", token)
                if (response.code == 200) {
                    Result.success(JSONObject(response.body))
                } else {
                    Result.failure(Exception("Failed to get AI key status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * 解除 AI 綁定
         */
        suspend fun unbindAiKey(provider: String, token: String): Result<JSONObject> {
            return try {
                val response = ApiClient.delete("$BASE_URL/ai-key/$provider", token)
                if (response.code == 200) {
                    Result.success(JSONObject(response.body))
                } else {
                    Result.failure(Exception("Failed to unbind AI key"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * 與 AI 對話（用自然語言管理鬧鐘）
         * @param message 使用者訊息
         * @return { reply: String, action: "create_alarm"|"delete_alarm"|"list_alarms", alarm?: {...}, alarms?: [...], client_id?: String }
         */
        suspend fun chatWithAi(message: String, token: String): Result<JSONObject> {
            return try {
                val body = JSONObject()
                body.put("message", message)

                val response = ApiClient.post("$BASE_URL/chat", body, token)
                if (response.code == 200) {
                    Result.success(JSONObject(response.body))
                } else {
                    val errorObj = try {
                        JSONObject(response.body)
                    } catch (e: Exception) {
                        JSONObject()
                    }
                    val message = errorObj.optString("detail", "Chat request failed")
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
