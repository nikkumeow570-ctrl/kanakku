# How the Insights were validated

Everything runs on the phone; no model, no server. Methods are simple on purpose: a personal book has tens to hundreds of
entries, so robust statistics beat anything heavier.

| Feature | Method |
|---|---|
| Month-end outlook | spent so far + still-due fixed items + shrunk daily run-rate (weight = day/(day+30) on this month, rest on your last 3 months). Fixed items come from your Recurring list, or are detected from history. |
| Unusual expenses | robust z-score (median/MAD) against the same habitual item or category; needs 7+ entries, ≥3× usual, ≥₹300 |
| Double entries | identical type/category/amount/date/note saved within 2 minutes |
| Recurring / budget suggestions | same item in 3+ of the last 6 months within ±3 days; budget = 105% of the higher of mean/median of your last 3 months |

## Backtest (synthetic households, seeded; thresholds tuned on seeds 1–30, results below on held-out seeds 101–130)
3 personas (homemaker, shopkeeper, salaried) × 30 seeds, 14 months each.

* Outlook error (MAPE): day 5 / 10 / 15 / 20 / 25 = 10.3 / 9.9 / 7.9 / 6.3 / 4.2 %. Using only the last 3 months' average gives about 10.1 % on every day, and projecting this month's pace alone gives 163 / 101 / 58 / 30 / 13 %. So early in the month it is no better than your usual average; it improves as the month goes on. With no Recurring list set it is 15 / 13 / 10 / 7 / 4.5 %.
* Unusual expenses: 76 % of injected 4× spikes found (85 % at 6×, 62 % at 3×), about 1 false alert per user per 45 days. Most false alerts are genuine tail events (a big medical bill), not errors.
* Double entries: 100 % found, 0 false alarms (including same-day legitimate repeats).
* Recurring suggestions: 100 % recall, 98 % precision (3 suggestions per user).

## Limits
Synthetic data is cleaner and more stationary than real life, so real accuracy will be lower — treat the numbers as a floor for logic bugs, not a promise. Category hints scored 100 % only because synthetic notes are clean; real accuracy is unknown, so hints are always tap-to-apply, never automatic.
