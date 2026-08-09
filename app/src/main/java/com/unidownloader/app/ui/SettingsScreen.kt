package com.unidownloader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidownloader.app.UniDownloaderApp
import com.unidownloader.app.components.QualitySelector
import com.unidownloader.app.ui.theme.AccentPrimary
import com.unidownloader.app.ui.theme.AccentSecondary
import com.unidownloader.app.ui.theme.DarkBackground
import com.unidownloader.app.ui.theme.DarkCardBorder
import com.unidownloader.app.ui.theme.DarkSurface
import com.unidownloader.app.ui.theme.DarkSurfaceElevated
import com.unidownloader.app.ui.theme.TextMuted
import com.unidownloader.app.ui.theme.TextPrimary
import com.unidownloader.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val settingsRepo = UniDownloaderApp.settingsRepository
    val settings by settingsRepo.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Download Preferences
        SettingsSectionHeader(title = "Download Preferences", icon = Icons.Default.Tune)

        Spacer(modifier = Modifier.height(10.dp))

        QualitySelector(
            label = "Preferred Video Quality",
            selectedOption = settings.defaultVideoQuality,
            options = listOf("Best", "1080p (Full HD)", "720p (HD)", "480p", "360p"),
            onOptionSelected = { settingsRepo.updateDefaultVideoQuality(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        QualitySelector(
            label = "Preferred Audio Format",
            selectedOption = settings.defaultFormat,
            options = listOf("MP3", "M4A", "OPUS"),
            onOptionSelected = { settingsRepo.updateDefaultAudioFormat(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Network & Notifications
        SettingsSectionHeader(title = "Network & Notifications", icon = Icons.Default.Wifi)

        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggleItem(
            title = "Download on Wi-Fi Only",
            subtitle = "Prevent downloading over mobile cellular data",
            checked = settings.wifiOnlyDownloads,
            onCheckedChange = { settingsRepo.updateWifiOnly(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggleItem(
            title = "Download Notifications",
            subtitle = "Show progress and completion notifications",
            checked = settings.downloadNotificationsEnabled,
            onCheckedChange = { settingsRepo.updateNotifications(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // About & Credits
        SettingsSectionHeader(title = "About", icon = Icons.Default.Info)

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "UniDownloader",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version 1.0.0 • Native Android Edition",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vibe coded by Khayal and Antigravity",
                    color = AccentSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentPrimary,
                checkedTrackColor = AccentPrimary.copy(alpha = 0.3f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceElevated
            )
        )
    }
}
