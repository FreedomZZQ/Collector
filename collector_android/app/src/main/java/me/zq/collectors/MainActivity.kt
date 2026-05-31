package me.zq.collectors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import me.zq.collectors.ui.nav.AppScaffold
import me.zq.collectors.ui.nav.LocalSettings
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.theme.CollectorTheme
import me.zq.collectors.vm.CollectorViewModel
import me.zq.collectors.vm.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Activity-scoped so every screen shares one source of truth.
            val store: CollectorViewModel = viewModel()
            val settings: SettingsViewModel = viewModel()
            CollectorTheme(themeMode = settings.themeMode, accent = settings.accent) {
                CompositionLocalProvider(
                    LocalStore provides store,
                    LocalSettings provides settings,
                ) {
                    AppScaffold()
                }
            }
        }
    }
}
