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
                "Ürün fikrini netleştir" to "Problem, kullanıcı ve başarı ölçütünü tek sayfada tanımla.",
                "Çalışan çekirdeği kur" to "Ana kullanıcı akışını çalışan bir prototipe dönüştür.",
                "Kalite ve kanıt ekle" to "Test, erişilebilirlik ve ekran görüntüleriyle kaliteyi görünür yap.",
                "Yayınla ve anlat" to "Projeyi yayınla, README ve portföy anlatımını tamamla."
            ).mapIndexed { weekIndex, (title, outcome) ->
                CareerPlanWeek(
                    number = weekIndex + 1,
                    title = title,
                    focus = listOf("Keşif", "Uygulama", "Kalite", "Görünürlük")[weekIndex],
                    outcome = outcome,
                    actions = listOf(
                        CareerPlanAction(
                            id = "preview_w${weekIndex + 1}_a1",
                            title = listOf("Ürün özetini yaz", "Ana ekranı geliştir", "Kritik akışı test et", "Mağaza metnini hazırla")[weekIndex],
                            description = "Tek oturumda tamamlanacak ölçülebilir bir çalışma.",
                            durationMinutes = 45,
                            deliverable = listOf("Tek sayfalık ürün özeti", "Çalışan APK", "Test raporu", "Yayın taslağı")[weekIndex]
                        ),
                        CareerPlanAction(
                            id = "preview_w${weekIndex + 1}_a2",
                            title = "Çıktıyı gözden geçir",
                            description = "Bir kontrol listesiyle eksikleri bul ve tek iyileştirme yap.",
                            durationMinutes = 30,
                            deliverable = "Önce/sonra notu"
                        ),
                        CareerPlanAction(
                            id = "preview_w${weekIndex + 1}_a3",
                            title = "Haftanın kanıtını kaydet",
                            description = "Sonucu portföy günlüğüne ekle.",
                            durationMinutes = 20,
                            deliverable = "Portföy kaydı"
                        )
                    )
                )
            }
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
