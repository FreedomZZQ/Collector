package me.zq.collectors.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.screens.CollectionDetailScreen
import me.zq.collectors.ui.screens.CollectionsHomeScreen
import me.zq.collectors.ui.screens.ItemDetailScreen
import me.zq.collectors.ui.screens.ItemEditorScreen
import me.zq.collectors.ui.screens.RecycleBinScreen
import me.zq.collectors.ui.screens.SearchScreen
import me.zq.collectors.ui.screens.SettingsScreen
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.vm.CollectorViewModel
import me.zq.collectors.vm.SettingsViewModel

// Store + settings injected through the composition (mirrors iOS @EnvironmentObject).
val LocalStore = staticCompositionLocalOf<CollectorViewModel> { error("CollectorViewModel not provided") }
val LocalSettings = staticCompositionLocalOf<SettingsViewModel> { error("SettingsViewModel not provided") }

object Routes {
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val RECYCLE = "settings/recycle"
    const val COLLECTION = "collection/{cid}"
    const val ITEM = "collection/{cid}/item/{iid}"
    const val EDITOR = "editor/{cid}?iid={iid}"

    fun collection(cid: String) = "collection/$cid"
    fun item(cid: String, iid: String) = "collection/$cid/item/$iid"
    fun editorNew(cid: String) = "editor/$cid"
    fun editorEdit(cid: String, iid: String) = "editor/$cid?iid=$iid"
}

private data class Tab(val route: String, val icon: String, val label: String)

private val tabs = listOf(
    Tab(Routes.LIBRARY, "layers", "Library"),
    Tab(Routes.SEARCH, "search", "Search"),
    Tab(Routes.SETTINGS, "gear", "Settings"),
)

@Composable
fun AppScaffold() {
    val p = LocalPalette.current
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val hideBar = currentRoute == Routes.ITEM || currentRoute == Routes.EDITOR
    val selectedTab = when {
        currentRoute == null -> Routes.LIBRARY
        currentRoute.startsWith("collection") || currentRoute.startsWith("editor") -> Routes.LIBRARY
        currentRoute.startsWith("settings") -> Routes.SETTINGS
        currentRoute.startsWith("search") -> Routes.SEARCH
        else -> currentRoute
    }

    Scaffold(
        containerColor = p.bg,
        bottomBar = {
            if (!hideBar) {
                NavigationBar(containerColor = p.surface, contentColor = p.text) {
                    for (tab in tabs) {
                        NavigationBarItem(
                            selected = selectedTab == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, size = 24.dp, tint = Color.Unspecified) },
                            label = { Text(tab.label, style = sans(11f, FontWeight.Medium)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = p.accent,
                                selectedTextColor = p.accent,
                                indicatorColor = p.accentSoft,
                                unselectedIconColor = p.faint,
                                unselectedTextColor = p.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(inner),
        ) {
            composable(Routes.LIBRARY) { CollectionsHomeScreen(nav) }
            composable(Routes.SEARCH) { SearchScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.RECYCLE) { RecycleBinScreen(nav) }
            composable(
                Routes.COLLECTION,
                arguments = listOf(navArgument("cid") { type = NavType.StringType }),
            ) { entry ->
                CollectionDetailScreen(nav, entry.arguments?.getString("cid") ?: "")
            }
            composable(
                Routes.ITEM,
                arguments = listOf(
                    navArgument("cid") { type = NavType.StringType },
                    navArgument("iid") { type = NavType.StringType },
                ),
            ) { entry ->
                ItemDetailScreen(
                    nav,
                    entry.arguments?.getString("cid") ?: "",
                    entry.arguments?.getString("iid") ?: "",
                )
            }
            composable(
                Routes.EDITOR,
                arguments = listOf(
                    navArgument("cid") { type = NavType.StringType },
                    navArgument("iid") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                ItemEditorScreen(
                    nav,
                    entry.arguments?.getString("cid") ?: "",
                    entry.arguments?.getString("iid"),
                )
            }
        }
    }
}
