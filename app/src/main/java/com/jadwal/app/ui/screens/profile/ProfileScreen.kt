package com.jadwal.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.jadwal.R


// ===== الشاشة الكاملة مع عنوان ورجوع =====
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) viewModel.onPhotoSelected(uri) }

    if (uiState.showEditNameDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditNameDialog,
            title = { Text(stringResource(R.string.edit_name_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = uiState.editNameText,
                    onValueChange = viewModel::onEditNameChange,
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { Button(onClick = viewModel::saveNewName) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissEditNameDialog) { Text(stringResource(R.string.cancel)) } },
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(48.dp))
            }

            ProfileContent(
                uiState = uiState,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onShowEditName = viewModel::showEditNameDialog,
            )
        }
    }
}

// ===== المحتوى القابل للتضمين (بدون شريط العنوان) =====
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onPickPhoto: () -> Unit,
    onShowEditName: () -> Unit,
) {
    Spacer(Modifier.height(16.dp))

    // ===== الصورة الشخصية =====
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier.size(110.dp),
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors = listOf(JadwalIndigo, JadwalViolet)))
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickPhoto,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.profilePhotoPath.isNotBlank()) {
                AsyncImage(
                    model = uiState.profilePhotoPath,
                    contentDescription = stringResource(R.string.profile_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    text = if (uiState.userName.isNotBlank())
                        uiState.userName.first().uppercase() else "؟",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickPhoto,
                ),
        ) {
            Icon(Icons.Rounded.CameraAlt, stringResource(R.string.edit),
                tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }

    Spacer(Modifier.height(12.dp))

    // ===== الاسم =====
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (uiState.userName.isNotBlank()) uiState.userName else stringResource(R.string.student),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onShowEditName, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Edit, stringResource(R.string.edit_name),
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }

    // شارة الـ Streak
    if (uiState.streakDays > 0) {
        Surface(
            color = JadwalWarning.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("🔥", fontSize = 16.sp)
                Text(
                    stringResource(R.string.streak_days_format, uiState.streakDays),
                    style = MaterialTheme.typography.labelLarge,
                    color = JadwalWarning,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // ===== الإحصائيات (3 بطاقات متساوية) =====
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProfileStatCard(
            modifier = Modifier.weight(1f),
            icon = "⏱️",
            value = if (uiState.totalStudyHours >= 1f)
                "%.1f".format(uiState.totalStudyHours)
            else "${uiState.totalStudyMinutes}",
            unit = if (uiState.totalStudyHours >= 1f) stringResource(R.string.hour_short) else stringResource(R.string.minute_short),
            label = stringResource(R.string.total_study),
            color = JadwalIndigo,
        )
        ProfileStatCard(
            modifier = Modifier.weight(1f),
            icon = "📖",
            value = "${uiState.totalSessions}",
            unit = stringResource(R.string.completed_sessions),
            label = stringResource(R.string.completed_sessions),
            color = JadwalViolet,
        )
        ProfileStatCard(
            modifier = Modifier.weight(1f),
            icon = "🔥",
            value = "${uiState.streakDays}",
            unit = stringResource(R.string.day_label),
            label = stringResource(R.string.streak_days_label),
            color = JadwalWarning,
        )
    }

    Spacer(Modifier.height(16.dp))

    // ===== أعلى مادة =====
    if (uiState.topSubjectName.isNotBlank()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            cornerRadius = JadwalRadius.lg,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(JadwalIndigo.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                ) {
                    Text(uiState.topSubjectIcon, fontSize = 26.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.top_subject_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.topSubjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.top_subject_hours_format, uiState.topSubjectMinutes / 60, uiState.topSubjectMinutes % 60),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.EmojiEvents, null, tint = JadwalWarning,
                    modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ===== شارات الإنجاز =====
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.MilitaryTech, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.achievement_badges),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = JadwalRadius.lg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.badges.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { badge ->
                        BadgeItem(badge = badge, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ===== بطاقة إحصاء — ثابتة الارتفاع بغض النظر عن طول النص =====
@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    unit: String,
    label: String,
    color: Color,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.md) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = if (value.length > 4) 18.sp else 22.sp,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun BadgeItem(
    badge: AchievementBadge,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (badge.isUnlocked) 1f else 0.35f,
        label = "badge_alpha",
    )
    Surface(
        modifier = modifier,
        color = if (badge.isUnlocked)
            JadwalIndigo.copy(alpha = 0.10f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(badge.emoji, fontSize = 28.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha))
            Text(
                text = stringResource(id = badge.titleRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(id = badge.descriptionRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            if (badge.isUnlocked) {
                Surface(color = JadwalSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(stringResource(R.string.unlocked_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = JadwalSuccess,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}
