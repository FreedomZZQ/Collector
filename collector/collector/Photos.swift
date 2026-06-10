//
//  Photos.swift
//  collector
//
//  Item photo storage + display. Photos are downscaled to JPEG and stored in
//  Documents/collector-images; items reference them by file name, so the
//  library JSON stays small and photos stay on-device (mirroring the desktop
//  app, where photos don't travel between devices).
//

import SwiftUI
import UIKit

// MARK: - Disk store

enum ImageStore {
    /// Documents/collector-images, created on first use.
    nonisolated static func directory() -> URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dir = docs.appendingPathComponent("collector-images", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    nonisolated static func url(_ name: String) -> URL {
        directory().appendingPathComponent(name)
    }

    /// Decode picked photo data, downscale it to a sane size and store it as
    /// JPEG. Returns the new file name, or nil if the data isn't an image.
    nonisolated static func save(_ data: Data) -> String? {
        guard let image = UIImage(data: data) else { return nil }
        let maxDim: CGFloat = 2048
        let scale = min(1, maxDim / max(image.size.width, image.size.height))
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        // Redrawing also bakes in the EXIF orientation and converts HEIC → JPEG.
        let scaled = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        guard let jpeg = scaled.jpegData(compressionQuality: 0.85) else { return nil }
        let name = UUID().uuidString + ".jpg"
        do {
            try jpeg.write(to: url(name), options: .atomic)
            return name
        } catch {
            return nil
        }
    }

    nonisolated static func delete(_ name: String) {
        try? FileManager.default.removeItem(at: url(name))
    }

    /// Delete every stored photo file that is not in `referenced`.
    nonisolated static func prune(referenced: Set<String>) {
        let dir = directory()
        guard let files = try? FileManager.default.contentsOfDirectory(atPath: dir.path) else { return }
        for f in files where !referenced.contains(f) {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent(f))
        }
    }
}

// MARK: - Async thumbnail view

/// Decoded thumbnails, keyed by "name#maxPixel". NSCache evicts under pressure.
private let thumbCache = NSCache<NSString, UIImage>()

/// Loads a stored photo off the main thread, downscaled to `maxPixel`, and
/// shows it `scaledToFill`. Callers give it a frame and clip it.
struct ItemPhoto: View {
    let name: String
    /// Longest decoded edge in pixels — keep small for list thumbs.
    var maxPixel: CGFloat = 600

    @Environment(\.palette) private var p
    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else if failed {
                // Referenced file is gone (e.g. restored from a backup made on
                // another device) — show a quiet placeholder.
                Rectangle().fill(p.surface2)
                    .overlay(Icon(name: "image", size: 22).foregroundColor(p.faint))
            } else {
                Color.clear
            }
        }
        .task(id: name) {
            let key = "\(name)#\(Int(maxPixel))" as NSString
            if let hit = thumbCache.object(forKey: key) {
                image = hit
                return
            }
            guard let raw = UIImage(contentsOfFile: ImageStore.url(name).path) else {
                failed = true
                return
            }
            let scale = min(1, maxPixel / max(raw.size.width, raw.size.height))
            let size = CGSize(width: raw.size.width * scale, height: raw.size.height * scale)
            guard let thumb = await raw.byPreparingThumbnail(ofSize: size) else {
                failed = true
                return
            }
            thumbCache.setObject(thumb, forKey: key)
            failed = false
            image = thumb
        }
    }
}

// MARK: - Detail hero pager

/// Swipeable photo pager for the item detail hero. Tapping a page reports its
/// index so the caller can open the full-screen viewer.
struct PhotoPager: View {
    let images: [String]
    let onTap: (Int) -> Void

    @State private var page = 0

