# Trip Checker — Implementation Plan

> **Agents:** read `AGENTS.md` first (rules, stack, work loop). This file is the **task list and spec**.
> Work top to bottom. Tick `[x]` when a task is done and green. Append to the **Progress log** at the bottom.
> Record every judgment call in the **Decisions log**. Do not ask the user questions. Decide, log it, and keep going.

---

## 1. Product summary

The owner travels by Indian Railways (IRCTC) about every 2 weeks, on different trains at different times. They download every e-ticket PDF (ERS, "Electronic Reservation Slip"), but the files are unorganized and the IRCTC app is slow (load, log in, API calls).

**Trip Checker** is a fully **offline** Android app that:

1. **Imports ticket PDFs**: bulk picker, "Share to" and "Open with" from any app.
2. **Extracts only what matters**:
   - PNR, train number and name
   - from, boarding and to stations
   - departure and arrival times
   - class and quota
   - passengers with coach, berth and berth type
   - fare
3. **Auto-groups an outbound journey and its return into one round-trip card.** Pairs must be within 14 days, and city-level station matching is used (e.g. BMT and SC are both Hyderabad).
4. Shows a **summary** when a card is tapped.
5. Has a **"Go to ticket"** button that opens the original PDF in the phone's external PDF app.

**Owner decisions (final; do not revisit):**

| Topic | Decision |
|---|---|
| Whose tickets | Only the owner's. Pairing uses **stations + dates only**, with no passenger-name matching. |
| Go to ticket | Opens in an **external PDF app** (FileProvider + `ACTION_VIEW`). **No in-app PDF viewer.** |
| Pair window | **14 days**, from outbound departure to return departure (`PAIRING_WINDOW_DAYS = 14`). |
| Package | Keep `com.example.tripchecker`. **Do not rename.** |
| Network | Fully offline. **No `INTERNET` permission.** Supabase sync is a future phase; only keep the design sync-friendly. |
| Visual design | Follow the mockups in `design/` (Material 3, indigo brand scheme, light + dark). Spec in §3.9 and §3.10; work in milestone **MD**. |

**Critical fact about real tickets:** the owner's sample ticket was saved via *Microsoft Print to PDF*. It has **no text layer**: no fonts, and every glyph is drawn as a vector path. So **OCR is mandatory**. Original IRCTC downloads usually do have a text layer. The extractor therefore tries the text layer first and falls back to OCR.

---

## 2. Sample ticket: ground truth

This is the owner's real ticket (page 1 of 2; page 2 is only instructions and customer-care numbers). The layout is the current IRCTC ERS:

```
                 Electronic Reservation Slip (ERS)-Normal User
 [IR logo]                                                        [IRCTC logo]
   Booked From            [Boarding At]  (blue arrow IMAGE)            To
 SAINAGAR SHIRDI (SNSI)      SAINAGAR SHIRDI (SNSI)            BEGAMPET (BMT)
 Start Date* 03-Aug-2026   Departure* 17:00 03-Aug-2026   Arrival* 07:59 04-Aug-2026
 ---------------------------------------------------------------------------------
       PNR                     Train No./Name                      Class
   8751652729             17001 / SNSI SC EXPRESS              THIRD AC (3A)
      Quota                      Distance                       Booking Date
   TATKAL (TQ)                    772 KM                02-Aug-2026 10:01:50 HRS
 ---------------------------------------------------------------------------------
 Passenger Details
 #  Name        Age   Gender   Booking Status        Current Status
 1. MADHURI     26    F        CNF/B4/33/LOWER       CNF /B4/33/LOWER
 Acronyms: RLWL: REMOTE LOCATION WAITLIST  PQWL: POOLED QUOTA WAITLIST  RSWL: ROAD-SIDE WAITLIST
 Transaction ID: 100006745112096
 IR recovers only 57% of cost of travel on an average.            [QR CODE]
 Payment Details
 Ticket Fare                                   ₹ 1,470.00
 IRCTC Convenience Fee (Incl. of GST)          ₹ 23.60
 Travel Insurance Premium (Incl. of GST)       ₹ 0.45
 Total Fare (all inclusive)                    ₹ 1,494.05
 ... instructions ... page 2 contains phone numbers like +91-8044647999 (10 digits! not a PNR)
```

Notes for parsing:
- "Boarding At" is an **image**, so OCR may not read that label. Do not depend on it.
- The three station values sit on **one visual row**: From, Boarding, To in left-to-right order.
- `Current Status` can contain stray spaces (`CNF /B4/33/LOWER`).
- Page 2 has 10-digit phone numbers. **The PNR must be anchored to the PNR row**, never "first 10-digit number".

**Expected parse of the sample (golden values):**

| Field | Value |
|---|---|
| pnr | `8751652729` |
| trainNumber / trainName | `17001` / `SNSI SC EXPRESS` |
| from | `SNSI` / `SAINAGAR SHIRDI` |
| boarding | `SNSI` / `SAINAGAR SHIRDI` |
| to | `BMT` / `BEGAMPET` |
| startDate | `2026-08-03` |
| departure | `2026-08-03T17:00` Asia/Kolkata |
| arrival | `2026-08-04T07:59` Asia/Kolkata |
| travelClass / classLabel | `3A` / `THIRD AC` |
| quota / quotaLabel | `TQ` / `TATKAL` |
| distanceKm | `772` |
| bookedAt | `2026-08-02T10:01:50` Asia/Kolkata |
| transactionId | `100006745112096` |
| totalFarePaise | `149405` |
| passengers | 1: `MADHURI`, 26, `F`, booking `CNF/B4/33/LOWER`, current → `CNF`, coach `B4`, berth `33`, `LOWER` |

---

## 3. Architecture spec

### 3.1 Package layout (`app/src/main/java/com/example/tripchecker/`)

```
TripCheckerApp.kt                  Application: builds AppContainer, inits PdfBox resource loader
MainActivity.kt                    single activity; handles SEND / SEND_MULTIPLE / VIEW intents
di/AppContainer.kt                 manual DI: db, repo, file store, extractors, importer, clock, appScope
domain/model/                      Ticket, Passenger, Station, BerthStatus, JourneyGroup, JourneyStatus, ExtractionMethod
domain/grouping/StationClusters.kt station code -> city key
domain/grouping/TripGrouper.kt     PURE: List<Ticket> + Instant -> List<JourneyGroup>
domain/ObserveJourneysUseCase.kt   repo.observeTickets() mapped through TripGrouper
data/db/AppDatabase.kt, Converters.kt
data/db/entity/TicketEntity.kt, PassengerEntity.kt
data/db/dao/TicketDao.kt
data/db/relation/TicketWithPassengers.kt
data/db/Mappers.kt                 entity <-> domain
data/file/TicketFileStore.kt       copy Uri -> filesDir/tickets/<uuid>.pdf with SHA-256; delete; File for name
data/repo/TicketRepository.kt      interface
data/repo/OfflineTicketRepository.kt
extraction/model/TextLine.kt       data class TextLine(text, left, top, right, bottom)
extraction/model/ExtractedText.kt  (text: String, method: ExtractionMethod, pageCount: Int)
extraction/RowMerger.kt            PURE: List<TextLine> -> reading-order rows joined by "  "
extraction/PdfTextExtractor.kt     interface { suspend fun extract(file: File): ExtractedText? }
extraction/PdfBoxTextExtractor.kt  text layer
extraction/OcrTextExtractor.kt     PdfRenderer + ML Kit
extraction/CompositeTextExtractor.kt text layer first, OCR fallback
extraction/parser/TicketParser.kt  interface { fun canParse(text): Boolean; fun parse(text): ParsedTicket }
extraction/parser/ParsedTicket.kt  nullable fields + missingFields + warnings
extraction/parser/IrctcErsParser.kt
extraction/parser/PassengerStatusParser.kt
extraction/parser/IrctcDates.kt
extraction/parser/OcrNormalizer.kt digit fixes inside numeric captures
importer/ImportManager.kt          app-scoped queue + StateFlow<List<ImportItem>>
importer/TicketImporter.kt         one file: copy -> dedupe -> extract -> parse -> upsert
importer/ParsedTicketMapper.kt     PURE: ParsedTicket (+ids, now, method, pdf info) -> Ticket; merge into existing Ticket
ui/navigation/Routes.kt, AppNavHost.kt
ui/home/HomeScreen.kt, HomeViewModel.kt, components/{HomeHeader, NextJourneyHero, NeedsReviewRow, TripGroupCard, PastTripRow, AddTicketFabMenu, HomeEmptyState}.kt
ui/importflow/ImportScreen.kt, ImportViewModel.kt, components/{ImportSummaryCard, ImportItemRow}.kt
ui/trip/TripDetailScreen.kt, TripDetailViewModel.kt, components/{TripHeader, LegCard, RouteTimeline, PnrBlock, PassengerRow, StayConnector, PairingInfoCard, LinkReturnSheet}.kt
ui/edit/EditTicketScreen.kt, EditTicketViewModel.kt, components/{MissingFieldsBanner, FormSectionCard, StationFieldRow, PassengerEditorCard}.kt
ui/common/TicketActions.kt         openPdf(context, ticket), sharePdf(...)
ui/common/Formatters.kt            IST date/time, duration, countdown, ranges, money, PNR grouping
ui/common/StatusTone.kt            PURE: BerthStatus -> tone + label (§3.10)
ui/common/components/              TagChip, StatusChip, DateBadge, IconTile, SectionHeader, JoinedList, TicketActionRow
ui/theme/Color.kt, Theme.kt, Type.kt, Shape.kt, Dimens.kt   brand scheme + type + shapes (§3.10)
domain/grouping/CityNames.kt       PURE: Station -> display city ("Hyderabad"), route titles
domain/text/DisplayText.kt         PURE: title-casing for station/train/passenger names (§3.10)
```

**Rules:**
- `domain/**` and `extraction/parser/**` and `extraction/RowMerger.kt` are **pure Kotlin**: no `android.*` imports, so they can be unit tested on the JVM.
- `Clock` (`java.time.Clock`) is injected wherever "now" matters. Tests use `Clock.fixed`.
- Time zone: **always** `ZoneId.of("Asia/Kolkata")` (const `IST` in `IrctcDates`). Parse and display in IST regardless of device zone.

### 3.2 Domain models (shape; names may be refined, but log any change)

```kotlin
enum class ExtractionMethod { TEXT_LAYER, OCR, MANUAL }
data class Station(val code: String, val name: String?)

data class Passenger(
    val id: String, val serial: Int, val name: String, val age: Int?, val gender: String?,
    val bookingStatusRaw: String?, val currentStatusRaw: String?,
    val current: BerthStatus?           // parsed from currentStatusRaw (fallback bookingStatusRaw)
)
data class BerthStatus(
    val code: String,                   // CNF, RAC, WL, RLWL, PQWL, GNWL, TQWL, RSWL, CAN, ...
    val coach: String?, val berthNumber: Int?, val berthType: String?, // "LOWER", "SIDE UPPER", ...
    val waitlistNumber: Int?            // for WL/RAC-number forms
)

data class Ticket(
    val id: String, val pnr: String?,
    val trainNumber: String?, val trainName: String?,
    val from: Station?, val boarding: Station?, val to: Station?,
    val startDate: LocalDate?, val departureAt: Instant?, val arrivalAt: Instant?,
    val travelClass: String?, val classLabel: String?, val quota: String?, val quotaLabel: String?,
    val distanceKm: Int?, val bookedAt: Instant?, val transactionId: String?, val totalFarePaise: Long?,
    val passengers: List<Passenger>,
    val pdfFileName: String?, val pdfSha256: String?, val originalFileName: String?,
    val extractionMethod: ExtractionMethod, val needsReview: Boolean,
    val linkGroupId: String?, val autoPairDisabled: Boolean, val isCancelled: Boolean,
    val notes: String?, val createdAt: Instant, val updatedAt: Instant,
) { val origin: Station? get() = boarding ?: from }

enum class JourneyKind { ROUND_TRIP, ONE_WAY, CUSTOM }
enum class JourneyStatus { UPCOMING, IN_PROGRESS, PAST }
data class JourneyGroup(
    val key: String,                    // "manual:<linkGroupId>" | "auto:<outboundId>" | "single:<ticketId>"
    val kind: JourneyKind, val legs: List<Ticket>, // legs sorted by departure
    val isManual: Boolean, val status: JourneyStatus,
)
```

### 3.3 Database (Room, version 1, schema export ON → `app/schemas/`)

