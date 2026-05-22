package com.jadwal.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.jadwal.R
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: AIChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    // Auto-scroll to the newest message.
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.lastIndex)
            }
        }
    }

    // Show a Toast whenever the ViewModel emits one, then clear it.
    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (uiState.showApiKeySetup) {
        AlertDialog(
            onDismissRequest = { if (uiState.hasApiKey) viewModel.hideApiKeySetup() },
            icon = { Icon(Icons.Rounded.Key, null, tint = JadwalIndigo) },
            title = { Text(stringResource(R.string.gemini_api_key_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.gemini_api_key_message))
                    OutlinedTextField(
                        value = uiState.apiKeyInput,
                        onValueChange = viewModel::onApiKeyInputChange,
                        label = { Text(stringResource(R.string.gemini_api_key_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveApiKey,
                    enabled = uiState.apiKeyInput.isNotBlank(),
                ) { Text(stringResource(R.string.gemini_api_key_save)) }
            },
            dismissButton = {
                if (uiState.hasApiKey) {
                    TextButton(onClick = viewModel::hideApiKeySetup) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }

    // حوار تأكيد مسح المحادثة
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.clear_chat), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.clear_chat_confirm)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearHistory(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.clear), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // Height the floating pill occupies at the bottom of the screen:
    //   system nav-bar inset  +  24 dp float gap (matches JadwalApp)  +  pill content height.
    // Pill content = vertical padding 10 dp * 2 + icon 22 dp + label ~14 dp + spacedBy 2 dp ≈ 58 dp.
    // Total ≈ navBarInset + 24 + 58 = navBarInset + 82 dp. Use 80 dp as a round number.
    val navBarInset       = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarHeight = navBarInset + 80.dp

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 1. imePadding() is outermost: it handles the soft-keyboard inset
                //    BEFORE fixed paddings are applied, so opening the keyboard
                //    simply shrinks the available height rather than stacking gaps.
                .imePadding()
                // 2. Status bar inset so the header doesn't go under the notch.
                .statusBarsPadding()
                // 3. Bottom padding reserves space for the floating pill so the
                //    input field is never hidden underneath it.
                .padding(bottom = floatingBarHeight),
        ) {
            ChatHeader(
                hasApiKey = uiState.hasApiKey,
                onApiKeyClick = viewModel::showApiKeySetup,
                onClearHistory = { showClearDialog = true },
            )

            // weight(1f) — fills all remaining vertical space, naturally
            // pushing ChatInputBar to the bottom of the Column.
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    ) {
                        ChatBubble(message = message)
                    }
                }
                if (uiState.isTyping) {
                    item { TypingIndicator() }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            ChatInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                isTyping = uiState.isTyping,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    hasApiKey: Boolean,
    onApiKeyClick: () -> Unit,
    onClearHistory: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        cornerRadius = JadwalRadius.xl,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(JadwalViolet, JadwalIndigo)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ai_assistant),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.powered_by_gemini),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val statusColor = if (hasApiKey) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                stringResource(if (hasApiKey) R.string.connected else R.string.not_connected),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium,
            )
            IconButton(
                onClick = onApiKeyClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Rounded.Key,
                    contentDescription = stringResource(R.string.gemini_api_key_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = stringResource(R.string.clear_chat),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val isDark = LocalAppDarkTheme.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(JadwalViolet.copy(alpha = 0.8f), JadwalIndigo)
                        )
                    )
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null,
                    tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        val bubbleColor = if (isUser) {
            if (isDark) JadwalIndigo.copy(alpha = 0.7f) else JadwalIndigo.copy(alpha = 0.85f)
        } else {
            if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.92f)
        }

        val textColor = if (isUser) Color.White
        else MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }

        if (isUser) Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(JadwalIndigo.copy(alpha = 0.3f))
                .align(Alignment.Bottom),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null,
                tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                    bottomEnd = 18.dp, bottomStart = 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    stringResource(R.string.ask_anything),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JadwalIndigo.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
        )

        FilledIconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isTyping,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = JadwalIndigo,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = stringResource(R.string.send),
                tint = if (text.isNotBlank() && !isTyping) Color.White
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
