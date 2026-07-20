package com.example.migo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.migo.data.local.MigoPreferences
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerSession
import com.example.migo.data.model.CareerTask
import com.example.migo.data.model.CoachMessage
import com.example.migo.data.model.UserProfile

class StoreAssetSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return

        MigoPreferences(context.applicationContext).run {
            acceptPrivacy()
            saveSession(storeDemoSession())
        }
    }

    private fun storeDemoSession() = CareerSession(
        profile = UserProfile(
            fullName = "Deniz Yılmaz",
            university = "İstanbul Teknik Üniversitesi",
            department = "Bilgisayar Mühendisliği",
            classLevel = "3. sınıf",
            scoreType = "SAY"
        ),
        interests = listOf("Mobil ürünler", "Yapay zekâ", "Kullanıcı deneyimi"),
        skills = listOf("Kotlin", "Problem çözme", "Takım çalışması"),
        analysis = CareerAnalysis(
            headline = "Teknoloji ve ürün odağın güçlü bir eşleşme oluşturuyor",
            summary = "Teknik merakını kullanıcı odaklı düşünmeyle birleştirmen, ürün geliştiren ekiplerde güçlü bir başlangıç noktası sunuyor.",
            strengths = listOf("Analitik düşünme", "Hızlı öğrenme", "Ürün merakı"),
            careers = listOf(
                CareerRecommendation("Android Geliştirici", "Mobil ürün ilgini Kotlin yetkinliğinle birleştirir."),
                CareerRecommendation("Yapay Zekâ Ürün Uzmanı", "Teknik bilgi ile kullanıcı ihtiyacını aynı masada buluşturur."),
                CareerRecommendation("Ürün Analisti", "Problem çözme gücünü ölçülebilir ürün kararlarına dönüştürür.")
            ),
            nextSteps = listOf(
                "Jetpack Compose ile küçük bir portföy uygulaması yayınla",
                "Bir açık kaynak Android projesine katkı yap",
                "Haftada iki teknik mülakat sorusu çöz",
                "Mobil ürün ekiplerinden üç kişiyle tanış"
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
        coachMessages = listOf(
            CoachMessage("Merhaba Deniz! Android Geliştirici hedefin için bugün net bir adım seçelim.", false),
            CoachMessage("Portföyümde ilk olarak neyi göstermeliyim?", true),
            CoachMessage("Tek bir güçlü proje seç: problemi, kararlarını ve ölçülebilir sonucu kısa başlıklarla anlat. İlk ekran görüntüsünde ürünün değerini görünür yap.", false)
        )
    )

    private companion object {
        const val ACTION_SEED = "com.migo.kariyer.SEED_STORE_ASSETS"
    }
}
