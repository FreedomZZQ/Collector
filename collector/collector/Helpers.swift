//
//  Helpers.swift
//  collector
//
//  Value/format helpers ported from the design bundle (ui.jsx / screens).
//

import SwiftUI

/// Numeric value parsed from an item's `value`-kind field (digits + dot only).
func parseValue(_ item: Item) -> Double {
    guard let f = item.fields.first(where: { $0.kind == .value }) else { return 0 }
    let cleaned = f.value.filter { $0.isNumber || $0 == "." }
    return Double(cleaned) ?? 0
}

func collectionValue(_ c: ItemCollection) -> Double {
    c.items.reduce(0) { $0 + parseValue($1) }
}

private let moneyFormatter: NumberFormatter = {
    let f = NumberFormatter()
    f.numberStyle = .decimal
    f.maximumFractionDigits = 0
    f.usesGroupingSeparator = true
    f.locale = Locale(identifier: "en_US")
    return f
}()

func money(_ n: Double) -> String {
    let s = moneyFormatter.string(from: NSNumber(value: n.rounded())) ?? "0"
    return "$" + s
}

func fieldByKind(_ item: Item, _ kind: FieldKind) -> Field? {
    item.fields.first { $0.kind == kind }
}

/// All tags across collections, sorted by descending frequency.
func allTags(_ cols: [ItemCollection]) -> [(tag: String, count: Int)] {
    var counts: [String: Int] = [:]
    for c in cols {
        for it in c.items {
            for t in it.tags { counts[t, default: 0] += 1 }
        }
    }
    return counts.sorted { $0.value > $1.value }.map { (tag: $0.key, count: $0.value) }
}

/// "2024-06" → "Jun 2024". Passes through anything that isn't YYYY-MM.
func fmtDate(_ s: String) -> String {
    let parts = s.split(separator: "-")
    guard parts.count >= 2, let month = Int(parts[1]), (1...12).contains(month) else { return s }
    let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    return "\(months[month - 1]) \(parts[0])"
}

/// Condition → swatch color (mint/excellent green, good indigo, fair yellow, poor orange).
func conditionColor(_ v: String) -> Color {
    let hue: Double
    switch v.lowercased().trimmingCharacters(in: .whitespaces) {
    case "mint", "excellent": hue = 150
    case "good": hue = 244
    case "fair": hue = 60
    case "poor": hue = 25
    default: hue = 244
    }
    return Color(oklch: 0.62, 0.13, hue)
}

/// The "maker"-ish subtitle for an item row (maker/brand field, else first text field).
func makerSubtitle(_ item: Item) -> String? {
    func isMakerLabel(_ f: Field) -> Bool {
        let l = f.label.lowercased()
        return l.contains("maker") || l.contains("brand")
    }
    if let f = item.fields.first(where: isMakerLabel), !f.value.isEmpty {
        return f.value
    }
    if let f = item.fields.first(where: { $0.kind == .text }), !f.value.isEmpty {
        return f.value
    }
    return nil
}
