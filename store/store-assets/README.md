# Mağaza görselleri

İlk yayın için aşağıdaki dosyalar gerçek uygulama üzerinden sentetik `Deniz Yılmaz`
profiliyle üretildi:

1. `phone/01-welcome.png`: Migo değer önerisi.
2. `phone/02-home.png`: hedef, günlük odak, ilerleme ve seri.
3. `phone/03-plan.png`: kişisel yol haritası.
4. `phone/04-tools.png`: AI CV analizi ve kariyer araçları.
5. `phone/05-coach.png`: yapılandırılmış AI koç konuşması.
6. `phone/06-profile.png`: kariyer profili ve gizlilik alanı.
7. `feature-graphic-1024x500.png`: Google Play özellik grafiği.

Telefon görüntüleri 1080×2400 PNG, özellik grafiği tam 1024×500 PNG boyutundadır.
Gerçek ad, CV, sohbet, e-posta veya API anahtarı içermezler.

Görselleri tekrar üretmek için debug APK'yı yükledikten sonra sentetik profili şu komutla
uygulama sürecinde oluştur:

```powershell
adb shell run-as com.migo.kariyer am broadcast `
  --user 0 `
  -n com.migo.kariyer/com.example.migo.StoreAssetSeedReceiver `
  -a com.migo.kariyer.SEED_STORE_ASSETS
```

`StoreAssetSeedReceiver` yalnızca debug kaynak setindedir ve release paketine girmez.
