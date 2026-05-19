package com.jadwal.ui.screens.subjects

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.Subject
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    onBack: () -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // عرض رسالة الخطأ
    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearError()
        }
    }

    // حوار إضافة / تعديل مادة
    if (uiState.showAddDialog) {
        SubjectDialog(
            formState = uiState.formState,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveSubject,
            onNameChange = viewModel::updateName,
            onNameEnChange = viewModel::updateNameEn,
            onDifficultyChange = viewModel::updateDifficulty,
            onColorChange = viewModel::updateColor,
            onIconChange = viewModel::updateIcon,
            onChaptersChange = viewModel::updateTotalChapters,
            onExamDateChange = viewModel::updateExamDate,
            errorMessage = uiState.errorMessage,
        )
    }

    JadwalBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = viewModel::showAddDialog,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text(stringResource(R.string.add_subject_button)) },
                    containerColor = JadwalIndigo,
                    contentColor = Color.White,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding),
            ) {
                // ===== الهيدر =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            stringResource(R.string.manage_subjects_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            stringResource(R.string.subjects_count, uiState.subjects.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = JadwalIndigo)
                        }
                    }

                    uiState.subjects.isEmpty() -> {
                        EmptySubjectsState(onAdd = viewModel::showAddDialog)
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                bottom = 120.dp, top = 4.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.subjects, key = { it.id }) { subject ->
                                SubjectCard(
                                    subject = subject,
                                    onEdit = { viewModel.showEditDialog(subject) },
                                    onDelete = { viewModel.deleteSubject(subject) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // فحص اللغة الحالية
    val currentLang = java.util.Locale.getDefault().language
    val displayTitle = if (currentLang == "en" && subject.nameEn.isNotBlank()) subject.nameEn else subject.name

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_subject_title, displayTitle)) },
            text = { Text(stringResource(R.string.delete_subject_confirm)) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val subjectColor = runCatching {
                Color(android.graphics.Color.parseColor(subject.colorHex))
            }.getOrDefault(JadwalIndigo)

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(subjectColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(subject.icon, style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayTitle, // استخدام الاسم الصحيح هنا
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // إذا كنا في النسخة العربية والمادة لها اسم إنجليزي نظهره كاسم ثانوي
                if (currentLang != "en" && subject.nameEn.isNotBlank()) {
                    Text(
                        subject.nameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyChip(subject.difficulty)
                    ProgressChip(subject.completedChapters, subject.totalChapters)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Edit, stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: Difficulty) {
    val (label, color) = when (difficulty) {
        Difficulty.EASY   -> stringResource(R.string.difficulty_easy) to Color(0xFF66BB6A)
        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium) to Color(0xFFFFA726)
        Difficulty.HARD   -> stringResource(R.string.difficulty_hard) to Color(0xFFEF5350)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressChip(completed: Int, total: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            stringResource(R.string.chapters_done_format, completed, total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySubjectsState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📚", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.no_subjects_yet),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.add_your_first_subject),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_subject_now))
        }
    }
}

// ===== حوار إضافة / تعديل المادة =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDialog(
    formState: SubjectFormState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEnChange: (String) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onChaptersChange: (String) -> Unit,
    onExamDateChange: (Long?) -> Unit,
    errorMessage: String?,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.examDate
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onExamDateChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (formState.isEditing) stringResource(R.string.subject_dialog_title_edit) else stringResource(R.string.subject_dialog_title_add),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // اسم المادة (عربي)
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.subject_name_label)) },
                    placeholder = { Text(stringResource(R.string.subject_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null && formState.name.isBlank(),
                )

                // اسم المادة (إنجليزي)
                OutlinedTextField(
                    value = formState.nameEn,
                    onValueChange = onNameEnChange,
                    label = { Text(stringResource(R.string.subject_name_en_label)) },
                    placeholder = { Text(stringResource(R.string.subject_name_en_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // عدد الفصول
                OutlinedTextField(
                    value = formState.totalChapters,
                    onValueChange = onChaptersChange,
                    label = { Text(stringResource(R.string.chapters_count_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // الصعوبة
                Text(stringResource(R.string.difficulty_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { diff ->
                        val (label, color) = when (diff) {
                            Difficulty.EASY   -> stringResource(R.string.difficulty_easy) to Color(0xFF66BB6A)
                            Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium) to Color(0xFFFFA726)
                            Difficulty.HARD   -> stringResource(R.string.difficulty_hard) to Color(0xFFEF5350)
                        }
                        FilterChip(
                            selected = formState.difficulty == diff,
                            onClick = { onDifficultyChange(diff) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.20f),
                                selectedLabelColor = color,
                            ),
                        )
                    }
                }

                // اختيار الأيقونة
                Text(stringResource(R.string.icon_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SUBJECT_ICONS) { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (formState.icon == icon)
                                        JadwalIndigo.copy(alpha = 0.20f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (formState.icon == icon) 2.dp else 0.dp,
                                    color = if (formState.icon == icon) JadwalIndigo else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onIconChange(icon) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(icon, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // اختيار اللون
                Text(stringResource(R.string.color_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SUBJECT_COLOR_PALETTE) { hex ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrDefault(JadwalIndigo)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (formState.colorHex == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (formState.colorHex == hex) 0.6f else 0f
                                    ),
                                    shape = CircleShape,
                                )
                                .clickable { onColorChange(hex) },
                        )
                    }
                }

                // موعد الامتحان
                Text(stringResource(R.string.exam_date_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (formState.examDate != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.timeInMillis = formState.examDate
                            "${cal.get(java.util.Calendar.YEAR)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
                        } else stringResource(R.string.choose_exam_date)
                    )
                    if (formState.examDate != null) {
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.remove_date_desc),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onExamDateChange(null) },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // رسالة الخطأ
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
            ) {
                Text(if (formState.isEditing) stringResource(R.string.save_changes) else stringResource(R.string.add_subject_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
