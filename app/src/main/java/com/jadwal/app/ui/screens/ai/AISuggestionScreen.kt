package com.jadwal.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.AISuggestion
import com.jadwal.domain.model.ScheduleSlot
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*

val arabicDayNames = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

@Composable
fun AISuggestionScreen(
    viewModel: AISuggestionViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onScheduleSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // الانتقال بعد الحفظ
    LaunchedEffect(uiState.showSaveSuccess) {
        if (uiState.showSaveSuccess) {
            viewModel.dismissSuccess()
            onScheduleSaved()
        }
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ===== شريط العنوان =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اقتراح الذكاء الاصطناعي",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "مدعوم بـ Gemini 1.5 Flash 🤖",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // زر إعادة التوليد
                AnimatedVisibility(
                    visible = uiState.state !is AISuggestionState.Loading,
                ) {
                    IconButton(onClick = viewModel::generate) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "توليد جديد",
                            tint = JadwalIndigo,
                        )
                    }
                }
            }

            // ===== المحتوى الرئيسي =====
            AnimatedContent(
                targetState = uiState.state,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "ai_state",
            ) { state ->
                when (state) {
                    is AISuggestionState.Loading -> LoadingState()
                    is AISuggestionState.Error   -> ErrorState(
                        message = state.message,
                        onRetry = viewModel::generate,
                    )
                    is AISuggestionState.Success -> SuccessContent(
                        suggestion     = state.suggestion,
                        selectedSlots  = uiState.selectedSlots,
                        isSaving       = uiState.isSaving,
                        onToggleSlot   = viewModel::toggleSlot,
                        onSelectAll    = viewModel::selectAll,
                        onDeselectAll  = viewModel::deselectAll,
                        onSave         = viewModel::saveSelectedSlots,
                    )
                    else -> {}
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// ===== حالة التحميل =====
// ─────────────────────────────────────────────
@Composable
private fun LoadingState() {
    val phrases = listOf(
        "جارٍ تحليل موادّك ومواعيد امتحاناتك... 🔍",
        "أحسب أفضل توزيع للوقت... ⚖️",
        "أُولوية المواد الصعبة أولاً... 📊",
        "أُنهي الجدول المثالي لك... ✨",
    )
    var phraseIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    val pulseAlpha by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // أيقونة AI متحركة
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JadwalIndigo.copy(alpha = pulseAlpha * 0.3f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape,
                )
        ) {
            Text("🤖", fontSize = 56.sp)
        }

        Spacer(Modifier.height(32.dp))

        AnimatedContent(
            targetState = phraseIndex,
            transitionSpec = {
                slideInVertically { it / 2 } + fadeIn() togetherWith
                        slideOutVertically { -it / 2 } + fadeOut()
            },
            label = "phrase",
        ) { index ->
            Text(
                text = phrases[index],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = JadwalIndigo,
        )
    }
}

// ─────────────────────────────────────────────
// ===== حالة الخطأ =====
// ─────────────────────────────────────────────
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("😔", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JadwalIndigo),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("حاول مجدداً", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────
// ===== حالة النجاح =====
// ─────────────────────────────────────────────
@Composable
private fun SuccessContent(
    suggestion: AISuggestion,
    selectedSlots: Set<String>,
    isSaving: Boolean,
    onToggleSlot: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSave: () -> Unit,
) {
    val totalSelected = selectedSlots.size
    val totalSlots    = suggestion.schedule.size

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 4.dp, bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ===== البطاقة التشجيعية =====
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = JadwalRadius.xl,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(JadwalIndigo.copy(alpha = 0.15f), JadwalViolet.copy(alpha = 0.08f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "جدولك المخصص جاهز!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = suggestion.motivationMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // إحصاء سريع
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickStat("📅", "${suggestion.schedule.distinctBy { it.dayOfWeek }.size}", "أيام")
                        QuickStat("📚", "${suggestion.schedule.distinctBy { it.subjectId }.size}", "مواد")
                        QuickStat("⏱️", "${suggestion.totalHours}س", "إجمالي")
                        QuickStat("🍅", "${suggestion.totalPomodoros}", "دورات")
                    }
                }
            }
        }

        // ===== شريط الاختيار =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "اختر ما تريد إضافته ($totalSelected/$totalSlots)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("الكل", color = JadwalIndigo, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onDeselectAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // ===== الجدول مُجمَّع حسب اليوم =====
        val byDay = suggestion.schedule.groupBy { it.dayOfWeek }.entries.sortedBy { it.key }

        items(byDay, key = { it.key }) { (dayIndex, slots) ->
            DayScheduleSection(
                dayName     = arabicDayNames.getOrElse(dayIndex) { "يوم $dayIndex" },
                slots       = slots,
                selectedIds = selectedSlots,
                onToggle    = onToggleSlot,
            )
        }

        // ===== ملاحظة AI =====
        if (suggestion.notes.isNotBlank()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = JadwalRadius.lg,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "ملاحظة Gemini",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = JadwalIndigo,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = suggestion.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }
        }
    }

    // ===== زر الحفظ الثابت =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Button(
            onClick = onSave,
            enabled = totalSelected > 0 && !isSaving,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = JadwalIndigo,
                disabledContainerColor = JadwalIndigo.copy(alpha = 0.4f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            AnimatedContent(
                targetState = isSaving,
                label = "save_btn",
            ) { saving ->
                if (saving) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Text("جارٍ الحفظ...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                        Text(
                            "حفظ $totalSelected جلسة في الجدول",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// ===== قسم يوم كامل =====
// ─────────────────────────────────────────────
@Composable
private fun DayScheduleSection(
    dayName: String,
    slots: List<ScheduleSlot>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // عنوان اليوم
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(JadwalIndigo, CircleShape)
            )
            Text(
                text = dayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // بطاقات الجلسات
        slots.forEach { slot ->
            ScheduleSlotCard(
                slot = slot,
                isSelected = slot.id in selectedIds,
                onToggle = { onToggle(slot.id) },
            )
        }
    }
}

// ─────────────────────────────────────────────
// ===== بطاقة جلسة واحدة =====
// ─────────────────────────────────────────────
@Composable
private fun ScheduleSlotCard(
    slot: ScheduleSlot,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(slot.colorHex))
    } catch (e: Exception) { JadwalIndigo }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) subjectColor else Color.Transparent,
        label = "border",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.18f else 0.08f,
        label = "bg_alpha",
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(JadwalRadius.md),
            ),
        cornerRadius = JadwalRadius.md,
        glassAlpha = bgAlpha,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // شارة اليوم + أيقونة
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = subjectColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    )
            ) {
                Text(slot.subjectIcon, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // الوقت
                    SlotChip(
                        text = String.format("%02d:%02d", slot.startHour, slot.startMinute),
                        icon = "🕐",
                    )
                    // المدة
                    SlotChip(
                        text = "${slot.durationMinutes} د",
                        icon = "⏱️",
                    )
                    // الأولوية
                    if (slot.priority > 0) {
                        SlotChip(
                            text = "أولوية ${slot.priority}",
                            icon = "🎯",
                            color = when (slot.priority) {
                                1 -> JadwalError
                                2 -> JadwalWarning
                                else -> JadwalSuccess
                            },
                        )
                    }
                }
            }

            // Checkbox الاختيار
            AnimatedContent(
                targetState = isSelected,
                label = "check",
                transitionSpec = {
                    scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn() togetherWith
                            scaleOut() + fadeOut()
                },
            ) { selected ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (selected) subjectColor else Color.Transparent,
                            shape = CircleShape,
                        )
                        .border(
                            width = 2.dp,
                            color = if (selected) subjectColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape,
                        )
                ) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "مُختار",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// ===== مساعدات =====
// ─────────────────────────────────────────────
@Composable
private fun SlotChip(text: String, icon: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(icon, fontSize = 10.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun QuickStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
