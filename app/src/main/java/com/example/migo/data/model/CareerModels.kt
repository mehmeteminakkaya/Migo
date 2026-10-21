package com.example.migo.data.model

data class UserProfile(
    val fullName: String,
    val university: String,
    val department: String,
    val classLevel: String,
    val universityId: String = "",
    val programId: String = "",
    val scoreType: String = ""
)

data class EducationOption(
    val id: String,
    val name: String,
    val meta: String = ""
)

data class CareerRecommendation(
    val title: String,
    val reason: String,
    val roles: List<String> = emptyList(),
    val workAreas: List<String> = emptyList(),
    val exampleEmployers: List<String> = emptyList(),
    val salaryRange: String = "",
    val salaryNote: String = "",
    val salarySourceName: String = "",
    val salarySourceUrl: String = "",
    val salarySourceDate: String = "",
    val salaryDataType: String = "",
    val hiringOutlook: String = "",
    val demandNote: String = "",
    val requirements: List<String> = emptyList(),
    val dailyWork: List<String> = emptyList(),
    val first90Days: List<String> = emptyList()
)

data class CareerRoadmapStep(
    val period: String,
    val title: String,
    val description: String,
    val actions: List<String> = emptyList()
)

data class CareerAnalysis(
    val headline: String,
    val summary: String,
    val strengths: List<String>,
    val careers: List<CareerRecommendation>,
    val nextSteps: List<String>,
    val levelFocusTitle: String = "",
    val levelFocusDescription: String = "",
    val roadmap: List<CareerRoadmapStep> = emptyList()
)

data class SmartProfileResult(
    val analysis: CareerAnalysis,
    val inferredInterests: List<String>,
    val inferredSkills: List<String>
)

data class CareerPlanAction(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val deliverable: String
)

data class CareerPlanWeek(
    val number: Int,
    val title: String,
    val focus: String,
    val outcome: String,
    val actions: List<CareerPlanAction>
)

data class CareerActionPlan(
    val title: String,
    val objective: String,
    val durationDays: Int = 30,
    val weeks: List<CareerPlanWeek>
)

data class DailyCareerInsight(
    val dateKey: String,
    val title: String,
    val message: String,
    val actionLabel: String,
    val coachPrompt: String
)

data class CareerSession(
    val profile: UserProfile,
    val interests: List<String>,
    val skills: List<String>,
    val analysis: CareerAnalysis,
    val targetCareer: String,
    val completedSteps: Set<Int> = emptySet(),
    val tasks: List<CareerTask> = emptyList(),
    val completedTaskIds: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: String = "",
    val isPremium: Boolean = false,
    val actionPlan: CareerActionPlan? = null,
    val dailyInsight: DailyCareerInsight? = null,
    val coachMessages: List<CoachMessage> = emptyList()
)

data class CareerTask(
    val id: String,
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val description: String = ""
)

data class ParsedCareerTaskText(
    val title: String,
    val description: String
)

object AiContentSanitizer {
    private val markdownLink = Regex("\\[([^]]+)]\\(https?://[^)]+\\)")
    private val leadingListMarker = Regex("^\\s*(?:[-•]\\s+|\\d+[.)]\\s+)")

    fun inline(raw: String, maxLength: Int = 700): String = raw
        .replace(markdownLink) { match -> match.groupValues[1] }
        .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\s*\\n\\s*"), " ")
        .trim()
        .take(maxLength)
        .trim()

    fun taskText(raw: String): ParsedCareerTaskText {
        val cleaned = inline(raw.replaceFirst(leadingListMarker, ""), 420)
        if (cleaned.isBlank()) return ParsedCareerTaskText("", "")

        val colon = cleaned.indexOf(':').takeIf { it in 3..90 }
        val sentence = cleaned.indexOf(". ").takeIf { it in 20..90 }
        val splitAt = colon ?: sentence ?: if (cleaned.length > 110) {
            cleaned.lastIndexOf(' ', startIndex = 88).takeIf { it >= 45 }
        } else null

        if (splitAt == null) {
            return ParsedCareerTaskText(cleaned.trimEnd('.', ':'), "")
        }

        val separatorLength = if (colon != null) 1 else if (sentence != null) 2 else 1
        return ParsedCareerTaskText(
            title = cleaned.substring(0, splitAt).trim().trimEnd('.', ':'),
            description = cleaned.substring((splitAt + separatorLength).coerceAtMost(cleaned.length))
                .trim()
        )
    }
}

