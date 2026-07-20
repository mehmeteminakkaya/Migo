const POLICY_VERSION = 1;
const LAST_UPDATED = "20 Temmuz 2026";
const SUPPORT_URL = "https://github.com/mehmeteminakkaya/Migo/issues";

const documents = {
  "/privacy": {
    title: "Gizlilik Bildirimi",
    description: "Migo'nun hangi verileri neden işlediğini ve verilerini nasıl silebileceğini açıklar.",
    body: `
      <section>
        <h2>Kısa özet</h2>
        <p>Migo, sana kişiselleştirilmiş kariyer analizi, yol haritası, AI koçluğu ve CV geri bildirimi sunmak için verdiğin bilgileri işler. Verilerin reklam amacıyla satılmaz ve reklam profili oluşturmak için kullanılmaz.</p>
      </section>
      <section>
        <h2>İşlenen veriler</h2>
        <ul>
          <li>Ad ve soyad, üniversite, bölüm/program ve sınıf bilgisi.</li>
          <li>Hedef kariyer, önerilen ilgi alanları, gelişim alanları, görevler ve ilerleme.</li>
          <li>AI kariyer koçuyla yaptığın son 50 mesaj.</li>
          <li>CV analizi istediğinde seçtiğin PDF'den cihazında çıkarılan metin.</li>
          <li>Güvenlik ve kota için rastgele uygulama kurulum kimliği ile sınırlı ağ bilgileri.</li>
        </ul>
      </section>
      <section>
        <h2>Verilerin kullanım amacı ve aktarımlar</h2>
        <p>Profil, hedef ve mesaj içerikleri kişiselleştirilmiş yanıt oluşturmak için Migo'nun Cloudflare Worker altyapısına gönderilir. AI yanıtı gereken istekler NVIDIA NIM hizmetine aktarılır. Eğitim kurumu ve program araması için YÖK Atlas verileri kullanılır.</p>
        <p>PDF dosyasının kendisi Migo sunucusunda saklanmaz. Metin cihazda çıkarılır; analiz için yalnızca çıkarılan metin AI hizmetine gönderilir ve Migo'nun D1 veritabanına yazılmaz.</p>
      </section>
      <section>
        <h2>Saklama ve güvenlik</h2>
        <p>Kariyer oturumun ve son 50 koç mesajın, uygulamanın ürettiği anonim kasa kimliği altında Cloudflare D1'de saklanır. Kasa anahtarının yalnızca SHA-256 özeti sunucuda tutulur. Aktif olmayan anonim kasalar son eşitlemeden 180 gün sonra otomatik silinir.</p>
        <p>Uygulamadaki <strong>Profil → Gizlilik ve veriler → Tüm verilerimi sil</strong> seçeneği cihazdaki oturumu siler ve uzak kasanın silinmesini ister.</p>
      </section>
      <section>
        <h2>Seçimlerin ve hakların</h2>
        <p>Bilgilendirme onayını vermeden AI özelliklerini kullanmazsın. İstediğin zaman tüm verilerini silebilir ve uygulamayı kullanmayı bırakabilirsin. Uygulamayı kaldırmadan önce veri silme seçeneğini kullanman, uzak kasanın anında kaldırılmasını sağlar.</p>
      </section>
      <section>
        <h2>Çocukların gizliliği</h2>
        <p>Migo genel kariyer planlama amacıyla sunulur ve 13 yaş altındaki çocuklara yönelik değildir. Bulunduğun ülkede dijital hizmetler için daha yüksek bir yaş sınırı geçerliyse veli onayı gerekebilir.</p>
      </section>
      <section>
        <h2>İletişim</h2>
        <p>Gizlilik veya veri silme talebin için uygulama mağazasındaki geliştirici iletişim adresini kullanabilirsin. Teknik sorunlar için <a href="${SUPPORT_URL}">Migo destek sayfasını</a> ziyaret edebilirsin. Kişisel veya CV bilgilerini herkese açık GitHub kayıtlarına yazma.</p>
      </section>`
  },
  "/terms": {
    title: "Kullanım Koşulları",
    description: "Migo'nun kullanım sınırlarını ve AI önerilerinin niteliğini açıklar.",
    body: `
      <section>
        <h2>Hizmetin kapsamı</h2>
        <p>Migo; kariyer keşfi, hedef belirleme, çalışma planı, CV geri bildirimi ve AI koçluğu sağlayan bir yardımcı araçtır. Uygulamayı kullanarak bu koşulları ve Gizlilik Bildirimi'ni kabul etmiş olursun.</p>
      </section>
      <section>
        <h2>AI yanıtları</h2>
        <p>AI tarafından oluşturulan yanıtlar hata, eksik veya güncelliğini yitirmiş bilgi içerebilir. Migo iş, gelir, sınav sonucu veya kariyer başarısı garantisi vermez. Eğitim, iş ve finans kararlarını doğrulanmış kaynaklar ve gerektiğinde yetkin bir uzmanla değerlendir.</p>
      </section>
      <section>
        <h2>Kullanıcı sorumluluğu</h2>
        <ul>
          <li>Girdiğin bilgilerin doğruluğundan ve paylaşma yetkisine sahip olmaktan sen sorumlusun.</li>
          <li>CV'ye kimlik numarası, sağlık verisi, finansal bilgi veya gereksiz hassas bilgi eklememelisin.</li>
          <li>Hizmeti kötüye kullanamaz, güvenlik önlemlerini aşmaya veya başkalarının verilerini işlemeye çalışamazsın.</li>
        </ul>
      </section>
      <section>
        <h2>Hizmet değişiklikleri</h2>
        <p>Özellikler güvenlik, kalite veya mevzuat gereksinimleri nedeniyle değiştirilebilir ya da geçici olarak durdurulabilir. Koşullarda önemli değişiklik olduğunda uygulamadaki onay sürümü yenilenir.</p>
      </section>
      <section>
        <h2>İletişim</h2>
        <p>Teknik destek için <a href="${SUPPORT_URL}">Migo destek sayfasını</a> kullanabilirsin. Kişisel verilerini herkese açık destek kayıtlarına ekleme.</p>
      </section>`
  },
  "/support": {
    title: "Migo Destek",
    description: "Yaygın sorunlar, veri silme ve destek kanalları.",
    body: `
      <section>
        <h2>Hızlı çözümler</h2>
        <ul>
          <li><strong>AI yanıt vermiyor:</strong> İnternet bağlantını kontrol et ve bir dakika sonra tekrar dene.</li>
          <li><strong>Üniversite veya bölüm bulunmuyor:</strong> En az iki karakter yaz ve YÖK Atlas sonucunun yüklenmesini bekle.</li>
          <li><strong>PDF okunmuyor:</strong> Dosyanın şifreli olmadığını ve metin içerdiğini kontrol et. Taranmış görsellerde metni elle yapıştırabilirsin.</li>
          <li><strong>Verilerimi silmek istiyorum:</strong> Profil sekmesindeki Gizlilik ve veriler bölümünü açıp Tüm verilerimi sil seçeneğini kullan.</li>
        </ul>
      </section>
      <section>
        <h2>Destek kaydı</h2>
        <p>Bir hata bildirirken uygulama sürümünü, telefon modelini ve sorunu yeniden oluşturma adımlarını paylaş. Ad, CV, sohbet içeriği veya başka kişisel bilgileri herkese açık kayda ekleme.</p>
        <p><a class="button" href="${SUPPORT_URL}">GitHub üzerinden teknik destek al</a></p>
      </section>`
  }
};

