# Plan Well — Product Requirements Document (PRD)

**Version:** 1.0  
**Author:** Dev (Foodma Technologies)  
**Platform:** Android  
**Status:** Approved

---

## 1. Overview

**Plan Well** is a lightweight, fully offline Android application that helps individuals organise their daily lives by planning tasks, setting reminders, and maintaining personal notes — all without requiring an internet connection. The app is designed for simplicity and speed, built natively in Kotlin.

> **Mission:** Give every person a calm, reliable personal planning companion that works anywhere, anytime — even without data.

---

## 2. Target Users

| Persona | Description |
|---------|-------------|
| **Students** | Scheduling assignments, exams, and class reminders in low-connectivity environments. |
| **Professionals** | Tracking meetings, deadlines, and personal to-dos alongside work commitments. |
| **Home Managers** | Planning household tasks, shopping lists, and family schedules with recurring reminders. |
| **Users in Low-Connectivity Regions** | People in areas with unreliable internet who need a planning tool that always works. |

---

## 3. Core Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Task Management** | Create, edit, delete, and complete tasks with title, description, due date, and priority. | P0 — Must Have |
| **Reminders** | Set one-time or recurring (daily, weekly) reminders with local push notifications. | P0 — Must Have |
| **Categories / Labels** | Organise tasks into user-defined categories (Work, Personal, Health, etc.) with colour coding. | P1 — Should Have |
| **Quick Notes** | Attach freeform notes to any task or create standalone notes. | P1 — Should Have |
| **Search** | Full-text search across tasks and notes, locally. | P1 — Should Have |
| **Dashboard / Today View** | A summary home screen showing today's tasks, upcoming reminders, and completion stats. | P1 — Should Have |
| **Completion Tracking** | Mark tasks complete, view history. Progress ring showing daily completion rate. | P1 — Should Have |
| **Data Export** | Export tasks to a local JSON/CSV file for backup. | P2 — Nice to Have |
| **Widget** | Android home-screen widget showing today's tasks. | P2 — Nice to Have |

---

## 4. Non-Goals (Out of Scope for v1.0)

- No cloud sync or account/login system
- No collaboration or sharing features
- No iOS version
- No AI/ML-powered suggestions
- No calendar integration with Google/Outlook

---

## 5. User Stories

### Task Management

```
As a user, I want to create a task with a title and due date
so that I can keep track of what I need to do and when.

As a user, I want to set a priority level on my tasks
so that I can focus on what matters most first.

As a user, I want to mark a task as complete
so that I can see my progress through the day.

As a user, I want to delete a task (with a trash/undo option)
so that I don't accidentally lose tasks permanently.
```

### Reminders

```
As a user, I want to receive a notification reminder for a task
so that I don't forget about it even when the app is closed.

As a user, I want to set a recurring daily reminder
so that I'm prompted about regular habits without re-creating reminders each time.

As a user, I want reminders to still work after I restart my phone
so that I can rely on the app in my daily routine.
```

### Organisation

```
As a user, I want to create categories for my tasks
so that I can separate work tasks from personal ones visually.

As a user, I want to search for a task by keyword
so that I can find specific tasks quickly without scrolling.
```

---

## 6. Success Metrics

| Metric | Target |
|--------|--------|
| Day-30 Retention | ≥ 40% of users return after 30 days |
| Play Store Rating | ≥ 4.3 stars within 90 days of launch |
| Crash-Free Sessions | ≥ 99.5% (via Firebase Crashlytics) |
| Reminder Delivery Rate | ≥ 98% of scheduled reminders delivered on time |
| Cold Start Time | < 1.5 seconds on mid-range device |

---

## 7. Assumptions & Constraints

- The app will be free on the Play Store at launch (no monetisation in v1.0).
- All data is stored locally on-device; there is no backend.
- The development team is a solo developer at initial release.
- Minimum supported Android version: API 26 (Android 8.0 Oreo).
- APK/AAB release size target: < 10 MB.
