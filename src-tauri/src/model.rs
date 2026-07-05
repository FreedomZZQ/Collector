// src/model.rs — data model, persistence, sorting, date helpers.
//
// Carried over from the Slint version, with fixes:
//  * Sort modes use ONE numbering everywhere (see SortMode), removing the
//    Slint/Rust off-by-one mismatch.
//  * Dates round-trip losslessly via Option parts; the sort key is explicit.
//  * Collection item-counts honor the active search where relevant.

use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use uuid::Uuid;

// ─── Core records ───────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Collection {
    pub id: String,
    pub name: String,
    pub icon: String,
    /// Monotonic insertion order, used for the "Date added" sort. Defaults to 0
    /// for older data; normalized on load so existing collections keep order.
    #[serde(default)]
    pub order: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct Item {
    pub id: String,
    pub collection_id: String,
    pub name: String,
    pub short_desc: String,
    /// photos[0] is the primary (card thumbnail + main image).
    #[serde(default)]
    pub photos: Vec<String>,
    /// Date acquired, pseudo-ISO "YYYY-MM-DD" with zero-filled unknown parts,
    /// or "" if fully unset. See date helpers below; treat purely as a sort key.
    #[serde(default)]
    pub acquired_date: String,
    pub custom_fields: Vec<CustomField>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CustomField {
    pub id: String,
    pub label: String,
    pub value: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Template {
    pub id: String,
    pub name: String,
    pub field_labels: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct AppData {
    #[serde(default)]
    pub collections: Vec<Collection>,
    #[serde(default)]
    pub items: Vec<Item>,
    #[serde(default)]
    pub templates: Vec<Template>,
}

// ─── Portable interchange format ───────────────────────────────────────────
//
// The desktop app stores a flat AppData (`collections` and `items` as sibling
// arrays). The mobile apps exchange a nested shape where each collection owns
// its items. These converters keep desktop import/export compatible with iOS
// and Android while preserving legacy AppData imports.

#[derive(Debug, Clone, Serialize, Deserialize)]
struct PortableField {
    #[serde(default)]
    label: String,
    #[serde(default)]
    value: String,
    #[serde(default = "default_field_kind")]
    kind: String,
}

fn default_field_kind() -> String {
    "text".into()
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct PortableItem {
    #[serde(default)]
    id: String,
    #[serde(default)]
    name: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    image: Option<String>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    images: Vec<String>,
    #[serde(default)]
    description: String,
    #[serde(default)]
    tags: Vec<String>,
    #[serde(default)]
    fields: Vec<PortableField>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct PortableCollection {
    #[serde(default)]
    id: String,
    #[serde(default)]
    name: String,
    #[serde(default = "default_icon_name")]
    icon: String,
    #[serde(default)]
    items: Vec<PortableItem>,
}

fn default_icon_name() -> String {
    "box".into()
}

#[derive(Debug, Deserialize)]
struct PortableEnvelopeIn {
    #[serde(default)]
    collections: Vec<PortableCollection>,
}

#[derive(Debug, Serialize)]
struct PortableEnvelopeOut {
    app: String,
    version: u32,
    #[serde(rename = "exportedAt")]
    exported_at: String,
    collections: Vec<PortableCollection>,
}

fn icon_name_to_emoji(name: &str) -> String {
    match name {
        "headphones" => return "🎧".into(),
        "pen" => return "✒".into(),
        "camera" => return "📷".into(),
        "coin" => return "🪙".into(),
        "tag" => return "🏷".into(),
        "image" => return "🖼".into(),
        "box" => return "📦".into(),
        _ => {}
    }
    if name.chars().any(|c| !c.is_ascii()) {
        name.to_string()
    } else {
        "📦".into()
    }
}

fn emoji_to_icon_name(icon: &str) -> &'static str {
    match icon {
        "🎧" => "headphones",
        "✒" | "🖊" | "🖋" | "✏" => "pen",
        "📷" | "📸" => "camera",
        "🪙" | "💰" | "💵" => "coin",
        "🏷" => "tag",
        "🖼" | "🌄" | "🌅" => "image",
        _ => "box",
    }
}

fn infer_kind(label: &str) -> String {
    let l = label.to_lowercase();
    if l.contains("value") || l.contains("price") || l.contains("cost") || l.contains("worth") {
        "value"
    } else if l.contains("condition") || l.contains("grade") {
        "condition"
    } else if l.contains("acquired")
        || l.contains("date")
        || l.contains("purchased")
        || l.contains("year")
    {
        "date"
    } else if l.contains("note") || l.contains("comment") {
        "multiline"
    } else {
        "text"
    }
    .into()
}

fn today_iso() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0);
    let days = (secs / 86_400) as i64;

    // Howard Hinnant's civil-from-days algorithm.
    let z = days + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097;
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = doy - (153 * mp + 2) / 5 + 1;
    let m = if mp < 10 { mp + 3 } else { mp - 9 };
    let y = if m <= 2 { y + 1 } else { y };
    format!("{:04}-{:02}-{:02}", y, m, d)
}

fn portable_to_appdata(cols: Vec<PortableCollection>) -> AppData {
    let mut collections = Vec::new();
    let mut items = Vec::new();

    for (order, pc) in cols.into_iter().enumerate() {
        let cid = if pc.id.is_empty() {
            Uuid::new_v4().to_string()
        } else {
            pc.id
        };
        collections.push(Collection {
            id: cid.clone(),
            name: if pc.name.is_empty() {
                "Untitled".into()
            } else {
                pc.name
            },
            icon: icon_name_to_emoji(&pc.icon),
            order: order as u64,
        });

        for pi in pc.items {
            let mut acquired_date = String::new();
            let mut custom_fields: Vec<CustomField> = pi
                .fields
                .into_iter()
                .map(|f| {
                    if acquired_date.is_empty() && f.kind == "date" {
                        acquired_date = f.value.clone();
                    }
                    CustomField {
                        id: Uuid::new_v4().to_string(),
                        label: f.label,
                        value: f.value,
                    }
                })
                .collect();

            if !pi.tags.is_empty() {
                custom_fields.push(CustomField {
                    id: Uuid::new_v4().to_string(),
                    label: "TAGS".into(),
                    value: pi.tags.join(", "),
                });
            }

            let mut photos = pi.images;
            if photos.is_empty() {
                if let Some(image) = pi.image {
                    if !image.trim().is_empty() {
                        photos.push(image);
                    }
                }
            }
            photos.retain(|p| !p.trim().is_empty());

            items.push(Item {
                id: if pi.id.is_empty() {
                    Uuid::new_v4().to_string()
                } else {
                    pi.id
                },
                collection_id: cid.clone(),
                name: pi.name,
                short_desc: pi.description,
                photos,
                acquired_date,
                custom_fields,
            });
        }
    }

    AppData {
        collections,
        items,
        templates: Vec::new(),
    }
}

fn appdata_to_portable(data: &AppData) -> Vec<PortableCollection> {
    data.collections
        .iter()
        .map(|c| {
            let items = data
                .items
                .iter()
                .filter(|i| i.collection_id == c.id)
                .map(|i| {
                    let mut tags = Vec::new();
                    let mut fields = Vec::new();

                    for f in &i.custom_fields {
                        if f.label.trim().eq_ignore_ascii_case("tags") {
                            tags = f
                                .value
                                .split(',')
                                .map(|t| t.trim().to_string())
                                .filter(|t| !t.is_empty())
                                .collect();
                        } else {
                            fields.push(PortableField {
                                label: f.label.clone(),
                                value: f.value.clone(),
                                kind: infer_kind(&f.label),
                            });
                        }
                    }

                    if !i.acquired_date.is_empty() && !fields.iter().any(|f| f.kind == "date") {
                        fields.push(PortableField {
                            label: "Acquired".into(),
                            value: i.acquired_date.clone(),
                            kind: "date".into(),
                        });
                    }

                    let images: Vec<String> = i
                        .photos
                        .iter()
                        .filter(|p| !p.trim().is_empty())
                        .cloned()
                        .collect();

                    PortableItem {
                        id: i.id.clone(),
                        name: i.name.clone(),
                        image: images.first().cloned(),
                        images,
                        description: i.short_desc.clone(),
                        tags,
                        fields,
                    }
                })
                .collect();

            PortableCollection {
                id: c.id.clone(),
                name: c.name.clone(),
                icon: emoji_to_icon_name(&c.icon).into(),
                items,
            }
        })
        .collect()
}

pub fn portable_export_json(data: &AppData) -> Result<String, serde_json::Error> {
    let envelope = PortableEnvelopeOut {
        app: "Collector".into(),
        version: 1,
        exported_at: today_iso(),
        collections: appdata_to_portable(data),
    };
    serde_json::to_string_pretty(&envelope)
}

pub fn parse_import_any(contents: &str) -> Option<AppData> {
    let value: serde_json::Value = serde_json::from_str(contents).ok()?;
    let obj = value.as_object();
    let has_top_items = obj
        .map(|o| o.get("items").map(|v| v.is_array()).unwrap_or(false))
        .unwrap_or(false);
    let has_collections = obj
        .map(|o| o.get("collections").map(|v| v.is_array()).unwrap_or(false))
        .unwrap_or(false);

    // Legacy desktop AppData has sibling top-level collections + items arrays.
    if has_top_items && has_collections {
        if let Ok(data) = serde_json::from_str::<AppData>(contents) {
            return Some(data);
        }
    }

    // Mobile/web portable envelope, and mobile on-device LibraryFile, both have
    // nested collections. Optional templates/trash are ignored by desktop.
    if let Ok(env) = serde_json::from_value::<PortableEnvelopeIn>(value.clone()) {
        if !env.collections.is_empty() {
            return Some(portable_to_appdata(env.collections));
        }
    }

    // Single bare collection export.
    if let Ok(col) = serde_json::from_value::<PortableCollection>(value) {
        if !col.name.is_empty() || !col.items.is_empty() {
            return Some(portable_to_appdata(vec![col]));
        }
    }

    None
}

// ─── Sort modes — single source of truth ────────────────────────────────────
// Same five variants for both panels; the count/date pair is panel-specific in
// LABEL only, not in numbering. This removes the old Slint(0..4)/Rust scheme
// mismatch entirely.

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum SortMode {
    Added, // insertion order
    NameAsc,
    NameDesc,
    /// collections: fewest items · items: oldest acquired
    LowOrOld,
    /// collections: most items · items: newest acquired
    HighOrNew,
}

impl Default for SortMode {
    fn default() -> Self {
        SortMode::Added
    }
}

// ─── Settings ───────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    pub dark_mode: bool,
    pub accent_hex: String,
    /// Split ratios for the resizable pane_grid (each in 0..1). `left_ratio` is
    /// the left pane's share of the whole width; `mid_ratio` is the middle
    /// pane's share of the REMAINING width after the left pane (this matches
    /// pane_grid's nested-split model: outer split = left | rest, inner split =
    /// mid | right).
    #[serde(default = "default_left_ratio")]
    pub left_ratio: f32,
    #[serde(default = "default_mid_ratio")]
    pub mid_ratio: f32,
    pub font_size: f32,
    #[serde(default)]
    pub coll_sort: SortMode,
    #[serde(default)]
    pub item_sort: SortMode,
}

pub fn default_left_ratio() -> f32 {
    0.25
}
// Mid pane's ratio is its share of the space RIGHT of the left pane. To make
// the middle pane ~25% of the whole window when left is 25%: 0.25 / 0.75 = 0.333.
pub fn default_mid_ratio() -> f32 {
    0.333
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            dark_mode: true,
            accent_hex: "#4f8ef7".into(),
            left_ratio: default_left_ratio(),
            mid_ratio: default_mid_ratio(),
            font_size: 15.0,
            coll_sort: SortMode::Added,
            item_sort: SortMode::Added,
        }
    }
}

