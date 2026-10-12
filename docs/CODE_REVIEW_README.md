# Trip Checker: code review of the agent's M0–M6 work

> **For the coding agent (Cursor):** read this file **before** continuing `docs/PLAN.md`.
> Every item below is a task. Work through them **in order** (P0 → P1 → P2 → conventions), test-first where marked.
> Tick `[x]` only when `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` is **actually green on this machine**, and paste the `BUILD SUCCESSFUL` line into the PLAN §9 Progress log.
> Log every judgment call in PLAN §7. Do not weaken, delete or `@Ignore` tests.

Reviewed: commits `1633a09` … `bd19c0f` (all code on `main`) on 2026-10-12.

---

## 0. Verification status (measured during this review, not claimed)

| Gate | Result | Evidence |
|---|---|---|
| `assembleDebug` | ✅ passes | APK built locally with JBR 21 |
| `testDebugUnitTest` | ❌ **2 of 62 tests fail** | `ImportManagerTest`: `Method parse in android.net.Uri not mocked` ([ImportManagerTest.kt:37](../app/src/test/java/com/example/tripchecker/importer/ImportManagerTest.kt:37), [:67](../app/src/test/java/com/example/tripchecker/importer/ImportManagerTest.kt:67)) |
| `lintDebug` | ❌ **7 errors, 34 warnings** | 2 × `NewApi` (real crashes on API 28–33), 5 × `LocalContextGetResourceValueCall` |
| `app/build` before this review | no test results, no APK | the suite was never run green |

### What went wrong in the process

1. **Tasks were ticked "done and green" without a green build.** AGENTS.md step 4 says "Fix until green". M4.5 (ImportManager test), M3.2 and M5.3 (code with `NewApi` lint errors) are ticked, but their tests or lint fail. The §8 Blocker said the network was blocked. In that case the tasks should have stayed unticked.
2. **M6.5's claim "every public class has a one-line KDoc" is false** (see §5).
3. **Tests are named after spec scenarios but don't assert them**, so they hid real bugs:
   - `same pnr updates and keeps id` never checks the PDF. Its fake `delete()` is a no-op, and the fake names the old file `old.pdf` instead of the real `<ticketId>.pdf`. That combination hides P0-2.
   - `needsReview cancelled and autoPairDisabled are not paired` only tests `needsReview`.
   - `ers_multi_passenger expectations` asserts the passenger count but none of the §5 passenger statuses (RLWL/5, RAC S2/71 SIDE LOWER, CAN).
   - The golden-value test doesn't assert `bookedAt`, passenger age, gender or coach/berth, or the `to`/`boarding` names.
   - `FormattersTest` covers only "Today". "Tomorrow" and "in Nd Hh" are untested, so P2-1 was missed.
   - No test covers Home's past-by-month grouping (P1-2) or cancelled tickets (P1-3).
   - M5.8 asked for Trip-detail state-mapping tests. None exist.
   - M3.2 asked to "extract the hashing … into a pure helper and test it". Hashing is still private in `TicketFileStore` and untested.
4. **Spec items were ticked but not implemented** (see §4): `onNewIntent` → navigate to Import (M5.1), and the Home card components, dates, cancelled styling, "No return yet" and per-card previews (M5.3).

---

## P0: crashes and data loss (fix first, test-first)

### - [ ] P0-1 · Every import crashes the app on Android 9–12 (`readNBytes` is API 33)
- **Where:** [TicketFileStore.kt:46](../app/src/main/java/com/example/tripchecker/data/file/TicketFileStore.kt:46). `temp.inputStream().use { it.readNBytes(5) }`.
- **Why:** `InputStream.readNBytes(int)` only exists from API 33. minSdk is 28. On API 28–32 it throws `NoSuchMethodError`. That is an `Error`, so `catch (e: Exception)` in [ImportManager.kt:45](../app/src/main/java/com/example/tripchecker/importer/ImportManager.kt:45) misses it. It escapes `appScope`, which has no `CoroutineExceptionHandler`, and the process dies.
- **Fix:**
  - Read the header with a plain `read(buf, off, len)` loop.
  - Better: check the magic bytes on the **source stream before copying** (see P1-9).
  - Add a `CoroutineExceptionHandler` to `appScope` in [AppContainer.kt:60](../app/src/main/java/com/example/tripchecker/di/AppContainer.kt:60) as a last line of defense. It should log the error and mark the item failed, not crash.
- **Also:** [HomeUiMapper.kt:50](../app/src/main/java/com/example/tripchecker/ui/home/HomeUiMapper.kt:50) uses `LocalDate.EPOCH`, which is API 34 (the second `NewApi` error). Replace it with `LocalDate.of(1970, 1, 1)`, or better, drop groups without a departure.
- **Guard:** keep `NewApi` as a lint error. Never use Java 9+ APIs (`readNBytes`, `readAllBytes`, `transferTo`, `Duration.toHoursPart`, `LocalDate.EPOCH`, `List.of`, …) without checking lint.

