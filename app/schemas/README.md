# Room exported schemas

Generated with `exportSchema = true` and `room.schemaLocation = app/schemas`.

| Version | Notes |
|---------|--------|
| 7 | Locked baseline after Phase 5 (`scheduled_calls`). |
| 8 | Additive `tasks.source_type` / `source_image_path` / `source_url`. |

**V_min ≥ 4:** the app was never released to Play with DB v1–3. Versions 4–6 were never exported historically (no git tags). Do not invent untested migrations for those versions.

`MigrationTestHelper` (androidTest) and Robolectric migration tests read these JSON files / DDL accordingly.
