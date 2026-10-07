# Kanakku — manual test checklist (run on a real phone before sharing)

Automated checks (33) cover the logic only. These need a human and a phone.

## P0 — must pass
1. Install: open the live link in Chrome, tap **Install app**; icon shows the Tamil க; opens full-screen.
2. Offline: turn on airplane mode, open the app; entries still show and you can add one.
3. Add income and expense (with paise, e.g. 1234.50); Home totals and "Net this month" match by hand.
4. Edit and delete an entry; **Undo** restores it in the same place.
5. Android **Back** button goes History → Home, and only exits from Home.
6. Backup to Gmail: file arrives; **Erase all data**; import the file; everything returns.
7. Language toggle: every screen in Tamil, no cut-off text, ₹ amounts correct.
8. PIN: set, close the app for 1+ minute, reopen → asks for PIN; wrong PIN is refused; PIN digits are hidden.
9. Two windows: open the app and the browser tab; add an entry in one; the other shows it after you switch back.
10. Update: after pushing a new version, the "New version ready — Reload" bar appears.

## P1 — should pass
11. Recurring: add one for today; it appears once, and not again after reopening.
12. Udhaar: add, part-pay, mark settled; totals update.
13. Monthly report: check the numbers, then **Save as PDF** — Tamil text is readable, no menus in the PDF.
14. Voice (Chrome): say "milk 60" and "பால் ஐநூறு"; the form fills but does not save by itself.
15. Calendar reminder: add it, then see it ring with the app closed.
16. Drive: paste your Client ID, back up, restore (one-time setup in the README).
17. Insights (needs ~2 months of entries): Home shows the month-end outlook; More → Insights opens; "Remove duplicate" and "Make recurring" work and can be undone/deleted.
18. Type a note you used before (e.g. "milk"): a category/amount hint appears; tapping it fills the form.
19. Also try: Samsung Internet, an iPhone (Add to Home Screen), a low-end Android, large font size in phone settings.

## Known limits (by design)
Data lives on each phone only; the PIN is a screen lock, not encryption; background reminders are best-effort; no sync between phones.

## v5g — UPI collect (verified in Chromium 390×844)
- Offline QR encoder (byte mode, ECC M, v1–10) decoded back with OpenCV for 16 payload sizes incl. Tamil UTF-8.
- Invalid UPI ID rejected; save/edit; owing-customer chips prefill name+amount.
- "Received" → FIFO applies to that customer's udhaar (real income entries), overpay → extra income, walk-in → Business Income; Undo restores both.
- Spoken confirmation EN (en-IN) / TA (ta-IN); WhatsApp Remind includes UPI ID + pay link; "UPI QR" button on each customer.
- Kanakku never touches money; "Received" is a manual tap by design.

## v5h — Auto-confirm payments (Android app)
- `android-build/native/java/PaymentParser.java` unit-tested with JDK: GPay / PhonePe / Paytm / BHIM notifications, bank credit SMS (HDFC/SBI styles, balance-first), Tamil names; rejects debits, requests, failures, OTPs, cashback, "will be credited". Cross-source de-dup (app alert + bank SMS within 2 min).
- Native classes compile-checked against API stubs; **real-device test still needed** (enable Notification access → ₹1 test → real ₹1 payment).
- Web side tested with a mocked plugin: open-QR amount match, udhaar match by payer name, walk-in income, duplicate ids ignored, "ask first" mode with Add/Ignore, startup queue drain + ack, EN/TA, browser fallback.
