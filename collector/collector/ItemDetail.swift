//
//  ItemDetail.swift
//  collector
//
//  Read view for a single item: hero, highlights, details, notes, tags.
//

import SwiftUI

private enum ItemMenuAction { case none, edit, delete }

struct ItemDetail: View {
    let collectionID: String
    let itemID: String

    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss

    @State private var showMenu = false
    @State private var editing = false
    // Menu actions run after the sheet finishes dismissing (avoids a tear-down race).
    @State private var pendingAction: ItemMenuAction = .none

    private var col: ItemCollection? { store.collection(collectionID) }
    private var item: Item? { store.item(collectionID: collectionID, itemID: itemID) }

    var body: some View {
        Group {
            if let col, let item {
                content(col: col, item: item)
            } else {
                Color.clear.onAppear { dismiss() }
            }
        }
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        .fullScreenCover(isPresented: $editing) {
            ItemEditor(collectionID: collectionID, item: item)
        }
    }

    @ViewBuilder
    private func content(col: ItemCollection, item: Item) -> some View {
        let val = parseValue(item)
        let cond = fieldByKind(item, .condition)
        let date = fieldByKind(item, .date)
        let notes = item.fields.filter { $0.kind == .multiline }
        let details = item.fields.filter { ![.value, .condition, .date, .multiline].contains($0.kind) }

        VStack(spacing: 0) {
            topBar(col: col, item: item)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HeroStripes()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(4.0 / 3.0, contentMode: .fit)
                        .overlay(
                            VStack(spacing: 8) {
                                Icon(name: col.icon, size: 54)
                                Text("product photo").font(.mono(10.5)).tracking(0.8).textCase(.uppercase)
                            }
                            .foregroundColor(p.faint)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: Radius.r, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
                        .padding(.horizontal, 20)
                        .padding(.top, 4)

                    VStack(alignment: .leading, spacing: 0) {
                        MonoLabel(text: col.name)
                        Text(item.name).font(.serif(28)).foregroundColor(p.text).tracking(-0.3)
                            .padding(.top, 6)
                        if !item.description.isEmpty {
                            Text(item.description).font(.ui(15.5)).foregroundColor(p.muted)
                                .lineSpacing(3)
                                .padding(.top, 12)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)

                    if val > 0 || cond != nil || date != nil {
                        HStack(spacing: 9) {
                            if val > 0 { highlight(label: "Value", value: money(val)) }
                            if let cond { highlight(label: "Condition", value: cond.value, color: conditionColor(cond.value)) }
                            if let date { highlight(label: "Acquired", value: fmtDate(date.value)) }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 18)
                    }

                    if !details.isEmpty {
                        sectionLabel("Details").padding(.top, 22)
                        detailsCard(details).padding(.horizontal, 20).padding(.top, 10)
                    }

                    ForEach(notes) { f in
                        sectionLabel(f.label).padding(.top, 22)
                        Text(f.value).font(.ui(15.5)).foregroundColor(p.muted).lineSpacing(3)
                            .padding(.horizontal, 20).padding(.top, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    if !item.tags.isEmpty {
                        sectionLabel("Tags").padding(.top, 22)
                        FlowLayout(spacing: 7) {
                            ForEach(item.tags, id: \.self) { t in Chip(text: t) }
                        }
                        .padding(.horizontal, 20).padding(.top, 10)
                    }
                }
                .padding(.bottom, 40)
            }
        }
        .sheet(isPresented: $showMenu, onDismiss: runPendingAction) { menuSheet(item) }
    }

    private func runPendingAction() {
        switch pendingAction {
        case .edit:
            pendingAction = .none
            editing = true
        case .delete:
            pendingAction = .none
            store.deleteItem(collectionID: collectionID, itemID: itemID)
            // `item` becomes nil → the body's else-branch dismisses this view.
        case .none:
            break
        }
    }

    // MARK: - Pieces

    private func topBar(col: ItemCollection, item: Item) -> some View {
        HStack {
            Button { dismiss() } label: {
                HStack(spacing: 1) {
                    Icon(name: "chevron-left", size: 22, weight: .semibold)
                    Text(col.name).font(.ui(16)).lineLimit(1)
                }
                .foregroundColor(p.accent)
            }
            .buttonStyle(.plain)

            Spacer()

            HStack(spacing: 2) {
                Button { editing = true } label: {
                    Text("Edit").font(.ui(16, .semibold)).foregroundColor(p.accent)
                        .padding(.horizontal, 8).padding(.vertical, 6)
                }
                .buttonStyle(.plain)
                IconButton(name: "dots", iconSize: 20) { showMenu = true }
                    .accessibilityLabel("More")
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 6)
        .padding(.bottom, 6)
    }

    private func sectionLabel(_ text: String) -> some View {
        MonoLabel(text: text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
    }

    private func highlight(label: String, value: String, color: Color? = nil) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            MonoLabel(text: label, size: 9.5)
            HStack(spacing: 6) {
                if let color { Circle().fill(color).frame(width: 8, height: 8) }
                Text(value).font(.mono(14, .bold)).foregroundColor(p.text).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 13)
        .padding(.vertical, 11)
        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
    }

    private func detailsCard(_ details: [Field]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(details.enumerated()), id: \.element.id) { i, f in
                HStack(alignment: .firstTextBaseline, spacing: 16) {
                    Text(f.label.uppercased()).font(.mono(11)).tracking(0.5).foregroundColor(p.faint)
                    Spacer(minLength: 16)
                    Text(f.value.isEmpty ? "—" : f.value).font(.ui(15)).foregroundColor(p.text)
                        .multilineTextAlignment(.trailing)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                if i < details.count - 1 {
                    Rectangle().fill(p.line).frame(height: 0.5)
                }
            }
        }
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        .clipShape(RoundedRectangle(cornerRadius: Radius.r, style: .continuous))
    }

    private func menuSheet(_ item: Item) -> some View {
        SheetScaffold(title: item.name, onClose: { showMenu = false }) {
            VStack(spacing: 10) {
                GhostButton(title: "Edit item", icon: "edit", expands: true, leadingAlign: true, padding: 15) {
                    pendingAction = .edit
                    showMenu = false
                }
                GhostButton(title: "Delete item", icon: "trash", danger: true, expands: true, leadingAlign: true, padding: 15) {
                    pendingAction = .delete
                    showMenu = false
                }
            }
            .padding(.top, 6)
        }
        .presentationDetents([.medium])
    }
}
