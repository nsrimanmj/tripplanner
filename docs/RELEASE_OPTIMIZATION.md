# Release build optimization (2026-10-13)

Measured on AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10 (KGP 2.3.21), R8 bundled with AGP, compileSdk/targetSdk 37, minSdk 28. Kotlin DSL, single `:app` module, no flavors, no release signing config.

## Results

| Artifact (unsigned) | Bytes | MiB | vs. baseline |
|---|---:|---:|---:|
| **Baseline** `app-release-unsigned.apk` (R8 off, 4 ABIs) | 83,275,287 | 79.42 | — |
| `app-universal-release-unsigned.apk` (all 4 ABIs) | 49,056,626 | 46.78 | −34,218,661 (−41.1%) |
| `app-arm64-v8a-release-unsigned.apk` (most phones) | 19,019,703 | 18.14 | −64,255,584 (−77.2%) |
| `app-armeabi-v7a-release-unsigned.apk` | 14,737,103 | 14.05 | −82.3% |
| `app-x86_64-release-unsigned.apk` / `app-x86-…` | 19,581,281 / 19,516,195 | 18.67 / 18.61 | −76.5% |
| `app-release.aab` (`bundleRelease`; Play builds per-device APKs from it) | 26,030,948 | 24.82 | n/a (different artifact) |

DEX method references: 191,360 → 25,772 (−86.5%).

### Where the size went (compressed bytes inside the APK)

| Category | Baseline | Universal now | arm64 now |
|---|---:|---:|---:|
| DEX | 33,878,756 (3 files) | 4,244,008 (1 file) | 4,244,008 |
| Native `libmlkit_google_ocr_pipeline.so` | 41,070,052 (4 ABIs) | 41,070,052 | 11,074,640 |
| BouncyCastle Java resources | 4,150,840 | 12,770 | 12,770 |
| PdfBox assets (fonts, CMaps, AFM) | 1,618,605 | 1,618,605 | 1,618,605 |
| ML Kit OCR models (assets) | 1,272,325 | 1,272,325 | 1,272,325 |
| `resources.arsc` + `res/` | 1,087,641 | 666,972 | 666,972 |

Contributions to the universal reduction: R8 code shrinking −29.6 MB (86.6%), BouncyCastle PQC exclusion −4.1 MB (12.1%), resource shrinking −0.4 MB. The ABI split saves a further 30 MB per device APK.

## Root cause of the 80 MB APK

1. **R8 was off** (`optimization { enable = false }`): 33.9 MB of unshrunk DEX from Compose, ML Kit/GMS, PdfBox and BouncyCastle.
2. **One native library ×4 ABIs**: ML Kit's bundled OCR pipeline (`libmlkit_google_ocr_pipeline.so`, 6.8–11.6 MB per ABI) is required, since offline bundled OCR is an owner decision. It is stored uncompressed and page-aligned, as is standard for minSdk ≥ 23.
3. **4.1 MB of BouncyCastle post-quantum tables** (`org/bouncycastle/pqc/**`: Picnic, SIKE) pulled in by `pdfbox-android` → `bcpkix` → `bcprov`, and never used.

## Changes

| File | Change | Why |
|---|---|---|
| `app/build.gradle.kts` | `optimization { enable = true }` | AGP 9.3+ DSL: R8 code shrinking/optimization/obfuscation, resource shrinking and the `proguard-android-optimize` defaults. R8 full mode is the default (no `android.enableR8.fullMode=false`). |
| `app/build.gradle.kts` | `packaging.resources.excludes += "org/bouncycastle/pqc/**"` | R8 removes the PQC classes but not Java resources; `mapping.txt` confirms no PQC class survives. |
| `app/build.gradle.kts` | `splits.abi` with all 4 ABIs + `isUniversalApk = true`, disabled for `bundle*` tasks | Per-ABI APKs for sideloading, and a universal APK for any device. AGP can't build an AAB with APK splits on (issuetracker 402800800). |
| `app/src/main/keepRules/rules.keep` | Removed `-keep class com.tom_roush.pdfbox.** { *; }`, `-keep class org.apache.pdfbox.** { *; }` and `-dontwarn org.bouncycastle.**`; kept `-dontwarn com.gemalto.jp2.**`; added `-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }` | See the R8 analyzer section. |

