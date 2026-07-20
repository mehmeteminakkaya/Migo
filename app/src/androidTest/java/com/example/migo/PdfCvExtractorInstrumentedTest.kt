package com.example.migo

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.migo.data.local.PdfCvExtractor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfCvExtractorInstrumentedTest {
    @Test
    fun textPdf_isExtractedLocally() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "migo-cv-test.pdf")
        val expected =
            "Android developer experienced with Kotlin Compose testing architecture and secure API integration."

        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(48f, 760f)
                content.showText(expected)
                content.endText()
            }
            document.save(file)
        }

        try {
            val extracted = runBlocking {
                PdfCvExtractor.extract(context, Uri.fromFile(file)).getOrThrow()
            }

            assertEquals(1, extracted.pageCount)
            assertTrue(extracted.text.contains("Android developer"))
        } finally {
            file.delete()
        }
    }
}
