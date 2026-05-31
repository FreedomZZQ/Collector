package me.zq.collectors.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import me.zq.collectors.data.Accent
import me.zq.collectors.data.ThemeMode

/** The resolved design palette, injected through the composition (like iOS's environment). */
val LocalPalette = staticCompositionLocalOf { makePalette(dark = false, accent = Accent.AMBER) }

@Composable
fun CollectorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.AMBER,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = remember(dark, accent) { makePalette(dark, accent) }

    // Map the custom palette onto a Material color scheme so Material primitives
    // (ripple, bottom-sheet scrim, text cursor) stay on-brand. Dynamic color is
    // intentionally disabled — the design uses a fixed 4-accent system.
    val colorScheme = remember(palette, dark) {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.muted,
            error = palette.danger,
            outline = palette.line,
            outlineVariant = palette.line,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
