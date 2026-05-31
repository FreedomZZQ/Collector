//
//  Components.swift
//  collector
//
//  Shared SwiftUI primitives that recreate the design bundle's UI kit.
//

import SwiftUI

// MARK: - Icon (design line-icons → SF Symbols)

func sfSymbol(_ name: String) -> String {
    switch name {
    // collection types
    case "headphones": return "headphones"
    case "pen": return "pencil.tip"
    case "camera": return "camera"
    case "box": return "shippingbox"
    case "coin": return "dollarsign.circle"
    case "tag": return "tag"
    case "image": return "photo"
    // ui
    case "search": return "magnifyingglass"
    case "plus": return "plus"
    case "gear": return "gearshape"
    case "layers": return "square.stack.3d.up"
    case "chevron-left": return "chevron.left"
    case "chevron-right": return "chevron.right"
    case "chevron-down": return "chevron.down"
    case "grid": return "square.grid.2x2"
    case "list": return "list.bullet"
    case "share": return "square.and.arrow.up"
    case "import": return "square.and.arrow.down"
    case "export": return "square.and.arrow.up"
    case "trash": return "trash"
    case "edit": return "pencil"
    case "close": return "xmark"
    case "calendar": return "calendar"
    case "check": return "checkmark"
    case "sliders": return "slider.horizontal.3"
    case "doc": return "doc"
    case "sun": return "sun.max"
    case "moon": return "moon"
    case "auto": return "circle.lefthalf.filled"
    case "sort": return "arrow.up.arrow.down"
    case "info": return "info.circle"
    case "folder-plus": return "folder.badge.plus"
    case "dots": return "ellipsis"
    default: return "circle"
    }
}

struct Icon: View {
    let name: String
    var size: CGFloat = 24
    var weight: Font.Weight = .regular

    var body: some View {
        Image(systemName: sfSymbol(name))
            .font(.system(size: size * 0.82, weight: weight))
            .frame(width: size, height: size)
    }
}

// MARK: - Mono caps label

struct MonoLabel: View {
    let text: String
    var size: CGFloat = 11
    var color: Color? = nil
    @Environment(\.palette) private var p

    var body: some View {
        Text(text.uppercased())
            .font(.mono(size, .bold))
            .tracking(size * 0.12)
            .foregroundColor(color ?? p.faint)
    }
}

// MARK: - Thumbnail tile

struct Thumb: View {
    let icon: String
    var size: CGFloat = 52
    var radius: CGFloat? = nil
    var accentTint: Bool = false
    @Environment(\.palette) private var p