### - [ ] P0-2 · Re-importing the same PNR deletes the new PDF ("Go to ticket" breaks)
- **Where:** [TicketImporter.kt:57-58](../app/src/main/java/com/example/tripchecker/importer/TicketImporter.kt:57).
- **Why:** `moveToTicketId(staged, existing.id)` renames the new file to `<existingId>.pdf`. On Android, `rename(2)` overwrites the old file. Then `fileStore.delete(existingByPnr.pdfFileName)` deletes `<existingId>.pdf`, which is the **new** file. Every "Updated" ticket ends up pointing at a missing PDF. This is exactly the re-download-after-chart-preparation flow from §3.5 step 4.
- **Fix:**
  - Give every stored PDF a unique name, e.g. `<ticketId>-<sha256.take(12)>.pdf`.
  - Order the steps as: move to the new name → upsert the DB → **then** delete the old file, only if `old != new`.
  - On any failure after the move, delete the moved file (see P1-10).
- **Test:** make the fake file store use a real temp dir and the **real** naming (`<id>.pdf` for the existing ticket). Assert that after `Updated` the new PDF exists, `pdfFileName` resolves to it, and the old file is gone.

### - [ ] P0-3 · `@Upsert` + UNIQUE(`pnr`/`pdfSha256`) silently drops writes or crashes with a FOREIGN KEY error
- **Where:** [TicketDao.kt:32-46](../app/src/main/java/com/example/tripchecker/data/db/dao/TicketDao.kt:32), [OfflineTicketRepository.kt:37-42](../app/src/main/java/com/example/tripchecker/data/repo/OfflineTicketRepository.kt:37).
- **Why (checked in the Room 2.8.0 sources, `EntityUpsertAdapter.checkUniquenessException`):** when the INSERT fails with *any* UNIQUE violation (code 2067, not just the primary key), Room falls back to `UPDATE … WHERE id = <new id>`. That updates **0 rows**. Then `insertPassengers` fails with `FOREIGN KEY constraint failed`.
  - **Add manually** with a PNR that already exists and **no passengers** → nothing is saved, but `onSaved()` pops back as if it succeeded.
  - With passengers → a raw SQLite error.
  - Two concurrent imports (P1-1) of the same PDF or PNR → one is lost, and its PDF is orphaned.
- **Fix:**
  - Replace `@Upsert` with an explicit `@Transaction`: `val n = updateTicket(entity); if (n == 0) insertTicket(entity)`. Use `@Insert(onConflict = ABORT)`.
  - Insert passengers with `@Insert`, not `@Upsert`.
  - In the repository, check before writing: `dao.getByPnr(pnr)?.takeIf { it.id != ticket.id }` → throw a domain `DuplicatePnrException(existingId)`.
  - `EditTicketViewModel` shows "This PNR already exists" with an **Open existing** action. Don't show the raw `e.message`.
- **Test:** M3.4 in-memory Room tests (androidTest): a new id with an existing PNR must throw, not no-op. Also add a cascade delete test.
- **Future edge (decide now, while schema v1 is still editable):** Indian Railways **recycles PNRs**. A new booking years later with an old PNR would hit the "Updated" path and overwrite a past trip. Treat it as the same booking only if `trainNumber` + `startDate` also match. Otherwise insert it as new, which means relaxing the unique index to `(pnr, startDate)`. Log the decision.

---

## P1: wrong behaviour users will hit

### - [ ] P1-1 · Import batches are not sequential across `enqueue()` calls
- **Where:** [ImportManager.kt:35](../app/src/main/java/com/example/tripchecker/importer/ImportManager.kt:35). Every `enqueue` starts its own `scope.launch` on the multi-threaded `Dispatchers.Default`.
- **Failure:** the user shares a PDF while a picker batch is running, or shares twice. Two `TicketImporter.process` calls then run in parallel:
  - SHA/PNR dedupe races (TOCTOU) → P0-3 data loss.
  - Two PdfBox documents are in memory at once.
  - The 20-file case from M6.1 is not actually serialized.
- **Fix:** one worker. Use `Channel<StagedItem>(UNLIMITED)` consumed by a single `scope.launch { for (x in ch) … }` (or a `Mutex` around phase B). Phase A (staging) can stay immediate.
- **Test:** a fake importer that records max concurrency. Two `enqueue` calls → assert `maxConcurrent == 1`.

### - [ ] P1-2 · OCR can allocate an unbounded bitmap; OOM crashes the app; the bitmap leaks on failure
- **Where:** [OcrTextExtractor.kt:58-86](../app/src/main/java/com/example/tripchecker/extraction/OcrTextExtractor.kt:58), [:52](../app/src/main/java/com/example/tripchecker/extraction/OcrTextExtractor.kt:52).
- **Why:**
  - `scale = 2000f / page.width` uses no height cap. A tall or narrow page (receipt-style PDF, a 1-pt-wide page) gives `2000 × 60 000 × 4 B` and an `OutOfMemoryError`. That is an `Error`, not caught → crash.
  - An A4 page is already about 22 MB of ARGB_8888.
  - `bitmap.recycle()` (line 84) is skipped if `render` or `process().await()` throws or is cancelled.
  - `catch (_: Exception)` also swallows `CancellationException`.
