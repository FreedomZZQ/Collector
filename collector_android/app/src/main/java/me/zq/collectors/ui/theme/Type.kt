package me.zq.collectors.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Three type roles, mirroring iOS:
//   serif (New York → Noto Serif)  — item / collection names + titles
//   sans  (SF Pro   → Roboto)      — UI text
//   mono  (SF Mono  → system mono) — specimen labels + numeric values
// `tracking` maps iOS point letter-spacing onto Compose letterSpacing (sp).

val SerifFamily = FontFamily.Serif
val SansFamily = FontFamily.SansSerif
val MonoFamily = FontFamily.Monospace

fun serif(size: Float, weight: FontWeight = FontWeight.SemiBold, tracking: Float = 0f): TextStyle =
    TextStyle(fontFamily = SerifFamily, fontWeight = weight, fontSize = size.sp, letterSpacing = tracking.sp)

fun sans(size: Float, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f): TextStyle =
    TextStyle(fontFamily = SansFamily, fontWeight = weight, fontSize = size.sp, letterSpacing = tracking.sp)

fun mono(size: Float, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f): TextStyle =
    TextStyle(fontFamily = MonoFamily, fontWeight = weight, fontSize = size.sp, letterSpacing = tracking.sp)

val AppTypography = Typography()

/** Corner radii (iOS Radius). */
object Radius {
    val r = 16.dp
    val sm = 12.dp
}
