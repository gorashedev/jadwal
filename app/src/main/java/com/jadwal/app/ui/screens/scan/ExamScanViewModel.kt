package com.jadwal.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.app.data.ai.GeminiService
import com.jadwal.data.repository.ExamRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// ===== حالة مادة مستخرَجة من الصورة =====
data class ExtractedExam(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String,
    val date: String,           // "YYYY-MM-DD"
    val time: String,           // "HH:MM"
    val location: String,
    val notes: String,
    val matchedSubjectId: String? = null,
    val matchedSubjectIcon: String = "📋",
    val isSelected: Boolean = true,
)

enum class ScanStep { PICK_IMAGE, ANALYZING, REVIEW, SAVED }

data class ExamScanUiState(
    val step: ScanStep = ScanStep.PICK_IMAGE,
    val imageBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val extractedExams: List<ExtractedExam> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val analyzedCount: Int = 0,
)

@HiltViewModel
class ExamScanViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamScanUiState())
    val uiState = _uiState.asStateFlow()

    // ===== استقبال الصورة من المعرض أو الكاميرا =====

    fun onImagePicked(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(context, uri) ?: return@launch
                _uiState.update { it.copy(imageBitmap = bitmap, step = ScanStep.ANALYZING) }
                analyzeImage(bitmap)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل تحميل الصورة: ${e.message}") }
            }
        }
    }

    fun onCameraBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(imageBitmap = bitmap, step = ScanStep.ANALYZING) }
            analyzeImage(bitmap)
        }
    }

    // ===== استدعاء Gemini Vision لاستخراج بيانات الامتحانات =====

    private suspend fun analyzeImage(bitmap: Bitmap) {
        _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

        val prompt = """
            أنت محلل جدول امتحانات. افحص هذه الصورة التي تحتوي على جدول امتحانات دراسية.
            استخرج جميع الامتحانات وأعد JSON فقط بهذا الشكل الدقيق (بدون نص آخر):
            {
              "exams": [
                {
                  "subject_name": "اسم المادة",
                  "date": "YYYY-MM-DD",
                  "time": "HH:MM",
                  "location": "موقع الامتحان أو قاعة إن وُجدت، وإلا فراغ",
                  "notes": "أي ملاحظات إضافية"
                }
              ]
            }
            إذا كان تاريخ الامتحان بدون سنة، افترض السنة الحالية أو التالية بما يجعل منطقياً.
            إذا كان الوقت غير موجود استخدم "09:00".
            إذا لم تجد أي امتحانات في الصورة أعد: {"exams": []}
        """.trimIndent()

        try {
            val rawText = withContext(Dispatchers.IO) {
                geminiService.generateFromBitmap(bitmap, prompt)
            }.ifBlank { "{\"exams\":[]}" }
            val exams = parseGeminiResponse(rawText)
            val enriched = matchSubjects(exams)

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    extractedExams = enriched,
                    step = ScanStep.REVIEW,
                    analyzedCount = enriched.size,
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    step = ScanStep.PICK_IMAGE,
                    errorMessage = "فشل تحليل الصورة. تأكد من وضوح الصورة واتصال الإنترنت.",
                )
            }
        }
    }

    // ===== تحليل استجابة Gemini JSON =====

    private fun parseGeminiResponse(raw: String): List<ExtractedExam> {
        return try {
            // استخراج JSON من النص (أحياناً Gemini يضيف نصاً قبله أو بعده)
            val jsonStart = raw.indexOf('{')
            val jsonEnd = raw.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1) return emptyList()

            val jsonStr = raw.substring(jsonStart, jsonEnd + 1)
            val root = JSONObject(jsonStr)
            val examsArray = root.optJSONArray("exams") ?: return emptyList()

            (0 until examsArray.length()).map { i ->
                val obj = examsArray.getJSONObject(i)
                ExtractedExam(
                    subjectName = obj.optString("subject_name", "مادة غير معروفة").trim(),
                    date = obj.optString("date", "").trim(),
                    time = obj.optString("time", "09:00").trim(),
                    location = obj.optString("location", "").trim(),
                    notes = obj.optString("notes", "").trim(),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ===== مطابقة الأسماء المستخرجة مع المواد الموجودة في قاعدة البيانات =====

    private suspend fun matchSubjects(exams: List<ExtractedExam>): List<ExtractedExam> {
        val subjects = try {
            subjectRepository.getAllSubjects().first()
        } catch (_: Exception) {
            return exams
        }

        return exams.map { exam ->
            val matched = subjects.firstOrNull { subject ->
                subject.name.contains(exam.subjectName, ignoreCase = true) ||
                        exam.subjectName.contains(subject.name, ignoreCase = true) ||
                        subject.nameEn.contains(exam.subjectName, ignoreCase = true) ||
                        exam.subjectName.contains(subject.nameEn, ignoreCase = true)
            }
            exam.copy(
                matchedSubjectId = matched?.id,
                matchedSubjectIcon = matched?.icon ?: "📋",
            )
        }
    }

    // ===== تعديل عناصر القائمة =====

    fun toggleExamSelection(examId: String) {
        _uiState.update { state ->
            state.copy(
                extractedExams = state.extractedExams.map { exam ->
                    if (exam.id == examId) exam.copy(isSelected = !exam.isSelected)
                    else exam
                }
            )
        }
    }

    fun removeExam(examId: String) {
        _uiState.update { state ->
            state.copy(extractedExams = state.extractedExams.filter { it.id != examId })
        }
    }

    fun retryWithNewImage() {
        _uiState.update { ExamScanUiState() }
    }

    // ===== حفظ الامتحانات المحددة في قاعدة البيانات =====

    fun saveExams() {
        val selected = _uiState.value.extractedExams.filter { it.isSelected }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "لم تحدد أي امتحانات للحفظ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            var savedCount = 0

            selected.forEach { extracted ->
                try {
                    val timestampMs = dateTimeToTimestamp(extracted.date, extracted.time)
                    val subjectId = extracted.matchedSubjectId ?: "unknown_${UUID.randomUUID()}"

                    examRepository.insertExam(
                        Exam(
                            id = UUID.randomUUID().toString(),
                            subjectId = subjectId,
                            subjectName = extracted.subjectName,
                            examDate = timestampMs,
                            location = extracted.location,
                            notes = extracted.notes,
                        )
                    )
                    savedCount++
                } catch (_: Exception) { /* تخطّ الامتحانات التي فشل حفظها */ }
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    step = ScanStep.SAVED,
                    analyzedCount = savedCount,
                )
            }
        }
    }

    // ===== مساعدات =====

    private fun dateTimeToTimestamp(date: String, time: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            sdf.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    @Suppress("DEPRECATION")
    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                    decoder.setTargetSampleSize(2)
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
