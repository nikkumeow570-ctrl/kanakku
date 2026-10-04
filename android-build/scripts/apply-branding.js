// Replaces Capacitor's default icons/splash with Kanakku's. Run after `npx cap add android`.
const fs = require("fs"), path = require("path");
const res = path.join(__dirname, "..", "android", "app", "src", "main", "res");
const brand = path.join(__dirname, "..", "branding");
if (!fs.existsSync(res)) throw new Error("android project not found at " + res);
for (const d of fs.readdirSync(brand)) {
  const from = path.join(brand, d);
  if (!d.startsWith("mipmap-") || !fs.statSync(from).isDirectory()) continue;
  const to = path.join(res, d); fs.mkdirSync(to, { recursive: true });
  for (const f of fs.readdirSync(from)) fs.copyFileSync(path.join(from, f), path.join(to, f));
}
// adaptive icon definition + background colour
const any = path.join(res, "mipmap-anydpi-v26"); fs.mkdirSync(any, { recursive: true });
const xml = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n  <background android:drawable="@color/ic_launcher_background"/>\n  <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n</adaptive-icon>\n';
fs.writeFileSync(path.join(any, "ic_launcher.xml"), xml);
fs.writeFileSync(path.join(any, "ic_launcher_round.xml"), xml);
const vals = path.join(res, "values"); fs.mkdirSync(vals, { recursive: true });
fs.writeFileSync(path.join(vals, "ic_launcher_background.xml"), '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n  <color name="ic_launcher_background">#0A6169</color>\n</resources>\n');
// splash: overwrite every existing splash.png (template ships several drawable-* variants)
let n = 0;
for (const d of fs.readdirSync(res)) {
  const p = path.join(res, d, "splash.png");
  if (d.startsWith("drawable") && fs.existsSync(p)) { fs.copyFileSync(path.join(brand, "splash.png"), p); n++; }
}
console.log("branding applied; splash files replaced:", n);
