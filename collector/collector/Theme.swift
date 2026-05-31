//
//  Theme.swift
//  collector
//
//  Design tokens. Colors are authored in OKLCH (matching the design bundle) and
//  converted to sRGB at runtime so the exact design numbers stay the source of
//  truth. The resolved Palette is injected through the environment.
//

import SwiftUI
import UIKit

// MARK: - OKLCH → sRGB

extension Color {
    /// Create a Color from an OKLCH triplet (L 0–1, C chroma, H degrees).
    init(oklch L: Double, _ C: Double, _ Hdeg: Double, opacity: Double = 1) {
        let h = Hdeg * .pi / 180
        let a = C * cos(h)
        let b = C * sin(h)

        // OKLab → LMS
        let l_ = L + 0.3963377774 * a + 0.2158037573 * b
        let m_ = L - 0.1055613458 * a - 0.0638541728 * b
        let s_ = L - 0.0894841775 * a - 1.2914855480 * b
        let l = l_ * l_ * l_
        let m = m_ * m_ * m_
        let s = s_ * s_ * s_

        // LMS → linear sRGB
        let lr =  4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        let lg = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        let lb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

        func gamma(_ x: Double) -> Double {
            let v = min(max(x, 0), 1)
            return v <= 0.0031308 ? 12.92 * v : 1.055 * pow(v, 1 / 2.4) - 0.055
        }

        self.init(.sRGB, red: gamma(lr), green: gamma(lg), blue: gamma(lb), opacity: opacity)
    }
}

/// Mix two colors in sRGB space. `t` is the weight of `a` (CSS `color-mix`).
private func mixSRGB(_ a: Color, _ b: Color, _ t: Double) -> Color {
    func comps(_ c: Color) -> (Double, Double, Double) {
        var r: CGFloat = 0, g: CGFloat = 0, bl: CGFloat = 0, al: CGFloat = 0
        UIColor(c).getRed(&r, green: &g, blue: &bl, alpha: &al)
        return (Double(r), Double(g), Double(bl))
    }
    let (ar, ag, ab) = comps(a)
    let (br, bg, bb) = comps(b)
    return Color(.sRGB,
                 red: ar * t + br * (1 - t),
                 green: ag * t + bg * (1 - t),
                 blue: ab * t + bb * (1 - t))
}

// MARK: - Accent

extension Accent {
    func color(dark: Bool) -> Color {
        switch self {
        case .amber:  return dark ? Color(oklch: 0.745, 0.125, 62)  : Color(oklch: 0.585, 0.118, 55)
        case .indigo: return dark ? Color(oklch: 0.705, 0.130, 264) : Color(oklch: 0.545, 0.135, 264)
        case .forest: return dark ? Color(oklch: 0.705, 0.120, 152) : Color(oklch: 0.515, 0.105, 150)
        case .plum:   return dark ? Color(oklch: 0.705, 0.130, 330) : Color(oklch: 0.540, 0.130, 330)
        }
    }
    /// Swatch shown in Settings (uses the light-mode value for a stable preview).
    var swatch: Color { color(dark: false) }
}

// MARK: - Palette

struct Palette {
    var bg: Color
    var surface: Color
    var surface2: Color
    var heroStripe: Color
    var text: Color
    var muted: Color
    var faint: Color
    var line: Color
    var lineStrong: Color
    var danger: Color
    var onAccent: Color
    var accent: Color
    var accentSoft: Color
    var accentLine: Color

    static func make(dark: Bool, accent: Accent) -> Palette {
        let acc = accent.color(dark: dark)
        if dark {
            let surface = Color(oklch: 0.214, 0.009, 250)
            return Palette(
                bg: Color(oklch: 0.172, 0.008, 250),
                surface: surface,
                surface2: Color(oklch: 0.258, 0.010, 250),
                heroStripe: Color(oklch: 0.305, 0.011, 250),
                text: Color(oklch: 0.945, 0.006, 80),
                muted: Color(oklch: 0.685, 0.010, 250),
                faint: Color(oklch: 0.535, 0.012, 250),
                line: Color(oklch: 0.305, 0.010, 250),
                lineStrong: Color(oklch: 0.395, 0.012, 250),
                danger: Color(oklch: 0.68, 0.15, 25),
                onAccent: Color(oklch: 0.195, 0.02, 70),
                accent: acc,
                accentSoft: mixSRGB(acc, surface, 0.13),
                accentLine: mixSRGB(acc, surface, 0.32)
            )
        } else {
            let surface = Color(oklch: 0.995, 0.004, 85)
            return Palette(
                bg: Color(oklch: 0.972, 0.006, 80),
                surface: surface,
                surface2: Color(oklch: 0.945, 0.008, 78),
                heroStripe: Color(oklch: 0.915, 0.010, 78),
                text: Color(oklch: 0.255, 0.012, 60),
                muted: Color(oklch: 0.505, 0.012, 60),
                faint: Color(oklch: 0.635, 0.010, 60),
                line: Color(oklch: 0.885, 0.008, 75),
                lineStrong: Color(oklch: 0.805, 0.010, 72),
                danger: Color(oklch: 0.56, 0.16, 25),
                onAccent: Color(oklch: 0.99, 0.008, 90),
                accent: acc,
                accentSoft: mixSRGB(acc, surface, 0.13),
                accentLine: mixSRGB(acc, surface, 0.32)
            )
        }
    }
}

// MARK: - Environment

private struct PaletteKey: EnvironmentKey {
    static let defaultValue = Palette.make(dark: false, accent: .amber)
}

extension EnvironmentValues {
    var palette: Palette {
        get { self[PaletteKey.self] }
        set { self[PaletteKey.self] = newValue }
    }
}

// MARK: - Radius + fonts

enum Radius {
    static let r: CGFloat = 16
    static let sm: CGFloat = 12
}

extension Font {
    /// Serif (New York) — item / collection names + titles.
    static func serif(_ size: CGFloat, _ weight: Font.Weight = .semibold) -> Font {
        .system(size: size, weight: weight, design: .serif)
    }
    /// Sans (SF Pro) — UI text.
    static func ui(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight)
    }
    /// Monospace (SF Mono) — specimen labels + numeric values.
    static func mono(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .monospaced)
    }
}
