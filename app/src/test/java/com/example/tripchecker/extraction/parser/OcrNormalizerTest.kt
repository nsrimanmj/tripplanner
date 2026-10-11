package com.example.tripchecker.extraction.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrNormalizerTest {
    @Test
    fun `normalizes transaction id ocr noise`() {
        assertEquals("100006745112096", OcrNormalizer.digits("1OOOO6745112096"))
    }
}
