package me.zq.collectors.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import me.zq.collectors.ui.components.EmptyStateView
import me.zq.collectors.ui.components.GhostButton
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.Thumb
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

@Composable
fun RecycleBinScreen(nav: NavHostController) {
    val p = LocalPalette.current
    val store = LocalStore.current
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 8.dp)) {
            Row(
                Modifier.align(Alignment.CenterStart).clickable { nav.popBackStack() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon("chevron-left", size = 22.dp, tint = p.accent)
                Text("Settings", style = sans(16f), color = p.accent)
            }
            Text("Recycle Bin", style = serif(18f), color = p.text, modifier = Modifier.align(Alignment.Center))
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.line))

        if (store.trash.isEmpty) {
            EmptyStateView(
                icon = "trash",
                title = "Recycle bin is empty",
                sub = "Deleted collections and items are kept here until you remove them permanently.",
            )
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                if (store.trash.collections.isNotEmpty()) {
                    Box(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp)) { MonoLabel("Collections") }
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        store.trash.collections.forEach { tc ->
                            TrashRow(
                                icon = tc.collection.icon,
                                title = tc.collection.name,
                                subtitle = "Collection · ${tc.collection.items.size} items · ${deletedAgo(tc.deletedAt)}",
                                onRestore = { store.restoreCollection(tc.id) },
                                onPurge = { store.purgeCollection(tc.id) },
                            )
                        }
                    }
                }
                if (store.trash.items.isNotEmpty()) {
                    Box(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 8.dp)) { MonoLabel("Items") }
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        store.trash.items.forEach { ti ->
                            TrashRow(
                                icon = ti.collectionIcon,
                                title = ti.item.name.ifEmpty { "Untitled" },
                                subtitle = "from ${ti.collectionName} · ${deletedAgo(ti.deletedAt)}",
                                onRestore = { store.restoreItem(ti.id) },
                                onPurge = { store.purgeItem(ti.id) },
                            )
                        }
                    }
                }
                Box(Modifier.padding(20.dp)) {
                    GhostButton(title = "Empty recycle bin", icon = "trash", danger = true, expands = true) { confirmEmpty = true }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = p.surface,
            titleContentColor = p.text,
            textContentColor = p.muted,
            title = { Text("Empty the recycle bin?", style = serif(19f, FontWeight.SemiBold)) },
            text = { Text("This permanently deletes everything in it.", style = sans(14.5f)) },
            confirmButton = {
                TextButton(onClick = { store.emptyTrash(); confirmEmpty = false }) {
                    Text("Delete all", style = sans(15f, FontWeight.SemiBold), color = p.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text("Cancel", style = sans(15f), color = p.muted)
                }
            },
        )
    }
}

@Composable
private fun TrashRow(icon: String, title: String, subtitle: String, onRestore: () -> Unit, onPurge: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().card(p.surface, p.line, Radius.r).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(icon, size = 42.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = serif(16f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = mono(10.5f), color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.clip(CircleShape).background(p.accentSoft, CircleShape).clickable { onRestore() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Restore", style = sans(13f, FontWeight.SemiBold), color = p.accent)
        }
        Box(Modifier.clip(CircleShape).clickable { onPurge() }.padding(6.dp)) {
            Icon("trash", size = 18.dp, tint = p.danger)
        }
    }
}

private fun deletedAgo(millis: Long): String {
    val rel = DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    return "deleted ${rel.lowercase()}"
}
