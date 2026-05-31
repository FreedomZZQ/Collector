package me.zq.collectors.data

import android.content.Context

// User preferences (mirrors iOS AppSettings), persisted to SharedPreferences.

enum class ThemeMode(val wire: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun from(s: String?): ThemeMode = entries.firstOrNull { it.wire == s } ?: SYSTEM
    }
}

enum class Accent(val wire: String) {
    AMBER("amber"), INDIGO("indigo"), FOREST("forest"), PLUM("plum");

    companion object {
        fun from(s: String?): Accent = entries.firstOrNull { it.wire == s } ?: AMBER
    }
}

enum class ItemLayout(val wire: String) {
    LIST("list"), GRID("grid");

    companion object {
        fun from(s: String?): ItemLayout = entries.firstOrNull { it.wire == s } ?: LIST
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("collector.settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.from(prefs.getString(KEY_THEME, null))
        set(v) = prefs.edit().putString(KEY_THEME, v.wire).apply()

    var accent: Accent
        get() = Accent.from(prefs.getString(KEY_ACCENT, null))
        set(v) = prefs.edit().putString(KEY_ACCENT, v.wire).apply()

    var defaultView: ItemLayout
        get() = ItemLayout.from(prefs.getString(KEY_VIEW, null))
        set(v) = prefs.edit().putString(KEY_VIEW, v.wire).apply()

    private companion object {
        const val KEY_THEME = "themeMode"
        const val KEY_ACCENT = "accent"
        const val KEY_VIEW = "defaultView"
    }
}