- **Fix:**
  - Cap the pixel count: `scale = min(2000f / w, sqrt(MAX_PIXELS / (w * h)))` with `MAX_PIXELS ≈ 6_000_000`.
  - Wrap the bitmap use in `try { … } finally { bitmap.recycle() }`.
  - Catch `OutOfMemoryError` → return `null`.
  - Rethrow `CancellationException`.
- **PdfBox** ([PdfBoxTextExtractor.kt:17,29](../app/src/main/java/com/example/tripchecker/extraction/PdfBoxTextExtractor.kt:17)):
  - `PDDocument.load(file)` reads the whole file into heap. Use `MemoryUsageSetting.setupTempFileOnly()`.
  - Malformed PDFs can throw `StackOverflowError` or `OutOfMemoryError`. Catch those too and return `null`.
- **ML Kit client:** it is created eagerly at app start ([AppContainer.kt:48-49](../app/src/main/java/com/example/tripchecker/di/AppContainer.kt:48)) and never closed. Make it `lazy`. Document that it is intentionally app-scoped.

### - [ ] P1-3 · `ImportManagerTest` is red (the suite is not green)
- **Where:** [ImportManagerTest.kt:37](../app/src/test/java/com/example/tripchecker/importer/ImportManagerTest.kt:37), [:67](../app/src/test/java/com/example/tripchecker/importer/ImportManagerTest.kt:67). `Uri.parse` is a stub on the JVM.
- **Fix (root cause):** decouple `ImportManager` from `android.net.Uri`. Options:
  - Make it generic over the source type (`ImportManager<S>(stager: Stager<S>, displayName: (S) -> String)`).
  - Or pass `ImportRequest(id: String, uriString: String)` and resolve the `Uri` only inside `TicketFileStore`.
- Do **not** add Robolectric (it is not in the allowed library list). `unitTests.isReturnDefaultValues = true` only turns this into an NPE.
- Move `StagedFile` out of the Android class `TicketFileStore` into its own pure file.

### - [ ] P1-4 · The 14-day pairing window actually allows up to 14 d 23 h 59 m
- **Where:** [TripGrouper.kt:43](../app/src/main/java/com/example/tripchecker/domain/grouping/TripGrouper.kt:43). `Duration.between(o, r).toDays() <= 14` truncates.
- **Example:** out 1 Aug 10:00, return 15 Aug 20:00 = 14 d 10 h → paired. The spec says ≤ 14 days.
- **Fix:** `Duration.between(o, r) <= Duration.ofDays(PAIRING_WINDOW_DAYS)`.
- **Test:** exactly 14 d → paired; 14 d + 1 min → two ONE_WAY.

### - [ ] P1-5 · Home "Past" is grouped by **day**, not month (duplicate "August 2026" headers)
- **Where:** [HomeUiMapper.kt:48-51](../app/src/main/java/com/example/tripchecker/ui/home/HomeUiMapper.kt:48). `groupBy { …toLocalDate() }`.
- **Fix:** `groupBy { YearMonth.from(it) }`. The header comes from the `YearMonth`.
- **Test:** two past trips on 3 Aug and 10 Aug → exactly one "August 2026" section with 2 groups.

### - [ ] P1-6 · Cancelled tickets appear as the "Next journey" hero
- **Where:** [HomeUiMapper.kt:25-29](../app/src/main/java/com/example/tripchecker/ui/home/HomeUiMapper.kt:25). There is no `isCancelled` filter.
- **Fix:** `.filter { !it.isCancelled && … }`. Consider also skipping legs where every passenger's `current.code == "CAN"`.
- **Test:** a cancelled leg departing before a non-cancelled leg → the hero is the non-cancelled one.

### - [ ] P1-7 · Journey status never refreshes while a screen is open; Home queries Room twice
- **Where:** [ObserveJourneysUseCase.kt:15-18](../app/src/main/java/com/example/tripchecker/domain/ObserveJourneysUseCase.kt:15), [HomeViewModel.kt:55-62](../app/src/main/java/com/example/tripchecker/ui/home/HomeViewModel.kt:55), [TripDetailViewModel.kt:39-43](../app/src/main/java/com/example/tripchecker/ui/trip/TripDetailViewModel.kt:39).
- **Why:**
  - Groups (and so UPCOMING / IN_PROGRESS / PAST) are computed with `clock.instant()` **at DB emission**. The 60 s ticker only re-runs `HomeUiMapper` with the old groups. A train that departs while Home is open stays "Upcoming"; the trip-detail chip is likewise stale.
  - `combine(observeJourneys.observe(), repository.observeTickets(), …)` subscribes to the same table twice. That doubles the work and can produce inconsistent snapshots (groups from list N, tickets from list N+1).
