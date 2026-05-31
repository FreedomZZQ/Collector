package me.zq.collectors.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import me.zq.collectors.data.Accent
import me.zq.collectors.data.ItemLayout
import me.zq.collectors.data.SettingsStore
import me.zq.collectors.data.ThemeMode

/** Theme / accent / default-view preferences, backed by SharedPreferences. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)

    var themeMode by mutableStateOf(store.themeMode)
        private set
    var accent by mutableStateOf(store.accent)
        private set
    var defaultView by mutableStateOf(store.defaultView)
        private set

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        store.themeMode = mode
    }

    fun updateAccent(value: Accent) {
        accent = value
        store.accent = value
    }

    fun updateView(layout: ItemLayout) {
        defaultView = layout
        store.defaultView = layout
    }
}
