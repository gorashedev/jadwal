package com.jadwal.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // شعار التطبيق والترحيب
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📅", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "مرحباً بك في جدول",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "مساعدك الدراسي الذكي الذي يتكيف مع روتينك اليومي ويضمن لك أفضل تنظيم بدون تشتت.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ميزة زجاجية سريعة
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✨ ذكاء اصطناعي مرن", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "إذا فاتتك جلسة اليوم، سيعيد التطبيق توزيع المهام تلقائياً دون إشعارك بالإحباط.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // زر البداية
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("ابدأ رحلتك الآن", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}