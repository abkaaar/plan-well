# Release keystore (local only)

This folder holds the **release signing key** for Play Store uploads.

## Generate (one time)

From the repo root (JDK `keytool` on PATH):

```powershell
New-Item -ItemType Directory -Force keystore | Out-Null
keytool -genkeypair -v `
  -storetype PKCS12 `
  -keystore keystore/planwell-release.jks `
  -alias planwell `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -dname "CN=Plan Well, OU=Mobile, O=PlanWell, C=US"
```

Then add to **gitignored** `local.properties` (never commit):

```properties
keystore.path=keystore/planwell-release.jks
keystore.password=YOUR_STORE_PASSWORD
key.alias=planwell
key.password=YOUR_KEY_PASSWORD
```

Or set env vars for CI: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Build signed AAB

```powershell
.\gradlew.bat :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Security

- Back up the `.jks` and passwords in a password manager.
- Losing the keystore means you cannot update the same Play listing.
- Do **not** commit `*.jks`, `*.keystore`, or signing passwords.
