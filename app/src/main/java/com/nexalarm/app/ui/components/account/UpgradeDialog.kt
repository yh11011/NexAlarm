package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 升級 Premium 對話框
 * 提供優惠碼兌換和 Google Play 購買兩種升級方式
 */
@Composable
fun UpgradeDialog(
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextOnPrimary
                    )
                ) {
                    Text(S.applyPromo, fontWeight = FontWeight.SemiBold)
                }
                // Google Play 購買按鈕
                OutlinedButton(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
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
