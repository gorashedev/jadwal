package com.jadwal.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jadwal.R
import com.jadwal.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = listOf(
        OnboardingPage(
            emoji = "📚",
            title = stringResource(R.string.onboarding_title_1),
            subtitle = stringResource(R.string.onboarding_subtitle_1),
            gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        ),
        OnboardingPage(
            emoji = "🤖",
            title = stringResource(R.string.onboarding_title_2),
            subtitle = stringResource(R.string.onboarding_subtitle_2),
            gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        ),
        OnboardingPage(
            emoji = "🔔",
            title = stringResource(R.string.onboarding_title_3),
            subtitle = stringResource(R.string.onboarding_subtitle_3),
            gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFFFE66D)),
        ),
        OnboardingPage(
            emoji = "📈",
            title = stringResource(R.string.onboarding_title_4),
            subtitle = stringResource(R.string.onboarding_subtitle_4),
            gradientColors = listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    fun finishOnboarding() {
        viewModel.completeOnboarding()
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dot_width",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) {
                        finishOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF5C6BC0),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = if (isLastPage) stringResource(R.string.get_started)
                           else stringResource(R.string.next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (!isLastPage) {
                TextButton(onClick = { finishOnboarding() }) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emoji_float",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = page.gradientColors)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // الإيموجي العائم
            Text(
                text = page.emoji,
                fontSize = 96.sp,
                modifier = Modifier.offset(y = offsetY.dp),
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
            )
        }
    }
}
