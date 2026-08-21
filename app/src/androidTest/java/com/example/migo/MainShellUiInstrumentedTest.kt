package com.example.migo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainShellUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainShellPreviewActivity>()

    @Test
    fun bottomNavigation_opensEveryPrimaryArea() {
        composeRule.onNodeWithText("BUGÜNÜN ODAĞI", substring = true).assertExists()

        composeRule.onNodeWithContentDescription("Planım").performClick()
        composeRule.onNodeWithText("Yol haritan").assertExists()

        composeRule.onNodeWithContentDescription("Araçlar").performClick()
        composeRule.onNodeWithText("AI CV analizi").assertExists()

        composeRule.onNodeWithContentDescription("AI Koç").performClick()
        composeRule.onNodeWithText("Migo AI Koç", substring = true).assertExists()

        composeRule.onNodeWithContentDescription("Profil").performClick()
        composeRule.onNodeWithText("Kariyer profilin").assertExists()

        composeRule.onNodeWithContentDescription("Ana Sayfa").performClick()
        composeRule.onNodeWithText("BUGÜNÜN ODAĞI", substring = true).assertExists()
    }

    @Test
    fun mainShell_doesNotRenderRawMarkdown() {
        composeRule.onAllNodes(hasText("**", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("__", substring = true)).assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Planım").performClick()
        composeRule.onAllNodes(hasText("**", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("__", substring = true)).assertCountEquals(0)
    }
}
