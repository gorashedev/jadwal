package com.jadwal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * JadwalTopBar — شريط العنوان العلوي بتأثير زجاجي
 *
 * الاستخدام:
 * ```
 * JadwalTopBar(
 *     title = "الجدول",
 *     onNavigateBack = { navController.popBackStack() }
 * )
 * ```
 */
@Composable
fun JadwalTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val isDark = isSystemInDarkTheme()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val glassColor = if (isDark) Color.White.copy(0.07f) else Color.White.copy(0.65f)

    // أيقونة الرجوع تتغير حسب الاتجاه
    val backIcon = if (isRtl) Icons.Rounded.ArrowForward else Icons.Rounded.ArrowBack

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(glassColor, Color.Transparent)
                )
            )
            .border(
                width = 0.dp,
                color = Color.Transparent
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // زر الرجوع
            if (onNavigateBack != null) {
                GlassIconButton(
                    icon = backIcon,
                    onClick = onNavigateBack,
                    size = 44.dp
                )
            } else {
                Spacer(Modifier.width(44.dp))
            }

            // العنوان في المنتصف
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // الأيقونات على اليمين/اليسار
            Box(
                modifier = Modifier.width(44.dp),
                contentAlignment = Alignment.Center
            ) {
                actions?.invoke()
            }
        }
    }
}

/**
 * TopBarAction — أيقونة في شريط العنوان
 */
@Composable
fun TopBarAction(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onBackground,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
