# Migo

Migo, YÖK Atlas program verilerini ve NVIDIA NIM destekli kariyer analizini kullanan Android kariyer planlama uygulamasıdır.

## Android geliştirme

Projeyi Android Studio ile bu klasörden aç. Debug sürümünü terminalden üretmek için:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

Windows/OneDrive dosya kilitlerini önlemek için çıktılar `%LOCALAPPDATA%\MigoAndroidBuild\app\outputs` altında oluşturulur.

- Uygulama kimliği: `com.migo.kariyer`
- Minimum Android: 7.0 (API 24)
- Hedef Android: API 37

## Play Store paketi

İlk yayından önce upload anahtarını bir kez oluştur:

```powershell
.\setup-release-signing.cmd
```

Parolayı güvenli bir parola yöneticisine kaydet; `keystore` klasörü ile `keystore.properties` dosyasını şifreli olarak yedekle. Ardından imzalı AAB'yi üret ve imzasını doğrula:

```powershell
.\build-play-release.cmd
```

Hazır paket ve SHA-256 özeti git tarafından dışlanan `release-output` klasörüne yazılır.

Her Play Store güncellemesinde `app/build.gradle.kts` içindeki `versionCode` değerini artır.

## Cloudflare Worker

NVIDIA anahtarı yalnızca Cloudflare secret olarak tutulur; Android uygulamasına yazılmaz. PowerShell'de:

```powershell
cd backend
.\deploy-auth.cmd
```

Git Bash'te:

```bash
cd backend
bash ./deploy.sh
```

Deploy scripti D1 migration'larını uygular; canlı `/health`, bulut eşitleme, YÖK katalog
ve yasal belge uçlarını doğrular. Ardından `local.properties` içindeki Android backend
adresini otomatik günceller. Gizlilik bildirimi `/privacy`, kullanım koşulları `/terms`
ve destek sayfası `/support` adresinde yayınlanır. Ayrıntılar için
[backend/README.md](backend/README.md) dosyasına bak.

## Mağaza hazırlığı

Google Play listeleme metni, Data Safety çalışma taslağı ve yayın kontrol listesi
`store/google-play` klasöründedir. Bunlar Console'a girilmeden önce canlı Worker adresi,
geliştirici iletişim bilgileri ve NVIDIA veri saklama koşulları son kez doğrulanmalıdır.
Telefon ekran görüntüleri ve 1024×500 özellik grafiği `store/store-assets` klasöründedir.

## Kontroller

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
cd backend
.\cloudflare.cmd deploy --dry-run
.\cloudflare.cmd check startup
```

GitHub Actions aynı Android ve Worker kontrollerini her pull request ve `main` push'unda
otomatik çalıştırır.
