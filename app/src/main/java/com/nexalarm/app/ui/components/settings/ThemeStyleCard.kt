package com.nexalarm.app.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AppSettingsProvider

/**
 * 主題樣式選擇卡片
 * 顯示所有可用的主題樣式縮圖供選擇
 */
@Composable
fun ThemeStyleCard(settingsManager: com.nexalarm.app.data.SettingsManager) {
    val currentTheme = AppSettingsProvider.currentThemeMutableState.value
    val allThemes = AppTheme.entries

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .nexGlassSurface(16.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(S.themeStyle, fontSize = 14.sp, color = TextSecondary)
            Text(
                text = if (isAppEnglish) currentTheme.displayNameEn else currentTheme.displayNameZh,
                fontSize = 13.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.heightIn(max = 480.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allThemes) { theme ->
                val isSelected = theme == currentTheme
                val colors = theme.colors()
                val swatches = theme.swatchColors()
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else DarkCard
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) PrimaryBlue else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            AppSettingsProvider.setTheme(theme)
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 縮圖：四色色塊
                    Box(
                        modifier = Modifier
                            .size(48.dp, 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.background)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.background))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.card))
                        }
                        // 主色條
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(colors.primary)
                        )
                        // 選取勾
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 7.sp, color = TextOnPrimary)
                            }
                        }
                    }
                    Text(
                        text = if (isAppEnglish) theme.displayNameEn else theme.displayNameZh,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PrimaryBlue else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}
