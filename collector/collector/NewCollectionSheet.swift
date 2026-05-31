//
//  NewCollectionSheet.swift
//  collector
//
//  Bottom sheet for creating a collection (name + icon picker).
//

import SwiftUI

struct NewCollectionSheet: View {
    let onAdd: (String, String) -> Void
    let onClose: () -> Void

    @Environment(\.palette) private var p
    @State private var name = ""
    @State private var icon = "box"

    private let icons = ["box", "headphones", "pen", "camera", "coin", "tag", "image"]
    private var trimmed: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        SheetScaffold(title: "New collection", onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                MonoLabel(text: "Name").padding(.bottom, 8)
                PlainTextField(placeholder: "e.g. Wristwatches", text: $name)

                MonoLabel(text: "Icon").padding(.top, 18).padding(.bottom, 10)
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 50, maximum: 50), spacing: 10, alignment: .leading)],
                    alignment: .leading,
                    spacing: 10
                ) {
                    ForEach(icons, id: \.self) { ic in
                        iconButton(ic)
                    }
                }

                PrimaryButton(title: "Create collection", icon: "check", disabled: trimmed.isEmpty) {
                    onAdd(trimmed, icon)
                }
                .padding(.top, 24)
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func iconButton(_ ic: String) -> some View {
        let selected = icon == ic
        return Button { icon = ic } label: {
            Icon(name: ic, size: 24, weight: .regular)
                .foregroundColor(selected ? p.accent : p.muted)
                .frame(width: 50, height: 50)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(selected ? p.accentSoft : p.surface2))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(selected ? p.accent : p.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
