package com.example.migo

import com.example.migo.data.model.AiContentSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContentSanitizerTest {
    @Test
    fun inline_stripsMarkdownLinksHeadersAndEmphasis() {
        val cleaned = AiContentSanitizer.inline(
            "## Başlık\n[Kaynak](https://example.com) **kalın** ve __alt çizgili__ `kod` metin"
        )

        assertEquals("Başlık Kaynak kalın ve alt çizgili kod metin", cleaned)
    }

    @Test
    fun inline_collapsesWhitespaceAndNewlines() {
        val cleaned = AiContentSanitizer.inline("Birinci   satır\n\nİkinci\tsatır")

        assertEquals("Birinci satır İkinci satır", cleaned)
    }

    @Test
    fun inline_truncatesToMaxLengthAndTrims() {
        val raw = "a".repeat(50)

        val cleaned = AiContentSanitizer.inline(raw, maxLength = 10)

        assertEquals(10, cleaned.length)
    }

    @Test
    fun taskText_splitsOnColonWhenPresent() {
        val parsed = AiContentSanitizer.taskText("Staj yap: 3. sınıftan itibaren pratik deneyim kazan")

        assertEquals("Staj yap", parsed.title)
        assertEquals("3. sınıftan itibaren pratik deneyim kazan", parsed.description)
    }

    @Test
    fun taskText_removesLeadingListMarkers() {
        val parsed = AiContentSanitizer.taskText("1. Portföy projesi yayınla")

        assertTrue(!parsed.title.startsWith("1."))
        assertEquals("Portföy projesi yayınla", parsed.title)
    }

    @Test
    fun taskText_returnsEmptyWhenInputIsBlank() {
        val parsed = AiContentSanitizer.taskText("   ")

        assertEquals("", parsed.title)
        assertEquals("", parsed.description)
    }

    @Test
    fun taskText_keepsWholeShortSentenceAsTitleWhenNoSplitPoint() {
        val parsed = AiContentSanitizer.taskText("Kısa görev.")

        assertEquals("Kısa görev", parsed.title)
        assertEquals("", parsed.description)
    }
}
