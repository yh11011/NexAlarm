package com.nexalarm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

data class AiProvider(
    val id: String,
    val label: String,
    val description: String,
    val apiKeyUrl: String,
    val isFree: Boolean
)

val AI_PROVIDERS = listOf(
    AiProvider(
        id = "groq",
        label = "Groq",
        description = "Free + Fast (Open Source Models)",
        apiKeyUrl = "https://console.groq.com/keys",
        isFree = true
    ),
    AiProvider(
        id = "gemini",
        label = "Google Gemini",
        description = "Free tier available",
        apiKeyUrl = "https://aistudio.google.com",
        isFree = true
    ),
    AiProvider(
        id = "openai",
        label = "OpenAI",
        description = "GPT-4o-mini (Paid)",
        apiKeyUrl = "https://platform.openai.com/api-keys",
        isFree = false
    ),
    AiProvider(
        id = "anthropic",
        label = "Anthropic Claude",
        description = "Haiku 4.5 (Paid)",
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        isFree = false
    )
)

@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    var bindings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loadingStatus by remember { mutableStateOf(true) }
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    val authToken = settingsManager.authToken

    // 載入已綁定的 AI
    LaunchedEffect(authToken) {
        if (authToken != null) {
            scope.launch {
                val result = AiRepository.getAiKeyStatus(authToken)
                result.onSuccess { response ->
                    val bindingsArray = response.optJSONArray("bindings")
                    val bindingsMap = mutableMapOf<String, String>()
                    if (bindingsArray != null) {
                        for (i in 0 until bindingsArray.length()) {
                            val item = bindingsArray.getJSONObject(i)
                            bindingsMap[item.optString("provider")] = item.optString("key_preview", "")
                        }
                    }
                    bindings = bindingsMap
                }
                loadingStatus = false
            }
        } else {
            loadingStatus = false
        }
    }

    fun openKeyUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun bindKey() {
        if (selectedProvider == null || apiKeyInput.isBlank() || authToken == null) return

        scope.launch {
            isVerifying = true
            verifyError = null

            val result = AiRepository.bindAiKey(selectedProvider!!, apiKeyInput.trim(), authToken!!)
            isVerifying = false

            result.onSuccess { response ->
                val preview = response.optString("key_preview", "")
                bindings = bindings.toMutableMap().apply {
                    put(selectedProvider!!, preview)
                }
                apiKeyInput = ""
                selectedProvider = null
            }
            result.onFailure { ex ->
                verifyError = ex.message ?: "Failed to bind API key"
            }
        }
    }

    fun unbindKey(provider: String) {
        if (authToken == null) return

        scope.launch {
            val result = AiRepository.unbindAiKey(provider, authToken!!)
            result.onSuccess {
                bindings = bindings.toMutableMap().apply {
                    remove(provider)
                }
            }
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
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = S.back,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = S.aiSettings,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (authToken == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = S.aiLoginRequired, color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // AI Provider Cards
                AI_PROVIDERS.forEach { provider ->
                    val isBound = bindings.containsKey(provider.id)
                    AiProviderCard(
                        provider = provider,
                        isBound = isBound,
                        keyPreview = bindings[provider.id] ?: "",
                        isSelected = selectedProvider == provider.id,
                        onSelect = {
                            selectedProvider = provider.id
                            apiKeyInput = ""
                            verifyError = null
                        },
                        onOpenUrl = { openKeyUrl(provider.apiKeyUrl) },
                        onUnbind = { unbindKey(provider.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Key Input Section (when a provider is selected)
                if (selectedProvider != null && !bindings.containsKey(selectedProvider)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = S.enterApiKey,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            TextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                placeholder = { Text("sk-...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = DarkBackground,
                                    focusedContainerColor = DarkBackground,
                                    unfocusedTextColor = TextPrimary,
                                    focusedTextColor = TextPrimary,
                                    cursorColor = PrimaryBlue
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (verifyError != null) {
                                Text(
                                    text = verifyError!!,
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { bindKey() },
                                enabled = !isVerifying && apiKeyInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    disabledContainerColor = AccentDim
                                )
                            ) {
                                if (isVerifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = TextPrimary,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(S.apiKeyVerifying)
                                } else {
                                    Text(S.verifyAndSave)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AiProviderCard(
    provider: AiProvider,
    isBound: Boolean,
    keyPreview: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenUrl: () -> Unit,
    onUnbind: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isSelected) it.background(color = AccentDim, shape = RoundedCornerShape(12.dp)) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentDim else DarkCard
        ),
        onClick = { if (!isBound) onSelect() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = provider.description,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (provider.isFree) {
                        Text(
                            text = "💰 Free",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (isBound) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Bound",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isBound) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = DarkBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = keyPreview,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onUnbind,
                        modifier = Modifier
                            .height(24.dp)
                            .wrapContentWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = S.unbindAi,
                            fontSize = 10.sp,
                            color = Color.Red
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenUrl,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(S.getApiKey, fontSize = 13.sp)
                }
            }
        }
    }
}
