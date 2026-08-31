package com.nexalarm.app.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.model.RepeatDaysConverter
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.data.repository.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Deep Link 處理器
 *
 * 負責處理所有 nexalarm:// URI scheme 的 deep link 操作：
 * - 新增鬧鐘：nexalarm://add?time=0730&title=WakeUp&folder=Study&repeat=1,2,3,4,5&silent=true
 * - 刪除鬧鐘：nexalarm://delete?id=1
 * - 切換資料夾：nexalarm://toggle_folder?name=Study
 */
class DeepLinkHandler(private val context: Context) {

    private val database = NexAlarmDatabase.getDatabase(context)
    private val alarmRepo = AlarmRepository(database.alarmDao())
    private val folderRepo = FolderRepository(database.folderDao())

    /**
     * 處理 Deep Link URI
     */
    suspend fun handleDeepLink(uri: Uri): String? {
        if (uri.scheme != "nexalarm") return null

        val action = uri.host ?: uri.path?.trimStart('/') ?: return null

        return try {
            when (action) {
                "add" -> handleUriAdd(uri)
                "delete" -> handleUriDelete(uri)
                "toggle_folder" -> handleUriToggleFolder(uri)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 處理新增鬧鐘 URI
     */
    private suspend fun handleUriAdd(uri: Uri): String {
        val timeStr = uri.getQueryParameter("time") ?: return "Invalid time parameter"
        if (!Regex("""\d{4}""").matches(timeStr)) return "Invalid time parameter"
        val hour = timeStr.take(2).toIntOrNull() ?: return "Invalid hour"
        val minute = timeStr.drop(2).toIntOrNull() ?: return "Invalid minute"
        if (hour !in 0..23) return "Invalid hour"
        if (minute !in 0..59) return "Invalid minute"
        val title = uri.getQueryParameter("title") ?: ""
        val folderName = uri.getQueryParameter("folder")
        val repeatStr = uri.getQueryParameter("repeat")
        val silent = uri.getQueryParameter("silent")?.toBooleanStrictOrNull() ?: false

        val folderId = if (folderName != null) {
            val folder = folderRepo.findByName(folderName)
            if (folder != null) {
                folder.id
            } else {
                val folderCount = folderRepo.getUserFolderCount()
                if (!FeatureFlags.canCreateFolder(folderCount)) return "Folder limit reached"
                folderRepo.insert(FolderEntity(name = folderName))
            }
        } else null

        val repeatDays = repeatStr
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.distinct()
            ?: emptyList()
        if (repeatDays.any { it !in 1..7 }) return "Invalid repeat days"
        val isRecurring = repeatDays.isNotEmpty()

        val existing = database.alarmDao().findDuplicate(
            hour = hour,
            minute = minute,
            title = title,
            folderId = folderId,
            repeatDays = RepeatDaysConverter().fromList(repeatDays)
        )
        if (existing == null && !FeatureFlags.canCreateAlarm(database.alarmDao().getTotalAlarmCount())) {
            return "Alarm limit reached"
        }

        val alarm = AlarmEntity(
            hour = hour, minute = minute, title = title, isEnabled = true,
            isRecurring = isRecurring, repeatDays = repeatDays,
            folderId = folderId, vibrateOnly = silent
        )
        val alarmId = alarmRepo.insertOrUpdate(alarm)

        // 排程鬧鐘
        val scheduledAlarm = alarm.copy(id = alarmId)
        AlarmScheduler.schedule(context, scheduledAlarm)

        val message = "Alarm added: $title ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}"
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        return message
    }

    /**
     * 處理刪除鬧鐘 URI
     */
    private suspend fun handleUriDelete(uri: Uri): String {
        val id = uri.getQueryParameter("id")?.toLongOrNull() ?: return "Invalid alarm ID"
        // 先從 DB 取得鬧鐘資料，才能正確取消 AlarmManager 排程
        val alarm = alarmRepo.getAlarmById(id) ?: return "Alarm not found"
        alarmRepo.deleteById(id)
        AlarmScheduler.cancel(context, alarm)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Alarm deleted", Toast.LENGTH_SHORT).show()
        }
        return "Alarm deleted"
    }

    /**
     * 處理切換資料夾 URI
     */
    private suspend fun handleUriToggleFolder(uri: Uri): String {
        val folderName = uri.getQueryParameter("name") ?: return "Invalid folder name"
        val folder = folderRepo.findByName(folderName) ?: return "Folder not found"
        folderRepo.setEnabled(folder.id, !folder.isEnabled)
        val message = "Folder '${folder.name}' toggled"
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        return message
    }
}
