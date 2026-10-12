# AGENTS.md — Trip Checker

Guidance for AI coding agents (Cursor, Claude Code, etc.) working in this repo.
**The task list and full specs are in [`docs/PLAN.md`](docs/PLAN.md). Read it before writing code.**

> ⚠️ **Review fixes come first.** A code review of the M0–M6 work found crashes, data-loss bugs and a failing test suite. Work through every unchecked `[ ]` item in [`docs/CODE_REVIEW_README.md`](docs/CODE_REVIEW_README.md), in its §8 order, **before** taking any new task from `docs/PLAN.md` §4. Follow the same loop below (test-first, verify, log) for each one.

## What we're building

Trip Checker is an **offline** Android app for one person who travels by Indian Railways about every 2 weeks.
- They import IRCTC e-ticket PDFs (ERS).
- The app extracts the key fields: PNR, train, from/boarding/to, times, class, quota, and passengers with coach and berth.
- It **auto-groups an outbound journey and its return (within 14 days) into one round-trip card**.
- It shows a summary when a card is tapped.
- **"Go to ticket"** opens the original PDF in an external PDF app.

Owner decisions are final and listed in `docs/PLAN.md` §1. Don't revisit them.

## Autonomous work loop (follow this exactly)

The owner wants you to work **continuously and autonomously** through the plan. Do not stop to ask questions.

