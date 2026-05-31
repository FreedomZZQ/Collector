package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import me.zq.collectors.data.Field
import me.zq.collectors.data.FieldKind
import me.zq.collectors.data.fieldByKind
import me.zq.collectors.data.fmtDate
import me.zq.collectors.data.money
import me.zq.collectors.data.parseValue
import me.zq.collectors.ui.components.Chip
import me.zq.collectors.ui.components.HeroStripes
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.IconCircleButton
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.nav.LocalStore
import me.zq.collectors.ui.nav.Routes
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.conditionColor
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(nav: NavHostController, collectionId: String, itemId: String) {
    val p = LocalPalette.current
    val store = LocalStore.current

    val col = store.collection(collectionId)
    val item = store.item(collectionId, itemId)
    if (col == null || item == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    var showMenu by remember { mutableStateOf(false) }

    val value = parseValue(item)
    val cond = fieldByKind(item, FieldKind.CONDITION)
    val date = fieldByKind(item, FieldKind.DATE)
    val notes = item.fields.filter { it.kind == FieldKind.MULTILINE }
    val details = item.fields.filter { it.kind !in setOf(FieldKind.VALUE, FieldKind.CONDITION, FieldKind.DATE, FieldKind.MULTILINE) }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.clickable { nav.popBackStack() }.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon("chevron-left", size = 22.dp, tint = p.accent)
                Text(col.name, style = sans(16f), color = p.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "Edit",
                style = sans(16f, FontWeight.SemiBold),
                color = p.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { nav.navigate(Routes.editorEdit(collectionId, itemId)) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Box {
                IconCircleButton("dots", iconSize = 20.dp) { showMenu = true }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit item", style = sans(15f), color = p.text) },
                        onClick = { showMenu = false; nav.navigate(Routes.editorEdit(collectionId, itemId)) },
                        leadingIcon = { Icon("edit", size = 18.dp, tint = p.text) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete item", style = sans(15f), color = p.danger) },
                        onClick = { showMenu = false; store.deleteItem(collectionId, itemId) },
                        leadingIcon = { Icon("trash", size = 18.dp, tint = p.danger) },
                    )
                }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            // Hero placeholder
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(Radius.r))
                    .card(p.surface2, p.line, Radius.r),
                contentAlignment = Alignment.Center,
            ) {
                HeroStripes(Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(col.icon, size = 54.dp, tint = p.faint)
                    MonoLabel("product photo")
                }
            }

            // Title block
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
                MonoLabel(col.name)
                Text(item.name, style = serif(28f, tracking = -0.3f), color = p.text, modifier = Modifier.padding(top = 6.dp))
                if (item.description.isNotEmpty()) {
                    Text(
                        item.description,
                        style = sans(15.5f).copy(lineHeight = 22.sp),
                        color = p.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // Highlights
            if (value > 0 || cond != null || date != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (value > 0) Highlight("Value", money(value), Modifier.weight(1f))
                    if (cond != null) Highlight("Condition", cond.value, Modifier.weight(1f), dot = conditionColor(cond.value))
                    if (date != null) Highlight("Acquired", fmtDate(date.value), Modifier.weight(1f))
                }
            }

            // Details
            if (details.isNotEmpty()) {
                SectionLabel("Details")
                DetailsCard(details, Modifier.padding(horizontal = 20.dp).padding(top = 10.dp))
            }

            // Notes
            notes.forEach { f ->
                SectionLabel(f.label)
                Text(
                    f.value,
                    style = sans(15.5f).copy(lineHeight = 22.sp),
                    color = p.muted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
                )
            }

            // Tags
            if (item.tags.isNotEmpty()) {
                SectionLabel("Tags")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    item.tags.forEach { Chip(it) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 22.dp)) {
        MonoLabel(text)
    }
}

@Composable
private fun Highlight(label: String, value: String, modifier: Modifier = Modifier, dot: androidx.compose.ui.graphics.Color? = null) {
    val p = LocalPalette.current
    Column(
        modifier = modifier.card(p.surface, p.line, Radius.sm).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MonoLabel(label, size = 9.5f)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(value, style = mono(14f, FontWeight.Bold), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailsCard(details: List<Field>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.r)).card(p.surface, p.line, Radius.r)) {
        details.forEachIndexed { i, f ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(f.label.uppercase(), style = mono(11f, tracking = 0.5f), color = p.faint)
                Box(Modifier.weight(1f))
                Text(
                    f.value.ifEmpty { "—" },
                    style = sans(15f),
                    color = p.text,
                    textAlign = TextAlign.End,
                )
            }
            if (i < details.size - 1) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.line))
        }
    }
}
