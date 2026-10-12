package com.example.tripchecker.data.file

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfValidationTest {
    @Test
    fun `accepts pdf magic`() {
        assertTrue(PdfValidation.isPdfHeader("%PDF-".encodeToByteArray()))
    }

    @Test
    fun `rejects non pdf`() {
        assertFalse(PdfValidation.isPdfHeader("NOTPD".encodeToByteArray()))
    }

    @Test
    fun `size limit`() {
        assertTrue(PdfValidation.isWithinSizeLimit(1024))
        assertFalse(PdfValidation.isWithinSizeLimit(0))
        assertFalse(PdfValidation.isWithinSizeLimit(PdfValidation.MAX_BYTES + 1))
    }
}