- **Fix:**
  - Use a single source: `combine(repository.observeTickets(), ticker) { t, now -> HomeUiMapper.map(TripGrouper.group(t, now), t, now) }.flowOn(Dispatchers.Default)`.
  - Let `ObserveJourneysUseCase` take the `now` flow.
  - Do the same in TripDetail.
- **Test:** a VM with a fake repo and `StandardTestDispatcher`. Advance past departure → the group moves to IN_PROGRESS without any DB change.

### - [ ] P1-8 · Fare loses 1 paisa via `Double` (17,774 of 290,000 values from ₹100 to ₹2,999.99)
- **Where:** [EditTicketForm.kt:129-130](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketForm.kt:129) (`(toDouble() * 100).toLong()`) and [:78](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketForm.kt:78) (`(it / 100.0).toString()`).
- **Failure:** `"128.14"` → 12813 paise. Because the form re-encodes the fare on every save, **any** edit can silently change an imported fare. `"1,470.00"` (with a comma) → `null`, so the fare is dropped.
- **Fix:**
  - Parse with `trim().replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.setScale(0, HALF_UP)?.longValueExact()`.
  - Display with `BigDecimal.valueOf(paise, 2).toPlainString()`.
- **Test:** round-trip `"128.14"`; `147000` ↔ `"1470.00"`; a comma input.

### - [ ] P1-9 · Staging copies the whole stream before checking size or type; SHA-256 is computed twice
- **Where:** [TicketFileStore.kt:35-57](../app/src/main/java/com/example/tripchecker/data/file/TicketFileStore.kt:35), [:87-105](../app/src/main/java/com/example/tripchecker/data/file/TicketFileStore.kt:87).
- **Why:**
  - A 2 GB (or endless) provider stream is fully written to `cacheDir` before the 20 MB check, which can fill storage.
  - `copyWithDigest` computes a digest and **throws it away**. Then `sha256File` reads the file again.
  - On an exception mid-copy, the temp file is left until the next app start.
- **Fix:**
  - Check `%PDF-` on the first 5 bytes of the source stream.
  - Count bytes while copying and abort past `MAX_BYTES`.
  - Return the digest from the copy.
  - Use `try/finally` to delete the temp file on failure.
  - Extract the hashing into the pure `PdfValidation` helper and unit-test it (the original M3.2 requirement).
- **Also:** `staged.renameTo(dest)` at [TicketFileStore.kt:70-72](../app/src/main/java/com/example/tripchecker/data/file/TicketFileStore.kt:70) ignores the `false` return. The DB then stores a name with no file behind it, and the staged copy is wiped at the next start. Check the return value; fall back to copy + delete; throw on failure.

### - [ ] P1-10 · A failure after the PDF is moved leaves an orphan PDF in `files/tickets/`
- **Where:** [ImportManager.kt:72-73](../app/src/main/java/com/example/tripchecker/importer/ImportManager.kt:72) deletes `file.file` (the staging path). After `moveToTicketId` that path no longer exists, so a failed `repository.upsert` leaves `tickets/<id>.pdf` unreferenced forever. `StagingCleanup` only removes `staging-*`.
- **Fix:**
  - `TicketImporter` owns the cleanup: on failure after the move, delete the moved file.
  - Add a startup reconcile, off the main thread: delete files in `tickets/` that no DB row references.

### - [ ] P1-11 · Re-import overwrites the owner's manual corrections with `null`
- **Where:** [ParsedTicketMapper.kt:71-91](../app/src/main/java/com/example/tripchecker/importer/ParsedTicketMapper.kt:71).
- **Failure:** the owner fixed the station or class by hand. A new OCR download misses that field → it is overwritten with `null` and `needsReview` may flip on.
- **Fix:**
  - Merge field-wise: `parsed.x ?: existing.x`.
  - Keep the existing passengers if the parse found none.
  - Recompute `needsReview` on the **merged** ticket. That also removes the duplicated computation at line 81.

### - [ ] P1-12 · Deleting one leg of a manual pair leaves a one-leg "Custom" trip forever
- **Where:** [OfflineTicketRepository.kt:44-48](../app/src/main/java/com/example/tripchecker/data/repo/OfflineTicketRepository.kt:44).
- **Why:** the surviving leg keeps its `linkGroupId`, so [TripGrouper.kt:75-79](../app/src/main/java/com/example/tripchecker/domain/grouping/TripGrouper.kt:75) makes it a 1-leg CUSTOM group that never auto-pairs again.
- **Fix:** in one transaction, after the delete, clear `linkGroupId` on the remaining members if fewer than 2 are left.

### - [ ] P1-13 · Parser: unreadable departure time silently becomes 00:00 and is **not** flagged for review
- **Where:** [IrctcErsParser.kt:153-156](../app/src/main/java/com/example/tripchecker/extraction/parser/IrctcErsParser.kt:153). The warning is generated but `warnings` is never used anywhere.
- **Fix:**
  - Add a `DEPARTURE_TIME` field to `missingFields` and treat it as required for auto-save, or set `needsReview` when this warning fires.
  - Persist the warnings, or at least surface them in the Needs review row.
