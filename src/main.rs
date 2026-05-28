// src/main.rs — Collector v4
#![windows_subsystem = "windows"]

slint::include_modules!();
use slint::Model;

use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::rc::Rc;
use std::cell::RefCell;
use uuid::Uuid;

// ─── Data Model ───────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Collection {
    id: String,
    name: String,
    icon: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct Item {
    id: String,
    collection_id: String,
    name: String,
    short_desc: String,
    thumbnail_path: Option<String>,
    acquired: String,
    condition: String,
    value: String,
    tags: String,
    notes: String,
    custom_fields: Vec<CustomField>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct CustomField {
    id: String,
    label: String,
    value: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Template {
    id: String,
    name: String,
    field_labels: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct AppData {
    collections: Vec<Collection>,
    items: Vec<Item>,
    #[serde(default)]
    templates: Vec<Template>,
}

// ─── Settings ─────────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Settings {
    dark_mode: bool,
    accent_hex: String,
    left_panel_width: f32,
    mid_panel_width: f32,
    font_size: f32,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            dark_mode: true,
            accent_hex: "#4f8ef7".into(),
            left_panel_width: 220.0,
            mid_panel_width: 268.0,
            font_size: 15.0,
        }
    }
}

// ─── Colour helpers ───────────────────────────────────────────────────────────

fn hex(s: &str) -> slint::Color {
    let s = s.trim_start_matches('#');
    let v = u32::from_str_radix(s, 16).unwrap_or(0);
    slint::Color::from_rgb_u8(((v>>16)&0xff) as u8, ((v>>8)&0xff) as u8, (v&0xff) as u8)
}
fn darken(c: slint::Color, t: f32) -> slint::Color {
    slint::Color::from_rgb_u8((c.red() as f32*(1.0-t)) as u8, (c.green() as f32*(1.0-t)) as u8, (c.blue() as f32*(1.0-t)) as u8)
}
fn lighten(c: slint::Color, t: f32) -> slint::Color {
    let l=|v:u8|(v as f32+(255.0-v as f32)*t) as u8;
    slint::Color::from_rgb_u8(l(c.red()),l(c.green()),l(c.blue()))
}
fn with_alpha(c: slint::Color, a: f32) -> slint::Color {
    slint::Color::from_argb_u8((a*255.0) as u8, c.red(), c.green(), c.blue())
}
fn color_to_hex(c: slint::Color) -> String {
    format!("#{:02x}{:02x}{:02x}", c.red(), c.green(), c.blue())
}

// ─── Palette ──────────────────────────────────────────────────────────────────

struct Palette {
    bg_base:slint::Color,bg_panel:slint::Color,bg_surface:slint::Color,
    bg_elevated:slint::Color,bg_card:slint::Color,bg_card_hover:slint::Color,
    bg_selected:slint::Color,bg_input:slint::Color,
    text_primary:slint::Color,text_secondary:slint::Color,text_muted:slint::Color,
    border:slint::Color,danger_bg:slint::Color,danger_text:slint::Color,
}

fn dark_palette() -> Palette { Palette {
    bg_base:hex("#0f1117"),bg_panel:hex("#161b24"),bg_surface:hex("#1e2535"),
    bg_elevated:hex("#252d40"),bg_card:hex("#1a2130"),bg_card_hover:hex("#212a3e"),
    bg_selected:hex("#1d3557"),bg_input:hex("#0f1117"),
    text_primary:hex("#e8edf5"),text_secondary:hex("#8896b0"),text_muted:hex("#4a5568"),
    border:hex("#252d40"),danger_bg:hex("#3d1515"),danger_text:hex("#f87171"),
}}

fn light_palette() -> Palette { Palette {
    bg_base:hex("#f4f6fb"),bg_panel:hex("#ffffff"),bg_surface:hex("#edf0f7"),
    bg_elevated:hex("#e2e6f0"),bg_card:hex("#f8f9fd"),bg_card_hover:hex("#eef1f8"),
    bg_selected:hex("#dce8ff"),bg_input:hex("#ffffff"),
    text_primary:hex("#0f1117"),text_secondary:hex("#4a5568"),text_muted:hex("#9aa3b5"),
    border:hex("#d6dcea"),danger_bg:hex("#fff0f0"),danger_text:hex("#e53935"),
}}

