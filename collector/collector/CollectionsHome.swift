//
//  CollectionsHome.swift
//  collector
//
//  Library tab: overview header, estimated-value card, collection list.
//

import SwiftUI

struct CollectionsHome: View {
    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p

    @State private var showNewCollection = false
    @State private var showData = false

    var body: some View {
        List {
            VStack(alignment: .leading, spacing: 0) {
                LargeHeader(
                    overline: "Local library",
                    title: "Collector",
                    sub: "\(store.collections.count) collections · \(store.totalItems) pieces"
                ) {
                    HStack(spacing: 4) {
                        IconButton(name: "share", iconSize: 19, bordered: true) { showData = true }
                        IconButton(name: "plus", iconSize: 19, bordered: true) { showNewCollection = true }
                    }
                }

                valueCard
                    .padding(.horizontal, 20)
                    .padding(.top, 14)
                    .padding(.bottom, 6)

                MonoLabel(text: "Collections")
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 8)
            }
            .listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)

            ForEach(store.collections) { c in
                ZStack {
                    collectionRow(c)
                    NavigationLink(value: CollectionRef(id: c.id)) { Color.clear }.opacity(0)
                }
                .listRowInsets(EdgeInsets(top: 5, leading: 20, bottom: 5, trailing: 20))
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        store.deleteCollection(c.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }

            newCollectionButton
                .listRowInsets(EdgeInsets(top: 7, leading: 20, bottom: 24, trailing: 20))
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.defaultMinListRowHeight, 1)
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showNewCollection) {
            NewCollectionSheet(
                onAdd: { name, icon in
                    store.addCollection(name: name, icon: icon)
                    showNewCollection = false
                },
                onClose: { showNewCollection = false }
            )
        }
        .sheet(isPresented: $showData) {
            ImportExportSheet(scopeCollectionID: nil, onClose: { showData = false })
        }
    }

    // MARK: - Pieces

    private var valueCard: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 4) {
                MonoLabel(text: "Estimated value", color: p.accent.opacity(0.9))
                Text(money(store.totalValue))
                    .font(.serif(30))
                    .foregroundColor(p.text)
                    .tracking(-0.3)
            }
            Spacer()
            Icon(name: "coin", size: 26)
                .foregroundColor(p.accent)
                .opacity(0.85)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.accentSoft))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.accentLine, lineWidth: 0.5))
    }

    private func collectionRow(_ c: ItemCollection) -> some View {
        HStack(spacing: 14) {
            Thumb(icon: c.icon, size: 50, accentTint: true)
            VStack(alignment: .leading, spacing: 3) {
                Text(c.name)
                    .font(.serif(18))
                    .foregroundColor(p.text)
                    .lineLimit(1)
                Text("\(c.items.count) \(c.items.count == 1 ? "item" : "items") · \(money(collectionValue(c)))")
                    .font(.mono(11.5))
                    .foregroundColor(p.muted)
            }
            Spacer(minLength: 8)
            Icon(name: "chevron-right", size: 18)
                .foregroundColor(p.faint)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        .contentShape(Rectangle())
    }

    private var newCollectionButton: some View {
        Button { showNewCollection = true } label: {
            HStack(spacing: 9) {
                Icon(name: "folder-plus", size: 19, weight: .medium)
                Text("New collection").font(.ui(15, .semibold))
            }
            .foregroundColor(p.muted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(
                RoundedRectangle(cornerRadius: Radius.r, style: .continuous)
                    .strokeBorder(p.lineStrong, style: StrokeStyle(lineWidth: 1, dash: [5]))
            )
        }
        .buttonStyle(.plain)
        .padding(.top, 2)
    }
}
