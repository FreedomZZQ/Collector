//
//  ItemEditor.swift
//  collector
//
//  New / edit item: photos, name, description, tags, dynamic fields, templates.
//

import SwiftUI
import PhotosUI

struct ItemEditor: View {
    let collectionID: String
    let existing: Item?

    @EnvironmentObject private var store: CollectorStore
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss

    @State private var draft: Item
    @State private var tagInput = ""
    @State private var showAddField = false
    @State private var showLoadTemplate = false
    @State private var showSaveTemplate = false
    @State private var pickerItems: [PhotosPickerItem] = []
    /// Photo files written during this editing session; deleted again on Cancel.
    @State private var sessionPhotos: [String] = []

    init(collectionID: String, item: Item?) {
        self.collectionID = collectionID
        self.existing = item
        _draft = State(initialValue: item ?? Item(name: "", fields: Item.defaultFields()))
    }

    private var trimmedName: String { draft.name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    photosSection
                    labeled("Name", top: 20) {
                        PlainTextField(placeholder: "e.g. Leica M6", text: $draft.name)
                    }
                    labeled("Description", top: 16) {
                        PlainTextField(placeholder: "A short description…", text: $draft.description, multiline: true)
                    }
                    tagsSection
                    detailsSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 60)
            }
        }
        .background(p.bg.ignoresSafeArea())
        .sheet(isPresented: $showAddField) {
            AddFieldSheet { label, kind in
                draft.fields.append(Field(label: label, value: "", kind: kind))
                showAddField = false
            } onClose: { showAddField = false }
        }
        .sheet(isPresented: $showLoadTemplate) {
            LoadTemplateSheet(templates: store.templates, onApply: applyTemplate, onClose: { showLoadTemplate = false })
        }
        .sheet(isPresented: $showSaveTemplate) {
            SaveTemplateSheet(fieldCount: draft.fields.count) { name in
                store.saveTemplate(name: name, fields: draft.fields.map { TemplateField(label: $0.label, kind: $0.kind) })
                showSaveTemplate = false
            } onClose: { showSaveTemplate = false }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack {
            Button {
                // Drop photo files added during this session — the draft that
                // references them is being discarded.
                for name in sessionPhotos { ImageStore.delete(name) }
                dismiss()
            } label: {
                Text("Cancel").font(.ui(16)).foregroundColor(p.muted)
            }
            .buttonStyle(.plain)

            Spacer()
            Text(existing == nil ? "New item" : "Edit item").font(.serif(18)).foregroundColor(p.text)
            Spacer()

            Button {
                guard !trimmedName.isEmpty else { return }
                draft.name = trimmedName
                store.saveItem(collectionID: collectionID, item: draft)
                dismiss()
            } label: {
                Text("Save").font(.ui(16, .bold)).foregroundColor(trimmedName.isEmpty ? p.faint : p.accent)
            }
            .buttonStyle(.plain)
            .disabled(trimmedName.isEmpty)
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 10)
        .overlay(Rectangle().fill(p.line).frame(height: 0.5), alignment: .bottom)
    }

    // MARK: - Photos

    @ViewBuilder
    private var photosSection: some View {
        Group {
            if draft.images.isEmpty {
                PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                    HeroStripes()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(4.0 / 3.0, contentMode: .fit)
                        .overlay(
                            VStack(spacing: 8) {
                                Icon(name: "image", size: 32)
                                Text("Add photos").font(.ui(13, .semibold))
                            }
                            .foregroundColor(p.faint)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: Radius.r, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: Radius.r, style: .continuous)
                                .strokeBorder(p.lineStrong, style: StrokeStyle(lineWidth: 1, dash: [5]))
                        )
                }
                .buttonStyle(.plain)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(draft.images, id: \.self) { name in
                            photoTile(name)
                        }
                        PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                            VStack(spacing: 6) {
                                Icon(name: "plus", size: 20, weight: .semibold)
                                Text("Add").font(.ui(12, .semibold))
                            }
                            .foregroundColor(p.accent)
                            .frame(width: 96, height: 96)
                            .overlay(
                                RoundedRectangle(cornerRadius: Radius.sm, style: .continuous)
                                    .strokeBorder(p.lineStrong, style: StrokeStyle(lineWidth: 1, dash: [5]))
                            )
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.vertical, 2)
                }
            }
        }
        .padding(.top, 18)
        .onChange(of: pickerItems) { importPicked($0) }
    }

    private func photoTile(_ name: String) -> some View {
        ItemPhoto(name: name, maxPixel: 400)
            .frame(width: 96, height: 96)
            .clipShape(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.sm, style: .continuous)
                    .strokeBorder(p.line, lineWidth: 0.5)
            )
            .overlay(alignment: .topTrailing) {
                Button { removePhoto(name) } label: {
                    Icon(name: "close", size: 12, weight: .bold)
                        .foregroundColor(.white)
                        .frame(width: 22, height: 22)
                        .background(Circle().fill(Color.black.opacity(0.55)))
                }
                .buttonStyle(.plain)
                .padding(4)
            }
    }

    /// Re-encode the picked photos into the image store and reference them.
    private func importPicked(_ items: [PhotosPickerItem]) {
        guard !items.isEmpty else { return }
        pickerItems = []
        Task {
            for item in items {
                guard let data = try? await item.loadTransferable(type: Data.self) else { continue }
                guard let name = await Task.detached(priority: .userInitiated, operation: {
                    ImageStore.save(data)
                }).value else { continue }
                draft.images.append(name)
                sessionPhotos.append(name)
            }
        }
    }

    private func removePhoto(_ name: String) {
        draft.images.removeAll { $0 == name }
        // A photo added this session has no other owner — delete the file now.
        // Pre-existing photos stay on disk until Save, in case the user cancels.
        if let i = sessionPhotos.firstIndex(of: name) {
            sessionPhotos.remove(at: i)
            ImageStore.delete(name)
        }
    }

    // MARK: - Tags

    private var tagsSection: some View {
        labeled("Tags", top: 16) {
            VStack(alignment: .leading, spacing: 9) {
                if !draft.tags.isEmpty {
                    FlowLayout(spacing: 7) {
                        ForEach(draft.tags, id: \.self) { t in
                            Button { draft.tags.removeAll { $0 == t } } label: {
                                HStack(spacing: 5) {
                                    Text(t).font(.mono(12))
                                    Icon(name: "close", size: 13, weight: .semibold)
                                }
                                .foregroundColor(p.accent)
                                .padding(.leading, 11).padding(.trailing, 8).padding(.vertical, 5)
                                .background(Capsule().fill(p.accentSoft))
                                .overlay(Capsule().strokeBorder(p.accentLine, lineWidth: 0.5))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                HStack(spacing: 8) {
                    TextField("", text: $tagInput, prompt: Text("Add a tag…").foregroundColor(p.faint))
                        .font(.ui(15)).foregroundColor(p.text)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .onSubmit(addTag)
                        .padding(.horizontal, 14).padding(.vertical, 11)
                        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
                        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
                    GhostButton(title: "Add", icon: "plus") { addTag() }
                }
            }
        }
    }

    private func addTag() {
        let t = tagInput.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if !t.isEmpty, !draft.tags.contains(t) { draft.tags.append(t) }
        tagInput = ""
    }

    // MARK: - Details (dynamic fields)

    private var detailsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                MonoLabel(text: "Details")
                Spacer()
                HStack(spacing: 6) {
                    pillButton("Load template") { showLoadTemplate = true }
                    pillButton("Save as") { showSaveTemplate = true }
                }
            }
            .padding(.top, 26)
            .padding(.bottom, 12)

            VStack(spacing: 14) {
                ForEach($draft.fields) { $field in
                    fieldCard($field)
                }
            }

            Button { showAddField = true } label: {
                HStack(spacing: 8) {
                    Icon(name: "plus", size: 18, weight: .semibold)
                    Text("Add field").font(.ui(15, .semibold))
                }
                .foregroundColor(p.accent)
                .frame(maxWidth: .infinity)
                .padding(14)
                .background(
                    RoundedRectangle(cornerRadius: Radius.sm, style: .continuous)
                        .strokeBorder(p.lineStrong, style: StrokeStyle(lineWidth: 1, dash: [5]))
                )
            }
            .buttonStyle(.plain)
            .padding(.top, 14)
        }
    }

    private func fieldCard(_ field: Binding<Field>) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 8) {
                TextField("Field name", text: field.label)
                    .font(.mono(11, .bold)).foregroundColor(p.muted)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                Text(field.wrappedValue.kind.label.uppercased())
                    .font(.mono(9.5)).tracking(0.5).foregroundColor(p.faint)
                    .padding(.horizontal, 7).padding(.vertical, 2)
                    .overlay(Capsule().strokeBorder(p.line, lineWidth: 0.5))
                Button { draft.fields.removeAll { $0.id == field.wrappedValue.id } } label: {
                    Icon(name: "close", size: 16, weight: .semibold).foregroundColor(p.danger)
                }
                .buttonStyle(.plain)
            }
            fieldValueEditor(field)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
    }

    @ViewBuilder
    private func fieldValueEditor(_ field: Binding<Field>) -> some View {
        let kind = field.wrappedValue.kind
        switch kind {
        case .multiline:
            PlainTextField(placeholder: "Value", text: field.value, multiline: true)
        case .value:
            HStack(spacing: 4) {
                Text("$").font(.mono(16)).foregroundColor(p.faint)
                TextField("Value", text: field.value)
                    .font(.mono(16)).foregroundColor(p.text)
                    .keyboardType(.decimalPad)
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
            .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        case .date:
            PlainTextField(placeholder: "YYYY-MM", text: field.value)
        case .condition:
            PlainTextField(placeholder: "Mint / Good / Fair…", text: field.value)
        case .text:
            PlainTextField(placeholder: "Value", text: field.value)
        }
    }

    private func applyTemplate(_ tpl: Template) {
        // Replace the whole field list with the template's, carrying over values
        // whose labels match (case-insensitively) so typed data survives the swap.
        var existingValues: [String: String] = [:]
        for f in draft.fields where !f.value.isEmpty {
            let key = f.label.lowercased()
            if existingValues[key] == nil { existingValues[key] = f.value }
        }
        draft.fields = tpl.fields.map {
            Field(label: $0.label, value: existingValues[$0.label.lowercased()] ?? "", kind: $0.kind)
        }
        showLoadTemplate = false
    }

    // MARK: - Small helpers

    private func labeled<Content: View>(_ label: String, top: CGFloat, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            MonoLabel(text: label)
            content()
        }
        .padding(.top, top)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func pillButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(.ui(12.5, .semibold)).foregroundColor(p.accent)
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(Capsule().fill(p.surface2))
                .overlay(Capsule().strokeBorder(p.line, lineWidth: 0.5))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Add-field sheet

private struct AddFieldSheet: View {
    let onAdd: (String, FieldKind) -> Void
    let onClose: () -> Void

    @Environment(\.palette) private var p
    @State private var label = ""
    @State private var kind: FieldKind = .text
    private var trimmed: String { label.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        SheetScaffold(title: "Add field", onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                MonoLabel(text: "Field name").padding(.bottom, 8)
                PlainTextField(placeholder: "e.g. Serial number", text: $label)
                MonoLabel(text: "Type").padding(.top, 18).padding(.bottom, 8)
                FlowLayout(spacing: 7) {
                    ForEach(FieldKind.allCases, id: \.self) { k in
                        Chip(text: k.label, active: kind == k) { kind = k }
                    }
                }
                PrimaryButton(title: "Add field", icon: "plus", disabled: trimmed.isEmpty) {
                    onAdd(trimmed, kind)
                }
                .padding(.top, 22)
            }
        }
        .presentationDetents([.medium])
    }
}

