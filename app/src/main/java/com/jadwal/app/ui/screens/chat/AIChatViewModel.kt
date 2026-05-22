package com.jadwal.ui.screens.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.R
import com.jadwal.app.data.ai.GeminiError
import com.jadwal.app.data.ai.GeminiException
import com.jadwal.app.data.ai.GeminiService
import com.jadwal.app.util.LocaleHelper
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val isLoading: Boolean = false,
)

private data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
) {
    fun toChatMessage() = ChatMessage(id = id, role = MessageRole.valueOf(role), content = content)
    companion object {
        fun from(msg: ChatMessage) = ChatMessageDto(id = msg.id, role = msg.role.name, content = msg.content)
    }
}

data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val errorMessage: String? = null,
    val showApiKeySetup: Boolean = false,
    val apiKeyInput: String = "",
    val apiKeySaved: Boolean = false,
    val hasApiKey: Boolean = false,
    /** One-shot Toast text; consumed by the UI and cleared via clearToast(). */
    val toastMessage: String? = null,
)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val geminiService: GeminiService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val gson = Gson()
    private val requestInFlight = AtomicBoolean(false)

    init {
        _uiState.update { it.copy(messages = listOf(getWelcomeMessage())) }
        initializeApiKey()
        loadSavedMessages()
        viewModelScope.launch {
            prefs.languageCode
                .distinctUntilChanged()
                .collect { refreshWelcomeForLocale() }
        }
    }

    private fun isEnglish(): Boolean = LocaleHelper.isEnglish()

    private fun getWelcomeMessage(): ChatMessage =
        ChatMessage(
            role = MessageRole.ASSISTANT,
            content = context.getString(R.string.ai_welcome_message),
        )

    private fun refreshWelcomeForLocale() {
        val welcome = getWelcomeMessage()
        val current = _uiState.value.messages
        val updated = if (current.isEmpty()) {
            listOf(welcome)
        } else {
            current.toMutableList().also { it[0] = welcome }
        }
        _uiState.update { it.copy(messages = updated) }
        persistMessages(updated)
    }

    private fun initializeApiKey() {
        viewModelScope.launch {
            val hasKey = geminiService.resolveApiKey() != null
            _uiState.update {
                it.copy(hasApiKey = hasKey, showApiKeySetup = !hasKey)
            }
        }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) return
        viewModelScope.launch {
            // 1. Persist to DataStore — await completion before anything else.
            prefs.setGeminiApiKey(key)

            // 2. Force GeminiService to forget its cached key/model so the very
            //    next call picks up the freshly-stored key.
            geminiService.forceKeyRefresh()

            // 3. Verify the key was actually stored.
            val hasKey = geminiService.resolveApiKey() != null
            Log.d("JadwalChat", "saveApiKey: written=${key.length} chars, hasKey=$hasKey")

            // 4. Update UI state and show a Toast confirmation.
            _uiState.update {
                it.copy(
                    hasApiKey = hasKey,
                    showApiKeySetup = false,
                    apiKeySaved = true,
                    apiKeyInput = "",
                    toastMessage = if (hasKey) "API Key saved successfully ✓" else "Failed to save key — please try again",
                )
            }
        }
    }

    /** Call from the UI after the Toast has been shown to avoid re-showing it. */
    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }

    fun onApiKeyInputChange(text: String) {
        _uiState.update { it.copy(apiKeyInput = text, apiKeySaved = false) }
    }

    fun showApiKeySetup() { _uiState.update { it.copy(showApiKeySetup = true) } }
    fun hideApiKeySetup() { _uiState.update { it.copy(showApiKeySetup = false) } }

    private fun loadSavedMessages() {
        viewModelScope.launch {
            try {
                val json = prefs.chatMessagesJson.first()
                val freshWelcome = getWelcomeMessage()

                if (json.isNotBlank()) {
                    val type = object : TypeToken<List<ChatMessageDto>>() {}.type
                    val dtos: List<ChatMessageDto> = gson.fromJson(json, type)
                    val messages = dtos.map { it.toChatMessage() }.toMutableList()
                    if (messages.isNotEmpty()) messages[0] = freshWelcome
                    else messages.add(freshWelcome)
                    _uiState.update { it.copy(messages = messages) }
                } else {
                    _uiState.update { it.copy(messages = listOf(freshWelcome)) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun persistMessages(messages: List<ChatMessage>) {
        viewModelScope.launch {
            try {
                val dtos = messages.filter { !it.isLoading }.map { ChatMessageDto.from(it) }
                prefs.saveChatMessagesJson(gson.toJson(dtos))
            } catch (_: Exception) { }
        }
    }

    fun onInputChange(text: String) { _uiState.update { it.copy(inputText = text) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    fun clearHistory() {
        val welcome = getWelcomeMessage()
        _uiState.update { AIChatUiState(hasApiKey = _uiState.value.hasApiKey, messages = listOf(welcome)) }
        conversationHistory.clear()
        persistMessages(listOf(welcome))
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        if (!_uiState.value.hasApiKey) {
            _uiState.update { it.copy(showApiKeySetup = true) }
            return
        }
        if (!requestInFlight.compareAndSet(false, true)) return

        val userMessage = ChatMessage(role = MessageRole.USER, content = text)
        val loadingMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "...", isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + loadingMessage,
                inputText = "",
                isTyping = false,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val reply = callGemini(text)
                conversationHistory.add(text to reply)
                if (conversationHistory.size > 6) conversationHistory.removeAt(0)

                val newMessages = _uiState.value.messages.dropLast(1) +
                    ChatMessage(role = MessageRole.ASSISTANT, content = reply)
                _uiState.update { it.copy(messages = newMessages) }
                persistMessages(newMessages)
            } catch (e: Exception) {
                val errorMsg = buildErrorMessage(e)
                val newMessages = _uiState.value.messages.dropLast(1) +
                    ChatMessage(role = MessageRole.ASSISTANT, content = errorMsg)
                _uiState.update { it.copy(messages = newMessages, errorMessage = errorMsg) }
            } finally {
                requestInFlight.set(false)
            }
        }
    }

    private suspend fun callGemini(userInput: String): String {
        val isEn = isEnglish()

        val historyContext = if (conversationHistory.isNotEmpty()) {
            conversationHistory.takeLast(4).joinToString("\n") { (q, a) ->
                if (isEn) "User: $q\nYou: $a" else "المستخدم: $q\nأنت: $a"
            } + "\n\n"
        } else ""

        val userName = try { prefs.userName.first() } catch (_: Exception) { "" }
        val nameCtx = if (userName.isNotBlank()) {
            if (isEn) "Student Name: $userName\n" else "اسم الطالب: $userName\n"
        } else ""

        val prompt = if (isEn) {
            """
            You are a smart specialized study assistant. Your name is "Jadwal AI".
            You MUST answer in English unless the user asks in another language.
            Your responses are clear, concise, and helpful (max 4 sentences).
            Encourage and motivate the student.
            $nameCtx
            ${if (historyContext.isNotBlank()) "Conversation Context:\n$historyContext" else ""}
            User Question: $userInput
            """.trimIndent()
        } else {
            """
            أنت مساعد دراسي ذكي متخصص. اسمك "جدول AI".
            تجيب دائماً باللغة العربية ما لم يسأل المستخدم بلغة أخرى.
            ردودك واضحة ومختصرة ومفيدة (لا تتجاوز 4 جمل).
            تُشجّع الطلاب وتدعم تحفيزهم.
            $nameCtx
            ${if (historyContext.isNotBlank()) "سياق المحادثة:\n$historyContext" else ""}
            سؤال المستخدم: $userInput
            """.trimIndent()
        }

        // Log a masked version of the key actually being used so it's visible in Logcat.
        val activeKey = geminiService.resolveApiKey()
        if (activeKey != null) {
            val masked = activeKey.take(8) + "***" + activeKey.takeLast(4)
            Log.d("JadwalChat", "Using API key: $masked (len=${activeKey.length})")
        } else {
            Log.w("JadwalChat", "No API key resolved — request will fail.")
        }

        val reply = geminiService.generateText(prompt)
        return reply.ifBlank {
            if (isEn) "Sorry, I didn't understand. Can you rephrase?"
            else "آسف، لم أفهم سؤالك. هل يمكنك إعادة صياغته؟"
        }
    }

    private fun buildErrorMessage(e: Exception): String {
        val geminiEx = when (e) {
            is GeminiException -> e
            else -> geminiService.classifyError(e) as? GeminiException
        }
        val error = geminiEx?.error ?: GeminiError.Unknown(e.message.orEmpty())
        val isEn = isEnglish()
        return when (error) {
            GeminiError.ApiKeyInvalid ->
                if (isEn) "⚠️ Invalid Gemini API Key.\nTap the 🔑 icon to update it."
                else "⚠️ مفتاح Gemini API غير صحيح.\nاضغط على أيقونة المفتاح 🔑 لتحديثه."

            GeminiError.QuotaExceeded ->
                if (isEn) "⏳ API quota reached.\nWait a few minutes or visit Google AI Studio."
                else "⏳ تم استنفاد حصة الـ API مؤقتاً.\nانتظر بضع دقائق، أو زُر Google AI Studio."

            GeminiError.Network ->
                if (isEn) "📵 No internet connection.\nCheck your connection and try again."
                else "📵 لا يوجد اتصال بالإنترنت.\nتحقق من اتصالك وحاول مجدداً."

            GeminiError.ModelUnavailable ->
                if (isEn) "⚠️ Model unavailable. Try again later."
                else "⚠️ النموذج غير متاح حالياً. حاول مجدداً لاحقاً."

            is GeminiError.Unknown ->
                if (isEn) "❌ Error: ${error.detail.take(100)}"
                else "❌ حدث خطأ: ${error.detail.take(100)}"
        }
    }
}