// ─── Paths ──────────────────────────────────────────────────────────────────

pub fn app_dir() -> PathBuf {
    let mut p = dirs::data_local_dir().unwrap_or_else(|| PathBuf::from("."));
    p.push("Collectors-Notebook");
    std::fs::create_dir_all(&p).ok();
    p
}
pub fn photos_dir() -> PathBuf {
    let mut p = app_dir();
    p.push("photos");
    std::fs::create_dir_all(&p).ok();
    p
}
pub fn thumbs_dir() -> PathBuf {
    let mut p = photos_dir();
    p.push("thumbnails");
    std::fs::create_dir_all(&p).ok();
    p
}
pub fn data_path() -> PathBuf {
    let mut p = app_dir();
    p.push("data.json");
    p
}
pub fn settings_path() -> PathBuf {
    let mut p = app_dir();
    p.push("settings.json");
    p
}

// ─── Persistence ────────────────────────────────────────────────────────────

/// Loads app data, and also reports the path of a corrupt-data backup if one
/// was created on this load (so the UI can tell the user their previous data
/// couldn't be read and where the salvageable copy lives). `None` means the
/// data loaded cleanly or there was simply no file yet.
pub fn load_data_reporting() -> (AppData, Option<PathBuf>) {
    // Distinguish "no file yet" (legitimately empty first run) from "file
    // exists but won't parse" (corruption). In the corruption case we must NOT
    // silently fall back to an empty dataset, because the next save_data would
    // then overwrite the user's real (recoverable) file with nothing. Instead,
    // preserve the bad file under a timestamped .corrupt name first.
    let mut corrupt_backup: Option<PathBuf> = None;
    let mut data: AppData = match std::fs::read_to_string(data_path()) {
        Ok(s) => match serde_json::from_str(&s) {
            Ok(parsed) => parsed,
            Err(_) => {
                corrupt_backup = backup_corrupt_file(&data_path());
                AppData::default()
            }
        },
        // File missing or unreadable: normal first-run / empty state.
        Err(_) => AppData::default(),
    };
    // Migrate any legacy absolute photo paths to bare filenames so the data and
    // photos/ folder are portable across machines and user accounts.
    for item in &mut data.items {
        for p in &mut item.photos {
            if let Some(name) = std::path::Path::new(p).file_name().and_then(|n| n.to_str()) {
                if name != p {
                    *p = name.to_string();
                }
            }
        }
    }
    // Normalize collection insertion order. Old data has order=0 everywhere; if
    // we detect duplicate/zero orders, reassign by current position so the
    // "Date added" sort is stable and reflects existing order.
    let needs_order = {
        let mut seen = std::collections::HashSet::new();
        data.collections.iter().any(|c| !seen.insert(c.order)) && data.collections.len() > 1
    };
    if needs_order || data.collections.iter().all(|c| c.order == 0) {
        for (i, c) in data.collections.iter_mut().enumerate() {
            c.order = i as u64;
        }
    }
    (data, corrupt_backup)
}
pub fn save_data(data: &AppData) {
    if let Ok(json) = serde_json::to_string_pretty(data) {
        atomic_write(&data_path(), json.as_bytes());
    }
}

