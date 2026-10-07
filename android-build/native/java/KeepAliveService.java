package app.kanakku;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

/** Tiny always-on notification so phones that kill background apps (Realme/Oppo/Xiaomi/Vivo) keep Kanakku's payment listener alive. */
public class KeepAliveService extends Service {
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String ch = "kanakku_keepalive";
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm != null) nm.createNotificationChannel(new NotificationChannel(ch, "Payment alerts running", NotificationManager.IMPORTANCE_LOW));
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            PendingIntent pi = launch == null ? null : PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new NotificationCompat.Builder(this, ch)
                .setSmallIcon(getApplicationInfo().icon).setContentTitle("Kanakku is listening for payments")
                .setContentText("Tap to open. You can turn this off inside the app.").setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(pi).build();
            if (Build.VERSION.SDK_INT >= 34) startForeground(1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(1001, n);
            PayNotificationService.rebind(this);
        } catch (Exception e) { stopSelf(); }
        return START_STICKY;
    }
}
