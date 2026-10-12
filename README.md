# Trip Checker

Offline Android app for organizing IRCTC e-ticket PDFs: import, extract key fields (text layer + OCR), auto-pair round trips within 14 days, and open the original PDF in an external viewer.

Package: `com.example.tripchecker`. Full spec and task list: [`docs/PLAN.md`](docs/PLAN.md).

## Prerequisites (development)

| Requirement | Notes |
|-------------|--------|
| **JDK 17+** | Gradle/AGP 9 do not run on Java 8. On Windows, point `JAVA_HOME` at a modern JDK (e.g. JetBrains Runtime 21 under `%USERPROFILE%\.jdks\`). |
| **Android SDK** | `compileSdk` / `targetSdk` **37** — install “Android API 37” in SDK Manager. SDK path via `local.properties`: `sdk.dir=...` (gitignored). |
| **Gradle 9.6** | Wrapper in `gradle/wrapper/`; first build downloads the distribution (needs network). |

## Build and test

From the repo root (PowerShell):

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.6"   # adjust to your JDK
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug          # M6 gate
.\gradlew.bat assembleRelease    # M6.3 R8 check (release optimization)
.\gradlew.bat installDebug       # device/emulator required
```

If the Gradle wrapper cannot download (corporate network), use Android Studio **Terminal** (same JDK/SDK as the IDE) or run `scripts\verify.ps1`.

Manual QA: [`docs/QA.md`](docs/QA.md).

## Architecture (short)

- **UI:** Jetpack Compose, single activity, type-safe Navigation
- **State:** ViewModels + `StateFlow`
- **DI:** manual `AppContainer` (no Hilt)
- **Data:** Room; PDFs in `filesDir/tickets/`; import staging in `cacheDir/staging/`
