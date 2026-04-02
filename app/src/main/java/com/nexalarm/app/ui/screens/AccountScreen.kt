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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.FeatureFlags
import kotlinx.coroutines.launch

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
    onLogout: () -> Unit,
    // 雲端同步
    onManualUpload: () -> Unit = {},
    isSyncing: Boolean = false,
    lastSyncTime: Long = 0L
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

            // ── 雲端同步卡片（已登入才顯示）──
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 標題
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = S.cloudSync,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // 最後同步時間
                        val lastSyncText = if (lastSyncTime == 0L) {
                            S.neverSynced
                        } else {
                            val diff = System.currentTimeMillis() - lastSyncTime
                            val minutes = diff / (1000 * 60)
                            when {
                                minutes < 1 -> S.justNow
                                minutes < 60 -> if (SettingsManager(context).isEnglish)
                                    "$minutes minute${if (minutes > 1) "s" else ""} ago"
                                else "${minutes}分鐘前"
                                else -> {
                                    val hours = minutes / 60
                                    if (SettingsManager(context).isEnglish)
                                        "$hours hour${if (hours > 1) "s" else ""} ago"
                                    else "${hours}小時前"
                                }
                            }
                        }
                        Text(
                            text = "${S.lastSyncPrefix}$lastSyncText",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 上傳按鈕
                        Button(
                            onClick = onManualUpload,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimary
                                )
                                Text(S.syncing, fontSize = 14.sp)
                            } else {
                                Text(S.uploadToServer, fontSize = 14.sp)
                            }
                        }
                    }
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
                        Spacer(modifier = Modifier.height(10.dp))
                        // 鬧鐘使用進度
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(S.alarmUsage(alarmUsed, FeatureFlags.FREE_ALARM_LIMIT), fontSize = 12.sp, color = TextSecondary)
                            if (alarmUsed >= FeatureFlags.FREE_ALARM_LIMIT)
                                Text(if (isAppEnglish) "Full" else "已滿", fontSize = 12.sp, color = DangerRed)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (alarmUsed.toFloat() / FeatureFlags.FREE_ALARM_LIMIT).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = if (alarmUsed >= FeatureFlags.FREE_ALARM_LIMIT) DangerRed else PrimaryBlue,
                            trackColor = DarkCard
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // 資料夾使用進度
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAppEnglish) "$folderUsed / ${FeatureFlags.FREE_FOLDER_LIMIT} folders"
                                       else "$folderUsed / ${FeatureFlags.FREE_FOLDER_LIMIT} 個資料夾",
                                fontSize = 12.sp, color = TextSecondary)
                            if (folderUsed >= FeatureFlags.FREE_FOLDER_LIMIT)
                                Text(if (isAppEnglish) "Full" else "已滿", fontSize = 12.sp, color = DangerRed)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (folderUsed.toFloat() / FeatureFlags.FREE_FOLDER_LIMIT).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
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
                    FeatureRow(text = S.prioritySupport, enabled = isPremium)
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
    var showPassword by remember { mutableStateOf(false) }

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
                val eyeIcon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                val eyeDesc = if (showPassword) {
                    if (isAppEnglish) "Hide password" else "隱藏密碼"
                } else {
                    if (isAppEnglish) "Show password" else "顯示密碼"
                }
                val pwTransform = if (showPassword)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    PasswordVisualTransformation()

                OutlinedTextField(
                    value = currentPw,
                    onValueChange = { currentPw = it; errorMsg = null },
                    label = { Text(S.currentPassword) },
                    singleLine = true,
                    visualTransformation = pwTransform,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(eyeIcon, contentDescription = eyeDesc, tint = TextSecondary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading && !success
                )
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it; errorMsg = null },
                    label = { Text(S.newPassword) },
                    singleLine = true,
                    visualTransformation = pwTransform,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(eyeIcon, contentDescription = eyeDesc, tint = TextSecondary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading && !success
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it; errorMsg = null },
                    label = { Text(S.confirmNewPassword) },
                    singleLine = true,
                    visualTransformation = pwTransform,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(eyeIcon, contentDescription = eyeDesc, tint = TextSecondary)
                        }
                    },
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
    authToken: String?,
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
                            // 已登入：使用帳號綁定的兌換端點，將 Premium 狀態存到伺服器
                            // 未登入：僅本地驗證（重裝後需重新兌換）
                            val result = if (authToken != null) {
                                AuthRepository.redeemPromoCode(promoCode.trim(), authToken)
                            } else {
                                AuthRepository.validatePromoCode(promoCode.trim())
                            }
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
                                    val msg = e.message ?: ""
                                    promoError = if ("Invalid promo" in msg || "無效" in msg) S.promoCodeInvalid
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
