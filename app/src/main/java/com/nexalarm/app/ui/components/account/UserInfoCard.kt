package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*

/**
 * 使用者資訊卡片
 * 支援登入和未登入兩種狀態
 */
@Composable
fun UserInfoCard(
    isLoggedIn: Boolean,
    authDisplayName: String?,
    authUsername: String?,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nexGlassSurface(20.dp)
            .padding(20.dp)
    ) {
        if (isLoggedIn) {
            // 已登入：顯示頭像 + 名稱 + 登出按鈕
            LoggedInUserInfo(
                authDisplayName = authDisplayName,
                authUsername = authUsername,
                onLogout = onLogout
            )
        } else {
            // 未登入
            LoggedOutUserInfo(
                onLoginClick = onLoginClick
            )
        }
    }
}

/**
 * 已登入使用者資訊
 */
@Composable
private fun LoggedInUserInfo(
    authDisplayName: String?,
    authUsername: String?,
    onLogout: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 頭像圓圈（顯示名稱首字）
        val displayChar = (authDisplayName ?: authUsername ?: "?")
            .uppercase().take(1)
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(PrimaryBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayChar,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (!authDisplayName.isNullOrBlank()) {
                Text(
                    text = authDisplayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Text(
                text = authUsername ?: "",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = S.loggedInAs,
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
        TextButton(onClick = onLogout) {
            Text(S.logout, color = DangerRed, fontSize = 13.sp)
        }
    }
}

/**
 * 未登入使用者資訊
 */
@Composable
private fun LoggedOutUserInfo(
    onLoginClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(DarkCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = S.notLoggedIn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
        TextButton(onClick = onLoginClick) {
            Text(S.loginToAccount, color = PrimaryBlue, fontSize = 13.sp)
        }
    }
}