**`tickets` table:** one column per `Ticket` field.
- Stations are flattened: `fromCode`, `fromName`, `boardingCode`, `boardingName`, `toCode`, `toName`.
- Instants are stored as `Long` epoch millis. `LocalDate` is stored as an ISO `String`. Enums are stored as `String`.
- Also store `rawText TEXT` (the extracted text) so tickets can be re-parsed later.
- Indices: `pnr` UNIQUE (nullable), `pdfSha256` UNIQUE (nullable), `departureAt`, `linkGroupId`.

**`passengers` table:**
- `id` PK, `ticketId` FK → tickets.id with **CASCADE** delete, `serial`, `name`, `age`, `gender`
- `bookingStatusRaw`, `currentStatusRaw`
- parsed `statusCode`, `coach`, `berthNumber`, `berthType`, `waitlistNumber`
- Index on `ticketId`.

**DAO:**
- `@Transaction observeAll(): Flow<List<TicketWithPassengers>>` (ordered by `departureAt`)
- `observeById(id)`, `getByPnr(pnr)`, `getBySha(sha)`
- `@Transaction upsertWithPassengers(ticket, passengers)`: upsert the ticket, delete its passengers, insert the new ones
- `deleteById`
- `setLinkGroup(ids, groupId, autoPairDisabled)`, `setCancelled(id, value, updatedAt)`

**Migration policy:** until M6 is ticked, schema changes may just edit version 1. After M6, every change bumps the version and adds a Migration or AutoMigration with an exported schema. Never use `fallbackToDestructiveMigration`.

**Backup:** keep `allowBackup=true`. Update `backup_rules.xml` and `data_extraction_rules.xml` to **include** the database and `files/tickets/`, so Android Auto Backup acts as a free backup.

### 3.4 File storage

- Location: `context.filesDir/tickets/<ticketId>.pdf`. Store only the file name in the DB.
- `TicketFileStore.importFromUri(uri): StagedFile(file, sha256, originalName, sizeBytes)` streams the copy while computing SHA-256 with `DigestInputStream`. Get `originalName` from `OpenableColumns.DISPLAY_NAME`.
- Reject files that are not PDF: check the first 5 bytes equal `%PDF-`. Reject files larger than 20 MB.
- Deleting a ticket deletes its file.

### 3.5 Import pipeline

**`ImportManager`** (singleton in AppContainer, runs on `appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)`):
- `fun enqueue(uris: List<Uri>)`:
  - **Phase A (immediately):** stage-copy every URI. Share-intent URI permissions are temporary, so copy before doing anything slow.
  - **Phase B (sequentially, one file at a time):** OCR is heavy. Run `TicketImporter.process(staged)` for each file.
- `val items: StateFlow<List<ImportItem>>`. Each item has `ImportItem(id, displayName, state)` with `state` one of:
  - `Queued`
  - `Reading`
  - `Saved(ticketId)`
  - `Updated(ticketId)`
  - `Duplicate(existingTicketId)`
  - `NeedsReview(ticketId)`
  - `Failed(reason)`
- `fun clearFinished()`.

**`TicketImporter.process(staged)`**:
1. If a ticket with the same `pdfSha256` exists, delete the staged copy and return `Duplicate`.
2. Run `CompositeTextExtractor.extract(file)`. If it returns `null` or blank, save a ticket with `needsReview = true`, `extractionMethod = OCR` and empty fields, then return `NeedsReview`. The PDF is still kept and openable.
3. Parse with the first `TicketParser` whose `canParse(text)` is true (for now only `IrctcErsParser`). If none matches, behave as in step 2, but keep `rawText`.
4. If the parsed PNR matches an existing ticket, **update it in place**:
   - keep `id`, `linkGroupId`, `autoPairDisabled`, `isCancelled`, `notes` and `createdAt`
   - replace the parsed fields and passengers
   - point it at the new PDF and delete the old PDF file
   - return `Updated`. This handles re-downloads after chart preparation, when the status changed.
5. Otherwise insert a new ticket (new UUID; move/rename the staged file to `<id>.pdf`).
6. `needsReview = missingFields ∩ REQUIRED ≠ ∅`, where `REQUIRED = {PNR, TRAIN_NUMBER, FROM, TO, DEPARTURE_DATE}`. Return `Saved` or `NeedsReview`.

**Entry points:**
- Home FAB menu → "Import PDFs" → `ActivityResultContracts.OpenMultipleDocuments()` with `arrayOf("application/pdf")`.
- Manifest on `MainActivity` (set `android:launchMode="singleTop"`), with intent filters for:
  - `ACTION_SEND` with mimeType `application/pdf`
  - `ACTION_SEND_MULTIPLE` with mimeType `application/pdf`
  - `ACTION_VIEW` with mimeType `application/pdf` (category DEFAULT; scheme `content`)
- Handle the intent in `onCreate` (only when `savedInstanceState == null`) and in `onNewIntent`: extract the URIs, call `importManager.enqueue`, and navigate to the Import screen. Use `IntentCompat.getParcelableExtra` / `getParcelableArrayListExtra` for API-safe extraction.

### 3.6 Text extraction

**`PdfBoxTextExtractor`** (library: PdfBox-Android, `com.tom_roush.pdfbox`):
- Call `PDFBoxResourceLoader.init(applicationContext)` once in `TripCheckerApp.onCreate`.
- `PDDocument.load(file).use { doc -> PDFTextStripper().apply { sortByPosition = true; startPage = 1; endPage = minOf(2, doc.numberOfPages) }.getText(doc) }`.
- The text is **usable** only if it contains `PNR` (case-insensitive) **and** a `\b\d{10}\b`. Otherwise return `null`.
- Catch every exception and return `null`. Encrypted or corrupt PDFs must not crash the app.

**`OcrTextExtractor`** (`android.graphics.pdf.PdfRenderer` + ML Kit Text Recognition, **bundled Latin model**):
- Steps:
  1. Open with `ParcelFileDescriptor.open(file, MODE_READ_ONLY)`, then `PdfRenderer(pfd)`.
  2. Render page index 0 first.
  3. Use `scale = 2000f / page.width`. Create an ARGB_8888 bitmap and call **`bitmap.eraseColor(Color.WHITE)` before `page.render(...)`**. PdfRenderer leaves the background transparent, which breaks OCR.
  4. Render with `RENDER_MODE_FOR_DISPLAY`.
  5. Run `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`, then `.process(InputImage.fromBitmap(bmp, 0)).await()` (kotlinx-coroutines-play-services).
  6. Map every `Text.TextBlock.lines` entry to `TextLine(text, boundingBox…)`, pass the result through `RowMerger`, and recycle the bitmap.
  7. If the page-0 text doesn't contain `PNR`, also OCR page 1 and append it.
- `PdfRenderer` is not thread-safe. Guard it with a `Mutex`, run on `Dispatchers.Default`, and always close the page, the renderer and the pfd (`use {}`).
- Close the recognizer when the app scope ends, or keep one per extractor instance.

**`RowMerger`** (pure):
- Sort lines by vertical centre. Start a new row when a line's centre is more than `0.5 × medianLineHeight` from the current row's mean centre.
- Within a row, sort by `left` and join with **two spaces**. Join rows with `\n`.
- This reconstructs the 3-column ERS rows in left-to-right order.

**`CompositeTextExtractor`:** try PdfBox and return its result if usable (`TEXT_LAYER`). Otherwise use OCR (`OCR`).

### 3.7 Parser spec: `IrctcErsParser` (pure; works on plain text)

**Preprocessing:**
- Normalize `\r\n` → `\n` and tabs → spaces. Split into rows.
- **Keep runs of 2+ spaces.** They are OCR column separators (cells), and the train-name rule can use them. Every regex below uses `\s+` or `\s*`, so it works with single or double spaces.
- Labels (`PNR`, `Departure`, …) match case-insensitively. Output codes and names in uppercase and trimmed.

**`canParse`:** true if the text contains `PNR` and any of `Electronic Reservation Slip`, `ERS`, `IRCTC`, `Train No`.

**Field rules (each one independent; a failure adds to `missingFields`, never throws):**

1. **Stations:**
   - Find the row index of the "Booked From" label (regex `Booked\s*From`). Fallback: the first row that has ≥2 matches of `STATION_RX`.
   - In that row and the next 2 rows, collect matches of `STATION_RX = ([A-Z][A-Z0-9 .'&-]*?)\s*\(\s*([A-Z]{1,5})\s*\)`.
   - Skip matches whose code is a class or quota code; the "Boarding At" image text could be misread.
   - 3 matches → from, boarding, to. 2 matches → from, to, with boarding = from. Trim names.
2. **Start date:** `Start\s*Date\*?\s*(\d{1,2}-[A-Za-z]{3}-\d{4})`.
3. **Departure:** `Departure\*?\s*(\d{1,2}:\d{2})\s*(\d{1,2}-[A-Za-z]{3}-\d{4})`. If it is absent, fall back to startDate at 00:00 and add a warning.
4. **Arrival:** `Arrival\*?\s*(\d{1,2}:\d{2})\s*(\d{1,2}-[A-Za-z]{3}-\d{4})`.
   - If the text instead matches `Arrival\*?\s*N\.?\s*A\.?`, arrival is null and is **not** added to `missingFields` (an IRCTC "N.A." is legitimate).
   - If neither matches, add `ARRIVAL` to `missingFields`. Arrival is not a required field.
5. **PNR:** find the first row matching `\bPNR\b` (this does not match "PNRs" in the instructions). Search that row and the **next 2 rows** for `(?<!\d)([0-9OoIlS|]{10})(?!\d)`.
   - Accept a capture only if it contains **≥ 8 real digits** and `OcrNormalizer.digits(capture)` is exactly 10 digits.
   - **Never** search after the `Transaction ID` row; page 2 has 10-digit phone numbers.
6. **Class** (search the PNR row first, then the whole text): the code comes from `\((1A|2A|3A|3E|SL|CC|EC|2S|FC|EA|EV|VS)\)`. The label comes from a canonical table `CLASS_LABELS`, not from a capture:

   | Code | Label |
   |---|---|
   | 1A | FIRST AC |
   | 2A | SECOND AC |
   | 3A | THIRD AC |
   | 3E | AC 3 ECONOMY |
   | SL | SLEEPER CLASS |
   | CC | AC CHAIR CAR |
   | EC | EXEC. CHAIR CAR |
   | 2S | SECOND SITTING |
   | FC | FIRST CLASS |
   | EA | ANUBHUTI CLASS |
   | EV | VISTADOME AC |
   | VS | VISTADOME NON AC |
7. **Train** (on the PNR row; regexes alone are greedy-unsafe here, so do it in steps):
   1. Find `(\d{5})\s*/\s*`. The train number is the captured group.
   2. Take the substring after that match, up to the class `(` from rule 6 (or to the end of the row).
   3. If that substring contains a 2+ space gap, keep only the part before the first gap (an OCR cell boundary).
   4. Strip trailing class words with `(\s+(FIRST|SECOND|THIRD|AC|SLEEPER|CLASS|CHAIR|CAR|EXEC\.?|EXECUTIVE|ECONOMY|SITTING|ANUBHUTI|VISTADOME|NON|3))+\s*$`.
   5. Trim.
   - Both fixtures must yield `SNSI SC EXPRESS` and `SC SNSI EXPRESS`.
   - ⚠️ A lookahead like `(?=\s+[A-Z ]+\(3A\))` is **wrong**: it stops after "SNSI" because `[A-Z ]+` swallows "SC EXPRESS THIRD AC".
   - Known edge case: a train name that genuinely ends in "AC" loses that word. Accept this and add a test documenting it.
8. **Quota:** the code comes from `\((GN|TQ|PT|LD|SS|HP|DF|FT|YU|DP|PH|RE)\)`, searched only in **rows after the PNR row and before `Passenger Details`**. That keeps it away from the station row, where codes in parentheses are station codes.
   - The label comes from a table `QUOTA_LABELS`: GN GENERAL, TQ TATKAL, PT PREMIUM TATKAL, LD LADIES, SS LOWER BERTH/SR.CITIZEN, HP PERSON WITH DISABILITY, DF DEFENCE, FT FOREIGN TOURIST, YU YUVA, DP DUTY PASS, PH PARLIAMENT HOUSE, RE RAILWAY EMPLOYEE.
   - If a code is missing from the table, fall back to the text before `(` in the same cell.
