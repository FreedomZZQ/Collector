# Collector 🗂  — v0.2

A lightweight, native desktop app for cataloguing personal collections.  
Built with **Rust + Slint**: single binary, no runtime, no cloud.

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

Export writes the full `AppData` JSON to `~/Documents/collector-export.json`.  
Import reads from the same path and **merges** — existing items (matched by UUID) are not overwritten, so you can safely import old backups without losing edits.

### Enabling a native file picker (Save As / Open dialog)

1. Add to `Cargo.toml`:
   ```toml
   rfd = "0.14"
   ```
2. In `src/main.rs`, find the three `// With rfd enabled, you'd use:` comments and uncomment those blocks.  
   They replace the hard-coded `~/Documents/collector-export.json` paths with native OS dialogs.

---

## Enabling Photos

Same `rfd` crate. Find `on_pick_photo` in `main.rs` and replace the stub with:

```rust
use rfd::FileDialog;
let picked = FileDialog::new()
    .add_filter("Images", &["png", "jpg", "jpeg", "webp"])
    .pick_file();
if let Some(src) = picked {
    let mut dest = app_dir();
    dest.push("photos");
    std::fs::create_dir_all(&dest).ok();
    dest.push(src.file_name().unwrap());
    std::fs::copy(&src, &dest).ok();
    // store dest path in item.thumbnail_path, then reload
}
```

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
