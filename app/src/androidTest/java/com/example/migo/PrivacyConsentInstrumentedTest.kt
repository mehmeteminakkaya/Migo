package com.example.migo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.migo.ui.legal.PrivacyConsentScreen
import com.example.migo.ui.theme.MigoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrivacyConsentInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun consentMustBeExplicitBeforeContinuing() {
        var accepted = false
        composeRule.setContent {
            MigoTheme {
                PrivacyConsentScreen(onBack = {}, onAccept = { accepted = true })
            }
        }

        composeRule.onNodeWithTag("privacy_consent_continue").assertIsNotEnabled()
        composeRule.onNodeWithTag("privacy_consent_checkbox").performClick().assertIsOn()
        composeRule.onNodeWithTag("privacy_consent_continue").assertIsEnabled().performClick()

        composeRule.runOnIdle { assertTrue(accepted) }
    }
}
