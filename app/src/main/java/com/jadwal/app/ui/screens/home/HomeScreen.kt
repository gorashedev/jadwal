package com.jadwal.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground

@Composable
fun HomeScreen(
    onStartSession: (String) -> Unit,
    onViewSchedule: () -> Unit,
    onViewReport: () -> Unit
) {
    val scrollState = rememberScrollState()

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 100.dp) // مسافة للـ Bottom Bar
        ) {
            // Header
            HomeHeader(
                greeting = "صباح النور يا محمد!",
                streakDays = 12,
                modifier = Modifier.padding(24.dp)
            )

            // Daily Progress
            DailyProgressCard(
                completedMinutes = 60,
                totalMinutes = 180,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "📌 مهام اليوم",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Task List (محاكاة لبيانات)
            TaskCard(
                subjectName = "الرياضيات",
                subjectIcon = "📐",
                color = Color(0xFF5C6BC0),
                duration = "60 دقيقة",
                isCompleted = false,
                onStartSession = { onStartSession("1") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            TaskCard(
                subjectName = "الفيزياء",
                subjectIcon = "⚡",
                color = Color(0xFF7E57C2),
                duration = "45 دقيقة",
                isCompleted = true,
                onStartSession = { onStartSession("2") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeHeader(greeting: String, streakDays: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = greeting, style = MaterialTheme.typography.headlineMedium)
            Text(text = "جاهز تنجز مهامك اليوم؟", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GlassCard(cornerRadius = 16.dp, glassAlpha = 0.3f) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 20.sp)
                Spacer(Modifier.width(4.dp))
                Text("$streakDays", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun DailyProgressCard(completedMinutes: Int, totalMinutes: Int, modifier: Modifier = Modifier) {
    val progress = completedMinutes.toFloat() / totalMinutes.toFloat()

    GlassCard(modifier = modifier, glassAlpha = 0.2f) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إنجاز اليوم", style = MaterialTheme.typography.titleMedium)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "$completedMinutes من $totalMinutes دقيقة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TaskCard(
    subjectName: String,
    subjectIcon: String,
    color: Color,
    duration: String,
    isCompleted: Boolean,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, glassAlpha = if (isCompleted) 0.05f else 0.2f) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subjectIcon, fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = subjectName, style = MaterialTheme.typography.titleMedium)
                Text(text = duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCompleted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF82), modifier = Modifier.size(32.dp))
            } else {
                IconButton(
                    onClick = onStartSession,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start", tint = Color.White)
                }
            }
        }
    }
}