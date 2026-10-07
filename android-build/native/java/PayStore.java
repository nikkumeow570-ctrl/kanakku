package app.kanakku;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Tiny on-device queue (SharedPreferences). Payments wait here until the web app records them. */
final class PayStore {
    private static final String FILE = "kanakku_pay";
    private PayStore() {}
    static SharedPreferences sp(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }

    static synchronized JSONArray queue(Context c) {
        try { return new JSONArray(sp(c).getString("q", "[]")); } catch (JSONException e) { return new JSONArray(); }
    }
    static synchronized JSONObject enqueue(Context c, PaymentParser.Payment p, String pkg) {
        try {
            long now = System.currentTimeMillis();
            JSONObject o = new JSONObject();
            o.put("id", now + "-" + Math.abs((pkg + p.amt + p.who).hashCode()));
            o.put("amt", p.amt); o.put("who", p.who); o.put("pkg", pkg); o.put("ts", now);
            JSONArray q = queue(c); q.put(o);
            JSONArray trimmed = new JSONArray();
            for (int i = Math.max(0, q.length() - 100); i < q.length(); i++) trimmed.put(q.get(i));
            sp(c).edit().putString("q", trimmed.toString()).apply();
            return o;
        } catch (JSONException e) { return null; }
    }
    static synchronized void ack(Context c, List<String> ids) {
        JSONArray q = queue(c), keep = new JSONArray();
        for (int i = 0; i < q.length(); i++) { JSONObject o = q.optJSONObject(i); if (o != null && !ids.contains(o.optString("id"))) keep.put(o); }
        sp(c).edit().putString("q", keep.toString()).apply();
    }

    static synchronized List<PaymentParser.Seen> recent(Context c) {
        List<PaymentParser.Seen> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("seen", "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); out.add(new PaymentParser.Seen(o.getDouble("amt"), o.getString("pkg"), o.optString("key"), o.getLong("ts"))); }
        } catch (JSONException e) { /* start fresh */ }
        return out;
    }
    static synchronized void remember(Context c, double amt, String pkg, String key, long ts) {
        try {
            JSONArray a = new JSONArray(sp(c).getString("seen", "[]")), n = new JSONArray();
            for (int i = Math.max(0, a.length() - 30); i < a.length(); i++) n.put(a.get(i));
            JSONObject o = new JSONObject(); o.put("amt", amt); o.put("pkg", pkg); o.put("key", key == null ? "" : key); o.put("ts", ts); n.put(o);
            sp(c).edit().putString("seen", n.toString()).apply();
        } catch (JSONException e) { /* ignore */ }
    }

    /** Last few notifications from payment/SMS apps that looked relevant but weren't recognised (helps fix the parser). */
    static synchronized void addUnparsed(Context c, String pkg, String text) {
        try {
            JSONArray a = new JSONArray(sp(c).getString("unp", "[]")), n = new JSONArray();
            for (int i = Math.max(0, a.length() - 4); i < a.length(); i++) n.put(a.get(i));
            JSONObject o = new JSONObject(); o.put("pkg", pkg); o.put("text", text.length() > 200 ? text.substring(0, 200) : text); o.put("ts", System.currentTimeMillis()); n.put(o);
            sp(c).edit().putString("unp", n.toString()).apply();
        } catch (JSONException e) { /* ignore */ }
    }
    static synchronized JSONArray unparsed(Context c) {
        try { return new JSONArray(sp(c).getString("unp", "[]")); } catch (JSONException e) { return new JSONArray(); }
    }
}
