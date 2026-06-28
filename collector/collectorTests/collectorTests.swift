//
//  collectorTests.swift
//  collectorTests
//
//  Created by Frank Zhan Zhiquan on 31/5/26.
//

import Testing
@testable import collector

struct collectorTests {

    @Test func example() async throws {
        // Write your test here and use APIs like `#expect(...)` to check expected conditions.
        // Swift Testing Documentation
        // https://developer.apple.com/documentation/testing
    }

    @Test func parsesPortablePcExport() async throws {
        let json = """
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
        """

        let parsed = try #require(CollectorStore.parseImport(json))

        #expect(parsed.collections.count == 1)
        #expect(parsed.collections[0].name == "Audiophile")
        #expect(parsed.collections[0].icon == "headphones")
        #expect(parsed.collections[0].items[0].name == "Sony NW-ZX300")
        #expect(parsed.collections[0].items[0].fields[0].kind == .date)
    }

    @Test func parsesLegacyFlatPcBackup() async throws {
        let json = """
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
        """

        let parsed = try #require(CollectorStore.parseImport(json))
        let collection = try #require(parsed.collections.first)
        let item = try #require(collection.items.first)

        #expect(collection.icon == "pen")
        #expect(item.name == "Pilot")
        #expect(item.description == "Vanishing Point")
        #expect(item.tags == ["pen", "edc"])
        #expect(item.images.isEmpty)
        #expect(item.fields[0].label == "Acquired")
        #expect(item.fields[0].value == "2026-06-08")
        #expect(item.fields[0].kind == .date)
        #expect(item.fields[1].kind == .value)
    }

}