9. **Distance:** `(\d{1,4})\s*KM\b`, in the same row range as quota.
10. **Booked at:** `(\d{1,2}-[A-Za-z]{3}-\d{4})\s+(\d{1,2}:\d{2}:\d{2})\s*HRS`, in the same row range as quota.
11. **Passengers:** rows after the `Passenger Details` header row (skip the `# Name Age Gender` header) and before the first row matching `Acronyms|Transaction ID|Payment Details`.
    - Row regex: `^\s*(\d{1,2})\s*[.)]?\s+(.+?)\s+(\d{1,3})\s+(M|F|T|MALE|FEMALE|TRANSGENDER)\s+(.+)$`.
    - Status tail handling:
      1. Normalize `\s*/\s*` → `/`.
      2. Join multi-word berth types into one token: `SIDE UPPER`, `SIDE LOWER`, `SIDE MIDDLE`, `WINDOW SIDE`, `NO CHOICE` → replace the inner space with `_`.
      3. Split on whitespace. The first token is booking status and the last token is current status; if there is one token, both are that token.
      4. Restore the spaces.
    - Children or infants rows that don't match → add a warning and skip.
12. **Transaction ID:** `Transaction\s*ID\s*:?\s*([0-9OoIl]{10,20})`, then `OcrNormalizer.digits`.
13. **Total fare:** `Total\s*Fare[^\n]*?([\d,]+\.\d{2})`. Remove the commas, then convert to paise as a `Long`. Do not rely on the `₹` symbol, because OCR reads it as `<`, `R`, `%` or `?`.

**`IrctcDates`:** `DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH)` with case-insensitive parsing (`DateTimeFormatterBuilder().parseCaseInsensitive()`), plus `H:mm` and `H:mm:ss`. Convert with `ZonedDateTime.of(date, time, IST).toInstant()`.

**`OcrNormalizer.digits(s)`:** maps `O o D Q → 0`, `I l | i → 1`, `S s → 5`, `B → 8`, `Z → 2`, `g → 9`, and drops spaces. Use it **only on captures that must be numeric**.

**`PassengerStatusParser.parse(raw): BerthStatus?`:**

| Input | Output |
|---|---|
| `CNF/B4/33/LOWER` | CNF, B4, 33, `LOWER` |
| `CNF /B4/33/LOWER` | same |
| `CNF/S7/49/SIDE UPPER` | CNF, S7, 49, `SIDE UPPER` |
| `CNF/C1/45/WINDOW SIDE` | CNF, C1, 45, `WINDOW SIDE` |
| `CNF/B4/33` | CNF, B4, 33, null |
| `RAC/S2/71/SIDE LOWER` | RAC, S2, 71, `SIDE LOWER` |
| `RAC/23` | RAC, waitlistNumber 23 |
| `RLWL/12`, `WL/40`, `PQWL/7`, `GNWL/105`, `TQWL/3` | code + waitlistNumber |
| `CAN` | CAN |
| `` / null | null |

### 3.8 Grouping spec: `TripGrouper.group(tickets, now): List<JourneyGroup>` (pure)

1. **Manual groups:** tickets with the same non-null `linkGroupId` form one group. `kind` is ROUND_TRIP if there are 2 legs, otherwise CUSTOM. `isManual = true`. key `manual:<linkGroupId>`.
2. **Not auto-paired** (each becomes ONE_WAY, key `single:<id>`): tickets with `needsReview`, `isCancelled`, `autoPairDisabled`, or a null origin, destination or departure.
3. **Auto pairing** of the remaining tickets:
   - Sort by `departureAt` ascending, ties broken by `id`.
   - For each ticket O not yet paired, the candidates are tickets R where all of these hold:
     - R is not yet paired and R ≠ O
     - `city(R.origin) == city(O.to)`
     - `city(R.to) == city(O.origin)`
     - `R.departureAt >= (O.arrivalAt ?: O.departureAt)`
     - `Duration.between(O.departureAt, R.departureAt) <= 14 days`
   - Choose the candidate with the **earliest** departure. Emit ROUND_TRIP with key `auto:<O.id>` and legs `[O, R]`.
   - Here `origin = boarding ?: from` and `city(code) = StationClusters.cityOf(code)` (unknown codes map to themselves).
4. Unpaired tickets become ONE_WAY.
5. **Status**, with `first = legs.first().departureAt` and `last = legs.last().arrivalAt ?: legs.last().departureAt`:
   - `now < first` → UPCOMING
   - `now > last` → PAST
   - anything else → IN_PROGRESS
6. **Sort output:** UPCOMING and IN_PROGRESS groups by first departure ascending, then PAST groups by first departure descending.

**`StationClusters`:** a `Map<String, String>` of code to city. This is editable data. Verify codes against the owner's routes and add more as needed.

| City | Codes |
|---|---|
| HYDERABAD | SC, HYB, KCG, BMT, LPI, CHZ, MJF, HFZ |
| MUMBAI | CSMT, LTT, BCT, DR, DDR, BDTS, TNA, KYN, PNVL, BVI |
| DELHI | NDLS, DLI, NZM, ANVT, DEE |
| BENGALURU | SBC, YPR, SMVB, BNC, KJM |
| CHENNAI | MAS, MS, TBM, PER |
| KOLKATA | HWH, SDAH, KOAA, SRC, SHM |
| PUNE | PUNE, HDP, KK, SVJR |
| SHIRDI | SNSI, KPG |

### 3.9 Screens (Compose, Material 3, type-safe navigation)

> **Visual source of truth:** the mockups in `design/`.
> - `design/screens/*.png`: 7 phone screens (390 dp wide), light theme. **Open the matching PNG before building or changing a screen.**
> - `design/Trip Checker — Material 3 redesign.html`: the same screens as live HTML with a light/dark toggle. It is a 4 MB self-unpacking bundle; open it in a browser, don't parse it. Every token you need is transcribed in §3.10.
> - Where the mockups and this spec disagree on **behaviour**, this spec wins. On **looks** (layout, colour roles, sizes, copy), the mockups win.
> - Mockup data (names, dates, PNRs, file names) is sample data only.

**Routes:** `Home`, `Import`, `TripDetail(anchorTicketId: String, openLinkSheet: Boolean = false)`, `EditTicket(ticketId: String?)` (`null` = create manually).

**TripDetail** shows *the group that contains `anchorTicketId`*. This stays stable even if regrouping changes the keys. If the ticket was deleted, pop back. When `openLinkSheet` is true and the group is ONE_WAY, open the Link-return sheet once on entry.

#### Home (`Home.png`, `Home — Empty state.png`, `Home — Add ticket menu.png`)

`HomeViewModel` → `StateFlow<HomeUiState>`; re-evaluates "now" every 60 s via a ticker flow. The screen is one `LazyColumn` on `surface`; sections are 28 dp apart; cards are inset 12 dp from the screen edge and section headers 20 dp.

