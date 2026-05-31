package me.zq.collectors.data

import java.util.UUID

// Domain model for the Collector library. Field names mirror the iOS/desktop
// JSON so import/export round-trips across platforms. `id` on Field is local
// only (never written to JSON); everything else maps 1:1 to the wire shape.

// MARK: - Field

enum class FieldKind(val wire: String, val label: String) {
    TEXT("text", "Text"),
    DATE("date", "Date"),
    VALUE("value", "Value"),
    CONDITION("condition", "Condition"),
    MULTILINE("multiline", "Note");

    companion object {
        fun fromWire(s: String?): FieldKind = entries.firstOrNull { it.wire == s } ?: TEXT
    }
}

data class Field(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String = "",
    val kind: FieldKind = FieldKind.TEXT,
)

// MARK: - Item

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val image: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val fields: List<Field> = emptyList(),
) {
    companion object {
        /** Fields a brand-new item starts with (mirrors the iOS "Basic" template). */
        fun defaultFields(): List<Field> = listOf(
            Field(label = "Acquired", kind = FieldKind.DATE),
            Field(label = "Condition", kind = FieldKind.CONDITION),
            Field(label = "Value", kind = FieldKind.VALUE),
            Field(label = "Notes", kind = FieldKind.MULTILINE),
        )
    }
}

// MARK: - Collection

data class ItemCollection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "box",
    val items: List<Item> = emptyList(),
)

// MARK: - Template

data class TemplateField(val label: String, val kind: FieldKind)

data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fields: List<TemplateField>,
)

// MARK: - Trash (recycle bin). `deletedAt` is epoch millis; serialized as ISO-8601.

data class TrashedItem(
    val item: Item,
    val collectionID: String,
    val collectionName: String,
    val collectionIcon: String,
    val deletedAt: Long,
) {
    val id: String get() = item.id
}

data class TrashedCollection(
    val collection: ItemCollection,
    val deletedAt: Long,
) {
    val id: String get() = collection.id
}

data class Trash(
    val items: List<TrashedItem> = emptyList(),
    val collections: List<TrashedCollection> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty() && collections.isEmpty()
    val count: Int get() = items.size + collections.size
}

// MARK: - Persistence + import containers

/** On-device persistence file: collections + templates + trash. */
data class LibraryFile(
    val collections: List<ItemCollection>,
    val templates: List<Template>,
    val trash: Trash = Trash(),
)

/** Result of parsing an import file (collections + recycle bin). */
data class ParsedImport(val collections: List<ItemCollection>, val trash: Trash)

enum class ImportStrategy { MERGE, REPLACE }
