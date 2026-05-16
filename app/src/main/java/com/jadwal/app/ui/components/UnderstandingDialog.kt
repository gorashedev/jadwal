package com.jadwal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jadwal.R
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.ui.theme.JadwalError
import com.jadwal.ui.theme.JadwalRadius
import com.jadwal.ui.theme.JadwalSuccess
import com.jadwal.ui.theme.JadwalWarning

/**
 * UnderstandingDialog — حوار تقييم مستوى الفهم بعد انتهاء الجلسة
 *
 * الاستخدام:
 * ```
 * UnderstandingDialog(
 *     onRate = { level -> viewModel.rateUnderstanding(level) }
 * )
 * ```
 */
@Composable
fun UnderstandingDialog(
    onRate: (UnderstandingLevel) -> Unit,
) {
    var selectedLevel by remember { mutableStateOf<UnderstandingLevel?>(null) }
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF8F9FF)

    Dialog(
        onDismissRequest = { /* لا يُغلق بالضغط خارجه */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f)),
            exit = scaleOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(JadwalRadius.xl))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(0.25f),
                        shape = RoundedCornerShape(JadwalRadius.xl)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الإيموجي
                Text(
                    text = "🧠",
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(Modifier.height(12.dp))

                // السؤال
                Text(
                    text = stringResource(R.string.how_was_understanding),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(24.dp))

                // خيارات الفهم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UnderstandingOption(
                        emoji = "😕",
                        label = stringResource(R.string.understanding_poor),
                        color = JadwalError,
                        isSelected = selectedLevel == UnderstandingLevel.POOR,
                        onClick = { selectedLevel = UnderstandingLevel.POOR },
                        modifier = Modifier.weight(1f)
                    )

                    UnderstandingOption(
                        emoji = "🤔",
                        label = stringResource(R.string.understanding_medium),
                        color = JadwalWarning,
                        isSelected = selectedLevel == UnderstandingLevel.PARTIAL,
                        onClick = { selectedLevel = UnderstandingLevel.PARTIAL },
                        modifier = Modifier.weight(1f)
                    )

                    UnderstandingOption(
                        emoji = "😊",
                        label = stringResource(R.string.understanding_great),
                        color = JadwalSuccess,
                        isSelected = selectedLevel == UnderstandingLevel.GREAT,
                        onClick = { selectedLevel = UnderstandingLevel.GREAT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // زر التأكيد
                GlassButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        selectedLevel?.let { onRate(it) }
                    },
                    enabled = selectedLevel != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UnderstandingOption(
    emoji: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) color.copy(0.15f) else Color.White.copy(0.5f)
    val borderColor = if (isSelected) color.copy(0.7f) else Color.White.copy(0.4f)
    val shape = RoundedCornerShape(JadwalRadius.md)

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor, shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
