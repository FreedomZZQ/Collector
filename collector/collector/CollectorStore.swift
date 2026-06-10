//
//  CollectorStore.swift
//  collector
//
//  Single source of truth. Owns collections + templates + trash and persists
//  them to a single JSON file in Documents (mirrors the desktop app's local-first
//  design). Deletes are "soft" — they move into the recycle bin first.
//

import Foundation
import Combine

enum ImportStrategy { case merge, replace }

/// Result of parsing an import file (collections + recycle bin).
struct ParsedImport {
    var collections: [ItemCollection]
    var trash: Trash
}

/// Not `@MainActor`-annotated so it can be created in a `@StateObject` initializer;
/// all mutations originate from SwiftUI views on the main thread.
final class CollectorStore: ObservableObject {
    @Published private(set) var collections: [ItemCollection] = []
    @Published private(set) var templates: [Template] = []
    @Published private(set) var trash: Trash = Trash()

    private let fileURL: URL = {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return docs.appendingPathComponent("collector-library.json")
    }()

    init() {
        load()
        // Sweep photo files orphaned by a previous run (e.g. the app was killed
        // while the item editor had unsaved photos).
        pruneImages()
    }

    // MARK: - JSON config

    static func makeEncoder() -> JSONEncoder {
        let e = JSONEncoder()
        e.outputFormatting = [.prettyPrinted, .withoutEscapingSlashes]
        e.dateEncodingStrategy = .iso8601
        return e
    }
    static func makeDecoder() -> JSONDecoder {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }

    // MARK: - Derived

    func collection(_ id: String) -> ItemCollection? {
        collections.first { $0.id == id }
    }

    func item(collectionID: String, itemID: String) -> Item? {
        collection(collectionID)?.items.first { $0.id == itemID }
    }

    var totalItems: Int { collections.reduce(0) { $0 + $1.items.count } }
    var totalValue: Double { collections.reduce(0) { $0 + collectionValue($1) } }
    var trashCount: Int { trash.count }

    // MARK: - Collection mutations

    @discardableResult
    func addCollection(name: String, icon: String) -> String {
        let col = ItemCollection(name: name, icon: icon, items: [])
        collections.append(col)
        save()
        return col.id
    }

    func renameCollection(_ id: String, name: String) {
        guard let i = collections.firstIndex(where: { $0.id == id }) else { return }
        collections[i].name = name
        save()
    }

    func setIcon(_ id: String, icon: String) {
        guard let i = collections.firstIndex(where: { $0.id == id }) else { return }
        collections[i].icon = icon
        save()
    }

    /// Soft-delete: move the collection (with its items) into the recycle bin.
    func deleteCollection(_ id: String) {
        guard let idx = collections.firstIndex(where: { $0.id == id }) else { return }
        let col = collections.remove(at: idx)
        trash.collections.insert(TrashedCollection(collection: col, deletedAt: Date()), at: 0)
        save()
    }

    // MARK: - Item mutations

    /// Insert-or-update an item within a collection.
    func saveItem(collectionID: String, item: Item) {
        guard let ci = collections.firstIndex(where: { $0.id == collectionID }) else { return }
        if let ii = collections[ci].items.firstIndex(where: { $0.id == item.id }) {
            collections[ci].items[ii] = item
        } else {
            collections[ci].items.append(item)
        }
        save()
        pruneImages() // drops files for photos removed during the edit
    }

    /// Soft-delete: move the item into the recycle bin.
    func deleteItem(collectionID: String, itemID: String) {
        guard let ci = collections.firstIndex(where: { $0.id == collectionID }),
              let ii = collections[ci].items.firstIndex(where: { $0.id == itemID }) else { return }
        let removed = collections[ci].items.remove(at: ii)
        let col = collections[ci]
        trash.items.insert(
            TrashedItem(item: removed, collectionID: col.id, collectionName: col.name,
                        collectionIcon: col.icon, deletedAt: Date()),
            at: 0)
        save()
    }

    // MARK: - Recycle bin

    /// Restore a trashed item to its original collection (recreating the
    /// collection shell if it no longer exists).
    func restoreItem(_ id: String) {
        guard let idx = trash.items.firstIndex(where: { $0.id == id }) else { return }
        let t = trash.items.remove(at: idx)
        if let ci = collections.firstIndex(where: { $0.id == t.collectionID }) {
            if !collections[ci].items.contains(where: { $0.id == t.item.id }) {
                collections[ci].items.append(t.item)
            }
        } else {
            collections.append(ItemCollection(id: t.collectionID, name: t.collectionName,
                                              icon: t.collectionIcon, items: [t.item]))
        }
        save()
    }

