package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.zq.collectors.data.Field
import me.zq.collectors.data.FieldKind
import me.zq.collectors.data.Item
import me.zq.collectors.data.Template
import me.zq.collectors.data.TemplateField
import me.zq.collectors.ui.components.AppBottomSheet
import me.zq.collectors.ui.components.Chip
import me.zq.collectors.ui.components.GhostButton
import me.zq.collectors.ui.components.HeroStripes
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.PlainTextField
import me.zq.collectors.ui.components.PrimaryButton
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.components.dashedBorder
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditorScreen(nav: NavHostController, collectionId: String, itemId: String?) {
    val p = LocalPalette.current
    val store = LocalStore.current

    val existing = remember { itemId?.let { store.item(collectionId, it) } }
    val draftId = remember { existing?.id ?: java.util.UUID.randomUUID().toString() }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    val tags = remember { mutableStateListOf<String>().apply { existing?.tags?.let { addAll(it) } } }
    val fields = remember { mutableStateListOf<Field>().apply { addAll(existing?.fields ?: Item.defaultFields()) } }
    var tagInput by remember { mutableStateOf("") }

    var showAddField by remember { mutableStateOf(false) }
    var showLoadTemplate by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }

    val trimmedName = name.trim()

    fun addTag() {
        val t = tagInput.trim().lowercase()
        if (t.isNotEmpty() && !tags.contains(t)) tags.add(t)
        tagInput = ""
    }

    fun save() {
        if (trimmedName.isEmpty()) return
        store.saveItem(
            collectionId,
            Item(
                id = draftId,
                name = trimmedName,
                image = existing?.image,
                description = description,
                tags = tags.toList(),
                fields = fields.toList(),
            ),
        )
        nav.popBackStack()
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 10.dp),
        ) {
            Text(
                "Cancel",
                style = sans(16f),
                color = p.muted,
                modifier = Modifier.align(Alignment.CenterStart).clip(RoundedCornerShape(8.dp)).clickable { nav.popBackStack() }.padding(4.dp),
            )
            Text(
                if (existing == null) "New item" else "Edit item",
                style = serif(18f),
                color = p.text,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                "Save",
                style = sans(16f, FontWeight.Bold),
                color = if (trimmedName.isEmpty()) p.faint else p.accent,
                modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(8.dp)).clickable { save() }.padding(4.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.line))

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 60.dp),
        ) {
            // Photo placeholder
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(Radius.r))
                    .dashedBorder(p.lineStrong, Radius.r),
                contentAlignment = Alignment.Center,
            ) {
                HeroStripes(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.r)))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon("image", size = 32.dp, tint = p.faint)
                    Text("Add photo", style = sans(13f, FontWeight.SemiBold), color = p.faint)
                }
            }

            Labeled("Name", top = 20.dp) {
                PlainTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Leica M6")
            }
            Labeled("Description", top = 16.dp) {
                PlainTextField(value = description, onValueChange = { description = it }, placeholder = "A short description…", multiline = true)
            }

            // Tags
            Labeled("Tags", top = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (tags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            tags.forEach { t ->
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(p.accentSoft, CircleShape)
                                        .border(0.5.dp, p.accentLine, CircleShape)
                                        .clickable { tags.remove(t) }
                                        .padding(start = 11.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(t, style = mono(12f), color = p.accent)
                                    Icon("close", size = 13.dp, tint = p.accent)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            PlainTextField(value = tagInput, onValueChange = { tagInput = it }, placeholder = "Add a tag…", onImeAction = { addTag() })
                        }
                        GhostButton(title = "Add", icon = "plus") { addTag() }
                    }
                }
            }

            // Details (dynamic fields)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoLabel("Details")
                Box(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Load template") { showLoadTemplate = true }
                    Pill("Save as") { showSaveTemplate = true }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                fields.forEachIndexed { i, f ->
                    key(f.id) {
                        FieldCard(
                            field = f,
                            onLabel = { fields[i] = fields[i].copy(label = it) },
                            onValue = { fields[i] = fields[i].copy(value = it) },
                            onRemove = { fields.removeAll { it.id == f.id } },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .dashedBorder(p.lineStrong, Radius.sm)
                    .clickable { showAddField = true }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon("plus", size = 18.dp, tint = p.accent)
                Text("Add field", style = sans(15f, FontWeight.SemiBold), color = p.accent)
            }
        }
    }

    if (showAddField) {
        AddFieldSheet(
            onAdd = { label, kind -> fields.add(Field(label = label, value = "", kind = kind)); showAddField = false },
            onClose = { showAddField = false },
        )
    }
    if (showLoadTemplate) {
        LoadTemplateSheet(
            templates = store.templates,
            onApply = { tpl ->
                val existingLabels = fields.map { it.label.lowercase() }.toSet()
                tpl.fields.filterNot { existingLabels.contains(it.label.lowercase()) }
                    .forEach { fields.add(Field(label = it.label, value = "", kind = it.kind)) }
                showLoadTemplate = false
            },
            onClose = { showLoadTemplate = false },
        )
    }
    if (showSaveTemplate) {
        SaveTemplateSheet(
            fieldCount = fields.size,
            onSave = { tname ->
                store.saveTemplate(tname, fields.map { TemplateField(it.label, it.kind) })
                showSaveTemplate = false
            },
            onClose = { showSaveTemplate = false },
        )
    }
}