- **Test:** the fixture without a `Departure*` value → `needsReview == true`.

### - [ ] P1-14 · Parser: the first passenger is lost if OCR puts the `# Name Age …` header on the same row or drops it
- **Where:** [IrctcErsParser.kt:331](../app/src/main/java/com/example/tripchecker/extraction/parser/IrctcErsParser.kt:331). It skips by index (`headerIdx + 2`).
- **Fix:** start at `headerIdx + 1` and skip rows that match `^\s*#|Name\s+Age`.
- **Test:** a fixture where the header row is missing → the passenger is still parsed.

### - [ ] P1-15 · Edit: picking only a departure date silently sets 09:00 and bypasses validation
- **Where:** [EditTicketScreen.kt:245-250](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketScreen.kt:245).
- **Fix:** don't default the time. Leave it `null` so the required-field check flags it, or open the time picker right after the date. Move the decision into the VM so it can be unit-tested.

### - [ ] P1-16 · Sharing into the already-open app doesn't navigate to Import
- **Where:** [MainActivity.kt:35-39](../app/src/main/java/com/example/tripchecker/MainActivity.kt:35). `onNewIntent` enqueues but never navigates (spec §3.5 / M5.1).
- **Fix:**
  - Expose a one-shot event, e.g. `MutableSharedFlow<Unit>` collected in `AppNavHost`, and call `navigate(Import) { launchSingleTop = true }`.
  - Use `launchSingleTop` for every `navigate(Import)` ([AppNavHost.kt:41](../app/src/main/java/com/example/tripchecker/ui/navigation/AppNavHost.kt:41)) so a double tap doesn't push two Import screens.
- **Also guard:** when the task is relaunched from Recents, Android re-delivers the original SEND intent with `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` and `savedInstanceState == null` ([MainActivity.kt:20](../app/src/main/java/com/example/tripchecker/MainActivity.kt:20)). That re-imports and shows "Already imported" or "Permission denial". Skip intents that carry this flag.

### - [ ] P1-17 · Home "Go to ticket" fails silently
- **Where:** [HomeScreen.kt:61-63](../app/src/main/java/com/example/tripchecker/ui/home/HomeScreen.kt:61). The `OpenPdfResult` is ignored, so with no PDF app or a missing file nothing happens.
- **Fix:** add a `SnackbarHost` to Home and reuse the TripDetail handling ("No PDF app found" + **Share**, "PDF file missing"). Extract it into one shared helper.

---

## P2: correctness and UX polish

