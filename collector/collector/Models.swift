//
//  Models.swift
//  collector
//
//  Domain model for the Collector library. Codable shapes mirror the design
//  bundle's JSON so import/export round-trips with the web prototype and the
//  desktop app.
//
//  NOTE: the "collection" concept is modelled as `ItemCollection` to avoid
//  clashing with Swift's standard-library `Collection` protocol. JSON keys are
//  derived from property names, so the on-disk shape is unaffected.
//

import Foundation

// MARK: - Field

/// A dynamic field on an item. Field kinds drive formatting + highlight slots.
enum FieldKind: String, Codable, CaseIterable, Hashable {
    case text, date, value, condition, multiline

    /// Human label used in the editor's "type" chip.
    var label: String {
        switch self {
        case .text: return "Text"
        case .date: return "Date"
        case .value: return "Value"
        case .condition: return "Condition"
        case .multiline: return "Note"
        }
    }
}

/// A single `{ label, value, kind }` field. `id` is local-only (not encoded) so
/// SwiftUI has stable identity; the design JSON has no field ids.
struct Field: Codable, Identifiable, Hashable {
    var id: String
    var label: String
    var value: String
    var kind: FieldKind

    init(id: String = UUID().uuidString, label: String, value: String = "", kind: FieldKind = .text) {
        self.id = id
        self.label = label
        self.value = value
        self.kind = kind
    }

    private enum CodingKeys: String, CodingKey { case label, value, kind }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = UUID().uuidString
        label = try c.decodeIfPresent(String.self, forKey: .label) ?? ""
        value = try c.decodeIfPresent(String.self, forKey: .value) ?? ""
        kind = (try? c.decode(FieldKind.self, forKey: .kind)) ?? .text
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(label, forKey: .label)
        try c.encode(value, forKey: .value)
        try c.encode(kind, forKey: .kind)
    }
}

// MARK: - Item

struct Item: Codable, Identifiable, Hashable {
    var id: String
    var name: String
    /// Photo file names inside the app's image store; `images.first` is the cover.
    var images: [String]
    var description: String
    var tags: [String]
    var fields: [Field]

    init(id: String = UUID().uuidString,
         name: String = "",
         images: [String] = [],
         description: String = "",
         tags: [String] = [],
         fields: [Field] = []) {
        self.id = id
        self.name = name
        self.images = images
        self.description = description
        self.tags = tags
        self.fields = fields
    }

    private enum CodingKeys: String, CodingKey { case id, name, image, images, description, tags, fields }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        // `images` is this app's multi-photo shape; older files carry a single `image`.
        let single = try c.decodeIfPresent(String.self, forKey: .image)
        images = try c.decodeIfPresent([String].self, forKey: .images) ?? single.map { [$0] } ?? []
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        tags = try c.decodeIfPresent([String].self, forKey: .tags) ?? []
        fields = try c.decodeIfPresent([Field].self, forKey: .fields) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(name, forKey: .name)
        // Keep the single-`image` key so the desktop/web apps still parse the file.
        try c.encodeIfPresent(images.first, forKey: .image)
        if !images.isEmpty { try c.encode(images, forKey: .images) }
        try c.encode(description, forKey: .description)
        try c.encode(tags, forKey: .tags)
        try c.encode(fields, forKey: .fields)
    }
}

extension Item {
    /// Fields a brand-new item starts with. Mirrors the desktop app's defaults and
    /// the design's "Basic" template, so a new item is immediately usable (and its
    /// Value counts toward the estimated total).
    static func defaultFields() -> [Field] {
        [
            Field(label: "Acquired", kind: .date),
            Field(label: "Condition", kind: .condition),
            Field(label: "Value", kind: .value),
            Field(label: "Notes", kind: .multiline),
        ]
    }
}

// MARK: - Collection

struct ItemCollection: Codable, Identifiable, Hashable {
    var id: String
    var name: String
    var icon: String
    var items: [Item]