    var body: some View {
        TabView(selection: $page) {
            ForEach(Array(images.enumerated()), id: \.element) { i, name in
                GeometryReader { geo in
                    ItemPhoto(name: name, maxPixel: 1400)
                        .frame(width: geo.size.width, height: geo.size.height)
                        .clipped()
                        .contentShape(Rectangle())
                        .onTapGesture { onTap(i) }
                }
                .tag(i)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .overlay(alignment: .bottom) {
            if images.count > 1 {
                HStack(spacing: 5) {
                    ForEach(0..<images.count, id: \.self) { i in
                        Circle()
                            .fill(.white.opacity(i == page ? 0.95 : 0.45))
                            .frame(width: 6, height: 6)
                    }
                }
                .padding(.vertical, 4).padding(.horizontal, 8)
                .background(Capsule().fill(Color.black.opacity(0.25)))
                .padding(.bottom, 8)
            }
        }
        .onChange(of: images) { imgs in
            page = min(page, max(0, imgs.count - 1))
        }
    }
}

// MARK: - Full-screen viewer

/// Full-screen photo viewer: swipe between photos, pinch or double-tap to zoom.
struct PhotoViewer: View {
    let images: [String]
    let onClose: () -> Void

    @State private var page: Int

    init(images: [String], startAt: Int, onClose: @escaping () -> Void) {
        self.images = images
        self.onClose = onClose
        _page = State(initialValue: min(max(0, startAt), max(0, images.count - 1)))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            TabView(selection: $page) {
                ForEach(Array(images.enumerated()), id: \.element) { i, name in
                    ZoomableImage(name: name)
                        .ignoresSafeArea()
                        .tag(i)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .ignoresSafeArea()
        }
        .overlay(alignment: .top) {
            if images.count > 1 {
                Text("\(page + 1) / \(images.count)")
                    .font(.mono(13)).foregroundColor(.white.opacity(0.85))
                    .padding(.top, 14)
            }
        }
        .overlay(alignment: .topTrailing) {
            Button(action: onClose) {
                Icon(name: "close", size: 17, weight: .semibold)
                    .foregroundColor(.white)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(Color.white.opacity(0.18)))
            }
            .buttonStyle(.plain)
            .padding(.top, 8).padding(.trailing, 16)
        }
        .preferredColorScheme(.dark)
    }
}

// MARK: - Zoomable image (UIScrollView-backed pinch zoom)

private struct ZoomableImage: UIViewRepresentable {
    let name: String

    func makeUIView(context: Context) -> ZoomScrollView {
        let v = ZoomScrollView()
        v.setImage(UIImage(contentsOfFile: ImageStore.url(name).path))
        return v
    }

    func updateUIView(_ uiView: ZoomScrollView, context: Context) {}
}

final class ZoomScrollView: UIScrollView, UIScrollViewDelegate {
    private let imageView = UIImageView()
    private var lastLayoutSize: CGSize = .zero

    init() {
        super.init(frame: .zero)
        delegate = self
        minimumZoomScale = 1
        maximumZoomScale = 4
        showsVerticalScrollIndicator = false
        showsHorizontalScrollIndicator = false
        contentInsetAdjustmentBehavior = .never
        backgroundColor = .clear
        addSubview(imageView)

        let doubleTap = UITapGestureRecognizer(target: self, action: #selector(handleDoubleTap(_:)))
        doubleTap.numberOfTapsRequired = 2
        addGestureRecognizer(doubleTap)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    func setImage(_ image: UIImage?) {
        imageView.image = image
        lastLayoutSize = .zero
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        // Re-fit the image when the view first gets a size or it changes
        // (rotation); while the user is zoomed in just keep it centered.
        if bounds.size != lastLayoutSize, zoomScale == minimumZoomScale {
            lastLayoutSize = bounds.size
            fitImage()
        }
        centerImage()
    }

    private func fitImage() {
        guard let img = imageView.image,
              img.size.width > 0, bounds.width > 0, bounds.height > 0 else { return }
        zoomScale = 1
        let fit = min(bounds.width / img.size.width, bounds.height / img.size.height)
        let size = CGSize(width: img.size.width * fit, height: img.size.height * fit)
        imageView.frame = CGRect(origin: .zero, size: size)
        contentSize = size
    }

    /// Keep the image centered while it is smaller than the viewport.
    private func centerImage() {
        let dx = max(0, (bounds.width - contentSize.width) / 2)
        let dy = max(0, (bounds.height - contentSize.height) / 2)
        contentInset = UIEdgeInsets(top: dy, left: dx, bottom: dy, right: dx)
    }

    func viewForZooming(in scrollView: UIScrollView) -> UIView? { imageView }
    func scrollViewDidZoom(_ scrollView: UIScrollView) { centerImage() }

    @objc private func handleDoubleTap(_ g: UITapGestureRecognizer) {
        if zoomScale > minimumZoomScale * 1.01 {
            setZoomScale(minimumZoomScale, animated: true)
        } else {
            let point = g.location(in: imageView)
            let w = bounds.width / 2.5
            let h = bounds.height / 2.5
            zoom(to: CGRect(x: point.x - w / 2, y: point.y - h / 2, width: w, height: h), animated: true)
        }
    }
}
