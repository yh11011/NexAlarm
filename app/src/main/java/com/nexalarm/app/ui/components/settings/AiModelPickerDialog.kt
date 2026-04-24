package com.nexalarm.app.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.nexalarm.app.R
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AI 模型資料類
 */
private data class AiModel(
    val id: String,
    val name: String,
    @DrawableRes val logoRes: Int,
)

/**
 * AI 模型清單
 */
private val AI_MODELS = listOf(
    AiModel("claude",     "Claude",     R.drawable.ai_claude),
    AiModel("chatgpt",    "ChatGPT",    R.drawable.ai_chatgpt),
    AiModel("gemini",     "Gemini",     R.drawable.ai_gemini),
    AiModel("copilot",    "Copilot",    R.drawable.ai_copilot),
    AiModel("grok",       "Grok",       R.drawable.ai_grok),
    AiModel("cursor",     "Cursor",     R.drawable.ai_cursor),
    AiModel("perplexity", "Perplexity", R.drawable.ai_perplexity),
    AiModel("deepseek",   "DeepSeek",   R.drawable.ai_deepseek),
    AiModel("kimi",       "Kimi",       R.drawable.ai_kimi),
    AiModel("doubao",     "豆包",        R.drawable.ai_doubao),
    AiModel("qwen",       "通義千問",    R.drawable.ai_qwen),
    AiModel("wenxin",     "文心一言",    R.drawable.ai_wenxin),
    AiModel("chatglm",    "智譜清言",    R.drawable.ai_chatglm),
)

/**
 * AI 模型選擇對話框
 * 選擇並連接 AI 服務
 */
@Composable
fun AiModelPickerDialog(
    authToken: String?,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var openingModelId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(S.aiSelectModel, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        },
        text = {
            Column {
                Text(S.aiSelectHint, color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AI_MODELS) { model ->
                        val isOpening = openingModelId == model.id
                        Column(
                            modifier = Modifier
                                .nexGlassSurface(12.dp)
                                .clickable(enabled = authToken != null && openingModelId == null) {
                                    errorMessage = null
                                    openingModelId = model.id
                                    scope.launch {
                                        val result = AuthRepository.createAiSetupSession(
                                            modelId = model.id,
                                            token = authToken!!
                                        )
                                        openingModelId = null
                                        result
                                            .onSuccess { session -> onOpenUrl(session.launchUrl) }
                                            .onFailure { error ->
                                                errorMessage = error.message ?: S.aiSetupFailed
                                            }
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(model.logoRes),
                                    contentDescription = model.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .then(
                                            if (authToken == null || isOpening)
                                                Modifier.alpha(0.4f)
                                            else Modifier
                                        )
                                )
                                if (isOpening) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                            Text(
                                text = model.name,
                                fontSize = 11.sp,
                                color = if (authToken == null || isOpening) TextSecondary.copy(alpha = 0.4f) else TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
                if (authToken == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = S.aiLoginRequired,
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                } else if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (authToken == null) {
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        // 移除直接開啟外部 URL，改為導引使用者到帳號頁面登入
                        // 為了安全性，避免 token 放在 URL query string 或直接開啟未驗證的頁面
                        errorMessage = "請先登入帳號以使用 AI 整合功能"
                    },
                    enabled = openingModelId == null,
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                ) {
                    Text("請先登入", color = TextSecondary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = openingModelId == null) {
                Text(S.cancel, color = TextSecondary)
            }
        }
    )
}
