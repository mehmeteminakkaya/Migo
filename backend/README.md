# Migo API

Bu Cloudflare Worker, NVIDIA anahtarını Android APK dışında tutar. Android uygulaması yalnızca bu API ile konuşur.

## Yayınlama

`backend` klasöründeki PowerShell terminalinde tek komut çalıştır:

```powershell
.\deploy-auth.cmd
```

Script sırasıyla Cloudflare API Token ister, Worker'ı yayınlar, D1 migration'larını
uygular, yeni Worker'da `NVIDIA_API_KEY` yoksa NVIDIA anahtarını gizli olarak ister
ve canlı API'yi doğrular.
Dönen HTTPS adresini Android projesindeki `local.properties` dosyasına otomatik ekler:

`MIGO_BACKEND_URL=https://migo-cloudflare.<hesap>.workers.dev`

Worker, kurulum kimliği başına dakikada 12 ve ağ başına dakikada 30 AI isteği sınırı uygular.
Kariyer oturumu ve son 50 koç mesajı `migo-users` D1 veritabanında, kurulum başına
üretilen gizli anahtarın yalnızca SHA-256 özetiyle korunarak eşitlenir. PDF dosyasının
kendisi yüklenmez; Android uygulaması metni cihazda çıkarır.
Ücretli üyelik öncesinde bu kurulum kimliği gerçek kullanıcı hesabı ve Play Integrity doğrulamasıyla değiştirilmelidir.

Cloudflare panelinden `Edit Cloudflare Workers` şablonuyla bir API Token oluştur.
Token yalnızca çalışan
PowerShell işleminin belleğinde tutulur ve deploy sonunda temizlenir.

Git Bash kullanıyorsan önce `backend` klasörüne gir ve `bash ./deploy.sh` çalıştır. PowerShell'deki
`.\deploy-auth.cmd` yazımı Git Bash'te çalışmaz. Bu komut aynı güvenli
token istemini açar. Dağıtım tamamlandığında betik canlı `/health` ve YÖK katalog
endpointlerini kontrol eder; eski Worker sürümü hâlâ yayındaysa başarılı görünerek kapanmaz.
