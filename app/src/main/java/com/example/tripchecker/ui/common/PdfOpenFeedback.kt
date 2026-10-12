package com.example.tripchecker.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.domain.model.Ticket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** User-visible feedback when opening or sharing a ticket PDF. */
object PdfOpenFeedback {
    data class Messages(
        val noPdfApp: String,
        val sharePdf: String,
        val pdfMissing: String,
    )

    fun openTicketPdf(
        scope: CoroutineScope,
        snackbar: SnackbarHostState,
        context: Context,
        fileStore: TicketFileStore,
        ticket: Ticket,
        messages: Messages,
    ) {
        openByFileName(scope, snackbar, context, fileStore, ticket.pdfFileName, messages)
    }

    fun openByFileName(
        scope: CoroutineScope,
        snackbar: SnackbarHostState,
        context: Context,
        fileStore: TicketFileStore,
        pdfFileName: String?,
        messages: Messages,
    ) {
        when (TicketActions.openPdfByFileName(context, fileStore, pdfFileName)) {
            OpenPdfResult.NoPdfApp ->
                scope.launch {
                    val result =
                        snackbar.showSnackbar(
                            message = messages.noPdfApp,
                            actionLabel = messages.sharePdf,
                        )
                    if (result == SnackbarResult.ActionPerformed && pdfFileName != null) {
                        shareByFileName(context, fileStore, pdfFileName, scope, snackbar, messages)
                    }
                }
            OpenPdfResult.MissingFile ->
                scope.launch { snackbar.showSnackbar(messages.pdfMissing) }
            OpenPdfResult.Opened -> Unit
        }
    }

    private fun shareByFileName(
        context: Context,
        fileStore: TicketFileStore,
        pdfFileName: String,
        scope: CoroutineScope,
        snackbar: SnackbarHostState,
        messages: Messages,
    ) {
        if (!TicketActions.sharePdfByFileName(context, fileStore, pdfFileName)) {
            scope.launch { snackbar.showSnackbar(messages.pdfMissing) }
        }
    }
}