/// Write `bytes` to `path` atomically: write to a sibling temp file, flush, then
/// rename over the destination. A crash or power loss mid-write leaves either
/// the old file or the new file intact — never a truncated, unparseable one.
/// (rename is atomic when source and destination are on the same filesystem,
/// which they are here since the temp file sits in the same directory.)
fn atomic_write(path: &std::path::Path, bytes: &[u8]) {
    use std::io::Write;
    let tmp = path.with_extension("tmp");
    // Scope the file handle so it's closed (flushed) before the rename.
    let wrote = {
        match std::fs::File::create(&tmp) {
            Ok(mut f) => f.write_all(bytes).and_then(|_| f.sync_all()).is_ok(),
            Err(_) => false,
        }
    };
    if wrote {
        // If the rename fails, drop the temp file rather than leaving litter.
        if std::fs::rename(&tmp, path).is_err() {
            std::fs::remove_file(&tmp).ok();
        }
    } else {
        std::fs::remove_file(&tmp).ok();
    }
}

/// Preserve an unparseable data/settings file before it can be overwritten, by
/// copying it to "<name>.corrupt-<unix_secs>". Returns the backup path on
/// success. Best-effort: failures yield `None` because this runs on a path
/// that's already degraded.
fn backup_corrupt_file(path: &std::path::Path) -> Option<PathBuf> {
    let secs = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0);
    let mut backup = path.as_os_str().to_owned();
    backup.push(format!(".corrupt-{secs}"));
    let backup = PathBuf::from(backup);
    std::fs::copy(path, &backup).ok().map(|_| backup)
}

