package com.example.migo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.migo.data.model.CareerAnalysis
import com.example.migo.data.model.CareerRecommendation
import com.example.migo.data.model.CareerRoadmapStep
import com.example.migo.data.model.UserProfile
import com.example.migo.data.remote.MigoClientIdentity
import com.example.migo.ui.analysis.CareerAnalysisScreen
import com.example.migo.ui.theme.MigoTheme

class CareerAnalysisPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurePreviewIdentity()
        enableEdgeToEdge()
        setContent {
            MigoTheme {
                CareerAnalysisScreen(
                    profile = UserProfile(
                        fullName = "Deniz Yılmaz",
                        university = "İstanbul Üniversitesi",
                        department = "Psikoloji",
                        classLevel = "1. sınıf"
                    ),
                    analysis = psychologyPreview,
                    onBackClick = ::finish,
                    onTargetSelected = { finish() }
                )
            }
        }
    }
}

class CareerAnalysisFallbackPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurePreviewIdentity()
        enableEdgeToEdge()
        setContent {
            MigoTheme {
                CareerAnalysisScreen(
                    profile = UserProfile(
                        fullName = "Deniz Yılmaz",
                        university = "İstanbul Üniversitesi",
                        department = "Psikoloji",
                        classLevel = "1. sınıf"
                    ),
                    analysis = psychologyPreview,
                    onBackClick = ::finish,
                    onTargetSelected = { finish() },
                    detailLoader = { _, _ ->
                        Result.failure(IllegalStateException("Test fallback"))
                    }
                )
            }
        }
    }
}

private fun configurePreviewIdentity() {
    MigoClientIdentity.configure(
        installationValue = "career_preview_installation_20260721",
        syncSecretValue = "career_preview_sync_secret_20260721_abcdefghijk"
    )
}

private val psychologyPreview = CareerAnalysis(
    headline = "Psikoloji 1. sınıf kariyer haritası",
    summary = "Bu harita kişisel uygunluk sıralaması yapmaz; Psikoloji programındaki kariyer yönlerini ve birinci sınıfta atılabilecek doğru adımları gösterir.",
    strengths = listOf("Temel dersler", "Akademik okuma", "Etik farkındalık", "Alan gözlemi"),
    careers = listOf(
        CareerRecommendation(
            title = "Klinik Psikoloji",
            reason = "Ruh sağlığı değerlendirmesi, araştırma ve uygun lisansüstü eğitim sonrasında klinik uygulamalara odaklanır.",
            roles = listOf("Klinik psikolog", "Araştırma asistanı", "Psikoterapi uygulayıcısı"),
            workAreas = listOf("Hastaneler", "Danışmanlık merkezleri", "Üniversiteler"),
            exampleEmployers = listOf("Sağlık kuruluşları", "Üniversiteler", "Psikolojik danışmanlık merkezleri"),
            salaryRange = "Aylık brüt yaklaşık 45–95 bin TL",
            salaryNote = "AI tahmini; deneyim, şehir ve çalışma biçimine göre değişir.",
            hiringOutlook = "Orta",
            demandNote = "Lisansüstü uzmanlık ve uygulama deneyimi talebi belirgin biçimde etkiler.",
            requirements = listOf("Psikoloji lisansı", "Klinik psikoloji yüksek lisansı", "Etik ve süpervizyon", "Araştırma okuryazarlığı"),
            dailyWork = listOf("Görüşme ve değerlendirme notlarını hazırlamak", "Süpervizyon ve vaka toplantılarına katılmak", "Araştırma ve mesleki kaynakları izlemek"),
            first90Days = listOf("Lisansüstü koşulları karşılaştır", "Bir alan uzmanıyla bilgi görüşmesi yap", "Etik ve araştırma okuma listesi oluştur")
        ),
        CareerRecommendation(
            title = "Endüstri ve Örgüt Psikolojisi",
            reason = "Çalışan deneyimi, yetenek yönetimi, ölçme-değerlendirme ve organizasyon gelişimine odaklanır.",
            roles = listOf("İK analisti", "Yetenek uzmanı", "Organizasyon gelişimi uzmanı"),
            salaryRange = "Aylık brüt yaklaşık 50–105 bin TL",
            salaryNote = "AI tahmini; şirket ve deneyime göre değişir.",
            hiringOutlook = "Yüksek"
        ),
        CareerRecommendation(
            title = "Gelişim Psikolojisi",
            reason = "Çocukluk, ergenlik, yetişkinlik ve yaşlılık dönemlerindeki değişimleri araştırır.",
            roles = listOf("Araştırmacı", "Proje uzmanı", "Akademisyen"),
            hiringOutlook = "Gelişen"
        ),
        CareerRecommendation(
            title = "Sosyal Psikoloji",
            reason = "İnsan davranışını grup, kültür ve toplumsal etkileşim bağlamında inceler.",
            roles = listOf("Araştırmacı", "Davranış bilimi uzmanı", "İçgörü analisti"),
            hiringOutlook = "Gelişen"
        )
    ),
    nextSteps = listOf("Ders planını incele", "Bir alan söyleşisine katıl", "Okuma listesi oluştur", "Dönem hedefini yaz"),
    levelFocusTitle = "Önce temeli ve alanları tanı",
    levelFocusDescription = "Birinci sınıfta erken uzmanlaşmak yerine temel kavramları öğren, farklı psikoloji branşlarını gözlemle ve akademik çalışma alışkanlığı kur.",
    roadmap = listOf(
        CareerRoadmapStep("Bu hafta", "Programını haritala", "Zorunlu dersleri ve ön koşulları incele.", listOf("Ders planını kaydet", "İki öğretim üyesinin çalışma alanına bak")),
        CareerRoadmapStep("İlk 30 gün", "Branşları gözlemle", "En az iki farklı psikoloji alanını karşılaştır.", listOf("Bir söyleşiye katıl", "Kısa gözlem notu yaz")),
        CareerRoadmapStep("Bu dönem", "Akademik temel kur", "Kaynak okuma, not alma ve araştırma etiği alışkanlığı geliştir."),
        CareerRoadmapStep("Dönem sonu", "Deneyimini değerlendir", "Ders ve etkinlik deneyimlerine göre ikinci sınıf keşif hedefini belirle.")
    )
)