    init(id: String = UUID().uuidString,
         name: String,
         icon: String = "box",
         items: [Item] = []) {
        self.id = id
        self.name = name
        self.icon = icon
        self.items = items
    }

    private enum CodingKeys: String, CodingKey { case id, name, icon, items }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? "Untitled"
        icon = try c.decodeIfPresent(String.self, forKey: .icon) ?? "box"
        items = try c.decodeIfPresent([Item].self, forKey: .items) ?? []
    }
}

// MARK: - Template

struct TemplateField: Codable, Hashable {
    var label: String
    var kind: FieldKind
}

struct Template: Codable, Identifiable, Hashable {
    var id: String
    var name: String
    var fields: [TemplateField]

    init(id: String = UUID().uuidString, name: String, fields: [TemplateField]) {
        self.id = id
        self.name = name
        self.fields = fields
    }
}

// MARK: - Trash (recycle bin)

/// A soft-deleted item, remembering where it came from so it can be restored.
struct TrashedItem: Codable, Identifiable, Hashable {
    var item: Item
    var collectionID: String
    var collectionName: String
    var collectionIcon: String
    var deletedAt: Date
    var id: String { item.id }
}

/// A soft-deleted collection (with all its items).
struct TrashedCollection: Codable, Identifiable, Hashable {
    var collection: ItemCollection
    var deletedAt: Date
    var id: String { collection.id }
}

struct Trash: Codable, Hashable {
    var items: [TrashedItem]
    var collections: [TrashedCollection]

    init(items: [TrashedItem] = [], collections: [TrashedCollection] = []) {
        self.items = items
        self.collections = collections
    }

    var isEmpty: Bool { items.isEmpty && collections.isEmpty }
    var count: Int { items.count + collections.count }

    enum CodingKeys: String, CodingKey { case items, collections }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        items = try c.decodeIfPresent([TrashedItem].self, forKey: .items) ?? []
        collections = try c.decodeIfPresent([TrashedCollection].self, forKey: .collections) ?? []
    }
}

// MARK: - Persistence + Export containers

/// On-device persistence file: collections + templates + trash.
struct LibraryFile: Codable {
    var collections: [ItemCollection]
    var templates: [Template]
    var trash: Trash

    init(collections: [ItemCollection], templates: [Template], trash: Trash = Trash()) {
        self.collections = collections
        self.templates = templates
        self.trash = trash
    }

    enum CodingKeys: String, CodingKey { case collections, templates, trash }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        collections = try c.decodeIfPresent([ItemCollection].self, forKey: .collections) ?? []
        templates = try c.decodeIfPresent([Template].self, forKey: .templates) ?? []
        trash = try c.decodeIfPresent(Trash.self, forKey: .trash) ?? Trash()
    }
}

/// Portable export envelope. Extends the design's shape with an optional `trash`
/// so the recycle bin round-trips; older files (no `trash`) still import.
struct LibraryExport: Codable {
    var app: String
    var version: Int
    var exportedAt: String
    var collections: [ItemCollection]
    var trash: Trash

    init(collections: [ItemCollection], trash: Trash = Trash()) {
        self.app = "Collector"
        self.version = 1
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        self.exportedAt = f.string(from: Date())
        self.collections = collections
        self.trash = trash
    }

    enum CodingKeys: String, CodingKey { case app, version, exportedAt, collections, trash }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        app = try c.decodeIfPresent(String.self, forKey: .app) ?? "Collector"
        version = try c.decodeIfPresent(Int.self, forKey: .version) ?? 1
        exportedAt = try c.decodeIfPresent(String.self, forKey: .exportedAt) ?? ""
        collections = try c.decodeIfPresent([ItemCollection].self, forKey: .collections) ?? []
        trash = try c.decodeIfPresent(Trash.self, forKey: .trash) ?? Trash()
    }
}
