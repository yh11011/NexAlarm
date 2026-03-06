package com.nexalarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.ui.theme.*

@Composable
fun SettingsScreen() {
    val openMenu = LocalMenuAction.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
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
                text = S.settings,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language setting
        SettingCard(
            title = S.language,
            options = listOf("中文", "English"),
            selectedIndex = if (isAppEnglish) 1 else 0,
            onSelect = { index ->
                isAppEnglish = index == 1
                settingsManager.isEnglish = index == 1
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Theme setting
        SettingCard(
            title = S.theme,
            options = listOf(S.darkMode, S.lightMode),
            selectedIndex = if (isDarkTheme) 0 else 1,
            onSelect = { index ->
                isDarkTheme = index == 0
                settingsManager.isDarkMode = index == 0
            }
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) PrimaryBlue else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}
