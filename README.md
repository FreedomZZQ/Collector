# Collector

A lightweight, native desktop app for cataloguing personal collections. I was looking for such a program and found none that either works or look modern. This is a personal project. I have little coding experience.
Built with **Rust + Slint** and written by Claude, Opus 4.8.

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

---

## Packaging

```bash
# Linux AppImage / .deb
cargo install cargo-bundle && cargo bundle --release

# macOS .app
cargo bundle --release   # → target/release/bundle/osx/Collector.app

# Windows: the .exe is already standalone
```

---

## Running the downloads

- Windows: run collector-windows-x86_64.exe. SmartScreen may warn on first launch (unsigned) — click "More info" then "Run anyway".
- macOS: on first launch Gatekeeper blocks unsigned apps — right-click the file → Open → confirm (double-clicking won't show the Open option). After that it runs normally.
- Linux: run the file

---

## Not in the plans

- UI translation.
    + The UI is easy enough to navigate without much dictionary look-ups. The field labels can be renamed into other languages. The only problem is I've encountered Chinese, Japanese, and Korean rendering, which will be packaged on a per-case basis.
- Migration to SQLite or other database structure vs JSON.
    + For a personal collection software (mainly for my own use), JSON works fine. Unless you own a museum with 1000+ articles, or you have... a problem (in which case you need a doctor, not a program)
