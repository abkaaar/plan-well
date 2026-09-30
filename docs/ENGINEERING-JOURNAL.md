# Plan Well — Engineering Journal

A running log of **what** we implemented, **how** it fits together, and **why** we chose it.
Product specs stay in [`app/planwell-docs/`](../app/planwell-docs/). This file is for learning the engineering decisions.

---

## How to read this

Each entry follows:

| Field | Meaning |
|-------|---------|
| **What** | Files / features added or changed |
| **Why** | Product or architecture reason (often from Roadmap / TDD) |
| **How** | Mental model of the wiring |
| **Trade-off** | Cost we accepted |

---

## Phase 0 — Project Setup & Architecture

**Source:** Roadmap Phase 0, TDD §1–§2 / §7, UI/UX colour system.

### Goal

A runnable skeleton: Clean Architecture packages, Hilt DI, empty Room database, Navigation Compose with placeholder screens — **no task CRUD yet** (that is Phase 1).

---

### Entry 0.1 — Engineering journal itself

| | |
|--|--|
| **What** | Created `docs/ENGINEERING-JOURNAL.md` |
| **Why** | Specs say *what* to build; a journal records *what we actually built* and the reasoning so future-you can relearn the stack without re-reading every design doc. |
| **How** | Living markdown at repo root, updated with each Phase 0 change. |
| **Trade-off** | Extra writing time; pays off when debugging architecture months later. |

---

### Entry 0.2 — Gradle version catalog & core dependencies

| | |
|--|--|
| **What** | Extended `gradle/libs.versions.toml` and `app/build.gradle.kts` with Navigation Compose, Room (+ KSP), Hilt (+ KSP), Coroutines, Lifecycle ViewModel Compose. Raised `minSdk` to **26**. KSP plugin version **2.3.12** (no `2.4.10-*` KSP artifact published yet). |
| **Why** | Roadmap Phase 0 and README tech stack require these before any feature code. PRD requires API 26+. Keeping `compileSdk` 37 / `targetSdk` 36 matches the Compose BOM already in the project. |
| **How** | Version catalog centralises versions; app module applies `hilt` + `ksp` plugins and depends on the libraries. KSP generates Hilt/Room code at compile time. |
| **Trade-off** | Hilt/KSP add build time vs writing manual factories. We accept that for lifecycle-aware DI as in the TDD. KSP 2.3.x with Kotlin 2.4.10 is the newest matching pair available on Maven at implementation time. |

---

### Entry 0.3 — Application class & package layers

| | |
|--|--|
| **What** | `PlanWellApplication` (`@HiltAndroidApp`); packages `ui/`, `domain/`, `data/`, `di/`; moved `MainActivity` under `ui/`. |
| **Why** | TDD Clean Architecture: UI → Domain → Data. Hilt needs an `Application` annotated with `@HiltAndroidApp` as the DI root. |
| **How** | Manifest points `android:name` at the Application. Activity uses `@AndroidEntryPoint` so Hilt can inject into it (and later into ViewModels). |
| **Trade-off** | More folders early feels empty; prevents dumping Room/DAO calls into Composables later. |

---

### Entry 0.4 — Empty Room `AppDatabase` + `DatabaseModule`

| | |
|--|--|
| **What** | `AppDatabase` + Hilt `DatabaseModule`. Room cannot use `entities = []`, so we added a temporary `Phase0SchemaAnchor` entity (`_phase0_schema_anchor` table). |
| **Why** | Roadmap Phase 0: wire Room before feature entities. Phase 1 adds `TaskEntity` and removes the anchor (schema bump). |
| **How** | `@Database(entities = [Phase0SchemaAnchor::class], version = 1, exportSchema = false)`. Module uses `Room.databaseBuilder` and `@Singleton`. |
| **Trade-off** | One unused table until Phase 1. Cleaner than delaying Hilt/Room wiring entirely. `exportSchema = false` until we have a real schema to commit for CI. |

---

