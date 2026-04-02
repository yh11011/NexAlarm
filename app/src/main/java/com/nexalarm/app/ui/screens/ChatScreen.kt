package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AiRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen() {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authToken = settingsManager.authToken

    // 檢查是否已綁定 AI 服務
    var aiBound by remember { mutableStateOf(false) }
    var checkingStatus by remember { mutableStateOf(true) }

    LaunchedEffect(authToken) {
        if (authToken == null) {
            checkingStatus = false
            return@LaunchedEffect
        }

        scope.launch {
            val result = AiRepository.getAiKeyStatus(authToken)
            result.onSuccess { response ->
                val bindings = response.optJSONArray("bindings")
                aiBound = bindings != null && bindings.length() > 0
            }
            checkingStatus = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = openMenu,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = S.menu,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = S.chatWithAi,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main content
        if (checkingStatus) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (!aiBound) {
            // AI 未綁定
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = S.aiNotBound,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        } else if (authToken == null) {
            // 未登入
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = S.aiLoginRequired,
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        } else {
            // Chat UI
            // 訊息列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    ChatMessageBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 錯誤訊息
            if (errorMessage != null) {
                Text(
                    text = "Error: ${errorMessage}",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // 輸入區
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text(S.typeMessage, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = DarkCard,
                        focusedContainerColor = DarkCard,
                        unfocusedTextColor = TextPrimary,
                        focusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue,
                        unfocusedPlaceholderColor = TextSecondary,
                        focusedPlaceholderColor = TextSecondary
                    )
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMsg = inputText.trim()
                            inputText = ""
                            messages = messages + ChatMessage(userMsg, isUser = true)

                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val result = AiRepository.chatWithAi(userMsg, authToken!!)
                                isLoading = false

                                result.onSuccess { response ->
                                    val reply = response.optString("reply", "已執行")
                                    messages = messages + ChatMessage(reply, isUser = false)
                                }
                                result.onFailure { ex ->
                                    errorMessage = ex.message
                                }
                            }
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank(),
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = AccentDim
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = S.send,
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isUser) PrimaryBlue else DarkCard,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .widthIn(maxWidth = 280.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
