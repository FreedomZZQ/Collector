//
//  SearchTab.swift
//  collector
//
//  Cross-collection search with tag filtering.
//

import SwiftUI

struct SearchTab: View {
    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p

    @State private var q = ""
    @State private var selectedTags: Set<String> = []

    private var flat: [(col: ItemCollection, item: Item)] {
        store.collections.flatMap { c in c.items.map { (col: c, item: $0) } }
    }

    private var results: [(col: ItemCollection, item: Item)] {
        let s = q.trimmingCharacters(in: .whitespaces).lowercased()
        return flat.filter { pair in
            let it = pair.item
            let matchesQuery = s.isEmpty
                || it.name.lowercased().contains(s)
                || it.description.lowercased().contains(s)
                || it.tags.contains { $0.contains(s) }
                || it.fields.contains { $0.value.lowercased().contains(s) }
            let matchesTags = selectedTags.isEmpty || selectedTags.allSatisfy { it.tags.contains($0) }
            return matchesQuery && matchesTags
        }
    }

    private var active: Bool { !q.trimmingCharacters(in: .whitespaces).isEmpty || !selectedTags.isEmpty }

    var body: some View {
        VStack(spacing: 0) {
            LargeHeader(title: "Search", sub: "across \(flat.count) pieces")

            SearchField(text: $q, placeholder: "Name, maker, tag, note…")
                .padding(.horizontal, 20)

            let tags = allTags(store.collections)
            if !tags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 7) {
                        ForEach(tags, id: \.tag) { entry in
                            Chip(text: "\(entry.tag) \(entry.count)", active: selectedTags.contains(entry.tag)) {
                                toggle(entry.tag)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.vertical, 14)
            }

            if !active {
                ScrollView {
                    EmptyStateView(
                        icon: "search",
                        title: "Find anything",
                        sub: "Search by name, maker, tag, or note — or tap a tag above to filter across every collection."
                    )
                }
            } else if results.isEmpty {
                ScrollView {
                    EmptyStateView(icon: "search", title: "No matches", sub: "Try a different term or clear a tag.")
                }
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(results, id: \.item.id) { pair in
                            NavigationLink(value: ItemRef(collectionID: pair.col.id, itemID: pair.item.id)) {
                                ItemRow(item: pair.item, icon: pair.col.icon)
                                    .overlay(alignment: .topTrailing) {
                                        Text(pair.col.name.uppercased())
                                            .font(.mono(9)).tracking(0.5).foregroundColor(p.faint)
                                            .padding(.horizontal, 6).padding(.vertical, 2)
                                            .background(Capsule().fill(p.surface2))
                                            .padding(.top, 11).padding(.trailing, 13)
                                    }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 14)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private func toggle(_ tag: String) {
        if selectedTags.contains(tag) { selectedTags.remove(tag) } else { selectedTags.insert(tag) }
    }
}
