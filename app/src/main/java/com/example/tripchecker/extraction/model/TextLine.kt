package com.example.tripchecker.extraction.model

/** One line of text with bounding box from OCR or PDF layout. */
data class TextLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Float get() = bottom - top
}
