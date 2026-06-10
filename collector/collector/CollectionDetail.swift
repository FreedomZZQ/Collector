//
//  CollectionDetail.swift
//  collector
//
//  One collection: search, sort, list/grid of items, plus collection actions.
//

import SwiftUI

// MARK: - Sorting

enum SortKey: String, CaseIterable, Identifiable {
    case name, nameDesc, valueDesc, valueAsc, recent
    var id: String { rawValue }

    var label: String {
        switch self {
        case .name: return "Name (A–Z)"
        case .nameDesc: return "Name (Z–A)"
        case .valueDesc: return "Value (high → low)"
        case .valueAsc: return "Value (low → high)"
        case .recent: return "Recently acquired"
        }
    }
    var short: String {
        switch self {
        case .name: return "Name A–Z"
        case .nameDesc: return "Name Z–A"
        case .valueDesc, .valueAsc: return "Value"
        case .recent: return "Recently"
        }
    }
}

func sortItems(_ items: [Item], _ sort: SortKey) -> [Item] {
    switch sort {
    case .name:
        return items.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    case .nameDesc:
        return items.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedDescending }
    case .valueDesc:
        return items.sorted { parseValue($0) > parseValue($1) }
    case .valueAsc:
        return items.sorted { parseValue($0) < parseValue($1) }
    case .recent:
        let acq: (Item) -> String = { fieldByKind($0, .date)?.value ?? "" }
        return items.sorted { acq($0) > acq($1) }
    }
}

// MARK: - Screen

private enum CollectionMenuAction { case none, export, delete }

struct CollectionDetail: View {
    let collectionID: String

    @EnvironmentObject private var store: CollectorStore
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss

    @State private var q = ""
    @State private var sort: SortKey = .name
    @State private var view: ItemLayout = .list
    @State private var didSeedView = false
    @State private var showSort = false
    @State private var showMenu = false
    @State private var showExport = false
    @State private var newItem = false
    // Menu actions run after the sheet finishes dismissing (avoids a tear-down race).
    @State private var pendingAction: CollectionMenuAction = .none

    private var col: ItemCollection? { store.collection(collectionID) }

    private var items: [Item] {
        guard let col else { return [] }
        var list = col.items
        let s = q.trimmingCharacters(in: .whitespaces).lowercased()
        if !s.isEmpty {
            list = list.filter { it in
                it.name.lowercased().contains(s)
                || it.description.lowercased().contains(s)
                || it.tags.contains { $0.contains(s) }
                || it.fields.contains { $0.value.lowercased().contains(s) }
            }
        }
        return sortItems(list, sort)
    }

    var body: some View {
        Group {
            if let col {
                content(col)
            } else {
                // Collection was deleted — pop back to Library.
                Color.clear.onAppear { dismiss() }
            }
        }
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .fullScreenCover(isPresented: $newItem) {
            ItemEditor(collectionID: collectionID, item: nil)
        }
        .sheet(isPresented: $showExport) {
            ImportExportSheet(scopeCollectionID: collectionID, onClose: { showExport = false })
        }
        .onAppear {
            if !didSeedView { view = settings.defaultView; didSeedView = true }
        }
    }

