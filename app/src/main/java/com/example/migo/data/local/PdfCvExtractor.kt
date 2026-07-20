package com.example.migo.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.min

data class ExtractedCv(
    val fileName: String,
    val pageCount: Int,
    val text: String
)

object PdfCvExtractor {
    suspend fun extract(context: Context, uri: Uri): Result<ExtractedCv> =
        withContext(Dispatchers.IO) {
            runCatching { extractBlocking(context, uri) }
        }

    internal fun extractBlocking(context: Context, uri: Uri): ExtractedCv {
        val metadata = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            FileMetadata(
                name = if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
            )
        }

        if ((metadata?.size ?: 0L) > MAX_FILE_BYTES) {
            throw IOException("PDF en fazla 8 MB olabilir.")
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_FILE_BYTES) throw IOException("PDF en fazla 8 MB olabilir.")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IOException("PDF dosyası açılamadı.")

        return try {
            PDDocument.load(bytes).use { document ->
                val pageCount = document.numberOfPages
                if (pageCount == 0) throw IOException("PDF içinde sayfa bulunamadı.")
                val rawText = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = min(pageCount, MAX_PAGES)
                }.getText(document)
                val cleanText = normalizeText(rawText).take(MAX_TEXT_CHARS)
                if (cleanText.length < MIN_TEXT_CHARS) {
                    throw IOException(
                        "PDF'den okunabilir CV metni çıkarılamadı. Taranmış görselse metni yapıştırmayı dene."
                    )
                }
                ExtractedCv(
                    fileName = metadata?.name?.takeIf(String::isNotBlank) ?: "CV.pdf",
                    pageCount = pageCount,
                    text = cleanText
                )
            }
        } catch (error: IOException) {
            throw error
        } catch (_: Exception) {
            throw IOException("PDF okunamadı. Şifreli veya bozuk olmadığını kontrol et.")
        }
    }

    private fun normalizeText(raw: String): String = raw
        .replace("\u0000", "")
        .lines()
        .joinToString("\n") { line -> line.trim().replace(Regex("[\\t ]+"), " ") }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    private data class FileMetadata(val name: String?, val size: Long?)

    private const val MAX_FILE_BYTES = 8L * 1024L * 1024L
    private const val MAX_PAGES = 10
    private const val MAX_TEXT_CHARS = 12_000
    private const val MIN_TEXT_CHARS = 80
}
