package com.nexalarm.app.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.S
import com.nexalarm.app.ui.theme.isAppEnglish

class MeetingModeTileService : TileService() {

    private val settingsManager: SettingsManager by lazy {
        SettingsManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isActive = settingsManager.isMeetingMode
        settingsManager.isMeetingMode = !isActive
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isActive = settingsManager.isMeetingMode
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) {
            if (isAppEnglish) "Meeting Mode ON" else "會議模式已開啟"
        } else {
            S.meetingMode
        }
        tile.updateTile()
    }
}
