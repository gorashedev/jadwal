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
                    text = { Text("إضافة مادة") },
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
                        Icon(Icons.Rounded.ArrowBack, "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "إدارة المواد",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "${uiState.subjects.size} مادة دراسية",
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف ${subject.name}؟") },
            text = { Text("هل أنت متأكد من حذف هذه المادة؟ لا يمكن التراجع عن هذه العملية.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("حذف", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
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
            // أيقونة المادة مع لون الخلفية
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
                    subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subject.nameEn.isNotBlank()) {
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
                    Icon(Icons.Rounded.Edit, "تعديل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, "حذف",
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
        Difficulty.EASY   -> "سهل" to Color(0xFF66BB6A)
        Difficulty.MEDIUM -> "متوسط" to Color(0xFFFFA726)
        Difficulty.HARD   -> "صعب" to Color(0xFFEF5350)
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
            "$completed/$total فصل",
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
            "لا توجد مواد دراسية بعد",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "أضف موادك الدراسية لتبدأ في تنظيم جدولك",
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
            Text("إضافة مادة الآن")
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
    errorMessage: String?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (formState.isEditing) "تعديل المادة" else "إضافة مادة جديدة",
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
                    label = { Text("اسم المادة *") },
                    placeholder = { Text("مثال: الرياضيات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null && formState.name.isBlank(),
                )

                // اسم المادة (إنجليزي)
                OutlinedTextField(
                    value = formState.nameEn,
                    onValueChange = onNameEnChange,
                    label = { Text("الاسم بالإنجليزية (اختياري)") },
                    placeholder = { Text("مثال: Mathematics") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // عدد الفصول
                OutlinedTextField(
                    value = formState.totalChapters,
                    onValueChange = onChaptersChange,
                    label = { Text("عدد الفصول") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // الصعوبة
                Text("مستوى الصعوبة",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { diff ->
                        val (label, color) = when (diff) {
                            Difficulty.EASY   -> "سهل" to Color(0xFF66BB6A)
                            Difficulty.MEDIUM -> "متوسط" to Color(0xFFFFA726)
                            Difficulty.HARD   -> "صعب" to Color(0xFFEF5350)
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
                Text("الأيقونة",
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
                Text("لون المادة",
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
                Text(if (formState.isEditing) "حفظ التعديلات" else "إضافة المادة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