1. **Resume point:** first open `docs/CODE_REVIEW_README.md` and take its first unchecked `[ ]` item. Only when every item there is ticked, open `docs/PLAN.md` and find the first unchecked `[ ]` task in §4. Either way, check PLAN §9 (Progress log) and §8 (Blockers).
2. **Implement the task completely.** No stubs, no `TODO()`, no "left as an exercise". Follow the spec in §3 and §5 of PLAN.md.
3. **Test it.** Write or extend unit tests for every pure-Kotlin piece. Parser and grouper work is **test-first**.
4. **Verify:**
   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest
   ```
   Fix until green. Never weaken, delete, or `@Ignore` a test to get green. Fix the code, or fix a test that is genuinely wrong and log why.
5. **Record:**
   - tick the task `[x]` in PLAN.md
   - append one line to §9 Progress log: `YYYY-MM-DD HH:MM · M1.4 · what was done · tests added`
   - append every judgment call to §7 Decisions log
6. **Continue** straight to the next task. Do **not** pause to summarize or ask "shall I continue?".
7. **If stuck** after 3 genuinely different attempts on the same error:
   - try the documented fallback (see "Dependency troubleshooting")
   - if it is still stuck, write a §8 Blockers entry (what, tried, needed) and move on to the **next independent task**
   - come back to it later
8. **Milestone end:**
   - run the full verify command plus `.\gradlew.bat lintDebug` (from M6 onward lint must be clean of errors)
   - make sure every box in the milestone is ticked before starting the next one
9. When M0–M6 are all done, continue into M7 (stretch). When everything is done, do a final review pass covering consistency, KDoc, and dead code.

A new chat session (fresh context) resumes from the PLAN.md checkboxes and logs. Keep them accurate, because they are the memory between sessions.

## Project facts (as of the starter template)

| Item | Value |
|---|---|
| Package / applicationId / namespace | `com.example.tripchecker` (**do not rename**) |
| Source root | `app/src/main/java/com/example/tripchecker/` (Kotlin lives under `java/`; keep it) |
| AGP | 9.4.1 with **built-in Kotlin**. There is **no** `org.jetbrains.kotlin.android` plugin, and you must not add one. |
| Kotlin | 2.2.10 (`kotlin` in the version catalog; used by the compose plugin) |
| Gradle | 9.6.0 wrapper, configuration cache on, JDK 25 daemon toolchain |
| SDK | compileSdk 37 (new DSL `compileSdk { version = release(37) }`), minSdk 28, targetSdk 37 |
| Java target | 11 (`compileOptions`) |
| Compose | BOM 2026.02.01, Material 3, the existing `TripcheckerTheme` (dynamic colour) |
| R8 rules | `app/src/main/keepRules/rules.keep` (AGP 9 location; there is no `proguard-rules.pro`). Release has `optimization { enable = false }` |
| Versions | all in `gradle/libs.versions.toml`. Add every new dependency there and reference it via `libs.*` |

## Tech stack

- **UI:** Jetpack Compose, Material 3, single activity, **navigation-compose with type-safe `@Serializable` routes**.
- **State:** ViewModel + `StateFlow`, collected with `collectAsStateWithLifecycle()`.
- **DI:** **manual**. `AppContainer` is created in `TripCheckerApp` (a custom `Application`). ViewModels are built with `viewModelFactory { initializer { … } }`. **No Hilt, Dagger or Koin.**
- **Persistence:** Room (via KSP) with schema export to `app/schemas/`. PDFs are copied into `filesDir/tickets/<ticketId>.pdf`, and only the file name is stored.
- **PDF text layer:** PdfBox-Android (`com.tom_roush.pdfbox`).
- **OCR fallback:** `android.graphics.pdf.PdfRenderer` → bitmap → ML Kit Text Recognition v2, **bundled** model (offline).
- **Async:** coroutines. `kotlinx-coroutines-play-services` provides `Task.await()` for ML Kit.
- **Time:** `java.time` only. **All ticket times are IST** (`ZoneId.of("Asia/Kolkata")`) for both parsing and display. Inject `java.time.Clock`; never call `now()` directly in domain code.
- **Tests:** JUnit4 on the JVM (`app/src/test`). Optional instrumented tests go in `app/src/androidTest`.

## Dependencies to add (M0.1)

Use the **latest stable** release of each library that works with AGP 9.4.1 + Kotlin 2.2.10. The versions shown are known-good minimums; newer stable versions are fine. **Never use alpha/beta/RC.**

| Purpose | Coordinates (catalog alias suggestion) | Notes |
|---|---|---|
| KSP plugin | `com.google.devtools.ksp` (`ksp`) | Prefer the newest stable **KSP2** (2.3.x+), which is versioned independently of Kotlin. Fallback: `2.2.10-2.0.2`. |
| Room | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (via `ksp(...)`); plugin `androidx.room` | latest stable **2.8.x** (≥ 2.7.0). **Not** Room 3 (`androidx.room3`). Configure `room { schemaDirectory("$projectDir/schemas") }`. |
| Serialization | plugin `org.jetbrains.kotlin.plugin.serialization` with **`version.ref = "kotlin"`**; `org.jetbrains.kotlinx:kotlinx-serialization-json` | The plugin version must equal the Kotlin version. Use JSON ≥ 1.8.x. |
| Navigation | `androidx.navigation:navigation-compose` | ≥ 2.9.x for type-safe routes. Not Navigation 3. |
| Lifecycle | `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | ≥ 2.9.x, all on one shared version ref. |
| Activity / Core | `androidx.activity:activity-compose`, `androidx.core:core-ktx` | Bump from the template's 1.8.0 / 1.10.1 to latest stable. |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android`, `kotlinx-coroutines-play-services`, `kotlinx-coroutines-test` (test) | ≥ 1.10.x, one shared version ref. |
| OCR | `com.google.mlkit:text-recognition` | ≥ 16.0.1. This is the **bundled** artifact; do *not* use `play-services-mlkit-text-recognition`, which downloads its model and needs network. |
| PDF text | `com.tom-roush:pdfbox-android` | ≥ 2.0.27.0 |
| Icons | `androidx.compose.material:material-icons-core` | No version; it comes from the Compose BOM. **Never** `material-icons-extended`. Other icons are vector drawables in `res/drawable`. |
| Test (androidTest) | `androidx.room:room-testing`, bump `androidx.test.ext:junit` / `espresso-core` to latest stable | optional |

Root `build.gradle.kts`: add `alias(libs.plugins.ksp) apply false`, `alias(libs.plugins.room) apply false` and `alias(libs.plugins.kotlin.serialization) apply false`. In `app/build.gradle.kts`, apply them.

### Dependency troubleshooting (try in order, and log what you did in the Decisions log)

1. **KSP fails with AGP 9 built-in Kotlin** (e.g. "KSP requires kotlin-android plugin", source sets not found):
   - upgrade KSP to the newest stable 2.3.x
   - if that still fails, bump `kotlin` in the catalog to the newest stable 2.2.x/2.3.x that the new KSP supports. The compose and serialization plugins follow it automatically via `version.ref`.
