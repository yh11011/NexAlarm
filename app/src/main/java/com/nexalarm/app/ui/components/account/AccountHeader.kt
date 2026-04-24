package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.TextPrimary
import com.nexalarm.app.ui.theme.S

/**
 * 帳號頁面頂部導覽列
 */
@Composable
fun AccountHeader(
    openMenu: () -> Unit
) {
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
            text = S.account,
            fontSize = 20.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}