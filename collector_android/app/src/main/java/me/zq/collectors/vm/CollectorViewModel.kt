package me.zq.collectors.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import me.zq.collectors.data.CollectorRepository
import me.zq.collectors.data.ImportStrategy
import me.zq.collectors.data.Item
import me.zq.collectors.data.ItemCollection
import me.zq.collectors.data.LibraryFile
import me.zq.collectors.data.LibraryJson
import me.zq.collectors.data.Template
import me.zq.collectors.data.TemplateField
import me.zq.collectors.data.Trash
import me.zq.collectors.data.TrashedCollection
import me.zq.collectors.data.TrashedItem
import me.zq.collectors.data.collectionValue
import me.zq.collectors.data.parseValue

/**
 * Single source of truth. Owns collections + templates + trash and persists them
 * to a single JSON file. Deletes are "soft" — they move into the recycle bin first.
 * State is exposed as Compose snapshot state, so reads recompose automatically.
 */
class CollectorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CollectorRepository(app)

    var collections by mutableStateOf<List<ItemCollection>>(emptyList())
        private set
    var templates by mutableStateOf<List<Template>>(emptyList())
        private set
    var trash by mutableStateOf(Trash())
        private set

    init {
        val lib = repo.load()
        collections = lib.collections
        templates = lib.templates
        trash = lib.trash
    }

    private fun persist() = repo.save(LibraryFile(collections, templates, trash))

    // MARK: - Derived

    fun collection(id: String): ItemCollection? = collections.firstOrNull { it.id == id }

    fun item(collectionID: String, itemID: String): Item? =
        collection(collectionID)?.items?.firstOrNull { it.id == itemID }

    val totalItems: Int get() = collections.sumOf { it.items.size }
    val totalValue: Double get() = collections.sumOf { collectionValue(it) }
    val trashCount: Int get() = trash.count

    // MARK: - Collection mutations

    fun addCollection(name: String, icon: String): String {
        val col = ItemCollection(name = name, icon = icon)
        collections = collections + col
        persist()
        return col.id
    }

    fun renameCollection(id: String, name: String) {
        val i = collections.indexOfFirst { it.id == id }
        if (i < 0) return
        collections = collections.toMutableList().also { it[i] = it[i].copy(name = name) }
        persist()
    }

    fun setIcon(id: String, icon: String) {
        val i = collections.indexOfFirst { it.id == id }
        if (i < 0) return
        collections = collections.toMutableList().also { it[i] = it[i].copy(icon = icon) }
        persist()
    }

    /** Soft-delete: move the collection (with its items) into the recycle bin. */
    fun deleteCollection(id: String) {
        val idx = collections.indexOfFirst { it.id == id }
        if (idx < 0) return
        val col = collections[idx]
        collections = collections.toMutableList().also { it.removeAt(idx) }
        trash = trash.copy(collections = listOf(TrashedCollection(col, now())) + trash.collections)
        persist()
    }

    // MARK: - Item mutations

    /** Insert-or-update an item within a collection. */
    fun saveItem(collectionID: String, item: Item) {
        val ci = collections.indexOfFirst { it.id == collectionID }
        if (ci < 0) return
        val col = collections[ci]
        val ii = col.items.indexOfFirst { it.id == item.id }
        val newItems = if (ii >= 0) col.items.toMutableList().also { it[ii] = item } else col.items + item
        collections = collections.toMutableList().also { it[ci] = col.copy(items = newItems) }
        persist()
    }

    /** Soft-delete: move the item into the recycle bin. */
    fun deleteItem(collectionID: String, itemID: String) {
        val ci = collections.indexOfFirst { it.id == collectionID }
        if (ci < 0) return
        val col = collections[ci]
        val removed = col.items.firstOrNull { it.id == itemID } ?: return
        collections = collections.toMutableList().also { it[ci] = col.copy(items = col.items.filterNot { f -> f.id == itemID }) }
        trash = trash.copy(items = listOf(TrashedItem(removed, col.id, col.name, col.icon, now())) + trash.items)
        persist()
    }

    // MARK: - Recycle bin

    fun restoreItem(id: String) {
        val t = trash.items.firstOrNull { it.id == id } ?: return
        trash = trash.copy(items = trash.items.filterNot { it.id == id })
        val ci = collections.indexOfFirst { it.id == t.collectionID }
        if (ci >= 0) {
            val col = collections[ci]
            if (col.items.none { it.id == t.item.id }) {
                collections = collections.toMutableList().also { it[ci] = col.copy(items = col.items + t.item) }
            }
        } else {
            collections = collections + ItemCollection(t.collectionID, t.collectionName, t.collectionIcon, listOf(t.item))
        }
        persist()
    }

    fun restoreCollection(id: String) {
        val t = trash.collections.firstOrNull { it.id == id } ?: return
        trash = trash.copy(collections = trash.collections.filterNot { it.id == id })
        val ci = collections.indexOfFirst { it.id == t.collection.id }
        if (ci >= 0) {
            val existing = collections[ci].items.map { it.id }.toSet()
            val merged = collections[ci].items + t.collection.items.filterNot { existing.contains(it.id) }
            collections = collections.toMutableList().also { it[ci] = it[ci].copy(items = merged) }
        } else {
            collections = collections + t.collection
        }
        persist()
    }

    fun purgeItem(id: String) {
        trash = trash.copy(items = trash.items.filterNot { it.id == id })
        persist()
    }

    fun purgeCollection(id: String) {
        trash = trash.copy(collections = trash.collections.filterNot { it.id == id })
        persist()
    }

    fun emptyTrash() {
        trash = Trash()
        persist()
    }

    // MARK: - Template mutations

    fun saveTemplate(name: String, fields: List<TemplateField>) {
        templates = templates + Template(name = name, fields = fields)
        persist()
    }

    fun deleteTemplate(id: String) {
        templates = templates.filterNot { it.id == id }
        persist()
    }

    // MARK: - Import / Export

    /** Whole-library export includes the recycle bin; a scoped export carries only that collection. */
    fun exportJson(scopeCollectionID: String?): String {
        return if (scopeCollectionID != null) {
            val c = collection(scopeCollectionID)
            LibraryJson.encodeExport(listOfNotNull(c), Trash())
        } else {
            LibraryJson.encodeExport(collections, trash)
        }
    }

    fun importData(incoming: List<ItemCollection>, incomingTrash: Trash, strategy: ImportStrategy) {
        when (strategy) {
            ImportStrategy.REPLACE -> {
                collections = incoming
                trash = incomingTrash
            }
            ImportStrategy.MERGE -> {
                val order = collections.map { it.id }.toMutableList()
                val byID = LinkedHashMap<String, ItemCollection>()
                collections.forEach { byID.putIfAbsent(it.id, it) }
                for (nc in incoming) {
                    val existing = byID[nc.id]
                    if (existing != null) {
                        val existingItemIDs = existing.items.map { it.id }.toSet()
                        byID[nc.id] = existing.copy(items = existing.items + nc.items.filterNot { existingItemIDs.contains(it.id) })
                    } else {
                        byID[nc.id] = nc
                        order.add(nc.id)
                    }
                }
                collections = order.mapNotNull { byID[it] }

                val exItemIDs = trash.items.map { it.id }.toSet()
                val exColIDs = trash.collections.map { it.id }.toSet()
                trash = trash.copy(
                    items = trash.items + incomingTrash.items.filterNot { exItemIDs.contains(it.id) },
                    collections = trash.collections + incomingTrash.collections.filterNot { exColIDs.contains(it.id) },
                )
            }
        }
        persist()
    }

    private fun now() = System.currentTimeMillis()
}
