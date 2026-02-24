package com.nexalarm.app.service

import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MeetingModeTileService : TileService() {

    companion object {
        private const val PREFS_NAME = "meeting_mode_prefs"
        private const val KEY_ACTIVE = "meeting_mode_active"
        private const val KEY_ORIGINAL_PREFIX = "original_vibrate_"
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isActive = prefs.getBoolean(KEY_ACTIVE, false)

        if (isActive) {
            deactivateMeetingMode()
        } else {
            activateMeetingMode()
        }
    }

    private fun activateMeetingMode() {
        val db = NexAlarmDatabase.getDatabase(this)
        val repo = AlarmRepository(db.alarmDao())

        CoroutineScope(Dispatchers.IO).launch {
            val todayDow = mapCalendarDow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
            val todayAlarms = repo.getTodayAlarms(todayDow)

            val editor = prefs.edit()
            for (alarm in todayAlarms) {
                // Save original vibrate state
                editor.putBoolean("$KEY_ORIGINAL_PREFIX${alarm.id}", alarm.vibrateOnly)
                // Set to vibrate-only
                if (!alarm.vibrateOnly) {
                    repo.setVibrateOnly(alarm.id, true)
                }
            }
            editor.putBoolean(KEY_ACTIVE, true)
            editor.apply()

            launch(Dispatchers.Main) { updateTile() }
        }
    }

    private fun deactivateMeetingMode() {
        val db = NexAlarmDatabase.getDatabase(this)
        val repo = AlarmRepository(db.alarmDao())

        CoroutineScope(Dispatchers.IO).launch {
            val todayDow = mapCalendarDow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
            val todayAlarms = repo.getTodayAlarms(todayDow)

            val editor = prefs.edit()
            for (alarm in todayAlarms) {
                val original = prefs.getBoolean("$KEY_ORIGINAL_PREFIX${alarm.id}", false)
                repo.setVibrateOnly(alarm.id, original)
                editor.remove("$KEY_ORIGINAL_PREFIX${alarm.id}")
            }
            editor.putBoolean(KEY_ACTIVE, false)
            editor.apply()

            launch(Dispatchers.Main) { updateTile() }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isActive = prefs.getBoolean(KEY_ACTIVE, false)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "Meeting Mode ON" else "Meeting Mode"
        tile.updateTile()
    }

    private fun mapCalendarDow(calDow: Int): Int = when (calDow) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}
