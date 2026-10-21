package com.example.migo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerActionPlan
import com.example.migo.data.model.CareerPlanAction
import com.example.migo.data.model.CareerPlanWeek
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.DailyCareerInsight
import com.example.migo.data.model.UserProfile

class StoreAssetSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return

        MigoPreferences(context.applicationContext).run {
            acceptPrivacy()
            saveSession(storeDemoSession())
        }
    }
    private companion object {
        const val ACTION_SEED = "com.migo.kariyer.SEED_STORE_ASSETS"
    }
}

internal fun storeDemoSession() = CareerSession(
        profile = UserProfile(
            fullName = "Deniz Yılmaz",
            university = "İstanbul Teknik Üniversitesi",
            department = "Bilgisayar Mühendisliği",
            classLevel = "3. sınıf",
            scoreType = "SAY"
        ),
        interests = listOf("Mobil geliştirme", "Yapay zekâ ve veri", "Ürün teknolojileri"),
        skills = listOf("Uzmanlık projesi", "Staj deneyimi", "Teknik mülakat"),
        analysis = CareerAnalysis(
            headline = "Bilgisayar Mühendisliği 3. sınıf kariyer haritası",
            summary = "Bu harita bölümün ve sınıf seviyen temel alınarak hazırlandı; kişisel uygunluk sıralaması değildir.",
            strengths = listOf("Uzmanlık seçimi", "Staj", "Portföy", "Sektör bağlantıları"),
            careers = listOf(
                CareerRecommendation(
                    title = "Mobil Uygulama Geliştirme",
                    reason = "Android ve iOS ürünlerinin yazılım geliştirme, test ve yayın süreçlerine odaklanır.",
                    roles = listOf("Android Developer", "iOS Developer", "Mobile Engineer"),
                    workAreas = listOf("Fintech", "E-ticaret", "SaaS"),
                    exampleEmployers = listOf("Teknoloji şirketleri", "Bankalar", "Ürün girişimleri"),
                    salaryRange = "Aylık brüt yaklaşık 55.000–120.000 TL",
                    salaryNote = "AI tahmini; deneyim ve şehre göre değişir.",
                    hiringOutlook = "Yüksek",
                    demandNote = "Ürün şirketlerinde düzenli talep görülür.",
                    requirements = listOf("Kotlin veya Swift", "Mobil mimari", "Git", "Portföy uygulaması"),
                    dailyWork = listOf("Ürün gereksinimini teknik görevlere bölmek", "Ekran ve veri akışlarını geliştirmek", "Kod incelemek ve test yazmak"),
                    first90Days = listOf("Tek ekranlı bir uygulama yayınla", "API kullanan ikinci bir proje ekle", "Kodunu bir geliştiriciye incelet")
                ),
                CareerRecommendation("Yapay Zekâ ve Veri", "Veri ürünleri, makine öğrenmesi ve analitik sistemlere odaklanır."),
                CareerRecommendation("Siber Güvenlik", "Uygulama, ağ ve bulut sistemlerinin güvenliğine odaklanır."),
                CareerRecommendation("Bulut ve Platform", "Dağıtık sistemler, DevOps ve bulut altyapılarına odaklanır.")
            ),
            nextSteps = listOf(
                "Jetpack Compose ile küçük bir portföy uygulaması yayınla",
                "Bir açık kaynak Android projesine katkı yap",
                "Haftada iki teknik mülakat sorusu çöz",
                "Mobil ürün ekiplerinden üç kişiyle tanış"
            ),
            levelFocusTitle = "Uzmanlaş ve deneyimini kanıtla",
            levelFocusDescription = "Üçüncü sınıfta bir branş seçip staj ve portföy çıktılarıyla hedefini görünür hâle getir.",
            roadmap = listOf(
                CareerRoadmapStep("Bu hafta", "Branşları karşılaştır", "Dört alanın rol ve gereksinimlerini incele."),
                CareerRoadmapStep("Bu ay", "Bir uzmanlık projesi başlat", "Seçtiğin alanda gösterilebilir bir çıktı üret."),
                CareerRoadmapStep("Bu dönem", "Staj hazırlığını tamamla", "CV, GitHub ve başvuru listen üzerinde çalış."),
                CareerRoadmapStep("Dönem sonu", "Kanıtlarını değerlendir", "Proje ve deneyim çıktılarına göre hedefini güncelle.")
            )
        ),
        targetCareer = "Android Geliştirici",
        tasks = listOf(
            CareerTask("portfolio", "Portföy uygulamasının ana ekranını tamamla", "Portföy", 60),
            CareerTask("interview", "İki Kotlin mülakat sorusu çöz", "Mülakat", 30),
            CareerTask("network", "Bir Android geliştiriciyle bağlantı kur", "Network", 20),
            CareerTask("market", "Üç güncel Android ilanını incele", "Araştırma", 25)
        ),
        completedTaskIds = setOf("interview"),
        streakCount = 4,
        lastCompletionDate = "2026-07-20",
        actionPlan = CareerActionPlan(
            title = "Android için 30 günlük rota",
            objective = "Bir ay içinde yayınlanabilir bir mobil ürün ve anlatılabilir portföy kanıtı oluştur.",
            weeks = listOf(
                CareerPlanWeek(
                    number = 1,
                    title = "Ürün fikrini netleştir",
                    focus = "Keşif",
                    outcome = "Problem, kullanıcı ve başarı ölçütünü tek sayfada tanımla.",
                    actions = listOf(
                        CareerPlanAction(
                            id = "preview_w1_a1",
                            title = "Ürün özetini yaz",
                            description = "Kimin hangi sorununu çözüyorsun? Tek sayfada üç soruya net cevap ver: kim, ne, neden şimdi.",
                            durationMinutes = 45,
                            deliverable = "Tek sayfalık ürün özeti"
                        ),
                        CareerPlanAction(
                            id = "preview_w1_a2",
                            title = "Rakip iki uygulamayı incele",
                            description = "Benzer iki uygulamayı indir; neyi iyi neyi kötü yaptıklarını üç maddede not al.",
                            durationMinutes = 30,
                            deliverable = "Kısa karşılaştırma notu"
                        ),
                        CareerPlanAction(
                            id = "preview_w1_a3",
                            title = "Haftanın notunu kaydet",
                            description = "Netleşen fikri ve hâlâ emin olmadığın bir soruyu portföy günlüğüne yaz.",
                            durationMinutes = 20,
                            deliverable = "Portföy kaydı"
                        )
                    )
                ),
                CareerPlanWeek(
                    number = 2,
                    title = "Çalışan çekirdeği kur",
                    focus = "Uygulama",
                    outcome = "Ana kullanıcı akışını çalışan bir prototipe dönüştür.",
                    actions = listOf(
                        CareerPlanAction(
                            id = "preview_w2_a1",
                            title = "Ana ekranı geliştir",
                            description = "Kullanıcının uygulamaya girer girmez göreceği ekranı kodla; şimdilik sahte veriyle çalışsın.",
                            durationMinutes = 45,
                            deliverable = "Çalışan ana ekran"
                        ),
                        CareerPlanAction(
                            id = "preview_w2_a2",
                            title = "Bir arkadaşına dene dedirt",
                            description = "Prototipi bir arkadaşına aç, nerede takıldığını izle; şimdilik açıklama yapma.",
                            durationMinutes = 30,
                            deliverable = "Üç gözlem notu"
                        ),
                        CareerPlanAction(
                            id = "preview_w2_a3",
                            title = "Haftanın notunu kaydet",
                            description = "Neyin çalıştığını, neyin bozulduğunu portföy günlüğüne yaz.",
                            durationMinutes = 20,
                            deliverable = "Portföy kaydı"
                        )
                    )
                ),
                CareerPlanWeek(
                    number = 3,
                    title = "Kalite ve kanıt ekle",
                    focus = "Kalite",
                    outcome = "Test, erişilebilirlik ve ekran görüntüleriyle kaliteyi görünür yap.",
                    actions = listOf(
                        CareerPlanAction(
                            id = "preview_w3_a1",
                            title = "Kritik akışı test et",
                            description = "En önemli kullanıcı akışı için birkaç otomatik test yaz; en azından hatasız çalıştığını kanıtla.",
                            durationMinutes = 45,
                            deliverable = "Test raporu"
                        ),
                        CareerPlanAction(
                            id = "preview_w3_a2",
                            title = "Erişilebilirliği kontrol et",
                            description = "Ekran okuyucuyla dene; kontrastı ve dokunma alanlarını gözden geçir.",
                            durationMinutes = 30,
                            deliverable = "Kontrol listesi"
                        ),
                        CareerPlanAction(
                            id = "preview_w3_a3",
                            title = "Ekran görüntülerini al",
                            description = "Uygulamanın en iyi üç anını görüntüle; portföyünde bunları kullanacaksın.",
                            durationMinutes = 20,
                            deliverable = "3 ekran görüntüsü"
                        )
                    )
                ),
                CareerPlanWeek(
                    number = 4,
                    title = "Yayınla ve anlat",
                    focus = "Görünürlük",
                    outcome = "Projeyi yayınla, README ve portföy anlatımını tamamla.",
                    actions = listOf(
                        CareerPlanAction(
                            id = "preview_w4_a1",
                            title = "Mağaza metnini hazırla",
                            description = "Uygulamanın ne yaptığını üç cümlede anlat; teknik değil, faydaya odaklan.",
                            durationMinutes = 45,
                            deliverable = "Yayın taslağı"
                        ),
                        CareerPlanAction(
                            id = "preview_w4_a2",
                            title = "README yaz",
                            description = "Projeyi nasıl çalıştıracağını ve hangi kararları neden aldığını GitHub'da özetle.",
                            durationMinutes = 30,
                            deliverable = "README dosyası"
                        ),
                        CareerPlanAction(
                            id = "preview_w4_a3",
                            title = "Projeyi paylaş",
                            description = "Linki bir topluluk veya mülakat listesine ekle; artık gösterebileceğin bir kanıtın var.",
                            durationMinutes = 20,
                            deliverable = "Paylaşılan proje linki"
                        )
                    )
                )
            )
        ),
        dailyInsight = DailyCareerInsight(
            dateKey = "2026-07-20",
            title = "Kanıt üretme günü",
            message = "Üçüncü sınıfta öğrendiğin teknolojiden çok, onunla ne ürettiğini göstermek fark yaratır.",
            actionLabel = "Ana ekranın çalışan demosunu kaydet",
            coachPrompt = "Ana ekran demosunu bugün bitirebileceğim üç küçük adıma böl."
        ),
        coachMessages = listOf(
            CoachMessage("Merhaba Deniz! Android Geliştirici hedefin için bugün net bir adım seçelim.", false),
            CoachMessage("Portföyümde ilk olarak neyi göstermeliyim?", true),
            CoachMessage("Tek bir güçlü proje seç: problemi, kararlarını ve ölçülebilir sonucu kısa başlıklarla anlat. İlk ekran görüntüsünde ürünün değerini görünür yap.", false)
        )
    )
