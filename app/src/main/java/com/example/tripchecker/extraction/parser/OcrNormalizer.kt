package com.example.tripchecker.extraction.parser

/** Maps common OCR misreads to digits for numeric captures only. */
object OcrNormalizer {
    fun digits(s: String): String =
        buildString {
            for (ch in s) {
                when (ch) {
                    'O', 'o', 'D', 'Q' -> append('0')
                    'I', 'l', '|', 'i' -> append('1')
                    'S', 's' -> append('5')
                    'B' -> append('8')
                    'Z' -> append('2')
                    'g' -> append('9')
                    ' ' -> Unit
                    else -> if (ch.isDigit()) append(ch)
                }
            }
        }
}
