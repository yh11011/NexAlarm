package com.nexalarm.app.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.R
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AppSettingsProvider
import kotlinx.coroutines.launch
import java.util.TimeZone

@Composable
fun SettingsScreen() {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var selectedTimezoneId by remember { mutableStateOf(settingsManager.timeZoneId) }
    var showAiDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                text = S.settings,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language setting
        SettingCard(
            title = S.language,
            options = listOf("中文", "English"),
            selectedIndex = if (isAppEnglish) 1 else 0,
            onSelect = { index ->
                isAppEnglish = index == 1
                settingsManager.isEnglish = index == 1
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Theme style picker
        ThemeStyleCard(settingsManager = settingsManager)

        Spacer(modifier = Modifier.height(12.dp))

        // Timezone setting
        TimezoneCard(
            currentTimezoneId = selectedTimezoneId,
            onClick = { showTimezoneDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Integration
        AiIntegrationCard(onClick = { showAiDialog = true })
    }

    if (showAiDialog) {
        AiModelPickerDialog(
            authToken = settingsManager.authToken,
            onDismiss = { showAiDialog = false },
            onOpenUrl = { url ->
                showAiDialog = false
                CustomTabsIntent.Builder()
                    .setShowTitle(false)
                    .build()
                    .launchUrl(context, Uri.parse(url))
            }
        )
    }

    if (showTimezoneDialog) {
        TimezonePickerDialog(
            currentTimezoneId = selectedTimezoneId,
            onSelect = { tzId ->
                selectedTimezoneId = tzId
                settingsManager.timeZoneId = tzId
                showTimezoneDialog = false
            },
            onReset = {
                selectedTimezoneId = null
                settingsManager.timeZoneId = null
                showTimezoneDialog = false
            },
            onDismiss = { showTimezoneDialog = false }
        )
    }
}

@Composable
private fun TimezoneCard(
    currentTimezoneId: String?,
    onClick: () -> Unit
) {
    val displayText = if (currentTimezoneId == null) {
        S.timezoneSystem
    } else {
        formatTimezoneDisplay(currentTimezoneId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(S.timezone, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TimezonePickerDialog(
    currentTimezoneId: String?,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allTimezones = remember {
        TimeZone.getAvailableIDs()
            .map { it }
            .sortedWith(compareBy(
                { TimeZone.getTimeZone(it).rawOffset },
                { it }
            ))
    }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allTimezones
        else allTimezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(S.timezoneSelect, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(S.timezoneSearch, color = TextSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Reset to system option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(S.timezoneReset, color = PrimaryBlue, fontSize = 14.sp)
                    if (currentTimezoneId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered) { tzId ->
                        val isSelected = tzId == currentTimezoneId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelect(tzId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tzId,
                                    fontSize = 14.sp,
                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                )
                                Text(
                                    text = formatUtcOffset(tzId),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(S.cancel, color = TextSecondary)
            }
        }
    )
}

private fun formatUtcOffset(tzId: String): String {
    val tz = TimeZone.getTimeZone(tzId)
    val offsetMs = tz.rawOffset
    val sign = if (offsetMs >= 0) "+" else "-"
    val absMs = Math.abs(offsetMs)
    val hours = absMs / 3_600_000
    val minutes = (absMs % 3_600_000) / 60_000
    return "UTC${sign}%02d:%02d".format(hours, minutes)
}

private fun formatTimezoneDisplay(tzId: String): String {
    return "${formatUtcOffset(tzId)} $tzId"
}

// ── Theme Style Picker ───────────────────────────────────────

@Composable
private fun ThemeStyleCard(settingsManager: SettingsManager) {
    val currentTheme by AppSettingsProvider.currentThemeMutableState
    val allThemes = AppTheme.entries

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(S.themeStyle, fontSize = 14.sp, color = TextSecondary)
            Text(
                text = if (isAppEnglish) currentTheme.displayNameEn else currentTheme.displayNameZh,
                fontSize = 13.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.heightIn(max = 480.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allThemes) { theme ->
                val isSelected = theme == currentTheme
                val colors = theme.colors()
                val swatches = theme.swatchColors()
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else DarkCard
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) PrimaryBlue else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            AppSettingsProvider.setTheme(theme)
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 縮圖：四色色塊
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.background)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.background))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.card))
                        }
                        // 主色條
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(colors.primary)
                        )
                        // 選取勾
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 7.sp, color = Color.White)
                            }
                        }
                    }
                    Text(
                        text = if (isAppEnglish) theme.displayNameEn else theme.displayNameZh,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PrimaryBlue else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// ── AI Integration ──────────────────────────────────────────

private data class AiModel(
    val id: String,
    val name: String,
    @DrawableRes val logoRes: Int,
)

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

@Composable
private fun AiIntegrationCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(S.aiIntegration, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(S.aiIntegrationDesc, fontSize = 13.sp, color = TextSecondary)
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AiModelPickerDialog(
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
            Text(S.aiSelectModel, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCard)
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
                        onOpenUrl("https://login.nex11.me/ai-setup")
                    },
                    enabled = openingModelId == null,
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                ) {
                    Text(S.aiOpenLoginPage, color = TextSecondary)
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

@Composable
private fun SettingCard(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) PrimaryBlue else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}
