//
//  RecycleBinView.swift
//  collector
//
//  The recycle bin: restore or permanently delete soft-deleted collections/items.
//

import SwiftUI

struct RecycleBinView: View {
    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss

    @State private var confirmEmpty = false

    var body: some View {
        VStack(spacing: 0) {
            topBar

            if store.trash.isEmpty {
                ScrollView {
                    EmptyStateView(
                        icon: "trash",
                        title: "Recycle bin is empty",
                        sub: "Deleted collections and items are kept here until you remove them permanently."
                    )
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        if !store.trash.collections.isEmpty {
                            MonoLabel(text: "Collections")
                                .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 8)
                            VStack(spacing: 8) {
                                ForEach(store.trash.collections) { tc in
                                    trashRow(
                                        icon: tc.collection.icon,
                                        title: tc.collection.name,
                                        subtitle: "Collection · \(tc.collection.items.count) items · \(deletedAgo(tc.deletedAt))",
                                        restore: { store.restoreCollection(tc.id) },
                                        purge: { store.purgeCollection(tc.id) }
                                    )
                                }
                            }
                            .padding(.horizontal, 20)
                        }

                        if !store.trash.items.isEmpty {
                            MonoLabel(text: "Items")
                                .padding(.horizontal, 20).padding(.top, 18).padding(.bottom, 8)
                            VStack(spacing: 8) {
                                ForEach(store.trash.items) { ti in
                                    trashRow(
                                        icon: ti.collectionIcon,
                                        title: ti.item.name.isEmpty ? "Untitled" : ti.item.name,
                                        subtitle: "from \(ti.collectionName) · \(deletedAgo(ti.deletedAt))",
                                        restore: { store.restoreItem(ti.id) },
                                        purge: { store.purgeItem(ti.id) }
                                    )
                                }
                            }
                            .padding(.horizontal, 20)
                        }

                        GhostButton(title: "Empty recycle bin", icon: "trash", danger: true, expands: true) {
                            confirmEmpty = true
                        }
                        .padding(20)
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .background(p.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .confirmationDialog(
            "Empty the recycle bin? This permanently deletes everything in it.",
            isPresented: $confirmEmpty,
            titleVisibility: .visible
        ) {
            Button("Delete all permanently", role: .destructive) { store.emptyTrash() }
            Button("Cancel", role: .cancel) {}
        }
    }

    // MARK: - Pieces

    private var topBar: some View {
        HStack {
            Button { dismiss() } label: {
                HStack(spacing: 1) {
                    Icon(name: "chevron-left", size: 22, weight: .semibold)
                    Text("Settings").font(.ui(16))
                }
                .foregroundColor(p.accent)
            }
            .buttonStyle(.plain)

            Spacer()
            Text("Recycle Bin").font(.serif(18)).foregroundColor(p.text)
            Spacer()
            Color.clear.frame(width: 64, height: 1) // balance the back button
        }
        .padding(.horizontal, 12)
        .padding(.top, 6)
        .padding(.bottom, 8)
        .overlay(Rectangle().fill(p.line).frame(height: 0.5), alignment: .bottom)
    }

    private func trashRow(icon: String, title: String, subtitle: String,
                          restore: @escaping () -> Void, purge: @escaping () -> Void) -> some View {
        HStack(spacing: 11) {
            Thumb(icon: icon, size: 42)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.serif(16)).foregroundColor(p.text).lineLimit(1)
                Text(subtitle).font(.mono(10.5)).foregroundColor(p.muted).lineLimit(1)
            }
            Spacer(minLength: 6)
            Button(action: restore) {
                Text("Restore").font(.ui(13, .semibold)).foregroundColor(p.accent)
                    .padding(.horizontal, 10).padding(.vertical, 6)
                    .background(Capsule().fill(p.accentSoft))
            }
            .buttonStyle(.plain)
            Button(action: purge) {
                Icon(name: "trash", size: 18).foregroundColor(p.danger)
                    .frame(width: 30, height: 30)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Delete permanently")
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
    }

    private func deletedAgo(_ date: Date) -> String {
        let f = RelativeDateTimeFormatter()
        f.unitsStyle = .abbreviated
        return "deleted " + f.localizedString(for: date, relativeTo: Date())
    }
}
