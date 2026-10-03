# Kanakku (கணக்கு)

**A free income & expense book for everyone** — self-employed people, small shopkeepers and homemakers.

Made by an ordinary middle-class man who wanted a simple, honest way to keep accounts. No ads. No sign-up. No servers. **Your data never leaves your phone** unless you back it up yourself. The optional AI advisor sends only a short summary of totals (no names or notes) after you sign in with a free Puter account.

🔗 **Live app:** https://nikkumeow570-ctrl.github.io/kanakku/

## தமிழில்
கணக்கு — அனைவருக்கும் இலவசம். சுயதொழில் செய்வோர், சிறு கடைக்காரர்கள், இல்லத்தரசிகள் வரவு-செலவை எளிமையாக எழுத ஒரு சாதாரண நடுத்தரக் குடும்பத்து மனிதரால் உருவாக்கப்பட்டது. விளம்பரம் இல்லை, பதிவு இல்லை, உங்கள் தரவு உங்கள் போனிலேயே.

## Features
- English and தமிழ் (one-tap language toggle)
- Income & expense entries with categories, edit, delete and **undo**
- One-tap quick-add (milk, vegetables, gas, sales...)
- Monthly dashboard with expense donut chart
- **Udhaar / கடன் ledger** — who owes you, whom you owe
- Monthly budgets per category with overspend warning
- Your own custom income & expense categories
- Mobile-first layout with bottom navigation
- Daily reminder to log expenses
- Optional **AI advisor** (sign in with a free Puter account; sends only category totals)
- Backup to Gmail (share sheet), CSV / JSON export, JSON import (asks before replacing data)
- **Recurring entries** (rent, salary, EMIs) added automatically each month
- **Monthly report** with 6-month trend — save as PDF or share the summary
- **Voice entry** (English / தமிழ்) that fills the form for you to confirm
- **Google Drive backup / restore** (your own private app folder)
- Daily reminder + one-tap **Calendar reminder** that works even when the app is closed
- **Insights** — month-end outlook and safe-to-spend per day, changes vs your usual, unusual expenses, double-entry finder, budget and recurring suggestions (all calculated on your phone)
- Quick-add chips learned from your own most frequent entries; category hint while typing a note
- Optional PIN lock (a screen lock only; backups are not encrypted)
- Paise-accurate amounts, all-time balance, savings rate, part-payments on udhaar
- Installable PWA, works offline, light/dark theme

## Install on your phone
Open the live link in Chrome and tap **Install app** (or Menu → *Add to Home screen*).

## Your data
Everything is stored in your browser's local storage on your device. Clearing browser data erases it, so use **Data → Backup to Gmail** regularly (the app reminds you every 30 days).

## Google Drive backup setup (one time)
1. In Google Cloud Console create a project and enable the **Google Drive API**.
2. Configure the OAuth consent screen (External, Testing) and add your own Google account as a test user.
3. Create an **OAuth client ID** of type **Web application** and add `https://<your-username>.github.io` as an authorized JavaScript origin.
4. Paste the Client ID into **More → Backup & Settings → Google Drive backup**.

## Privacy
Entries, budgets and udhaar stay on your device. Nothing is sent anywhere unless you back up (Gmail / Drive), use voice entry (your browser's speech service) or use the AI tab, which sends only category totals to Puter after you sign in. No ads, no tracking.

## Deploy your own copy
It is plain static files (`index.html`, `sw.js`, `manifest.json`, icons). Push to a GitHub repo and enable **Settings → Pages → Deploy from branch → main / root**.

## Roadmap
Push notifications that need a server, more languages, a shared book for couples

## Feedback
Open an issue on this repository — suggestions and Tamil wording fixes are very welcome.

## v4l — redesign
Peacock/kumkum palette (WCAG AA checked, light+dark), balance hero with spent-of-income bar, ranked category bars instead of a donut, floating glass bottom nav (glass only on navigation, with reduced-transparency fallback), SVG icons, large amount entry with sticky Save, 44–48px touch targets, focus rings, reduced-motion support.

## v4m — Udhaar fixes
Partial-payment dialog (no more prompt(); "1,000" parses correctly), un-settling a fully paid item restores the balance, delete has Undo, empty-save shows a hint, unique ids on import, accessible action buttons, long names no longer crush the row, ↓/↑ glyphs only on Income/Expense and Udhaar toggles.
