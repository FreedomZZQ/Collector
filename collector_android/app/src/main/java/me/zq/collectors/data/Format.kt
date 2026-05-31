package me.zq.collectors.data

import java.text.NumberFormat
import java.util.Locale

// Value/format helpers ported from the iOS Helpers.swift (which mirror the
// design bundle). Pure data — no Compose/Color dependency.

/** Numeric value parsed from an item's `value`-kind field (digits + dot only). */
fun parseValue(item: Item): Double {
    val f = item.fields.firstOrNull { it.kind == FieldKind.VALUE } ?: return 0.0
    val cleaned = f.value.filter { it.isDigit() || it == '.' }
    return cleaned.toDoubleOrNull() ?: 0.0
}

fun collectionValue(c: ItemCollection): Double = c.items.sumOf { parseValue(it) }

private val moneyFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 0
    isGroupingUsed = true
}

fun money(n: Double): String = "$" + moneyFormatter.format(Math.round(n))

fun fieldByKind(item: Item, kind: FieldKind): Field? = item.fields.firstOrNull { it.kind == kind }

/** All tags across collections, sorted by descending frequency. */
fun allTags(cols: List<ItemCollection>): List<Pair<String, Int>> {
    val counts = LinkedHashMap<String, Int>()
    for (c in cols) for (it in c.items) for (t in it.tags) counts[t] = (counts[t] ?: 0) + 1
    return counts.entries.sortedByDescending { it.value }.map { it.key to it.value }
}

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** "2024-06" → "Jun 2024". Passes through anything that isn't YYYY-MM. */
fun fmtDate(s: String): String {
    val parts = s.split("-")
    if (parts.size < 2) return s
    val month = parts[1].toIntOrNull() ?: return s
    if (month !in 1..12) return s
    return "${MONTHS[month - 1]} ${parts[0]}"
}

/** The "maker"-ish subtitle for an item row (maker/brand field, else first text field). */
fun makerSubtitle(item: Item): String? {
    fun isMakerLabel(f: Field): Boolean {
        val l = f.label.lowercase()
        return l.contains("maker") || l.contains("brand")
    }
    item.fields.firstOrNull { isMakerLabel(it) && it.value.isNotEmpty() }?.let { return it.value }
    item.fields.firstOrNull { it.kind == FieldKind.TEXT && it.value.isNotEmpty() }?.let { return it.value }
    return null
}
