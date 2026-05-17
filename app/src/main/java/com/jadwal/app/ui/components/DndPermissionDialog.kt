package com.jadwal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * DndPermissionDialog — إصلاح #4
 *
 * يظهر عندما يحاول المستخدم تفعيل DND لكن الإذن غير ممنوح
 * يشرح له الفائدة ويوجهه لإعدادات النظام
 *
 * الاستخدام في SessionScreen:
 * ```
 * val uiState by viewModel.uiState.collectAsStateWithLifecycle()
 *
 * if (uiState.showDndPermissionDialog) {
 *     DndPermissionDialog(
 *         onGoToSettings = { viewModel.openDndSettings() },
 *         onDismiss = { viewModel.dismissDndDialog() }
 *     )
 * }
 * ```
 */
@Composable
fun DndPermissionDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // أيقونة
                Icon(
                    imageVector = Icons.Rounded.DoNotDisturb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )

                Text(
                    text = "تفعيل وضع التركيز",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "للحصول على أفضل تجربة مذاكرة، يمكن لجدول إيقاف المقاطعات تلقائياً أثناء جلسات البومودورو.\n\nتحتاج منح الإذن من إعدادات الجهاز.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("لاحقاً")
                    }

                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("فتح الإعدادات")
                    }
                }
            }
        }
    }
}
