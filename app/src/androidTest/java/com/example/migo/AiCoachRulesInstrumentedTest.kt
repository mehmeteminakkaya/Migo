package com.example.migo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.migo.data.remote.AiCoachRules
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiCoachRulesInstrumentedTest {
    @Test
    fun bundledCareerCoachRules_areReadableAsUtf8() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rules = AiCoachRules.load(context)

        assertTrue(rules.startsWith("# Migo AI Kariyer Koçu"))
        assertTrue(rules.contains("Somut çıktı"))
        assertTrue(rules.contains("Ölçülebilir kabul kriterleri"))
    }
}