    /// Restore a trashed collection (merging items if an id collision exists).
    func restoreCollection(_ id: String) {
        guard let idx = trash.collections.firstIndex(where: { $0.id == id }) else { return }
        let t = trash.collections.remove(at: idx)
        if let ci = collections.firstIndex(where: { $0.id == t.collection.id }) {
            let existing = Set(collections[ci].items.map { $0.id })
            collections[ci].items.append(contentsOf: t.collection.items.filter { !existing.contains($0.id) })
        } else {
            collections.append(t.collection)
        }
        save()
    }

    func purgeItem(_ id: String) {
        trash.items.removeAll { $0.id == id }
        save()
        pruneImages()
    }

    func purgeCollection(_ id: String) {
        trash.collections.removeAll { $0.id == id }
        save()
        pruneImages()
    }

    func emptyTrash() {
        trash = Trash()
        save()
        pruneImages()
    }

    // MARK: - Template mutations

    func saveTemplate(name: String, fields: [TemplateField]) {
        templates.append(Template(name: name, fields: fields))
        save()
    }

    func deleteTemplate(_ id: String) {
        templates.removeAll { $0.id == id }
        save()
    }

    // MARK: - Import / Export

    /// Whole-library export includes the recycle bin; a scoped (single-collection)
    /// export carries only that collection.
    func exportEnvelope(scopeCollectionID: String? = nil) -> LibraryExport {
        if let scope = scopeCollectionID, let c = collection(scope) {
            return LibraryExport(collections: [c])
        }
        return LibraryExport(collections: collections, trash: trash)
    }

    func exportJSONData(scopeCollectionID: String? = nil) -> Data {
        (try? Self.makeEncoder().encode(exportEnvelope(scopeCollectionID: scopeCollectionID))) ?? Data()
    }

    func importData(collections incoming: [ItemCollection], trash incomingTrash: Trash, strategy: ImportStrategy) {
        switch strategy {
        case .replace:
            collections = incoming
            trash = incomingTrash
        case .merge:
            var order = collections.map { $0.id }
            var byID = Dictionary(collections.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
            for nc in incoming {
                if var existing = byID[nc.id] {
                    let existingItemIDs = Set(existing.items.map { $0.id })
                    existing.items.append(contentsOf: nc.items.filter { !existingItemIDs.contains($0.id) })
                    byID[nc.id] = existing
                } else {
                    byID[nc.id] = nc
                    order.append(nc.id)
                }
            }
            collections = order.compactMap { byID[$0] }

            // Merge trash by id.
            let exItemIDs = Set(trash.items.map { $0.id })
            trash.items.append(contentsOf: incomingTrash.items.filter { !exItemIDs.contains($0.id) })
            let exColIDs = Set(trash.collections.map { $0.id })
            trash.collections.append(contentsOf: incomingTrash.collections.filter { !exColIDs.contains($0.id) })
        }
        save()
        pruneImages() // a replace-import can drop the last reference to a photo
    }

    /// Parse raw JSON into collections + trash. Accepts a full envelope
    /// (`{ collections, trash }`) or a single bare collection (`{ name, items }`).
    static func parseImport(_ text: String) -> ParsedImport? {
        guard let data = text.data(using: .utf8) else { return nil }
        let decoder = makeDecoder()
        if let env = try? decoder.decode(LibraryExport.self, from: data), !env.collections.isEmpty {
            return ParsedImport(collections: env.collections, trash: env.trash)
        }
        if let single = try? decoder.decode(ItemCollection.self, from: data), !single.name.isEmpty {
            return ParsedImport(collections: [single], trash: Trash())
        }
        return nil
    }

    // MARK: - Persistence

    private func load() {
        guard let data = try? Data(contentsOf: fileURL) else { return }
        let decoder = Self.makeDecoder()
        if let file = try? decoder.decode(LibraryFile.self, from: data) {
            collections = file.collections
            templates = file.templates
            trash = file.trash
        } else if let cols = try? decoder.decode([ItemCollection].self, from: data) {
            collections = cols
        }
    }

    private func save() {
        let file = LibraryFile(collections: collections, templates: templates, trash: trash)
        guard let data = try? Self.makeEncoder().encode(file) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }

    // MARK: - Photo files

    /// Photo file names referenced by any live or trashed item.
    private var referencedImages: Set<String> {
        var refs = Set<String>()
        for c in collections { for it in c.items { refs.formUnion(it.images) } }
        for t in trash.items { refs.formUnion(t.item.images) }
        for t in trash.collections { for it in t.collection.items { refs.formUnion(it.images) } }
        return refs
    }

    /// Delete stored photo files nothing references any more. Safe to call at
    /// any mutation point: while the item editor is open (the only place new
    /// photo files are created before being referenced) no store mutation can
    /// happen, because it is presented full-screen.
    private func pruneImages() {
        let refs = referencedImages
        Task.detached(priority: .utility) {
            ImageStore.prune(referenced: refs)
        }
    }
}
