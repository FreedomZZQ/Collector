package me.zq.collectors.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// OKLCH → sRGB. Colors are authored in OKLCH (matching the design bundle) and
// converted at runtime so the exact design numbers stay the source of truth.

/** Create a Color from an OKLCH triplet (L 0–1, C chroma, H degrees). */
fun oklch(l: Double, c: Double, hDeg: Double, opacity: Double = 1.0): Color {
    val h = hDeg * Math.PI / 180.0
    val a = c * cos(h)
    val b = c * sin(h)

    // OKLab → LMS
    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b
    val lCube = l_ * l_ * l_
    val mCube = m_ * m_ * m_
    val sCube = s_ * s_ * s_

    // LMS → linear sRGB
    val lr = 4.0767416621 * lCube - 3.3077115913 * mCube + 0.2309699292 * sCube
    val lg = -1.2684380046 * lCube + 2.6097574011 * mCube - 0.3413193965 * sCube
    val lb = -0.0041960863 * lCube - 0.7034186147 * mCube + 1.7076147010 * sCube

    return Color(gamma(lr), gamma(lg), gamma(lb), opacity.toFloat())
}

private fun gamma(x: Double): Float {
    val v = x.coerceIn(0.0, 1.0)
    return (if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055).toFloat()
}

/** Mix two colors in sRGB space. `t` is the weight of `a` (CSS color-mix). */
fun mixSRGB(a: Color, b: Color, t: Float): Color = Color(
    red = a.red * t + b.red * (1 - t),
    green = a.green * t + b.green * (1 - t),
    blue = a.blue * t + b.blue * (1 - t),
)
