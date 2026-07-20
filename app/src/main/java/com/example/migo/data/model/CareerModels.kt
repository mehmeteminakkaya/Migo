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
    val reason: String
)

data class CareerAnalysis(
    val headline: String,
    val summary: String,
    val strengths: List<String>,
    val careers: List<CareerRecommendation>,
    val nextSteps: List<String>
)

data class SmartProfileResult(
    val analysis: CareerAnalysis,
    val inferredInterests: List<String>,
    val inferredSkills: List<String>
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
    val coachMessages: List<CoachMessage> = emptyList()
)

data class CareerTask(
    val id: String,
    val title: String,
    val category: String,
    val durationMinutes: Int
)

object CareerTaskPlanner {
    fun build(targetCareer: String, nextSteps: List<String>): List<CareerTask> {
        val generated = nextSteps.take(4).mapIndexed { index, step ->
            CareerTask(
                id = "ai_$index",
                title = step,
                category = when (index) {
                    0 -> "Öğrenme"
                    1 -> "Portföy"
                    2 -> "Network"
                    else -> "Hazırlık"
                },
                durationMinutes = listOf(30, 60, 20, 40).getOrElse(index) { 30 }
            )
        }
        val common = listOf(
            CareerTask("market", "$targetCareer için 3 güncel ilanı incele", "Araştırma", 25),
            CareerTask("profile", "CV veya LinkedIn profilinde hedefini görünür hâle getir", "Profil", 30),
            CareerTask("practice", "Hedef rol için 5 mülakat sorusuna cevap hazırla", "Mülakat", 35)
        )
        return (generated + common).distinctBy { it.title.lowercase() }
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
