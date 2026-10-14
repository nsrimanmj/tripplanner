# Trip Checker — manual QA

**To be run by the owner on a physical device or emulator.**  
Place real ticket PDFs in `samples/` (gitignored). Optional: `adb push samples/ /sdcard/Download/`.

## Prerequisites on device

- Android **API 28+**
- A PDF viewer app installed (for “Go to ticket”; test fallback without one in step 5)
- Airplane mode available (step 13)

## Checklist

1. Fresh install shows the empty state.
2. Share `madhuri_ticket.pdf` from the Files app to Trip Checker. The Import screen shows Reading, then Saved; the method is OCR.
3. Home shows a Next journey card with SNSI → BMT, Mon 3 Aug 17:00, `B4 · 33 · LOWER` (or it appears under Past if the date has passed).
4. Opening the trip shows every §2 golden value from `docs/PLAN.md`. The PNR copy button works.
5. **Go to ticket** opens the PDF in an external viewer. With no PDF app installed, the snackbar offers Share.
6. Re-share the same file → “Already imported”.
7. Add manually: BMT → SNSI, 10 Aug 21:00, train 17002 → Home shows **one round-trip card**.
8. Unlink → two one-way cards. Reset to automatic → round trip again.
9. Link return: pick a one-way ticket → it becomes a manual round trip.
10. Import 5 or more PDFs at once from the picker → all are processed sequentially and the summary is correct.
11. Delete a ticket → it is gone, and its PDF is removed from app storage.
12. Rotate the screen during an import → progress continues.
13. Airplane mode → everything still works, including OCR (bundled model).

## Edge cases (M6)

- Share a `.txt` or image renamed as PDF → Import row shows **Failed** with “Not a PDF” (or “Empty file” for 0-byte).
- Share a corrupt or encrypted PDF → **Needs review** or **Failed**, app does not crash.
- Force-stop the app during import, reopen → no orphan files left under cache staging; import queue may reset but DB stays consistent.

## Visual design (MD)

14. **Visual check against `design/screens/*.png`:** Home (with hero, needs review, upcoming, past) in `Home.png`, the Add ticket menu in `Home — Add ticket menu.png`, the empty state in `Home — Empty state.png`, Import (mid-import) in `Import.png`, Trip detail (round trip, with the boarding-differs leg) in `Trip detail — Round trip.png`, the Link return sheet in `Link return sheet.png`, and Review ticket (with missing fields) in `Review Edit ticket.png`. Layout, colour roles and copy match the mockups.
15. Switch the phone to **dark theme** — every screen above stays legible, with no hard-coded light colours.
16. Set the system font size to the largest — no clipped text; station codes and seat numbers don't wrap.
17. TalkBack — every icon-only button announces its label ("Copy PNR", "Share ticket PDF", "Trip options", …).

## Home swipe-to-delete (M7.6)

18. On Home, swipe a **needs-review** row, a **one-way** upcoming card, or a **one-way** past row left (end-to-start). A red delete background appears; releasing past the threshold opens the same delete confirmation as Trip detail.
19. Tap **Cancel** or press Back — the row stays, data unchanged.
20. Confirm **Delete** — the ticket disappears from Home and its PDF is removed (same as checklist step 11).
21. **Round-trip** and **custom** upcoming cards and past rows do **not** swipe; delete those legs from Trip detail.
22. With TalkBack, use the custom **Delete** action on a swipeable row when swipe is hard to perform.
