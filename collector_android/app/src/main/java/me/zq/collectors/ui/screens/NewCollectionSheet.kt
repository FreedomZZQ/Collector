package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.zq.collectors.ui.components.AppBottomSheet
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.MonoLabel
import me.zq.collectors.ui.components.PlainTextField
import me.zq.collectors.ui.components.PrimaryButton
import me.zq.collectors.ui.theme.LocalPalette

private val ICONS = listOf("box", "headphones", "pen", "camera", "coin", "tag", "image")

@Composable
fun NewCollectionSheet(onAdd: (String, String) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("box") }
    val trimmed = name.trim()

    AppBottomSheet(title = "New collection", onClose = onClose, skipPartiallyExpanded = true) {
        MonoLabel("Name")
        Box(Modifier.padding(top = 8.dp)) {
            PlainTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Wristwatches")
        }

        Box(Modifier.padding(top = 18.dp, bottom = 10.dp)) { MonoLabel("Icon") }
        // 7 icons across two rows (FlowRow-like wrap via fixed columns).
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ICONS.chunked(6).forEach { rowIcons ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowIcons.forEach { ic ->
                        val selected = icon == ic
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) p.accentSoft else p.surface2, RoundedCornerShape(14.dp))
                                .border(1.dp, if (selected) p.accent else p.line, RoundedCornerShape(14.dp))
                                .clickable { icon = ic },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(ic, size = 24.dp, tint = if (selected) p.accent else p.muted)
                        }
                    }
                }
            }
        }

        Box(Modifier.padding(top = 24.dp)) {
            PrimaryButton(title = "Create collection", icon = "check", disabled = trimmed.isEmpty()) {
                onAdd(trimmed, icon)
            }
        }
    }
}
