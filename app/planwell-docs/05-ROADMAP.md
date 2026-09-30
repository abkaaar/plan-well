# Plan Well — Project Plan & Roadmap

**Version:** 1.0  
**Total Duration:** ~9 weeks  
**Team:** Solo developer  
**Target:** v1.0.0 on Google Play Store

---

## Phase Overview

| Phase | Weeks | Focus | Milestone |
|-------|-------|-------|-----------|
| 0 | 1 | Project setup & architecture | Dev environment ready |
| 1 | 2–3 | Core task CRUD | Tasks work end-to-end |
| 2 | 4–5 | Reminders & notifications | Alarms fire reliably |
| 3 | 6 | Categories, search & dashboard | Feature-complete v1.0 |
| 4 | 7–8 | Testing, polish & dark mode | ≥70% coverage, zero P0 bugs |
| 5 | 9 | Release prep & Play Store submission | v1.0.0 live |

---

## Phase 0 — Week 1: Project Setup & Architecture

**Goal:** Runnable project skeleton with all core dependencies wired.

### Tasks
- [ ] Create new Android project in Android Studio (Kotlin DSL `build.gradle.kts`)
- [ ] Configure `libs.versions.toml` (version catalog) with all dependencies
- [ ] Add Jetpack Compose, Navigation Compose, Room, Hilt, Coroutines
- [ ] Create module structure: `ui/`, `domain/`, `data/`, `di/`
- [ ] Create `AppDatabase` with Room (empty, no entities yet)
- [ ] Set up Hilt `@HiltAndroidApp` in `PlanWellApplication`
- [ ] Set up GitHub repository + `.gitignore`
- [ ] Set up GitHub Actions CI: lint + unit test on every push to `main`
- [ ] Create base `NavGraph.kt` with placeholder screens

**Deliverable:** App compiles and runs on emulator with a blank screen.

---

## Phase 1 — Weeks 2–3: Core Task CRUD

**Goal:** Users can create, view, edit, complete, and delete tasks.

### Week 2
- [ ] `TaskEntity`, `TaskDao`, `AppDatabase` (version 1)
- [ ] `TaskRepository` interface + `TaskRepositoryImpl`
- [ ] `CreateTaskUseCase`, `UpdateTaskUseCase`, `DeleteTaskUseCase`, `GetTasksUseCase`
- [ ] `CompleteTaskUseCase` (sets `is_completed`, records `completed_at_ms`)
- [ ] `TaskViewModel` with `StateFlow<TaskListUiState>`
- [ ] Basic `HomeScreen` displaying a `LazyColumn` of tasks

### Week 3
- [ ] `AddEditTaskScreen` with title, description, due date picker, priority selector
- [ ] `TaskDetailScreen` showing full task info
- [ ] Swipe-to-complete (swipe right) with animated checkbox fill
- [ ] Swipe-to-delete (swipe left) with undo `Snackbar`
- [ ] Soft-delete + `TrashScreen` with restore and permanent delete
- [ ] Unit tests: `CreateTaskUseCase`, `TaskViewModel`

**Milestone:** ✅ Core task management working end-to-end on emulator.

---

## Phase 2 — Weeks 4–5: Reminders & Notifications

**Goal:** Users can set reminders that fire as notifications, survive device reboot.

### Week 4
- [ ] `ReminderEntity`, `ReminderDao`
- [ ] `ReminderRepository` interface + impl
- [ ] `ScheduleReminderUseCase`, `CancelReminderUseCase`
- [ ] `ReminderScheduler` using `AlarmManager.setExactAndAllowWhileIdle`
- [ ] Permission handling: `POST_NOTIFICATIONS` (API 33+), `SCHEDULE_EXACT_ALARM` (API 31+)
- [ ] `ReminderPickerSheet` (bottom sheet): time picker + repeat type selector

### Week 5
- [ ] `ReminderReceiver` (BroadcastReceiver) — fires notification
- [ ] `NotificationHelper` — builds styled notification with task title and deep link
- [ ] `BootReceiver` — triggers `RestoreRemindersWorker` via WorkManager on boot
- [ ] `RestoreRemindersWorker` — re-schedules all active reminders from DB after reboot
- [ ] `RescheduleReminderWorker` — re-schedules recurring reminders after they fire
- [ ] Manual QA: reminder fires, notification tapped → opens correct task, reboot test

**Milestone:** ✅ Reminders fire reliably and survive device reboot.

