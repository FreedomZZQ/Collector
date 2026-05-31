package me.zq.collectors.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.zq.collectors.data.ImportStrategy
import me.zq.collectors.data.LibraryJson
import me.zq.collectors.data.ParsedImport
import me.zq.collectors.ui.components.GhostButton
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.PlainTextField
import me.zq.collectors.ui.components.PrimaryButton
import me.zq.collectors.ui.components.AppBottomSheet
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans

private enum class DataMode { EXPORTING, IMPORTING }

@Composable
fun ImportExportSheet(
    scopeCollectionID: String?,
    initialExporting: Boolean = true,
    onClose: () -> Unit,
) {
    val p = LocalPalette.current
    val store = LocalStore.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val scopeCollection = scopeCollectionID?.let { store.collection(it) }
    val json = remember { store.exportJson(scopeCollectionID) }
    val bytes = remember(json) { json.toByteArray() }
    val filename = remember {
        scopeCollection?.let { "collector-${it.name.lowercase().replace(" ", "-")}.json" } ?: "collector-library.json"
    }

    var mode by remember { mutableStateOf(if (initialExporting) DataMode.EXPORTING else DataMode.IMPORTING) }
    var paste by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ParsedImport?>(null) }
    var parseError by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) { delay(1600); copied = false }
    }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
        }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull().orEmpty()
            paste = text
            tryParse(text) { result, error -> parsed = result; parseError = error }
        }
    }

    fun parse(text: String) = tryParse(text) { result, error -> parsed = result; parseError = error }

    AppBottomSheet(title = scopeCollection?.name ?: "Library data", onClose = onClose, skipPartiallyExpanded = true) {
        if (scopeCollection == null) {
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(CircleShape)
                    .background(p.surface2, CircleShape)
                    .border(0.5.dp, p.line, CircleShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TabButton("Export", mode == DataMode.EXPORTING, Modifier.weight(1f)) { mode = DataMode.EXPORTING }
                TabButton("Import", mode == DataMode.IMPORTING, Modifier.weight(1f)) { mode = DataMode.IMPORTING }
            }
        }

        if (mode == DataMode.EXPORTING) {
            Text(
                scopeCollection?.let { "Export “${it.name}” (${it.items.size} items)" }
                    ?: "Export your whole library (${store.collections.size} collections)",
                style = sans(14f),
                color = p.muted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .card(p.surface2, p.line, Radius.sm)
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(json, style = mono(11f), color = p.muted)
            }
            Text(
                "$filename · ${"%.1f".format(bytes.size / 1024.0)} KB",
                style = mono(10.5f),
                color = p.faint,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Box(Modifier.padding(top = 8.dp)) {
                PrimaryButton(title = "Save to file", icon = "export") { createDoc.launch(filename) }
            }
            Box(Modifier.padding(top = 10.dp)) {
                GhostButton(title = if (copied) "Copied ✓" else "Copy JSON", icon = "doc", expands = true) {
                    clipboard.setText(AnnotatedString(json))
                    copied = true
                }
            }
        } else {
            Text(
                "Restore from a Collector JSON file, or paste its contents below.",
                style = sans(14f),
                color = p.muted,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            GhostButton(title = "Choose JSON file…", icon = "import", expands = true) {
                openDoc.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
            }
            Text(
                "OR PASTE",
                style = mono(10.5f),
                color = p.faint,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            PlainTextField(
                value = paste,
                onValueChange = { paste = it; parse(it) },
                placeholder = "{ \"app\": \"Collector\", \"collections\": [ … ] }",
                mono = true,
                multiline = true,
            )

            if (parseError.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .card(p.danger.copy(alpha = 0.12f), p.danger.copy(alpha = 0.35f), Radius.sm)
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    Text(parseError, style = sans(13.5f), color = p.danger)
                }
            }

            parsed?.let { result ->
                val itemCount = result.collections.sumOf { it.items.size }
                val trashCount = result.trash.count
                Column(
                    Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .card(p.accentSoft, p.accentLine, Radius.sm)
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon("check", size = 17.dp, tint = p.accent)
                        Text("Valid file", style = sans(15f, FontWeight.SemiBold), color = p.accent)
                    }
                    Text(
                        "${result.collections.size} collections · $itemCount items" +
                            if (trashCount > 0) " · $trashCount in trash" else "",
                        style = mono(11.5f),
                        color = p.muted,
                    )
                }
                Box(Modifier.padding(top = 14.dp)) {
                    PrimaryButton(title = "Merge into library", icon = "import") {
                        store.importData(result.collections, result.trash, ImportStrategy.MERGE)
                        onClose()
                    }
                }
                Box(Modifier.padding(top = 10.dp)) {
                    GhostButton(title = "Replace entire library", icon = "trash", danger = true, expands = true) {
                        store.importData(result.collections, result.trash, ImportStrategy.REPLACE)
                        onClose()
                    }
                }
            }
        }
    }
}

private inline fun tryParse(text: String, set: (ParsedImport?, String) -> Unit) {
    val t = text.trim()
    if (t.isEmpty()) { set(null, ""); return }
    val result = LibraryJson.parseImport(t)
    if (result != null && result.collections.isNotEmpty()) set(result, "")
    else set(null, "Not a valid Collector JSON file.")
}

@Composable
private fun TabButton(title: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) p.surface else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = sans(14f, FontWeight.SemiBold), color = if (active) p.text else p.faint)
    }
}
