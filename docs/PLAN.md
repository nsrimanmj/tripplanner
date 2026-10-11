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
ui/home/HomeScreen.kt, HomeViewModel.kt, components/{NextJourneyCard, RoundTripCard, OneWayCard, NeedsReviewRow}.kt
ui/importflow/ImportScreen.kt, ImportViewModel.kt
ui/trip/TripDetailScreen.kt, TripDetailViewModel.kt, components/{LegSection, PassengerRow, LinkTicketSheet}.kt
ui/edit/EditTicketScreen.kt, EditTicketViewModel.kt
ui/common/TicketActions.kt         openPdf(context, ticket), sharePdf(...)
ui/common/Formatters.kt            IST date/time, duration, countdown, money
ui/theme/                          (existing)
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

**Routes:** `Home`, `Import`, `TripDetail(anchorTicketId: String)`, `EditTicket(ticketId: String?)` (`null` = create manually).

**TripDetail** shows *the group that contains `anchorTicketId`*. This stays stable even if regrouping changes the keys. If the ticket was deleted, pop back.

**Home** (`HomeViewModel` → `StateFlow<HomeUiState>`; re-evaluates "now" every 60 s via a ticker flow):
- Top app bar titled "Trip Checker".
- **Next journey hero card:** the earliest leg with `departureAt > now` across all groups. It shows:
  - a countdown: "in 2d 4h"; "Today · 17:00"; or "Tomorrow · 17:00"
  - `17001 SNSI SC EXPRESS`
  - `SNSI → BMT`
  - departure time
  - **big coach and berth** (`B4 · 33 · LOWER`; for WL show `WL 12`)
  - a **Go to ticket** button
- **Needs review** section, only shown if non-empty: rows labelled "Ticket from <originalFileName> — tap to fill missing details". Tapping opens `EditTicket(id)`.
- Tickets with `needsReview` appear **only** in the Needs review section. Filter them out of the groups below and out of the hero card.
- **Upcoming** section: groups with UPCOMING or IN_PROGRESS status.
  - `RoundTripCard`: two stacked legs, each with route, date and time, train no and coach/berth; plus chips "Round trip" and "In progress" when applicable.
  - `OneWayCard`: one leg plus a muted "No return yet" hint.
  - `CUSTOM`: all legs stacked.
  - Cancelled tickets: struck-through text with a "Cancelled" chip.
- **Past** section: grouped under month headers ("August 2026"), most recent first.
- **Empty state:** an illustration or icon, the text "Import your IRCTC ticket PDFs", and a button.
- **FAB** (extended, "Add ticket") opens a small menu with "Import PDFs" and "Add manually".
- Tapping a card opens `TripDetail(anchorTicketId = first leg id)`.

**Import screen:**
- A list of `ImportItem` rows: name, a state icon or progress indicator, and the state text.
  - Tap a `Saved` or `Updated` row to open TripDetail.
  - Tap a `NeedsReview` row to open EditTicket.
- A "Done" button that clears finished items and returns Home.
- Show a running summary such as "3 saved · 1 duplicate · 1 needs review".

**Trip detail:**
- **Header:** kind chip ("Round trip", "One way" or "Custom"), date range, and status.
- **Per leg** (`LegSection`):
  - Train no and name.
  - Route block: From → To with names, plus "Boarding at X" when the boarding station differs from From.
  - Departure and arrival in IST, with duration ("14h 59m").
  - Class and quota chips, plus distance.
  - PNR in a monospace style with a **copy** button (`ClipboardManager`, then a snackbar "PNR copied").
  - Passengers (`PassengerRow`): name, age and gender; the **current status shown prominently**; the booking status muted.
  - Total fare.
  - Buttons:
    - **Go to ticket** (primary; hidden when `pdfFileName == null`)
    - Share PDF
    - Edit
    - overflow menu: Mark as cancelled/active, Delete (`AlertDialog` confirm; deletes the row and the PDF)
- **Group actions:**
  - If ROUND_TRIP or CUSTOM: "Unlink". This sets `autoPairDisabled = true` and `linkGroupId = null` on all legs.
  - If ONE_WAY: "Link return ticket…". This opens a `ModalBottomSheet` listing other ONE_WAY tickets, sorted by closeness in time and with likely returns (reverse city route) first. Selecting one sets the same new UUID `linkGroupId` and `autoPairDisabled = false` on both tickets.
  - If any leg is manual or has auto-pairing disabled: "Reset to automatic pairing". This clears `linkGroupId` and `autoPairDisabled` on all legs.
- Show an "Extracted via OCR — double-check details" hint when `extractionMethod == OCR`.

**Edit ticket** (also used for manual create):
- A text field per field.
  - Station inputs are a code plus a name; the code is uppercased.
  - Dates use the M3 `DatePickerDialog`. **Gotcha:** the picker returns UTC-midnight millis, so convert with `ZoneOffset.UTC` to a `LocalDate`.
  - Times use an M3 `TimePicker` in a dialog.
  - Class and quota are free text.
- Passenger list editor: add, remove, and edit name/age/gender/current status.
- Required fields that are missing are highlighted with an error colour and helper text.
- Save writes the ticket. It sets `needsReview = false` when all required fields are present, sets `updatedAt`, and pops back.
- In create mode: `extractionMethod = MANUAL` and `pdfFileName = null`.