fn apply_theme(ui: &AppWindow, dark: bool, accent_hex: &str) {
    let p = if dark { dark_palette() } else { light_palette() };
    let t = ui.global::<Theme>();
    t.set_dark_mode(dark);
    t.set_bg_base(p.bg_base);t.set_bg_panel(p.bg_panel);t.set_bg_surface(p.bg_surface);
    t.set_bg_elevated(p.bg_elevated);t.set_bg_card(p.bg_card);t.set_bg_card_hover(p.bg_card_hover);
    t.set_bg_selected(p.bg_selected);t.set_bg_input(p.bg_input);
    t.set_text_primary(p.text_primary);t.set_text_secondary(p.text_secondary);t.set_text_muted(p.text_muted);
    t.set_border(p.border);t.set_danger_bg(p.danger_bg);t.set_danger_text(p.danger_text);
    let accent = hex(accent_hex);
    t.set_accent(accent);t.set_accent_dim(darken(accent,0.42));
    t.set_accent_glow(with_alpha(accent,0.18));t.set_accent_text(lighten(accent,0.25));
    t.set_border_accent(with_alpha(accent,0.32));
}

// ─── Paths ────────────────────────────────────────────────────────────────────

fn app_dir() -> PathBuf {
    let mut p = dirs::data_local_dir().unwrap_or_else(|| PathBuf::from("."));
    p.push("collector");
    std::fs::create_dir_all(&p).ok();
    p
}
fn photos_dir() -> PathBuf { let mut p=app_dir(); p.push("photos"); std::fs::create_dir_all(&p).ok(); p }
fn data_path()     -> PathBuf { let mut p=app_dir(); p.push("data.json");     p }
fn settings_path() -> PathBuf { let mut p=app_dir(); p.push("settings.json"); p }

// ─── Persistence ──────────────────────────────────────────────────────────────

fn load_data() -> AppData {
    std::fs::read_to_string(data_path()).ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_else(|| AppData {
            collections: vec![
                Collection { id: Uuid::new_v4().to_string(), name: "Headphones".into(),    icon: "🎧".into() },
                Collection { id: Uuid::new_v4().to_string(), name: "Fountain Pens".into(), icon: "✒️".into() },
            ],
            items: vec![],
            templates: vec![],
        })
}
fn save_data(data: &AppData) {
    if let Ok(json)=serde_json::to_string_pretty(data) { std::fs::write(data_path(),json).ok(); }
}
fn load_settings() -> Settings {
    std::fs::read_to_string(settings_path()).ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_default()
}
fn save_settings(s: &Settings) {
    if let Ok(json)=serde_json::to_string_pretty(s) { std::fs::write(settings_path(),json).ok(); }
}

// ─── Model helpers ────────────────────────────────────────────────────────────

fn to_slint_collections(data: &AppData) -> slint::ModelRc<CollectionData> {
    let v: Vec<CollectionData> = data.collections.iter().map(|c| {
        let count = data.items.iter().filter(|i| i.collection_id==c.id).count();
        CollectionData { id:c.id.clone().into(), name:c.name.clone().into(), icon:c.icon.clone().into(), item_count:count as i32 }
    }).collect();
    slint::ModelRc::new(slint::VecModel::from(v))
}

fn load_slint_image(path: &str) -> Option<slint::Image> {
    let img = image::open(path).ok()?.into_rgba8();
    let (w,h) = img.dimensions();
    let buf = slint::SharedPixelBuffer::<slint::Rgba8Pixel>::clone_from_slice(img.as_raw(),w,h);
    Some(slint::Image::from_rgba8(buf))
}

fn thumbnail_image(path: &str) -> slint::Image {
    if let Ok(img) = image::open(path) {
        let thumb = img.thumbnail(200,200).into_rgba8();
        let (w,h) = thumb.dimensions();
        let buf = slint::SharedPixelBuffer::<slint::Rgba8Pixel>::clone_from_slice(thumb.as_raw(),w,h);
        return slint::Image::from_rgba8(buf);
    }
    slint::Image::default()
}