### Entry 0.5 — Navigation Compose + Home placeholder

| | |
|--|--|
| **What** | `NavGraph.kt` routes (`home`, `task`, `category`, `search`); `HomeScreen` Phase 0 empty state; other screens as placeholders. |
| **Why** | Single-activity + Navigation Compose (TDD). Placeholders prove the graph works before CRUD. |
| **How** | `MainActivity` → `setContent` → `PlanWellTheme` → `PlanWellNavGraph` → `NavHost`. |
| **Trade-off** | String routes are simple for Phase 0; typed routes can come later if needed. |

---

### Entry 0.6 — Theme aligned to UI/UX specs

| | |
|--|--|
| **What** | `Color.kt` / `Theme.kt` use PrimaryBlue, AccentGreen, Background, etc. Dynamic colour **off** by default. |
| **Why** | UI/UX spec defines the brand palette. Dynamic colour previously contributed to a black Compose frame on this emulator’s GPU path. |
| **How** | Material 3 `lightColorScheme` / `darkColorScheme` mapped from UIUX tokens. |
| **Trade-off** | No wallpaper-based Material You tinting until we re-enable dynamic colour carefully. |

---

### Entry 0.7 — CI workflow stub

| | |
|--|--|
| **What** | `.github/workflows/ci.yml` runs lint + unit tests on push/PR. |
| **Why** | Roadmap Phase 0 asks for CI on `main`. Catching breakages early. |
| **How** | GitHub Actions JDK 21 + Gradle wrapper tasks. |
| **Trade-off** | Unit tests may be empty in Phase 0; the pipeline still validates the project configures and lints. |

---

## Phase 1 — Core Task CRUD

**Source:** Roadmap Phase 1, TDD schema / DAO / ViewModel pattern, UIUX task card + FAB.

### Entry 1.1 — Data layer (TaskEntity → Repository)

| | |
|--|--|
| **What** | `TaskEntity`, `TaskDao`, domain `Task` + `Priority`, mappers, `TaskRepository` / `TaskRepositoryImpl`, use cases (create/update/delete/get/complete/restore/permanent delete). Removed Phase 0 schema anchor; DB version **2** with destructive fallback. |
| **Why** | Offline-first CRUD needs a local source of truth. Clean Architecture keeps Room out of the UI. Soft-delete matches the PRD undo/trash story. |
| **How** | UI → UseCase → Repository → DAO → SQLite. Flows from Room power live lists via `StateFlow` in ViewModels. |
| **Trade-off** | Destructive migration from Phase 0 is fine (no real user data). Later releases need real `Migration` objects. |

### Entry 1.2 — Home list, Add/Edit, Detail, Trash

| | |
|--|--|
| **What** | `HomeViewModel` + task `LazyColumn`, FAB → `AddEditTaskScreen`, detail screen, swipe complete/delete with undo snackbar, `TrashScreen`. |
| **Why** | Roadmap Phase 1 milestone: end-to-end task management on emulator. |
| **How** | Navigation routes `home`, `task/add`, `task/{id}`, `task/edit/{id}`, `trash`. Hilt injects ViewModels via `hiltViewModel()`. |
| **Trade-off** | Date/time pickers still use platform dialogs (simple, not fully Compose Material 3 pickers). Unit tests deferred to a follow-up if needed. |

---

## Phase 2 — Reminders & Notifications

**Source:** Roadmap Phase 2, TDD reminder schema / AlarmManager + WorkManager.

### Entry 2.1 — Reminder data + AlarmManager

| | |
|--|--|
| **What** | `ReminderEntity` / `ReminderDao`, domain `Reminder` + `RepeatType`, `ReminderRepository`, schedule/cancel use cases, `ReminderScheduler` (`setExactAndAllowWhileIdle` with inexact fallback). DB version **3**. |
| **Why** | Offline reminders must survive process death; AlarmManager is the right API for exact local alarms. |
| **How** | Schedule writes Room then arms AlarmManager. Delete-task cancels all reminders for that task. |
| **Trade-off** | Exact alarms need user permission on API 31+; we fall back to `setWindow` if denied. |

