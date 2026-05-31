package me.zq.collectors.ui.theme

import androidx.compose.ui.graphics.Color
import me.zq.collectors.data.Accent

// Resolved design tokens. Mirrors the iOS Palette (Theme.swift) value-for-value.

data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val heroStripe: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val lineStrong: Color,
    val danger: Color,
    val onAccent: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentLine: Color,
)

fun accentColor(accent: Accent, dark: Boolean): Color = when (accent) {
    Accent.AMBER -> if (dark) oklch(0.745, 0.125, 62.0) else oklch(0.585, 0.118, 55.0)
    Accent.INDIGO -> if (dark) oklch(0.705, 0.130, 264.0) else oklch(0.545, 0.135, 264.0)
    Accent.FOREST -> if (dark) oklch(0.705, 0.120, 152.0) else oklch(0.515, 0.105, 150.0)
    Accent.PLUM -> if (dark) oklch(0.705, 0.130, 330.0) else oklch(0.540, 0.130, 330.0)
}

/** Swatch shown in Settings (uses the light-mode value for a stable preview). */
fun accentSwatch(accent: Accent): Color = accentColor(accent, false)

fun makePalette(dark: Boolean, accent: Accent): Palette {
    val acc = accentColor(accent, dark)
    return if (dark) {
        val surface = oklch(0.214, 0.009, 250.0)
        Palette(
            bg = oklch(0.172, 0.008, 250.0),
            surface = surface,
            surface2 = oklch(0.258, 0.010, 250.0),
            heroStripe = oklch(0.305, 0.011, 250.0),
            text = oklch(0.945, 0.006, 80.0),
            muted = oklch(0.685, 0.010, 250.0),
            faint = oklch(0.535, 0.012, 250.0),
            line = oklch(0.305, 0.010, 250.0),
            lineStrong = oklch(0.395, 0.012, 250.0),
            danger = oklch(0.68, 0.15, 25.0),
            onAccent = oklch(0.195, 0.02, 70.0),
            accent = acc,
            accentSoft = mixSRGB(acc, surface, 0.13f),
            accentLine = mixSRGB(acc, surface, 0.32f),
        )
    } else {
        val surface = oklch(0.995, 0.004, 85.0)
        Palette(
            bg = oklch(0.972, 0.006, 80.0),
            surface = surface,
            surface2 = oklch(0.945, 0.008, 78.0),
            heroStripe = oklch(0.915, 0.010, 78.0),
            text = oklch(0.255, 0.012, 60.0),
            muted = oklch(0.505, 0.012, 60.0),
            faint = oklch(0.635, 0.010, 60.0),
            line = oklch(0.885, 0.008, 75.0),
            lineStrong = oklch(0.805, 0.010, 72.0),
            danger = oklch(0.56, 0.16, 25.0),
            onAccent = oklch(0.99, 0.008, 90.0),
            accent = acc,
            accentSoft = mixSRGB(acc, surface, 0.13f),
            accentLine = mixSRGB(acc, surface, 0.32f),
        )
    }
}

/** Condition → swatch color (mint/excellent green, good indigo, fair yellow, poor orange). */
fun conditionColor(v: String): Color {
    val hue = when (v.lowercase().trim()) {
        "mint", "excellent" -> 150.0
        "good" -> 244.0
        "fair" -> 60.0
        "poor" -> 25.0
        else -> 244.0
    }
    return oklch(0.62, 0.13, hue)
}
