package com.unidownloader.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MediaInfo : Screen("media_info")
    object ActiveDownloads : Screen("active_downloads")
    object DownloadsHistory : Screen("downloads_history")
    object Settings : Screen("settings")
}
