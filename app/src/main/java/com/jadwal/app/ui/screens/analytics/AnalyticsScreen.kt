package com.jadwal.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground

@Composable
fun AnalyticsScreen() {
    val scrollState = rememberScrollState()

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = "تقرير الأسبوع",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(24.dp)
            )

            // بطاقات الإحصاءات السريعة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMiniCard("11.5", "ساعة", "المذاكرة", Modifier.weight(1f))
                StatMiniCard("12", "يوم", "السلسلة 🔥", Modifier.weight(1f))
                StatMiniCard("82%", "", "الإنجاز", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // رسم بياني مخصص ومرن لتفادي تعقيدات المكتبات الخارجية في النسخة التجريبية
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("معدل المذاكرة اليومي", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(0.4f, 0.8f, 0.6f, 1f, 0.2f, 0.9f, 0.5f).forEach { scale ->
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(scale)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // توقعات الذكاء الاصطناعي للجاهزية
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("توقعات الجاهزية للامتحان", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("الرياضيات: جاهز بنسبة 87%", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4CAF82))
                }
            }
        }
    }
}

@Composable
fun StatMiniCard(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, glassAlpha = 0.2f) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$value $unit", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}