- **Header:** a M3 `LargeTopAppBar` (or `LargeFlexibleTopAppBar` with its `subtitle` slot, if the BOM's material3 has it) with `exitUntilCollapsedScrollBehavior`.
  - Title "Trip Checker" in `headlineLarge`.
  - Subtitle `bodyMedium`/`onSurfaceVariant`: today's date plus the upcoming count, e.g. "Sat, 1 Aug · 2 trips ahead" ("1 trip ahead", "No trips ahead"). The count is the number of UPCOMING + IN_PROGRESS groups. If the app bar has no subtitle slot, render it as the first list item.
  - Actions: **Search** (shown only once M7.1 exists) and **More options** (shown only when it has items; until M7.4/M7.5 there are none, so hide it).
- **Next journey hero** (`NextJourneyHero`): the earliest leg with `departureAt > now` across all groups. Section header "Next journey". A `primaryContainer` surface, corner 32 dp, padding 20 dp, 20 dp gaps:
  1. **Top row:** an overline in `labelLarge` caps: "MON, 3 AUG · 17:00", or "TODAY · 17:00" / "TOMORROW · 17:00". Under it, the countdown in `TripTypography.countdown`: "in 2d 4h" (≥ 24 h), "in 5h 20m" (< 24 h), "in 12m" (< 1 h). On the right, a 52 dp `primary` tile (corner 18) with the train icon.
  2. **Route row:** a three-column row.
     - Left: origin code (`TripTypography.stationCodeHero`), name in title case (`bodySmall`), departure time (`titleMedium`).
     - Middle: a hollow dot, a line, an arrow, a line, a filled dot, with the duration ("14h 59m") under it.
     - Right (end-aligned): destination code, name, and arrival time with a `+1` superscript day offset ("07:59 ⁺¹"). If arrival is null, show "—".
  3. **Train line** (`bodyMedium`, medium weight): "17001 · SNSI SC Express · Third AC".
  4. **Seat panel:** a `surfaceContainerLowest` card, corner 24 dp.
     - Label "Your seat · Madhuri" (with more than one passenger: "Your seat · Madhuri · +3 more"), plus a `StatusChip` on the right ("✓ Confirmed").
     - Then three columns: **Coach / Berth / Type** with large values (`TripTypography.seatValue`): `B4` `33` `Lower`.
     - For RAC, WL and similar, replace the three columns with one large value ("WL 12", "RAC 23") and the matching tone chip.
     - Use the first passenger whose status is not CAN. If there are no passengers, hide the panel.
  5. **Actions:** "Go to ticket" (filled `primary` pill, 56 dp, open_in_new icon, fills the width) and a 56 dp square **Share** button (corner 18, `surfaceContainerLowest` container, `primary` icon). Hide both when `pdfFileName == null`.
  - Tapping the hero card body opens `TripDetail(that leg's id)`.
- **Needs review** (only if non-empty): a header "Needs review" with a `tertiary` count badge (20 dp pill). One `NeedsReviewRow` per ticket:
  - a `tertiaryContainer` row, corner 24 dp, min height 72 dp
  - a 40 dp `tertiary` icon tile (corner 14) with the edit icon
  - title "Ticket from <originalFileName>" (`titleSmall`)
  - subtitle "Tap to fill missing details · PNR, departure" (up to 2 missing required fields in plain words, then "+N")
  - a trailing chevron
  - Tapping opens `EditTicket(id)`.
  - Tickets with `needsReview` appear **only** here. Filter them out of the groups below and out of the hero.
- **Upcoming:** groups with UPCOMING or IN_PROGRESS status, rendered with `TripGroupCard`. It is a `surfaceContainerLow` card, corner 28 dp, padding 16, 12–14 dp gaps.
  - **Header row:** a kind `TagChip` ("⇄ Round trip" on `secondaryContainer`; "One way" outlined; "Custom" outlined), an "In progress" status chip when applicable, and the date range on the right (`bodySmall`/`onSurfaceVariant`): "3 – 10 Aug · 8 days" for multi-leg, "Sat, 22 Aug" for one-way.
  - **Title** (ROUND_TRIP and CUSTOM only), in `titleLarge`: "Shirdi ⇄ Hyderabad" (CUSTOM: "Shirdi → Hyderabad → Pune"; see `CityNames`).
  - **Leg rows** in a `JoinedList` (`surfaceContainerLowest`, 20 dp outer and 6 dp inner corners, 4 dp gap). Each row has:
    - a `DateBadge` (48×56 dp, corner 14, "MON / 3 / AUG"). The leg that departs next is on `primaryContainer`; others are on `surfaceContainerHighest`.
    - route "SNSI → BMT" (`titleMedium`)
    - meta "17:00 · 17001 · 3A", with " · boards LPI" appended when boarding ≠ from
    - on the right:
      - seat "B4 · 33" (`titleMedium` bold), with the berth type under it, or "+3 passengers" when there are several
      - for a waitlist or RAC status, a tone chip instead ("WL 12" on `tertiaryContainer`)
  - **One-way footer:** "No return yet" (`bodySmall`/`onSurfaceVariant`) on the left; on the right a `TextButton` "Link return" with the link icon. It navigates to `TripDetail(id, openLinkSheet = true)`.
  - **Cancelled legs:** the route is struck through in `onSurfaceVariant`, with a "Cancelled" chip (`errorContainer`) in place of the seat.
  - Tapping a card opens `TripDetail(anchorTicketId = first leg id)`.
- **Past:** a section header "Past", then month headers ("JULY 2026" in `labelMedium` caps, `primary`), most recent first.
  - Under each month, compact `PastTripRow`s in a `JoinedList` (`surfaceContainerLow`, min height 68 dp). Each has:
    - a 40 dp circular `surfaceContainerHighest` icon (swap_horiz for round trip or custom, arrow_forward for one-way)
    - title "Shirdi ⇄ Hyderabad" or "SNSI → BMT"
    - subtitle "17 – 24 Jul · 17001 / 17002" or "Sun, 5 Jul · 17001"
    - a trailing chevron, or a "Cancelled" chip for a cancelled one-way
- **FAB:** an `ExtendedFloatingActionButton` "Add ticket" (add icon, `primary`, corner 18) that collapses to icon-only while scrolling. Tapping it opens the **Add ticket menu** (`Home — Add ticket menu.png`):
  - a full-screen scrim (`surface` at ~70 % alpha) over the content
  - two right-aligned 56 dp `primaryContainer` pill items stacked above the FAB: "Import PDFs" (description icon) and "Add manually" (edit icon)
  - the FAB morphs into a 56 dp round `primary` button with a close icon
  - Use material3's `FloatingActionButtonMenu` + `ToggleFloatingActionButton` if the BOM ships them (opting into `ExperimentalMaterial3ExpressiveApi` is fine; log it). Otherwise build it with `AnimatedVisibility` and `SmallExtendedFloatingActionButton`s.
  - Back or a scrim tap closes it.
  - "Import PDFs" → `OpenMultipleDocuments`; "Add manually" → `EditTicket(null)`.
- **Empty state** (`Home — Empty state.png`; no tickets at all):
  - the header with no subtitle
  - a centred illustration: a vector drawable `ic_empty_tickets.xml` showing a tilted white ticket with the train glyph and placeholder bars, over `primaryContainer`/`tertiaryContainer` blobs. Recreate it from the PNG as a vector, using theme-friendly colours.
  - headline "Import your IRCTC ticket PDFs" (`headlineSmall`, centred)
  - body "Pick several at once, or share a ticket from any app. Round trips are grouped automatically, and everything stays on this phone."
  - a full-width filled 56 dp "Import PDFs" button (description icon)
  - a `TextButton` "Add manually"
  - No FAB in the empty state.

#### Import (`Import.png`)

- **Top app bar:** back arrow and "Import".
- **Summary card** (`ImportSummaryCard`): `surfaceContainerLow`, corner 28 dp.
  - "5 of 7" (`headlineMedium` bold) = finished/total, with "Reading one at a time" on the right while anything is queued or reading, and "All done" otherwise.
  - A determinate `LinearProgressIndicator` (M3 style with the track gap).
  - A `FlowRow` of count chips, shown only when non-zero: "2 saved" (`primaryContainer`), "1 updated" (`primaryContainer`), "1 duplicate" (`surfaceContainerHighest`), "1 needs review" (`tertiaryContainer`), "1 failed" (`errorContainer`).
- **Rows** (`ImportItemRow`) in a `JoinedList` (`surfaceContainerLow`, min height 64 dp). Each row has a 40 dp icon tile (corner 14), the file name (`titleMedium`, one line, ellipsized in the middle) and a subtitle:

  | State | Tile (container / icon) | Subtitle | Tap |
  |---|---|---|---|
  | `Queued` | `surfaceContainerHighest` / hourglass | "Queued" | — |
  | `Reading` | `secondaryContainer` / indeterminate `CircularProgressIndicator` | "Reading…" in `primary` | — |
  | `Saved` | `primaryContainer` / check | "Saved · SNSI → BMT, 3 Aug · via OCR" (`via text` for TEXT_LAYER); "Saved · paired as round trip" when that ticket is now in a ROUND_TRIP group | TripDetail, chevron |
  | `Updated` | `primaryContainer` / check | "Updated · latest status saved" | TripDetail, chevron |
  | `Duplicate` | `surfaceContainerHighest` / content_copy | "Already imported" | TripDetail of the existing ticket, no chevron |
  | `NeedsReview` | `tertiaryContainer` / edit | "Needs review · tap to finish" | EditTicket, chevron |
  | `Failed` | `errorContainer` / error | the reason, in `error` ("Not a PDF", "Password-protected PDF", …) | — |

  The route and pairing text come from combining the items with `repo.observeTickets()` and `ObserveJourneysUseCase` in `ImportViewModel`.
- **Footer:** "Importing continues if you leave this screen" (`bodySmall`, centred), then a full-width 56 dp filled "Done" button. Done clears finished items and returns Home.

#### Trip detail (`Trip detail — Round trip.png`)

- **Top app bar** (transparent, no title):
  - back
  - **Trip options** overflow. Its items depend on the group:
    - "Unlink tickets" (ROUND_TRIP or CUSTOM)
    - "Link return ticket…" (ONE_WAY)
    - "Reset to automatic pairing" (any leg manual or auto-pair disabled)
- **Header** (`TripHeader`, 20 dp side padding):
  - A row of `TagChip`s:
    - kind: "⇄ Round trip" (`secondaryContainer`), "One way" (outlined) or "Custom" (outlined)
    - status: a dot plus "Upcoming" (`primaryContainer`), "In progress" (`primaryContainer`) or "Past" (`surfaceContainerHighest`)
    - pairing, outlined: "Auto-paired" or "Linked manually". None for ONE_WAY.
  - Title in `headlineMedium` bold: "Shirdi ⇄ Hyderabad", "Shirdi → Hyderabad", or a CUSTOM chain.
  - Subtitle (`bodyLarge`/`onSurfaceVariant`):
    - multi-leg: "Mon, 3 Aug – Mon, 10 Aug · 8 days · ₹3,657.65 total". The total sums the legs that have a fare; omit it if none do.
    - one-way: "Sat, 22 Aug · 17001".
- **Per leg** (`LegCard`): a `surfaceContainerLow` card, corner 32 dp, padding 20 dp, 18 dp gaps.
  1. **Overline** (`labelMedium` caps, `primary`): "OUTBOUND · MON, 3 AUG" / "RETURN · MON, 10 AUG" for ROUND_TRIP, "ONE WAY · SAT, 22 AUG", or "LEG 2 · …" for CUSTOM. Title "17001 · SNSI SC Express" (`titleLarge`).
     - A per-ticket **Ticket options** overflow on the right: "Mark as cancelled" / "Mark as active", and "Delete…" (`AlertDialog` confirm; deletes the row and the PDF).
     - A cancelled leg shows a "Cancelled" chip (`errorContainer`) next to the overline.
  2. **OCR hint** (when `extractionMethod == OCR`): a `tertiaryContainer` banner, corner 14, with an info icon and "Extracted via OCR — double-check details".
  3. **Route timeline** (`RouteTimeline`): three columns (64 dp time, 20 dp rail, station).
     - **Top stop:** time (`titleLarge` bold) with the date under it ("Mon 3 Aug"); a hollow 14 dp ring (3 dp `primary` border); the station code (`TripTypography.stationCode`) with its title-cased name.
     - **Middle:** an `outlineVariant` rail, and a `surfaceContainerHighest` chip with the schedule icon and "14h 59m · 772 km". If arrival is null, show the distance only.
     - **Bottom stop:** a filled `primary` dot; arrival time and date, or "—" with "Arrival N.A."
     - **Boarding ≠ from:** the top stop shows the **boarding** station, with a `primaryContainer` chip under the name: "Boarding here · booked from SC".
  4. **Chips** (`surfaceContainerHighest`, 32 dp, corner 10): "Third AC · 3A" and "Tatkal · TQ".
  5. **PNR block** (`PnrBlock`): `surfaceContainerLowest`, corner 20.
     - Label "PNR", then the PNR grouped 3-3-4 ("875 165 2729") in `TripTypography.pnr`.
     - A 48 dp copy button on the right (`secondaryContainer`, corner 16, content description "Copy PNR"). It copies the **ungrouped** PNR via `ClipboardManager`, then shows the snackbar "PNR copied".
  6. **Passengers:** header "Passenger" or "Passengers · 4" (`labelLarge`/`onSurfaceVariant`), then `PassengerRow`s in a `JoinedList` (`surfaceContainerLowest`). Each row has:
     - a 40 dp avatar circle with the initial (`secondaryContainer`; `surfaceContainerHighest` when cancelled)
     - the name in title case (`titleMedium`)
     - meta "26 · F · booked CNF/B4/33" (`bodySmall`/`onSurfaceVariant`)
     - on the right, the **current status shown prominently**:
       - CNF/RAC with a berth: "B4 · 33" (`titleMedium`, extra bold), then the berth type plus a `StatusChip` ("CNF", "RAC")
       - waitlists: "RLWL 5" plus a "Waitlist" chip
       - CAN: only a "Cancelled" chip
  7. **Total fare:** a row with "Total fare" (`bodyMedium`/`onSurfaceVariant`) and "₹1,494.05" (`titleLarge` bold).
  8. **Actions** (`TicketActionRow`):
     - **Go to ticket**: filled `primary` pill, 56 dp, fills the width. Hidden when `pdfFileName == null`.
     - **Share PDF**: 56 dp square, corner 18, `secondaryContainer`. Hidden when `pdfFileName == null`.
     - **Edit**: 56 dp square, corner 18, outlined.
- **Between legs** (`StayConnector`): a dashed `outlineVariant` line, a `surfaceContainerHigh` pill "6 days in Hyderabad", then another dashed line.
  - The count is the days from leg N's arrival date (or its departure date if arrival is null) to leg N+1's departure date, in IST.
  - Wording: "1 day in X", or "Same-day connection" for 0.
- **Footer** (`PairingInfoCard`): an outlined card, corner 28, padding 16.
  - Auto ROUND_TRIP:
    - title "Paired automatically"
    - body "The return leaves from Hyderabad within 14 days of the outbound, so these tickets were grouped."
    - an `OutlinedButton` "Unlink tickets" (link_off icon). Unlink sets `autoPairDisabled = true` and `linkGroupId = null` on all legs.
  - Manual: "Linked manually", with "Unlink tickets" and "Reset to automatic pairing". Reset clears `linkGroupId` and `autoPairDisabled` on all legs.
  - ONE_WAY:
    - title "No return yet"
    - body "Link a return ticket to see this as a round trip."
    - a filled tonal button "Link return ticket" (link icon) that opens the sheet
    - If `autoPairDisabled` is set, also show "Reset to automatic pairing".

#### Link return sheet (`Link return sheet.png`)

A `ModalBottomSheet` (`surfaceContainerLow`, drag handle).
- Title "Link a return ticket" (`headlineSmall`). Subtitle "Likely returns from <destination city> are listed first."
- **Candidates:** other ONE_WAY, non-cancelled, non-needs-review tickets.
  - Likely returns (reverse city route) come first, then the rest, each sorted by closeness in time.
  - They are shown as single-choice rows in a `JoinedList` (`surfaceContainerLowest`; the selected row is `primaryContainer`), each with:
    - a `RadioButton`
    - route "BMT → SNSI" (`titleMedium`)
    - meta "Sat, 29 Aug · 21:40 · 17002"
    - on the right, a "Likely return" chip (filled `primary` when selected, `primaryContainer` otherwise) above "7 days later" (or "3 days earlier")
  - The whole row is `selectable(role = RadioButton)`.
- **Bottom actions:** `TextButton` "Cancel", plus a filled "Link tickets" with the link icon. It is disabled until a row is selected.
  - "Link tickets" sets the same new UUID `linkGroupId` and `autoPairDisabled = false` on both tickets, then dismisses.
- **Empty:** "No other one-way tickets to link yet."

#### Edit ticket (`Review Edit ticket.png`; also used for review and manual create)

- **Top app bar:**
  - a close icon (on unsaved changes, confirm discard with an `AlertDialog`)
  - title "Review ticket" (`needsReview`), "Edit ticket" or "Add ticket" (create)
  - a small filled "Save" button as the action
- **Missing-fields banner** (`MissingFieldsBanner`), shown only while required fields are missing: `errorContainer`, corner 24.
  - A warning icon with "2 required fields missing" (`titleMedium` bold).
  - The list in plain words: "PNR · Departure date and time".
  - "From scan_0812.pdf · read with OCR", with an "Open PDF" button (a `surface` pill with the open_in_new icon) when a PDF exists.
  - It updates live as fields are filled.
- **Section cards** (`FormSectionCard`: `surfaceContainerLow`, corner 28, padding 16, title `titleMedium`). All inputs are `OutlinedTextField`s.
  - **Ticket:**
    - PNR\* (full width, number keyboard, max 10 digits; supporting text "Required · not found in the PDF" when missing)
    - Train no. (≈ 40 % width, number keyboard) and Train name
  - **Route** (`StationFieldRow`: a ~96 dp code field, uppercased and bold, plus a Station name field):
    - From\*
    - Boarding (placeholders "Code" / "Same as From"; supporting "Optional · only if you board at a different station")
    - To\*
  - **Schedule · IST:**
    - Departure date\* and Time\*, both required, side by side. These are read-only fields with calendar or clock trailing icons, and tapping opens the M3 `DatePickerDialog` / `TimePicker` dialog. When missing, show the supporting text "Required for grouping into trips".
    - Arrival date and Time.
    - **Gotcha:** the date picker returns UTC-midnight millis, so convert with `ZoneOffset.UTC` to a `LocalDate`.
    - Display dates as "Tue, 4 Aug 2026".
  - **Class and fare:** a 2×2 grid of Class, Quota, Distance (km) and Total fare (₹) (decimal keyboard). Class and quota are free text.
  - **Passengers:** one `PassengerEditorCard` each (`surfaceContainerLowest`, corner 20). It contains:
    - "Passenger N" and a delete icon button (content description "Remove passenger N")
    - Name
    - Age (narrow) and a gender `SingleChoiceSegmentedButtonRow` (M / F / T)
    - Current status in a monospace field, with live supporting text from `PassengerStatusParser`: "Reads as coach B4, berth 33, lower", or "Not recognised — will be saved as typed"
    - Below the cards, a full-width `FilledTonalButton` "+ Add passenger".
- **Bottom:** a full-width 56 dp filled "Save ticket" button. It does the same as the top-bar Save.
- **Validation:** required fields that are missing get `isError = true`, an error label with `*`, and helper text. Save is always enabled: it saves and keeps `needsReview = true` while required fields are missing, and sets `needsReview = false` when all are present. It also sets `updatedAt`, then pops back.
- **Create mode:** `extractionMethod = MANUAL`, `pdfFileName = null`, no banner PDF button.

#### Go to ticket and Share (`ui/common/TicketActions.kt`)

- Manifest `<provider androidx.core.content.FileProvider authorities="${applicationId}.fileprovider" exported="false" grantUriPermissions="true">` with `res/xml/file_paths.xml` → `<files-path name="tickets" path="tickets/"/>`.
- Open: `Intent(ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK)`. Catch `ActivityNotFoundException` → snackbar "No PDF app found" with a **Share** action.
- Share: `ACTION_SEND`, type `application/pdf`, `EXTRA_STREAM`, plus the grant flag, wrapped in `Intent.createChooser`.

#### Formatting (`Formatters.kt`, all in IST, `Locale.ENGLISH`, all pure and JVM-tested)

| Function | Example |
|---|---|
| date | `EEE, d MMM` → "Mon, 3 Aug" |
| date with year (edit form) | "Tue, 4 Aug 2026" |
| time | `HH:mm` → "17:00" |
| hero overline | "MON, 3 AUG · 17:00", "TODAY · 17:00", "TOMORROW · 17:00" |
| countdown | "in 2d 4h", "in 5h 20m", "in 12m" |
| day offset | arrival on the next IST date → "+1" (null when the same day) |
| month header | `MMMM yyyy` uppercased → "JULY 2026" |
| date range | "3 – 10 Aug"; across months "28 Jul – 4 Aug"; across years "28 Dec 2026 – 4 Jan 2027"; detail form "Mon, 3 Aug – Mon, 10 Aug" |
| trip length | inclusive IST days: 3 → 10 Aug = "8 days"; same day = "1 day" |
| stay | "6 days in Hyderabad", "1 day in Hyderabad", "Same-day connection" |
| relative days (link sheet) | "7 days later", "1 day later", "3 days earlier", "Same day" |
| home subtitle | "Sat, 1 Aug · 2 trips ahead" / "1 trip ahead" / "No trips ahead" |
| duration | "14h 59m" |
| money | "₹1,494.05", using `NumberFormat` for `en-IN` |
| PNR | "8751652729" → "875 165 2729"; anything other than 10 digits is returned unchanged |

#### Icons

Material3 ≥ 1.4 no longer pulls in icons transitively.
- From `material-icons-core` (`Icons.Default.*`): Add, ArrowBack, ArrowForward, Check, Close, Create/Edit, Delete, Info, KeyboardArrowRight, MoreVert, Search, Share, Warning, DateRange.
- Everything else is a **vector drawable** from Material Symbols (Rounded, weight 400, fill 1 where the mockup is filled) under `res/drawable/`:
  - `ic_train` (directions_railway)
  - `ic_swap_horiz`
  - `ic_open_in_new`
  - `ic_content_copy`
  - `ic_link`
  - `ic_link_off`
  - `ic_schedule`
  - `ic_description`
  - `ic_hourglass`
  - `ic_error`
  - `ic_event` (calendar)
  - `ic_empty_tickets` (the illustration)
- **Do not add `material-icons-extended`**; it is huge.

### 3.10 Visual design system (transcribed from `design/`)

**Theme (`ui/theme/`):**
- `TripcheckerTheme` uses the **brand colour scheme below by default**. Keep the `dynamicColor` parameter but default it to `false`; the mockups' look depends on these exact roles.
- Follow the system light/dark setting.
- Call `enableEdgeToEdge()` in `MainActivity`. Content draws behind the system bars and handles insets via `Scaffold`/`WindowInsets`.
- Use **only M3 colour roles** (`MaterialTheme.colorScheme.*`) in UI code. No hard-coded colours outside `Color.kt`.

**Colour scheme** (seed indigo `#4355B9`). Values marked † are not in the mockups; they are derived from the same tonal palettes.

| Role | Light | Dark |
|---|---|---|
| primary | `#4355B9` | `#BAC3FF` |
| onPrimary | `#FFFFFF` | `#08218A` |
| primaryContainer | `#DEE0FF` | `#293CA0` |
| onPrimaryContainer | `#00105C` | `#DEE0FF` |
| secondary † | `#5A5D72` | `#C3C5DD` |
| onSecondary † | `#FFFFFF` | `#2C2F42` |
| secondaryContainer | `#E0E1F9` | `#43465A` |
| onSecondaryContainer | `#181A2C` | `#E0E1F9` |
| tertiary | `#77536D` | `#E6BAD7` |
| onTertiary | `#FFFFFF` | `#44263D` |
| tertiaryContainer | `#FFD7F1` | `#5D3C55` |
| onTertiaryContainer | `#2D1228` | `#FFD7F1` |
| error | `#BA1A1A` | `#FFB4AB` |
| onError † | `#FFFFFF` | `#690005` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |
| background, surface | `#FBF8FF` | `#121318` |
| onBackground, onSurface | `#1B1B21` | `#E4E1E9` |
| surfaceVariant † | `#E3E1EC` | `#46464F` |
| onSurfaceVariant | `#46464F` | `#C6C5D0` |
| surfaceContainerLowest | `#FFFFFF` | `#0D0E13` |
| surfaceContainerLow | `#F5F2FA` | `#1B1B21` |
| surfaceContainer | `#EFEDF4` | `#1F1F25` |
| surfaceContainerHigh | `#E9E7EF` | `#2A292F` |
| surfaceContainerHighest | `#E4E1E9` | `#35343A` |
| surfaceDim † | `#DBD9E0` | `#121318` |
| surfaceBright † | `#FBF8FF` | `#38393F` |
| outline | `#767680` | `#90909A` |
| outlineVariant | `#C6C5D0` | `#46464F` |
| inverseSurface † | `#303036` | `#E4E1E9` |
| inverseOnSurface † | `#F2EFF7` | `#303036` |
| inversePrimary † | `#BAC3FF` | `#4355B9` |
| surfaceTint | = primary | = primary |
| scrim | `#000000` | `#000000` |

**Typography (`Type.kt`):**
- The mockups use Roboto Flex and Roboto Mono. The app uses the **system font** (`FontFamily.Default`, which is Roboto on Android) and `FontFamily.Monospace`. **Do not bundle or download fonts**; the app is offline and this keeps the APK small.
- Mockup weight 650 → `SemiBold`; 700–750 → `Bold`; 800 → `ExtraBold`. Ignore `font-stretch`.

| M3 role (override) | Size / line (sp) | Weight | Tracking | Used for |
|---|---|---|---|---|
| displaySmall | 44 / 50 | Bold | −1 | — (base of `countdown`) |
| headlineLarge | 34 / 42 | SemiBold | −0.5 | Home title |
| headlineMedium | 32 / 40 | Bold | −0.5 | Trip detail title, Import "5 of 7" |
| headlineSmall | 28 / 36 | SemiBold | 0 | Sheet titles, empty-state headline |
| titleLarge | 22 / 28 | SemiBold | 0 | Group card title, leg title, fare |
| titleMedium | 16 / 22 | SemiBold | 0 | Leg-row route, passenger name, list titles, button text |
| titleSmall | 14 / 20 | SemiBold | 0 | Section headers ("Next journey", "Upcoming") |
| bodyLarge | 16 / 24 | Normal | 0.5 | Body |
| bodyMedium | 14 / 20 | Normal | 0.25 | Body, subtitles |
| bodySmall | 13 / 18 | Normal | 0.2 | Meta lines ("17:00 · 17001 · 3A") |
| labelLarge | 14 / 20 | SemiBold | 0.1 | Chips, buttons |
| labelMedium | 12 / 16 | Bold | 0.6 | Overlines in caps ("JULY 2026", "OUTBOUND · …") |
| labelSmall | 11 / 16 | Bold | 0.5 | Small status chips ("CNF") |

Extra styles go in `object TripTypography` (built from the roles above with `copy`):

| Name | Spec | Used for |
|---|---|---|
| `countdown` | 44/50 Bold, −1 | Hero "in 2d 4h" |
| `stationCodeHero` | 32/36 ExtraBold | Hero station codes |
| `stationCode` | 22/26 ExtraBold | Timeline station codes |
| `seatValue` | 30/36 Bold | Hero Coach/Berth/Type values |
| `pnr` | Monospace, 22/30 SemiBold, +1 | PNR block |

**Shapes (`Shape.kt`):**
- `Shapes(extraSmall = 6.dp, small = 10.dp, medium = 16.dp, large = 20.dp, extraLarge = 28.dp)`.
- Extra values in `object TripShapes`:
  - `hero = 32.dp` (hero and leg cards)
  - `tile = 14.dp` (icon tiles, date badges)
  - `squareButton = 18.dp` (56 dp square action buttons, FAB)
  - `joinedInner = 6.dp`
  - Pill buttons use `CircleShape`.

**Spacing (`Dimens.kt`):**
- screen gutter for cards: 12 dp
- text headers: 20 dp
- card padding: 16 dp (hero and leg cards: 20 dp)
- section gap: 28 dp
- gap between cards: 12 dp
- `JoinedList` item gap: 4 dp
- primary buttons: 56 dp tall
- minimum touch target: 48 dp

**Shared components (`ui/common/components/`, each with light and dark `@Preview`s):**
- **`TagChip(text, tone, leadingIcon?, dot?)`**: 28 dp, corner 10, `labelLarge`. Tones: `Secondary` (`secondaryContainer`), `Primary` (`primaryContainer`), `Outlined` (1 dp `outlineVariant` border, `onSurfaceVariant` text), `Neutral` (`surfaceContainerHighest`), `Tertiary`, `Error`. It is non-interactive, so use `Surface` and not `AssistChip`.
- **`StatusChip(status: BerthStatus?)`**: picks its tone and label through `StatusTone` (below).
- **`DateBadge(date, highlighted)`**: 48×56 dp, corner 14, with weekday/day/month stacked ("MON" `labelSmall`, "3" 22sp Bold, "AUG" `labelSmall`).
- **`IconTile(icon, container, content, size = 40.dp)`**: a rounded square (corner 14) or a circle.
- **`SectionHeader(title, count?)`**: `titleSmall` on `onSurfaceVariant`, with an optional `tertiary` count badge.
- **`JoinedList`**: lays out items with M3 "connected" corners: first item 20/20/6/6, middle items 6, last item 6/6/20/20, a single item 20 all round, and a 4 dp gap.
- **`TicketActionRow(onOpen, onShare, onEdit?, shareContainer)`**: a "Go to ticket" pill plus 56 dp square buttons.

**`StatusTone` (pure; JVM-tested):**

| Status code | Tone / container | Chip label (hero) | Chip label (passenger row) |
|---|---|---|---|
| `CNF` | Primary / `primaryContainer` | "Confirmed" (check icon) | "CNF" |
| `RAC` | Secondary / `secondaryContainer` | "RAC" | "RAC" |
| `WL`, `RLWL`, `PQWL`, `GNWL`, `TQWL`, `RSWL`, any other `*WL` | Tertiary / `tertiaryContainer` | "Waitlist" | "Waitlist" |
| `CAN` | Error / `errorContainer` | "Cancelled" | "Cancelled" |
| null or unknown | Neutral / `surfaceContainerHighest` | raw code or "Status unknown" | raw code |

**`DisplayText.titleCase(raw, keepUpper)` (pure, `domain/text`; JVM-tested):**
- Title-cases ALL-CAPS ticket text: "SAINAGAR SHIRDI" → "Sainagar Shirdi", and "MADHURI" → "Madhuri".
- A word stays uppercase if any of these hold:
  - it is in `keepUpper` (the ticket's station codes plus all `StationClusters` codes)
  - it is in `{JN, AC, SF, SL, CC, EC, CNF, RAC}`
  - it is ≤ 2 letters
  - it contains a digit
- Examples: "SNSI SC EXPRESS" → "SNSI SC Express"; "SECUNDERABAD JN" → "Secunderabad JN"; "THIRD AC" → "Third AC".
- Apply it only at display time; the DB keeps the raw uppercase values.

**`CityNames` (pure, `domain/grouping`; JVM-tested):**
- `cityLabel(station)` returns the `StationClusters` city in title case ("HYDERABAD" → "Hyderabad", "BENGALURU" → "Bengaluru"). Otherwise it returns the title-cased station name, otherwise the code.
- `routeTitle(group)`:
  - ROUND_TRIP: "Shirdi ⇄ Hyderabad"
  - ONE_WAY: "Shirdi → Hyderabad"
  - CUSTOM: "A → B → C" (collapse consecutive duplicates)

**Accessibility:**
- Every icon-only button has a content description, matching the mockup's labels: "Search tickets", "More options", "Share ticket PDF", "Copy PNR", "Trip options", "Ticket options", "Edit ticket", "Remove passenger N", "Close".
- Touch targets are ≥ 48 dp.
- Seat and status information is not conveyed by colour alone; the chip label always carries it.
- Layouts survive a 1.3× font scale: station names and file names ellipsize, and codes never wrap.
- Check the dark theme with the same screens.

---

## 4. Milestones and tasks

Every milestone ends **green**: `.\gradlew.bat assembleDebug testDebugUnitTest` passes. Instrumented tests (`connectedDebugAndroidTest`) are optional; they need a device or emulator, so never block on them.

### M0: Project setup
- [x] **M0.1** Update `gradle/libs.versions.toml` per `AGENTS.md` § Dependencies:
  - bump core-ktx, lifecycle, activity-compose and the test libs to the latest stable
  - add KSP, Room (+ the `androidx.room` Gradle plugin), the kotlinx-serialization plugin and json, navigation-compose, lifecycle-viewmodel-compose and lifecycle-runtime-compose, coroutines (android, play-services, test), ML Kit text-recognition (bundled), pdfbox-android, and material-icons-core
  - apply the plugins in the root and app `build.gradle.kts`
  - configure `room { schemaDirectory("$projectDir/schemas") }`
  - *Accept:* Gradle sync + `assembleDebug` pass.
- [x] **M0.2** Create `TripCheckerApp` (registered in the manifest via `android:name`) and `di/AppContainer`. Leave stubs only for what M0 needs, filled in later milestones. Set `app_name` to "Trip Checker". *Accept:* the app launches into an empty Compose `Scaffold` with the title "Trip Checker".
- [x] **M0.3** Remove the template `Greeting`, `GreetingPreview` and `ExampleUnitTest` (replaced by real tests in M1). Keep `ExampleInstrumentedTest`, but make sure it still passes.
- [x] **M0.4** Manifest changes:
  - `singleTop` launch mode
  - SEND / SEND_MULTIPLE / VIEW intent filters for `application/pdf`
  - the FileProvider plus `res/xml/file_paths.xml`
  - backup rules including the DB and `files/tickets/`
  - *Accept:* builds, and the manifest merger reports no errors.

### M1: Parser (JVM unit tests, test-first)
- [x] **M1.1** Domain models (§3.2), `ExtractionMethod`, and `IrctcDates` with tests (case-insensitive months, single-digit days, IST conversion).
- [x] **M1.2** `OcrNormalizer` and `PassengerStatusParser`, with a test for **every row** of the §3.7 table.
- [x] **M1.3** Create the fixtures in `app/src/test/resources/fixtures/` exactly as in §5. Add a small `Fixtures.load(name)` helper that uses the classloader.
- [x] **M1.4** `ParsedTicket`, `TicketParser`, and `IrctcErsParser`.
  - Tests:
    - `ers_sample_text_layer.txt` → **all** §2 golden values
    - `ers_sample_ocr_rows.txt` → the same golden values
    - `ers_multi_passenger.txt` → its §5 expectations
    - text with no PNR → `missingFields` contains PNR and there is no crash
    - empty string → all fields missing and no crash
    - `canParse` returns false for random text
  - *Accept:* all pass.
- [x] **M1.5** `RowMerger` (pure), with tests that build `TextLine`s for the 3-column station row in shuffled order and assert the merged row reads `SAINAGAR SHIRDI (SNSI)  SAINAGAR SHIRDI (SNSI)  BEGAMPET (BMT)`. Add a test for slight vertical jitter (±4 px) and one for two separate rows.

### M2: Grouping (JVM unit tests, test-first)
- [x] **M2.1** `StationClusters` with tests: BMT and SC map to the same city, and an unknown code maps to itself.
- [x] **M2.2** `TripGrouper`, using a small `ticket(...)` test builder. Required scenarios, with times in IST:
  1. SNSI→BMT on 3 Aug 17:00 (arrives 4 Aug 07:59) and BMT→SNSI on 10 Aug → one ROUND_TRIP.
  2. Return on 20 Aug (more than 14 days) → two ONE_WAY.
  3. Alternating A→B on 1, B→A on 8, A→B on 15 and B→A on 22 → pairs (1,8) and (15,22), **not** (8,15).
  4. Cluster: SNSI→BMT, then SC→SNSI 5 days later → ROUND_TRIP.
  5. Same input as scenario 1 in reversed list order → identical output.
  6. Two candidate returns (6 Aug and 9 Aug) → pairs with 6 Aug; 9 Aug is ONE_WAY.
  7. A return whose departure is before the outbound arrival → not paired.
  8. `needsReview`, `isCancelled` or `autoPairDisabled` tickets → ONE_WAY, and never paired.
  9. A manual `linkGroupId` across 3 tickets → one CUSTOM with `isManual = true`, even if dates are 30 days apart.
  10. Boarding station differs from From: from SC, boarding LPI, to SNSI. Matching uses boarding (LPI, which is HYDERABAD).
  11. Status with `Clock.fixed`: before → UPCOMING; between the legs → IN_PROGRESS; after → PAST.
  12. Output ordering per §3.8 step 6.
- [x] **M2.3** Create `importer/ParsedTicketMapper.kt` (pure) now, with its own tests. Integration test: parse `ers_sample_text_layer.txt` and `ers_multi_passenger.txt`, map them to `Ticket` via `ParsedTicketMapper`, and group them. Expect **one ROUND_TRIP** (SNSI→BMT on 3 Aug paired with SC/LPI→SNSI on 10 Aug).

### M3: Data layer
- [x] **M3.1** Entities, relation, `Converters`, `TicketDao`, `AppDatabase` (version 1, exportSchema), and `Mappers` (entity ↔ domain, including passengers with parsed `BerthStatus`). Add a JVM test that round-trips the mapper.
- [x] **M3.2** `TicketFileStore` (copy plus SHA-256, PDF magic check, 20 MB cap, delete, `fileFor(name)`). Extract the hashing and magic check into a pure helper and test it on the JVM with byte arrays.
- [x] **M3.3** `TicketRepository` and `OfflineTicketRepository`:
  - `observeTickets(): Flow<List<Ticket>>`, `observeTicket(id)`, `findByPnr`, `findBySha`
  - `upsert(ticket)`, `delete(id)` (also deletes the file), `setCancelled`
  - `link(ids)`, `unlink(ids)`, `resetPairing(ids)`
  - `ObserveJourneysUseCase`
- [ ] **M3.4** (optional, androidTest) Room DAO tests with an in-memory DB: upsert with passengers, cascade delete, and the unique PNR.

### M4: Extraction and import
- [x] **M4.1** `PdfBoxTextExtractor` per §3.6, with exception-safe handling and the usability check. Call `PDFBoxResourceLoader.init` in the App.
- [x] **M4.2** `OcrTextExtractor` per §3.6: white background, ~2000 px width, a mutex, `use {}` everywhere, and a page-2 fallback.
- [x] **M4.3** `CompositeTextExtractor`.
- [x] **M4.4** `TicketImporter` per §3.5, covering the duplicate-by-SHA, update-by-PNR and needs-review rules. Design it against interfaces (extractor, parser, repository, file store) so it can be **JVM-tested with fakes**. Tests:
  - new → Saved
  - same SHA → Duplicate (staged file deleted)
  - same PNR → Updated (id, links and notes kept; old file deleted)
  - missing PNR → NeedsReview
  - extractor returns null → NeedsReview, with the PDF kept
- [x] **M4.5** `ImportManager` with phase A copying immediately and phase B sequential. `StateFlow` items. JVM test with fakes and `StandardTestDispatcher`.

### M5: UI
- [x] **M5.1** `Routes` (`@Serializable`), `AppNavHost`, and `MainActivity`, with intent handling (share, open-with) → enqueue → navigate to Import.
- [x] **M5.2** `Formatters` with JVM tests (IST formatting, duration, countdown strings, money "₹1,494.05").
- [x] **M5.3** Home screen and `HomeViewModel`: next-journey hero, needs-review, upcoming and past by month, empty state, and the FAB menu wired to `OpenMultipleDocuments`. Add `@Preview`s with fake data for every card.
- [x] **M5.4** Import screen and ViewModel.
- [x] **M5.5** Trip detail screen and ViewModel: legs, passengers, PNR copy, Go to ticket, Share, Edit, Cancel toggle, Delete confirm, Unlink, Link-return sheet, and Reset to automatic.
- [x] **M5.6** Edit ticket screen and ViewModel: edit and create modes, the passenger editor, date and time pickers, and validation.
- [x] **M5.7** `TicketActions`: open the PDF externally (with the ActivityNotFound fallback) and share.
- [x] **M5.8** ViewModel unit tests for the Home and Trip detail state mapping, using a fake repository and a fixed clock.

### M6: Hardening
- [x] **M6.1** Edge cases:
  - an encrypted, corrupt or 0-byte PDF → Failed or NeedsReview, never a crash
  - a non-PDF shared file → Failed("Not a PDF")
  - 20 files imported at once → still responsive
  - rotation during import → state survives (it is app-scoped)
  - process death → the DB is consistent and no orphan staged files are left (clean `cacheDir/staging` at app start)
- [ ] **M6.2** `.\gradlew.bat lintDebug`: fix all errors and the obvious warnings (hardcoded strings may stay for now).
- [x] **M6.3** R8 check. Temporarily set release `optimization { enable = true }` and run `.\gradlew.bat assembleRelease`. Add any keep rules PdfBox-Android needs (e.g. `-dontwarn com.gemalto.jp2.**`, `-dontwarn org.bouncycastle.**`, and keep rules for `com.tom_roush.pdfbox` resources if they are missing at runtime) to `app/src/main/keepRules/rules.keep`. Then set it back to `false`, unless the build works and you are confident, in which case log the decision.
- [x] **M6.4** Write `docs/QA.md` from the §6 checklist, marked "to be run by owner on device".
- [x] **M6.5** Final pass:
  - no TODOs or stubs left in M0–M6 code
  - every public class has a one-line KDoc
  - the `README.md` has a short description and the build/run steps

### MD: Material 3 redesign (match `design/` mockups)
Do this after M6 and before M7. Every task means: open the matching `design/screens/*.png`, build to §3.9 and §3.10, add light **and** dark `@Preview`s with fake data for every new composable, then run the verify command. Keep stateful `XScreen` and stateless `XContent` apart, and keep files under ~400 lines (split into `components/`). Existing behaviour and tests must keep passing. Change the UI, not the domain rules.
- [x] **MD.1** Theme foundation (§3.10).
  - Rewrite `Color.kt` with the full light and dark brand schemes, and drop the template purple values.
  - In `Theme.kt`, use the brand scheme by default (`dynamicColor = false`).
  - Fill `Type.kt` with the M3 role overrides plus `TripTypography`.
  - Add `Shape.kt` (`Shapes` + `TripShapes`) and `Dimens.kt`.
  - Call `enableEdgeToEdge()` in `MainActivity`, with correct inset handling on every screen.
  - *Accept:* builds. A preview "palette sheet" composable in `ui/theme/ThemePreview.kt` shows every role swatch and type style, light and dark.
- [x] **MD.2** Pure display helpers, **test-first** (JVM):
  - `DisplayText.titleCase` (every §3.10 example, plus empty or blank input)
  - `StatusTone` (every row of the §3.10 table)
  - `CityNames.cityLabel` and `routeTitle` (cluster city, unknown code falls back to the name, then the code; ROUND_TRIP, ONE_WAY and CUSTOM titles; duplicate collapse)
  - the new `Formatters` functions from the §3.9 table: hero overline today/tomorrow/other, countdown boundaries at 1 h and 24 h, day offset, date ranges within a month/across months/across years, inclusive trip length, stay wording 0/1/N, relative days later/earlier/same, home subtitle 0/1/N, PNR grouping including non-10-digit input
  - *Accept:* all pass.
- [x] **MD.3** Vector drawables and shared components.
  - Add the §3.9 Icons drawables (Material Symbols, from their path data), plus the `ic_empty_tickets` illustration recreated from `Home — Empty state.png`.
  - Build `ui/common/components/`: `TagChip`, `StatusChip`, `DateBadge`, `IconTile`, `SectionHeader`, `JoinedList`, `TicketActionRow`.
  - *Accept:* previews render in light and dark.
- [x] **MD.4** Home redesign (`Home.png`):
  - the large header with the computed subtitle
  - `NextJourneyHero` with the seat panel and its waitlist variant
  - `NeedsReviewRow` with the missing-field summary
  - `TripGroupCard` for ROUND_TRIP, ONE_WAY (with the "Link return" footer) and CUSTOM, including the cancelled and in-progress states
  - `PastTripRow` under month headers
  - Extend `HomeUiMapper` and `HomeUiState` with what the cards need (city titles, date ranges, seat summaries, missing-field labels, the `+1` offset), and extend `HomeUiMapperTest` to cover the new fields.
  - Add the `TripDetail.openLinkSheet` route argument.
- [x] **MD.5** Home FAB menu and empty state (`Home — Add ticket menu.png`, `Home — Empty state.png`):
  - an extended FAB that collapses on scroll
  - the scrim menu with "Import PDFs" and "Add manually", closed by back or a scrim tap
  - the empty state with the illustration, the "Import PDFs" button and the "Add manually" text button
  - Log in §7 whether material3's `FloatingActionButtonMenu` or a hand-built menu was used.
- [x] **MD.6** Import redesign (`Import.png`):
  - `ImportSummaryCard` (finished/total, progress bar, non-zero count chips)
  - `ImportItemRow` per the §3.9 state table, including the "paired as round trip" and "via OCR" subtitles; the subtitle builder must be pure, with a JVM test
  - the footer note and the full-width Done button
- [x] **MD.7** Trip detail redesign (`Trip detail — Round trip.png`):
  - the transparent top bar with the Trip options menu
  - `TripHeader` (kind, status and pairing chips; city title; the subtitle with the total fare)
  - `LegCard`: overline, per-ticket options, OCR banner, `RouteTimeline` (with the boarding-differs and arrival-N.A. variants), class and quota chips, `PnrBlock` (copies ungrouped), `PassengerRow` with status tones, total fare, `TicketActionRow`
  - `StayConnector` between legs
  - `PairingInfoCard` for the auto, manual and one-way variants
  - Extend the TripDetail ViewModel tests for the header subtitle, stay label and pairing-card variant.
- [x] **MD.8** Link return sheet (`Link return sheet.png`):
  - select-then-confirm radio rows (likely returns first) with "Likely return" chips and relative-days text
  - Cancel and "Link tickets" (disabled until a row is selected)
  - the empty state
  - Exclude cancelled and needs-review tickets from the candidates, and extend `LinkReturnCandidatesTest` to cover it.
  - Rename `LinkTicketSheet` → `LinkReturnSheet`.
- [x] **MD.9** Edit / Review ticket redesign (`Review Edit ticket.png`):
  - the top bar (close with a discard confirm, a mode-dependent title, a Save action)
  - `MissingFieldsBanner`, live, with "Open PDF"
  - section cards: Ticket, Route (`StationFieldRow`), Schedule · IST (read-only picker fields), Class and fare, Passengers
  - `PassengerEditorCard` with the M/F/T segmented buttons and the live status read-back from `PassengerStatusParser`
  - the "Add passenger" tonal button and the bottom "Save ticket" button
  - The status read-back text builder must be pure, with a JVM test.
  - Keep `EditTicketMapperTest` green.
- [x] **MD.10** Polish and gate:
  - every screen in dark theme, at 1.3× font scale, and with a long station name or file name: no clipping, codes never wrap
  - content descriptions per §3.10
  - all user-visible strings moved to `strings.xml`, with plurals via `pluralStringResource` for "N trips ahead", "N days" and "Passengers · N"
  - remove dead composables from the old UI (`NextJourneyCard`, `RoundTripCard`, `OneWayCard`, `LegSection`, if they are unused)
  - add the §6 visual checks to `docs/QA.md`
  - *Accept:* `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` is green, with no lint errors.

### M7: Stretch (only after M0–M6 and MD are all ticked)
- [ ] **M7.1** Search bar on Home that filters by PNR, station code or name, and train number or name.
- [ ] **M7.2** A "Train tomorrow" local reminder notification the evening before departure (WorkManager or AlarmManager; asks for `POST_NOTIFICATIONS`). It shows train, time and coach/berth.
- [ ] **M7.3** A home-screen widget (Glance) for the next journey (route, time, coach/berth); tapping it opens the trip.
- [ ] **M7.4** Export/backup: a zip containing every PDF plus `tickets.json`, saved via `CreateDocument`, and an import of that zip.
- [ ] **M7.5** A "Re-parse all" action in a debug or settings menu that re-runs the parser on the stored `rawText` after parser improvements.

---

## 5. Test fixtures (create these files exactly)

### `app/src/test/resources/fixtures/ers_sample_text_layer.txt`
This simulates PdfBox `sortByPosition` output: single spaces, with "Boarding At" present.
```
Electronic Reservation Slip (ERS)-Normal User
Booked From Boarding At To
SAINAGAR SHIRDI (SNSI) SAINAGAR SHIRDI (SNSI) BEGAMPET (BMT)
Start Date* 03-Aug-2026 Departure* 17:00 03-Aug-2026 Arrival* 07:59 04-Aug-2026
PNR Train No./Name Class
8751652729 17001 / SNSI SC EXPRESS THIRD AC (3A)
Quota Distance Booking Date
TATKAL (TQ) 772 KM 02-Aug-2026 10:01:50 HRS
Passenger Details
# Name Age Gender Booking Status Current Status
1. MADHURI 26 F CNF/B4/33/LOWER CNF /B4/33/LOWER
Acronyms: RLWL: REMOTE LOCATION WAITLIST PQWL: POOLED QUOTA WAITLIST RSWL: ROAD-SIDE WAITLIST
Transaction ID: 100006745112096
IR recovers only 57% of cost of travel on an average.
Payment Details
Ticket Fare ₹ 1,470.00
IRCTC Convenience Fee (Incl. of GST) ₹ 23.60
Travel Insurance Premium (Incl. of GST) ₹ 0.45
Total Fare (all inclusive) ₹ 1,494.05
PG Charges as applicable (Additional)
Beware of fraudulent customer care number. For any assistance, use only the IRCTC e-ticketing Customer care number:14646.
Customer Support (Outside India): Call: +91-8044647999 / +91-8035734999
```

### `app/src/test/resources/fixtures/ers_sample_ocr_rows.txt`
This simulates OCR plus RowMerger output: two-space column gaps, the image label missing, and typical OCR noise.
```
Electronic Reservation Slip (ERS)-Normal User
Booked From  To
SAINAGAR SHIRDI (SNSI)  SAINAGAR SHIRDI (SNSI)  BEGAMPET (BMT)
Start Date* 03-Aug-2026  Departure*17:00 03-Aug-2026  Arrival* 07:59 04-Aug-2026
PNR  Train No./Name  Class
8751652729  17001 / SNSI SC EXPRESS  THIRD AC (3A)
Quota  Distance  Booking Date
TATKAL (TQ)  772 KM  02-Aug-2026 10:01:50 HRS
Passenger Details
#  Name  Age  Gender  Booking Status  Current Status
1.  MADHURI  26  F  CNF/B4/33/LOWER  CNF / B4/33/LOWER
Acronyms:  RLWL: REMOTE LOCATION WAITLIST  PQWL: POOLED QUOTA WAITLIST  RSWL: ROAD-SIDE WAITLIST
Transaction ID: 1OOOO6745112096
IR recovers only 57% of cost of travel on an average.
Payment Details
Ticket Fare  < 1,470.00
IRCTC Convenience Fee (Incl. of GST)  < 23.60
Travel Insurance Premium (Incl. of GST)  < 0.45
Total Fare (all inclusive)  < 1,494.05
Customer Support (Outside India): Call: +91-8044647999 / +91-8035734999
```
Expected: the **same golden values as §2**. Note that the transaction ID `1OOOO6745112096` must normalize to `100006745112096`.

### `app/src/test/resources/fixtures/ers_multi_passenger.txt`
A synthetic return journey. It has a boarding station that differs from From, Arrival N.A., and mixed statuses.
```
Electronic Reservation Slip (ERS)-Normal User
Booked From Boarding At To
SECUNDERABAD JN (SC) LINGAMPALLI (LPI) SAINAGAR SHIRDI (SNSI)
Start Date* 10-Aug-2026 Departure* 21:40 10-Aug-2026 Arrival* N.A.
PNR Train No./Name Class
4523198760 17002 / SC SNSI EXPRESS SLEEPER CLASS (SL)
Quota Distance Booking Date
GENERAL (GN) 690 KM 15-Jun-2026 08:00:12 HRS
Passenger Details
# Name Age Gender Booking Status Current Status
1. RAVI KUMAR 34 M CNF/S7/49/SIDE UPPER CNF/S7/49/SIDE UPPER
2. ANITA 31 F RLWL/12 RLWL/5
3. ARJUN 8 M RAC/23 RAC/S2/71/SIDE LOWER
4. MEERA 60 F WL/40 CAN
Acronyms: RLWL: REMOTE LOCATION WAITLIST PQWL: POOLED QUOTA WAITLIST RSWL: ROAD-SIDE WAITLIST
Transaction ID: 100006799887766
Payment Details
Ticket Fare ₹ 2,140.00
Total Fare (all inclusive) ₹ 2,163.60
```

Expected values:

| Field | Expected |
|---|---|
| pnr | `4523198760` |
| train | `17002` / `SC SNSI EXPRESS` |
| from / boarding / to | from `SC` `SECUNDERABAD JN`; boarding `LPI` `LINGAMPALLI`; to `SNSI` |
| departure | `2026-08-10T21:40` IST |
| arrival | **null**, and **not** in `missingFields` |
| class / quota | `SL` / `SLEEPER CLASS`; `GN` / `GENERAL` |
| distanceKm | `690` |
| totalFarePaise | `216360` |
| passengers | 4 (see below) |

Passengers:
1. RAVI KUMAR, 34, M: current CNF, S7, 49, `SIDE UPPER`
2. ANITA, 31, F: booking `RLWL/12`; current RLWL with waitlistNumber 5
3. ARJUN, 8, M: booking `RAC/23`; current RAC, S2, 71, `SIDE LOWER`
4. MEERA, 60, F: booking `WL/40`; current CAN

---

## 6. Manual QA checklist (owner runs on device; copy to `docs/QA.md` in M6.4)

Put real PDFs in `samples/` (gitignored) and push them with `adb push samples/ /sdcard/Download/`.

1. Fresh install shows the empty state.
2. Share `madhuri_ticket.pdf` from the Files app to Trip Checker. The Import screen shows Reading, then Saved; the method is OCR.
3. Home shows a Next journey card with SNSI → BMT, Mon 3 Aug 17:00, `B4 · 33 · LOWER` (or it appears under Past if the date has passed).
4. Opening the trip shows every §2 golden value. The PNR copy button works.
5. **Go to ticket** opens the PDF in an external viewer. With no PDF app installed, the snackbar offers Share.
6. Re-share the same file → "Already imported".
7. Add manually: BMT → SNSI, 10 Aug 21:00, train 17002 → Home shows **one round-trip card**.
8. Unlink → two one-way cards. Reset to automatic → round trip again.
9. Link return: pick a one-way ticket → it becomes a manual round trip.
10. Import 5 or more PDFs at once from the picker → all are processed sequentially and the summary is correct.
11. Delete a ticket → it is gone, and its PDF is removed from app storage.
12. Rotate the screen during an import → progress continues.
13. Airplane mode → everything still works, including OCR (bundled model).
14. **Visual check against `design/screens/*.png`:** Home (with hero, needs review, upcoming, past), the Add ticket menu, the empty state, Import (mid-import), Trip detail (round trip, with the boarding-differs leg), the Link return sheet, and Review ticket (with missing fields). Layout, colour roles and copy match the mockups.
15. Switch the phone to **dark theme** → every screen above stays legible, with no hard-coded light colours.
16. Set the system font size to the largest → no clipped text; station codes and seat numbers don't wrap.
17. TalkBack → every icon-only button announces its label ("Copy PNR", "Share ticket PDF", "Trip options", …).

---

## 7. Decisions log
<!-- Append: YYYY-MM-DD · task · decision · reason -->
- 2026-10-11 · planning · Manual DI instead of Hilt · avoids annotation-processing risk with AGP 9 built-in Kotlin; the app is small.
- 2026-10-11 · planning · No Trip table; groups are derived from tickets plus `linkGroupId`/`autoPairDisabled` · a single source of truth and a pure, testable grouper.
- 2026-10-11 · planning · OCR fallback is mandatory · the owner's sample PDF ("Microsoft: Print To PDF") has no text layer.
- 2026-10-11 · planning · Auto-save complete parses and flag only incomplete ones for review · bulk-importing old tickets must not need N manual reviews.
- 2026-10-11 · M0.1 · KSP 2.3.4 for AGP 9 built-in Kotlin · kotlin.sourceSets error with 2.2.10-2.0.2.
- 2026-10-11 · M4.4 · TicketFileOperations interface for importer tests · avoids mocking Android Context in JVM tests.
- 2026-10-12 · M6.1 · Import staging in `cacheDir/staging` with legacy `staging-*` cleanup in `filesDir/tickets` · matches PLAN process-death requirement while keeping final PDFs in tickets dir.
- 2026-10-12 · M6 prep · lint.xml + lint block; PdfBox keep rules already in rules.keep; release optimization stays `false` until owner `assembleRelease` green · —
- 2026-10-12 · daemon JVM · `gradle-daemon-jvm.properties` toolchain 21 · foojay JDK 25 provision fails offline; Studio JBR 21 works.
- 2026-10-12 · code review · `gradle-daemon-jvm.properties` toolchain 25 · JDK 21 auto-download URL missing on Windows; Studio JBR is 25.
- 2026-10-12 · code review P0/P1 · Unique PDF names, explicit Room upsert, ImportManager without Uri, OCR/PdfBox hardening, fare BigDecimal · ImportManagerTest, TicketImporterTest, TripGrouper boundary, EditTicketMapperTest.
- 2026-10-13 · planning (MD) · The UI follows the owner's mockups in `design/` (PNG + HTML); new §3.9 screen spec, §3.10 tokens, milestone MD · owner request; replaces the earlier "dynamic colour, ElevatedCard, no custom fonts" style note.
- 2026-10-13 · planning (MD) · Brand indigo scheme (seed `#4355B9`) by default; `dynamicColor` kept but defaults to `false` · the mockups rely on specific roles (primaryContainer hero, tertiary review rows); wallpaper colours would break that look. Roles marked † in §3.10 are derived, not from the mockups.
- 2026-10-13 · planning (MD) · Use the system Roboto and `FontFamily.Monospace` instead of the mockups' Roboto Flex / Roboto Mono · offline app with no downloadable fonts, bundling variable fonts costs APK size, and visual parity is close enough. Weight 650 maps to SemiBold, and `font-stretch` is ignored.
- 2026-10-13 · planning (MD) · Home Search and More-options actions are hidden until M7.1 / M7.4–M7.5 give them a purpose · the mockup shows them, but empty menus are worse than no menu.
- 2026-10-13 · planning (MD) · The Link-return sheet changes from tap-to-link to select-then-"Link tickets"; Edit Save always saves (it keeps `needsReview` while required fields are missing) · matches the mockups and avoids accidental links and lost partial edits.
- 2026-10-13 · MD.2 · `DisplayText` lives in `domain/text` (not `ui/common`) and `StationClusters.isKnownCode` was added · `CityNames` (domain) uses it, so it can't depend on a ui package; the accessor fixes "PUNE" (its city key equals its code).
- 2026-10-13 · MD.2 · `formatCountdownIn` returns "now" under 1 minute; the old `formatCountdown` ("Today · 17:00") is kept for existing callers; `formatHomeSubtitle` keeps the date prefix with "No trips ahead"; `formatDayOffset` returns "+N" for any N ≥ 1 · gaps in the spec.
- 2026-10-13 · MD.4 · Hand-built FAB menu (scrim at 0.86 alpha, as in the mockup) and `LargeTopAppBar` with the subtitle as the first list item · material3 1.4.0 in this BOM has no public `FloatingActionButtonMenu`/`ToggleFloatingActionButton`, and `LargeFlexibleTopAppBar` is internal.
- 2026-10-13 · MD.4 · Home cards and hero show `from` codes with " · boards LPI" in the meta line; the highlighted DateBadge is the global next departure · matches the mockup.
- 2026-10-13 · MD.6 · Import rows use "d MMM" dates ("3 Aug") and uppercase codes; the back arrow doesn't clear finished items, Done does; a plain M3 `LinearProgressIndicator` (track `secondaryContainer`) · matches the mockup closely enough without a custom bar.
- 2026-10-13 · MD.7 · Class/quota and duration chips use the shared 28 dp `TagChip`; a cancelled leg shows a small "Cancelled" chip instead of strikethrough; no `StayConnector` when a leg lacks dates; "Likely return" requires non-blank city keys · consistency and a bug fix (two null stations used to match).
- 2026-10-13 · MD.9 · Save is never blocked: it keeps `needsReview = true` until the required fields are filled. In create mode the error styling appears only after the first save attempt. The start date is no longer a separate field (it is derived from the departure date). The ViewModel gets an optional `TicketFileStore` for the banner's "Open PDF" · per §3.9; avoids an all-red empty form.
- 2026-10-13 · MD.10 · Removed 47 string resources the redesign left unused · keep `strings.xml` honest; per-screen strings live in `strings_<area>.xml`.
- 2026-10-13 · MD.10 · On-emulator review found and fixed: a clipped "Boarding" label (the code field is now 120 dp), a `TimePicker` wrongly inside `DatePickerDialog` (now material3 `TimePickerDialog`), and `DateBadge` clipping at 1.3× font (now a minimum size, not fixed) · visual QA on Medium_Phone API 36.
- 2026-10-13 · tooling · Gradle on this machine needs a short TEMP (e.g. `E:\androidApps\.gtmp`): the 8.3 temp path from the user name with a space breaks Java's Unix-domain-socket pipe ("Unable to establish loopback connection") · environment workaround, no project change.
- 2026-10-13 · M6.3 · Release keeps `optimization { enable = true }` (R8 code + resource shrinking, full mode) · verified: 206 unit tests, lint, and an emulator smoke test of the release APK (cold start, text-layer import via PdfBox, vector-only import via PdfRenderer + ML Kit OCR, round-trip pairing). Details: `docs/RELEASE_OPTIMIZATION.md`.
- 2026-10-13 · M6.3 · Removed `-keep class com.tom_roush.pdfbox.** { *; }`, `-keep class org.apache.pdfbox.** { *; }` and `-dontwarn org.bouncycastle.**`. The r8-analyzer showed the PdfBox rule pinning 9.3% of live code and subsuming PdfBox's own consumer rules; the Apache rule matched nothing; R8 reports no BouncyCastle missing classes. PdfBox resources load through absolute asset paths, so they're safe under obfuscation. Kept the targeted `-dontwarn com.gemalto.jp2.**` (JPXFilter has 6 direct references to that optional, absent dependency).
- 2026-10-13 · M6.3 · Added `-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }` · in R8 full mode, firebase-components' consumer rule keeps the ML Kit registrar classes but not their no-arg constructors, so MlKitInitProvider hit NoSuchMethodException and OCR silently returned nothing. Found by the release smoke test; covers 3 methods.
- 2026-10-13 · M6.3 · `packaging.resources.excludes += "org/bouncycastle/pqc/**"` (4.14 MB of Picnic/SIKE tables pulled in by pdfbox-android → bcprov) · PdfBox never uses post-quantum crypto; `mapping.txt` shows no PQC class survives R8. Certificate-encrypted PDFs (PdfBox `PublicKeySecurityHandler` → reflective BouncyCastleProvider) aren't covered by keep rules: such PDFs fail extraction gracefully (needs review). IRCTC tickets use standard/no encryption, which runs on the platform JCA.
- 2026-10-13 · M6.3 · ABI splits for APK builds (arm64-v8a, armeabi-v7a, x86_64, x86) plus a universal APK; disabled for `bundle*` tasks (AGP can't bundle with APK splits on, issuetracker 402800800) · ~90% of the native payload is ML Kit's bundled OCR library once per ABI; per-ABI APKs are 14.7–19.6 MB vs 49.1 MB universal. No device loses support. Output names are now `app-<abi|universal>-<buildType>[-unsigned].apk`.

## 8. Blockers
<!-- Append: YYYY-MM-DD · task · what is blocked · what you tried · what's needed -->
- 2026-10-12 · verify · Agent CI network · Google Maven / Gradle wrapper download blocked in agent sandbox · Owner: run `scripts\verify.ps1` or Studio Terminal; install SDK platform android-37 if needed.

## 9. Progress log
<!-- Append one line per finished task: YYYY-MM-DD HH:MM · task id · summary · tests added -->
- 2026-10-11 22:30 · M0–M4.4 · Project setup, parser/grouping/data/extraction/importer on branch feature/plan-implementation · IrctcErsParserTest, TripGrouperTest, MappersTest, TicketImporterTest, etc.
- 2026-10-11 22:45 · M4.5–M5.4 · ImportManager, navigation, Home/Import MVVM, Trip detail stub · FormattersTest
- 2026-10-12 09:45 · M6.1 · Staging under cacheDir/staging, startup cleanup, empty-file handling, ImportManager failure cleanup · StagingCleanupTest, ImportManagerTest, PdfValidationTest
- 2026-10-12 09:45 · M6.4 · docs/QA.md + README.md build prerequisites · —
- 2026-10-12 09:45 · M6.3 partial · PdfBox R8 keep/dontwarn rules in rules.keep · assembleRelease not run (Gradle download blocked)
- 2026-10-12 12:00 · M5.5–M5.8 · Trip detail, edit ticket, TicketActions, HomeUiMapper/LinkReturnCandidates tests · HomeUiMapperTest, LinkReturnCandidatesTest, EditTicketMapperTest, ImportManagerTest
- 2026-10-12 12:15 · M6.5 · Compose previews, KDoc pass, verify.ps1, lint.xml, TripDetail snackbar fix · —
- 2026-10-12 18:50 · code review · P0 import/lint fixes; gate green (`assembleDebug testDebugUnitTest lintDebug`) · ImportManagerTest, TicketImporterTest, TripGrouperTest, EditTicketMapperTest
- 2026-10-12 19:00 · code review · P1-7–P1-17 (ticker, orphan PDF reconcile, parser/edit/home PDF UX) · IrctcErsParserTest, JourneyRefresh
- 2026-10-13 18:00 · MD.1–MD.9 · M3 redesign from design/ mockups: brand theme, type/shape tokens, shared components, Home/Import/Trip detail/Link sheet/Edit screens; gate green (`assembleDebug testDebugUnitTest lintDebug`, 206 tests) · StatusToneTest, DisplayTextTest, CityNamesTest, FormattersTest+, HomeUiMapperTest+, ImportRowTextTest, TripDetailUiMapperTest, LinkReturnCandidatesTest+, StatusReadbackTest, EditTicketUiStateTest
- 2026-10-13 18:30 · MD.10 · Unused strings removed, QA.md visual checks, emulator pass (light/dark/1.3× font; Home, FAB menu, empty state, Import, Trip detail, Link sheet, Edit) with 3 UI fixes; gate green, 206 tests · —
- 2026-10-13 19:30 · M6.3 · Release optimization: R8 + resource shrinking on, keep rules refined via r8-analyzer, ML Kit registrar fix, BC PQC resources excluded, ABI splits + universal; universal 83.3 MB → 49.1 MB, arm64 19.0 MB; emulator smoke test of release passed · —
