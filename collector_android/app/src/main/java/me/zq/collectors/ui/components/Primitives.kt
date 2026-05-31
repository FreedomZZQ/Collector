package me.zq.collectors.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zq.collectors.ui.theme.LocalPalette
import me.zq.collectors.ui.theme.Radius
import me.zq.collectors.ui.theme.mono
import me.zq.collectors.ui.theme.sans
import me.zq.collectors.ui.theme.serif

// Shared primitives that recreate the iOS UI kit (Components.swift).

/** Fill + hairline border in a rounded rect (iOS `.background(...).overlay(strokeBorder)`). */
fun Modifier.card(
    fill: Color,
    border: Color,
    radius: Dp = Radius.r,
    borderWidth: Dp = 0.5.dp,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.background(fill, shape).border(borderWidth, border, shape)
}

// MARK: - Mono caps label

@Composable
fun MonoLabel(text: String, size: Float = 11f, color: Color? = null) {
    val p = LocalPalette.current
    Text(
        text = text.uppercase(),
        style = mono(size, FontWeight.Bold, tracking = size * 0.12f),
        color = color ?: p.faint,
    )
}

// MARK: - Thumbnail tile

@Composable
fun Thumb(icon: String, size: Dp = 52.dp, radius: Dp? = null, accentTint: Boolean = false) {
    val p = LocalPalette.current
    val r = radius ?: (size * 0.26f)
    Box(
        modifier = Modifier
            .size(size)
            .card(if (accentTint) p.accentSoft else p.surface2, p.line, r),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, size = size * 0.46f, tint = if (accentTint) p.accent else p.faint)
    }
}

// MARK: - Tag chip

@Composable
fun Chip(
    text: String,
    active: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val shape = CircleShape
    val base = Modifier
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .background(if (active) p.accent else Color.Transparent, shape)
        .then(if (active) Modifier else Modifier.border(0.5.dp, p.lineStrong, shape))
        .padding(horizontal = if (small) 8.dp else 11.dp, vertical = if (small) 3.dp else 5.dp)
    Box(base) {
        Text(
            text = text.lowercase(),
            style = mono(if (small) 10.5f else 12f),
            color = if (active) p.onAccent else p.muted,
        )
    }
}

// MARK: - Segmented control

data class SegOption<T>(val value: T, val icon: String? = null, val label: String? = null)

@Composable
fun <T> Segmented(value: T, options: List<SegOption<T>>, onChange: (T) -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(p.surface2, CircleShape)
            .border(0.5.dp, p.line, CircleShape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (o in options) {
            val active = o.value == value
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (active) Modifier.shadow(1.dp, CircleShape) else Modifier)
                    .background(if (active) p.surface else Color.Transparent, CircleShape)
                    .clickable { onChange(o.value) }
                    .heightIn(min = 28.dp)
                    .padding(horizontal = if (o.label != null) 14.dp else 11.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (o.icon != null) Icon(o.icon, size = 16.dp, tint = if (active) p.text else p.faint)
                if (o.label != null) {
                    Text(o.label, style = sans(13f, FontWeight.SemiBold), color = if (active) p.text else p.faint)
                }
            }
        }
    }
}

// MARK: - Buttons

@Composable
fun PrimaryButton(
    title: String,
    icon: String? = null,
    expands: Boolean = true,
    disabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(Radius.sm)
    Row(
        modifier = modifier
            .then(if (expands) Modifier.fillMaxWidth() else Modifier)
            .clip(shape)
            .alpha(if (disabled) 0.45f else 1f)
            .background(p.accent, shape)
            .clickable(enabled = !disabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, size = 18.dp, tint = p.onAccent)
        Text(title, style = sans(16f, FontWeight.SemiBold), color = p.onAccent)
    }
}

@Composable
fun GhostButton(
    title: String,
    icon: String? = null,
    danger: Boolean = false,
    expands: Boolean = false,
    leadingAlign: Boolean = false,
    padding: Dp = 12.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val tint = if (danger) p.danger else p.text
    Row(
        modifier = modifier
            .then(if (expands) Modifier.fillMaxWidth() else Modifier)
            .card(p.surface2, p.line, Radius.sm)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = padding),
        horizontalArrangement = if (leadingAlign) Arrangement.spacedBy(7.dp)
        else Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, size = 17.dp, tint = tint)
        Text(title, style = sans(15f, FontWeight.SemiBold), color = tint)
        if (leadingAlign) Spacer(Modifier.weight(1f))
    }
}

/** iOS IconButton — a circular icon button (optionally bordered / active). */
@Composable
fun IconCircleButton(
    name: String,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    active: Boolean = false,
    bordered: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) p.accentSoft else if (bordered) p.surface2 else Color.Transparent, CircleShape)
            .then(if (bordered) Modifier.border(0.5.dp, p.line, CircleShape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(name, size = iconSize, tint = tint ?: if (active) p.accent else p.text)
    }
}

// MARK: - Empty state

@Composable
fun EmptyStateView(
    icon: String,
    title: String,
    sub: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp).card(p.surface2, p.line, 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, size = 28.dp, tint = p.faint)
        }
        Spacer(Modifier.height(2.dp))
        Text(title, style = serif(20f), color = p.text)
        if (sub != null) {
            Text(
                sub,
                style = sans(14f).copy(textAlign = TextAlign.Center, lineHeight = 20.sp),
                color = p.faint,
                modifier = Modifier.width(240.dp),
            )
        }
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}

// MARK: - Large header

@Composable
fun LargeHeader(
    title: String,
    overline: String? = null,
    sub: String? = null,
    right: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                MonoLabel(overline)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                title,
                style = serif(33f, FontWeight.SemiBold, tracking = -0.3f),
                color = p.text,
            )
            if (sub != null) {
                Spacer(Modifier.height(8.dp))
                Text(sub, style = mono(12.5f), color = p.muted)
            }
        }
        if (right != null) right()
    }
}

// MARK: - Search field

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search…",
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .card(p.surface2, p.line, Radius.sm)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon("search", size = 18.dp, tint = p.faint)
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = sans(16f).copy(color = p.text),
                cursorBrush = SolidColor(p.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) Text(placeholder, style = sans(16f), color = p.faint)
        }
        if (value.isNotEmpty()) {
            Box(Modifier.clip(CircleShape).clickable { onValueChange("") }) {
                Icon("close", size = 16.dp, tint = p.faint)
            }
        }
    }
}

// MARK: - Styled text field

@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val style: TextStyle = (if (mono) mono(16f) else sans(16f)).copy(color = p.text)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .card(p.surface2, p.line, Radius.sm)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            cursorBrush = SolidColor(p.accent),
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            maxLines = if (multiline) 6 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction?.invoke() },
                onSend = { onImeAction?.invoke() },
                onGo = { onImeAction?.invoke() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = p.faint))
    }
}

// MARK: - Striped "product photo" placeholder

@Composable
fun HeroStripes(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        drawRect(p.surface2)
        val spacing = 12.dp.toPx()
        val stroke = 1.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color = p.heroStripe,
                start = Offset(x, size.height),
                end = Offset(x + size.height, 0f),
                strokeWidth = stroke,
            )
            x += spacing
        }
    }
}
