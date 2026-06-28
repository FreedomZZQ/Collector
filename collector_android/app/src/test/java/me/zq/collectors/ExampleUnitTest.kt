package me.zq.collectors

import me.zq.collectors.data.FieldKind
import me.zq.collectors.data.LibraryJson
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun parsesPortablePcExport() {
        val json = """
            {
              "app": "Collector",
              "version": 1,
              "exportedAt": "2026-06-08",
              "collections": [
                {
                  "id": "c1",
                  "icon": "headphones",
                  "name": "Audiophile",
                  "items": [
                    {
                      "id": "i1",
                      "name": "Sony NW-ZX300",
                      "description": "",
                      "tags": [],
                      "fields": [
                        { "label": "Acquired", "kind": "date", "value": "" },
                        { "label": "Condition", "kind": "condition", "value": "" }
                      ]
                    }
                  ]
                }
              ],
              "trash": { "collections": [], "items": [] }
            }
        """.trimIndent()

        val parsed = LibraryJson.parseImport(json)

        assertNotNull(parsed)
        assertEquals(1, parsed!!.collections.size)
        assertEquals("Audiophile", parsed.collections[0].name)
        assertEquals("headphones", parsed.collections[0].icon)
        assertEquals("Sony NW-ZX300", parsed.collections[0].items[0].name)
        assertEquals(FieldKind.DATE, parsed.collections[0].items[0].fields[0].kind)
    }

    @Test
    fun parsesLegacyFlatPcBackup() {
        val json = """
            {
              "collections": [
                { "id": "c1", "name": "Pens", "icon": "✒" }
              ],
              "items": [
                {
                  "id": "i1",
                  "collection_id": "c1",
                  "name": "Pilot",
                  "short_desc": "Vanishing Point",
                  "photos": ["/Users/me/Pilot.jpg"],
                  "acquired_date": "2026-06-08",
                  "custom_fields": [
                    { "id": "f1", "label": "VALUE / PRICE", "value": "120" },
                    { "id": "f2", "label": "TAGS", "value": "pen, edc" }
                  ]
                }
              ],
              "templates": []
            }
        """.trimIndent()

        val parsed = LibraryJson.parseImport(json)

        assertNotNull(parsed)
        val collection = parsed!!.collections.single()
        val item = collection.items.single()
        assertEquals("pen", collection.icon)
        assertEquals("Pilot", item.name)
        assertEquals("Vanishing Point", item.description)
        assertEquals(listOf("pen", "edc"), item.tags)
        assertNull(item.image)
        assertEquals(FieldKind.DATE, item.fields[0].kind)
        assertEquals("Acquired", item.fields[0].label)
        assertEquals("2026-06-08", item.fields[0].value)
        assertEquals(FieldKind.VALUE, item.fields[1].kind)
    }
}
