package com.nexalarm.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.FeatureFlags

@Composable
fun AccountScreen(
    folderUsed: Int,
    billingManager: BillingManager,
    onPremiumStatusChanged: (Boolean) -> Unit
) {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val isPremium by billingManager.isPremium.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
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
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 目前方案卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isPremium) PrimaryBlue.copy(alpha = 0.15f) else DarkSurface,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = S.currentPlan,
                        fontSize = 12.sp,
                        color = TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${folderUsed} / ${FeatureFlags.FREE_FOLDER_LIMIT} 個資料夾",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (folderUsed.toFloat() / FeatureFlags.FREE_FOLDER_LIMIT).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = if (folderUsed >= FeatureFlags.FREE_FOLDER_LIMIT) DangerRed else PrimaryBlue,
                            trackColor = DarkCard
                        )
                    }
                }
            }

            // 功能比較
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = S.premiumFeatures,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    FeatureRow(text = S.unlimitedFolders, enabled = isPremium)
                    FeatureRow(text = if (isAppEnglish) "Priority support" else "優先客服支援", enabled = isPremium)
                    FeatureRow(text = if (isAppEnglish) "Cloud backup" else "雲端備份", enabled = isPremium)
                }
            }

            // 升級按鈕（透過 Google Play Billing 購買）
            Button(
                onClick = {
                    val activity = context as? Activity ?: return@Button
                    billingManager.launchPurchaseFlow(activity)
                    onPremiumStatusChanged(!isPremium)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) DarkCard else PrimaryBlue,
                    contentColor = if (isPremium) DangerRed else TextPrimary
                )
            ) {
                Text(
                    text = if (isPremium) S.deactivatePremium else S.upgradeToPremium,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureRow(text: String, enabled: Boolean) {
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
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (enabled) TextPrimary else TextTertiary
        )
    }
}
