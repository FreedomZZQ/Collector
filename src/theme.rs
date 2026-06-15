// src/theme.rs — palette and color helpers.
// Fixes from the audit: hex() validates length & ignores stray alpha safely;
// all float→u8 casts are clamped.

use iced::Color;

#[derive(Debug, Clone, Copy)]
pub struct Palette {
    pub bg_base: Color,
    pub bg_panel: Color,
    pub bg_surface: Color,
    pub bg_elevated: Color,
    pub bg_card: Color,
    pub bg_selected: Color,
    pub bg_input: Color,
    pub text_primary: Color,
    pub text_secondary: Color,
    pub text_muted: Color,
    pub border: Color,
    pub bg_hover: Color,      // subtle highlight for hovered rows/cards
    pub danger_bg: Color,
    pub danger_text: Color,
    pub accent: Color,
    pub accent_dim: Color,
    pub accent_text: Color,
    pub border_accent: Color,
}

pub fn hex(s: &str) -> Color {
    let s = s.trim().trim_start_matches('#');
    // Accept 6 (RGB) or 8 (RGBA) hex digits; anything else -> opaque black.
    let (rgb, a) = match s.len() {
        6 => (s, 255u8),
        8 => (&s[..6], u8::from_str_radix(&s[6..8], 16).unwrap_or(255)),
        _ => return Color::BLACK,
    };
    let v = u32::from_str_radix(rgb, 16).unwrap_or(0);
    Color::from_rgba8(
        ((v >> 16) & 0xff) as u8,
        ((v >> 8) & 0xff) as u8,
        (v & 0xff) as u8,
        a as f32 / 255.0,
    )
}

fn clamp_u8(v: f32) -> u8 {
    v.round().clamp(0.0, 255.0) as u8
}

fn darken(c: Color, t: f32) -> Color {
    let t = t.clamp(0.0, 1.0);
    Color::from_rgb8(
        clamp_u8(c.r * 255.0 * (1.0 - t)),
        clamp_u8(c.g * 255.0 * (1.0 - t)),
        clamp_u8(c.b * 255.0 * (1.0 - t)),
    )
}

fn lighten(c: Color, t: f32) -> Color {
    let t = t.clamp(0.0, 1.0);
    let l = |v: f32| clamp_u8(v * 255.0 + (255.0 - v * 255.0) * t);
    Color::from_rgb8(l(c.r), l(c.g), l(c.b))
}

fn with_alpha(c: Color, a: f32) -> Color {
    Color { a: a.clamp(0.0, 1.0), ..c }
}

pub fn color_to_hex(c: Color) -> String {
    format!(
        "#{:02x}{:02x}{:02x}",
        clamp_u8(c.r * 255.0),
        clamp_u8(c.g * 255.0),
        clamp_u8(c.b * 255.0)
    )
}

pub fn build_palette(dark: bool, accent_hex: &str) -> Palette {
    let (base, panel, surface, elevated, card, selected, input,
         text_primary, text_secondary, text_muted, border, hover, danger_bg, danger_text) = if dark {
        ("#0f1117", "#161b24", "#1e2535", "#252d40", "#1a2130",
         "#1d3557", "#0f1117", "#e8edf5", "#8896b0", "#4a5568", "#252d40",
         "#212a3e", "#3d1515", "#f87171")
    } else {
        ("#f4f6fb", "#ffffff", "#edf0f7", "#e2e6f0", "#f8f9fd",
         "#dce8ff", "#ffffff", "#0f1117", "#4a5568", "#9aa3b5", "#d6dcea",
         "#eef2fb", "#fff0f0", "#e53935")
    };
    let accent = hex(accent_hex);
    Palette {
        bg_base: hex(base),
        bg_panel: hex(panel),
        bg_surface: hex(surface),
        bg_elevated: hex(elevated),
        bg_card: hex(card),
        bg_selected: hex(selected),
        bg_input: hex(input),
        text_primary: hex(text_primary),
        text_secondary: hex(text_secondary),
        text_muted: hex(text_muted),
        border: hex(border),
        bg_hover: hex(hover),
        danger_bg: hex(danger_bg),
        danger_text: hex(danger_text),
        accent,
        // Mid brightness: visible but not harsh. Keeps the hue.
        accent_dim: with_alpha(accent, if dark { 0.38 } else { 0.22 }),
        accent_text: if dark { lighten(accent, 0.18) } else { darken(accent, 0.05) },
        border_accent: with_alpha(accent, 0.55),
    }
}
