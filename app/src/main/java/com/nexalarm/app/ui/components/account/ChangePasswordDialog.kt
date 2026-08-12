package com.nexalarm.app.ui.components.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 修改密碼對話框
 * 讓已登入使用者修改帳號密碼
 */
@Composable
fun ChangePasswordDialog(
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = TextOnPrimary
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