### Entry 2.2 — Notifications, boot restore, UI

| | |
|--|--|
| **What** | `ReminderReceiver`, `NotificationHelper`, `BootReceiver`, `RestoreRemindersWorker`, `RescheduleReminderWorker`, `ReminderPickerSheet` on task detail, notification/exact-alarm permission prompts, notification deep link into task detail. |
| **Why** | Roadmap: fire notifications, survive reboot, recurring reschedule. |
| **How** | Receiver shows notification → WorkManager reschedules recurring. Boot enqueues restore worker. Hilt `Configuration.Provider` owns WorkManager factory. |
| **Trade-off** | Default WorkManager initializer disabled so Hilt can inject workers. Manual reboot QA still recommended on a physical device. |

---

## Phase 3 — Categories, Search & Dashboard

**Source:** Roadmap Phase 3, TDD schema / FTS5 / CategoryDao, UIUX filter chips + Search + Categories screens, SRS FR-03 / FR-04.

### Entry 3.1 — Categories data + UI

| | |
|--|--|
| **What** | `CategoryEntity` / `CategoryDao` (with task counts), domain `Category` + 10 colour presets, `CategoryRepository` / impl, create/update/delete/get use cases, `CategoryScreen` + `CategoryViewModel` (list, add/edit dialog, colour picker, delete confirm). `TaskEntity` FK → categories with `ON DELETE SET_NULL`. DB version **4**. |
| **Why** | Roadmap Phase 3 / PRD P1: organise tasks with colour-coded labels. Deleting a category must not delete tasks. |
| **How** | UI → UseCase → Repository → DAO. Home / Add-Edit / Detail resolve `categoryId` → name via `GetCategoriesUseCase`. Filter chips on Home filter the list in the ViewModel. |
| **Trade-off** | Destructive migration again (dev schema; no real user data). Unique category names via Room index — SQLite unique constraint surfaces as insert abort. |

### Entry 3.2 — FTS5 search

| | |
|--|--|
| **What** | `TaskFtsEntity` (`@Fts4(contentEntity = TaskEntity)`), `TaskFtsDao`, `SearchTasksUseCase` (query sanitize + prefix wildcards), `SearchViewModel` with `debounce(300ms)`, `SearchScreen` real-time results. Home top bar opens Search. |
| **Why** | SRS FR-04: local full-text search on title/description within 200 ms after debounce. |
| **How** | External-content FTS stays synced via Room triggers. ViewModel debounces keystrokes → sanitized `MATCH` query → Flow of tasks. |
| **Trade-off** | Spec mentions FTS5; Room’s reliable contentEntity path is FTS4, which still meets the search requirement. Prefix tokens (`word*`) favour typeahead. Special FTS operators are stripped so typing cannot break MATCH. |

### Entry 3.3 — Today dashboard stats

| | |
|--|--|
| **What** | `CompletionStats` + `CompletionStatsUseCase` (today’s due total / completed), progress ring on `HomeScreen`, “N of M due today” header. |
| **Why** | Roadmap dashboard enhancements; Home is the Today view per UIUX. |
| **How** | Day bounds from local midnight → Room count queries → `combine` into `HomeViewModel` state. |
| **Trade-off** | “Today” means tasks with `due_date_ms` in the local calendar day — tasks without a due date are excluded from the ring (intentional). |

---

## Phase 4 — Testing, Polish & Dark Mode

**Source:** Roadmap Phase 4, Test Plan (JUnit / MockK / Turbine / Room / JaCoCo), UIUX empty states & overdue.

### Entry 4.1 — Unit & Room tests + JaCoCo