pub fn load_settings() -> Settings {
    // Settings corruption is far less costly than data corruption (the file is
    // small and regenerable), but we still preserve a bad one rather than
    // silently discarding it, for symmetry and easier debugging.
    let mut s: Settings = match std::fs::read_to_string(settings_path()) {
        Ok(text) => match serde_json::from_str(&text) {
            Ok(parsed) => parsed,
            Err(_) => {
                backup_corrupt_file(&settings_path());
                Settings::default()
            }
        },
        Err(_) => Settings::default(),
    };
    // Clamp pane ratios into the allowed range. This also self-heals older
    // settings.json files that stored wider ratios before the 0.4 cap existed.
    s.left_ratio = s.left_ratio.clamp(0.08, 0.33);
    s.mid_ratio = s.mid_ratio.clamp(0.12, 0.7);
    s
}
pub fn save_settings(s: &Settings) {
    if let Ok(json) = serde_json::to_string_pretty(s) {
        atomic_write(&settings_path(), json.as_bytes());
    }
}

// ─── Sorting ────────────────────────────────────────────────────────────────

pub fn item_count(data: &AppData, coll_id: &str) -> usize {
    data.items
        .iter()
        .filter(|i| i.collection_id == coll_id)
        .count()
}

/// Sort the underlying collections vec so stable indices stay valid.
pub fn sort_collections(data: &mut AppData, mode: SortMode) {
    match mode {
        SortMode::Added => data.collections.sort_by(|a, b| a.order.cmp(&b.order)),
        SortMode::NameAsc => data
            .collections
            .sort_by(|a, b| a.name.to_lowercase().cmp(&b.name.to_lowercase())),
        SortMode::NameDesc => data
            .collections
            .sort_by(|a, b| b.name.to_lowercase().cmp(&a.name.to_lowercase())),
        SortMode::LowOrOld | SortMode::HighOrNew => {
            let counts: std::collections::HashMap<String, usize> = data
                .collections
                .iter()
                .map(|c| (c.id.clone(), item_count(data, &c.id)))
                .collect();
            if mode == SortMode::LowOrOld {
                data.collections
                    .sort_by(|a, b| counts[&a.id].cmp(&counts[&b.id]));
            } else {
                data.collections
                    .sort_by(|a, b| counts[&b.id].cmp(&counts[&a.id]));
            }
        }
    }
}

