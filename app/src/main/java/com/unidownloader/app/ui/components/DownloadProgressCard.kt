package com.unidownloader.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unidownloader.app.data.model.DownloadStatus
import com.unidownloader.app.data.model.DownloadTask
import com.unidownloader.app.ui.theme.AccentPrimary
import com.unidownloader.app.ui.theme.AccentSecondary
import com.unidownloader.app.ui.theme.DarkCardBorder
import com.unidownloader.app.ui.theme.DarkSurfaceElevated
import com.unidownloader.app.ui.theme.ErrorRed
import com.unidownloader.app.ui.theme.SuccessGreen
import com.unidownloader.app.ui.theme.TextMuted
import com.unidownloader.app.ui.theme.TextPrimary
import com.unidownloader.app.ui.theme.TextSecondary
import com.unidownloader.app.ui.theme.WarningAmber
import com.unidownloader.app.utils.FormatUtils

@Composable
fun DownloadProgressCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (task.progressPercent / 100f).coerceIn(0f, 1f),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(18.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail / Icon
            if (!task.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = task.thumbnailUrl,
                    contentDescription = task.mediaTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1522))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.outputFileName.ifBlank { task.mediaTitle },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = task.stageDescription,
                    color = when (task.status) {
                        DownloadStatus.COMPLETED -> SuccessGreen
                        DownloadStatus.FAILED -> ErrorRed
                        DownloadStatus.CANCELLED -> WarningAmber
                        else -> AccentSecondary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (task.isActive) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel download",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { if (task.progressPercent > 0) animatedProgress else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when (task.status) {
                DownloadStatus.COMPLETED -> SuccessGreen
                DownloadStatus.FAILED -> ErrorRed
                else -> AccentPrimary
            },
            trackColor = DarkCardBorder
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stats Row: Percentage, Downloaded Size, Speed, ETA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${task.progressPercent.toInt()}%",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            val sizeText = if (task.totalBytes > 0) {
                "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)}"
            } else {
                FormatUtils.formatBytes(task.downloadedBytes)
            }
            Text(
                text = sizeText,
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (task.isActive && task.speedBytesPerSec > 0) {
                Text(
                    text = FormatUtils.formatSpeed(task.speedBytesPerSec),
                    color = AccentSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (task.isActive && task.etaSeconds > 0) {
                Text(
                    text = FormatUtils.formatEta(task.etaSeconds),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (task.status == DownloadStatus.FAILED && !task.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.errorMessage,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