- [ ] **P2-1 Countdown is off by up to a day.** [Formatters.kt:40-44](../app/src/main/java/com/example/tripchecker/ui/common/Formatters.kt:40) mixes calendar-day difference with elapsed-hours `% 24`.
  - Mon 23:00 → Wed 22:00 shows **"in 2d 23h"**; the real gap is 1 d 23 h.
  - Fix: use `Duration.between(now, dep)` for both parts: `toDays()` and `toHours() % 24`. (`toHoursPart()` is API 31, so don't use it.)
  - Test Tomorrow, the day after, and a 23:00 edge.
- [ ] **P2-2 Money format drops the paise.** [Formatters.kt:20,51-54](../app/src/main/java/com/example/tripchecker/ui/common/Formatters.kt:20).
  - `147000` → `₹1,470` (not `₹1,470.00`); `2360` → `₹23.6`.
  - Fix: set `minimumFractionDigits = 2` and `maximumFractionDigits = 2`, and work from `BigDecimal`.
  - The shared `NumberFormat` is not thread-safe. Create it per call, or use a `ThreadLocal`.
- [ ] **P2-3 Home cards show no date.** [HomeScreen.kt:211-212](../app/src/main/java/com/example/tripchecker/ui/home/HomeScreen.kt:211) and [LinkTicketSheet.kt:39-43](../app/src/main/java/com/example/tripchecker/ui/trip/components/LinkTicketSheet.kt:39) show only `HH:mm`.
  - Fortnightly trips on the same train look identical. The spec says "route, date and time".
  - A missing station prints the literal `null → null`.
- [ ] **P2-4 M5.3 spec gaps on Home.**
  - Missing: `RoundTripCard` / `OneWayCard` / `NeedsReviewRow` components, cancelled strike-through + chip, the "In progress" chip on cards, the "No return yet" hint, the extended FAB "Add ticket", and a `@Preview` for every card.
  - Also add a `TripDetail` state-mapping test (M5.8).
- [ ] **P2-5 The OCR hint only checks the first leg** ([TripDetailScreen.kt:188-192](../app/src/main/java/com/example/tripchecker/ui/trip/TripDetailScreen.kt:188)). Move it into `LegSection` so it shows per leg.
- [ ] **P2-6 After deleting the anchor leg, "Ticket not found" flashes** before `NavigateBack`. Emit `NavigateBack` first, or treat `group == null` after a delete as navigate-back.
- [ ] **P2-7 Import list problems.**
  - Staged items show "Reading…" while they are only queued ([ImportManager.kt:38](../app/src/main/java/com/example/tripchecker/importer/ImportManager.kt:38)). Keep them `Queued` until phase B picks them up.
  - The list is cleared only by **Done**. System Back leaves items behind forever, the summary keeps accumulating, and the unbounded list holds `Uri` + `StagedFile` objects. Clear finished items on Back too.
  - Give the `LazyColumn` items a key (`key = { it.id }`) in [ImportScreen.kt:84](../app/src/main/java/com/example/tripchecker/ui/importflow/ImportScreen.kt:84).
- [ ] **P2-8 Edit form issues.**
  - `DatePicker` opens on today instead of the current value ([EditTicketScreen.kt:233](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketScreen.kt:233)). Pass `initialSelectedDateMillis` as UTC midnight.
  - Editing the class or quota code leaves `classLabel` / `quotaLabel` stale. Recompute them from the label tables.
  - Dates display as ISO `2026-08-03`. Use `Formatters`.
  - Booking status is not editable.
- [ ] **P2-9 Edit when the ticket is deleted elsewhere.** `existing` becomes `null`, and Save **re-creates** it as a new MANUAL ticket ([EditTicketViewModel.kt:101](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketViewModel.kt:101)). Show "Ticket was deleted" and disable Save.
- [ ] **P2-10 `ExtractionMethod.valueOf` crashes the whole list on an unknown value** ([Mappers.kt:38](../app/src/main/java/com/example/tripchecker/data/db/Mappers.kt:38)). This will matter with future enum changes or Supabase sync. Use `runCatching { valueOf(it) }.getOrDefault(OCR)`.
- [ ] **P2-11 Lint errors `LocalContextGetResourceValueCall`** ([TripDetailScreen.kt:77-100](../app/src/main/java/com/example/tripchecker/ui/trip/TripDetailScreen.kt:77)). Resolve the strings with `stringResource` in composition and pass them into the lambdas, or use `LocalResources.current`.
- [ ] **P2-12 `@string/in_progress` overrides a private Compose resource** (lint `PrivateResource`, [strings.xml:13](../app/src/main/res/values/strings.xml)). Rename it to `journey_in_progress`.
- [ ] **P2-13 Startup work on the main thread.** `clearOrphanStaging()` does file I/O in `Application.onCreate` ([TripCheckerApp.kt:17](../app/src/main/java/com/example/tripchecker/TripCheckerApp.kt:17)), and the ML Kit client is created eagerly. Launch the cleanup on `appScope` with `Dispatchers.IO` and make the extractors `lazy`.

---

## Security and input guarding

- [ ] **Reject non-`content://` URIs** in `extractPdfUris` ([MainActivity.kt:51-65](../app/src/main/java/com/example/tripchecker/MainActivity.kt:51)). Also reject URIs whose authority is our own `${applicationId}.fileprovider`.
  - Why: the activity is exported. Another app can send `EXTRA_STREAM = file:///data/data/com.example.tripchecker/...`, and `openInputStream` would read our private files with our permissions.
- [ ] **Cap the batch size** (e.g. 50 URIs per intent) so a malicious `SEND_MULTIPLE` can't queue thousands of items.
- [ ] **Cap bytes while copying** (P1-9) and enforce a minimum PDF size.
- [ ] **Password-protected PDFs:** `PdfRenderer` throws `SecurityException` and PdfBox throws `InvalidPasswordException`. Both currently become a silent NeedsReview. Surface the reason ("Password-protected PDF") on the Import row.
- [ ] **Never show raw exception messages** (`e.message`) in the UI, e.g. `Permission Denial: …` or SQLite text. Map them to string resources.
- [ ] **Backup quota:** Android Auto Backup has a **25 MB per-app limit**. If it is exceeded, backups silently stop. With PDFs of up to 20 MB each in `files/tickets/`, this will happen.
  - Decide and log: exclude PDFs from `cloud-backup` (keep them for `device-transfer`), or document the limit and rely on M7.4 export.
- [ ] **R8:** `-keep class com.tom_roush.pdfbox.** { *; }` keeps the whole library, and `org.apache.pdfbox.**` doesn't exist in pdfbox-android. Narrow the rules when M6.3 is done.

---

## Memory and resource leaks (summary)

| # | Leak or risk | Where | Fix |
|---|---|---|---|
| 1 | ~22 MB bitmap not recycled when render or OCR throws or is cancelled | OcrTextExtractor.kt:63-84 | `try/finally { recycle() }` (P1-2) |
| 2 | Unbounded bitmap → OOM crash | OcrTextExtractor.kt:60-63 | pixel cap (P1-2) |
| 3 | PdfBox loads the whole document into heap | PdfBoxTextExtractor.kt:17 | `MemoryUsageSetting.setupTempFileOnly()` |
| 4 | Parallel batches → two PdfBox docs + OCR queued at once | ImportManager.kt:35 | single worker (P1-1) |
| 5 | `save(onSaved)` captures `NavController` (→ Activity) inside a `viewModelScope` coroutine. After rotation mid-save the old controller pops, so the screen doesn't close and the old Activity is held | EditTicketViewModel.kt:80-103 | `Channel<EditEvent>` exposed as a Flow (also an AGENTS.md rule) |
| 6 | `ImportManager._items` grows forever unless Done is pressed | ImportManager.kt:20,34 | clear on Back; drop `stagedFile` after processing |
| 7 | Orphan PDFs in `files/tickets/` after failures | ImportManager.kt:72-73 | P1-10 reconcile |
| 8 | Orphan staging files after a copy exception | TicketFileStore.kt:39-41 | `try/finally` delete |
| 9 | ML Kit `TextRecognizer` never closed | OcrTextExtractor.kt:23 | `lazy`, app-scoped by design (document it) |
| 10 | `findByPnr` / `findBySha` / `delete` open a Room `Flow` observer just to read once | OfflineTicketRepository.kt:25-33,45 | add `suspend getWithPassengersById(id)` |

---

## 5. AGENTS.md convention violations

| Rule (AGENTS.md) | Violation |
|---|---|
| "One-off events (snackbar, navigation) go through a `Channel` exposed as a `Flow`." | `EditTicketViewModel.save(onSaved: () -> Unit)` ([EditTicketViewModel.kt:80](../app/src/main/java/com/example/tripchecker/ui/edit/EditTicketViewModel.kt:80)) |
| "Put user-visible strings in `res/values/strings.xml`." | `"Ticket deleted"`, `"Unlinked"`, `"Automatic pairing restored"`, `"Tickets linked"` ([TripDetailViewModel.kt:68-94](../app/src/main/java/com/example/tripchecker/ui/trip/TripDetailViewModel.kt:68)); the summary in `ImportViewModel`; `"Not a PDF"`, `"Empty file"` and `"File too large"` shown in the UI ([TicketFileStore.kt:41-53](../app/src/main/java/com/example/tripchecker/data/file/TicketFileStore.kt:41)). `lint.xml` hides this by ignoring `HardcodedText` globally. Use typed failure reasons mapped to string resources. |
| "Wrap file I/O in `withContext(Dispatchers.IO)`" | `clearOrphanStaging()` on the main thread ([TripCheckerApp.kt:17](../app/src/main/java/com/example/tripchecker/TripCheckerApp.kt:17)); `file.exists()` in `TicketActions` |
| "Add a one-line KDoc on every public class" (M6.5 claims this is done) | Missing on `AppDatabase`, `TicketDao`, `TicketEntity`, `PassengerEntity`, `TicketWithPassengers`, `ImportItem`, `ImportItemState`, `NextJourneyUi`, `PastMonthGroup`, `HomeViewModel`, `ImportViewModel`, `ImportUiState`, `TripDetailUiState`, `TripDetailEvent`, `EditTicketUiState`, `PassengerFormRow`, `JourneyKind`, `JourneyStatus`, `OpenPdfResult`, the routes, the `TicketImportResult` subclasses. **Duplicate** KDoc blocks in [CompositeTextExtractor.kt:6-7](../app/src/main/java/com/example/tripchecker/extraction/CompositeTextExtractor.kt:6) and [PdfBoxTextExtractor.kt:11-12](../app/src/main/java/com/example/tripchecker/extraction/PdfBoxTextExtractor.kt:11). |
| "Keep files focused, under about 400 lines" | `IrctcErsParser.kt` is 443 lines. Split into station, passenger and payment sub-parsers. |
| "Each screen has … `XContent` … Add `@Preview`s" | There are no previews for `ImportContent` or `EditTicketContent`. |
| "Repository interface … ViewModels never touch DAOs"; testability | `OfflineTicketRepository` depends on the concrete `TicketFileStore`, not `TicketFileOperations`, so it can't be JVM-tested. |
| Decisions log | Undocumented deviations: `DatePickerDialog` used as the time-picker container, the 09:00 default time, the global `HardcodedText` ignore, `StagedFile` nested in an Android class. |

Small cleanups:
- In [Fixtures.kt](../app/src/test/java/com/example/tripchecker/Fixtures.kt) the `?: error(...)` is dead code after `!!`.
- `StagingCleanupTest` uses the deprecated `createTempDir()`.
- The fake repo in `ImportManagerTest` names the parameter `sha256`; the interface uses `sha`.
- Unused template colours are left in `colors.xml`.
- The manifest has a redundant `android:label` on the activity.

---

## 6. Future edge cases the parser and grouper must handle (add a fixture or test for each)

**Parser / OCR**
- [ ] OCR digit noise in dates (`O3-Aug-2026`, `03-Auq-2026`, `2O26`). Run `OcrNormalizer.digits` on the numeric parts of date captures and fuzzy-match the month. Today such tickets fall to NeedsReview.
- [ ] Lowercase or mixed-case station names from OCR. `STATION_RX` uses `[A-Z]` without `IGNORE_CASE` ([IrctcErsParser.kt:76-79](../app/src/main/java/com/example/tripchecker/extraction/parser/IrctcErsParser.kt:76)).
- [ ] Multi-word berth types split by a double space (`SIDE  UPPER`). `MULTI_WORD_BERTH_TYPES` only matches a single space, case-sensitively, so the current status becomes `UPPER`.
- [ ] A passenger row wrapped onto two OCR rows (long names), 6-passenger tickets, child rows with no berth (`CNF` with no coach), and senior-citizen rows.
- [ ] Train names that really end in `AC`, `CAR`, `NON` or `3` (the strip-suffix regex removes them). A test documenting the "AC" case is required by the spec and is missing.
- [ ] The train number and the PNR digits on different OCR rows (skewed scan). Search `detailRow ± 1` for `\d{5}\s*/`.
- [ ] Skewed or rotated scans. `RowMerger`'s 0.5 × median-height threshold mis-groups columns. Consider deskewing using the ML Kit line angle.
- [ ] Landscape pages, very small or very large page sizes (see the P1-2 cap), and multi-ticket PDFs (only the first PNR is used, so log a warning).
- [ ] `PdfBoxTextExtractor.isUsable` accepts any 10-digit number. Page 2's `+91-8044647999` passes, so a garbled text layer skips OCR. Require the 10 digits within 2 rows of the `PNR` label (reuse the parser's anchoring).
- [ ] When the PNR row is missing, `quotaSearchRange` starts at row 0 and includes the station row, so a station code like `(SS)` or `(PT)` can be read as the quota.
- [ ] `Total Fare` takes the **first** amount on the line. Add a fixture where OCR merges two amounts on one row.
- [ ] Cancelled-ticket ERS (all passengers `CAN`). Consider auto-setting `isCancelled` and log the decision.
- [ ] Password-protected or encrypted PDFs → a clear Failed / NeedsReview reason.

