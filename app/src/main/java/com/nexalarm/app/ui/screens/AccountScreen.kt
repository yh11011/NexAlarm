package com.nexalarm.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.components.account.*
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.BillingManager

@Composable
fun AccountScreen(
    folderUsed: Int,
    alarmUsed: Int,
    billingManager: BillingManager,
    onPremiumStatusChanged: (Boolean) -> Unit,
    // 登入狀態
    authUsername: String?,
    authDisplayName: String?,
    authToken: String?,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit
) {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val isPremium by billingManager.isPremium.collectAsState()
    val isLoggedIn = authUsername != null
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // ── 修改密碼 Dialog ──
    if (showChangePasswordDialog && authToken != null) {
        ChangePasswordDialog(
            token = authToken,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    // ── 升級/優惠碼 Dialog ──
    if (showUpgradeDialog) {
        UpgradeDialog(
            authToken = authToken,
            onDismiss = { showUpgradeDialog = false },
            onPromoSuccess = {
                onPremiumStatusChanged(true)
                showUpgradeDialog = false
            },
            onPurchase = {
                showUpgradeDialog = false
                val activity = context as? Activity ?: return@UpgradeDialog
                billingManager.launchPurchaseFlow(activity)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 頂部導覽列 ──
        AccountHeader(openMenu = openMenu)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── 使用者資訊卡片 ──
            UserInfoCard(
                isLoggedIn = isLoggedIn,
                authDisplayName = authDisplayName,
                authUsername = authUsername,
                onLoginClick = onLoginClick,
                onLogout = onLogout
            )

            // ── 目前方案卡片 ──
            CurrentPlanCard(
                isPremium = isPremium,
                isLoggedIn = isLoggedIn,
                alarmUsed = alarmUsed,
                folderUsed = folderUsed,
                onChangePasswordClick = { showChangePasswordDialog = true }
            )

            // ── 功能比較 ──
            FeatureComparison(isPremium = isPremium)

            // ── 升級按鈕（買斷制，付費後不顯示）──
            if (!isPremium) {
                Button(
                    onClick = { showUpgradeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        text = S.upgradeToPremium,
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

