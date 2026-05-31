package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.zq.collectors.data.Item
import me.zq.collectors.data.ItemCollection
import me.zq.collectors.data.allTags
import me.zq.collectors.ui.components.Chip
import me.zq.collectors.ui.components.EmptyStateView
import me.zq.collectors.ui.components.LargeHeader
import me.zq.collectors.ui.components.SearchField
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.nav.Routes
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono

@Composable
fun SearchScreen(nav: NavHostController) {
    val p = LocalPalette.current
    val store = LocalStore.current

    var q by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    val flat: List<Pair<ItemCollection, Item>> = store.collections.flatMap { c -> c.items.map { c to it } }
    val selectedTags = selected.filterValues { it }.keys

    val s = q.trim().lowercase()
    val results = flat.filter { (_, item) ->
        val matchesQuery = s.isEmpty() ||
            item.name.lowercase().contains(s) ||
            item.description.lowercase().contains(s) ||
            item.tags.any { it.contains(s) } ||
            item.fields.any { it.value.lowercase().contains(s) }
        val matchesTags = selectedTags.isEmpty() || selectedTags.all { tag -> item.tags.contains(tag) }
        matchesQuery && matchesTags
    }

    val active = s.isNotEmpty() || selectedTags.isNotEmpty()
    val tags = allTags(store.collections)

    Column(Modifier.fillMaxSize()) {
        LargeHeader(title = "Search", sub = "across ${flat.size} pieces")
        SearchField(q, { q = it }, placeholder = "Name, maker, tag, note…", modifier = Modifier.padding(horizontal = 20.dp))

        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tags.forEach { (tag, count) ->
                    Chip(text = "$tag $count", active = selected[tag] == true) {
                        selected[tag] = !(selected[tag] ?: false)
                    }
                }
            }
        }

        when {
            !active -> EmptyStateView(
                icon = "search",
                title = "Find anything",
                sub = "Search by name, maker, tag, or note — or tap a tag above to filter across every collection.",
            )
            results.isEmpty() -> EmptyStateView(icon = "search", title = "No matches", sub = "Try a different term or clear a tag.")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.second.id }) { (col, item) ->
                    Box(
                        Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(Radius.r))
                            .clickable { nav.navigate(Routes.item(col.id, item.id)) },
                    ) {
                        ItemRow(item, col.icon)
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 11.dp, end = 13.dp)
                                .clip(CircleShape)
                                .background(p.surface2, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(col.name.uppercase(), style = mono(9f, tracking = 0.5f), color = p.faint)
                        }
                    }
                }
            }
        }
    }
}
