package me.zq.collectors.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.zq.collectors.data.ItemCollection
import me.zq.collectors.data.collectionValue
import me.zq.collectors.data.money
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.IconCircleButton
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.SwipeToDelete
import me.zq.collectors.ui.components.Thumb
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.components.dashedBorder
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.nav.Routes
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

@Composable
fun CollectionsHomeScreen(nav: NavHostController) {
    val p = LocalPalette.current
    val store = LocalStore.current

    var showNew by remember { mutableStateOf(false) }
    var showData by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        MonoLabel("Local library")
                        Column(Modifier.padding(top = 6.dp)) {
                            Text("Collector", style = serif(33f, FontWeight.SemiBold, tracking = -0.3f), color = p.text)
                            Text(
                                "${store.collections.size} collections · ${store.totalItems} pieces",
                                style = mono(12.5f),
                                color = p.muted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconCircleButton("share", iconSize = 19.dp, bordered = true) { showData = true }
                        IconCircleButton("plus", iconSize = 19.dp, bordered = true) { showNew = true }
                    }
                }
            }

            item {
                ValueCard(store.totalValue, Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp))
            }

            item {
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 8.dp)) {
                    MonoLabel("Collections")
                }
            }

            items(store.collections, key = { it.id }) { c ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                    SwipeToDelete(onDelete = { store.deleteCollection(c.id) }) {
                        CollectionRow(c) { nav.navigate(Routes.collection(c.id)) }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 7.dp, bottom = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.r))
                        .dashedBorder(p.lineStrong, Radius.r)
                        .clickable { showNew = true }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon("folder-plus", size = 19.dp, tint = p.muted)
                    Text("New collection", style = sans(15f, FontWeight.SemiBold), color = p.muted)
                }
            }
        }
    }

    if (showNew) {
        NewCollectionSheet(
            onAdd = { name, icon -> store.addCollection(name, icon); showNew = false },
            onClose = { showNew = false },
        )
    }
    if (showData) {
        ImportExportSheet(scopeCollectionID = null, onClose = { showData = false })
    }
}

@Composable
private fun ValueCard(total: Double, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .card(p.accentSoft, p.accentLine, Radius.r)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonoLabel("Estimated value", color = p.accent.copy(alpha = 0.9f))
            Text(money(total), style = serif(30f, tracking = -0.3f), color = p.text)
        }
        Icon("coin", size = 26.dp, tint = p.accent.copy(alpha = 0.85f))
    }
}

@Composable
private fun CollectionRow(c: ItemCollection, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r))
            .card(p.surface, p.line, Radius.r)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(c.icon, size = 50.dp, accentTint = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(c.name, style = serif(18f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${c.items.size} ${if (c.items.size == 1) "item" else "items"} · ${money(collectionValue(c))}",
                style = mono(11.5f),
                color = p.muted,
            )
        }
        Icon("chevron-right", size = 18.dp, tint = p.faint)
    }
}
