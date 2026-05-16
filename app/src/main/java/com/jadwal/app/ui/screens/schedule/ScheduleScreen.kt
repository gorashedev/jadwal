package com.jadwal.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground

data class WeeklyTask(val day: String, val subject: String, val time: String)

@Composable
fun ScheduleScreen() {
    val weeklyTasks = listOf(
        WeeklyTask("الأحد", "الرياضيات والفيزياء", "3 ساعات"),
        WeeklyTask("الإثنين", "الإحصاء واللغة العربية", " ساعتان"),
        WeeklyTask("الثلاثاء", "الفيزياء والكيمياء", "3 ساعات"),
        WeeklyTask("الأربعاء", "الرياضيات والإنجليزي", "ساعتان"),
        WeeklyTask("الخميس", "مراجعة عامة واختبارات", "4 ساعات")
    )

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "جدولك الأسبوعي",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weeklyTasks) { task ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = task.day, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(text = task.subject, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(text = task.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}