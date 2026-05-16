package com.jadwal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * AnimatedCounter — عداد رقمي متحرك
 *
 * الأرقام تتحرك للأعلى عند الزيادة وللأسفل عند النقصان
 *
 * الاستخدام:
 * ```
 * AnimatedCounter(
 *     count = hoursStudied,
 *     style = MaterialTheme.typography.displaySmall,
 *     color = JadwalIndigo
 * )
 * ```
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    suffix: String = "",
) {
    var oldCount by remember { mutableIntStateOf(count) }

    SideEffect {
        oldCount = count
    }

    // تحريك كل رقم بشكل مستقل
    val countString = count.toString()

    AnimatedContent(
        targetState = countString,
        transitionSpec = {
            if (count > oldCount) {
                slideInVertically { -it } togetherWith slideOutVertically { it }
            } else {
                slideInVertically { it } togetherWith slideOutVertically { -it }
            }
        },
        modifier = modifier,
        label = "animated_counter"
    ) { targetCount ->
        Text(
            text = targetCount + suffix,
            style = style,
            color = color
        )
    }
}

/**
 * AnimatedPercentage — نسبة مئوية متحركة
 */
@Composable
fun AnimatedPercentage(
    percentage: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    AnimatedCounter(
        count = percentage,
        modifier = modifier,
        style = style,
        color = color,
        suffix = "%"
    )
}