@Composable
private fun Labeled(label: String, top: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = top), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoLabel(label)
        content()
    }
}

@Composable
private fun Pill(title: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(p.surface2, CircleShape)
            .border(0.5.dp, p.line, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(title, style = sans(12.5f, FontWeight.SemiBold), color = p.accent)
    }
}

@Composable
private fun FieldCard(field: Field, onLabel: (String) -> Unit, onValue: (String) -> Unit, onRemove: () -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier = Modifier.fillMaxWidth().card(p.surface, p.line, Radius.sm).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = field.label,
                    onValueChange = onLabel,
                    textStyle = mono(11f, FontWeight.Bold).copy(color = p.muted),
                    cursorBrush = SolidColor(p.accent),
                    singleLine = true,
                )
                if (field.label.isEmpty()) Text("FIELD NAME", style = mono(11f, FontWeight.Bold), color = p.faint)
            }
            Box(Modifier.clip(CircleShape).border(0.5.dp, p.line, CircleShape).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(field.kind.label.uppercase(), style = mono(9.5f, tracking = 0.5f), color = p.faint)
            }
            Box(Modifier.clip(CircleShape).clickable { onRemove() }.padding(2.dp)) {
                Icon("close", size = 16.dp, tint = p.danger)
            }
        }
        FieldValueEditor(field, onValue)
    }
}

@Composable
private fun FieldValueEditor(field: Field, onValue: (String) -> Unit) {
    val p = LocalPalette.current
    when (field.kind) {
        FieldKind.MULTILINE -> PlainTextField(value = field.value, onValueChange = onValue, placeholder = "Value", multiline = true)
        FieldKind.VALUE -> Row(
            modifier = Modifier.fillMaxWidth().card(p.surface2, p.line, Radius.sm).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("$", style = mono(16f), color = p.faint)
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = field.value,
                    onValueChange = onValue,
                    textStyle = mono(16f).copy(color = p.text),
                    cursorBrush = SolidColor(p.accent),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (field.value.isEmpty()) Text("Value", style = mono(16f), color = p.faint)
            }
        }
        FieldKind.DATE -> PlainTextField(value = field.value, onValueChange = onValue, placeholder = "YYYY-MM")
        FieldKind.CONDITION -> PlainTextField(value = field.value, onValueChange = onValue, placeholder = "Mint / Good / Fair…")
        FieldKind.TEXT -> PlainTextField(value = field.value, onValueChange = onValue, placeholder = "Value")
    }
}

// MARK: - Sheets

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFieldSheet(onAdd: (String, FieldKind) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    var label by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(FieldKind.TEXT) }
    val trimmed = label.trim()

    AppBottomSheet(title = "Add field", onClose = onClose, skipPartiallyExpanded = true) {
        Box(Modifier.padding(bottom = 8.dp)) { MonoLabel("Field name") }
        PlainTextField(value = label, onValueChange = { label = it }, placeholder = "e.g. Serial number")
        Box(Modifier.padding(top = 18.dp, bottom = 8.dp)) { MonoLabel("Type") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            FieldKind.entries.forEach { k ->
                Chip(text = k.label, active = kind == k) { kind = k }
            }
        }
        Box(Modifier.padding(top = 22.dp)) {
            PrimaryButton(title = "Add field", icon = "plus", disabled = trimmed.isEmpty()) { onAdd(trimmed, kind) }
        }
    }
}

@Composable
private fun LoadTemplateSheet(templates: List<Template>, onApply: (Template) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    AppBottomSheet(title = "Load template", onClose = onClose, skipPartiallyExpanded = true) {
        Text(
            "Adds the template's fields to this item (existing fields are kept).",
            style = sans(13.5f),
            color = p.muted,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        if (templates.isEmpty()) {
            Text(
                "No templates yet. Use \"Save as\" on an item with fields.",
                style = sans(14f),
                color = p.faint,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            templates.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .card(p.surface2, p.line, Radius.sm)
                        .clickable { onApply(t) }
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(t.name, style = sans(15.5f, FontWeight.SemiBold), color = p.text)
                        Text(t.fields.joinToString(" · ") { it.label }, style = mono(10.5f), color = p.faint, maxLines = 1)
                    }
                    Icon("plus", size = 18.dp, tint = p.accent)
                }
            }
        }
    }
}

@Composable
private fun SaveTemplateSheet(fieldCount: Int, onSave: (String) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()
    AppBottomSheet(title = "Save as template", onClose = onClose, skipPartiallyExpanded = true) {
        Text(
            "Saves the current field structure ($fieldCount fields) for reuse on new items.",
            style = sans(13.5f),
            color = p.muted,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Box(Modifier.padding(bottom = 8.dp)) { MonoLabel("Template name") }
        PlainTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Vintage camera")
        Box(Modifier.padding(top = 22.dp)) {
            PrimaryButton(title = "Save template", icon = "check", disabled = trimmed.isEmpty() || fieldCount == 0) { onSave(trimmed) }
        }
    }
}
