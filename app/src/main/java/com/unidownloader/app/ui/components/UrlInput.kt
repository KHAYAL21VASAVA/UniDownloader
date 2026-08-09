package com.unidownloader.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidownloader.app.ui.theme.AccentPrimary
import com.unidownloader.app.ui.theme.AccentSecondary
import com.unidownloader.app.ui.theme.DarkCardBorder
import com.unidownloader.app.ui.theme.DarkSurfaceElevated
import com.unidownloader.app.ui.theme.TextMuted
import com.unidownloader.app.ui.theme.TextPrimary
import com.unidownloader.app.ui.theme.TextSecondary

@Composable
fun UrlInput(
    urlText: String,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Paste video URL here..."
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Link Icon",
                tint = AccentSecondary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (urlText.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = TextStyle(
                            color = TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                BasicTextField(
                    value = urlText,
                    onValueChange = onUrlChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(AccentPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (urlText.isNotEmpty()) {
                IconButton(
                    onClick = { onUrlChange("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val pasteText = clip.getItemAt(0)?.text?.toString() ?: ""
                        if (pasteText.isNotBlank()) {
                            onUrlChange(pasteText)
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste from clipboard",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
