package com.nexalarm.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.FeatureFlags
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    folderUsed: Int,
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
    val hasPlayStorePurchase by billingManager.hasPlayStorePurchase.collectAsState()
    val isLoggedIn = authUsername != null
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var deactivateError by remember { mutableStateOf<String?>(null) }

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
            onDismiss = { showUpgradeDialog = false },
            onPromoSuccess = {
                onPremiumStatusChanged(true)
                showUpgradeDialog = false
            },
            onPurchase = {
                showUpgradeDialog = false
                val activity = context as? Activity ?: return@UpgradeDialog
                billingManager.launchPurchaseFlow(activity)
                onPremiumStatusChanged(true)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 頂部導覽列 ──
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

            // ── 使用者資訊卡片 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                if (isLoggedIn) {
                    // 已登入：顯示頭像 + 名稱 + 登出按鈕
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
                                color = TextPrimary
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
                } else {
                    // 未登入
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
            }

            // ── 修改密碼按鈕（已登入才顯示）──
            if (isLoggedIn) {
                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DarkCard)
                ) {
                    Text(S.changePassword, fontSize = 14.sp)
                }
            }

            // ── 目前方案卡片 ──
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = if (folderUsed >= FeatureFlags.FREE_FOLDER_LIMIT) DangerRed else PrimaryBlue,
                            trackColor = DarkCard
                        )
                    }
                }
            }

            // ── 功能比較 ──
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

            // 停用付費版錯誤提示
            if (deactivateError != null) {
                Text(
                    text = deactivateError!!,
                    color = DangerRed,
                    fontSize = 13.sp,
                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 4.dp)
                )
            }

            // ── 升級按鈕 ──
            Button(
                onClick = {
                    if (isPremium) {
                        if (hasPlayStorePurchase) {
                            // Google Play 有效購買 → 不允許本地停用，需由使用者到 Play Store 取消
                            deactivateError = if (com.nexalarm.app.ui.theme.isAppEnglish)
                                "Your purchase is managed by Google Play.\nTo cancel, go to Play Store → Subscriptions."
                            else
                                "付費版由 Google Play 管理，請前往 Play 商店 → 訂閱 取消"
                        } else {
                            // 優惠碼設定的付費版 → 允許本地停用
                            deactivateError = null
                            onPremiumStatusChanged(false)
                        }
                    } else {
                        deactivateError = null
                        // 未付費：開啟升級 dialog（可輸入優惠碼或正常購買）
                        showUpgradeDialog = true
                    }
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
private fun ChangePasswordDialog(
    token: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                text = S.changePassword,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCard,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryBlue
                )
                OutlinedTextField(
                    value = currentPw,
                    onValueChange = { currentPw = it; errorMsg = null },
                    label = { Text(S.currentPassword) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading && !success
                )
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it; errorMsg = null },
                    label = { Text(S.newPassword) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading && !success
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it; errorMsg = null },
                    label = { Text(S.confirmNewPassword) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading && !success
                )
                when {
                    success -> Text(
                        text = S.passwordChanged,
                        color = PrimaryBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    isLoading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = TextSecondary,
                            strokeWidth = 2.dp
                        )
                        Text(S.changingPassword, color = TextSecondary, fontSize = 13.sp)
                    }
                    errorMsg != null -> Text(
                        text = errorMsg!!,
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (success) { onDismiss(); return@Button }
                        when {
                            currentPw.isBlank() || newPw.isBlank() || confirmPw.isBlank() ->
                                errorMsg = S.passwordTooShort
                            newPw.length < 6 -> errorMsg = S.passwordTooShort
                            newPw != confirmPw -> errorMsg = S.passwordMismatch
                            else -> scope.launch {
                                isLoading = true
                                errorMsg = null
                                AuthRepository.changePassword(currentPw, newPw, token)
                                    .onSuccess { success = true }
                                    .onFailure { e -> errorMsg = e.message ?: S.loginError }
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        text = if (success) S.confirm else S.changePassword,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = { if (!isLoading) onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(S.cancel, color = TextTertiary)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun UpgradeDialog(
    onDismiss: () -> Unit,
    onPromoSuccess: () -> Unit,
    onPurchase: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var promoCode by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf<String?>(null) }
    var promoSuccess by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = S.upgradeToPremium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = S.promoCodeHint,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it; promoError = null; promoSuccess = false },
                    label = { Text(S.promoCode) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard,
                        focusedLabelColor = PrimaryBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )
                when {
                    promoSuccess -> Text(
                        text = S.promoSuccess,
                        color = PrimaryBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    isValidating -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = TextSecondary,
                            strokeWidth = 2.dp
                        )
                        Text(S.validating, color = TextSecondary, fontSize = 13.sp)
                    }
                    promoError != null -> Text(
                        text = promoError!!,
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 套用優惠碼按鈕（呼叫伺服器驗證）
                Button(
                    onClick = {
                        if (promoCode.isBlank()) {
                            promoError = S.promoCodeEmpty
                            return@Button
                        }
                        scope.launch {
                            isValidating = true
                            promoError = null
                            val result = AuthRepository.validatePromoCode(promoCode.trim())
                            isValidating = false
                            result
                                .onSuccess { isValid ->
                                    if (isValid) {
                                        promoSuccess = true
                                        onPromoSuccess()
                                    } else {
                                        promoError = S.promoCodeInvalid
                                    }
                                }
                                .onFailure { e ->
                                    // 400 = 無效優惠碼；其他 = 網路錯誤
                                    val msg = e.message ?: ""
                                    promoError = if ("Invalid promo" in msg) S.promoCodeInvalid
                                                 else S.promoNetworkError
                                }
                        }
                    },
                    enabled = !isValidating && !promoSuccess,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(S.applyPromo, fontWeight = FontWeight.SemiBold)
                }
                // Google Play 購買按鈕
                OutlinedButton(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DarkCard)
                ) {
                    Text(S.buyWithGooglePlay)
                }
                // 取消
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(S.cancel, color = TextTertiary)
                }
            }
        },
        dismissButton = {}
    )
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
