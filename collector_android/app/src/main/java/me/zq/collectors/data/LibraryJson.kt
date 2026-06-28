package me.zq.collectors.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Hand-rolled JSON (Android's built-in org.json) so the wire format matches the
// iOS app and desktop app exactly, with no compiler-plugin dependency.
//
// Wire shapes:
//   Field       { label, value, kind }                         (id is local-only)
//   Item        { id, name, image?, description, tags, fields }
//   Collection  { id, name, icon, items }
//   Template    { id, name, fields:[{label, kind}] }
//   Trash       { items:[{item, collectionID, collectionName, collectionIcon, deletedAt}],
//                 collections:[{collection, deletedAt}] }
//   LibraryFile { collections, templates, trash }              (on-device)
//   Export      { app, version, exportedAt, collections, trash }
object LibraryJson {

    // MARK: - Dates (ISO-8601, matching iOS .iso8601 encoding)

    private val isoOut = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    )

    private fun encodeDate(millis: Long): String = isoOut.format(Date(millis))

    private fun decodeDate(s: String?): Long {
        if (s.isNullOrBlank()) return System.currentTimeMillis()
        for (p in isoPatterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                if (p.endsWith("'Z'")) fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(s)?.time ?: continue
            } catch (_: Exception) { /* try next */ }
        }
        return System.currentTimeMillis()
    }

    // MARK: - Small JSON helpers

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map { optString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun iconNameFromDesktop(icon: String): String {
        return when (icon) {
            "headphones", "pen", "camera", "box", "coin", "tag", "image" -> icon
            "🎧" -> "headphones"
            "✒", "🖊", "🖋", "✏" -> "pen"
            "📷", "📸" -> "camera"
            "🪙", "💰", "💵" -> "coin"
            "🏷" -> "tag"
            "🖼", "🌄", "🌅" -> "image"
            else -> "box"
        }
    }

    private fun inferKind(label: String): FieldKind {
        val l = label.lowercase(Locale.US)
        return when {
            l.contains("value") || l.contains("price") || l.contains("cost") || l.contains("worth") -> FieldKind.VALUE
            l.contains("condition") || l.contains("grade") -> FieldKind.CONDITION
            l.contains("acquired") || l.contains("date") || l.contains("purchased") || l.contains("year") -> FieldKind.DATE
            l.contains("note") || l.contains("comment") -> FieldKind.MULTILINE
            else -> FieldKind.TEXT
        }
    }

    // MARK: - Encode

    private fun fieldJson(f: Field) = JSONObject().apply {
        put("label", f.label)
        put("value", f.value)
        put("kind", f.kind.wire)
    }

    private fun itemJson(it: Item) = JSONObject().apply {
        put("id", it.id)
        put("name", it.name)
        if (it.image != null) put("image", it.image) // mirrors Swift encodeIfPresent
        put("description", it.description)
        put("tags", JSONArray(it.tags))
        put("fields", JSONArray(it.fields.map { fieldJson(it) }))
    }

    private fun collectionJson(c: ItemCollection) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("icon", c.icon)
        put("items", JSONArray(c.items.map { itemJson(it) }))
    }

    private fun templateJson(t: Template) = JSONObject().apply {
        put("id", t.id)
        put("name", t.name)
        put("fields", JSONArray(t.fields.map {
            JSONObject().apply { put("label", it.label); put("kind", it.kind.wire) }
        }))
    }

    private fun trashJson(t: Trash) = JSONObject().apply {
        put("items", JSONArray(t.items.map { ti ->
            JSONObject().apply {
                put("item", itemJson(ti.item))
                put("collectionID", ti.collectionID)
                put("collectionName", ti.collectionName)
                put("collectionIcon", ti.collectionIcon)
                put("deletedAt", encodeDate(ti.deletedAt))
            }
        }))
        put("collections", JSONArray(t.collections.map { tc ->
            JSONObject().apply {
                put("collection", collectionJson(tc.collection))
                put("deletedAt", encodeDate(tc.deletedAt))
            }
        }))
    }

    fun encodeLibraryFile(lib: LibraryFile): String = JSONObject().apply {
        put("collections", JSONArray(lib.collections.map { collectionJson(it) }))
        put("templates", JSONArray(lib.templates.map { templateJson(it) }))
        put("trash", trashJson(lib.trash))
    }.toString(2)

    /** Portable export envelope (whole library carries trash; scoped export does not). */
    fun encodeExport(collections: List<ItemCollection>, trash: Trash): String = JSONObject().apply {
        put("app", "Collector")
        put("version", 1)
        put("exportedAt", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        put("collections", JSONArray(collections.map { collectionJson(it) }))
        put("trash", trashJson(trash))
    }.toString(2)

    // MARK: - Decode

    private fun fieldFrom(o: JSONObject) = Field(
        label = o.optString("label"),
        value = o.optString("value"),
        kind = FieldKind.fromWire(o.optStringOrNull("kind")),
    )

    private fun itemFrom(o: JSONObject) = Item(
        id = o.optStringOrNull("id") ?: java.util.UUID.randomUUID().toString(),
        name = o.optString("name"),
        image = o.optStringOrNull("image"),
        description = o.optString("description"),
        tags = o.optJSONArray("tags")?.strings() ?: emptyList(),
        fields = o.optJSONArray("fields")?.objects()?.map { fieldFrom(it) } ?: emptyList(),
    )

    private fun collectionFrom(o: JSONObject) = ItemCollection(
        id = o.optStringOrNull("id") ?: java.util.UUID.randomUUID().toString(),
        name = o.optStringOrNull("name") ?: "Untitled",
        icon = o.optStringOrNull("icon") ?: "box",
        items = o.optJSONArray("items")?.objects()?.map { itemFrom(it) } ?: emptyList(),
    )

    private fun templateFrom(o: JSONObject) = Template(
        id = o.optStringOrNull("id") ?: java.util.UUID.randomUUID().toString(),
        name = o.optString("name"),
        fields = o.optJSONArray("fields")?.objects()?.map {
            TemplateField(it.optString("label"), FieldKind.fromWire(it.optStringOrNull("kind")))
        } ?: emptyList(),
    )

    private fun trashFrom(o: JSONObject) = Trash(
        items = o.optJSONArray("items")?.objects()?.mapNotNull { ti ->
            val itemObj = ti.optJSONObject("item") ?: return@mapNotNull null
            TrashedItem(
                item = itemFrom(itemObj),
                collectionID = ti.optString("collectionID"),
                collectionName = ti.optString("collectionName"),
                collectionIcon = ti.optStringOrNull("collectionIcon") ?: "box",
                deletedAt = decodeDate(ti.optStringOrNull("deletedAt")),
            )
        } ?: emptyList(),
        collections = o.optJSONArray("collections")?.objects()?.mapNotNull { tc ->
            val colObj = tc.optJSONObject("collection") ?: return@mapNotNull null
            TrashedCollection(
                collection = collectionFrom(colObj),
                deletedAt = decodeDate(tc.optStringOrNull("deletedAt")),
            )
        } ?: emptyList(),
    )

    private fun desktopFieldFrom(o: JSONObject): Field {
        val label = o.optString("label")
        return Field(
            label = label,
            value = o.optString("value"),
            kind = FieldKind.fromWire(o.optStringOrNull("kind")).takeIf { o.has("kind") } ?: inferKind(label),
        )
    }

    private fun desktopItemFrom(o: JSONObject): Item {
        val rawFields = o.optJSONArray("custom_fields")?.objects() ?: emptyList()
        val tags = rawFields
            .firstOrNull { it.optString("label").trim().equals("tags", ignoreCase = true) }
            ?.optString("value")
            ?.split(",")
            ?.map { it.trim().lowercase(Locale.US) }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val fields = rawFields
            .filterNot { it.optString("label").trim().equals("tags", ignoreCase = true) }
            .map { desktopFieldFrom(it) }
            .toMutableList()
        val acquired = o.optStringOrNull("acquired_date").orEmpty()
        val alreadyHasAcquired = fields.any { it.label.contains("acquired", ignoreCase = true) }
        if (acquired.isNotBlank() && !alreadyHasAcquired) {
            fields.add(0, Field(label = "Acquired", value = acquired, kind = FieldKind.DATE))
        }
        return Item(
            id = o.optStringOrNull("id") ?: java.util.UUID.randomUUID().toString(),
            name = o.optString("name"),
            image = null, // Desktop photo paths are local to that machine.
            description = o.optStringOrNull("short_desc") ?: o.optString("description"),
            tags = tags,
            fields = fields,
        )
    }

    private fun desktopImportFrom(root: JSONObject): ParsedImport? {
        val collectionsJson = root.optJSONArray("collections") ?: return null
        val itemsJson = root.optJSONArray("items") ?: return null
        val itemsByCollection = itemsJson.objects().groupBy { it.optString("collection_id") }
        val collections = collectionsJson.objects().map { c ->
            val id = c.optStringOrNull("id") ?: java.util.UUID.randomUUID().toString()
            ItemCollection(
                id = id,
                name = c.optStringOrNull("name") ?: "Untitled",
                icon = iconNameFromDesktop(c.optStringOrNull("icon") ?: "box"),
                items = itemsByCollection[id]?.map { desktopItemFrom(it) } ?: emptyList(),
            )
        }
        if (collections.isEmpty()) return null
        return ParsedImport(collections, Trash())
    }

    /** Decode the on-device file: `{collections, templates, trash}` or a bare `[collection,…]`. */
    fun decodeLibraryFile(text: String): LibraryFile? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            if (trimmed.startsWith("[")) {
                LibraryFile(JSONArray(trimmed).objects().map { collectionFrom(it) }, emptyList(), Trash())
            } else {
                val o = JSONObject(trimmed)
                LibraryFile(
                    collections = o.optJSONArray("collections")?.objects()?.map { collectionFrom(it) } ?: emptyList(),
                    templates = o.optJSONArray("templates")?.objects()?.map { templateFrom(it) } ?: emptyList(),
                    trash = o.optJSONObject("trash")?.let { trashFrom(it) } ?: Trash(),
                )
            }
        } catch (_: Exception) { null }
    }

    /**
     * Parse an import file. Accepts a full envelope (`{ collections, trash }`) or a
     * single bare collection (`{ name, items }`).
     */
    fun parseImport(text: String): ParsedImport? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val root = try { JSONObject(trimmed) } catch (_: Exception) { return null }
        desktopImportFrom(root)?.let { return it }
        if (root.has("collections")) {
            val collections = root.optJSONArray("collections")?.objects()?.map { collectionFrom(it) } ?: emptyList()
            if (collections.isEmpty()) return null
            val trash = root.optJSONObject("trash")?.let { trashFrom(it) } ?: Trash()
            return ParsedImport(collections, trash)
        }
        if (root.has("name")) {
            val single = collectionFrom(root)
            if (single.name.isNotEmpty()) return ParsedImport(listOf(single), Trash())
        }
        return null
    }
}