| | |
|--|--|
| **What** | Test deps (JUnit4, MockK, Turbine, coroutines-test, Room testing, Robolectric). UseCase tests (`CreateTask`, `CompleteTask`, `SearchTasks`, `ScheduleReminder`). `HomeViewModel` / `SearchViewModel` Turbine tests. In-memory Room tests (CRUD, FTS, category nullify, reminder cascade). `jacocoTestReport` task + CI step. |
| **Why** | Roadmap ≥70% coverage goal needs a green pyramid before release polish. |
| **How** | JVM unit tests for logic; Robolectric for Room so CI (`testDebugUnitTest`) covers DAO/FTS without an emulator. |
| **Trade-off** | Full Compose journey tests deferred to instrumented `EmptyStateComposeTest` sample; Baseline Profile module not added yet (post-v1). Coverage % still needs a local JaCoCo HTML check against the 70% target. |

### Entry 4.2 — Polish: overdue, empty states, export, icon, ProGuard

| | |
|--|--|
| **What** | Shared `EmptyState` (Home/Categories/Trash). Overdue red due-date on `TaskCard`. JSON export via MediaStore Downloads (⋮ menu). Adaptive launcher icon (brand blue + green check). Release minify + Hilt/Room/Work keep rules. Dark theme already tokenised via `MaterialTheme.colorScheme`. |
| **Why** | Roadmap Week 8 polish items for v1 readiness and accessibility of empty/error paths. |
| **How** | Export writes `planwell-tasks-*.json`. Icons in `mipmap-anydpi-v26`. Release `isMinifyEnabled = true`. |
| **Trade-off** | Pre-API 29 export lands in app-specific Downloads folder (no storage permission). Baseline Profile / TalkBack full audit still manual. |

---

## Phase 5 — Release Prep & Play Store

**Source:** Roadmap Phase 5, Deployment doc §§1–7.

### Entry 5.1 — Version, signing, store package

| | |
|--|--|
| **What** | `versionName` **1.0.0** / `versionCode` 1. Conditional release signing from `local.properties` or env. Debug `applicationIdSuffix = .debug`. Generated local PKCS12 keystore (gitignored). `bundleRelease` produces signed AAB. Backup/data-extraction rules exclude Room DB from cloud backup. |
| **Why** | Play requires a signed AAB, stable applicationId, and clear versioning. |
| **How** | Props: `keystore.path`, `keystore.password`, `key.alias`, `key.password`. See `keystore/README.md` and `docs/RELEASE-CHECKLIST.md`. |
| **Trade-off** | Keystore + passwords live only on this machine (`keystore/CREDENTIALS.txt` gitignored). **You must back them up** — losing them blocks updates to the same Play listing. |

### Entry 5.2 — Listing, privacy, graphics

| | |
|--|--|
| **What** | `docs/privacy-policy.html` (no data collected / offline). `docs/play-store-listing.md` (title, short/full description, Data Safety answers, release notes). Store graphics stubs in `docs/store-assets/` (512 icon + 1024×500 feature graphic). |
| **Why** | Play Console requires a live Privacy Policy URL and listing assets before production. |
| **How** | Host the HTML (e.g. GitHub Pages), paste URL into Console. Upload AAB to Internal testing first. |
| **Trade-off** | Screenshots and Console upload / IARC / rollout remain manual. Contact email in the policy is a placeholder until you replace it. |

---

### Entry 5.3 — Application ID change for Play

| | |
|--|--|
| **What** | Play `applicationId` changed from `com.planwell.app` → **`com.planwellapp.app`**. Kotlin `namespace` / source packages remain `com.planwell.app`. |
| **Why** | Previous ID was unavailable / taken on Play Console. |
| **How** | Only `applicationId` in `app/build.gradle.kts` must be unique for Store; R8 keep rules still target code packages. Rebuild AAB after the change. |
| **Trade-off** | Old debug/release installs under `com.planwell.app` are a different app; uninstall or keep alongside. New debug id is `com.planwellapp.app.debug`. |

---

## Next (not done yet)

**Play Console submission** — host privacy URL, capture screenshots, upload AAB (`com.planwellapp.app`), Internal → Production rollout.
