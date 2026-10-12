package com.example.tripchecker.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.domain.model.Ticket

/** Opens or shares a ticket PDF via FileProvider. */
object TicketActions {
    fun openPdf(
        context: Context,
        fileStore: TicketFileStore,
        ticket: Ticket,
    ): OpenPdfResult = openPdfByFileName(context, fileStore, ticket.pdfFileName)

    fun openPdfByFileName(
        context: Context,
        fileStore: TicketFileStore,
        pdfFileName: String?,
    ): OpenPdfResult {
        val name = pdfFileName ?: return OpenPdfResult.MissingFile
        val file = fileStore.fileFor(name)
        if (!file.exists()) return OpenPdfResult.MissingFile
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            OpenPdfResult.Opened
        } catch (_: ActivityNotFoundException) {
            OpenPdfResult.NoPdfApp
        }
    }

    fun sharePdf(context: Context, fileStore: TicketFileStore, ticket: Ticket): Boolean {
        val name = ticket.pdfFileName ?: return false
        val file = fileStore.fileFor(name)
        if (!file.exists()) return false
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val intent =
            Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}

sealed class OpenPdfResult {
    data object Opened : OpenPdfResult()
    data object NoPdfApp : OpenPdfResult()
    data object MissingFile : OpenPdfResult()
}
