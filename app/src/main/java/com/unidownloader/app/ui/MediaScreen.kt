package com.unidownloader.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidownloader.app.UniDownloaderApp
import com.unidownloader.app.components.FormatSelector
import com.unidownloader.app.components.MediaCard
import com.unidownloader.app.components.QualitySelector
import com.unidownloader.app.data.model.MediaInfo
import com.unidownloader.app.ui.theme.AccentGradientEnd
import com.unidownloader.app.ui.theme.AccentGradientStart
import com.unidownloader.app.ui.theme.DarkBackground
import com.unidownloader.app.ui.theme.TextPrimary
import com.unidownloader.app.ui.theme.TextSecondary

@Composable
fun MediaScreen(
    mediaInfo: MediaInfo,
    onNavigateBack: () -> Unit,
    onDownloadStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val videoQualities = remember(mediaInfo) { mediaInfo.getAvailableVideoQualities() }
    val audioQualities = remember(mediaInfo) { mediaInfo.getAvailableAudioQualities() }

    var selectedVideoQuality by remember { mutableStateOf(videoQualities.firstOrNull() ?: "Best") }
    var selectedAudioQuality by remember { mutableStateOf(audioQualities.firstOrNull() ?: "320 kbps") }

    val isAudioOnly = selectedVideoQuality == "Audio only"
    val containerFormats = remember(isAudioOnly) {
        mediaInfo.getSupportedContainers(!isAudioOnly)
    }

    var selectedFormat by remember(isAudioOnly) {
        mutableStateOf(if (isAudioOnly) "MP3" else "MP4")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Media Details",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Preview Card
        MediaCard(mediaInfo = mediaInfo)

        Spacer(modifier = Modifier.height(22.dp))

        // Section Header
        Text(
            text = "Download Options",
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Video Quality Picker
        QualitySelector(
            label = "Video Quality",
            selectedOption = selectedVideoQuality,
            options = videoQualities,
            onOptionSelected = {
                selectedVideoQuality = it
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Audio Quality Picker
        QualitySelector(
            label = "Audio Quality",
            selectedOption = selectedAudioQuality,
            options = audioQualities,
            onOptionSelected = {
                selectedAudioQuality = it
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Format Selector
        Text(
            text = "Format Container",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        FormatSelector(
            formats = containerFormats,
            selectedFormat = selectedFormat,
            onFormatSelected = { selectedFormat = it }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Download Action Button
        Button(
            onClick = {
                UniDownloaderApp.downloadManager.startDownload(
                    mediaInfo = mediaInfo,
                    selectedVideoQuality = selectedVideoQuality,
                    selectedAudioQuality = selectedAudioQuality,
                    selectedFormat = selectedFormat
                )
                Toast.makeText(context, "Download started: ${mediaInfo.title}", Toast.LENGTH_SHORT).show()
                onDownloadStarted()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(AccentGradientStart, AccentGradientEnd)),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Download ${selectedFormat.uppercase()}",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
