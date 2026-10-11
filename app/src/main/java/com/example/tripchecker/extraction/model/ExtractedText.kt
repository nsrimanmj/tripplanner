package com.example.tripchecker.extraction.model

import com.example.tripchecker.domain.model.ExtractionMethod

/** Text extracted from a ticket PDF. */
data class ExtractedText(
    val text: String,
    val method: ExtractionMethod,
    val pageCount: Int,
)
