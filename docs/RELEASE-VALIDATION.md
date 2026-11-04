# Plan Well — Release validation (Prompt 7)

**Date:** 29 October 2026  
**App:** `com.planwellapp.app` · `versionCode` 1 · `versionName` 1.0.0  
**Variants verified:** `assembleDebug`, `assembleRelease` (minify/shrink on)  
**Not done:** Play Store upload, production track changes, keystore/signing credential edits

---

## Summary of implemented features

| Area | Status |
|------|--------|
| Tasks CRUD, complete, trash/restore | Present; covered by existing unit tests |
| Reminders + boot reschedule (`RestoreRemindersWorker` / `BootReceiver`) | Present; schedule/dedupe unit tests; boot path not instrumented on device in this pass |
| Categories + FTS search | Present; Search ViewModel + SearchTasks use-case tests |
| Opportunity Inbox / detail / add-edit | Present; Inbox VM + DAO + create use-case tests |
| Gallery attach + on-device ML Kit OCR + review UI | Present; `TextExtractor` unit + instrumented asset tests; see `docs/OCR-VALIDATION.md` |
| Opportunity → task link + confirmed-date reminders | Present; action use-case + link DAO tests |
| Unified daily agenda on Home | Present; builder + Home VM tests |
| Room migrations 4→5 and 5→6 | Present; Robolectric migration tests |
| Android share-to-save (`ACTION_SEND`) | **Not implemented** (Inbox copy still says “share … later”) |

---

## Commands executed (actual results)