**Grouping / data**
- [ ] A manual group containing a leg with `departureAt == null` is always PAST ([TripGrouper.kt:100](../app/src/main/java/com/example/tripchecker/domain/grouping/TripGrouper.kt:100)). Compute the status from non-null legs only.
- [ ] `status` uses `legs.last().arrivalAt`, but the last leg by departure may not arrive last. Use the max over legs.
- [ ] Cancelled tickets inside manual groups, and `LinkReturnCandidates` offering cancelled tickets.
- [ ] Same-city "trips" (e.g. SC → BMT) pair with each other because both cities are HYDERABAD.
- [ ] PNR recycling (see P0-3).
- [ ] Device time zone ≠ IST. Make sure `DatePicker` → `LocalDate` stays UTC and that every display goes through `Formatters` (some still use `LocalDate.toString()`).
- [ ] **Migrations:** once M6 is ticked, every schema change needs a version bump plus a Migration/AutoMigration and a `MigrationTestHelper` test. Do the P0-3 index change **before** closing M6.

---

## 7. Tests to add (minimum)

| Area | Test |
|---|---|
| Importer | Update by PNR keeps the **new** PDF on disk with the real naming; deletes the old one only when the name differs (P0-2) |
| Importer | A failure after the move deletes the moved file (P1-10) |
| Importer | Merge keeps existing values when the parse returns `null` (P1-11) |
| ImportManager | Two `enqueue` calls → max concurrency 1 (P1-1); queued items stay `Queued` (P2-7) |
| Room (androidTest, M3.4) | Duplicate PNR with a new id throws; cascade delete; unique SHA |
| Grouper | Window boundary: 14 d (pair) vs 14 d + 1 min (no pair) (P1-4); cancelled and `autoPairDisabled` separately (scenario 8) |
| Home mapper | Past by **month**; cancelled not chosen as hero; status refresh via ticker (P1-5/6/7) |
| Formatters | Tomorrow; "in Nd Hh" across midnight; `₹1,470.00`; `₹23.60` (P2-1/2) |
| Edit mapper | Fare round-trip `"128.14"`; comma input; date-only → departure missing (P1-8/15) |
| Parser | All §5 passenger statuses; `bookedAt`; missing header row (P1-14); departure-time missing → needs review (P1-13); "AC"-ending train name |
| File helper | Pure SHA-256 helper on byte arrays; size cap during the copy (P1-9) |
| TripDetail | State mapping: anchor → group, link candidates (M5.8) |

---

## 8. Suggested order of work

1. P1-3 (make the suite green) → P0-1 (`readNBytes` / `NewApi`) → `lintDebug` errors (P2-11, P2-12).
2. P0-2, P0-3 (+ M3.4 Room tests), P1-1, P1-10: the import pipeline's integrity.
3. P1-2, P1-9: memory and input guards.
4. P1-4 … P1-8: grouping and display correctness.
5. P1-11 … P1-17, then P2, security, conventions.
6. Re-run `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`. Only then tick M6.2, and continue to M6.3 / M7.
