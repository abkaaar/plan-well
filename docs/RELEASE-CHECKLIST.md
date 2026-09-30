# Phase 5 — Release checklist

Use this alongside [play-store-listing.md](play-store-listing.md) and [privacy-policy.html](privacy-policy.html).

## Done in repo

- [x] `versionCode` 1 / `versionName` `1.0.0`
- [x] Release minify + shrink + ProGuard rules
- [x] Conditional release signing (`local.properties` / env)
- [x] Keystore instructions (`keystore/README.md`)
- [x] Privacy policy HTML
- [x] Play listing copy + Data Safety guidance
- [x] Store asset placeholders (`docs/store-assets/`)

## You must do (outside the repo)

- [ ] Generate keystore; save passwords in a password manager; fill `local.properties`
- [ ] Host privacy policy at a public HTTPS URL (GitHub Pages, etc.)
- [ ] Replace `privacy@planwell.app` with your real contact email
- [ ] Build: `.\gradlew.bat :app:bundleRelease`
- [ ] Install release build on ≥2 physical devices and smoke-test
- [ ] Capture ≥2 phone screenshots (1080×1920) with real tasks
- [ ] Create / refine 512×512 icon + 1024×500 feature graphic
- [ ] Play Console: create app, upload AAB to Internal testing
- [ ] Complete Data Safety, IARC (Everyone), Ads = No
- [ ] Promote Internal → Production (20% then 100%)

## Build commands

```powershell
# Debug (side-by-side package: com.planwellapp.app.debug)
.\gradlew.bat :app:installDebug

# Signed release bundle (requires keystore props)
.\gradlew.bat :app:bundleRelease
# → app\build\outputs\bundle\release\app-release.aab
```
