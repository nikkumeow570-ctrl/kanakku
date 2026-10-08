package app.kanakku;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java (no Android classes) so it can be unit-tested. Reads a notification's text and decides
 *  whether it is an incoming UPI/bank credit. Nothing leaves the phone. */
public final class PaymentParser {
    public static final class Payment {
        public final double amt; public final String who;
        public Payment(double amt, String who) { this.amt = amt; this.who = who; }
    }
    public static final class Seen {
        public final double amt; public final String pkg, key; public final long ts;
        public Seen(double amt, String pkg, String key, long ts) { this.amt = amt; this.pkg = pkg; this.key = key; this.ts = ts; }
    }

    /** Payment apps + common SMS apps. Bank credit SMS arrive via the SMS app's notification. */
    private static final String[] PKGS = {
        "com.google.android.apps.nbu.paisa.user", "com.google.android.apps.nbu.paisa.merchant",
        "com.phonepe.app", "com.phonepe.app.business", "com.phonepe.psp.merchant",
        "net.one97.paytm", "com.paytm.business", "in.org.npci.upiapp", "in.amazon.mShop.android.shopping",
        "com.bharatpe.app", "com.mobikwik_new", "com.freecharge.android", "com.dreamplug.androidapp",
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
        "com.android.messaging", "com.oneplus.mms", "com.miui.mms", "com.coloros.mms", "com.oplus.mms", "com.vivo.mms", "com.truecaller", "com.microsoft.android.smsorganizer"
    };
    public static boolean isAllowed(String pkg) {
        if (pkg == null) return false;
        for (String p : PKGS) if (p.equals(pkg)) return true;
        String l = pkg.toLowerCase(Locale.ROOT);
        for (String k : new String[]{"paytm", "phonepe", "paisa", "upi", "bhim", "bharatpe", "mobikwik", "razorpay", "cred.club", "bank"}) if (l.contains(k)) return true;
        return false;
    }

    private static final int CI = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern AMT = Pattern.compile("(?:\\u20B9|(?<![A-Za-z])(?:rs\\.?|inr))\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", CI);
    private static final Pattern CREDIT = Pattern.compile("\\b(received|credited|paid you|sent you|deposited)\\b", CI);
    private static final Pattern REJECT = Pattern.compile(
        "\\b(debited|withdrawn|spent|sent to|paid to|you paid|you sent|you have paid|you have sent|requested|requests|request|failed|declined|reversed|refund|cashback|reward|rewards|offer|expired|will be|to be credited|otp)\\b", CI);
    private static final Pattern PAID_YOU = Pattern.compile("([A-Za-z][A-Za-z0-9 .'&@_-]{1,40}?)\\s+(?:has\\s+)?(?:paid|sent)\\s+you", CI);
    private static final Pattern FROM = Pattern.compile(
        "\\bfrom\\s+(?:vpa\\s+|a/c\\s+)?([^\\s,;()]+(?:\\s+[^\\s,;()]+){0,3}?)(?=\\s+(?:on|via|using|through|at|ref|upi|to|in|for|a/c|ac|utr|txn|imps)\\b|\\s*[,;()\\n]|\\s*$)", CI);

    public static Payment parse(String pkg, String... parts) {
        StringBuilder sb = new StringBuilder();
        List<String> seen = new ArrayList<>();
        for (String p : parts) { if (p == null) continue; String q = p.trim(); if (q.isEmpty() || seen.contains(q)) continue; seen.add(q); if (sb.length() > 0) sb.append(" \n"); sb.append(q); }
        String text = sb.toString();
        if (text.isEmpty() || text.length() > 600) return null;
        if (!CREDIT.matcher(text).find() || REJECT.matcher(text).find()) return null;
        double amt = -1;
        Matcher m = AMT.matcher(text);
        while (m.find()) {
            String before = text.substring(Math.max(0, m.start() - 16), m.start()).toLowerCase(Locale.ROOT);
            if (before.contains("bal") || before.contains("limit") || before.contains("avl")) continue;
            try { amt = Double.parseDouble(m.group(1).replace(",", "")); } catch (NumberFormatException e) { continue; }
            break;
        }
        if (!(amt > 0) || amt >= 1e7) return null;
        amt = Math.round(amt * 100.0) / 100.0;
        String who = "";
        Matcher a = PAID_YOU.matcher(text);
        if (a.find()) who = a.group(1);
        else { Matcher f = FROM.matcher(text); if (f.find()) who = f.group(1); }
        who = who.replaceAll("^[\\s\\-:]+|[\\s.\\-:]+$", "").replaceAll("(?i)^(payment|money|rs|inr)\\s+", "");
        if (who.length() > 40) who = who.substring(0, 40);
        if (who.matches("(?i)(you|your|a/c|ac|bank|upi|paytm|phonepe|google pay|gpay)")) who = "";
        return new Payment(amt, who);
    }

    /** Same payment seen twice? (GPay notification + bank SMS, or an updated notification.) */
    public static boolean isDuplicate(List<Seen> recent, double amt, String pkg, String key, long now) {
        for (Seen s : recent) {
            if (Math.abs(s.amt - amt) > 0.004) continue;
            long age = now - s.ts;
            if (s.pkg.equals(pkg)) { if (key != null && key.equals(s.key) && age < 600000L) return true; }
            else if (age < 120000L) return true;
        }
        return false;
    }

    /** Messaging apps that carry bank SMS. Their alerts are held briefly so a payment-app alert can win. */
    public static boolean isSmsApp(String pkg) {
        if (pkg == null) return false;
        String l = pkg.toLowerCase(Locale.ROOT);
        return l.contains("messag") || l.contains("mms") || l.contains("sms") || l.contains("truecaller");
    }
    /** True if a non-SMS (payment app) alert for this amount was seen within windowMs. */
    public static boolean appAlertSeen(List<Seen> recent, double amt, long now, long windowMs) {
        for (Seen s : recent) {
            if (isSmsApp(s.pkg)) continue;
            if (Math.abs(s.amt - amt) > 0.004) continue;
            if (now - s.ts <= windowMs) return true;
        }
        return false;
    }
}
