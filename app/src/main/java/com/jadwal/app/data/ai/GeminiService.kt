package com.jadwal.app.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jadwal.BuildConfig
import com.jadwal.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Classified Gemini API failures for consistent UI messaging. */
sealed class GeminiError {
    data object ApiKeyInvalid : GeminiError()
    data object QuotaExceeded : GeminiError()
    data object Network : GeminiError()
    data object ModelUnavailable : GeminiError()
    data class Unknown(val detail: String) : GeminiError()
}

/**
 * Single entry point for Gemini API calls.
 *
 * Uses the Gemini **v1** REST endpoint directly via OkHttp + Gson.
 * This replaces the deprecated `com.google.ai.client.generativeai:generativeai:0.9.0` SDK
 * which targeted v1beta and had a MissingFieldException serialization bug on error responses.
 *
 * The user-entered key in DataStore is preferred over `local.properties`/BuildConfig.
 */
@Singleton
class GeminiService @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) {
    private val requestMutex = Mutex()
    private var lastRequestTimeMs = 0L
    private var lastResolvedApiKey: String? = null
    private var activeCandidateIndex: Int = 0

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        /**
         * Model IDs to try in order against the Gemini v1 REST endpoint.
         * The free tier supports gemini-1.5-flash at 15 RPM.
         */
        val MODEL_CANDIDATES: List<String> = listOf(
            "gemini-1.5-flash",
            "gemini-1.5-pro",
        )

        const val MODEL_NAME: String = "gemini-1.5-flash"

        private const val LOG_TAG = "JadwalGemini"
        private const val MIN_INTERVAL_MS = 2_500L

        /** Base URL for the Gemini v1 REST API (not v1beta). */
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1/models"

        private val httpCodeRegex = Regex("""\b(?:HTTP\s*)?(\d{3})\b""")

        fun logRawApiFailure(throwable: Throwable) {
            Log.e(LOG_TAG, "=== Gemini REST failure: ${throwable.javaClass.name} | ${throwable.message}")
            throwable.cause?.let { Log.e(LOG_TAG, "  caused by: ${it.javaClass.name} | ${it.message}") }
        }

        fun extractHttpStatusCode(root: Throwable): Int? {
            var current: Throwable? = root
            var depth = 0
            while (current != null && depth < 8) {
                current.message?.let { msg ->
                    httpCodeRegex.find(msg)?.groupValues?.get(1)?.toIntOrNull()
                        ?.takeIf { it in 100..599 }
                        ?.let { return it }
                }
                current = current.cause
                depth++
            }
            return null
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Key resolution
    // ─────────────────────────────────────────────────────────────────

    suspend fun resolveApiKey(): String? {
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        val storedKey = prefs.getGeminiApiKey().trim()
        val chosen = when {
            storedKey.isNotBlank() -> storedKey
            buildKey.isNotBlank()  -> buildKey
            else                   -> null
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "resolveApiKey: source=" + when {
                    chosen == null                              -> "none"
                    chosen == storedKey && storedKey.isNotBlank() -> "DataStore"
                    else                                       -> "BuildConfig"
                } + " keyLength=" + (chosen?.length ?: 0),
            )
        }
        return chosen?.takeIf { it.isNotBlank() }
    }

    fun clearModelCache() {
        activeCandidateIndex = 0
        lastResolvedApiKey = null
    }

    fun forceKeyRefresh() {
        activeCandidateIndex = 0
        lastResolvedApiKey = null
        Log.d(LOG_TAG, "forceKeyRefresh: candidate index reset, next call will use fresh key")
    }

