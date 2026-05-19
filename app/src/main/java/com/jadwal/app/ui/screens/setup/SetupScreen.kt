package com.jadwal.ui.screens.setup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.StudyTime
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onSetupComplete()
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ===== شريط التقدم =====
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.step_format, currentStep + 1, totalSteps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = JadwalIndigo,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "setup_step",
            ) { step ->
                when (step) {
                    0 -> SubjectsStep(
                        subjects = uiState.subjects,
                        availableIcons = viewModel.availableIcons,
                        onAddSubject = viewModel::addSubject,
                        onUpdateName = viewModel::updateSubjectName,
                        onUpdateIcon = viewModel::updateSubjectIcon,
                        onRemoveSubject = viewModel::removeSubject,
                        onNext = { currentStep++ },
                    )
                    1 -> ExamDatesStep(
                        subjects = uiState.subjects,
                        onSetExamDate = viewModel::setExamDate,
                        onNext = { currentStep++ },
                    )
                    2 -> StudyHoursStep(
                        selectedHours = uiState.dailyHours,
                        onSelectHours = viewModel::setDailyHours,
                        onNext = { currentStep++ },
                    )
                    3 -> DifficultyAndTimeStep(
                        subjects = uiState.subjects,
                        selectedTime = uiState.preferredTime,
                        onSetDifficulty = viewModel::setSubjectDifficulty,
                        onSelectTime = viewModel::setPreferredTime,
                        isGenerating = uiState.isGenerating,
                        error = uiState.error,
                        onFinish = viewModel::generateSchedule,
                    )
                }
            }
        }
    }
}

// ===== الخطوة 1: إضافة المواد (تبدأ فارغة) =====
@Composable
fun SubjectsStep(
    subjects: List<SubjectDraft>,
    availableIcons: List<String>,
    onAddSubject: () -> Unit,
    onUpdateName: (String, String) -> Unit,
    onUpdateIcon: (String, String) -> Unit,
    onRemoveSubject: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_subjects_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.setup_subjects_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        // قائمة المواد
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ===== حالة فارغة =====
            if (subjects.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = JadwalRadius.lg,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("📚", fontSize = 48.sp)
                            Text(
                                text = stringResource(R.string.no_subjects_yet),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.add_your_first_subject),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            items(subjects, key = { it.id }) { subject ->
                SubjectInputCard(
                    subject = subject,
                    availableIcons = availableIcons,
                    onUpdateName = { onUpdateName(subject.id, it) },
                    onUpdateIcon = { onUpdateIcon(subject.id, it) },
                    onRemove = { onRemoveSubject(subject.id) },
                )
            }

            // زر إضافة مادة
            item {
                OutlinedButton(
                    onClick = onAddSubject,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_subject))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = subjects.any { it.name.isNotBlank() },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.next), fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SubjectInputCard(
    subject: SubjectDraft,
    availableIcons: List<String>,
    onUpdateName: (String) -> Unit,
    onUpdateIcon: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var showIconPicker by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = JadwalRadius.md,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // أيقونة المادة (قابلة للتغيير)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { showIconPicker = !showIconPicker }
                ) {
                    Text(subject.icon, fontSize = 24.sp)
                }

                // حقل اسم المادة
                OutlinedTextField(
                    value = subject.name,
                    onValueChange = onUpdateName,
                    placeholder = { Text(stringResource(R.string.subject_name_placeholder)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )

                // زر الحذف
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // منتقي الأيقونات
            AnimatedVisibility(visible = showIconPicker) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableIcons) { icon ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (icon == subject.icon)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onUpdateIcon(icon)
                                    showIconPicker = false
                                }
                        ) {
                            Text(icon, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ===== الخطوة 2: تواريخ الامتحانات =====
@Composable
fun ExamDatesStep(
    subjects: List<SubjectDraft>,
    onSetExamDate: (String, Long) -> Unit,
    onNext: () -> Unit,
) {
    val validSubjects = subjects.filter { it.name.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            stringResource(R.string.setup_exams_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.setup_exams_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(validSubjects, key = { it.id }) { subject ->
                ExamDateCard(
                    subject = subject,
                    onSetDate = { onSetExamDate(subject.id, it) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.next), fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ExamDateCard(
    subject: SubjectDraft,
    onSetDate: (Long) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = JadwalRadius.md,
        onClick = { showDatePicker = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(subject.icon, fontSize = 24.sp)
                Column {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subject.examDateMillis?.let {
                            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(it))
                        } ?: stringResource(R.string.not_set_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (subject.examDateMillis != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSetDate(it) }
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
}

// ===== الخطوة 3: ساعات الدراسة =====
@Composable
fun StudyHoursStep(
    selectedHours: Int,
    onSelectHours: (Int) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                stringResource(R.string.setup_hours_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.setup_hours_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(1, 2, 3, 4).forEach { hours ->
                    val isSelected = selectedHours == hours
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f),
                        cornerRadius = JadwalRadius.lg,
                        onClick = { onSelectHours(hours) },
                        glassAlpha = if (isSelected) 0.5f else 0.1f,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "$hours",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (hours == 1) stringResource(R.string.hour_singular) else stringResource(R.string.hours),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Column {
            Button(
                onClick = onNext,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.next), fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ===== الخطوة 4: الصعوبة + وقت الدراسة =====
@Composable
fun DifficultyAndTimeStep(
    subjects: List<SubjectDraft>,
    selectedTime: StudyTime,
    onSetDifficulty: (String, Difficulty) -> Unit,
    onSelectTime: (StudyTime) -> Unit,
    isGenerating: Boolean,
    error: String?,
    onFinish: () -> Unit,
) {
    val validSubjects = subjects.filter { it.name.isNotBlank() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.setup_difficulty_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(R.string.setup_difficulty_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        items(validSubjects, key = { it.id }) { subject ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = JadwalRadius.md,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject.icon, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Difficulty.EASY to stringResource(R.string.easy),
                            Difficulty.MEDIUM to stringResource(R.string.medium),
                            Difficulty.HARD to stringResource(R.string.hard),
                        ).forEach { (difficulty, label) ->
                            val isSelected = subject.difficulty == difficulty
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetDifficulty(subject.id, difficulty) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.setup_time_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    StudyTime.MORNING to "🌅" to stringResource(R.string.morning),
                    StudyTime.EVENING to "🌆" to stringResource(R.string.evening),
                    StudyTime.NIGHT to "🌙" to stringResource(R.string.night),
                ).forEach { (timePair, label) ->
                    val (time, icon) = timePair
                    val isSelected = selectedTime == time
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = JadwalRadius.md,
                        onClick = { onSelectTime(time) },
                        glassAlpha = if (isSelected) 0.5f else 0.1f,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(icon, fontSize = 28.sp)
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // رسالة خطأ
        if (error != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        item {
            Button(
                onClick = onFinish,
                enabled = !isGenerating,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.generate_smart_schedule), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
