package app.kanakku;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Writes the daily backup JSON to Downloads/Kanakku with no storage permission (Android 10+), keeping the newest 7. */
final class BackupWriter {
    static final String DIR = "Download/Kanakku/";
    static final String PREFIX = "kanakku-auto-";
    private BackupWriter() {}

    static String write(Context c, String name, String json) throws Exception {
        if (Build.VERSION.SDK_INT < 29) throw new IllegalStateException("android-too-old");
        if (!name.startsWith(PREFIX) || !name.endsWith(".json")) throw new IllegalArgumentException("bad-name");
        ContentResolver cr = c.getContentResolver();
        // replace today's file if it exists (the app runs this at most once a day)
        Cursor cur = cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID},
            MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?", new String[]{name, DIR}, null);
        if (cur != null) { while (cur.moveToNext()) cr.delete(Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(cur.getLong(0))), null, null); cur.close(); }
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, DIR);
        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new IllegalStateException("insert-failed");
        try (OutputStream os = cr.openOutputStream(uri)) { if (os == null) throw new IllegalStateException("no-stream"); os.write(json.getBytes(StandardCharsets.UTF_8)); }
        prune(c);
        return DIR + name;
    }

    /** Keep the 7 newest auto backups. */
    static void prune(Context c) {
        ContentResolver cr = c.getContentResolver();
        List<long[]> ids = new ArrayList<>(); List<String> names = new ArrayList<>();
        Cursor cur = cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME},
            MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?", new String[]{PREFIX + "%", DIR}, null);
        if (cur == null) return;
        while (cur.moveToNext()) { ids.add(new long[]{cur.getLong(0)}); names.add(cur.getString(1)); }
        cur.close();
        Integer[] order = new Integer[names.size()]; for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> names.get(b).compareTo(names.get(a)));
        for (int k = 7; k < order.length; k++) cr.delete(Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(ids.get(order[k])[0])), null, null);
    }
}
