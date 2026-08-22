package com.lumenconnection.stream.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumenconnection.stream.R

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{mediaId}"
    fun player(mediaId: Long) = "player/$mediaId"
}

@Composable
fun AppNav(sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        Triple(Routes.HOME, Icons.Filled.Download, R.string.nav_home),
        Triple(Routes.LIBRARY, Icons.Filled.VideoLibrary, R.string.nav_library),
        Triple(Routes.SETTINGS, Icons.Filled.Settings, R.string.nav_settings),
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.first }) {
                NavigationBar {
                    tabs.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    sharedUrl = sharedUrl,
                    onSharedUrlConsumed = onSharedUrlConsumed,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(onOpenMedia = { id -> navController.navigate(Routes.player(id)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.PLAYER) { entry ->
                val id = entry.arguments?.getString("mediaId")?.toLongOrNull() ?: return@composable
                PlayerScreen(mediaId = id)
            }
        }
    }
}
