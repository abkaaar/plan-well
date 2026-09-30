# Plan Well — Play Store Listing (v1.0.0)

Copy/paste into Google Play Console. Host `docs/privacy-policy.html` publicly and paste that URL into Console.

---

## App identity

| Field | Value |
|-------|--------|
| **Title** (≤30) | Plan Well - Personal Planner |
| **Application ID** | `com.planwellapp.app` |
| **Category** | Productivity |
| **Tags** | tasks, planner, reminders, offline, todo |
| **Ads** | No |
| **Content rating** | Everyone (IARC) |

---

## Short description (≤80)

```
Offline task planner with reminders. Simple, private, always works.
```

---

## Full description

```
Plan Well is a simple, private, offline-first personal planner.

Create tasks with priorities, due dates, and notes. Set reminders that fire even after reboot. Organise work with colour-coded categories. Search instantly across everything you have planned. See today’s progress on the home dashboard.

Why Plan Well?
• 100% offline — no account, no ads, no tracking
• Your tasks stay on your device
• Reminders survive reboot
• Dark mode supported
• Fast full-text search
• Optional JSON export when you want a backup

No internet required. No data collected. Just you and your plans.
```

---

## What’s new (release notes)

```
Welcome to Plan Well v1.0.0 — your simple, private, offline-first planner.

• Tasks with priorities, due dates, and descriptions
• Reminders (one-time, daily, weekly) that survive reboot
• Colour-coded categories and home filters
• Today dashboard with completion progress
• Full-text search
• Dark Mode
• JSON export to Downloads

No account. No internet. No data collected.
```

---

## Data Safety (Play Console)

Answer **No** / does not collect for all data types:

- No location, personal info, financial, health, messages, photos, files, calendar, contacts, app activity analytics, web browsing, app info & performance diagnostics sent off-device, device IDs.
- Data is encrypted in transit: **N/A** (no network).
- Users can request deletion: data is local — uninstalling the app removes it.

---

## Graphics checklist

| Asset | Spec | Status |
|-------|------|--------|
| High-res icon | 512×512 PNG, no alpha, square (no rounded mask) | See `docs/store-assets/` |
| Feature graphic | 1024×500 PNG | See `docs/store-assets/` |
| Phone screenshots | ≥2, recommend 1080×1920 | Capture on device (manual) |

### Capture screenshots (device)

1. Create a few real tasks + categories on your phone.
2. Open Home, Search, Categories, Task detail.
3. `adb -s <serial> exec-out screencap -p > docs/store-assets/screenshot-home.png`
4. Crop/resize to 1080×1920 if needed before upload.

---

## Console rollout

1. Internal testing → smoke test on 2 devices  
2. Closed testing (optional)  
3. Production 20% → watch crash/ANR 3 days → 100%
