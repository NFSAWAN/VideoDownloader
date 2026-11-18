package com.app.videodownloader.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.videodownloader.presentation.download.DownloadScreen
import com.app.videodownloader.presentation.download.DownloadViewModel
import com.app.videodownloader.presentation.history.HistoryScreen
import com.app.videodownloader.presentation.status.StatusScreen

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Download("download", "Download", Icons.Default.Home),
    History("history", "Downloads", Icons.AutoMirrored.Filled.List),
    Status("status", "Status", Icons.Default.Star),
}

/** [shareCount] changes every time a link is shared in, which brings the Download tab to the front. */
@Composable
fun AppScaffold(downloadViewModel: DownloadViewModel, shareCount: Int) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun open(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(shareCount) {
        if (shareCount > 0) open(Destination.Download)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { open(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Download.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Download.route) { DownloadScreen(downloadViewModel) }
            composable(Destination.History.route) { HistoryScreen() }
            composable(Destination.Status.route) { StatusScreen() }
        }
    }
}
