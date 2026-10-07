// Adds Kanakku's native payment-alert code to the generated Capacitor project. Run after `npx cap add android`.
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const javaDir = path.join(root, "android", "app", "src", "main", "java", "app", "kanakku");
if (!fs.existsSync(path.join(root, "android"))) throw new Error("android project not found");
fs.mkdirSync(javaDir, { recursive: true });
const src = path.join(root, "native", "java");
for (const f of fs.readdirSync(src)) fs.copyFileSync(path.join(src, f), path.join(javaDir, f));
const mf = path.join(root, "android", "app", "src", "main", "AndroidManifest.xml");
let x = fs.readFileSync(mf, "utf8");
if (!x.includes("KeepAliveService")) {
  const perms = ["POST_NOTIFICATIONS", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_SPECIAL_USE", "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"].filter(p => !x.includes("android.permission." + p))
    .map(p => '    <uses-permission android:name="android.permission.' + p + '" />\n').join("");
  const ka = '\n        <service android:name=".KeepAliveService" android:exported="false" android:foregroundServiceType="specialUse">\n            <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Keeps the payment-alert listener alive" />\n        </service>\n';
  if (!x.includes("</application>")) throw new Error("manifest has no </application>");
  x = x.replace("</application>", ka + "    </application>");
  x = x.replace(/(<manifest[^>]*>)/, "$1\n" + perms);
  fs.writeFileSync(mf, x);
}
if (!x.includes("PayNotificationService")) {
  const svc = '\n        <service android:name=".PayNotificationService" android:exported="true" android:label="Kanakku payment alerts" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">\n            <intent-filter>\n                <action android:name="android.service.notification.NotificationListenerService" />\n            </intent-filter>\n        </service>\n';
  if (!x.includes("</application>")) throw new Error("manifest has no </application>");
  x = x.replace("</application>", svc + "    </application>");
  fs.writeFileSync(mf, x);
}
console.log("native payment code applied:", fs.readdirSync(javaDir).join(", "));
