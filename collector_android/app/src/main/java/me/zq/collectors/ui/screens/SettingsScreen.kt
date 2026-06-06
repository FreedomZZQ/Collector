package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.zq.collectors.data.Accent
import me.zq.collectors.data.ItemLayout
import me.zq.collectors.data.ThemeMode
import me.zq.collectors.data.money
import me.zq.collectors.ui.components.GhostButton
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.SegOption
import me.zq.collectors.ui.components.Segmented
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.nav.LocalSettings
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.nav.Routes
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.accentSwatch
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

private fun themeIcon(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> "auto"
    ThemeMode.LIGHT -> "sun"
    ThemeMode.DARK -> "moon"
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    val p = LocalPalette.current
    val store = LocalStore.current
    val settings = LocalSettings.current

    var showData by remember { mutableStateOf(false) }
    var dataExporting by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        me.zq.collectors.ui.components.LargeHeader(title = "Settings")

        Section("Appearance") {
            Card {
                SettingRow(last = false) {
                    Text("Theme", style = sans(16f), color = p.text)
                    Box(Modifier.weight(1f))
                    Segmented(
                        value = settings.themeMode,
                        options = ThemeMode.entries.map { SegOption(it, icon = themeIcon(it)) },
                        onChange = { settings.updateTheme(it) },
                    )
                }
                SettingRow(last = false) {
                    Text("Accent", style = sans(16f), color = p.text)
                    Box(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Accent.entries.forEach { a ->
                            val isSel = settings.accent == a
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(accentSwatch(a), CircleShape)
                                    .border(2.dp, if (isSel) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                                    .clickable { settings.updateAccent(a) },
                            )
                        }
                    }
                }
                SettingRow(last = true) {
                    Text("Default view", style = sans(16f), color = p.text)
                    Box(Modifier.weight(1f))
                    Segmented(
                        value = settings.defaultView,
                        options = listOf(SegOption(ItemLayout.LIST, icon = "list"), SegOption(ItemLayout.GRID, icon = "grid")),
                        onChange = { settings.updateView(it) },
                    )
                }
            }
        }

        Section("Data") {
            Card {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Stat("${store.collections.size}", "Collections", first = true)
                    Stat("${store.totalItems}", "Pieces", first = false)
                    Stat(money(store.totalValue), "Value", first = false)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(title = "Import", icon = "import", expands = true, modifier = Modifier.weight(1f)) {
                    dataExporting = false; showData = true
                }
                GhostButton(title = "Export", icon = "export", expands = true, modifier = Modifier.weight(1f)) {
                    dataExporting = true; showData = true
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon("info", size = 14.dp, tint = p.faint)
                Text(
                    "Everything lives in a single JSON file on this device. Back it up by exporting.",
                    style = sans(12.5f),
                    color = p.faint,
                )
            }
        }

        Section("Recycle Bin") {
            Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.r)).clickable { nav.navigate(Routes.RECYCLE) }) {
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon("trash", size = 18.dp, tint = p.muted)
                        Text(
                            if (store.trashCount == 0) "Empty" else "${store.trashCount} deleted",
                            style = sans(16f),
                            color = if (store.trashCount == 0) p.faint else p.text,
                        )
                        Box(Modifier.weight(1f))
                        Icon("chevron-right", size = 16.dp, tint = p.faint)
                    }
                }
            }
        }

        Section("Templates") {
            Card {
                if (store.templates.isEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("No templates yet.", style = sans(15f), color = p.faint)
                    }
                } else {
                    store.templates.forEachIndexed { i, t ->
                        SettingRow(last = i == store.templates.size - 1) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(t.name, style = sans(15.5f), color = p.text)
                                Text("${t.fields.size} fields", style = mono(10.5f), color = p.faint)
                            }
                            Box(Modifier.clip(CircleShape).clickable { store.deleteTemplate(t.id) }.padding(4.dp)) {
                                Icon("trash", size = 17.dp, tint = p.faint)
                            }
                        }
                    }
                }
            }
        }

        Section("About") {
            Card {
                SettingRow(last = false) {
                    Text("Collector's Notebook", style = sans(16f), color = p.text)
                    Box(Modifier.weight(1f))
                    Text("Android · 1.0", style = mono(13f), color = p.faint)
                }
                SettingRow(last = true) {
                    Text("Storage", style = sans(16f), color = p.text)
                    Box(Modifier.weight(1f))
                    Text("On device", style = mono(13f), color = p.faint)
                }
            }
        }
    }

    if (showData) {
        ImportExportSheet(scopeCollectionID = null, initialExporting = dataExporting, onClose = { showData = false })
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MonoLabel(label)
        content()
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.r)).card(p.surface, p.line, Radius.r),
    ) { content() }
}

@Composable
private fun SettingRow(last: Boolean, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        if (!last) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.line))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(value: String, label: String, first: Boolean) {
    val p = LocalPalette.current
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        if (!first) Box(Modifier.width(0.5.dp).height(36.dp).background(p.line))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = serif(21f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoLabel(label, size = 9.5f)
        }
    }
}