fn to_slint_items(data: &AppData, coll_id: &str, search: &str) -> slint::ModelRc<ItemData> {
    let search_lc = search.to_lowercase();
    let v: Vec<ItemData> = data.items.iter()
        .filter(|i| i.collection_id == coll_id)
        .filter(|i| {
            if search_lc.is_empty() { return true; }
            i.name.to_lowercase().contains(&search_lc)
                || i.short_desc.to_lowercase().contains(&search_lc)
                || i.tags.to_lowercase().contains(&search_lc)
                || i.notes.to_lowercase().contains(&search_lc)
        })
        .map(|i| {
            let has_photo = i.thumbnail_path.as_deref().map(|p| !p.is_empty()).unwrap_or(false);
            let thumbnail = if has_photo { thumbnail_image(i.thumbnail_path.as_deref().unwrap_or("")) }
                            else { slint::Image::default() };
            ItemData {
                id: i.id.clone().into(),
                name: i.name.clone().into(),
                short_desc: i.short_desc.clone().into(),
                has_photo,
                thumbnail,
                collection_id: i.collection_id.clone().into(),
            }
        }).collect();
    slint::ModelRc::new(slint::VecModel::from(v))
}

fn to_slint_fields(item: &Item) -> slint::ModelRc<FieldData> {
    let v: Vec<FieldData> = item.custom_fields.iter().map(|f| FieldData {
        id: f.id.clone().into(), label: f.label.clone().into(), value: f.value.clone().into(),
    }).collect();
    slint::ModelRc::new(slint::VecModel::from(v))
}

fn to_slint_templates(data: &AppData) -> slint::ModelRc<TemplateData> {
    let v: Vec<TemplateData> = data.templates.iter().map(|t| TemplateData {
        id: t.id.clone().into(),
        name: t.name.clone().into(),
        field_labels: t.field_labels.join(", ").into(),
    }).collect();
    slint::ModelRc::new(slint::VecModel::from(v))
}

fn bool_model(len: usize) -> slint::ModelRc<CheckedItem> {
    let v: Vec<CheckedItem> = (0..len).map(|_| CheckedItem { checked: false }).collect();
    slint::ModelRc::new(slint::VecModel::from(v))
}

fn clear_detail(ui: &AppWindow) {
    ui.set_detail_name("".into()); ui.set_detail_desc("".into());
    ui.set_detail_notes("".into()); ui.set_detail_acquired("".into());
    ui.set_detail_condition("".into()); ui.set_detail_value("".into());
    ui.set_detail_tags("".into());
    ui.set_detail_has_photo(false);
    ui.set_detail_photo(slint::Image::default());
    ui.set_detail_fields(slint::ModelRc::new(slint::VecModel::from(vec![])));
}

fn load_detail(ui: &AppWindow, item: &Item) {
    ui.set_detail_name(item.name.clone().into());
    ui.set_detail_desc(item.short_desc.clone().into());
    ui.set_detail_notes(item.notes.clone().into());
    ui.set_detail_acquired(item.acquired.clone().into());
    ui.set_detail_condition(item.condition.clone().into());
    ui.set_detail_value(item.value.clone().into());
    ui.set_detail_tags(item.tags.clone().into());
    let has_photo = item.thumbnail_path.as_deref().map(|p| !p.is_empty()).unwrap_or(false);
    ui.set_detail_has_photo(has_photo);
    if has_photo {
        if let Some(img) = load_slint_image(item.thumbnail_path.as_deref().unwrap_or("")) {
            ui.set_detail_photo(img);
        }
    } else {
        ui.set_detail_photo(slint::Image::default());
    }
    ui.set_detail_fields(to_slint_fields(item));
}

fn flush_detail(ui: &AppWindow, item: &mut Item) {
    item.name       = ui.get_detail_name().to_string();
    item.short_desc = ui.get_detail_desc().to_string();
    item.notes      = ui.get_detail_notes().to_string();
    item.acquired   = ui.get_detail_acquired().to_string();
    item.condition  = ui.get_detail_condition().to_string();
    item.value      = ui.get_detail_value().to_string();
    item.tags       = ui.get_detail_tags().to_string();
}

fn set_status(ui: &AppWindow, msg: impl Into<slint::SharedString>) {
    ui.set_status_message(msg.into());
}

// ─── Entry Point ──────────────────────────────────────────────────────────────

