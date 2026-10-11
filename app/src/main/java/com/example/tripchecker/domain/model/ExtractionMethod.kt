package com.example.tripchecker.domain.model

/** How ticket text was obtained from the PDF. */
enum class ExtractionMethod {
    TEXT_LAYER,
    OCR,
    MANUAL,
}
