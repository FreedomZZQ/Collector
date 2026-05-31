package me.zq.collectors.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.zq.collectors.data.FieldKind
import me.zq.collectors.data.Item
import me.zq.collectors.data.ItemLayout
import me.zq.collectors.data.collectionValue
import me.zq.collectors.data.fieldByKind
import me.zq.collectors.data.money
import me.zq.collectors.data.parseValue
import me.zq.collectors.ui.components.EmptyStateView
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.IconCircleButton
import me.zq.collectors.ui.components.PrimaryButton
import me.zq.collectors.ui.components.SearchField
import me.zq.collectors.ui.components.SegOption
import me.zq.collectors.ui.components.Segmented
import me.zq.collectors.ui.components.SwipeToDelete
import me.zq.collectors.ui.components.Thumb
import me.zq.collectors.ui.nav.LocalSettings
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.nav.Routes
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

enum class SortKey(val label: String) {
    NAME("Name (A–Z)"),
    VALUE_DESC("Value (high → low)"),
    VALUE_ASC("Value (low → high)"),
    RECENT("Recently acquired");

    val short: String get() = label.substringBefore(" ")
}

fun sortItems(items: List<Item>, sort: SortKey): List<Item> = when (sort) {
    SortKey.NAME -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortKey.VALUE_DESC -> items.sortedByDescending { parseValue(it) }
    SortKey.VALUE_ASC -> items.sortedBy { parseValue(it) }
    SortKey.RECENT -> items.sortedByDescending { fieldByKind(it, FieldKind.DATE)?.value ?: "" }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionDetailScreen(nav: NavHostController, collectionId: String) {
    val p = LocalPalette.current
    val store = LocalStore.current
    val settings = LocalSettings.current

    val col = store.collection(collectionId)
    if (col == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    var q by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortKey.NAME) }
    var view by remember { mutableStateOf(settings.defaultView) }
    var showSort by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var gridMenuFor by remember { mutableStateOf<String?>(null) }

    val items = remember(col.items, q, sort) {
        val s = q.trim().lowercase()
        var list = col.items
        if (s.isNotEmpty()) {
            list = list.filter { it ->
                it.name.lowercase().contains(s) ||
                    it.description.lowercase().contains(s) ||
                    it.tags.any { t -> t.contains(s) } ||
                    it.fields.any { f -> f.value.lowercase().contains(s) }
            }
        }
        sortItems(list, sort)
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.clickable { nav.popBackStack() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon("chevron-left", size = 22.dp, tint = p.accent)
                Text("Library", style = sans(16f, FontWeight.Medium), color = p.accent)
            }
            Box(Modifier.weight(1f))
            IconCircleButton("plus", iconSize = 20.dp) { nav.navigate(Routes.editorNew(collectionId)) }
            Box {
                IconCircleButton("dots", iconSize = 20.dp) { showOverflow = true }
                DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                    DropdownMenuItem(
                        text = { Text("Export this collection as JSON", style = sans(15f), color = p.text) },
                        onClick = { showOverflow = false; showExport = true },
                        leadingIcon = { Icon("export", size = 18.dp, tint = p.text) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete collection", style = sans(15f), color = p.danger) },
                        onClick = {
                            showOverflow = false
                            // The null-guard at the top of this screen pops back once `col` is gone.
                            store.deleteCollection(collectionId)
                        },
                        leadingIcon = { Icon("trash", size = 18.dp, tint = p.danger) },
                    )
                }
            }
        }

        // Collection header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumb(col.icon, size = 40.dp, radius = 11.dp, accentTint = true)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(col.name, style = serif(27f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${col.items.size} items · ${money(collectionValue(col))}", style = mono(11f), color = p.muted)
            }
        }

        SearchField(q, { q = it }, placeholder = "Search ${col.name.lowercase()}…", modifier = Modifier.padding(horizontal = 20.dp))

        // Controls row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Row(
                    modifier = Modifier.clickable { showSort = true },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon("sort", size = 15.dp, tint = p.muted)
                    Text(sort.short.uppercase(), style = mono(11.5f, tracking = 0.5f), color = p.muted)
                }
                DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                    SortKey.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label, style = sans(15f), color = p.text) },
                            onClick = { sort = s; showSort = false },
                            trailingIcon = { if (sort == s) Icon("check", size = 18.dp, tint = p.accent) },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f))
            Segmented(
                value = view,
                options = listOf(SegOption(ItemLayout.LIST, icon = "list"), SegOption(ItemLayout.GRID, icon = "grid")),
                onChange = { view = it },
            )
        }

        // Content
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    EmptyStateView(
                        icon = if (q.isEmpty()) col.icon else "search",
                        title = if (q.isEmpty()) "Nothing here yet" else "No matches",
                        sub = if (q.isEmpty()) "Add your first piece to this collection." else "Try a different term.",
                        action = if (q.isEmpty()) {
                            { PrimaryButton(title = "Add item", icon = "plus", expands = false) { nav.navigate(Routes.editorNew(collectionId)) } }
                        } else null,
                    )
                }
            } else if (view == ItemLayout.LIST) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { it ->
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            SwipeToDelete(onDelete = { store.deleteItem(collectionId, it.id) }) {
                                Box(Modifier.clickable { nav.navigate(Routes.item(collectionId, it.id)) }) {
                                    ItemRow(it, col.icon)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { it ->
                        Box {
                            ItemCard(
                                it,
                                col.icon,
                                modifier = Modifier.combinedClickable(
                                    onClick = { nav.navigate(Routes.item(collectionId, it.id)) },
                                    onLongClick = { gridMenuFor = it.id },
                                ),
                            )
                            DropdownMenu(expanded = gridMenuFor == it.id, onDismissRequest = { gridMenuFor = null }) {
                                DropdownMenuItem(
                                    text = { Text("Delete", style = sans(15f), color = p.danger) },
                                    onClick = { gridMenuFor = null; store.deleteItem(collectionId, it.id) },
                                    leadingIcon = { Icon("trash", size = 18.dp, tint = p.danger) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExport) {
        ImportExportSheet(scopeCollectionID = collectionId, onClose = { showExport = false })
    }
}
