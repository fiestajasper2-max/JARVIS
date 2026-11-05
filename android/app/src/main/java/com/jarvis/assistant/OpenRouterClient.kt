package com.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterClient(private val vault: SecretVault) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun reply(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = vault.getKey() ?: error("OpenRouter API key is not configured.")
        val messages = JSONArray()
        messages.put(JSONObject()
            .put("role", "system")
            .put("content", "You are JARVIS, a calm, intelligent assistant. Address the user as Chief. Use concise, useful responses."))
        history.takeLast(30).forEach { item ->
            messages.put(JSONObject().put("role", item.role).put("content", item.content))
        }

        val body = JSONObject()
            .put("model", "openrouter/auto")
            .put("messages", messages)
            .put("temperature", 0.7)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/fiestajasper2-max/JARVIS")
            .header("X-Title", "J.A.R.V.I.S. Android")
            .post(body)
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                error(detail?.takeIf { it.isNotBlank() } ?: "OpenRouter request failed (${response.code}).")
            }
            JSONObject(raw).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message")
                .optString("content")
                .takeIf { it.isNotBlank() }
                ?: error("OpenRouter returned an empty response.")
        }
    }
}
