# Plan Well — Documentation Suite

**Platform:** Android (Kotlin)  
**Architecture:** MVVM + Clean Architecture  
**Database:** Room (SQLite)  
**Reminders:** AlarmManager + WorkManager  
**Version:** 1.0.0  

---

## Documents

| File | Description |
|------|-------------|
| [`01-PRD.md`](./01-PRD.md) | Product Requirements Document |
| [`02-SRS.md`](./02-SRS.md) | Software Requirements Specification |
| [`03-TDD.md`](./03-TDD.md) | Technical Design Document |
| [`04-UIUX.md`](./04-UIUX.md) | UI/UX Design Specifications |
| [`05-ROADMAP.md`](./05-ROADMAP.md) | Project Plan & Roadmap |
| [`06-TEST-PLAN.md`](./06-TEST-PLAN.md) | Test Plan & QA Documentation |
| [`07-DEPLOYMENT.md`](./07-DEPLOYMENT.md) | Deployment & Release Notes |
| [`planwell-docs.html`](./planwell-docs.html) | Interactive visual version (open in browser) |

---

## Quick Reference — Tech Stack

```
Language       : Kotlin 2.0
UI             : Jetpack Compose 1.6.x
Navigation     : Navigation Compose 2.7.x
Database       : Room 2.6.x  (SQLite + FTS5)
DI             : Hilt 2.51
Reminders      : AlarmManager (exact) + WorkManager (recurring/boot)
Async          : Kotlin Coroutines + StateFlow
Serialisation  : Kotlinx Serialization 1.6.x
Testing        : JUnit5 · MockK · Turbine · Espresso · Compose Test
Min SDK        : API 26 (Android 8.0)
Target SDK     : API 35 (Android 15)
```

---

## Project Structure

```
app/
├── ui/
│   ├── home/
│   ├── task/
│   ├── reminder/
│   ├── category/
│   └── search/
├── domain/
│   ├── model/
│   └── usecase/
├── data/
│   ├── local/
│   │   ├── db/
│   │   └── dao/
│   └── repository/
└── di/
```

---

*Generated for Dev — Foodma Technologies*
