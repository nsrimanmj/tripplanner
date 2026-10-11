package com.example.tripchecker.extraction

import com.example.tripchecker.extraction.model.ExtractedText
import java.io.File

/** Extracts plain text from a ticket PDF file. */
interface PdfTextExtractor {
    suspend fun extract(file: File): ExtractedText?
}
