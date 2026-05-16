package com.jadwal.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val isLoading: Boolean = false,
)

// DTO لحفظ الرسائل بدون isLoading
private data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
) {
    fun toChatMessage() = ChatMessage(
        id = id,
        role = MessageRole.valueOf(role),
        content = content,
        isLoading = false,
    )

    companion object {
        fun from(msg: ChatMessage) = ChatMessageDto(
            id = msg.id,
            role = msg.role.name,
            content = msg.content,
        )
    }
}

private val WELCOME_MESSAGE = ChatMessage(
    role = MessageRole.ASSISTANT,
    content = "مرحباً! أنا مساعدك الدراسي المدعوم بـ Gemini. " +
            "يمكنني مساعدتك في:\n" +
            "• تنظيم وقت المذاكرة\n" +
            "• شرح المواد الصعبة\n" +
            "• نصائح لتحسين التركيز\n" +
            "• خطط المراجعة قبل الامتحانات\n\n" +
            "اسألني أي شيء يتعلق بدراستك! 📚",
)

data class AIChatUiState(
    val messages: List<ChatMessage> = listOf(WELCOME_MESSAGE),
    val inputText: String = "",
    val isTyping: Boolean = false,
)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val gson = Gson()

    init {
        loadSavedMessages()
    }

    // ===== تحميل المحادثة المحفوظة =====
    private fun loadSavedMessages() {
        viewModelScope.launch {
            try {
                val json = prefs.chatMessagesJson.first()
                if (json.isNotBlank()) {
                    val type = object : TypeToken<List<ChatMessageDto>>() {}.type
                    val dtos: List<ChatMessageDto> = gson.fromJson(json, type)
                    if (dtos.isNotEmpty()) {
                        val messages = dtos.map { it.toChatMessage() }
                        _uiState.update { it.copy(messages = messages) }
                        // إعادة بناء سياق المحادثة للـ AI
                        val pairs = mutableListOf<Pair<String, String>>()
                        var lastUser: ChatMessage? = null
                        for (msg in messages) {
                            if (msg.role == MessageRole.USER) {
                                lastUser = msg
                            } else if (msg.role == MessageRole.ASSISTANT && lastUser != null) {
                                pairs.add(lastUser.content to msg.content)
                                lastUser = null
                            }
                        }
                        conversationHistory.addAll(pairs.takeLast(4))
                    }
                }
            } catch (_: Exception) {
                // إبقاء رسالة الترحيب الافتراضية
            }
        }
    }

    // ===== حفظ المحادثة =====
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

    fun clearHistory() {
        _uiState.update { AIChatUiState() }
        conversationHistory.clear()
        persistMessages(listOf(WELCOME_MESSAGE))
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isTyping) return

        val userMessage = ChatMessage(role = MessageRole.USER, content = text)
        val loadingMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "...", isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + loadingMessage,
                inputText = "",
                isTyping = true,
            )
        }

        viewModelScope.launch {
            try {
                val reply = callGemini(text)
                conversationHistory.add(text to reply)

                val newMessages = _uiState.value.messages.dropLast(1) +
                        ChatMessage(role = MessageRole.ASSISTANT, content = reply)

                _uiState.update { state ->
                    state.copy(messages = newMessages, isTyping = false)
                }
                persistMessages(newMessages)
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("API_KEY", ignoreCase = true) == true ||
                    e.message?.contains("API key", ignoreCase = true) == true ->
                        "خطأ في مفتاح Gemini API. تأكد من إضافة GEMINI_API_KEY في ملف local.properties."
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve", ignoreCase = true) == true ->
                        "تعذّر الاتصال بالإنترنت. تحقق من اتصالك وحاول مجدداً."
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "تجاوزت الحد المسموح به من الطلبات. انتظر قليلاً وحاول مجدداً."
                    else ->
                        "حدث خطأ: ${e.message?.take(120) ?: "خطأ غير معروف"}"
                }
                _uiState.update { state ->
                    val updatedMessages = state.messages.dropLast(1) +
                            ChatMessage(role = MessageRole.ASSISTANT, content = errorMsg)
                    state.copy(messages = updatedMessages, isTyping = false)
                }
            }
        }
    }

    private suspend fun callGemini(userInput: String): String {
        val historyContext = if (conversationHistory.isNotEmpty()) {
            conversationHistory.takeLast(4).joinToString("\n") { (q, a) ->
                "المستخدم: $q\nأنت: $a"
            } + "\n\n"
        } else ""

        val systemPrompt = """
            أنت مساعد دراسي ذكي متخصص في مساعدة الطلاب. اسمك "جدول AI".
            تجيب دائماً باللغة العربية ما لم يسأل المستخدم بلغة أخرى.
            ردودك واضحة ومختصرة ومفيدة. تُشجّع الطلاب وتدعم تحفيزهم.
            يمكنك مساعدتهم في: تنظيم الوقت، شرح المواد، نصائح الدراسة، والتحضير للامتحانات.
            
            ${if (historyContext.isNotBlank()) "سياق المحادثة السابقة:\n$historyContext" else ""}
            
            سؤال المستخدم: $userInput
        """.trimIndent()

        val response = generativeModel.generateContent(
            content { text(systemPrompt) }
        )
        return response.text?.trim() ?: "آسف، لم أفهم سؤالك. هل يمكنك إعادة صياغته؟"
    }
}
