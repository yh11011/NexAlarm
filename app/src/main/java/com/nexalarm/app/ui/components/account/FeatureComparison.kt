package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.FeatureFlags

/**
 * 付費功能比較卡片
 * 顯示免費版與付費版功能對照
 */
@Composable
fun FeatureComparison(isPremium: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nexGlassSurface(20.dp)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = S.premiumFeatures,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = TextPrimary
            )
            FeatureRow(
                text = S.unlimitedAlarms,
                enabled = isPremium,
                freeLimit = if (!isPremium) FeatureFlags.FREE_ALARM_LIMIT else null
            )
            FeatureRow(
                text = S.unlimitedFolders,
                enabled = isPremium,
                freeLimit = if (!isPremium) FeatureFlags.FREE_FOLDER_LIMIT else null
            )
            FeatureRow(text = S.cloudBackupRestore, enabled = isPremium)
        }
    }
}

/**
 * 功能比較行
 * 顯示單個功能的啟用狀態和免費版限制
 */
@Composable
private fun FeatureRow(text: String, enabled: Boolean, freeLimit: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (enabled) PrimaryBlue else TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = if (enabled) TextPrimary else TextTertiary
            )
            if (!enabled && freeLimit != null) {
                Text(
                    text = if (isAppEnglish) "Free: $freeLimit" else "免費: $freeLimit 個",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}
