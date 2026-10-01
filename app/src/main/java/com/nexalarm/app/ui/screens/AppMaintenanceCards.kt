package com.nexalarm.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexalarm.app.BuildConfig
import com.nexalarm.app.data.AiDeviceRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.SyncDiagnostics
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.update.AppUpdateViewModel
import com.nexalarm.app.update.UpdatePolicy
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SyncStatusCard() {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val prefs = remember { context.getSharedPreferences("sync_diagnostics", android.content.Context.MODE_PRIVATE) }
    var phase by remember { mutableStateOf(prefs.getString("phase", "idle") ?: "idle") }
    var success by remember { mutableLongStateOf(prefs.getLong("success_at", 0)) }
    LaunchedEffect(Unit) {
        while (true) {
            phase = prefs.getString("phase", "idle") ?: "idle"
            success = prefs.getLong("success_at", 0)
            delay(1000)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(DarkSurface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(S.syncStatus, color = TextPrimary)
        Text(settings.authUsername ?: settings.authDisplayName ?: S.aiLoginRequired, color = TextSecondary)
        Text(S.syncPhase(phase), color = TextSecondary)
        Text(S.lastSync + ": " + if (success == 0L) S.notConfirmed else DateFormat.getDateTimeInstance().format(Date(success)), color = TextSecondary)
        TextButton(enabled = settings.authToken != null && phase !in setOf("queued", "syncing", "registered"), onClick = {
            SyncDiagnostics.save(context, "queued")
            AiDeviceRepository.refreshPushToken(context)
            AiDeviceRepository.enqueue(context)
        }) { Text(S.syncNow) }
    }
}

@Composable
internal fun AppUpdateCard(updates: AppUpdateViewModel = viewModel()) {
    val context = LocalContext.current
    val state by updates.state.collectAsState()
    var notesVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(DarkSurface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(S.appUpdates, color = TextPrimary)
        Text("${S.currentVersion}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = TextSecondary)
        Text(S.updatePhase(state.phase), color = TextSecondary)
        state.release?.let { Text("${S.latestVersion}: ${it.versionName}", color = TextSecondary) }
        if (state.phase == "downloading") {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text("${(state.progress * 100).toInt()}%", color = TextSecondary)
        }
        Row {
            TextButton(enabled = state.phase !in setOf("checking", "downloading"), onClick = { updates.check() }) { Text(S.checkUpdates) }
            if (state.phase in setOf("available", "download_failed")) {
                TextButton(onClick = { updates.download(context) }) { Text(S.downloadUpdate) }
            }
            if (state.phase in setOf("ready", "permission_required", "install_failed")) {
                TextButton(onClick = { updates.install(context) }) { Text(S.installUpdate) }
            }
        }
        Text(UpdatePolicy.SOURCE, color = TextSecondary)
        TextButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdatePolicy.SOURCE))) }
        }) { Text(S.downloadSource) }
        if (!state.release?.notes.isNullOrBlank()) {
            TextButton(onClick = { notesVisible = !notesVisible }) { Text(S.releaseNotes) }
            if (notesVisible) Text(state.release!!.notes, color = TextSecondary)
        }
    }
}
