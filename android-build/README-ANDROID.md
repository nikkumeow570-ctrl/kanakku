# Kanakku Android build (GitHub does the building)

One-time setup in Termux (creates your signing key and stores it as GitHub secrets):

    pkg install openjdk-21 gh -y
    keytool -genkeypair -v -keystore kanakku.jks -alias kanakku -keyalg RSA -keysize 2048 -validity 10000
    # use the SAME password for the keystore and the key when asked
    base64 -w0 kanakku.jks > ks.b64
    gh secret set KS_B64 -R nikkumeow570-ctrl/kanakku < ks.b64
    gh secret set KEY_ALIAS -R nikkumeow570-ctrl/kanakku --body kanakku
    gh secret set KS_PASS -R nikkumeow570-ctrl/kanakku      # paste the password
    gh secret set KEY_PASS -R nikkumeow570-ctrl/kanakku     # same password again
    rm ks.b64

BACK UP kanakku.jks and the password somewhere safe (not in the repo). Lose them and you can never update the app.

Build: GitHub repo -> Actions -> "Build Android APK" -> Run workflow.
Download: repo -> Releases -> "Kanakku Android (latest)" -> Kanakku.apk (public link you can share).

Note: the native app has its own storage, separate from the website/PWA. Move data with
Backup & Settings -> Share backup code (old app) and Restore from code (new app).