    @ViewBuilder
    private func content(_ col: ItemCollection) -> some View {
        VStack(spacing: 0) {
            topBar(col)
            collectionHeader(col)
            SearchField(text: $q, placeholder: "Search \(col.name.lowercased())…")
                .padding(.horizontal, 20)
            controlsRow

            if items.isEmpty {
                ScrollView {
                    EmptyStateView(
                        icon: q.isEmpty ? col.icon : "search",
                        title: q.isEmpty ? "Nothing here yet" : "No matches",
                        sub: q.isEmpty ? "Add your first piece to this collection." : "Try a different term.",
                        action: q.isEmpty ? AnyView(PrimaryButton(title: "Add item", icon: "plus", expands: false) { newItem = true }) : nil
                    )
                }
            } else if view == .list {
                List {
                    ForEach(items) { it in
                        ZStack {
                            ItemRow(item: it, icon: col.icon)
                            // Invisible link on top → tap navigates, no disclosure chevron.
                            NavigationLink(value: ItemRef(collectionID: collectionID, itemID: it.id)) {
                                Color.clear
                            }
                            .opacity(0)
                        }
                        .listRowInsets(EdgeInsets(top: 4, leading: 20, bottom: 4, trailing: 20))
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                store.deleteItem(collectionID: collectionID, itemID: it.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .environment(\.defaultMinListRowHeight, 1)
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                        ForEach(items) { it in
                            NavigationLink(value: ItemRef(collectionID: collectionID, itemID: it.id)) {
                                ItemCard(item: it, icon: col.icon)
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                Button(role: .destructive) {
                                    store.deleteItem(collectionID: collectionID, itemID: it.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
                }
            }
        }
        .sheet(isPresented: $showSort) { sortSheet }
        .sheet(isPresented: $showMenu, onDismiss: runPendingAction) { menuSheet(col) }
    }

    private func runPendingAction() {
        switch pendingAction {
        case .export:
            pendingAction = .none
            showExport = true
        case .delete:
            pendingAction = .none
            store.deleteCollection(collectionID)
            // `col` becomes nil → the body's else-branch dismisses this view.
        case .none:
            break
        }
    }

    // MARK: - Pieces

    private func topBar(_ col: ItemCollection) -> some View {
        HStack {
            Button { dismiss() } label: {
                HStack(spacing: 1) {
                    Icon(name: "chevron-left", size: 22, weight: .semibold)
                    Text("Library").font(.ui(16, .medium))
                }
                .foregroundColor(p.accent)
            }
            .buttonStyle(.plain)

            Spacer()

            HStack(spacing: 2) {
                IconButton(name: "plus", iconSize: 20) { newItem = true }
                    .accessibilityLabel("New item")
                IconButton(name: "dots", iconSize: 20) { showMenu = true }
                    .accessibilityLabel("More")
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 6)
        .padding(.bottom, 4)
    }

    private func collectionHeader(_ col: ItemCollection) -> some View {
        HStack(spacing: 11) {
            Thumb(icon: col.icon, size: 40, radius: 11, accentTint: true)
            VStack(alignment: .leading, spacing: 5) {
                Text(col.name).font(.serif(27)).foregroundColor(p.text).lineLimit(1)
                Text("\(col.items.count) items · \(money(collectionValue(col)))")
                    .font(.mono(11)).foregroundColor(p.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 10)
    }

    private var controlsRow: some View {
        HStack {
            Button { showSort = true } label: {
                HStack(spacing: 6) {
                    Icon(name: "sort", size: 15, weight: .medium)
                    Text(SortKey(rawValue: sort.rawValue).map { $0.short } ?? sort.short)
                        .font(.mono(11.5))
                        .tracking(0.5)
                        .textCase(.uppercase)
                }
                .foregroundColor(p.muted)
            }
            .buttonStyle(.plain)

            Spacer()

            Segmented(
                value: view,
                options: [SegOption(value: .list, icon: "list"), SegOption(value: .grid, icon: "grid")],
                onChange: { view = $0 }
            )
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private var sortSheet: some View {
        SheetScaffold(title: "Sort by", onClose: { showSort = false }) {
            VStack(spacing: 0) {
                ForEach(SortKey.allCases) { s in
                    Button {
                        sort = s
                        showSort = false
                    } label: {
                        HStack {
                            Text(s.label).font(.ui(16)).foregroundColor(p.text)
                            Spacer()
                            if sort == s {
                                Icon(name: "check", size: 19, weight: .semibold).foregroundColor(p.accent)
                            }
                        }
                        .padding(.vertical, 15)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .overlay(Rectangle().fill(p.line).frame(height: 0.5), alignment: .bottom)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func menuSheet(_ col: ItemCollection) -> some View {
        SheetScaffold(title: col.name, onClose: { showMenu = false }) {
            VStack(spacing: 10) {
                GhostButton(title: "Export this collection as JSON", icon: "export", expands: true, leadingAlign: true, padding: 15) {
                    pendingAction = .export
                    showMenu = false
                }
                GhostButton(title: "Delete collection", icon: "trash", danger: true, expands: true, leadingAlign: true, padding: 15) {
                    pendingAction = .delete
                    showMenu = false
                }
            }
            .padding(.top, 6)
        }
        .presentationDetents([.medium])
    }
}

// MARK: - Item row / card

struct ItemRow: View {
    let item: Item
    let icon: String
    @Environment(\.palette) private var p

    var body: some View {
        HStack(spacing: 13) {
            if let cover = item.images.first {
                ItemPhoto(name: cover, maxPixel: 300)
                    .frame(width: 50, height: 50)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 13, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
            } else {
                Thumb(icon: icon, size: 50)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name).font(.serif(17)).foregroundColor(p.text).lineLimit(1)
                HStack(spacing: 7) {
                    if let maker = makerSubtitle(item) {
                        Text(maker).font(.ui(12.5)).foregroundColor(p.muted).lineLimit(1)
                    }
                    if let cond = fieldByKind(item, .condition), !cond.value.isEmpty {
                        HStack(spacing: 4) {
                            Circle().fill(conditionColor(cond.value)).frame(width: 6, height: 6)
                            Text(cond.value).font(.mono(10.5)).foregroundColor(p.faint)
                        }
                    }
                }
            }
            Spacer(minLength: 6)
            let v = parseValue(item)
            if v > 0 {
                Text(money(v)).font(.mono(13)).foregroundColor(p.text)
            }
        }
        .padding(11)
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        .contentShape(Rectangle())
    }
}

struct ItemCard: View {
    let item: Item
    let icon: String
    @Environment(\.palette) private var p

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topTrailing) {
                Rectangle()
                    .fill(p.surface2)
                    .aspectRatio(1, contentMode: .fit)
                    .overlay {
                        if let cover = item.images.first {
                            ItemPhoto(name: cover, maxPixel: 600)
                        } else {
                            Icon(name: icon, size: 42).foregroundColor(p.faint)
                        }
                    }
                    .clipped()
                if let cond = fieldByKind(item, .condition), !cond.value.isEmpty {
                    Circle().fill(conditionColor(cond.value))
                        .frame(width: 9, height: 9)
                        .overlay(Circle().strokeBorder(p.surface2, lineWidth: 2))
                        .padding(8)
                }
            }
            .overlay(Rectangle().fill(p.line).frame(height: 0.5), alignment: .bottom)

            VStack(alignment: .leading, spacing: 5) {
                Text(item.name).font(.serif(15.5)).foregroundColor(p.text).lineLimit(1)
                let v = parseValue(item)
                if v > 0 {
                    Text(money(v)).font(.mono(11.5)).foregroundColor(p.muted)
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 10)
            .padding(.bottom, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        .clipShape(RoundedRectangle(cornerRadius: Radius.r, style: .continuous))
        .contentShape(Rectangle())
    }
}