**Go to ticket and Share** (`ui/common/TicketActions.kt`):
- Manifest `<provider androidx.core.content.FileProvider authorities="${applicationId}.fileprovider" exported="false" grantUriPermissions="true">` with `res/xml/file_paths.xml` → `<files-path name="tickets" path="tickets/"/>`.
- Open: `Intent(ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK)`. Catch `ActivityNotFoundException` → snackbar "No PDF app found" with a **Share** action.
- Share: `ACTION_SEND`, type `application/pdf`, `EXTRA_STREAM`, plus the grant flag, wrapped in `Intent.createChooser`.

**Formatting** (`Formatters.kt`, all in IST, `Locale.ENGLISH`):
- Date: `EEE, d MMM` → "Mon, 3 Aug"
- Time: `HH:mm`
- Month header: `MMMM yyyy`
- Duration: "14h 59m"
- Money: "₹1,494.05", using `NumberFormat` for `en-IN`

**Visual style:** Material 3 with the existing dynamic colour theme. Use `ElevatedCard`s. Add a small train glyph and a route arrow between station codes. Station codes are large (titleLarge, bold) with the names small below. Keep it clean; no custom fonts.

**Icons:** Material3 ≥1.4 no longer pulls in icons transitively. Add `androidx.compose.material:material-icons-core` (version managed by the BOM) for the basic `Icons.Default.*`. For anything else (train, link, link_off, content_copy, open_in_new), add **vector drawables** from Material Symbols under `res/drawable/`. **Do not add `material-icons-extended`**; it is huge.

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
- [ ] **M4.5** `ImportManager` with phase A copying immediately and phase B sequential. `StateFlow` items. JVM test with fakes and `StandardTestDispatcher`.

### M5: UI
- [ ] **M5.1** `Routes` (`@Serializable`), `AppNavHost`, and `MainActivity`, with intent handling (share, open-with) → enqueue → navigate to Import.
- [ ] **M5.2** `Formatters` with JVM tests (IST formatting, duration, countdown strings, money "₹1,494.05").
- [ ] **M5.3** Home screen and `HomeViewModel`: next-journey hero, needs-review, upcoming and past by month, empty state, and the FAB menu wired to `OpenMultipleDocuments`. Add `@Preview`s with fake data for every card.
- [ ] **M5.4** Import screen and ViewModel.
- [ ] **M5.5** Trip detail screen and ViewModel: legs, passengers, PNR copy, Go to ticket, Share, Edit, Cancel toggle, Delete confirm, Unlink, Link-return sheet, and Reset to automatic.
- [ ] **M5.6** Edit ticket screen and ViewModel: edit and create modes, the passenger editor, date and time pickers, and validation.
- [ ] **M5.7** `TicketActions`: open the PDF externally (with the ActivityNotFound fallback) and share.
- [ ] **M5.8** ViewModel unit tests for the Home and Trip detail state mapping, using a fake repository and a fixed clock.

### M6: Hardening
- [ ] **M6.1** Edge cases:
  - an encrypted, corrupt or 0-byte PDF → Failed or NeedsReview, never a crash
  - a non-PDF shared file → Failed("Not a PDF")
  - 20 files imported at once → still responsive
  - rotation during import → state survives (it is app-scoped)
  - process death → the DB is consistent and no orphan staged files are left (clean `cacheDir/staging` at app start)
- [ ] **M6.2** `.\gradlew.bat lintDebug`: fix all errors and the obvious warnings (hardcoded strings may stay for now).
- [ ] **M6.3** R8 check. Temporarily set release `optimization { enable = true }` and run `.\gradlew.bat assembleRelease`. Add any keep rules PdfBox-Android needs (e.g. `-dontwarn com.gemalto.jp2.**`, `-dontwarn org.bouncycastle.**`, and keep rules for `com.tom_roush.pdfbox` resources if they are missing at runtime) to `app/src/main/keepRules/rules.keep`. Then set it back to `false`, unless the build works and you are confident, in which case log the decision.
- [ ] **M6.4** Write `docs/QA.md` from the §6 checklist, marked "to be run by owner on device".
- [ ] **M6.5** Final pass:
  - no TODOs or stubs left in M0–M6 code
  - every public class has a one-line KDoc
  - the `README.md` has a short description and the build/run steps

### M7: Stretch (only after M0–M6 are all ticked)
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

---

## 7. Decisions log
<!-- Append: YYYY-MM-DD · task · decision · reason -->
- 2026-10-11 · planning · Manual DI instead of Hilt · avoids annotation-processing risk with AGP 9 built-in Kotlin; the app is small.
- 2026-10-11 · planning · No Trip table; groups are derived from tickets plus `linkGroupId`/`autoPairDisabled` · a single source of truth and a pure, testable grouper.
- 2026-10-11 · planning · OCR fallback is mandatory · the owner's sample PDF ("Microsoft: Print To PDF") has no text layer.
- 2026-10-11 · planning · Auto-save complete parses and flag only incomplete ones for review · bulk-importing old tickets must not need N manual reviews.
- 2026-10-11 · M0.1 · KSP 2.3.4 for AGP 9 built-in Kotlin · kotlin.sourceSets error with 2.2.10-2.0.2.
- 2026-10-11 · M4.4 · TicketFileOperations interface for importer tests · avoids mocking Android Context in JVM tests.

## 8. Blockers
<!-- Append: YYYY-MM-DD · task · what is blocked · what you tried · what's needed -->

## 9. Progress log
<!-- Append one line per finished task: YYYY-MM-DD HH:MM · task id · summary · tests added -->
- 2026-10-11 22:30 · M0–M4.4 · Project setup, parser/grouping/data/extraction/importer on branch feature/plan-implementation · IrctcErsParserTest, TripGrouperTest, MappersTest, TicketImporterTest, etc.
