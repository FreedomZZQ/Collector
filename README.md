# Collector

A lightweight, native desktop app for cataloguing personal collections. I was looking for such a program and found none that either works or look modern. This is a personal project. I have little coding experience.
Built with **Rust + Slint** and written by Claude, Opus 4.8.

---

## What's new in v0.2

| Feature | Details |
|---|---|
| **Resizable panels** | Drag the dividers between all three panels; sizes persist across sessions |
| **Light / Dark mode** | Toggle in Settings (⚙ icon); preference saved |
| **Accent colour** | 8 presets in Settings; deriveds (dim, glow, text) computed automatically |
| **Export** | Saves full JSON to `~/Documents/collector-export.json` |
| **Import** | Merges from `~/Documents/collector-export.json`; duplicates skipped by ID |

---

## Features

- Three-panel layout: Collections → Items → Detail
- Item cards with thumbnail placeholder (photo support via `rfd`)
- Fields: name, description, acquired, condition, value, tags, notes, custom fields
- Edit / View mode toggle — clean read view, inline edit when needed
- Persistent JSON storage — no database, no server, easy to back up

---

## Project Structure

```
collector/
├── Cargo.toml
├── build.rs
├── src/
│   └── main.rs          # Data model, persistence, settings, all UI callbacks
└── ui/
    └── main.slint       # Full UI: panels, drag handles, settings modal, theme
```

---

## Building

### Prerequisites

- **Rust stable** — https://rustup.rs
- **Linux extras**: `sudo apt install libxcb-shape0-dev libxkbcommon-dev libfontconfig1-dev`
- macOS / Windows: no extras needed

```bash
cd collector
cargo run              # development
cargo build --release  # → target/release/collector
```

---

## Data locations

| OS | Path |
|---|---|
| Linux | `~/.local/share/collector/` |
| macOS | `~/Library/Application Support/collector/` |
| Windows | `%APPDATA%\collector\` |

Two files are written: `data.json` (collections + items) and `settings.json` (theme, panel widths).

---

## Import / Export

Both live in **Settings → Data** and use a native file picker.

**Export** writes a portable JSON envelope — `{ app, version, exportedAt, collections: [ … ] }`
with items nested inside their collection. This is the **same shape the mobile app reads
and writes**, so a file exported on the desktop opens directly on your phone and vice‑versa.

**Import** auto-detects the file and **merges** by UUID (existing items are never
overwritten). It accepts:

- the portable envelope above (from desktop **or** mobile),
- a single exported collection, and
- the legacy desktop `AppData` shape (older backups still import).

The desktop model is "flat" (separate `collections` + `items` arrays); conversion to and
from the nested portable shape happens in `src/main.rs` (`appdata_to_portable` /
`portable_to_appdata`). Collection icons are mapped between emoji (desktop) and symbol
names (mobile); item photos and field-types are handled best-effort since the two apps
store them differently.

---

## Theme System

`Theme` is a Slint global with `in-out` properties so Rust can update every colour live.  
`apply_theme(ui, dark, accent_hex)` in `main.rs` sets all tokens in one call.

Adding a new accent preset: add the hex colour to the `accents` array in the `SettingsPanel` component in `main.slint` — the swatch row auto-expands.

Light palette tokens are in `light_palette()` in `main.rs` — easy to customise.

---

## Packaging

```bash
# Linux AppImage / .deb
cargo install cargo-bundle && cargo bundle --release

# macOS .app
cargo bundle --release   # → target/release/bundle/osx/Collector.app

# Windows: the .exe is already standalone
```
