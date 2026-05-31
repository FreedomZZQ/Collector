package me.zq.collectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.zq.collectors.data.Item
import me.zq.collectors.data.FieldKind
import me.zq.collectors.data.fieldByKind
import me.zq.collectors.data.makerSubtitle
import me.zq.collectors.data.money
import me.zq.collectors.data.parseValue
import me.zq.collectors.ui.components.Icon
import me.zq.collectors.ui.components.Thumb
import me.zq.collectors.ui.components.card
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.conditionColor
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

@Composable
fun ItemRow(item: Item, icon: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .card(p.surface, p.line, Radius.r)
            .padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(icon, size = 50.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = serif(17f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                makerSubtitle(item)?.let { maker ->
                    Text(maker, style = sans(12.5f), color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val cond = fieldByKind(item, FieldKind.CONDITION)
                if (cond != null && cond.value.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(conditionColor(cond.value)))
                        Text(cond.value, style = mono(10.5f), color = p.faint)
                    }
                }
            }
        }
        val v = parseValue(item)
        if (v > 0) {
            Text(money(v), style = mono(13f), color = p.text)
        }
    }
}

@Composable
fun ItemCard(item: Item, icon: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r))
            .card(p.surface, p.line, Radius.r),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(p.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, size = 42.dp, tint = p.faint)
            val cond = fieldByKind(item, FieldKind.CONDITION)
            if (cond != null && cond.value.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(conditionColor(cond.value))
                        .border(2.dp, p.surface2, CircleShape),
                )
            }
            // bottom hairline
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(p.line),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(item.name, style = serif(15.5f), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val v = parseValue(item)
            if (v > 0) Text(money(v), style = mono(11.5f), color = p.muted)
        }
    }
}
