package app.kanakku;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import java.util.Locale;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "PayListener")
public class PayListenerPlugin extends Plugin {
    private static PayListenerPlugin instance;

    @Override public void load() { instance = this; }
    @Override protected void handleOnDestroy() { if (instance == this) instance = null; }

    static void emit(JSONObject o) {
        PayListenerPlugin p = instance; if (p == null) return;
        try { p.notifyListeners("payment", new JSObject(o.toString()), true); } catch (JSONException ignored) {}
    }

    private boolean enabled() {
        Context c = getContext();
        return NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.getPackageName());
    }

    @PluginMethod public void status(PluginCall call) {
        Context c = getContext(); android.content.SharedPreferences sp = PayStore.sp(c); boolean en = enabled();
        JSObject r = new JSObject(); r.put("enabled", en); r.put("sdk", Build.VERSION.SDK_INT);
        r.put("connected", en && sp.getBoolean("connected", false));
        r.put("seenTs", sp.getLong("seen_ts", 0L)); r.put("seenPkg", sp.getString("seen_pkg", ""));
        r.put("payTs", sp.getLong("pay_ts", 0L)); r.put("selfTs", sp.getLong("selftest_ts", 0L));
        boolean ign = false; try { PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE); ign = pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName()); } catch (Exception ignored) {}
        r.put("battery", ign); r.put("keepAlive", sp.getBoolean("keepalive", false));
        r.put("notifOk", androidx.core.app.NotificationManagerCompat.from(c).areNotificationsEnabled());
        call.resolve(r);
        if (en) PayNotificationService.rebind(c);
    }
    /** Posts a silent notification from Kanakku itself; if the listener is really connected, it records selftest_ts. */
    @PluginMethod public void selfTest(PluginCall call) {
        Context c = getContext(); JSObject r = new JSObject();
        try {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                try { ActivityCompat.requestPermissions(getActivity(), new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001); } catch (Exception ignored) {}
                r.put("posted", false); r.put("reason", "notif_off"); call.resolve(r); return;
            }
            if (!androidx.core.app.NotificationManagerCompat.from(c).areNotificationsEnabled()) { r.put("posted", false); r.put("reason", "notif_off"); call.resolve(r); return; }
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("kanakku_test", "Listener test", NotificationManager.IMPORTANCE_MIN));
            nm.notify(9001, new NotificationCompat.Builder(c, "kanakku_test").setSmallIcon(c.getApplicationInfo().icon)
                .setContentTitle("Kanakku listener test").setContentText("Checking that payment alerts reach Kanakku").setPriority(NotificationCompat.PRIORITY_MIN).setAutoCancel(true).build());
            r.put("posted", true); call.resolve(r);
        } catch (Exception e) { r.put("posted", false); r.put("reason", "error"); call.resolve(r); }
    }
    @PluginMethod public void keepAlive(PluginCall call) {
        boolean on = call.getBoolean("on", false); Context c = getContext();
        try {
            Intent i = new Intent(c, KeepAliveService.class);
            if (on) { if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i); } else c.stopService(i);
            PayStore.sp(c).edit().putBoolean("keepalive", on).apply();
        } catch (Exception ignored) {}
        call.resolve();
    }
    @PluginMethod public void openBattery(PluginCall call) {
        Context c = getContext();
        try { Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName())); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); }
        catch (Exception e) { try { Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); } catch (Exception ignored) {} }
        call.resolve();
    }
    @PluginMethod public void openAutoStart(PluginCall call) {
        Context c = getContext();
        String[][] t = {
            {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
            {"com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"},
            {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
            {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
            {"com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"}};
        for (String[] x : t) {
            try { Intent i = new Intent(); i.setComponent(new ComponentName(x[0], x[1])); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); call.resolve(); return; } catch (Exception ignored) {}
        }
        openAppInfo(call);
    }
    @PluginMethod public void openSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { getContext().startActivity(i); call.resolve(); } catch (Exception e) { call.reject("settings"); }
    }
    @PluginMethod public void openAppInfo(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName())); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { getContext().startActivity(i); call.resolve(); } catch (Exception e) { call.reject("appinfo"); }
    }
    @PluginMethod public void config(PluginCall call) {
        PayStore.sp(getContext()).edit().putString("lang", "ta".equals(call.getString("lang")) ? "ta" : "en").putBoolean("speak", call.getBoolean("speak", true)).apply();
        call.resolve();
    }
    @PluginMethod public void drain(PluginCall call) {
        JSObject r = new JSObject();
        try { r.put("items", new JSArray(PayStore.queue(getContext()).toString())); } catch (JSONException e) { r.put("items", new JSArray()); }
        call.resolve(r);
    }
    @PluginMethod public void ack(PluginCall call) {
        List<String> ids = new ArrayList<>(); JSArray a = call.getArray("ids");
        if (a != null) for (int i = 0; i < a.length(); i++) ids.add(a.optString(i));
        PayStore.ack(getContext(), ids); call.resolve();
    }
    @PluginMethod public void unparsed(PluginCall call) {
        JSObject r = new JSObject(); try { r.put("items", new JSArray(PayStore.unparsed(getContext()).toString())); } catch (Exception e) { r.put("items", new JSArray()); } call.resolve(r);
    }
    /** Fires the whole pipe (queue + event) with a ₹1 test payment so the person can check it works. */
    @PluginMethod public void test(PluginCall call) {
        PaymentParser.Payment p = new PaymentParser.Payment(1, "Test");
        JSONObject o = PayStore.enqueue(getContext(), p, "test"); if (o != null) emit(o); call.resolve();
    }

    private TextToSpeech tts; private boolean ttsReady = false; private String pendingText, pendingLang = "en";

    /** Speaks text with the phone's own text-to-speech (used by the staff-phone soundbox). */
    @PluginMethod public void speak(PluginCall call) {
        pendingText = call.getString("text"); pendingLang = "ta".equals(call.getString("lang")) ? "ta" : "en";
        if (pendingText == null || pendingText.isEmpty()) { call.resolve(); return; }
        try {
            if (tts == null) { tts = new TextToSpeech(getContext().getApplicationContext(), status -> { ttsReady = (status == TextToSpeech.SUCCESS); if (ttsReady) say(); }); }
            else if (ttsReady) say();
        } catch (Exception e) { /* no TTS engine */ }
        call.resolve();
    }
    private void say() {
        try {
            if (pendingText == null) return;
            int r = tts.setLanguage("ta".equals(pendingLang) ? new Locale("ta", "IN") : new Locale("en", "IN"));
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(new Locale("en", "IN"));
            tts.speak(pendingText, TextToSpeech.QUEUE_ADD, null, "kanakku-sb-" + System.nanoTime()); pendingText = null;
        } catch (Exception ignored) {}
    }
    /** Keeps the screen on while the soundbox screen is open. */
    @PluginMethod public void keepAwake(PluginCall call) {
        final boolean on = call.getBoolean("on", false);
        try { getActivity().runOnUiThread(() -> { if (on) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }); } catch (Exception ignored) {}
        call.resolve();
    }

    /** Writes the daily backup JSON to Downloads/Kanakku (called by the web app once a day). */
    @PluginMethod public void saveBackup(PluginCall call) {
        JSObject r = new JSObject();
        try {
            String name = call.getString("name", ""), data = call.getString("data", "");
            if (data == null || data.isEmpty()) { r.put("ok", false); r.put("reason", "empty"); call.resolve(r); return; }
            String path = BackupWriter.write(getContext(), name, data);
            r.put("ok", true); r.put("path", path);
        } catch (IllegalStateException e) { r.put("ok", false); r.put("reason", e.getMessage()); }
        catch (Exception e) { r.put("ok", false); r.put("reason", "error"); }
        call.resolve(r);
    }
}
