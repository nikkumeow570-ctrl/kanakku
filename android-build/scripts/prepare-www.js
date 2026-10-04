// Copies the web app (repo root) into android-build/www. sw.js is left out on purpose:
// the native app bundles its files, so a service worker would only cause stale caches.
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..", "..");
const out = path.join(__dirname, "..", "www");
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });
const files = ["index.html", "manifest.json", "icon.svg", "icon-192.png", "icon-512.png", "icon-maskable-192.png", "icon-maskable-512.png"];
for (const f of files) {
  const src = path.join(root, f);
  if (!fs.existsSync(src)) throw new Error("missing " + f);
  fs.copyFileSync(src, path.join(out, f));
}
console.log("www ready:", fs.readdirSync(out).join(", "));