    private fun ensureApiKeyContext(apiKey: String) {
        if (lastResolvedApiKey != apiKey) {
            lastResolvedApiKey = apiKey
            activeCandidateIndex = 0
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Public generate methods
    // ─────────────────────────────────────────────────────────────────

    suspend fun generateText(prompt: String): String {
        val apiKey = resolveApiKey() ?: throw IllegalStateException("Gemini API key is not configured")
        ensureApiKeyContext(apiKey)
        throttle()

        var lastFailure: Exception? = null
        for (i in activeCandidateIndex until MODEL_CANDIDATES.size) {
            val modelId = MODEL_CANDIDATES[i]
            val freshKey = resolveApiKey() ?: throw IllegalStateException("Gemini API key is not configured")
            Log.d(LOG_TAG, "generateText[candidate=$i] model=\"$modelId\" key=${freshKey.take(5)}...")
            try {
                val body = buildTextRequestBody(prompt)
                val result = executeRequest(modelId, freshKey, body)
                activeCandidateIndex = i
                return result
            } catch (e: GeminiException) {
                lastFailure = e
                if (i < MODEL_CANDIDATES.lastIndex && isModelOrEndpointFailure(e)) {
                    Log.w(LOG_TAG, "Switching candidate after failure on \"$modelId\": ${e.message}")
                    continue
                }
                throw e
            } catch (e: Exception) {
                logRawApiFailure(e)
                val classified = classifyError(e)
                lastFailure = classified as? Exception ?: e
                if (i < MODEL_CANDIDATES.lastIndex && isModelOrEndpointFailure(classified as? GeminiException)) {
                    continue
                }
                throw classified
            }
        }
        throw lastFailure ?: IllegalStateException("Gemini: no model candidates left")
    }

    suspend fun generateFromBitmap(bitmap: Bitmap, prompt: String): String {
        val apiKey = resolveApiKey() ?: throw IllegalStateException("Gemini API key is not configured")
        ensureApiKeyContext(apiKey)
        throttle()

        var lastFailure: Exception? = null
        for (i in activeCandidateIndex until MODEL_CANDIDATES.size) {
            val modelId = MODEL_CANDIDATES[i]
            val freshKey = resolveApiKey() ?: throw IllegalStateException("Gemini API key is not configured")
            Log.d(LOG_TAG, "generateFromBitmap[candidate=$i] model=\"$modelId\" key=${freshKey.take(5)}...")
            try {
                val body = buildImageRequestBody(bitmap, prompt)
                val result = executeRequest(modelId, freshKey, body)
                activeCandidateIndex = i
                return result
            } catch (e: GeminiException) {
                lastFailure = e
                if (i < MODEL_CANDIDATES.lastIndex && isModelOrEndpointFailure(e)) {
                    Log.w(LOG_TAG, "Switching candidate after failure on \"$modelId\": ${e.message}")
                    continue
                }
                throw e
            } catch (e: Exception) {
                logRawApiFailure(e)
                val classified = classifyError(e)
                lastFailure = classified as? Exception ?: e
                if (i < MODEL_CANDIDATES.lastIndex && isModelOrEndpointFailure(classified as? GeminiException)) {
                    continue
                }
                throw classified
            }
        }
        throw lastFailure ?: IllegalStateException("Gemini: no model candidates left")
    }

    // Kept for compatibility with getModel() callers (e.g. bitmap scanner outside chat)
    suspend fun getModel(): Any? = if (resolveApiKey() != null) this else null

    // ─────────────────────────────────────────────────────────────────
    // HTTP execution
    // ─────────────────────────────────────────────────────────────────

    private suspend fun executeRequest(modelId: String, apiKey: String, jsonBody: String): String =
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/$modelId:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = try {
                http.newCall(request).execute()
            } catch (e: IOException) {
                throw GeminiException(
                    GeminiError.Network,
                    rawMessages = e.message ?: "Network error",
                )
            }

            val bodyStr = response.body?.string() ?: ""
            Log.d(LOG_TAG, "HTTP ${response.code} for $modelId — body length=${bodyStr.length}")

            if (!response.isSuccessful) {
                val errorMsg = parseApiError(bodyStr)
                val classified = classifyHttpError(response.code, bodyStr + " " + errorMsg)
                throw GeminiException(classified, httpStatus = response.code, rawMessages = errorMsg.take(500))
            }

            extractTextFromResponse(bodyStr)
                ?: throw GeminiException(GeminiError.Unknown("Empty response from model"), rawMessages = bodyStr.take(200))
        }

    // ─────────────────────────────────────────────────────────────────
    // Request / response builders
    // ─────────────────────────────────────────────────────────────────

    private fun buildTextRequestBody(prompt: String): String {
        val obj = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(mapOf(
                "parts" to listOf(mapOf("text" to prompt))
            ))))
        }
        return gson.toJson(obj)
    }

    private fun buildImageRequestBody(bitmap: Bitmap, prompt: String): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val obj = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(mapOf(
                "parts" to listOf(
                    mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to b64)),
                    mapOf("text" to prompt),
                )
            ))))
        }
        return gson.toJson(obj)
    }

    private fun extractTextFromResponse(bodyStr: String): String? = try {
        val root = JsonParser.parseString(bodyStr).asJsonObject
        root.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?.trim()
    } catch (_: Exception) {
        null
    }

    private fun parseApiError(bodyStr: String): String = try {
        val root = JsonParser.parseString(bodyStr).asJsonObject
        val err = root.getAsJsonObject("error")
        val code = err?.get("code")?.asInt
        val msg  = err?.get("message")?.asString ?: bodyStr.take(300)
        "[$code] $msg"
    } catch (_: Exception) {
        bodyStr.take(300)
    }

    // ─────────────────────────────────────────────────────────────────
    // Error classification
    // ─────────────────────────────────────────────────────────────────

    fun classifyError(e: Throwable): Exception {
        if (e is GeminiException) return e
        val status  = extractHttpStatusCode(e)
        val combined = buildThrowableMessages(e).joinToString(" ")
        return GeminiException(classifyHttpError(status, combined), httpStatus = status, rawMessages = combined.take(500))
    }

    private fun classifyHttpError(status: Int?, combined: String): GeminiError = when (status) {
        401, 403 -> when {
            looksLikeQuota(combined) -> GeminiError.QuotaExceeded
            else                     -> GeminiError.ApiKeyInvalid
        }
        404      -> GeminiError.ModelUnavailable
        429      -> GeminiError.QuotaExceeded
        in 500..599 -> GeminiError.Network
        else     -> classifyFromMessage(combined)
    }

    private fun isModelOrEndpointFailure(e: GeminiException?): Boolean =
        e?.error == GeminiError.ModelUnavailable || e?.httpStatus == 404 || e?.httpStatus == 503

    private fun looksLikeQuota(msg: String): Boolean =
        msg.contains("quota", ignoreCase = true) ||
            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            httpCodeRegex.findAll(msg).any { it.groupValues[1] == "429" }

    private fun classifyFromMessage(combined: String): GeminiError = when {
        combined.contains("API_KEY", ignoreCase = true) ||
            combined.contains("API key", ignoreCase = true) ||
            combined.contains("API_KEY_INVALID", ignoreCase = true) ||
            combined.contains("invalid key", ignoreCase = true) ||
            combined.contains("UNAUTHENTICATED", ignoreCase = true) ||
            (combined.contains("PERMISSION_DENIED", ignoreCase = true) &&
                combined.contains("key", ignoreCase = true)) -> GeminiError.ApiKeyInvalid

        looksLikeQuota(combined) -> GeminiError.QuotaExceeded

        combined.contains("network", ignoreCase = true) ||
            combined.contains("UnknownHostException", ignoreCase = true) ||
            combined.contains("Unable to resolve host", ignoreCase = true) ||
            combined.contains("timeout", ignoreCase = true) ||
            combined.contains("Connection reset", ignoreCase = true) -> GeminiError.Network

        combined.contains("model", ignoreCase = true) &&
            (combined.contains("not found", ignoreCase = true) ||
                combined.contains("NOT_FOUND", ignoreCase = true) ||
                Regex("""\b404\b""").containsMatchIn(combined)) -> GeminiError.ModelUnavailable

        else -> GeminiError.Unknown(combined.take(120))
    }

    private fun buildThrowableMessages(e: Throwable): List<String> {
        val messages = mutableListOf<String>()
        var current: Throwable? = e
        var depth = 0
        while (current != null && depth < 8) {
            current.message?.takeIf { it.isNotBlank() }?.let { messages.add(it) }
            current = current.cause
            depth++
        }
        return messages
    }

    // ─────────────────────────────────────────────────────────────────
    // Throttle
    // ─────────────────────────────────────────────────────────────────

    private suspend fun throttle() {
        requestMutex.withLock {
            val now     = System.currentTimeMillis()
            val elapsed = now - lastRequestTimeMs
            if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
            lastRequestTimeMs = System.currentTimeMillis()
        }
    }
}

class GeminiException(
    val error: GeminiError,
    val httpStatus: Int? = null,
    val rawMessages: String = "",
) : Exception("Gemini error=$error http=$httpStatus detail=${rawMessages.take(160)}")