| Command | Result |
|---------|--------|
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` | **BUILD SUCCESSFUL** — **72** unit tests, **0** failures |
| `.\gradlew.bat :app:assembleRelease` | **BUILD SUCCESSFUL** — `app/build/outputs/apk/release/app-release.apk` |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | **BUILD SUCCESSFUL** — **1** instrumented test on Pixel 6a (API 17 device id `28121JEGR03129`) |

Unit suites included (among others): task/reminder/search use cases; Opportunity DAO/links; Migration 4→5 & 5→6; OCR parser; attachment store; agenda builder; Home / Inbox / Search ViewModels.

### Gap tests added this pass

- `Migration5To6Test` — preserves opportunity row; adds extraction columns + candidates table  
- `OpportunityTextParserTest` — blank, URL normalize, deadline/event classify, ambiguous slash date  
- `FileOpportunityAttachmentStoreTest` — save/resolve, missing delete safe, delete-all  
- `ValidateOpportunityFieldsTest` — oversized URL/description  

---

## Migration considerations

- DB version **6**. Registered migrations: **4→5**, **5→6** (additive).  
- `fallbackToDestructiveMigrationFrom(1, 2, 3)` — upgrades from DB v1–3 **wipe** those schemas (acceptable only if no production users on those versions).  
- **4→5 / 5→6** preserve existing tasks/categories/reminders (validated in unit tests with seeded rows).  
- `exportSchema = false` — no Room schema JSON in repo; migrations are hand-maintained.  
- **Not executed:** side-by-side install of a pre-opportunity production APK → upgrade → open app with real user data (no prior store build available in this session). Recommend that smoke on Internal testing before Production.

---

## Privacy & dependencies

**New / notable deps:** ML Kit Text Recognition `16.0.1`, `kotlinx-coroutines-play-services`.

**Network:** `INTERNET` + `ACCESS_NETWORK_STATE` are declared for **optional OG link previews only** (Settings toggle, default on). All other features work offline. See `docs/SMART-IMPORT.md`.

**Privacy policy** (`docs/privacy-policy.html`) updated for opportunities, attachments, on-device OCR, and gallery pick. Play listing short/full description still task-centric — update before store push if shipping opportunity features in the same release.

**Data Safety:** Still “no data collected / no off-device transmission by Plan Well.” Confirm Console answers after listing OCR/attachments wording.

---

## Requirement coverage matrix

| Requirement | Result |
|-------------|--------|
| Protect task/reminder/category/search/trash/nav/data | Unit coverage + release minify build; no destructive migration on v4+ |
| Share-to-save / text / URL / image intents | **Gap:** no `ACTION_SEND` filters; manual capture + gallery OCR only |
| Attachment persistence | File store unit tests; missing path delete is no-op |
| Opportunity Inbox | Unit + UI present |
| OCR + date confirmation | Parser tests; unconfirmed dates refused for reminders/agenda |
| Linked tasks/reminders | Use-case tests; permission denial requests permission via VM event (not UI-automated) |
| Daily agenda | Builder + Home tests |
| DB migrations | 4→5 and 5→6 unit tests |
| Malformed intents / bad URLs / missing attachments | Field validation + attachment delete-safe; **no** share-intent handler to fuzz |
| Denied notification permission | VM emits `RequestReminderPermissions` when `permissionsGranted=false`; not instrumented |
| Reminder reschedule after reboot | Code path present (`BootReceiver` → worker); **not** device-tested this pass |
| Process recreation / restart persistence | Room + file attachments by design; **no** dedicated process-kill UI test |
| Accessibility | Many icons have `contentDescription`; some decorative icons `null`; agenda chips limited semantics |
| Performance | Debug APK ~65 MB (ML Kit); release ~45 MB — watch cold start on low-end devices |
| Error handling | Empty/loading/error on Home; OCR/review error fields on opportunity |
| Signing / publish | Release APK built; **no** Play publish; signing configs untouched |

---

## Known limitations (non-blocking unless noted)

1. **Share-to-save not shipped** — capture is manual / gallery only.  
2. **Calls/appointments** only appear on the agenda as confirmed opportunity event dates or task reminders.  
3. **OCR** is heuristic Latin text; ambiguous numeric dates stay unparsed (`extractedDateMs = null`).  
4. **Instrumented UI coverage** is minimal (one Compose empty-state test).  
5. **Play listing / What’s new** not yet reflecting Opportunity + Agenda features.  
6. **Keystore / hosted privacy URL / screenshots / Internal testing** remain operator steps in `RELEASE-CHECKLIST.md`.

---

## Release blockers

| ID | Blocker | Severity |
|----|---------|----------|
| B1 | Operator checklist incomplete (keystore, hosted privacy URL, device smoke, screenshots, Console Data Safety) | **Blocker for Production** (process) |
| B2 | Share filters ship in debug; **release Play APK on device still lacked SEND** until next store build | **Blocker for Production share UX** until release rebuild uploaded |
| B3 | No end-to-end upgrade smoke from a real pre-v5 install with user data | **Soft blocker** — run on Internal track before Production |
| B4 | Play listing copy / Data Safety narrative outdated vs Opportunity + OCR | **Soft blocker** — update docs before Console submission |

**No automated test failures or failed Gradle builds** were observed in this validation pass after the added tests and manifest permission cleanup.

---

## Files touched during validation

- Tests: `Migration5To6Test`, `OpportunityTextParserTest`, `FileOpportunityAttachmentStoreTest`, `ValidateOpportunityFieldsTest`  
- `app/proguard-rules.pro` — ML Kit keep/dontwarn  
- `AndroidManifest.xml` — remove merged `INTERNET` / `ACCESS_NETWORK_STATE`  
- `docs/privacy-policy.html` — opportunities / OCR / attachments  
- This document: `docs/RELEASE-VALIDATION.md`

---

## Recommended next steps (human)

1. Update Play listing “What’s new” + description for Opportunity Inbox + agenda (or strip Inbox share wording).  
2. Internal testing: install release → create tasks/reminders → deny notifications → reboot → confirm restore; attach image → OCR → confirm date → agenda.  
3. Upgrade path: install prior DB-v4 build if available, then this release, confirm tasks intact.  
4. `aapt dump permissions app-release.apk` — confirm no `INTERNET`.  
5. Only then `bundleRelease` + Console Internal → Production (with explicit authorization).

---

## Share target + expandable FAB remediation (29 Oct 2026)

### Root causes (audit)

- System resolver **does** register `ACTION_SEND` on **`com.planwellapp.app.debug`** (`query-activities` for `text/plain` and `image/jpeg` → `MainActivity`).
- Released package **`com.planwellapp.app`** on device had **no** SEND filters (older Play build). Use **Plan Well (Debug)** for share testing until a new release ships with the same filters.
- Instagram often uses a **custom** share sheet that omits many system targets — not the same as a missing manifest filter. Prefer Photos / Gallery / Chrome for verification.
- Home FAB was a single Add-task control (not the intended speed dial).

### Fixes shipped in debug

- Split MIME `intent-filter`s + `share_to_plan_well` label; debug `app_name` = **Plan Well (Debug)**.
- Share image URI fail-closed messaging; ClipData / IntentCompat stream resolution.
- Expandable Home FAB: Add task, Schedule a call, Add event, Save opportunity (reuses task + capture sheet).

### Automated verification

| Check | Result |
|-------|--------|
| `assembleDebug` + capture/home/agenda unit tests | **PASS** (this session) |
| `installDebug` on Pixel 6a | **PASS** |
| `query-activities` SEND `image/jpeg` → debug MainActivity | **PASS** |
| Gallery / Photos / Chrome / Instagram chooser UI | **Not fully UI-verified** — run manual matrix below |

### Manual matrix (operator)

1. Photos → Share image → choose **Plan Well (Debug)** → Inbox/extraction opens.  
2. Chrome → Share link → Plan Well (Debug) → opportunity with URL.  
3. Instagram → if missing from list, use Copy link / system Share; document custom-sheet limitation.  
4. Cold start vs app already open for share.  
5. Home FAB → expand → each of four actions reaches the correct screen.
