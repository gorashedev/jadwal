package com.jadwal.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.JadwalRadius
import com.jadwal.ui.theme.JadwalSuccess
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onEmailSent: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }

    LaunchedEffect(uiState.emailSent) {
        if (uiState.emailSent) {
            // انتظر ثانيتين ثم ارجع
            kotlinx.coroutines.delay(2000)
            onEmailSent()
        }
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }

            Spacer(Modifier.height(40.dp))

            // أيقونة القفل
            Icon(
                imageVector = Icons.Rounded.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.forgot_password_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(R.string.forgot_password_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            // ===== تم الإرسال =====
            AnimatedVisibility(
                visible = uiState.emailSent,
                enter = scaleIn() + fadeIn(),
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = JadwalRadius.lg,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.MarkEmailRead,
                            contentDescription = null,
                            tint = JadwalSuccess,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            stringResource(R.string.reset_sent_ok),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JadwalSuccess,
                        )
                        Text(
                            stringResource(R.string.reset_sent_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== نموذج الإدخال =====
            AnimatedVisibility(visible = !uiState.emailSent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = JadwalRadius.xl,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(R.string.email)) },
                            leadingIcon = { Icon(Icons.Rounded.Email, null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.sendPasswordReset(email)
                            }),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // رسالة الخطأ
                        AnimatedVisibility(visible = uiState.error != null) {
                            uiState.error?.let { err ->
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.sendPasswordReset(email)
                            },
                            enabled = !uiState.isLoading && email.isNotBlank(),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp,
                                )
                            } else {
                                Text(stringResource(R.string.send_reset_link_action), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