---

## Phase 3 — Week 6: Categories, Search & Dashboard

**Goal:** Full feature set for v1.0 is complete.

### Tasks
- [ ] `CategoryEntity`, `CategoryDao`
- [ ] `CategoryScreen` (list, add, edit, delete with colour picker)
- [ ] Category filter chips on `HomeScreen`
- [ ] FTS5 virtual table (`tasks_fts`) added to `AppDatabase` (migration v1→v2 if needed)
- [ ] `TaskFtsDao` with search query
- [ ] `SearchTasksUseCase` with `debounce(300ms)` in `SearchViewModel`
- [ ] `SearchScreen` with real-time results
- [ ] Dashboard enhancements: today task count, completion progress ring
- [ ] `CompletionStatsUseCase` — today completed / total count

**Milestone:** ✅ App is feature-complete for v1.0.0.

---

## Phase 4 — Weeks 7–8: Testing, Polish & Dark Mode

**Goal:** Stable, polished, accessible app with ≥70% test coverage.

### Week 7 — Testing
- [ ] Unit tests: all UseCases (`CreateTask`, `CompleteTask`, `SearchTasks`, `ScheduleReminder`)
- [ ] Unit tests: `TaskViewModel`, `SearchViewModel` with Turbine for Flow assertions
- [ ] Integration tests: Room DAOs (in-memory DB) — insert, query, soft-delete, FTS
- [ ] Integration test: reminder cascade delete when task is deleted
- [ ] Compose UI tests: Add task flow, complete task flow, search flow
- [ ] JaCoCo report — verify ≥70% line coverage

### Week 8 — Polish
- [ ] Dark Mode — verify all colours via `MaterialTheme.colorScheme`
- [ ] Empty states for all screens (illustration + CTA)
- [ ] Accessibility audit with TalkBack — fix all missing `contentDescription`
- [ ] Performance: Baseline Profile (`BaselineProfileGenerator`) for cold-start optimisation
- [ ] ProGuard rules — verify release build does not crash
- [ ] Data export: JSON export to Downloads folder (`MediaStore`)
- [ ] Overdue task indicator (red due-date label)
- [ ] App icon (adaptive icon `ic_launcher.xml` + `ic_launcher_round.xml`)

**Milestone:** ✅ ≥70% test coverage, zero P0/P1 bugs, Dark Mode passes QA.

---

## Phase 5 — Week 9: Release Prep & Play Store Submission

**Goal:** v1.0.0 published on Google Play Store.

### Tasks
- [ ] Increment `versionCode` to 1, `versionName` to "1.0.0" in `build.gradle.kts`
- [ ] Generate release keystore (store securely, NOT in Git)
- [ ] Build signed AAB (Android App Bundle) in release mode
- [ ] Test signed AAB on physical device (2 different devices minimum)
- [ ] Write Play Store listing: title, short description, full description (English)
- [ ] Capture screenshots (minimum 2 phone, 1080×1920 px) with real tasks
- [ ] Export 512×512 app icon PNG for Play Console
- [ ] Create Privacy Policy page (simple HTML — "no data is collected")
- [ ] Complete Play Console Data Safety form (all "No" — no data collected)
- [ ] Complete IARC content rating questionnaire (Everyone)
- [ ] Upload to Internal Testing track → test → promote to Closed Testing → Production (20%)
- [ ] Monitor ANR rate and crash rate for 3 days → promote to 100% rollout

**Milestone:** ✅ v1.0.0 live on Google Play Store.

---

## Post-Launch Backlog

| Feature | Estimated Effort | Target Version |
|---------|-----------------|----------------|
| Home screen widget | 1 week | v1.1.0 |
| CSV / JSON import | 3 days | v1.1.0 |
| Task sub-tasks (checklist items) | 1 week | v1.2.0 |
| Calendar / agenda view | 2 weeks | v1.2.0 |
| Tablet / large-screen adaptive layout | 1 week | v1.3.0 |
| Colour themes / custom accent | 3 days | v1.3.0 |

---

## Dependencies / Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `SCHEDULE_EXACT_ALARM` denied by user | Medium | Graceful fallback to `setWindow()` with user explanation |
| Battery optimisation blocking reminders | High on some OEMs | Document in app: Settings → Battery → Plan Well → Unrestricted |
| Room migration error on update | Low | Schema export enabled; migration tests in CI |
| Play Store review delay | Low | Submit at start of week 9 to allow buffer |