object CareerTaskPlanner {
    fun build(
        targetCareer: String,
        nextSteps: List<String>,
        recommendation: CareerRecommendation? = null,
        classLevel: String = ""
    ): List<CareerTask> {
        val careerRequirements = recommendation?.requirements.orEmpty().take(2).mapIndexed { index, requirement ->
            CareerTask(
                id = "career_requirement_$index",
                title = "${AiContentSanitizer.inline(requirement, 80)} yetkinliğini kanıtla",
                category = "Yetkinlik",
                durationMinutes = 45,
                description = "Küçük bir uygulama, analiz veya portföy çıktısı üret ve tamamlandığını kaydet."
            )
        }
        val generated = nextSteps.take(4).mapIndexed { index, step ->
            val parsed = AiContentSanitizer.taskText(step)
            CareerTask(
                id = "ai_$index",
                title = parsed.title.ifBlank { "Kariyer adımını tamamla" },
                category = categoryFor(parsed.title + " " + parsed.description),
                durationMinutes = listOf(30, 60, 20, 40).getOrElse(index) { 30 },
                description = parsed.description
            )
        }
        val levelTask = levelSpecificTask(targetCareer, classLevel)
        val common = listOf(
            CareerTask(
                "market",
                "$targetCareer için 3 güncel ilanı karşılaştır",
                "Araştırma",
                25,
                "Ortak beklentileri tek sayfalık bir beceri listesine dönüştür."
            ),
            CareerTask(
                "profile",
                "Hedefini profilinde görünür hâle getir",
                "Profil",
                30,
                "CV veya LinkedIn özetini hedef rolle uyumlu, dürüst ve ölçülebilir biçimde güncelle."
            ),
            CareerTask(
                "practice",
                "5 rol odaklı mülakat sorusuna hazırlan",
                "Mülakat",
                35,
                "Yanıtlarını örnek ve kanıtlarla yaz; geliştireceğin iki noktayı işaretle."
            )
        )
        return (careerRequirements + generated + listOfNotNull(levelTask) + common)
            .map(::normalize)
            .distinctBy { it.title.lowercase() }
    }

    /**
     * Turns the CV analysis' top improvements into plan tasks. Ids derive from the improvement text so
     * adding the same analysis twice is a no-op rather than a duplicate.
     */
    fun fromCvImprovements(improvements: List<String>, limit: Int = 3): List<CareerTask> =
        improvements
            .map { AiContentSanitizer.taskText(it) }
            .filter { it.title.isNotBlank() }
            .take(limit)
            .map { parsed ->
                normalize(
                    CareerTask(
                        id = "cv_" + parsed.title.lowercase().hashCode().toUInt().toString(16),
                        title = parsed.title,
                        category = "CV",
                        durationMinutes = 30,
                        description = parsed.description
                    )
                )
            }
            .distinctBy { it.id }

    fun normalize(task: CareerTask): CareerTask {
        val parsed = AiContentSanitizer.taskText(task.title)
        val normalized = task.copy(
            title = parsed.title.ifBlank { "Kariyer adımını tamamla" },
            category = AiContentSanitizer.inline(task.category, 40).ifBlank { "Gelişim" },
            description = AiContentSanitizer.inline(
                task.description.ifBlank { parsed.description },
                320
            )
        )
        val internshipText = "${normalized.title} ${normalized.description}".lowercase()
        return if ("staj" in internshipText) {
            normalized.copy(
                title = "Opsiyonel staj veya eşdeğer uygulama",
                category = "Uygulama",
                description = "Erişimin varsa stajı değerlendir; yoksa aynı kazanımı proje, laboratuvar, vaka analizi veya gönüllü çalışmayla üret."
            )
        } else normalized
    }

    private fun categoryFor(step: String): String {
        val normalized = step.lowercase()
        return when {
            listOf("proje", "portföy", "github", "yayınla").any(normalized::contains) -> "Portföy"
            listOf("tanış", "bağlantı", "topluluk", "network").any(normalized::contains) -> "Network"
            listOf("ilan", "sektör", "araştır", "karşılaştır").any(normalized::contains) -> "Araştırma"
            listOf("cv", "linkedin", "profil").any(normalized::contains) -> "Profil"
            listOf("mülakat", "soru", "görüşme").any(normalized::contains) -> "Mülakat"
            else -> "Öğrenme"
        }
    }

    private fun levelSpecificTask(targetCareer: String, classLevel: String): CareerTask? {
        val normalized = classLevel.lowercase()
        if (normalized.isBlank()) return null
        return when {
            "1" in normalized -> CareerTask(
                "level_focus",
                "$targetCareer alanında çalışan iki rolü karşılaştır",
                "Keşif",
                25,
                "Günlük işlerini, gerekli eğitimi ve başlangıç koşullarını kısa bir tabloda karşılaştır."
            )
            "2" in normalized -> CareerTask(
                "level_focus",
                "$targetCareer için bir mini proje kapsamı yaz",
                "Portföy",
                35,
                "Bir haftada üretilebilecek çıktıyı, araçları ve bitti kriterini tanımla."
            )
            "3" in normalized -> CareerTask(
                "level_focus",
                "$targetCareer odaklı 5 başlangıç rolünü karşılaştır",
                "Uygulama",
                35,
                "İlan, proje özeti veya vaka örneklerindeki ortak becerileri sırala; geliştireceğin ilk açığı seç."
            )
            "4" in normalized || "mezun" in normalized -> CareerTask(
                "level_focus",
                "$targetCareer için hedeflediğin 10 şirketlik başvuru listesi oluştur",
                "Başvuru",
                40,
                "Her şirket için rol uyumu, son tarih ve bir sonraki işlemi kaydet."
            )
            else -> null
        }
    }
}

data class CoachMessage(
    val text: String,
    val isFromUser: Boolean
)

data class CvAnalysis(
    val score: Int,
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val missingKeywords: List<String>,
    val rewrittenBullets: List<String>
)
