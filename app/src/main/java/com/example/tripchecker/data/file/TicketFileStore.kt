package com.example.tripchecker.data.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Copies ticket PDFs into app-private storage and tracks SHA-256. */
class TicketFileStore(
    private val context: Context,
) : TicketFileOperations, TicketUriStager {
    private val ticketsDir: File
        get() = File(context.filesDir, "tickets").also { it.mkdirs() }

    private val stagingDir: File
        get() = File(context.cacheDir, StagingCleanup.STAGING_DIR_NAME).also { it.mkdirs() }

    /** Clears orphan staged PDFs (see M6.1 process-death handling). */
    fun clearOrphanStaging() {
        StagingCleanup.clean(stagingDir, ticketsDir)
    }

    /** Deletes ticket PDFs on disk that are not referenced by any DB row. */
    fun deleteUnreferencedTicketPdfs(referencedNames: Set<String>) {
        ticketsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true) && file.name !in referencedNames) {
                file.delete()
            }
        }
    }

    override suspend fun importFromUri(uriString: String): StagedFile =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val originalName = queryDisplayName(uri) ?: "ticket.pdf"
            val temp = File(stagingDir, "${System.nanoTime()}.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val header = PdfValidation.readExactly(input, 5)
                    if (!PdfValidation.isPdfHeader(header)) {
                        throw IOException("Not a PDF")
                    }
                    val result =
                        temp.outputStream().use { out ->
                            PdfValidation.copyPrefixAndStream(header, input, out)
                        }
                    if (!PdfValidation.isWithinSizeLimit(result.bytesWritten)) {
                        throw IOException("File too large")
                    }
                    StagedFile(temp, result.sha256Hex, originalName, result.bytesWritten)
                } ?: throw IOException("Cannot open URI")
            } catch (e: Exception) {
                temp.delete()
                throw e
            }
        }

    fun fileFor(fileName: String): File = File(ticketsDir, fileName)

    override suspend fun delete(fileName: String?) =
        withContext(Dispatchers.IO) {
            if (fileName == null) return@withContext
            fileFor(fileName).delete()
        }

    override suspend fun moveStagedToTicket(staged: File, ticketId: String, sha256: String): String =
        withContext(Dispatchers.IO) {
            val dest = fileFor(ticketPdfFileName(ticketId, sha256))
            if (staged.absolutePath == dest.absolutePath) {
                return@withContext dest.name
            }
            val renamed = staged.renameTo(dest)
            if (renamed) {
                return@withContext dest.name
            }
            staged.inputStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            staged.delete()
            if (!dest.exists()) {
                throw IOException("Failed to store PDF")
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
}
