package com.jadwal.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.jadwal.R
import com.jadwal.ui.theme.*

/**
 * NotificationPermissionHandler — يجب استدعاؤه من الشاشة الرئيسية (HomeScreen أو MainActivity).
 *
 * يتحقق تلقائياً:
 * 1. هل الجهاز يعمل بـ Android 13+ (API 33)؟ — الإذن مطلوب فقط هناك
 * 2. هل المستخدم رفض الإذن سابقاً؟ — لا يُزعجه مرتين
 * 3. هل الإذن ممنوح بالفعل؟ — لا يُظهر الحوار
 *
 * مثال الاستخدام في HomeScreen:
 *   NotificationPermissionHandler(
 *       onGranted = { /* جدوِل الإشعارات */ },
 *       onDenied = { /* أعلم SettingsViewModel */ }
 *   )
 */
@Composable
fun NotificationPermissionHandler(
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {},
) {
    // الإذن مطلوب فقط في Android 13+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // على الإصدارات الأقدم الإذن ممنوح تلقائياً
        LaunchedEffect(Unit) { onGranted() }
        return
    }

    NotificationPermissionHandlerApi33(
        onGranted = onGranted,
        onDenied = onDenied,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationPermissionHandlerApi33(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var permissionHandled by remember { mutableStateOf(false) }

    // مُطلق طلب الإذن
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionHandled = true
        showDialog = false
        if (isGranted) onGranted() else onDenied()
    }

    // إطلاق الحوار مرة واحدة عند فتح الشاشة
    LaunchedEffect(Unit) {
        if (!permissionHandled) {
            showDialog = true
        }
    }

    if (showDialog) {
        NotificationPermissionDialog(
            onAllow = {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showDialog = false
                permissionHandled = true
                onDenied()
            },
        )
    }
}

/**
 * بطاقة شرح أنيقة تطلب إذن الإشعارات
 */
@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false, // لا يُغلق بالنقر خارجه لضمان قراءة الرسالة
            usePlatformDefaultWidth = false,
        )
    ) {
        // بطاقة زجاجية بتصميم احترافي
        NotificationRationaleCard(
            onAllow = onAllow,
            onSkip = onDismiss,
        )
    }
}

@Composable
fun NotificationRationaleCard(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    // أنيميشن دخول للبطاقة
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // أنيميشن نبض لأيقونة الجرس
    val infiniteTransition = rememberInfiniteTransition(label = "bell_pulse")
    val bellScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bell_scale",
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        ) + fadeIn(tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {

                // ===== أيقونة الجرس المتحركة =====
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    JadwalIndigo.copy(alpha = 0.18f),
                                    JadwalViolet.copy(alpha = 0.08f),
                                    Color.Transparent,
                                )
                            ),
                            shape = CircleShape,
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(JadwalIndigo, JadwalViolet)
                                ),
                                shape = CircleShape,
                            )
                            .scale(bellScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ===== العنوان =====
                Text(
                    text = stringResource(R.string.enable_notifications),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.notif_perm_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                // ===== قائمة المزايا =====
                BenefitRow(
                    emoji = "📚",
                    title = stringResource(R.string.daily_reminder),
                    subtitle = stringResource(R.string.notif_daily_sub),
                )
                Spacer(Modifier.height(12.dp))
                BenefitRow(
                    emoji = "⚠️",
                    title = stringResource(R.string.notif_exam_title),
                    subtitle = stringResource(R.string.notif_exam_sub),
                )
                Spacer(Modifier.height(12.dp))
                BenefitRow(
                    emoji = "🎉",
                    title = stringResource(R.string.notif_achievement_title),
                    subtitle = stringResource(R.string.notif_achievement_sub),
                )

                Spacer(Modifier.height(28.dp))

                // ===== زر السماح =====
                Button(
                    onClick = onAllow,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JadwalIndigo,
                    ),
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.enable_notifications),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ===== زر التخطي =====
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.later),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ===== نص طمأنة =====
                Text(
                    text = stringResource(R.string.notif_change_anytime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(
    emoji: String,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // أيقونة مضمّنة في دائرة
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                )
        ) {
            Text(emoji, fontSize = 22.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