Debug builds are unchanged apart from per-ABI output names. They stay debuggable (`application-debuggable`), with no R8.

## R8 configuration analyzer (r8-analyzer skill, `:app:analyzeReleaseR8Config`)

| Score | Existing rules (R8 on) | Final |
|---|---:|---:|
| Optimization | 87.76% | 96.17% |
| Obfuscation | 88.03% | 97.04% |
| Shrinking | 88.00% | 96.44% |

- `-keep class com.tom_roush.pdfbox.** { *; }` was package-wide and kept 10,714 items (732 classes, 2,767 fields, 7,215 methods; 9.3% of live code) from all optimizations. **Removed.** PdfBox-Android ships consumer rules for its reflection (`SecurityHandler` constructors, `documentinterchange`). Its other reflective lookups load resources through absolute paths (`PDFBoxResourceLoader.getStream("com/tom_roush/…")` or `/com/tom_roush/…`), so they're safe under obfuscation.
- `-keep class org.apache.pdfbox.** { *; }` matched nothing in pdfbox-android. **Removed.**
- `-dontwarn org.bouncycastle.**`: R8 reports no missing BouncyCastle classes without it. **Removed.**
- `-dontwarn com.gemalto.jp2.**`: `JPXFilter` references this optional, absent JPEG 2000 dependency 6 times. **Kept.**
- **Added** `ComponentRegistrar { <init>(); }`. In full mode, firebase-components' consumer rule (`-keep class * implements …ComponentRegistrar`) keeps the classes but not their constructors. ML Kit's `MlKitInitProvider` instantiates them reflectively, so without the rule OCR silently returned no text (`NoSuchMethodException: CommonComponentRegistrar.<init>`). This was found on device, and it affects any R8 build with the old rules too.
- The remaining high-impact rules are library consumer rules (ML Kit `…zbuf <fields>` 2.2%, PdfBox `documentinterchange` 0.6%) or AGP defaults; the subsumed rules are all library duplicates. Nothing actionable in project rules.
- Not covered by keep rules: certificate-encrypted PDFs. PdfBox's `PublicKeySecurityHandler` creates `BouncyCastleProvider` reflectively. Such PDFs now fail extraction and land in needs-review, as the importer already handles. Password- and permission-encrypted PDFs (IRCTC) use the platform JCA and are unaffected.

## Baseline profile

- `assets/dexopt/baseline.prof` (10 KB) plus `.profm` is packaged in every release APK, and `baselineProfiles/{0,1}/*.dm` are emitted. AGP rewrites the profile through R8 (`r8_art_profile/release/minifyReleaseWithR8`). `androidx.profileinstaller` 1.4.0 and `ProfileInstallReceiver` survive R8, so sideloaded installs get the profile compiled.
- **Gap:** the profile only merges library profiles (Compose, coroutines, navigation, activity: 3,936 rules). The app has no `baseline-prof.txt`, no `androidx.baselineprofile` plugin, no generator or Macrobenchmark module, so app startup code isn't covered. A reproducible workflow needs these pieces:
  - a `:baselineprofile` `com.android.test` module with `androidx.benchmark:benchmark-macro-junit4` and `androidx.test.uiautomator`
  - the `androidx.baselineprofile` plugin on `:app` and that module
  - a `BaselineProfileGenerator` covering cold start → Home → open trip → import screen
  - `./gradlew :app:generateReleaseBaselineProfile` on a device or emulator, plus a `StartupBenchmark` (compilation `None` vs `Partial`) to prove the gain

  These are new dependencies, so they need a decisions-log entry per AGENTS.md.

## Verification

