package com.nexalarm.app.ui.components.account

import androidx.compose.runtime.Composable
import com.nexalarm.app.ui.components.NexTopBar
import com.nexalarm.app.ui.theme.S

/**
 * 帳號頁面頂部導覽列
 */
@Composable
fun AccountHeader(
    openMenu: () -> Unit
) {
    NexTopBar(title = S.account, onMenuClick = openMenu)
}
