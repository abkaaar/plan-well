# Plan Well — Software Requirements Specification (SRS)

**Version:** 1.0  
**Platform:** Android 8.0+ (API 26+)  
**Language:** Kotlin  
**Status:** Approved

---

## 1. Functional Requirements

### FR-01 · Task Management

| ID | Requirement |
|----|-------------|
| FR-01.1 | The system shall allow users to create a task with: title (required, max 100 chars), description (optional, max 500 chars), due date/time (optional), priority (None / Low / Medium / High), and category (optional). |
| FR-01.2 | The system shall allow editing of any task field at any time. |
| FR-01.3 | The system shall allow soft-deletion (trash) and permanent deletion of tasks. |
| FR-01.4 | The system shall allow marking a task as complete, which records a completion timestamp. |
| FR-01.5 | The system shall support reordering of tasks by drag-and-drop within a list. |
| FR-01.6 | The system shall display tasks grouped by: All, Today, Upcoming, and Completed. |
| FR-01.7 | The system shall display overdue tasks with a distinct visual indicator (red due-date label). |

---

### FR-02 · Reminders

| ID | Requirement |
|----|-------------|
| FR-02.1 | The system shall schedule local push notifications for tasks that have a due date and reminder set. |
| FR-02.2 | The system shall support one-time reminders at a specified date and time. |
| FR-02.3 | The system shall support recurring reminders: daily, weekly, and custom intervals. |
| FR-02.4 | Tapping a notification shall deep-link directly to the relevant task detail screen. |
| FR-02.5 | The system shall reschedule active reminders automatically after device reboot via a `BroadcastReceiver`. |
| FR-02.6 | The system shall allow cancellation of any active reminder. |
| FR-02.7 | Completing a task shall automatically cancel its associated pending reminders. |

---

### FR-03 · Categories

| ID | Requirement |
|----|-------------|
| FR-03.1 | Users shall create, rename, and delete custom categories with a colour label. |
| FR-03.2 | Deleting a category shall set associated tasks' category to `NULL` (Uncategorised), not delete the tasks. |
| FR-03.3 | The home screen shall support filtering the task list by category. |
| FR-03.4 | A colour picker offering at least 10 preset colours shall be provided when creating/editing a category. |

---

### FR-04 · Search

| ID | Requirement |
|----|-------------|
| FR-04.1 | Search shall query task titles and descriptions using a SQLite FTS5 virtual table. |
| FR-04.2 | Search results shall appear within 200 ms of the last keystroke (debounced at 300 ms). |
| FR-04.3 | Search shall be accessible from a persistent search icon in the top app bar. |

---

### FR-05 · Data Management

| ID | Requirement |
|----|-------------|
| FR-05.1 | All data shall be persisted locally using Room (SQLite). |
| FR-05.2 | The system shall support export of all tasks to a JSON file saved to the device's Downloads folder. |
| FR-05.3 | The trash (soft-deleted tasks) shall be auto-purged after 30 days. |

---

## 2. Non-Functional Requirements

| Category | Requirement | Target |
|----------|-------------|--------|
| **Performance** | App cold-start time on mid-range device | < 1.5 seconds |
| **Performance** | List scroll frame rate | ≥ 60 fps (no jank) |
| **Performance** | Search result latency | < 200 ms |
| **Offline** | All features must work with zero network access | 100% offline capable |
| **Storage** | APK/AAB size (release build, minified) | < 10 MB |
| **Storage** | Data stored locally using Room | SQLite on internal storage |
| **Compatibility** | Minimum Android version | API 26 (Android 8.0) |
| **Compatibility** | Target Android version | API 35 (Android 15) |
| **Accessibility** | All interactive elements must have content descriptions | TalkBack compatible |
| **Accessibility** | Minimum touch target size | 48 × 48 dp |
| **Accessibility** | Colour contrast ratio (text on background) | ≥ 4.5:1 (WCAG AA) |
| **Reliability** | Reminder delivery rate after boot | ≥ 98% |
| **Maintainability** | Test code coverage (unit + integration) | ≥ 70% |
| **Privacy** | Zero data leaves the device | No network calls, no analytics SDK |
| **Localisation** | Initial release language | English (en) |

---

## 3. Permissions Required

| Permission | API Level | Reason |
|-----------|-----------|--------|
| `POST_NOTIFICATIONS` | API 33+ | Display reminder notifications |
| `SCHEDULE_EXACT_ALARM` | API 31+ | Fire reminders at exact times via AlarmManager |
| `USE_EXACT_ALARM` | API 33+ | Alternative to SCHEDULE_EXACT_ALARM for clock apps |
| `RECEIVE_BOOT_COMPLETED` | All | Reschedule alarms after device reboot |
| `VIBRATE` | All | Haptic feedback on notification delivery |

> **Note:** `SCHEDULE_EXACT_ALARM` requires a runtime permission check on API 31+. The app must call `AlarmManager.canScheduleExactAlarms()` and gracefully fall back to `setWindow()` if the permission is not granted.

---

## 4. System Constraints

- **No internet permission** — `android.permission.INTERNET` shall NOT be declared in the manifest.
- **No third-party analytics** — No Firebase Analytics, Crashlytics, or any SDK that transmits data.
- **Room database migrations** — Every schema change must include a Room `Migration` object; destructive migrations are not permitted in production builds.
- **ProGuard/R8** — Release builds must enable minification and resource shrinking.