fn main() {
    let app_data = load_data();
    let settings  = load_settings();

    let ui = AppWindow::new().expect("Failed to create window");
    apply_theme(&ui, settings.dark_mode, &settings.accent_hex);
    ui.set_left_width(settings.left_panel_width);
    ui.set_mid_width(settings.mid_panel_width);
    ui.global::<Theme>().set_ui_font_size(settings.font_size);
    ui.set_collections(to_slint_collections(&app_data));
    ui.set_templates(to_slint_templates(&app_data));
    ui.set_coll_checked(bool_model(app_data.collections.len()));
    ui.set_item_checked(bool_model(0));

    let data     = Rc::new(RefCell::new(app_data));
    let cfg      = Rc::new(RefCell::new(settings));
    let sel_coll = Rc::new(RefCell::new(Option::<String>::None));
    let sel_item = Rc::new(RefCell::new(Option::<String>::None));
    let search   = Rc::new(RefCell::new(String::new()));
    let icon_target_coll: Rc<RefCell<Option<usize>>> = Rc::new(RefCell::new(None));

    macro_rules! weak { () => { ui.as_weak() }; }

    // ── select-collection ─────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_select_collection(move |idx| {
            let ui = ui_w.unwrap();
            let d = data.borrow();
            // Clicking the already-selected collection deselects it
            if ui.get_selected_collection() == idx {
                *sel_coll.borrow_mut() = None;
                *sel_item.borrow_mut() = None;
                ui.set_selected_collection(-1);
                ui.set_selected_item(-1);
                ui.set_items(slint::ModelRc::new(slint::VecModel::from(vec![])));
                ui.set_item_checked(bool_model(0));
                clear_detail(&ui);
                return;
            }
            if let Some(c) = d.collections.get(idx as usize) {
                *sel_coll.borrow_mut() = Some(c.id.clone());
                *sel_item.borrow_mut() = None;
                let items = to_slint_items(&d, &c.id, &search.borrow());
                let n = items.row_count();
                ui.set_items(items);
                ui.set_item_checked(bool_model(n));
                ui.set_selected_collection(idx);
                ui.set_selected_item(-1);
                ui.set_is_editing(false);
                clear_detail(&ui);
            }
        });
    }

    // ── select-item ───────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_select_item(move |idx| {
            let ui = ui_w.unwrap();
            // Clicking selected item deselects
            if ui.get_selected_item() == idx {
                *sel_item.borrow_mut() = None;
                ui.set_selected_item(-1);
                ui.set_is_editing(false);
                clear_detail(&ui);
                return;
            }
            let d = data.borrow();
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            let search_str = search.borrow().clone();
            let search_lc = search_str.to_lowercase();
            // Index into the same filtered list that the UI shows
            let coll_items: Vec<&Item> = d.items.iter()
                .filter(|i| i.collection_id == coll_id)
                .filter(|i| {
                    if search_lc.is_empty() { return true; }
                    i.name.to_lowercase().contains(&search_lc)
                        || i.short_desc.to_lowercase().contains(&search_lc)
                        || i.tags.to_lowercase().contains(&search_lc)
                        || i.notes.to_lowercase().contains(&search_lc)
                })
                .collect();
            if let Some(item) = coll_items.get(idx as usize) {
                *sel_item.borrow_mut() = Some(item.id.clone());
                ui.set_selected_item(idx);
                ui.set_is_editing(false);
                load_detail(&ui, item);
            }
        });
    }

    // ── new-collection ────────────────────────────────────────────────────────
    {
        let (ui_w,data) = (weak!(),data.clone());
        ui.on_new_collection(move || {
            let ui = ui_w.unwrap();
            let mut d = data.borrow_mut();
            let icons = ["📁","🎧","✒️","📷","🎮","📚","⌚","💍","🎸","🎨"];
            let count = d.collections.len();
            let icon = icons[count % icons.len()].to_string();
            let name = format!("New Collection {}", count+1);
            d.collections.push(Collection { id:Uuid::new_v4().to_string(), name, icon });
            save_data(&d);
            ui.set_collections(to_slint_collections(&d));
            ui.set_coll_checked(bool_model(d.collections.len()));
        });
    }

    // ── delete-collection ─────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone());
        ui.on_delete_collection(move |idx| {
            let ui = ui_w.unwrap();
            let mut d = data.borrow_mut();
            if let Some(c) = d.collections.get(idx as usize) {
                let cid = c.id.clone();
                // Delete photos of items in this collection
                for item in d.items.iter().filter(|i| i.collection_id==cid) {
                    if let Some(p) = &item.thumbnail_path {
                        std::fs::remove_file(p).ok();
                    }
                }
                d.items.retain(|i| i.collection_id!=cid);
                d.collections.remove(idx as usize);
                *sel_coll.borrow_mut() = None;
                *sel_item.borrow_mut() = None;
                save_data(&d);
            }
            ui.set_collections(to_slint_collections(&d));
            ui.set_coll_checked(bool_model(d.collections.len()));
            ui.set_items(slint::ModelRc::new(slint::VecModel::from(vec![])));
            ui.set_item_checked(bool_model(0));
            ui.set_selected_collection(-1);
            ui.set_selected_item(-1);
            clear_detail(&ui);
        });
    }

    // ── new-item ──────────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_new_item(move || {
            let ui = ui_w.unwrap();
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            if coll_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            let new = Item { id:Uuid::new_v4().to_string(), collection_id:coll_id.clone(), name:"New Item".into(), ..Default::default() };
            let nid = new.id.clone();
            d.items.push(new);
            save_data(&d);
            let items = to_slint_items(&d, &coll_id, &search.borrow());
            let n = items.row_count();
            let idx = items.iter().position(|i| i.id.as_str()==nid).unwrap_or(0) as i32;
            ui.set_items(items);
            ui.set_item_checked(bool_model(n));
            *sel_item.borrow_mut() = Some(nid);
            ui.set_selected_item(idx);
            ui.set_is_editing(true);
            clear_detail(&ui);
            ui.set_detail_name("New Item".into());
            ui.set_collections(to_slint_collections(&d));
        });
    }

    // ── delete-item ───────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_delete_item(move |_| {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            {
                let mut d = data.borrow_mut();
                // Delete photo file from disk
                if let Some(item) = d.items.iter().find(|i| i.id==item_id) {
                    if let Some(p) = &item.thumbnail_path {
                        std::fs::remove_file(p).ok();
                    }
                }
                d.items.retain(|i| i.id!=item_id);
                save_data(&d);
            }
            *sel_item.borrow_mut() = None;
            let d = data.borrow();
            let items = to_slint_items(&d, &coll_id, &search.borrow());
            let n = items.row_count();
            ui.set_items(items);
            ui.set_item_checked(bool_model(n));
            ui.set_selected_item(-1);
            ui.set_is_editing(false);
            clear_detail(&ui);
            ui.set_collections(to_slint_collections(&d));
        });
    }

    // ── delete-selected-items ─────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_delete_selected_items(move || {
            let ui = ui_w.unwrap();
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            if coll_id.is_empty() { return; }
            let checked = ui.get_item_checked();
            let mut d = data.borrow_mut();
            let coll_items: Vec<String> = d.items.iter()
                .filter(|i| i.collection_id==coll_id)
                .map(|i| i.id.clone())
                .collect();
            let to_delete: Vec<String> = coll_items.iter().enumerate()
                .filter(|(idx,_)| checked.row_data(*idx).map(|c| c.checked).unwrap_or(false))
                .map(|(_,id)| id.clone())
                .collect();
            for id in &to_delete {
                if let Some(item) = d.items.iter().find(|i| &i.id==id) {
                    if let Some(p) = &item.thumbnail_path { std::fs::remove_file(p).ok(); }
                }
            }
            d.items.retain(|i| !to_delete.contains(&i.id));
            save_data(&d);
            *sel_item.borrow_mut() = None;
            let items = to_slint_items(&d, &coll_id, &search.borrow());
            let n = items.row_count();
            ui.set_items(items);
            ui.set_item_checked(bool_model(n));
            ui.set_selected_item(-1);
            clear_detail(&ui);
            ui.set_collections(to_slint_collections(&d));
            set_status(&ui, format!("Deleted {} item(s).", to_delete.len()));
        });
    }

    // ── toggle-edit / save ────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_toggle_edit(move || {
            let ui = ui_w.unwrap();
            if ui.get_is_editing() {
                let item_id = sel_item.borrow().clone().unwrap_or_default();
                let coll_id = sel_coll.borrow().clone().unwrap_or_default();
                if item_id.is_empty() { ui.set_is_editing(false); return; }
                let mut d = data.borrow_mut();
                if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                    flush_detail(&ui, item);
                }
                save_data(&d);
                ui.set_items(to_slint_items(&d, &coll_id, &search.borrow()));
                ui.set_is_editing(false);
                set_status(&ui, "Saved.");
            } else {
                ui.set_is_editing(true);
                ui.set_status_message("".into());
            }
        });
    }

    // ── field-changed ─────────────────────────────────────────────────────────
    ui.on_field_changed(|_, _| {});

    // ── pick-photo ────────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_item,search,sel_coll) = (weak!(),data.clone(),sel_item.clone(),search.clone(),sel_coll.clone());
        ui.on_pick_photo(move || {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let picked = rfd::FileDialog::new()
                .set_title("Choose a photo")
                .add_filter("Images", &["png","jpg","jpeg","webp","gif"])
                .pick_file();
            if let Some(src_path) = picked {
                let ext = src_path.extension().and_then(|e| e.to_str()).unwrap_or("jpg");
                let dest = photos_dir().join(format!("{}.{}", Uuid::new_v4(), ext));
                match std::fs::copy(&src_path, &dest) {
                    Ok(_) => {
                        let path_str = dest.to_string_lossy().to_string();
                        let coll_id = {
                            let mut d = data.borrow_mut();
                            let cid = d.items.iter().find(|i| i.id==item_id)
                                .map(|i| i.collection_id.clone()).unwrap_or_default();
                            // Remove old photo file if there was one
                            if let Some(item) = d.items.iter().find(|i| i.id==item_id) {
                                if let Some(old) = &item.thumbnail_path {
                                    if !old.is_empty() { std::fs::remove_file(old).ok(); }
                                }
                            }
                            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                                item.thumbnail_path = Some(path_str.clone());
                            }
                            save_data(&d);
                            cid
                        };
                        ui.set_detail_has_photo(true);
                        if let Some(img) = load_slint_image(&path_str) { ui.set_detail_photo(img); }
                        let d2 = data.borrow();
                        ui.set_items(to_slint_items(&d2, &coll_id, &search.borrow()));
                        set_status(&ui, "Photo updated.");
                    }
                    Err(e) => set_status(&ui, format!("Could not copy photo: {e}")),
                }
            }
        });
    }

    // ── add-custom-field ──────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_add_custom_field(move || {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                item.custom_fields.push(CustomField { id:Uuid::new_v4().to_string(), label:"NEW FIELD".into(), value:"".into() });
                let fields = to_slint_fields(item);
                save_data(&d);
                ui.set_detail_fields(fields);
            }
        });
    }

    // ── delete-custom-field ───────────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_delete_custom_field(move |field_id| {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                item.custom_fields.retain(|f| f.id!=field_id.as_str());
                let fields = to_slint_fields(item);
                save_data(&d);
                ui.set_detail_fields(fields);
            }
        });
    }

    // ── custom-field-label-changed ────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_custom_field_label_changed(move |field_id, new_label| {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                if let Some(field) = item.custom_fields.iter_mut().find(|f| f.id==field_id.as_str()) {
                    field.label = new_label.to_string().to_uppercase();
                }
                let fields = to_slint_fields(item);
                save_data(&d);
                ui.set_detail_fields(fields);
            }
        });
    }

    // ── custom-field-value-changed ────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_custom_field_value_changed(move |field_id, new_value| {
            let _ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                if let Some(field) = item.custom_fields.iter_mut().find(|f| f.id==field_id.as_str()) {
                    field.value = new_value.to_string();
                }
                save_data(&d);
            }
        });
    }

    // ── save-as-template ──────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_save_as_template(move || {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            let labels: Vec<String> = d.items.iter()
                .find(|i| i.id==item_id)
                .map(|i| i.custom_fields.iter().map(|f| f.label.clone()).collect())
                .unwrap_or_default();
            if labels.is_empty() { set_status(&ui, "No custom fields to save as template."); return; }
            let name = format!("Template {}", d.templates.len()+1);
            d.templates.push(Template { id:Uuid::new_v4().to_string(), name, field_labels: labels });
            save_data(&d);
            let tmpl = to_slint_templates(&d);
            ui.set_templates(tmpl);
            set_status(&ui, "Template saved.");
        });
    }

    // ── apply-template ────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_item) = (weak!(),data.clone(),sel_item.clone());
        ui.on_apply_template(move |tmpl_id| {
            let ui = ui_w.unwrap();
            let item_id = sel_item.borrow().clone().unwrap_or_default();
            if item_id.is_empty() { return; }
            let mut d = data.borrow_mut();
            let labels: Vec<String> = d.templates.iter()
                .find(|t| t.id==tmpl_id.as_str())
                .map(|t| t.field_labels.clone())
                .unwrap_or_default();
            if let Some(item) = d.items.iter_mut().find(|i| i.id==item_id) {
                for label in labels {
                    item.custom_fields.push(CustomField { id:Uuid::new_v4().to_string(), label, value:"".into() });
                }
                let fields = to_slint_fields(item);
                save_data(&d);
                ui.set_detail_fields(fields);
            }
        });
    }

    // ── delete-template ───────────────────────────────────────────────────────
    {
        let (ui_w,data) = (weak!(),data.clone());
        ui.on_delete_template(move |tmpl_id| {
            let ui = ui_w.unwrap();
            let mut d = data.borrow_mut();
            d.templates.retain(|t| t.id!=tmpl_id.as_str());
            save_data(&d);
            ui.set_templates(to_slint_templates(&d));
        });
    }

    // ── search-changed ────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item,search) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone(),search.clone());
        ui.on_search_changed(move |q| {
            let ui = ui_w.unwrap();
            *search.borrow_mut() = q.to_string();
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            if coll_id.is_empty() { return; }
            let d = data.borrow();
            let items = to_slint_items(&d, &coll_id, &q);
            let n = items.row_count();
            ui.set_items(items);
            ui.set_item_checked(bool_model(n));
            *sel_item.borrow_mut() = None;
            ui.set_selected_item(-1);
            clear_detail(&ui);
        });
    }

    // ── collection-icon-clicked ───────────────────────────────────────────────
    {
        let (ui_w,icon_target_coll) = (weak!(),icon_target_coll.clone());
        ui.on_collection_icon_clicked(move |idx| {
            let ui = ui_w.unwrap();
            *icon_target_coll.borrow_mut() = Some(idx as usize);
            ui.set_show_icon_picker(true);
        });
    }

    // ── icon-picked ───────────────────────────────────────────────────────────
    {
        let (ui_w,data,icon_target_coll) = (weak!(),data.clone(),icon_target_coll.clone());
        ui.on_icon_picked(move |ico| {
            let ui = ui_w.unwrap();
            let idx_opt = *icon_target_coll.borrow();
            if let Some(idx) = idx_opt {
                let mut d = data.borrow_mut();
                if let Some(c) = d.collections.get_mut(idx) {
                    c.icon = ico.to_string();
                }
                save_data(&d);
                ui.set_collections(to_slint_collections(&d));
            }
        });
    }

    // ── toggle-multi-select ───────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll) = (weak!(),data.clone(),sel_coll.clone());
        ui.on_toggle_multi_select(move || {
            let ui = ui_w.unwrap();
            let new_mode = !ui.get_multi_select_mode();
            ui.set_multi_select_mode(new_mode);
            // Reset all checkboxes
            let d = data.borrow();
            ui.set_coll_checked(bool_model(d.collections.len()));
            let coll_id = sel_coll.borrow().clone().unwrap_or_default();
            let n = d.items.iter().filter(|i| i.collection_id==coll_id).count();
            ui.set_item_checked(bool_model(n));
        });
    }

    // ── toggle-coll-check ─────────────────────────────────────────────────────
    {
        let ui_w = weak!();
        ui.on_toggle_coll_check(move |idx| {
            let ui = ui_w.unwrap();
            let checked = ui.get_coll_checked();
            if let Some(row) = checked.row_data(idx as usize) {
                checked.set_row_data(idx as usize, CheckedItem { checked: !row.checked });
            }
        });
    }

    // ── toggle-item-check ─────────────────────────────────────────────────────
    {
        let ui_w = weak!();
        ui.on_toggle_item_check(move |idx| {
            let ui = ui_w.unwrap();
            let checked = ui.get_item_checked();
            if let Some(row) = checked.row_data(idx as usize) {
                checked.set_row_data(idx as usize, CheckedItem { checked: !row.checked });
            }
        });
    }

    // ── Panel resize ──────────────────────────────────────────────────────────
    {
        let cfg = cfg.clone();
        ui.on_resize_left(move |w| {
            cfg.borrow_mut().left_panel_width = w;
            let s = cfg.borrow().clone();
            save_settings(&s);
        });
    }
    {
        let cfg = cfg.clone();
        ui.on_resize_mid(move |w| {
            cfg.borrow_mut().mid_panel_width = w;
            let s = cfg.borrow().clone();
            save_settings(&s);
        });
    }

    // ── Toggle dark/light ─────────────────────────────────────────────────────
    {
        let (ui_w,cfg) = (weak!(),cfg.clone());
        ui.on_toggle_dark_mode(move || {
            let ui = ui_w.unwrap();
            let mut s = cfg.borrow_mut();
            s.dark_mode = !s.dark_mode;
            let accent = s.accent_hex.clone();
            apply_theme(&ui, s.dark_mode, &accent);
            save_settings(&s);
        });
    }

    // ── Set accent ────────────────────────────────────────────────────────────
    {
        let (ui_w,cfg) = (weak!(),cfg.clone());
        ui.on_set_accent(move |c| {
            let ui = ui_w.unwrap();
            let mut s = cfg.borrow_mut();
            s.accent_hex = color_to_hex(c);
            let dark = s.dark_mode;
            let accent = s.accent_hex.clone();
            apply_theme(&ui, dark, &accent);
            save_settings(&s);
        });
    }

    // ── Set font size ─────────────────────────────────────────────────────────
    {
        let (ui_w,cfg) = (weak!(),cfg.clone());
        ui.on_set_font_size(move |size| {
            let ui = ui_w.unwrap();
            ui.global::<Theme>().set_ui_font_size(size);
            let mut s = cfg.borrow_mut();
            s.font_size = size;
            save_settings(&s);
        });
    }

    // ── Export ────────────────────────────────────────────────────────────────
    {
        let (ui_w,data) = (weak!(),data.clone());
        ui.on_export_data(move || {
            let ui = ui_w.unwrap();
            let default_path = dirs::document_dir()
                .or_else(|| dirs::home_dir())
                .unwrap_or_else(|| PathBuf::from("."))
                .join("collector-export.json");
            let path = rfd::FileDialog::new()
                .set_title("Export collection data")
                .set_file_name("collector-export.json")
                .add_filter("JSON",&["json"])
                .save_file()
                .unwrap_or(default_path);
            let d = data.borrow();
            match serde_json::to_string_pretty(&*d) {
                Ok(json) => match std::fs::write(&path,json) {
                    Ok(_)  => set_status(&ui, format!("Exported to {}", path.display())),
                    Err(e) => set_status(&ui, format!("Export failed: {e}")),
                },
                Err(e) => set_status(&ui, format!("Serialise error: {e}")),
            }
        });
    }

    // ── Import ────────────────────────────────────────────────────────────────
    {
        let (ui_w,data,sel_coll,sel_item) = (weak!(),data.clone(),sel_coll.clone(),sel_item.clone());
        ui.on_import_data(move || {
            let ui = ui_w.unwrap();
            let picked = rfd::FileDialog::new()
                .set_title("Import collection data")
                .add_filter("JSON",&["json"])
                .pick_file();
            let path = match picked { Some(p) => p, None => { set_status(&ui,"Import cancelled."); return; } };
            match std::fs::read_to_string(&path) {
                Ok(contents) => match serde_json::from_str::<AppData>(&contents) {
                    Ok(imported) => {
                        let mut d = data.borrow_mut();
                        let ex_colls: std::collections::HashSet<_> = d.collections.iter().map(|c| c.id.clone()).collect();
                        let ex_items: std::collections::HashSet<_> = d.items.iter().map(|i| i.id.clone()).collect();
                        let (mut ac,mut ai) = (0usize,0usize);
                        for c in imported.collections { if !ex_colls.contains(&c.id) { d.collections.push(c); ac+=1; } }
                        for i in imported.items { if !ex_items.contains(&i.id) { d.items.push(i); ai+=1; } }
                        save_data(&d);
                        *sel_coll.borrow_mut() = None;
                        *sel_item.borrow_mut() = None;
                        ui.set_collections(to_slint_collections(&d));
                        ui.set_coll_checked(bool_model(d.collections.len()));
                        ui.set_items(slint::ModelRc::new(slint::VecModel::from(vec![])));
                        ui.set_item_checked(bool_model(0));
                        ui.set_selected_collection(-1);
                        ui.set_selected_item(-1);
                        clear_detail(&ui);
                        set_status(&ui, format!("Imported {ac} collection(s), {ai} item(s)."));
                    }
                    Err(e) => set_status(&ui, format!("Parse error: {e}")),
                },
                Err(e) => set_status(&ui, format!("Could not read file: {e}")),
            }
        });
    }

    ui.run().expect("Event loop failed");
}
