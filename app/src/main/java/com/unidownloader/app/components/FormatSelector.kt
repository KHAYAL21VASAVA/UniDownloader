package com.unidownloader.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidownloader.app.ui.theme.AccentPrimary
import com.unidownloader.app.ui.theme.DarkCardBorder
import com.unidownloader.app.ui.theme.DarkSurfaceElevated
import com.unidownloader.app.ui.theme.TextMuted
import com.unidownloader.app.ui.theme.TextPrimary

@Composable
fun FormatSelector(
    formats: List<String>,
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        formats.forEach { format ->
            val isSelected = format.equals(selectedFormat, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        if (isSelected) 1.5.dp else 0.dp,
                        if (isSelected) AccentPrimary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onFormatSelected(format) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format.uppercase(),
                    color = if (isSelected) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
