package com.example.tripchecker.extraction

import com.example.tripchecker.extraction.model.ExtractedText
import java.io.File

/** Tries the text layer first, then OCR. */
class CompositeTextExtractor(
    private val textLayer: PdfTextExtractor,
    private val ocr: PdfTextExtractor,
) : PdfTextExtractor {
    override suspend fun extract(file: File): ExtractedText? =
        textLayer.extract(file) ?: ocr.extract(file)
}
