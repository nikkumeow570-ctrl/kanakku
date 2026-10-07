package app.kanakku;

import java.util.Locale;

/** Pure Java. Wire format for one payment alert sent through the relay: "v1|id|amount|who". */
public final class RelayCodec {
    public static final class Msg { public final String id, who; public final double amt; public Msg(String id, double amt, String who) { this.id = id; this.amt = amt; this.who = who; } }
    private RelayCodec() {}

    public static String encode(String id, double amt, String who) {
        String w = who == null ? "" : who.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (w.length() > 40) w = w.substring(0, 40);
        return "v1|" + id.replace('|', '_') + "|" + String.format(Locale.US, "%.2f", amt) + "|" + w;
    }
    public static Msg decode(String s) {
        if (s == null) return null;
        String[] p = s.split("\\|", 4);
        if (p.length < 3 || !"v1".equals(p[0])) return null;
        try { double a = Double.parseDouble(p[2]); if (!(a > 0) || a >= 1e7) return null; return new Msg(p[1], a, p.length > 3 ? p[3] : ""); }
        catch (NumberFormatException e) { return null; }
    }
    /** Pulls a JSON string field out of one ntfy event line without a JSON library. */
    public static String field(String json, String name) {
        String key = "\"" + name + "\":\"";
        int i = json.indexOf(key); if (i < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int k = i + key.length(); k < json.length(); k++) {
            char c = json.charAt(k);
            if (c == '"') return sb.toString();
            if (c == '\\' && k + 1 < json.length()) {
                char n = json.charAt(++k);
                switch (n) {
                    case 'n': sb.append('\n'); break; case 't': sb.append('\t'); break; case 'r': sb.append('\r'); break;
                    case 'u': if (k + 4 < json.length()) { try { sb.append((char) Integer.parseInt(json.substring(k + 1, k + 5), 16)); k += 4; } catch (NumberFormatException e) { sb.append('u'); } } break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        return null;
    }
}
