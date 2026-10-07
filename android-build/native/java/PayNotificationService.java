package app.kanakku;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import org.json.JSONObject;
import java.util.Locale;

/** Listens for "money received" notifications from UPI apps / bank SMS, queues them and speaks the amount. */
public class PayNotificationService extends NotificationListenerService {
    private TextToSpeech tts; private boolean ttsReady = false;

    @Override public void onCreate() {
        super.onCreate();
        try { tts = new TextToSpeech(getApplicationContext(), status -> ttsReady = (status == TextToSpeech.SUCCESS)); } catch (Exception e) { tts = null; }
    }
    @Override public void onDestroy() { try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {} super.onDestroy(); }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null || !PaymentParser.isAllowed(sbn.getPackageName())) return;
            Notification n = sbn.getNotification(); if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
            Bundle ex = n.extras; if (ex == null) return;
            String title = str(ex.getCharSequence(Notification.EXTRA_TITLE)), text = str(ex.getCharSequence(Notification.EXTRA_TEXT)), big = str(ex.getCharSequence(Notification.EXTRA_BIG_TEXT));
            String pkg = sbn.getPackageName(), key = sbn.getKey();
            PaymentParser.Payment p = PaymentParser.parse(pkg, title, text, big);
            if (p == null) {
                String all = (title + " " + text).trim();
                if (all.matches("(?is).*[0-9].*") && all.length() > 8) PayStore.addUnparsed(this, pkg, all);
                return;
            }
            long now = System.currentTimeMillis();
            if (PaymentParser.isDuplicate(PayStore.recent(this), p.amt, pkg, key, now)) return;
            PayStore.remember(this, p.amt, pkg, key, now);
            JSONObject o = PayStore.enqueue(this, p, pkg);
            if (PayStore.sp(this).getBoolean("speak", true)) speak(p);
            if (o != null) PayListenerPlugin.emit(o);
        } catch (Exception ignored) { /* never crash the listener */ }
    }

    private static String str(CharSequence c) { return c == null ? "" : c.toString(); }

    private void speak(PaymentParser.Payment p) {
        try {
            if (tts == null || !ttsReady) return;
            boolean ta = "ta".equals(PayStore.sp(this).getString("lang", "en"));
            String a = (p.amt == Math.floor(p.amt)) ? String.valueOf((long) p.amt) : String.format(Locale.US, "%.2f", p.amt);
            String msg; boolean ok;
            if (ta) { int r = tts.setLanguage(new Locale("ta", "IN")); ok = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED; }
            else ok = false;
            if (ta && ok) msg = (p.who.isEmpty() ? "" : p.who + " கிட்ட இருந்து ") + a + " ரூபா வந்துச்சு";
            else { tts.setLanguage(new Locale("en", "IN")); msg = "Received " + a + " rupees" + (p.who.isEmpty() ? "" : " from " + p.who); }
            tts.speak(msg, TextToSpeech.QUEUE_ADD, null, "kanakku-" + System.nanoTime());
        } catch (Exception ignored) {}
    }

    /** Ask Android to reconnect the listener (used after the user turns access on). */
    static void rebind(Context c) {
        try { NotificationListenerService.requestRebind(new ComponentName(c, PayNotificationService.class)); } catch (Exception ignored) {}
    }
}
