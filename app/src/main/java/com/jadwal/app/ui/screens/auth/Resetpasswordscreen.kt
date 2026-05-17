package com.jadwal.ui.screens.auth

import android.net.Uri
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.R
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*

/**
 * ResetPasswordScreen — شاشة إعادة تعيين كلمة المرور
 *
 * إصلاح #3: هذه الشاشة تظهر عندما يضغط المستخدم على رابط
 * إعادة تعيين كلمة المرور في البريد الإلكتروني.
 *
 * التدفق:
 * 1. المستخدم يضغط الرابط في البريد
 * 2. يُعاد توجيهه لـ com.jadwal.app://reset-password#token...
 * 3. التطبيق يستورد الجلسة من الـ token
 * 4. يظهر هذه الشاشة لإدخال كلمة المرور الجديدة
 */
@Composable
fun ResetPasswordScreen(
    encodedFragment: String,
    viewModel: AuthViewModel = hiltViewModel(),
    onPasswordUpdated: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // ─── استيراد جلسة Supabase من الـ Deep Link fragment ───
    LaunchedEffect(encodedFragment) {
        val fragment = Uri.decode(encodedFragment)
        if (fragment.isNotBlank()) {
            viewModel.importSessionFromDeepLink(fragment)
        }
    }

    // ─── التنقل بعد نجاح التحديث ───
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onPasswordUpdated()
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

            // ─── زر الرجوع ───
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ─── الأيقونة ───
            Icon(
                imageVector = Icons.Rounded.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.reset_password_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.reset_password_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            // ─── النموذج ───
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
                    // كلمة المرور الجديدة
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            localError = null
                            viewModel.clearError()
                        },
                        label = { Text(stringResource(R.string.new_password)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Rounded.Visibility
                                    else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = localError != null || uiState.error != null,
                    )

                    // تأكيد كلمة المرور
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localError = null
                            viewModel.clearError()
                        },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Rounded.Visibility
                                    else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                submitPasswordReset(
                                    newPassword, confirmPassword,
                                    onError = { localError = it },
                                    onSubmit = { viewModel.updatePassword(newPassword) }
                                )
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = localError != null || uiState.error != null,
                    )

                    // رسالة الخطأ
                    val errorText = localError ?: uiState.error
                    AnimatedVisibility(visible = errorText != null) {
                        errorText?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── زر التأكيد ───
            Button(
                onClick = {
                    focusManager.clearFocus()
                    submitPasswordReset(
                        newPassword, confirmPassword,
                        onError = { localError = it },
                        onSubmit = { viewModel.updatePassword(newPassword) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update_password),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun submitPasswordReset(
    newPassword: String,
    confirmPassword: String,
    onError: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    when {
        newPassword.length < 6 -> onError("كلمة المرور يجب أن تكون 6 أحرف على الأقل")
        newPassword != confirmPassword -> onError("كلمتا المرور غير متطابقتان")
        else -> onSubmit()
    }
}