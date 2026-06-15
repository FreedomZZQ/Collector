// src/image_util.rs — thumbnail generation + loading into iced handles.
//
// PORTABILITY: photos are stored in the data file as bare *filenames*
// (e.g. "3f9c…e1.jpg"), NOT absolute paths. They are resolved against
// `photos_dir()` at access time via `resolve_photo`. This means the data file
// and the photos/ folder can be copied to any machine or user account and still
// work. Legacy data that stored absolute paths is still handled: if the stored
// string looks like an absolute path that exists, it's used as-is; otherwise we
// fall back to photos_dir()/<file name component>.

use crate::model::{photos_dir, thumbs_dir};
use std::path::{Path, PathBuf};
use uuid::Uuid;

/// Resolve a stored photo reference (normally a bare filename) to a concrete
/// path on this machine. Tolerates legacy absolute paths.
pub fn resolve_photo(stored: &str) -> PathBuf {
    if stored.is_empty() {
        return PathBuf::new();
    }
    let p = Path::new(stored);
    // Legacy absolute path that still exists on this machine: honor it.
    if p.is_absolute() && p.exists() {
        return p.to_path_buf();
    }
    // Otherwise treat the last path component as a filename inside photos_dir.
    let name = p.file_name().unwrap_or(p.as_os_str());
    photos_dir().join(name)
}

/// The thumbnail filename for a stored photo reference: "<stem>.jpg" inside the
/// thumbnails dir. Keyed off the file stem so it's also portable.
pub fn thumb_path_for(stored: &str) -> PathBuf {
    let stem = Path::new(stored)
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("thumb");
    thumbs_dir().join(format!("{stem}.jpg"))
}

pub fn generate_thumbnail(stored: &str) {
    if stored.is_empty() { return; }
    let src = resolve_photo(stored);
    if let Ok(img) = image::open(&src) {
        let thumb = img.resize(250, 250, image::imageops::FilterType::Lanczos3);
        let dest = thumb_path_for(stored);
        thumb.to_rgb8().save_with_format(&dest, image::ImageFormat::Jpeg).ok();
    }
}

pub fn delete_photo_files(stored: &str) {
    if stored.is_empty() { return; }
    std::fs::remove_file(resolve_photo(stored)).ok();
    std::fs::remove_file(thumb_path_for(stored)).ok();
}

/// Copy an existing stored photo to a new file in photos_dir, thumbnail it, and
/// return the new bare filename (used by duplicate).
pub fn copy_photo_file(stored: &str) -> Option<String> {
    if stored.is_empty() { return None; }
    let src = resolve_photo(stored);
    if !src.exists() { return None; }
    let ext = src.extension().and_then(|e| e.to_str()).unwrap_or("jpg");
    let name = format!("{}.{}", Uuid::new_v4(), ext);
    let dest = photos_dir().join(&name);
    std::fs::copy(&src, &dest).ok()?;
    generate_thumbnail(&name);
    Some(name)
}

/// Import a chosen file from anywhere on disk: copy it into photos_dir under a
/// fresh name, thumbnail it, and return the new bare filename.
pub fn import_picked_photo(src: &Path) -> Option<String> {
    let ext = src.extension().and_then(|e| e.to_str()).unwrap_or("jpg");
    let name = format!("{}.{}", Uuid::new_v4(), ext);
    let dest = photos_dir().join(&name);
    std::fs::copy(src, &dest).ok()?;
    generate_thumbnail(&name);
    Some(name)
}

fn decode_to_handle(path: &Path, max: u32) -> Option<iced::widget::image::Handle> {
    let img = image::open(path).ok()?;
    let scaled = if max > 0 {
        img.resize(max, max, image::imageops::FilterType::Lanczos3)
    } else {
        img
    };
    let rgba = scaled.into_rgba8();
    let (w, h) = rgba.dimensions();
    Some(iced::widget::image::Handle::from_rgba(w, h, rgba.into_raw()))
}

/// Thumbnail handle for cards / 140px detail image. Prefers the cached jpg.
pub fn thumbnail_handle(stored: &str) -> Option<iced::widget::image::Handle> {
    if stored.is_empty() { return None; }
    let thumb = thumb_path_for(stored);
    if !thumb.exists() {
        generate_thumbnail(stored);
    }
    if let Some(h) = decode_to_handle(&thumb, 0) {
        return Some(h);
    }
    // Fallback: decode the resolved original directly, scaled.
    decode_to_handle(&resolve_photo(stored), 250)
}

/// Full-resolution handle for the lightbox.
pub fn full_handle(stored: &str) -> Option<iced::widget::image::Handle> {
    if stored.is_empty() { return None; }
    decode_to_handle(&resolve_photo(stored), 0)
}