export function legalMetadata(origin) {
  return {
    policyVersion: POLICY_VERSION,
    lastUpdated: "2026-07-20",
    privacyUrl: `${origin}/privacy`,
    termsUrl: `${origin}/terms`,
    supportUrl: `${origin}/support`,
    retentionDays: 180
  };
}

export function legalDocumentResponse(pathname) {
  const document = documents[pathname];
  if (!document) return null;
  const navigation = Object.entries(documents)
    .map(([path, item]) => `<a${path === pathname ? ' aria-current="page"' : ""} href="${path}">${item.title}</a>`)
    .join("");
  const html = `<!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="${document.description}">
  <title>${document.title} • Migo</title>
  <style>
    :root{color-scheme:light;--ink:#17152d;--muted:#625f78;--line:#e5e3ef;--primary:#4f46e5;--soft:#f5f4ff}
    *{box-sizing:border-box}body{margin:0;background:#fafaff;color:var(--ink);font:16px/1.65 system-ui,-apple-system,"Segoe UI",sans-serif}
    header,main,footer{width:min(760px,calc(100% - 32px));margin:auto}header{padding:42px 0 22px}.brand{font-size:20px;font-weight:800;color:var(--primary)}
    h1{font-size:clamp(32px,7vw,48px);line-height:1.08;margin:18px 0 12px}h2{font-size:21px;line-height:1.3;margin:0 0 8px}p,li{color:var(--muted)}
    nav{display:flex;gap:10px;overflow:auto;padding:4px 0 12px}nav a{white-space:nowrap;text-decoration:none;color:var(--muted);padding:8px 12px;border:1px solid var(--line);border-radius:12px}
    nav a[aria-current="page"]{background:var(--primary);border-color:var(--primary);color:white}section{background:white;border:1px solid var(--line);border-radius:20px;padding:22px;margin:14px 0}
    a{color:var(--primary)}.button{display:inline-block;background:var(--primary);color:white;text-decoration:none;padding:11px 15px;border-radius:12px;font-weight:700}
    footer{padding:26px 0 48px;color:var(--muted);font-size:14px}ul{padding-left:22px}@media(max-width:520px){section{padding:18px}header{padding-top:28px}}
  </style>
</head>
<body>
  <header><div class="brand">Migo • AI Kariyer Koçun</div><h1>${document.title}</h1><p>${document.description}</p><nav aria-label="Yasal ve destek sayfaları">${navigation}</nav></header>
  <main>${document.body}</main>
  <footer>Son güncelleme: ${LAST_UPDATED} • Politika sürümü ${POLICY_VERSION}</footer>
</body>
</html>`;
  return new Response(html, {
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "public, max-age=3600",
      "content-security-policy": "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'",
      "permissions-policy": "camera=(), microphone=(), geolocation=()",
      "referrer-policy": "no-referrer",
      "strict-transport-security": "max-age=31536000; includeSubDomains",
      "x-content-type-options": "nosniff",
      "x-frame-options": "DENY"
    }
  });
}
