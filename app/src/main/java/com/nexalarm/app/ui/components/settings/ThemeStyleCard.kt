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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.util.AppSettingsProvider

/** Two deliberate working modes instead of an uncurated catalogue of styles. */
@Composable
fun ThemeStyleCard(settingsManager: com.nexalarm.app.data.SettingsManager) {
    val currentTheme = AppSettingsProvider.currentThemeMutableState.value.toSupportedTheme()

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
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppTheme.selectableThemes) { theme ->
                ThemeModeRow(theme = theme, selected = theme == currentTheme)
            }
        }
    }
}

@Composable
private fun ThemeModeRow(theme: AppTheme, selected: Boolean) {
    val colors = theme.colors()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.12f) else DarkCard)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) PrimaryBlue else DarkBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable { AppSettingsProvider.setTheme(theme) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp, 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.background))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.card))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(colors.primary)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 9.sp, color = TextOnPrimary)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isAppEnglish) theme.displayNameEn else theme.displayNameZh,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) PrimaryBlue else TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = themeDescription(theme),
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

private fun themeDescription(theme: AppTheme): String = if (isAppEnglish) {
    when (theme) {
        AppTheme.FOCUS_BLUE -> "Bright, calm focus for daytime planning"
        AppTheme.MINIMALIST -> "Dark, low-stimulation clarity for long sessions"
        else -> ""
    }
} else {
    when (theme) {
        AppTheme.FOCUS_BLUE -> "日間規劃清晰明亮，長看不疲勞"
        AppTheme.MINIMALIST -> "深色低刺激資訊層級，適合夜間專注"
        else -> ""
    }
}
