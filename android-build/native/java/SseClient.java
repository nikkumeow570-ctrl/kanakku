package app.kanakku;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Pure Java. Long-lived Server-Sent-Events reader for an ntfy topic, with resume + back-off. Blocks; run on a thread. */
public final class SseClient {
    public interface Listener { void onMessage(String eventId, String message); void onState(boolean connected); }
    private volatile boolean stop;
    private volatile HttpURLConnection conn;
    public void stop() { stop = true; try { HttpURLConnection c = conn; if (c != null) c.disconnect(); } catch (Exception ignored) {} }

    public static String publish(String baseUrl, String topic, String message) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + "/" + topic).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(10000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        c.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8)); c.getOutputStream().close();
        int code = c.getResponseCode(); c.disconnect();
        return String.valueOf(code);
    }

    public void run(String baseUrl, String topic, Listener l) {
        String last = ""; long backoff = 1000;
        while (!stop) {
            try {
                String url = baseUrl + "/" + topic + "/sse?since=" + (last.isEmpty() ? "10s" : last);
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); conn = c;
                c.setConnectTimeout(15000); c.setReadTimeout(100000); c.setRequestProperty("Accept", "text/event-stream");
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                l.onState(true); backoff = 1000;
                String line;
                while (!stop && (line = r.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String d = line.substring(5).trim();
                    String ev = RelayCodec.field(d, "event");
                    if (ev == null || !"message".equals(ev)) continue;
                    String id = RelayCodec.field(d, "id"), m = RelayCodec.field(d, "message");
                    if (id != null) last = id;
                    if (m != null) l.onMessage(id == null ? "" : id, m);
                }
            } catch (Exception ignored) { /* reconnect below */ }
            if (stop) break;
            l.onState(false);
            try { Thread.sleep(backoff); } catch (InterruptedException e) { break; }
            backoff = Math.min(backoff * 2, 30000);
        }
    }
}
