package com.unidownloader.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.unidownloader.app.UniDownloaderApp
import com.unidownloader.app.data.model.DownloadHistoryItem
import com.unidownloader.app.ui.theme.AccentPrimary
import com.unidownloader.app.ui.theme.DarkBackground
import com.unidownloader.app.ui.theme.DarkCardBorder
import com.unidownloader.app.ui.theme.DarkSurface
import com.unidownloader.app.ui.theme.DarkSurfaceElevated
import com.unidownloader.app.ui.theme.ErrorRed
import com.unidownloader.app.ui.theme.TextMuted
import com.unidownloader.app.ui.theme.TextPrimary
import com.unidownloader.app.ui.theme.TextSecondary
import com.unidownloader.app.utils.FormatUtils
import java.io.File

@Composable
fun DownloadsScreen(
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by UniDownloaderApp.downloadRepository.history.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredList = remember(historyList, searchQuery, selectedFilter) {
        historyList.filter { item ->
            val matchesSearch = item.fileName.contains(searchQuery, ignoreCase = true) ||
                    item.title.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Video" -> item.extension.equals("MP4", true) || item.extension.equals("MKV", true) || item.extension.equals("WEBM", true)
                "Audio" -> item.extension.equals("MP3", true) || item.extension.equals("M4A", true) || item.extension.equals("OPUS", true)
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val totalStorageUsed = remember(historyList) {
        historyList.sumOf { it.fileSizeBytes }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved Downloads",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved in Downloads/ (${FormatUtils.formatBytes(totalStorageUsed)} used)",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search downloaded files...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Tabs (All, Video, Audio)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Video", "Audio").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentPrimary else DarkSurfaceElevated)
                        .border(1.dp, if (isSelected) AccentPrimary else DarkCardBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching files found" else "No saved downloads yet",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    DownloadHistoryCard(
                        item = item,
                        onOpen = {
                            openFileIntent(context, item)
                        },
                        onShare = {
                            shareFileIntent(context, item)
                        },
                        onDelete = {
                            UniDownloaderApp.downloadRepository.deleteHistoryItem(item)
                            Toast.makeText(context, "Deleted ${item.fileName}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadHistoryCard(
    item: DownloadHistoryItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(14.dp)
    ) {
        // Media format badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AccentPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.extension,
                color = AccentPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${FormatUtils.formatBytes(item.fileSizeBytes)}  •  ${FormatUtils.formatDate(item.downloadedAt)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Open file", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = AccentPrimary) },
                    onClick = {
                        menuExpanded = false
                        onOpen()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = TextSecondary) },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ErrorRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun openFileIntent(context: android.content.Context, item: DownloadHistoryItem) {
    try {
        val file = File(item.filePath)
        val uri: Uri = if (item.fileUriString.startsWith("content://")) {
            Uri.parse(item.fileUriString)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this media format", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFileIntent(context: android.content.Context, item: DownloadHistoryItem) {
    try {
        val file = File(item.filePath)
        val uri: Uri = if (item.fileUriString.startsWith("content://")) {
            Uri.parse(item.fileUriString)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${item.fileName}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share this file", Toast.LENGTH_SHORT).show()
    }
}
