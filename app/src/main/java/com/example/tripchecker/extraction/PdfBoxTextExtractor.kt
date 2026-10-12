package com.example.tripchecker.extraction

import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.extraction.model.ExtractedText
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads the PDF text layer via PdfBox-Android. */
/** PdfBox-Android text-layer extraction with PNR usability check. */
class PdfBoxTextExtractor : PdfTextExtractor {
    override suspend fun extract(file: File): ExtractedText? =
        withContext(Dispatchers.IO) {
            try {
                PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                    val pages = minOf(2, doc.numberOfPages)
                    val stripper =
                        PDFTextStripper().apply {
                            sortByPosition = true
                            startPage = 1
                            endPage = pages
                        }
                    val text = stripper.getText(doc)
                    if (!isUsable(text)) return@withContext null
                    ExtractedText(text = text, method = ExtractionMethod.TEXT_LAYER, pageCount = pages)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: OutOfMemoryError) {
                null
            } catch (_: StackOverflowError) {
                null
            } catch (_: Exception) {
                null
            }
        }

    private fun isUsable(text: String): Boolean {
        if (!text.contains("PNR", ignoreCase = true)) return false
        return Regex("""\b\d{10}\b""").containsMatchIn(text)
    }
}
