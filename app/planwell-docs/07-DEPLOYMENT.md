# Plan Well — Deployment & Release Notes

**Version:** 1.0  
**Platform:** Google Play Store  
**Application ID:** `com.planwellapp.app`

---

## 1. Versioning Strategy

Plan Well uses **Semantic Versioning**: `MAJOR.MINOR.PATCH`

| Part | Increment When |
|------|---------------|
| MAJOR | Breaking change (e.g. DB migration with no fallback, complete rewrite) |
| MINOR | New feature added in backward-compatible manner |
| PATCH | Bug fix, performance improvement, no new feature |

The `versionCode` is an always-increasing integer for Play Store ordering.

```kotlin
// build.gradle.kts (app module)
android {
    defaultConfig {
        applicationId   = "com.planwellapp.app"
        minSdk          = 26
        targetSdk       = 35
        versionCode     = 1          // Increment on every Play Store release
        versionName     = "1.0.0"   // MAJOR.MINOR.PATCH
    }
}
```

### Version History

| versionCode | versionName | Notes |
|-------------|-------------|-------|
| 1 | 1.0.0 | Initial release |
| *(next)* | 1.0.1 | Bug fixes |
| *(next)* | 1.1.0 | Widget + export |

---

## 2. Build Configuration

### Release Build Setup

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"       // Install alongside release build
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled     = true            // Enable R8/ProGuard
            isShrinkResources   = true            // Remove unused resources
            isDebuggable        = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    signingConfigs {
        create("release") {
            // Read from environment variables (CI) or local.properties (local build)
            storeFile   = file(System.getenv("KEYSTORE_PATH") ?: properties["keystore.path"].toString())
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: properties["keystore.password"].toString()
            keyAlias    = System.getenv("KEY_ALIAS") ?: properties["key.alias"].toString()
            keyPassword = System.getenv("KEY_PASSWORD") ?: properties["key.password"].toString()
        }
    }
}
```

> ⚠️ **Security:** Never commit `local.properties` or your keystore file to version control. Add both to `.gitignore`.

```gitignore
# .gitignore additions
local.properties
*.jks
*.keystore
```

---

## 3. ProGuard / R8 Rules

```proguard
# proguard-rules.pro

# --- Room ---
# Keep all entity and DAO classes
-keep class com.planwell.app.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.planwell.app.** {
    @kotlinx.serialization.Serializable <methods>;
}

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keep class com.planwell.app.data.alarm.** extends androidx.work.Worker { *; }
-keep class com.planwell.app.data.alarm.** extends androidx.work.ListenableWorker { *; }

