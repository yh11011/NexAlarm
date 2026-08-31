package com.nexalarm.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.components.AlarmReliabilityCard
import com.nexalarm.app.ui.components.settings.*
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AlarmReliabilityChecker
import com.nexalarm.app.util.TestAlarmScheduler

@Composable
fun SettingsScreen() {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var selectedTimezoneId by remember { mutableStateOf(settingsManager.timeZoneId) }
    var showAiDialog by remember { mutableStateOf(false) }
    val reliabilityState = AlarmReliabilityChecker.evaluate(context)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        SettingsHeader(openMenu = openMenu)

        Spacer(modifier = Modifier.height(16.dp))

        AlarmReliabilityCard(
            state = reliabilityState,
            showDetails = true,
            onTestAlarm = {
                TestAlarmScheduler.schedule(context)
                Toast.makeText(context, S.testAlarmScheduled, Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        // Meeting Mode
        MeetingModeCard(settingsManager = settingsManager)

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

