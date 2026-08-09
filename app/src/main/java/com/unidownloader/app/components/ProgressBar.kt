package com.unidownloader.app.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unidownloader.app.ui.theme.AccentGradientEnd
import com.unidownloader.app.ui.theme.AccentGradientStart
import com.unidownloader.app.ui.theme.DarkSurfaceElevated

@Composable
fun ProgressBar(
    progressPercent: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = DarkSurfaceElevated,
    progressBrush: Brush = Brush.horizontalGradient(listOf(AccentGradientStart, AccentGradientEnd))
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        label = "progressBarAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(progressBrush)
        )
    }
}