    var body: some View {
        let r = radius ?? size * 0.26
        RoundedRectangle(cornerRadius: r, style: .continuous)
            .fill(accentTint ? p.accentSoft : p.surface2)
            .overlay(RoundedRectangle(cornerRadius: r, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
            .overlay(Icon(name: icon, size: size * 0.46).foregroundColor(accentTint ? p.accent : p.faint))
            .frame(width: size, height: size)
    }
}

// MARK: - Tag chip

struct Chip: View {
    let text: String
    var active: Bool = false
    var small: Bool = false
    var action: (() -> Void)? = nil
    @Environment(\.palette) private var p

    var body: some View {
        let label = Text(text.lowercased())
            .font(.mono(small ? 10.5 : 12))
            .foregroundColor(active ? p.onAccent : p.muted)
            .padding(.horizontal, small ? 8 : 11)
            .padding(.vertical, small ? 3 : 5)
            .background(Capsule().fill(active ? p.accent : Color.clear))
            .overlay(Capsule().strokeBorder(active ? Color.clear : p.lineStrong, lineWidth: 0.5))

        if let action {
            Button(action: action) { label }.buttonStyle(.plain)
        } else {
            label
        }
    }
}

// MARK: - Segmented control

struct SegOption<T: Hashable> {
    let value: T
    var icon: String? = nil
    var label: String? = nil
}

struct Segmented<T: Hashable>: View {
    let value: T
    let options: [SegOption<T>]
    let onChange: (T) -> Void
    @Environment(\.palette) private var p

    var body: some View {
        HStack(spacing: 2) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, o in
                let active = o.value == value
                Button { onChange(o.value) } label: {
                    HStack(spacing: 5) {
                        if let icon = o.icon { Icon(name: icon, size: 16, weight: .medium) }
                        if let label = o.label { Text(label).font(.ui(13, .semibold)) }
                    }
                    .foregroundColor(active ? p.text : p.faint)
                    .padding(.horizontal, o.label != nil ? 14 : 11)
                    .frame(minHeight: 28)
                    .background(
                        Capsule()
                            .fill(active ? p.surface : Color.clear)
                            .shadow(color: active ? Color.black.opacity(0.12) : Color.clear, radius: 1, y: 1)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(2)
        .background(Capsule().fill(p.surface2))
        .overlay(Capsule().strokeBorder(p.line, lineWidth: 0.5))
    }
}

// MARK: - Buttons

struct PrimaryButton: View {
    let title: String
    var icon: String? = nil
    var expands: Bool = true
    var disabled: Bool = false
    let action: () -> Void
    @Environment(\.palette) private var p

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon { Icon(name: icon, size: 18, weight: .semibold) }
                Text(title).font(.ui(16, .semibold))
            }
            .foregroundColor(p.onAccent)
            .frame(maxWidth: expands ? .infinity : nil)
            .padding(.horizontal, 18)
            .padding(.vertical, 13)
            .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.accent))
            .opacity(disabled ? 0.45 : 1)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }
}

struct GhostButton: View {
    let title: String
    var icon: String? = nil
    var danger: Bool = false
    var expands: Bool = false
    var leadingAlign: Bool = false
    var padding: CGFloat = 12
    let action: () -> Void
    @Environment(\.palette) private var p

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                if let icon { Icon(name: icon, size: 17, weight: .medium) }
                Text(title).font(.ui(15, .semibold))
                if leadingAlign { Spacer(minLength: 0) }
            }
            .foregroundColor(danger ? p.danger : p.text)
            .frame(maxWidth: expands ? .infinity : nil)
            .padding(.horizontal, 16)
            .padding(.vertical, padding)
            .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
            .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
        }
        .buttonStyle(.plain)
    }
}

struct IconButton: View {
    let name: String
    var size: CGFloat = 38
    var iconSize: CGFloat = 20
    var active: Bool = false
    var bordered: Bool = false
    var tint: Color? = nil
    let action: () -> Void
    @Environment(\.palette) private var p