// ─── Tests ─────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    const MOBILE_JSON: &str = r#"{
      "app": "Collector",
      "version": 1,
      "exportedAt": "2026-05-31",
      "collections": [
        {
          "id": "c1",
          "name": "Watches",
          "icon": "tag",
          "items": [
            {
              "id": "i1",
              "name": "Speedmaster",
              "image": "front.jpg",
              "images": ["front.jpg", "back.jpg"],
              "description": "Moonwatch",
              "tags": ["omega", "chrono"],
              "fields": [
                { "label": "Acquired", "value": "2026-05-31", "kind": "date" },
                { "label": "VALUE / PRICE", "value": "5000", "kind": "value" },
                { "label": "CONDITION", "value": "Mint", "kind": "condition" }
              ]
            }
          ]
        }
      ]
    }"#;

    #[test]
    fn imports_mobile_envelope() {
        let data = parse_import_any(MOBILE_JSON).expect("should parse mobile envelope");
        assert_eq!(data.collections.len(), 1);
        assert_eq!(data.collections[0].name, "Watches");
        assert_eq!(data.collections[0].icon, "🏷");
        assert_eq!(data.collections[0].order, 0);

        let item = &data.items[0];
        assert_eq!(item.collection_id, "c1");
        assert_eq!(item.short_desc, "Moonwatch");
        assert_eq!(item.photos, vec!["front.jpg", "back.jpg"]);
        assert_eq!(item.acquired_date, "2026-05-31");
        assert!(item
            .custom_fields
            .iter()
            .any(|f| f.label == "VALUE / PRICE" && f.value == "5000"));
        assert!(item
            .custom_fields
            .iter()
            .any(|f| f.label == "TAGS" && f.value == "omega, chrono"));
    }

    #[test]
    fn imports_legacy_desktop_appdata() {
        let legacy = r#"{
          "collections": [{ "id": "c1", "name": "Pens", "icon": "✒" }],
          "items": [{ "id": "i1", "collection_id": "c1", "name": "Pilot",
                      "short_desc": "", "custom_fields": [] }],
          "templates": []
        }"#;
        let data = parse_import_any(legacy).expect("should parse legacy AppData");
        assert_eq!(data.collections[0].icon, "✒");
        assert_eq!(data.items[0].name, "Pilot");
    }

    #[test]
    fn imports_single_bare_collection() {
        let bare = r#"{ "id": "c9", "name": "Solo", "icon": "camera",
                        "items": [{ "id": "x", "name": "Leica", "fields": [] }] }"#;
        let data = parse_import_any(bare).expect("should parse a bare collection");
        assert_eq!(data.collections.len(), 1);
        assert_eq!(data.collections[0].icon, "📷");
        assert_eq!(data.items[0].collection_id, "c9");
    }

    #[test]
    fn export_round_trips_back_to_mobile_shape() {
        let data = parse_import_any(MOBILE_JSON).unwrap();
        let cols = appdata_to_portable(&data);
        assert_eq!(cols.len(), 1);
        assert_eq!(cols[0].icon, "tag");

        let item = &cols[0].items[0];
        assert_eq!(item.name, "Speedmaster");
        assert_eq!(item.image.as_deref(), Some("front.jpg"));
        assert_eq!(item.images, vec!["front.jpg", "back.jpg"]);
        assert_eq!(item.description, "Moonwatch");
        assert_eq!(item.tags, vec!["omega", "chrono"]);
        assert!(!item
            .fields
            .iter()
            .any(|f| f.label.eq_ignore_ascii_case("tags")));
        assert!(item
            .fields
            .iter()
            .any(|f| f.label == "Acquired" && f.kind == "date"));
    }

    #[test]
    fn export_serializes_to_camelcase_envelope() {
        let data = parse_import_any(MOBILE_JSON).unwrap();
        let json = portable_export_json(&data).unwrap();
        assert!(json.contains("\"exportedAt\""));
        assert!(json.contains("\"app\": \"Collector\""));
        assert!(json.contains("\"collections\""));
    }
}
