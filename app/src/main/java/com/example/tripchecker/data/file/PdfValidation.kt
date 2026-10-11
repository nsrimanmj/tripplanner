package com.example.tripchecker.data.file

/** Pure PDF header and size checks. */
object PdfValidation {
    private val PDF_MAGIC = "%PDF-".encodeToByteArray()
    const val MAX_BYTES: Long = 20L * 1024 * 1024

    fun isPdfHeader(header: ByteArray): Boolean {
        if (header.size < PDF_MAGIC.size) return false
        return PDF_MAGIC.indices.all { header[it] == PDF_MAGIC[it] }
    }

    fun isWithinSizeLimit(sizeBytes: Long): Boolean = sizeBytes in 1..MAX_BYTES
}
