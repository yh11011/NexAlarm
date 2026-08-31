package com.nexalarm.app.ui.components.settings

import androidx.compose.runtime.Composable
import com.nexalarm.app.ui.components.NexTopBar
import com.nexalarm.app.ui.theme.S

/**
 * 設定頁面頂部導覽列
 */
@Composable
fun SettingsHeader(openMenu: () -> Unit) {
    NexTopBar(title = S.settings, onMenuClick = openMenu)
}