- `./gradlew assembleDebug testDebugUnitTest lintDebug :app:assembleRelease`: BUILD SUCCESSFUL, 206/206 unit tests, no lint errors.
- `./gradlew :app:bundleRelease`: BUILD SUCCESSFUL.
- Emulator (API 36, x86_64), release APKs test-signed **as copies** with the debug key (project outputs stay unsigned):
  - x86_64 split and universal APK install and cold-start (`am start -W`: 966–1,126 ms)
  - a text-layer ticket PDF imports via PdfBox: saved, "via text"
  - a vector-only ticket PDF (no text layer, like "Microsoft Print to PDF") imports via PdfRenderer + ML Kit OCR + native library: saved, "via OCR", with 4 passengers parsed
  - both auto-pair into a round trip; Trip detail renders
  - no `ClassNotFound`, `NoSuchMethod` or "Could not instantiate" in logcat
  - OCR output is identical to the debug build for the same PDF
- arm64 APK passes `zipalign -c -P 16` (16 KB page alignment). R8 mapping: `app/build/outputs/mapping/release/mapping.txt` (stack traces carry `r8-map-id-…`).

## Commands

```bash
# baseline (before changes)
./gradlew clean :app:assembleRelease
# after changes
./gradlew :app:assembleRelease          # per-ABI + universal unsigned APKs
./gradlew :app:analyzeReleaseR8Config   # -> app/build/reports/r8/r8-config-analyzer-release.{html,pb}
./gradlew :app:bundleRelease            # AAB (APK splits auto-disabled)
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On this Windows machine, Gradle needs a short TEMP (`TEMP=TMP=E:\androidApps\.gtmp`). See PLAN §7.

## Unsigned APK vs. signed APK vs. AAB

- **Unsigned APK** (`*-release-unsigned.apk`): the shrunk, aligned release payload. Android refuses to install it until it is signed (`apksigner sign …`). Signing adds only a few KB and doesn't change the payload size.
- **Signed APK**: the same bytes plus a v2/v3 signature block. It installs and sideloads, and updates must use the same key.
- **AAB**: a publishing format for Google Play, not installable itself. Play generates per-device split APKs from it (ABI, density, language), so users download roughly the arm64 APK size or less. Its file size isn't comparable to an APK's.

## Remaining opportunities (ranked by impact ÷ risk)

1. **Ship only the arm64-v8a APK to the owner's phone** (already built): −30 MB versus universal, zero risk.
2. **App baseline profile** (see above): startup and jank, not size; medium effort.
3. **Remove PdfBox CJK CMaps** (`assets/com/tom_roush/fontbox/resources/cmap/*`, about 1.2 MB): only needed for CID fonts with predefined CJK encodings. IRCTC tickets are Latin with Identity-H, but removing them could break text extraction on unusual PDFs, which would fall back to OCR. Medium risk.
4. **Locale filtering** (`androidResources.localeFilters += "en"`): the app is English-only; this would trim library translations from `resources.arsc` (≤ 0.4 MB), but date-picker and framework strings would stop following the device language. Low gain.
5. **Compressed native libraries** (`packaging.jniLibs.useLegacyPackaging = true`): smaller APK file, but the libraries get extracted on install (larger on-disk footprint, slower install). Not recommended.
6. Dropping x86/x86_64 or armeabi-v7a from the universal APK would remove support for those devices. Not done; the per-ABI APKs already give the size benefit.

## Release signing

`app/build.gradle.kts` signs the `release` build type when credentials are available, so `assembleRelease` and `bundleRelease` produce installable, signed output (`app-<abi|universal>-release.apk`, no `-unsigned` suffix; AAB `app-release.aab`).

Credentials are read in this order (nothing is ever printed or committed):

1. `keystore.properties` at the repo root (git-ignored). Keys: `storeFile` (absolute, or relative to the repo root), `storePassword`, `keyAlias`, `keyPassword`. Template: `keystore.properties.example`.
2. Environment variables `TRIPCHECKER_STORE_FILE`, `TRIPCHECKER_STORE_PASSWORD`, `TRIPCHECKER_KEY_ALIAS`, `TRIPCHECKER_KEY_PASSWORD`.

If any value is missing or the store file doesn't exist, release stays unsigned and Gradle logs one warning. Debug always uses the default debug keystore.

Verify: `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-universal-release.apk` (expect v2/v3 verified and a certificate that is not `CN=Android Debug`). Keep the keystore and its passwords backed up: updates must be signed with the same key.
