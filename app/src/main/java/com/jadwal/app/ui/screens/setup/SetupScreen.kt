package com.jadwal.ui.screens.setup

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jadwal.R
import com.jadwal.ui.components.GlassButton
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.domain.model.Subject
import com.jadwal.domain.model.StudyTime
import com.jadwal.domain.model.Difficulty

@Composable
fun SetupScreen(
    // viewModel: SetupViewModel = hiltViewModel(), // قم بتفعيلها لاحقاً عند برمجة الـ ViewModel
    onSetupComplete: () -> Unit
) {
    // محاكاة لحالة الـ ViewModel مؤقتاً لتصميم الواجهة
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedHours by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf(StudyTime.MORNING) }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // شريط التقدم الزجاجي
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                }, label = "SetupSteps"
            ) { step ->
                when (step) {
                    0 -> SetupTitleStep(
                        title = "كم مادة لديك هذا الفصل؟",
                        subtitle = "سنقوم بإضافتها لجدولك.",
                        onNext = { currentStep++ }
                    )
                    1 -> SetupTitleStep(
                        title = "متى تبدأ امتحاناتك؟",
                        subtitle = "حدد التواريخ لنحسب لك الوقت المتبقي.",
                        onNext = { currentStep++ }
                    )
                    2 -> StudyHoursStep(
                        selectedHours = selectedHours,
                        onSelectHours = { selectedHours = it },
                        onNext = { currentStep++ }
                    )
                    3 -> StudyTimeStep(
                        selectedTime = selectedTime,
                        onSelectTime = { selectedTime = it },
                        onNext = { currentStep++ }
                    )
                    4 -> SetupTitleStep(
                        title = "ما هي أصعب مادة؟",
                        subtitle = "سنعطيها وقتاً إضافياً في الجدول.",
                        onNext = {
                            // viewModel.generateSchedule()
                            onSetupComplete()
                        },
                        isLastStep = true
                    )
                }
            }
        }
    }
}

// === خطوة اختيار ساعات المذاكرة ===
@Composable
fun StudyHoursStep(
    selectedHours: Int,
    onSelectHours: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "كم ساعة تستطيع المذاكرة يومياً؟",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "كن واقعياً، الاستمرارية أهم من الكمية.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // خيارات الساعات (مربعات زجاجية)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(1, 2, 3, 4).forEach { hours ->
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.8f),
                    onClick = { onSelectHours(hours) },
                    glassAlpha = if (selectedHours == hours) 0.5f else 0.15f,
                    borderAlpha = if (selectedHours == hours) 0.8f else 0.3f
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$hours",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (selectedHours == hours) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if(hours == 1) "ساعة" else "ساعات",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = selectedHours > 0,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("التالي")
        }
    }
}

// === خطوة اختيار وقت المذاكرة ===
@Composable
fun StudyTimeStep(
    selectedTime: StudyTime,
    onSelectTime: (StudyTime) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "متى تفضل المذاكرة؟",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeOptionCard(
                title = "الصباح (أفضل تركيز)",
                icon = "🌅",
                isSelected = selectedTime == StudyTime.MORNING,
                onClick = { onSelectTime(StudyTime.MORNING) }
            )
            TimeOptionCard(
                title = "المساء (بعد العصر)",
                icon = "🌇",
                isSelected = selectedTime == StudyTime.EVENING,
                onClick = { onSelectTime(StudyTime.EVENING) }
            )
            TimeOptionCard(
                title = "الليل (هدوء تام)",
                icon = "🌙",
                isSelected = selectedTime == StudyTime.NIGHT,
                onClick = { onSelectTime(StudyTime.NIGHT) }
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("التالي")
        }
    }
}

@Composable
fun TimeOptionCard(title: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        onClick = onClick,
        glassAlpha = if (isSelected) 0.5f else 0.15f
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// خطوة وهمية لتكملة الواجهة (تُستبدل لاحقاً بالمكونات الفعلية)
@Composable
fun SetupTitleStep(title: String, subtitle: String, onNext: () -> Unit, isLastStep: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (isLastStep) "توليد جدولي الذكي ✨" else "التالي")
        }
    }
}
