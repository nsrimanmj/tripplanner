package com.example.tripchecker.data.file

import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/** Pure PDF header, size, and hashing helpers. */
object PdfValidation {
    private val PDF_MAGIC = "%PDF-".encodeToByteArray()
    const val MAX_BYTES: Long = 20L * 1024 * 1024

    data class CopyResult(
        val bytesWritten: Long,
        val sha256Hex: String,
    )

    fun isPdfHeader(header: ByteArray): Boolean {
        if (header.size < PDF_MAGIC.size) return false
        return PDF_MAGIC.indices.all { header[it] == PDF_MAGIC[it] }
    }

    fun isWithinSizeLimit(sizeBytes: Long): Boolean = sizeBytes in 1..MAX_BYTES

    fun readExactly(input: InputStream, count: Int): ByteArray {
        val buf = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buf, offset, count - offset)
            if (read < 0) break
            offset += read
        }
        return if (offset == count) buf else buf.copyOf(offset)
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Writes [prefix] then copies the rest of [input] to [output] with a byte cap. */
    fun copyPrefixAndStream(
        prefix: ByteArray,
        input: InputStream,
        output: OutputStream,
    ): CopyResult {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(prefix)
        output.write(prefix)
        var total = prefix.size.toLong()
        val din = DigestInputStream(input, digest)
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = din.read(buffer)
            if (read <= 0) break
            if (total + read > MAX_BYTES) {
                error("File too large")
            }
            output.write(buffer, 0, read)
            total += read
        }
        if (total == 0L) {
            error("Empty file")
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return CopyResult(total, hex)
    }
}
