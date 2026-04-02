package com.nexalarm.app.service

import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MeetingModeTileService : TileService() {

    companion object {
        private const val PREFS_NAME = "meeting_mode_prefs"
        private const val KEY_ACTIVE = "meeting_mode_active"
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
        prefs.edit().putBoolean(KEY_ACTIVE, !isActive).apply()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isActive = prefs.getBoolean(KEY_ACTIVE, false)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "Meeting Mode ON" else "Meeting Mode"
        tile.updateTile()
    }
}
