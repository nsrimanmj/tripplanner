package com.example.tripchecker.data.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/** Copies ticket PDFs into app-private storage and tracks SHA-256. */
class TicketFileStore(
    private val context: Context,
) : TicketFileOperations {
    private val ticketsDir: File
        get() = File(context.filesDir, "tickets").also { it.mkdirs() }

    data class StagedFile(
        val file: File,
        val sha256: String,
        val originalName: String,
        val sizeBytes: Long,
    )

    suspend fun importFromUri(uri: Uri): StagedFile =
        withContext(Dispatchers.IO) {
            val originalName = queryDisplayName(uri) ?: "ticket.pdf"
            val temp = File(ticketsDir, "staging-${System.nanoTime()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                copyWithDigest(input, temp)
            } ?: error("Cannot open URI")
            val header = temp.inputStream().use { it.readNBytes(5) }
            if (!PdfValidation.isPdfHeader(header)) {
                temp.delete()
                error("Not a PDF")
            }
            if (!PdfValidation.isWithinSizeLimit(temp.length())) {
                temp.delete()
                error("File too large")
            }
            val sha = sha256File(temp)
            StagedFile(temp, sha, originalName, temp.length())
        }

    fun fileFor(fileName: String): File = File(ticketsDir, fileName)

    override suspend fun delete(fileName: String?) =
        withContext(Dispatchers.IO) {
            if (fileName == null) return@withContext
            fileFor(fileName).delete()
        }

    override suspend fun moveToTicketId(staged: File, ticketId: String): String =
        withContext(Dispatchers.IO) {
            val dest = fileFor("$ticketId.pdf")
            if (staged.absolutePath != dest.absolutePath) {
                staged.renameTo(dest)
            }
            dest.name
        }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return null
    }

    private fun copyWithDigest(input: InputStream, dest: File) {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(input, digest).use { din ->
            dest.outputStream().use { out -> din.copyTo(out) }
        }
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
