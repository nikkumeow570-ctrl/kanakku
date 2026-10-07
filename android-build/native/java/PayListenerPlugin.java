package app.kanakku;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
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
        JSObject r = new JSObject(); r.put("enabled", enabled()); r.put("sdk", Build.VERSION.SDK_INT); call.resolve(r);
        if (enabled()) PayNotificationService.rebind(getContext());
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
}
