package com.jadwal.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.jadwal.ui.components.JadwalBackground
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(
    scheduleItemId: String,
    onSessionEnd: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(1500) } // 25 دقيقة الافتراضية بومودورو
    var isRunning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            showDialog = true
            isRunning = false
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("كيف كان فهمك؟") },
            text = { Text("تقييمك يساعد الذكاء الاصطناعي على ضبط جدول الأيام القادمة تلقائياً.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    onSessionEnd()
                }) { Text("مكتمل وجاهز") }
            }
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "جلسة التركيز الحالية",
                style = MaterialTheme.typography.headlineSmall
            )

            // المؤقت الدائري الزجاجي
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { timeLeft.toFloat() / 1500f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutes = timeLeft / 60
                    val seconds = timeLeft % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(text = "بومودورو", color = MaterialTheme.colorScheme.primary)
                }
            }

            // أزرار التحكم السفلى
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFEF5350).copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = Color(0xFFEF5350)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}