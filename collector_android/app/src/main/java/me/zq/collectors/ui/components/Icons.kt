package me.zq.collectors.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import me.zq.collectors.ui.theme.LocalPalette

/** Design line-icon name → Material icon (mirrors the iOS sfSymbol map). */
fun collectorIcon(name: String): ImageVector = when (name) {
    // collection types
    "headphones" -> Icons.Filled.Headphones
    "pen" -> Icons.Filled.Brush
    "camera" -> Icons.Filled.PhotoCamera
    "box" -> Icons.Filled.Inventory2
    "coin" -> Icons.Filled.MonetizationOn
    "tag" -> Icons.Filled.LocalOffer
    "image" -> Icons.Filled.Image
    // ui
    "search" -> Icons.Filled.Search
    "plus" -> Icons.Filled.Add
    "gear" -> Icons.Filled.Settings
    "layers" -> Icons.Filled.Layers
    "chevron-left" -> Icons.Filled.ChevronLeft
    "chevron-right" -> Icons.Filled.ChevronRight
    "chevron-down" -> Icons.Filled.ExpandMore
    "grid" -> Icons.Filled.GridView
    "list" -> Icons.AutoMirrored.Filled.List
    "share" -> Icons.Filled.IosShare
    "import" -> Icons.Filled.FileDownload
    "export" -> Icons.Filled.FileUpload
    "trash" -> Icons.Outlined.Delete
    "edit" -> Icons.Filled.Edit
    "close" -> Icons.Filled.Close
    "calendar" -> Icons.Filled.CalendarMonth
    "check" -> Icons.Filled.Check
    "sliders" -> Icons.Filled.Tune
    "doc" -> Icons.Outlined.Description
    "sun" -> Icons.Filled.LightMode
    "moon" -> Icons.Filled.DarkMode
    "auto" -> Icons.Filled.Contrast
    "sort" -> Icons.Filled.SwapVert
    "info" -> Icons.Outlined.Info
    "folder-plus" -> Icons.Filled.CreateNewFolder
    "dots" -> Icons.Filled.MoreVert // Android idiom: vertical overflow dots
    else -> Icons.Filled.Circle
}

/** Themed icon. `size` is the glyph box size. */
@Composable
fun Icon(
    name: String,
    size: Dp,
    tint: Color = LocalPalette.current.text,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Icon(
        imageVector = collectorIcon(name),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