    var body: some View {
        Button(action: action) {
            Icon(name: name, size: iconSize, weight: .medium)
                .foregroundColor(tint ?? (active ? p.accent : p.text))
                .frame(width: size, height: size)
                .background(Circle().fill(active ? p.accentSoft : (bordered ? p.surface2 : Color.clear)))
                .overlay(Circle().strokeBorder(p.line, lineWidth: bordered ? 0.5 : 0))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Empty state

struct EmptyStateView: View {
    let icon: String
    let title: String
    var sub: String? = nil
    var action: AnyView? = nil
    @Environment(\.palette) private var p

    var body: some View {
        VStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(p.surface2)
                .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
                .overlay(Icon(name: icon, size: 28).foregroundColor(p.faint))
                .frame(width: 64, height: 64)
                .padding(.bottom, 8)
            Text(title).font(.serif(20)).foregroundColor(p.text)
            if let sub {
                Text(sub).font(.ui(14)).foregroundColor(p.faint)
                    .multilineTextAlignment(.center).lineSpacing(2).frame(maxWidth: 240)
            }
            if let action { action.padding(.top, 14) }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
        .padding(.vertical, 48)
    }
}

// MARK: - Large header

struct LargeHeader<Right: View>: View {
    var overline: String? = nil
    let title: String
    var sub: String? = nil
    let right: Right
    @Environment(\.palette) private var p

    init(overline: String? = nil, title: String, sub: String? = nil, @ViewBuilder right: () -> Right) {
        self.overline = overline
        self.title = title
        self.sub = sub
        self.right = right()
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                if let overline { MonoLabel(text: overline).padding(.bottom, 6) }
                Text(title).font(.serif(33, .semibold)).foregroundColor(p.text).tracking(-0.3)
                if let sub { Text(sub).font(.mono(12.5)).foregroundColor(p.muted).padding(.top, 8) }
            }
            Spacer(minLength: 0)
            right.padding(.top, 2)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }
}

extension LargeHeader where Right == EmptyView {
    init(overline: String? = nil, title: String, sub: String? = nil) {
        self.init(overline: overline, title: title, sub: sub) { EmptyView() }
    }
}

// MARK: - Search field

struct SearchField: View {
    @Binding var text: String
    var placeholder: String = "Search…"
    @Environment(\.palette) private var p

    var body: some View {
        HStack(spacing: 9) {
            Icon(name: "search", size: 18, weight: .medium).foregroundColor(p.faint)
            TextField("", text: $text, prompt: Text(placeholder).foregroundColor(p.faint))
                .font(.ui(16)).foregroundColor(p.text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !text.isEmpty {
                Button { text = "" } label: {
                    Icon(name: "close", size: 16).foregroundColor(p.faint)
                }.buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
    }
}

// MARK: - Bottom-sheet scaffold

struct SheetScaffold<Content: View>: View {
    let title: String
    let onClose: () -> Void
    let content: Content
    @Environment(\.palette) private var p

    init(title: String, onClose: @escaping () -> Void, @ViewBuilder content: () -> Content) {
        self.title = title
        self.onClose = onClose
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(title).font(.serif(21, .semibold)).foregroundColor(p.text).lineLimit(1)
                Spacer(minLength: 12)
                Button(action: onClose) {
                    Icon(name: "close", size: 18).foregroundColor(p.muted)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(p.surface2))
                }.buttonStyle(.plain)
            }
            .padding(.horizontal, 18)
            .padding(.top, 18)
            .padding(.bottom, 10)

            ScrollView {
                content
                    .padding(.horizontal, 18)
                    .padding(.bottom, 24)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(p.surface.ignoresSafeArea())
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Styled text field

struct PlainTextField: View {
    var placeholder: String
    @Binding var text: String
    var mono: Bool = false
    var multiline: Bool = false
    @Environment(\.palette) private var p

    var body: some View {
        Group {
            if multiline {
                TextField("", text: $text, prompt: Text(placeholder).foregroundColor(p.faint), axis: .vertical)
                    .lineLimit(3...6)
            } else {
                TextField("", text: $text, prompt: Text(placeholder).foregroundColor(p.faint))
            }
        }
        .font(mono ? .mono(16) : .ui(16))
        .foregroundColor(p.text)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).fill(p.surface2))
        .overlay(RoundedRectangle(cornerRadius: Radius.sm, style: .continuous).strokeBorder(p.line, lineWidth: 0.5))
    }
}

// MARK: - Striped "product photo" placeholder

struct HeroStripes: View {
    @Environment(\.palette) private var p

    var body: some View {
        Canvas { ctx, size in
            ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(p.surface2))
            let spacing: CGFloat = 12
            var x: CGFloat = -size.height
            while x < size.width + size.height {
                var line = Path()
                line.move(to: CGPoint(x: x, y: size.height))
                line.addLine(to: CGPoint(x: x + size.height, y: 0))
                ctx.stroke(line, with: .color(p.heroStripe), lineWidth: 1)
                x += spacing
            }
        }
    }
}

// MARK: - Flow layout (wrapping chips)

struct FlowLayout: Layout {
    var spacing: CGFloat = 7

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if x + sz.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += sz.width + spacing
            rowHeight = max(rowHeight, sz.height)
        }
        let width = maxWidth.isFinite ? maxWidth : x
        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if x + sz.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            s.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: ProposedViewSize(sz))
            x += sz.width + spacing
            rowHeight = max(rowHeight, sz.height)
        }
    }
}
