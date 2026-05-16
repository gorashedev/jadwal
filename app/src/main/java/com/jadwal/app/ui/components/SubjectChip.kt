package com.jadwal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.JadwalRadius
import com.jadwal.ui.theme.NeutralWhite

/**
 * SubjectChip — شريحة المادة الدراسية
 *
 * تُستخدم في قائمة تصفية المواد أو عرض المادة المحددة
 *
 * الاستخدام:
 * ```
 * SubjectChip(
 *     name = "الرياضيات",
 *     color = SubjectMath,
 *     icon = "📐",
 *     isSelected = true,
 *     onClick = { }
 * )
 * ```
 */
@Composable
fun SubjectChip(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: String = "",
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(0.5f),
        animationSpec = spring(),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.7f) else Color.White.copy(0.4f),
        animationSpec = spring(),
        label = "chip_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) color else Color(0xFF616161),
        animationSpec = spring(),
        label = "chip_text"
    )

    val shape = RoundedCornerShape(JadwalRadius.full)

    Row(
        modifier = modifier
            .clip(shape)
            .background(color = bgColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // نقطة الألوان أو الإيموجي
        if (icon.isNotEmpty()) {
            Text(text = icon, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(6.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/**
 * SubjectColorDot — نقطة لون المادة فقط (للاستخدام في القوائم)
 */
@Composable
fun SubjectColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 12
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color = color, shape = CircleShape)
    )
}

/**
 * SubjectBadge — شارة المادة بالاسم والأيقونة
 */
@Composable
fun SubjectBadge(
    name: String,
    color: Color,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(JadwalRadius.sm)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon.isNotEmpty()) {
            Text(text = icon, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
