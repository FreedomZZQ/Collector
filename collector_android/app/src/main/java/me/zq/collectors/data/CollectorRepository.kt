package me.zq.collectors.data

import android.content.Context
import java.io.File

/**
 * Local-first persistence: the whole library lives in a single JSON file in the
 * app's private files dir (mirrors the iOS app's Documents/collector-library.json).
 */
class CollectorRepository(context: Context) {

    private val file = File(context.filesDir, "collector-library.json")

    fun load(): LibraryFile {
        if (!file.exists()) return LibraryFile(emptyList(), emptyList(), Trash())
        val text = try { file.readText() } catch (_: Exception) { return LibraryFile(emptyList(), emptyList(), Trash()) }
        return LibraryJson.decodeLibraryFile(text) ?: LibraryFile(emptyList(), emptyList(), Trash())
    }

    fun save(lib: LibraryFile) {
        try {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(LibraryJson.encodeLibraryFile(lib))
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                // Fallback if atomic rename fails for any reason.
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) { /* best-effort; in-memory state is the source of truth this session */ }
    }
}
