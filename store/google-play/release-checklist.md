# Migo Android yayın kontrol listesi

## Kod ve backend

- [ ] `backend/deploy-auth.cmd` ile `migo-api` Worker'ını yayınla.
- [ ] Scriptin yazdığı `MIGO_BACKEND_URL` değerinin yeni Worker adresi olduğunu doğrula.
- [ ] Canlı `/health`, `/privacy`, `/terms`, `/support` ve `/v1/legal` uçlarını aç.
- [ ] NVIDIA kariyer analizi, koç ve CV analizi isteklerini gerçek cihazda dene.
- [ ] Üniversite ve program aramasını farklı yazım örnekleriyle dene.
- [ ] Profil silme işleminin D1 kaydını kaldırdığını doğrula.
- [ ] Crash/ANR ve ağ hatası ekranlarını zayıf bağlantıda kontrol et.

## İmzalama ve paket

- [ ] `setup-release-signing.cmd` ile upload anahtarını oluştur.
- [ ] `keystore` ve `keystore.properties` dosyalarını şifreli, ayrı bir konuma yedekle.
- [ ] `versionCode` ve `versionName` değerlerini kontrol et.
- [ ] `gradlew.bat clean :app:lintRelease :app:bundleRelease` çalıştır.
- [ ] AAB'nin upload anahtarıyla imzalandığını doğrula.
- [ ] AAB'yi Play Console iç test kanalına yükle.

## Play Console

- [ ] Uygulama erişimi, reklam, içerik derecelendirmesi ve hedef kitle formlarını doldur.
- [ ] Data Safety formunu `data-safety-tr.md` envanteriyle eşleştir.
- [ ] Gizlilik politikası URL'sini canlı `/privacy` sayfasına ayarla.
- [ ] Veri silme açıklamasında uygulama içindeki yolu belirt.
- [ ] Mağaza açıklamasını, uygulama ikonunu, telefon ekran görüntülerini ve özellik
      grafiğini yükle.
- [ ] Geliştirici adı, destek e-postası ve destek URL'sini tamamla.
- [ ] Hesabın kapalı test şartına tabi olup olmadığını Console'da kontrol et; tabi ise
      gereken test kullanıcısı ve süre koşulunu tamamla.

## Son kalite turu

- [ ] En az bir düşük/orta ve bir büyük ekranlı Android cihazda kontrol et.
- [ ] Açık/koyu tema, klavye, geri tuşu, ekran döndürme ve süreç yeniden oluşturmayı dene.
- [ ] Yeni kurulumda bilgilendirme ekranını; güncellemede mevcut profilin korunmasını dene.
- [ ] Çevrimdışıyken veri silip uygulamayı yeniden aç; bekleyen silmenin tekrarlandığını doğrula.
- [ ] Uygulamayı iç testten indirip üretim AAB davranışını doğrula.

## Şu anki yayın engelleri

- Canlı `migo-api` Worker deploy'u ve uçtan uca canlı servis testi.
- Upload keystore oluşturulması ve imzalı release AAB.
- Play Console geliştirici iletişim bilgileri, Data Safety beyanı ve mağaza görselleri.
- Gerçek cihaz/iç test geri bildirimi.
