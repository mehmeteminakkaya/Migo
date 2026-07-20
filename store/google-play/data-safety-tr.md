# Google Play Data Safety çalışma taslağı

Bu belge mevcut Migo kodunun teknik envanteridir. Play Console yanıtları gönderilmeden
önce Cloudflare ve NVIDIA ile geçerli sözleşmeler/veri saklama koşulları doğrulanmalıdır.

## Genel yanıtlar

- Veri topluyor veya paylaşıyor mu: Evet; kişiselleştirme ve AI işlevleri için veri
  cihaz dışına aktarılır.
- Veriler aktarım sırasında şifreleniyor mu: Evet, HTTPS.
- Kullanıcı veri silme isteği gönderebilir mi: Evet; Profil → Gizlilik ve veriler →
  Tüm verilerimi sil.
- Hesap oluşturma: Hayır. Rastgele kurulum kimliği ve gizli kasa anahtarı kullanılır.
- Reklam veya davranışsal takip: Hayır.
- Konum, kişiler, kamera veya mikrofon izni: Yok.

## Veri türü envanteri

| Play veri grubu | Migo örneği | Amaç | Saklama | Paylaşım değerlendirmesi |
|---|---|---|---|---|
| Kişisel bilgiler / ad | Ad ve soyad | Uygulama işlevi, kişiselleştirme | D1 kasası; kullanıcı silene veya 180 gün etkinlik olmayana kadar | Cloudflare hizmet sağlayıcı olarak işler; AI analiz isteğinde NVIDIA'ya aktarılabilir |
| Kişisel bilgiler / eğitim | Üniversite, program, sınıf | Kariyer analizi ve öneri | D1 kasası; aynı saklama süresi | Cloudflare ve AI isteğinde NVIDIA |
| Kullanıcı kimlikleri | Rastgele uygulama kurulum kimliği | Bulut kasası, güvenlik ve kota | Uygulama kurulumu ve D1 kasa kaydı | Cloudflare |
| Kullanıcı tarafından oluşturulan içerik | AI koç mesajları | Koç yanıtı ve konuşma devamlılığı | Son 50 mesaj D1 kasasında | Cloudflare ve yanıt üretiminde NVIDIA |
| Kullanıcı tarafından oluşturulan içerik | CV'den cihazda çıkarılan metin | Tek seferlik CV analizi | Migo D1'e yazılmaz; yanıt üretimi sırasında işlenir | Cloudflare Worker ve NVIDIA |
| Uygulama etkinliği | Hedef, görevler, tamamlanan adımlar ve seri | Yol haritası ve ilerleme | D1 kasası; aynı saklama süresi | Cloudflare; gerektiğinde AI isteğinde NVIDIA |
| Tanılama / sınırlı ağ verisi | Worker'ın güvenlik ve kota için gördüğü istek/ağ bilgisi | Güvenlik, kötüye kullanımı önleme | Sağlayıcının yapılandırmasına göre | Cloudflare |

## Console'da amaç seçimi

Toplanan ilgili veri türlerinde birincil amaç olarak “Uygulama işlevselliği” seçilmelidir.
AI tarafından kişiye özel kariyer önerisi üreten alanlarda “Kişiselleştirme” de seçilmelidir.
Kurulum kimliği ve sınırlı ağ verisinde “Güvenlik, dolandırıcılığı önleme ve uyumluluk”
uygundur. Reklam, pazarlama ve analiz amacı mevcut kodda kullanılmıyor.

## Gönderimden önce açık kararlar

1. NVIDIA NIM hesabındaki istek/içerik saklama ve model eğitimi ayarlarını kontrol et.
2. Cloudflare gözlemlenebilirlik loglarında istek gövdesi tutulmadığını doğrula.
3. Cloudflare ve NVIDIA'nın Play tanımında yalnızca “hizmet sağlayıcı” olup olmadığını
   geçerli sözleşmeler üzerinden teyit et; buna göre “paylaşılan veri” kutularını kesinleştir.
4. Mağaza geliştirici e-postasını gizlilik sayfasındaki iletişim metniyle eşleştir.
5. Canlı veri silme akışını gerçek Worker üzerinde test et ve kanıt ekran görüntüsü al.