# --- BroadcastReceivers ---
-keep class com.planwell.app.data.alarm.ReminderReceiver { *; }
-keep class com.planwell.app.data.alarm.BootReceiver { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- General Android ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

---

## 4. AndroidManifest Checklist

```xml
<!-- AndroidManifest.xml — verify these are present for release -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <!-- API 31+ exact alarm permission -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"
        android:minSdkVersion="31"/>
    <!-- API 33+ alternative (for clock-category apps) -->
    <uses-permission android:name="android.permission.USE_EXACT_ALARM"
        android:minSdkVersion="33"/>

    <!-- NO internet permission — intentionally omitted -->

    <application
        android:name=".PlanWellApplication"
        android:allowBackup="true"
        android:fullBackupContent="@xml/backup_rules"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:theme="@style/Theme.PlanWell"
        android:supportsRtl="true">

        <!-- BroadcastReceivers -->
        <receiver android:name=".data.alarm.ReminderReceiver"
            android:exported="false"/>
        <receiver android:name=".data.alarm.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED"/>
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

---

## 5. Play Store Submission Checklist

### Account & Legal
- [ ] Google Play Developer account active and in good standing
- [ ] Developer Program Policies reviewed
- [ ] Privacy Policy URL live and accessible (required even for offline apps)
- [ ] Privacy Policy states: no data collected, no internet connection made

### Build Artifacts
- [ ] Release keystore generated and stored securely (password manager)
- [ ] Signed Android App Bundle (.aab) built in release mode
- [ ] Signed AAB tested on minimum 2 physical devices
- [ ] Confirmed: no internet permission in manifest
- [ ] Confirmed: `isDebuggable = false` in release build
- [ ] Confirmed: minify + resource shrink enabled

### Play Console — Store Listing
- [ ] App title: "Plan Well - Personal Planner" (max 30 chars)
- [ ] Short description (max 80 chars): "Offline task planner with reminders. Simple, private, always works."
- [ ] Full description written (max 4000 chars)
- [ ] App category: Productivity
- [ ] Contact email set

### Graphics & Assets
- [ ] App icon: 512 × 512 PNG, no transparency, no rounded corners (Play applies them)
- [ ] Feature Graphic: 1024 × 500 PNG (required for Play Store listing)
- [ ] Screenshots: minimum 2 phone screenshots (1080 × 1920 or 1440 × 2560 px)
- [ ] Screenshots show real tasks, not placeholder content

### Play Console — Safety & Ratings
- [ ] Data Safety form completed: all fields "No data collected"
- [ ] IARC content rating completed: "Everyone"
- [ ] Ads declaration: "No"

### Technical
- [ ] Target API level meets current Play Store requirement (API 34+ for new apps from 2024)
- [ ] App tested on API 26 (min SDK) emulator — no crashes
- [ ] App tested on API 35 (target SDK) device — no crashes
- [ ] ANR (Application Not Responding) rate: 0% on internal track before promoting

---

## 6. Rollout Strategy

| Stage | Track | Audience | Duration | Go / No-Go Criteria |
|-------|-------|----------|----------|---------------------|
| 1 | Internal Testing | Dev + 5 testers | 3 days | Zero crashes in session logs |
| 2 | Closed Testing (Alpha) | 20–50 users | 5 days | Crash-free rate ≥ 99%, no P0 bugs |
| 3 | Production (20% rollout) | 20% of new installs | 3 days | ANR rate < 0.47%, crash rate < 1% |
| 4 | Production (100%) | All users | Permanent | No regression vs Stage 3 |

> **OEM Reminder Issue:** Some Android OEMs (Xiaomi MIUI, Huawei EMUI, Samsung One UI) aggressively kill background processes. Include an in-app one-time prompt guiding users to whitelist the app in battery settings.

---

## 7. Release Notes

### v1.0.0 — Initial Release

**What's new in Plan Well v1.0.0:**

```
🎉 Welcome to Plan Well — your simple, private, offline-first personal planner.

✅ Create and manage tasks with priorities, due dates, and descriptions
🔔 Set one-time and recurring reminders (daily, weekly) — even works after reboot  
🏷  Organise tasks into colour-coded categories
📊 Today dashboard showing your daily plan and completion progress
🔍 Fast full-text search across all your tasks
🌙 Dark Mode support
🔒 100% offline — your data never leaves your device

No account needed. No internet required. No data collected. Just you and your plans.
```

---

## 8. Post-Release Monitoring

Even without analytics, monitor these signals from Play Console:

| Signal | Where | Target |
|--------|-------|--------|
| Crash rate | Android Vitals → Crashes | < 1% crash-free session floor |
| ANR rate | Android Vitals → ANRs | < 0.47% (Play Store threshold) |
| Ratings & reviews | Reviews tab | Respond to all 1–3 star reviews within 48 h |
| Install / uninstall | Statistics tab | Monitor Day-1, Day-7, Day-30 retention |
| Permission denial | — | Track via custom local log if SCHEDULE_EXACT_ALARM denied |

---

## 9. CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: gradle-${{ hashFiles('**/*.gradle.kts') }}

      - name: Lint
        run: ./gradlew lint

      - name: Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: JaCoCo Coverage Report
        run: ./gradlew jacocoTestReport

      - name: Build Debug APK
        run: ./gradlew assembleDebug

  release-build:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    needs: build-and-test
    steps:
      - uses: actions/checkout@v4
      - name: Build Signed AAB
        env:
          KEYSTORE_PATH: ${{ secrets.KEYSTORE_PATH }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew bundleRelease
```
