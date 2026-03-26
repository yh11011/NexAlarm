package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.data.AuthUser
import com.nexalarm.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    isOnboarding: Boolean,
    onSuccess: (AuthUser) -> Unit,
    onSkip: () -> Unit,
    onBack: (() -> Unit)?
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 登入, 1 = 註冊
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── 頂部導覽列 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            if (!isOnboarding && onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = S.back,
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = S.loginTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            if (isOnboarding) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(S.skip, color = TextSecondary, fontSize = 15.sp)
                }
            }
        }

        // ── 分頁標籤：登入 / 註冊 ──
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = PrimaryBlue,
            divider = { HorizontalDivider(color = DarkCard) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; errorMessage = null },
                text = {
                    Text(
                        S.login,
                        color = if (selectedTab == 0) PrimaryBlue else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; errorMessage = null },
                text = {
                    Text(
                        S.register,
                        color = if (selectedTab == 1) PrimaryBlue else TextSecondary
                    )
                }
            )
        }

        // ── 表單區 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (isOnboarding) {
                Text(
                    text = S.loginWelcome,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // 帳號或 Email
            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it; errorMessage = null },
                label = { Text(S.usernameOrEmail) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = loginTextFieldColors()
            )

            // 顯示名稱（僅註冊）
            if (selectedTab == 1) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(S.displayNameOptional) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = loginTextFieldColors()
                )
            }

            // 密碼
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text(S.password) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                colors = loginTextFieldColors()
            )

            // 錯誤訊息
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = DangerRed,
                    fontSize = 13.sp
                )
            }

            // 送出按鈕
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val result = if (selectedTab == 0) {
                            AuthRepository.login(usernameOrEmail.trim(), password)
                        } else {
                            AuthRepository.register(
                                usernameOrEmail.trim(),
                                password,
                                displayName.trim().takeIf { it.isNotBlank() }
                            )
                        }
                        isLoading = false
                        result
                            .onSuccess { user -> onSuccess(user) }
                            .onFailure { e -> errorMessage = e.message ?: S.loginError }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading && usernameOrEmail.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkCard,
                    disabledContentColor = TextTertiary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (selectedTab == 0) S.login else S.register,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DarkCard,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryBlue
)