2. **Last resort only:** opt out of built-in Kotlin. Set `android.builtInKotlin=false` (and `android.newDsl=false` if required) in `gradle.properties`, and apply `org.jetbrains.kotlin.android` with `version.ref = "kotlin"`. Write a Decisions log entry explaining exactly why.
3. **Duplicate-class or META-INF conflicts** from PdfBox or ML Kit: add `packaging { resources { excludes += ... } }` for the specific path in the error.
4. **Room "schema export directory not provided":** make sure the `androidx.room` plugin is applied and `room { schemaDirectory(...) }` is set.
5. **Gradle can't find a version:** check the exact artifact name on Google Maven (`maven.google.com`) or Maven Central, then pick the newest stable.
6. Do **not** upgrade AGP, Gradle, or the Compose BOM unless a build failure genuinely requires it. Log it if you do.

## Commands (Windows / PowerShell; the repo root is the cwd)

```powershell
.\gradlew.bat assembleDebug                         # build
.\gradlew.bat testDebugUnitTest                     # JVM unit tests (the main gate)
.\gradlew.bat testDebugUnitTest --tests "com.example.tripchecker.extraction.parser.*"   # focused
.\gradlew.bat lintDebug                             # lint (gate from M6)
.\gradlew.bat installDebug                          # install on a connected device/emulator
.\gradlew.bat connectedDebugAndroidTest             # instrumented tests (optional; needs a device)
```

Test reports are in `app/build/reports/tests/testDebugUnitTest/index.html`. If a test fails, read the XML in `app/build/test-results/` for the exact assertion.

## Architecture rules

- **Pure-Kotlin zones**: no `android.*` or `androidx.*` imports. Everything here must be JVM-unit-testable:
  - `domain/**`
  - `extraction/parser/**`
  - `extraction/RowMerger.kt`
  - `importer/ParsedTicketMapper.kt`
  - the logic in `importer/TicketImporter.kt`, which talks only to interfaces
- **Tickets are the single source of truth.** Journey groups (round trips) are **derived** by `TripGrouper` and never persisted. Manual overrides live on the ticket as `linkGroupId` and `autoPairDisabled`.
- **Repository interface** (`TicketRepository`) sits in front of Room. ViewModels never touch DAOs. This boundary is where Supabase sync plugs in later.
- **Sync-ready** (future Supabase phase; build nothing for it now):
  - UUID `String` ids
  - `createdAt`/`updatedAt` on rows
  - PDFs identified by SHA-256
  - domain models separate from entities
- **Never crash on a bad PDF.** Extraction and parsing return nullable or sealed results. An unreadable ticket is still saved with `needsReview = true` and its PDF kept.
- **Store only file names** in the DB, never absolute paths. Resolve them through `TicketFileStore`.

## Code conventions

- Kotlin official style (`kotlin.code.style=official`), 4-space indent.
- Keep files focused, under about 400 lines. Split large composables into `components/`.
- Each screen has a stateful `XScreen(viewModel, navigate…)` and a stateless `XContent(state, onEvent…)`. Add `@Preview`s on the stateless content with fake data.
- ViewModels expose a single `StateFlow<XUiState>` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. One-off events (snackbar, navigation) go through a `Channel` exposed as a `Flow`.
- Put user-visible strings in `res/values/strings.xml`.
- Add a one-line KDoc on every public class and on non-obvious functions. Don't add comments that restate the code.
- Test names describe behaviour in backticks: `` `pairs outbound with earliest return within 14 days` ``.
- Room handles its own threading for `suspend` and `Flow`. Wrap file I/O in `withContext(Dispatchers.IO)` and OCR/bitmap work in `Dispatchers.Default`.

## Hard don'ts

- ❌ Renaming the package, namespace, or applicationId.
- ❌ Adding the `INTERNET` permission, analytics, Firebase, crash reporters, or any network call. The app is fully offline.
- ❌ An in-app PDF viewer. "Go to ticket" uses FileProvider + `ACTION_VIEW` (owner decision).
- ❌ Hilt, Dagger, Koin, or kapt.
- ❌ Applying `org.jetbrains.kotlin.android`, except through the documented last-resort fallback.
- ❌ `material-icons-extended`, `fallbackToDestructiveMigration`, or Room 3 alphas.
- ❌ Deleting, skipping, or weakening tests to get green.
- ❌ Calling `LocalDateTime.now()` or `System.currentTimeMillis()` in domain/parser code. Inject a `Clock`.
- ❌ Taking a PNR from anywhere other than the PNR label row; page 2 has 10-digit phone numbers.
- ❌ Committing or reading files in `samples/` from tests. That folder holds the owner's real tickets for manual QA and is gitignored.
- ❌ Editing `local.properties`.
- ❌ Adding a library that isn't in the list above without a Decisions log entry. Any new library must work offline.
