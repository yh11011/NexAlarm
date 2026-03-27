package com.nexalarm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*
import java.util.TimeZone

@Composable
fun SettingsScreen() {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var selectedTimezoneId by remember { mutableStateOf(settingsManager.timeZoneId) }
    var showAiDialog by remember { mutableStateOf(false) }

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

        // Theme setting
        SettingCard(
            title = S.theme,
            options = listOf(S.darkMode, S.lightMode),
            selectedIndex = if (isDarkTheme) 0 else 1,
            onSelect = { index ->
                isDarkTheme = index == 0
                settingsManager.isDarkMode = index == 0
            }
        )

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
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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

// ── AI Integration ──────────────────────────────────────────

private data class AiModel(val id: String, val name: String, val emoji: String)

private val AI_MODELS = listOf(
    AiModel("claude",    "Claude",        "🤖"),
    AiModel("chatgpt",   "ChatGPT",       "💬"),
    AiModel("gemini",    "Gemini",        "✨"),
    AiModel("copilot",   "Copilot",       "🪟"),
    AiModel("grok",      "Grok",          "𝕏"),
    AiModel("cursor",    "Cursor",        "⌨️"),
    AiModel("perplexity","Perplexity",    "🔍"),
    AiModel("deepseek",  "DeepSeek",      "🐋"),
    AiModel("kimi",      "Kimi",          "🌙"),
    AiModel("doubao",    "豆包",           "🫘"),
    AiModel("qwen",      "通義千問",       "🔮"),
    AiModel("wenxin",    "文心一言",       "🖊️"),
    AiModel("chatglm",   "智譜清言",       "🧠"),
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
    var selectedModel by remember { mutableStateOf<AiModel?>(null) }

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
                        val isSelected = selectedModel?.id == model.id
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else DarkCard
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) PrimaryBlue else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedModel = model }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(model.emoji, fontSize = 24.sp)
                            Text(
                                text = model.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) PrimaryBlue else TextSecondary,
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val model = selectedModel ?: return@Button
                    val token = authToken ?: return@Button
                    val url = "https://login.nex11.me/ai-setup?model=${model.id}&token=${Uri.encode(token)}"
                    onOpenUrl(url)
                },
                enabled = selectedModel != null && authToken != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(S.aiConfirm, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
