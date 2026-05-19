package com.jadwal.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jadwal.R
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.navigation.Screen
import com.jadwal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLogout() }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.notificationHour,
        initialMinute = uiState.notificationMinute,
        is24Hour = true,
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNotificationTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLogoutDialog,
            icon = {
                Icon(Icons.Rounded.Logout, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(stringResource(R.string.logout_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                Button(
                    onClick = viewModel::logout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.logout_action),
                    color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 100.dp),
        ) {
            // ===== العنوان =====
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
            )

            // ===== المظهر =====
            SettingsSectionTitle(stringResource(R.string.appearance), Icons.Rounded.Palette)

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = JadwalRadius.lg,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSubtitle(
                        stringResource(R.string.theme_mode),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                    Column(modifier = Modifier.selectableGroup()) {
                        listOf(
                            "LIGHT"  to stringResource(R.string.theme_light),
                            "DARK"   to stringResource(R.string.theme_dark),
                            "SYSTEM" to stringResource(R.string.theme_system),
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(selected = uiState.themeMode == mode, onClick = null)
                                Text(text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // ===== اللغة =====
                    SettingsSubtitle(
                        stringResource(R.string.language),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                    Column(modifier = Modifier.selectableGroup()) {
                        listOf(
                            "ar" to stringResource(R.string.lang_arabic),
                            "en" to stringResource(R.string.lang_english),
                            ""   to stringResource(R.string.lang_system),
                        ).forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = uiState.languageCode == code,
                                        onClick = {
                                            viewModel.setLanguage(code)

                                            // --- الكود السحري لإعادة تشغيل التطبيق فوراً ---
                                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                            // ---------------------------------------------
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(selected = uiState.languageCode == code, onClick = null)
                                Text(text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Info, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(R.string.lang_restart_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== الإشعارات =====
            SettingsSectionTitle(stringResource(R.string.notifications), Icons.Rounded.Notifications)

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = JadwalRadius.lg,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(R.string.enable_notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.daily_reminder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled,
                        )
                    }

                    if (uiState.notificationsEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(stringResource(R.string.reminder_time),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = String.format("%02d:%02d",
                                        uiState.notificationHour, uiState.notificationMinute),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            FilledTonalButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.edit))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== عن التطبيق =====
            SettingsSectionTitle(stringResource(R.string.about), Icons.Rounded.Info)

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = JadwalRadius.lg,
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsInfoRow(stringResource(R.string.version), "1.0.0 (MVP)")
                    HorizontalDivider()
                    SettingsInfoRow(stringResource(R.string.ai_engine), "Google Gemini 1.5 Flash")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== عن المطور =====
            SettingsSectionTitle(stringResource(R.string.about_developer), Icons.Rounded.Code)

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = JadwalRadius.lg,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Person, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Column {
                            Text(stringResource(R.string.developer_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Gorashe Mohamed | قرشي محمد",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Surface(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:gorashe.suliman@outlook.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Jadwal - Contact")
                            }
                            context.startActivity(Intent.createChooser(intent, "Send email"))
                        },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Rounded.Email, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.email_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("gorashe.suliman@outlook.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Rounded.ArrowForwardIos, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:+201010736525")
                                }
                            )
                        },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Rounded.Phone, null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.phone_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+20 1010736525",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Rounded.ArrowForwardIos, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ===== تسجيل الخروج =====
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = JadwalRadius.lg,
            ) {
                Surface(
                    onClick = viewModel::showLogoutDialog,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(JadwalRadius.lg),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (uiState.isLoggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.error,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.Logout, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp))
                        }
                        Text(
                            text = if (uiState.isLoggingOut)
                                stringResource(R.string.logging_out)
                            else
                                stringResource(R.string.logout_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun SettingsSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
