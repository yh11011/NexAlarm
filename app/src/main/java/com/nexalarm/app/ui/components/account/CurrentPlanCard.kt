package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.FeatureFlags

/**
 * 目前方案卡片
 * 顯示付費狀態和使用進度
 */
@Composable
fun CurrentPlanCard(
    isPremium: Boolean,
    isLoggedIn: Boolean,
    alarmUsed: Int,
    folderUsed: Int,
    onChangePasswordClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nexGlassSurface(20.dp, elevated = isPremium)
            .padding(20.dp)
    ) {
        Column {
            // 方案標題
            PlanTitle(isPremium = isPremium)

            Spacer(modifier = Modifier.height(6.dp))

            // 修改密碼按鈕（已登入才顯示）
            if (isLoggedIn) {
                ChangePasswordButton(onClick = onChangePasswordClick)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 付費狀態相關內容
            if (isPremium) {
                PremiumStatusText()
            } else {
                FreePlanContent(
                    alarmUsed = alarmUsed,
                    folderUsed = folderUsed
                )
            }
        }
    }
}

/**
 * 方案標題
 */
@Composable
private fun PlanTitle(isPremium: Boolean) {
    Text(
        text = S.currentPlan,
        fontSize = 12.sp,
        color = TextTertiary,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            text = if (isPremium) S.premiumPlan else S.freePlan,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPremium) PrimaryBlue else TextPrimary
        )
        if (isPremium) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 修改密碼按鈕
 */
@Composable
private fun ChangePasswordButton(
    onClick: () -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = TextSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCard)
    ) {
        Text(S.changePassword, fontSize = 14.sp)
    }
}

/**
 * 付費狀態文字
 */
@Composable
private fun PremiumStatusText() {
    Text(
        text = S.premiumUnlocked,
        fontSize = 12.sp,
        color = TextSecondary
    )
}

/**
 * 免費方案內容
 */
@Composable
private fun FreePlanContent(
    alarmUsed: Int,
    folderUsed: Int
) {
    Text(
        text = S.usageLimits,
        fontSize = 12.sp,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 鬧鐘使用進度
    UsageProgress(
        label = S.alarmUsage(alarmUsed, FeatureFlags.FREE_ALARM_LIMIT),
        used = alarmUsed,
        limit = FeatureFlags.FREE_ALARM_LIMIT
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 資料夾使用進度
    UsageProgress(
        label = if (isAppEnglish) "$folderUsed / ${FeatureFlags.FREE_FOLDER_LIMIT} folders"
               else "$folderUsed / ${FeatureFlags.FREE_FOLDER_LIMIT} 個資料夾",
        used = folderUsed,
        limit = FeatureFlags.FREE_FOLDER_LIMIT
    )
}

/**
 * 使用進度指示器
 */
@Composable
private fun UsageProgress(
    label: String,
    used: Int,
    limit: Int
) {
    val isFull = used >= limit

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        if (isFull) {
            Text(if (isAppEnglish) "Full" else "已滿", fontSize = 12.sp, color = DangerRed)
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { (used.toFloat() / limit).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(4.dp),
        color = if (isFull) DangerRed else PrimaryBlue,
        trackColor = DarkCard
    )
}
