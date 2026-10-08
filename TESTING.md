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

## v5l — Staff-phone soundbox + fixes
- Owner phone publishes each detected/recorded payment to a private ntfy.sh topic, AES-GCM encrypted with a key derived from a 12-char pairing code (topic and key both hashed from the code). Wrong code cannot decrypt; no plaintext in the POST.
- Staff phone: "Soundbox" screen (EventSource, wake lock + native keep-awake, resumes after app restart), speaks via native TTS (PL.speak) or Web Speech; duplicate message ids ignored; EN/TA.
- One publish per payment in both "auto" and "ask first" modes. Verified with mocked fetch/EventSource (ntfy.sh unreachable from the build sandbox) — **needs a two-phone real-world test**.
- Known limit: the staff phone must keep Kanakku open in the foreground (no background service yet). Public ntfy.sh server sees only encrypted blobs + timing.
- Collect screen: extra bottom space so buttons are not hidden by the nav bar.

## v5m
- Alerts from any app whose package name looks like a payment/bank app are now read; money-in alerts from unlisted apps are logged locally under "Not detected? Recent alerts" as [unlisted] for diagnosis.

## v6 — Listener health & reliability
- Native: listener connected/disconnected flag + auto-rebind, heartbeat (last alert / app / last payment), real self-test (Kanakku posts a silent notification and verifies its own listener heard it), keep-alive foreground service (specialUse), battery-optimisation + OEM auto-start shortcuts, POST_NOTIFICATIONS request.
- apply-native.js adds permissions + services idempotently (verified on a mock manifest; XML valid). Native classes compile-checked against API stubs — **not run on a real device**.
- Removed two unreferenced files (RelayCodec.java, SseClient.java) found in android-build/native/java that were not part of any build step.
- Web: health card, test/keep-alive/battery buttons, EN/TA; mocked-plugin tests incl. failure paths.

## v6.1
- UPI is now its own bottom-nav tab with three sections (QR / Auto-confirm / Staff). Removed from the More menu. Verified at 360px in EN and TA; all earlier tests re-run after the change.

## v6.2 — Payment apps first, automatic daily backup
- Bank SMS (messaging apps) is held 75 s; if a payment-app alert for the same amount arrived within 3 min, the SMS is dropped. Unit-tested (PaymentParser.appAlertSeen / isSmsApp) plus all earlier parser tests.
- Trade-off: SMS-only payments (no payment-app alert) speak ~75 s after arrival.
- Automatic backup: once per day while Kanakku is open (and on resume), writes Download/Kanakku/kanakku-auto-YYYY-MM-DD.json via MediaStore (no storage permission, Android 10+), keeps newest 7. Toggle + "Back up now" + status in Backup & Settings. Web-only browser shows the existing manual buttons.
- Verified in Chromium with a mocked native plugin: saves on open, once per day, off switch, failure message. Native BackupWriter compile-checked against stubs only — **not run on a device**.
- Known gap: the staff-phone relay is still published from the web page, so it only sends while Kanakku is open on the owner's phone.
