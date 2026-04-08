package com.nexalarm.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.NexAlarmTheme
import com.nexalarm.app.util.AlarmScheduler
import com.nexalarm.app.util.AppSettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        // AppSettingsProvider 已在 NexAlarmApp.onCreate() 初始化
        // 無需再次載入設定，直接使用已同步的全域狀態

        setContent {
            NexAlarmTheme {
                NexAlarmMainContent(
                    onFirstAlarmCreated = ::guideAlarmAccessAfterFirstAlarm
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    fun guideAlarmAccessAfterFirstAlarm() {
        requestNotificationPermission()

        val settings = SettingsManager(this)

        // 精確鬧鐘權限改為首個鬧鐘建立後才導引，避免首頁直接打斷流程。
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2
        ) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms() && !settings.hasRequestedExactAlarmPerm) {
                settings.hasRequestedExactAlarmPerm = true
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
            }
        }

        // 不將電池白名單設為預設流程，僅在第一次建立鬧鐘後提示使用者自行評估是否需要。
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName) &&
            !settings.hasShownBatteryOptDialog
        ) {
            settings.hasShownBatteryOptDialog = true
            Toast.makeText(
                this,
                "若裝置省電機制造成漏響，再到設定將 NexAlarm 設為不受限制。",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "nexalarm") return

        val action = uri.host ?: uri.path?.trimStart('/') ?: return

        val db = NexAlarmDatabase.getDatabase(this)
        val alarmRepo = AlarmRepository(db.alarmDao())
        val folderRepo = FolderRepository(db.folderDao())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    "add" -> handleUriAdd(uri, alarmRepo, folderRepo)
                    "delete" -> handleUriDelete(uri, alarmRepo)
                    "toggle_folder" -> handleUriToggleFolder(uri, folderRepo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleUriAdd(uri: Uri, alarmRepo: AlarmRepository, folderRepo: FolderRepository) {
        val timeStr = uri.getQueryParameter("time") ?: return
        val hour = timeStr.take(2).toIntOrNull() ?: return
        val minute = timeStr.drop(2).toIntOrNull() ?: return
        val title = uri.getQueryParameter("title") ?: ""
        val folderName = uri.getQueryParameter("folder")
        val repeatStr = uri.getQueryParameter("repeat")
        val silent = uri.getQueryParameter("silent")?.toBooleanStrictOrNull() ?: false

        val folderId = if (folderName != null) {
            val folder = folderRepo.findByName(folderName)
            folder?.id ?: folderRepo.insert(FolderEntity(name = folderName))
        } else null

        val repeatDays = repeatStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        val isRecurring = repeatDays.isNotEmpty()

        val alarm = AlarmEntity(
            hour = hour, minute = minute, title = title, isEnabled = true,
            isRecurring = isRecurring, repeatDays = repeatDays,
            folderId = folderId, vibrateOnly = silent
        )
        val alarmId = alarmRepo.insertOrUpdate(alarm)
        
        // 排程鬧鐘（讓 AlarmScheduler 產生 NexAlarmTest logcat 事件供測試腳本監聽）
        val scheduledAlarm = alarm.copy(id = alarmId)
        AlarmScheduler.schedule(this, scheduledAlarm)

        runOnUiThread {
            Toast.makeText(this, "Alarm added: $title ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handleUriDelete(uri: Uri, alarmRepo: AlarmRepository) {
        val id = uri.getQueryParameter("id")?.toLongOrNull() ?: return
        // 先從 DB 取得鬧鐘資料，才能正確取消 AlarmManager 排程
        val alarm = alarmRepo.getAlarmById(id)
        alarmRepo.deleteById(id)
        if (alarm != null) {
            AlarmScheduler.cancel(this, alarm)
        }
        runOnUiThread { Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show() }
    }

    private suspend fun handleUriToggleFolder(uri: Uri, folderRepo: FolderRepository) {
        val folderName = uri.getQueryParameter("name") ?: return
        val folder = folderRepo.findByName(folderName) ?: return
        folderRepo.setEnabled(folder.id, !folder.isEnabled)
        runOnUiThread { Toast.makeText(this, "Folder '${folder.name}' toggled", Toast.LENGTH_SHORT).show() }
    }
}