// MARK: - Load-template sheet

private struct LoadTemplateSheet: View {
    let templates: [Template]
    let onApply: (Template) -> Void
    let onClose: () -> Void
    @Environment(\.palette) private var p

    var body: some View {
        SheetScaffold(title: "Load template", onClose: onClose) {
            VStack(alignment: .leading, spacing: 9) {
                Text("Replaces this item's fields with the template's. Values of fields with matching names are kept.")
                    .font(.ui(13.5)).foregroundColor(p.muted).lineSpacing(2)
                    .padding(.bottom, 4)

                if templates.isEmpty {
                    Text("No templates yet. Use \"Save as\" on an item with fields.")
                        .font(.ui(14)).foregroundColor(p.faint)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 12)
                }

                ForEach(templates) { t in
                    Button { onApply(t) } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(t.name).font(.ui(15.5, .semibold)).foregroundColor(p.text)
                                Text(t.fields.map { $0.label }.joined(separator: " · "))
                                    .font(.mono(10.5)).foregroundColor(p.faint).lineLimit(1)
                            }
                            Spacer(minLength: 12)
                            Icon(name: "plus", size: 18, weight: .medium).foregroundColor(p.accent)
                        }
                        .padding(.horizontal, 15).padding(.vertical, 13)
                        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
                        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

// MARK: - Save-template sheet

private struct SaveTemplateSheet: View {
    let fieldCount: Int
    let onSave: (String) -> Void
    let onClose: () -> Void

    @Environment(\.palette) private var p
    @State private var name = ""
    private var trimmed: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        SheetScaffold(title: "Save as template", onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                Text("Saves the current field structure (\(fieldCount) fields) for reuse on new items.")
                    .font(.ui(13.5)).foregroundColor(p.muted).lineSpacing(2)
                    .padding(.bottom, 14)
                MonoLabel(text: "Template name").padding(.bottom, 8)
                PlainTextField(placeholder: "e.g. Vintage camera", text: $name)
                PrimaryButton(title: "Save template", icon: "check", disabled: trimmed.isEmpty || fieldCount == 0) {
                    onSave(trimmed)
                }
                .padding(.top, 22)
            }
        }
        .presentationDetents([.medium])
    }
}
