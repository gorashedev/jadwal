package com.jadwal.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun ExamScanScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ExamScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // مشغّل التقاط صورة بالكاميرا (TakePicture → يحفظ في URI مؤقت)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { viewModel.onImagePicked(it, context) }
    }

    // مشغّل اختيار صورة من المعرض
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImagePicked(it, context) }
    }

    // طلب إذن الكاميرا
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraUri = createTempCameraUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            cameraUri = createTempCameraUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // انتقال تلقائي عند نجاح الحفظ
    LaunchedEffect(uiState.step) {
        if (uiState.step == ScanStep.SAVED) {
            // نعرض شاشة النجاح — المستخدم يضغط Done بنفسه
        }
    }

    JadwalBackground {
        when (uiState.step) {
            ScanStep.PICK_IMAGE -> PickImageStep(
                onCamera = ::launchCamera,
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onBack = onBack,
                errorMessage = uiState.errorMessage,
                onDismissError = viewModel::clearError,
            )

            ScanStep.ANALYZING -> AnalyzingStep(imageBitmap = uiState.imageBitmap)

            ScanStep.REVIEW -> ReviewStep(
                imageBitmap = uiState.imageBitmap,
                exams = uiState.extractedExams,
                isSaving = uiState.isSaving,
                onToggle = viewModel::toggleExamSelection,
                onRemove = viewModel::removeExam,
                onSave = viewModel::saveExams,
                onRetry = viewModel::retryWithNewImage,
                errorMessage = uiState.errorMessage,
                onDismissError = viewModel::clearError,
            )

            ScanStep.SAVED -> SavedStep(
                count = uiState.analyzedCount,
                onDone = onDone,
                onScanAnother = viewModel::retryWithNewImage,
            )
        }
    }
}

// ===== خطوة 1: اختيار مصدر الصورة =====

@Composable
private fun PickImageStep(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // هيدر
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.scan_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.scan_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(48.dp))

        // رسم توضيحي
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(JadwalIndigo.copy(alpha = 0.15f), JadwalViolet.copy(alpha = 0.08f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text("✨",
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.End))
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(R.string.scan_tip_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(48.dp))

        // زر الكاميرا
        Button(
            onClick = onCamera,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.capture_camera),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        // زر المعرض
        OutlinedButton(
            onClick = onGallery,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp, JadwalIndigo.copy(alpha = 0.5f)
            ),
        ) {
            Icon(Icons.Rounded.PhotoLibrary, null,
                modifier = Modifier.size(20.dp),
                tint = JadwalIndigo)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.pick_gallery),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = JadwalIndigo)
        }

        Spacer(Modifier.height(24.dp))

        // نصيحة
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Lightbulb, null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.scan_quality_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        }
    }
}

// ===== خطوة 2: جاري التحليل =====

@Composable
private fun AnalyzingStep(imageBitmap: Bitmap?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        imageBitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // طبقة شفافة مع أيقونة تحليل
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.analyzing),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        } ?: run {
            CircularProgressIndicator(color = JadwalIndigo, modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text(stringResource(R.string.ai_analyzing_image),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.may_take_seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ===== خطوة 3: مراجعة النتائج =====

@Composable
private fun ReviewStep(
    imageBitmap: Bitmap?,
    exams: List<ExtractedExam>,
    isSaving: Boolean,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
) {
    val selectedCount = exams.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // هيدر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onRetry) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.extraction_results),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.exams_extracted, exams.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.rescan))
            }
        }

        if (exams.isEmpty()) {
            // لم يُستخرج أي امتحان
            Column(
                modifier = Modifier.weight(1f).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("😕", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_exams_found),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.ensure_clear_image),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_new_photo))
                }
            }
        } else {
            // قائمة الامتحانات المستخرجة
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // معاينة الصورة
                imageBitmap?.let { bmp ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(0.4f))
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null,
                                    tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(stringResource(R.string.analyzed_by_gemini),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                items(exams, key = { it.id }) { exam ->
                    ExtractedExamCard(
                        exam = exam,
                        onToggle = { onToggle(exam.id) },
                        onRemove = { onRemove(exam.id) },
                    )
                }
            }

            if (errorMessage != null) {
                ErrorBanner(
                    message = errorMessage,
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // شريط الحفظ السفلي
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                cornerRadius = JadwalRadius.xl,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.selected_to_save),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.selected_count_exams, selectedCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JadwalIndigo)
                    }
                    Button(
                        onClick = onSave,
                        enabled = selectedCount > 0 && !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.save_to_calendar),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedExamCard(
    exam: ExtractedExam,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val borderColor = if (exam.isSelected) JadwalIndigo.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (exam.isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(JadwalRadius.lg),
            ),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Checkbox
            Checkbox(
                checked = exam.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = JadwalIndigo),
            )

            // أيقونة المادة
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (exam.matchedSubjectId != null)
                            JadwalIndigo.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(exam.matchedSubjectIcon, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(exam.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)

                    if (exam.matchedSubjectId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF66BB6A).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(stringResource(R.string.duplicate_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (exam.date.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.CalendarToday, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(formatDate(exam.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (exam.time.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.Schedule, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Text(exam.time,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (exam.location.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                        Text(exam.location,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // زر الحذف
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Rounded.Close, stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ===== خطوة 4: نجاح الحفظ =====

@Composable
private fun SavedStep(
    count: Int,
    onDone: () -> Unit,
    onScanAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF4CAF50).copy(0.2f), Color(0xFF4CAF50).copy(0.05f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎉", fontSize = 52.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.saved_success),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.height(8.dp))

        Text(stringResource(R.string.saved_count_exams, count),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.view_schedule),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onScanAnother,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scan_another),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium)
        }
    }
}

// ===== مكوّنات مشتركة =====

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Warning, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp))
            Text(message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// ===== مساعدات =====

private fun formatDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val display = SimpleDateFormat("d MMM yyyy", Locale("ar"))
        display.format(sdf.parse(dateStr) ?: Date())
    } catch (_: Exception) {
        dateStr
    }
}

private fun createTempCameraUri(context: android.content.Context): Uri {
    val file = File(context.externalCacheDir ?: context.cacheDir,
        "exam_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
