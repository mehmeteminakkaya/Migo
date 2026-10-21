# Migo Android yayın kontrol listesi

## Kod ve backend

- [x] `migo-api` Worker'ını Cloudflare'a yayınla.
- [x] Android `MIGO_BACKEND_URL` değerini canlı Worker adresiyle doğrula.
- [x] Canlı `/health`, `/privacy`, `/terms`, `/support` ve `/v1/legal` uçlarını aç.
- [x] NVIDIA kariyer analizi, koç ve CV analizi isteklerini canlı serviste dene.
- [x] Üniversite ve program aramasını farklı yazım örnekleriyle dene.
- [x] D1 anonim kasa oluşturma, okuma ve silme akışını canlı serviste doğrula.
- [ ] Aynı AI ve profil silme akışlarını fiziksel Android cihazda dene.
- [ ] Crash/ANR ve ağ hatası ekranlarını zayıf bağlantıda kontrol et (koçta başarısız soru artık
      geçmişe yazılmaz; "Tekrar dene" kartı çıkmalı).
- [ ] Günlük hatırlatmayı fiziksel cihazda dene: Ayarlar'dan aç, Android 13+ izin isteğini onayla,
      19:00 bildirimini ve dokununca uygulamanın açıldığını doğrula; "Tüm verilerimi sil" sonrası
      bildirimin durduğunu kontrol et.

## İmzalama ve paket

- [ ] `setup-release-signing.cmd` ile upload anahtarını oluştur.
- [ ] `keystore` ve `keystore.properties` dosyalarını şifreli, ayrı bir konuma yedekle.
- [x] `versionCode=1` ve `versionName=1.0.0` değerlerini kontrol et.
- [ ] `build-play-release.cmd` çalıştır.
- [ ] AAB'nin upload anahtarıyla imzalandığını doğrula.
- [ ] AAB'yi Play Console iç test kanalına yükle.

## Play Console

- [ ] Uygulama erişimi, reklam, içerik derecelendirmesi ve hedef kitle formlarını doldur.
- [ ] Data Safety formunu `data-safety-tr.md` envanteriyle eşleştir.
- [ ] Gizlilik politikası URL'sini canlı `/privacy` sayfasına ayarla.
- [ ] Veri silme açıklamasında uygulama içindeki yolu belirt.
- [x] Mağaza açıklamasını, uygulama ikonunu, telefon ekran görüntülerini ve 1024×500
      özellik grafiğini hazırla.
- [ ] Hazır mağaza görsellerini Play Console'a yükle.
- [ ] Geliştirici adı, destek e-postası ve destek URL'sini tamamla.
- [ ] Hesabın kapalı test şartına tabi olup olmadığını Console'da kontrol et; tabi ise
      gereken test kullanıcısı ve süre koşulunu tamamla.

## Son kalite turu

- [ ] En az bir düşük/orta ve bir büyük ekranlı Android cihazda kontrol et.
- [ ] Açık/koyu tema, klavye, geri tuşu, ekran döndürme ve süreç yeniden oluşturmayı dene.
      Onboarding sırasında döndürmede profil/analiz artık korunmalı (`MigoViewModel`); süreç
      ölümünde onboarding yarıda kalırsa başa dönmesi beklenen davranıştır.
- [ ] Yeni kurulumda bilgilendirme ekranını; güncellemede mevcut profilin korunmasını dene.
- [ ] Çevrimdışıyken veri silip uygulamayı yeniden aç; bekleyen silmenin tekrarlandığını doğrula.
- [ ] Uygulamayı iç testten indirip üretim AAB davranışını doğrula.

## Şu anki yayın engelleri

- Upload keystore oluşturulması ve imzalı release AAB.
- Play Console geliştirici iletişim bilgileri ve Data Safety beyanının kesinleştirilmesi.
- Gerçek cihaz/iç test geri bildirimi.

## Otomatik doğrulamalar

- [x] Android birim testleri, lint ve debug APK derlemesi.
- [ ] Yeni eklenen enstrümantasyon testi `LocalDataCipherInstrumentedTest` cihazda çalıştırıldı
      (şu an yalnızca derleniyor).
- [x] Pixel 8 emülatörde enstrümantasyon testleri.
- [x] Worker testleri, dry-run deploy ve startup kontrolü.
- [x] GitHub Actions CI ve Dependabot yapılandırması.
