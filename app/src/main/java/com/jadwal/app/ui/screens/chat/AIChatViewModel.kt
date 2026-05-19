package com.jadwal.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jadwal.BuildConfig
import com.jadwal.R
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
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
    // تم حذف الرسالة الثابتة من هنا وتمريرها ديناميكياً
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val errorMessage: String? = null,
    val showApiKeySetup: Boolean = false,
    val apiKeyInput: String = "",
    val apiKeySaved: Boolean = false,
    val hasApiKey: Boolean = false,
)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    // جلب Context للوصول إلى strings.xml المترجمة
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val gson = Gson()
    private var generativeModel: GenerativeModel? = null

    init {
        // تعيين رسالة الترحيب حسب لغة التطبيق
        _uiState.update { it.copy(messages = listOf(getWelcomeMessage())) }
        initializeApiKey()
        loadSavedMessages()
    }

    private fun getWelcomeMessage(): ChatMessage {
        return ChatMessage(
            role = MessageRole.ASSISTANT,
            content = context.getString(R.string.ai_welcome_message)
        )
    }

    private fun initializeApiKey() {
        viewModelScope.launch {
            val storedKey = prefs.getGeminiApiKey()
            val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }

            val effectiveKey = when {
                storedKey.isNotBlank() -> storedKey
                buildKey.isNotBlank()  -> buildKey
                else                   -> ""
            }

            if (effectiveKey.isNotBlank()) {
                generativeModel = buildModel(effectiveKey)
                _uiState.update { it.copy(hasApiKey = true, showApiKeySetup = false) }
            } else {
                _uiState.update { it.copy(hasApiKey = false, showApiKeySetup = true) }
            }
        }
    }

    private fun buildModel(apiKey: String): GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
    )

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) return

        viewModelScope.launch {
            prefs.setGeminiApiKey(key)
            generativeModel = buildModel(key)
            _uiState.update {
                it.copy(
                    hasApiKey = true,
                    showApiKeySetup = false,
                    apiKeySaved = true,
                    apiKeyInput = "",
                )
            }
        }
    }

    fun onApiKeyInputChange(text: String) {
        _uiState.update { it.copy(apiKeyInput = text, apiKeySaved = false) }
    }

    fun showApiKeySetup() {
        _uiState.update { it.copy(showApiKeySetup = true) }
    }

    fun hideApiKeySetup() {
        _uiState.update { it.copy(showApiKeySetup = false) }
    }

    private fun loadSavedMessages() {
        viewModelScope.launch {
            try {
                val json = prefs.chatMessagesJson.first()
                val welcome = getWelcomeMessage() // جلب الرسالة المترجمة حالياً

                if (json.isNotBlank()) {
                    val type = object : TypeToken<List<ChatMessageDto>>() {}.type
                    val dtos: List<ChatMessageDto> = gson.fromJson(json, type)

                    // استبدال أول رسالة (الترحيب) بالرسالة المترجمة فوراً
                    val messages = dtos.map { it.toChatMessage() }.toMutableList()
                    if (messages.isNotEmpty()) {
                        messages[0] = welcome
                    } else {
                        messages.add(welcome)
                    }

                    _uiState.update { it.copy(messages = messages) }
                    // ... بقية كود تحميل المحادثة
                } else {
                    _uiState.update { it.copy(messages = listOf(welcome)) }
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

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearHistory() {
        val welcome = getWelcomeMessage()
        _uiState.update { AIChatUiState(hasApiKey = _uiState.value.hasApiKey, messages = listOf(welcome)) }
        conversationHistory.clear()
        persistMessages(listOf(welcome))
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isTyping) return

        val model = generativeModel
        if (model == null || !_uiState.value.hasApiKey) {
            _uiState.update { it.copy(showApiKeySetup = true) }
            return
        }

        val userMessage = ChatMessage(role = MessageRole.USER, content = text)
        val loadingMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "...", isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + loadingMessage,
                inputText = "",
                isTyping = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val reply = callGemini(model, text)
                conversationHistory.add(text to reply)
                if (conversationHistory.size > 6) conversationHistory.removeAt(0)

                val newMessages = _uiState.value.messages.dropLast(1) +
                        ChatMessage(role = MessageRole.ASSISTANT, content = reply)

                _uiState.update { state -> state.copy(messages = newMessages, isTyping = false) }
                persistMessages(newMessages)

            } catch (e: Exception) {
                val errorMsg = buildErrorMessage(e)
                val newMessages = _uiState.value.messages.dropLast(1) +
                        ChatMessage(role = MessageRole.ASSISTANT, content = errorMsg)
                _uiState.update { state ->
                    state.copy(messages = newMessages, isTyping = false, errorMessage = errorMsg)
                }
            }
        }
    }

    private suspend fun callGemini(model: GenerativeModel, userInput: String): String {
        // فحص لغة التطبيق الحالية
        val isEn = Locale.getDefault().language == "en"

        val historyContext = if (conversationHistory.isNotEmpty()) {
            conversationHistory.takeLast(4).joinToString("\n") { (q, a) ->
                if (isEn) "User: $q\nYou: $a" else "المستخدم: $q\nأنت: $a"
            } + "\n\n"
        } else ""

        val userName = try { prefs.userName.first() } catch (_: Exception) { "" }
        val nameCtx = if (userName.isNotBlank()) {
            if (isEn) "Student Name: $userName\n" else "اسم الطالب: $userName\n"
        } else ""

        // تحديد لغة الأوامر التي نعطيها لـ Gemini
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

        val response = model.generateContent(content { text(prompt) })
        return response.text?.trim() ?: if (isEn) "Sorry, I didn't understand your question. Can you rephrase?" else "آسف، لم أفهم سؤالك. هل يمكنك إعادة صياغته؟"
    }

    private fun buildErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        val isEn = Locale.getDefault().language == "en"
        return when {
            msg.contains("API_KEY", ignoreCase = true) ||
                    msg.contains("API key", ignoreCase = true) ||
                    msg.contains("invalid key", ignoreCase = true) ->
                if (isEn) "⚠️ Invalid Gemini API Key.\nTap the 🔑 icon to update it."
                else "⚠️ مفتاح Gemini API غير صحيح.\nاضغط على أيقونة المفتاح 🔑 لتحديثه."

            msg.contains("quota", ignoreCase = true) ||
                    msg.contains("rate limit", ignoreCase = true) ||
                    msg.contains("429") ->
                if (isEn) "⏳ Rate limit exceeded.\nWait a minute and try again."
                else "⏳ تجاوزت عدد الطلبات المسموح.\nانتظر دقيقة وحاول مجدداً."

            msg.contains("network", ignoreCase = true) ||
                    msg.contains("UnknownHostException", ignoreCase = true) ->
                if (isEn) "📵 No internet connection.\nCheck your connection and try again."
                else "📵 لا يوجد اتصال بالإنترنت.\nتحقق من اتصالك وحاول مجدداً."

            msg.contains("model", ignoreCase = true) ||
                    msg.contains("not found", ignoreCase = true) ->
                if (isEn) "⚠️ Model currently unavailable. Try again later."
                else "⚠️ النموذج غير متاح. حاول مجدداً لاحقاً."

            else -> if (isEn) "❌ Error: ${msg.take(80)}" else "❌ حدث خطأ: ${msg.take(80)}"
        }
    }
}