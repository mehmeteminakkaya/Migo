package com.example.migo

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class CareerAnalysisUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<CareerAnalysisPreviewActivity>()

    @Test
    fun richBranch_opensReadableVisualDetails() {
        composeRule.onNodeWithText("Klinik Psikoloji").performClick()

        composeRule.onNodeWithText("KARİYER BRANŞI").assertExists()
        composeRule.onNodeWithText("Bu işte bir gün").assertExists()
        composeRule.onNodeWithText("Çalışabileceğin roller").assertExists()
        composeRule.onNodeWithText("İlk 90 gün rotası").assertExists()
        composeRule.onNodeWithText("Bu branşa odaklan").assertExists()
    }
}

class CareerAnalysisFallbackUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<CareerAnalysisFallbackPreviewActivity>()

    @Test
    fun incompleteBranch_showsUsefulFallbackInsteadOfBrokenErrorCard() {
        composeRule.onNodeWithText("Endüstri ve Örgüt Psikolojisi")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("KARİYER BRANŞI").assertExists()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText("Temel rehber gösteriliyor")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Temel rehber gösteriliyor").assertExists()
        composeRule.onNodeWithText("Bu işte bir gün").assertExists()
        composeRule.onNodeWithText("Çalışma alanları").assertExists()
        composeRule.onNodeWithText("İlk 90 gün rotası").assertExists()
        composeRule.onNodeWithText("Canlı ayrıntıyı yeniden dene").assertExists()
    }
}
