package com.jadwal.ui.screens.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground

// أداة تغيير لغة التطبيق
object LanguageManager {
    fun setAppLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showDevInfo by remember { mutableStateOf(false) }

    // معرفة اللغة الحالية للتطبيق
    val currentLang = context.resources.configuration.locales[0].language
    var isArabic by remember { mutableStateOf(currentLang == "ar") }

    if (showDevInfo) {
        AlertDialog(
            onDismissRequest = { showDevInfo = false },
            title = { Text("حول المطور", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الاسم: قرشي محمداحمد", style = MaterialTheme.typography.bodyLarge)
                    Text("Name: Gorashe Mohamedahmed", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Text("البريد الإلكتروني:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("gorashe.suliman@outlook.com", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevInfo = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوضع الداكن", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isDarkTheme, onCheckedChange = onThemeChange)
                }
            }

            // زر تبديل اللغة
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                onClick = {
                    isArabic = !isArabic
                    val newLang = if (isArabic) "ar" else "en"
                    LanguageManager.setAppLocale(context, newLang)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("لغة التطبيق", style = MaterialTheme.typography.bodyLarge)
                    Text(if (isArabic) "العربية" else "English", color = MaterialTheme.colorScheme.primary)
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                onClick = { showDevInfo = true }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("حول المطور", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("تعرّف على مطور التطبيق وتواصل معه", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("عن التطبيق", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("جدول - إصدار 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}