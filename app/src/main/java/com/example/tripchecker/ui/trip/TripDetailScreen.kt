package com.example.tripchecker.ui.trip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.JourneyStatus
import com.example.tripchecker.ui.common.OpenPdfResult
import com.example.tripchecker.ui.common.TicketActions
import com.example.tripchecker.ui.trip.components.LegSection
import com.example.tripchecker.ui.trip.components.LinkTicketSheet

@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    fileStore: com.example.tripchecker.data.file.TicketFileStore,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var deleteLegId by remember { mutableStateOf<String?>(null) }
    var linkSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TripDetailEvent.NavigateBack -> onBack()
                is TripDetailEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    TripDetailContent(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onEdit = onEdit,
        onCopyPnr = { pnr ->
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("pnr", pnr))
            snackbar.showSnackbar(context.getString(R.string.pnr_copied))
        },
        onGoToTicket = { leg ->
            when (TicketActions.openPdf(context, fileStore, leg)) {
                OpenPdfResult.NoPdfApp -> {
                    val result =
                        snackbar.showSnackbar(
                            message = context.getString(R.string.no_pdf_app),
                            actionLabel = context.getString(R.string.share_pdf),
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        TicketActions.sharePdf(context, fileStore, leg)
                    }
                }
                OpenPdfResult.MissingFile -> snackbar.showSnackbar(context.getString(R.string.pdf_missing))
                OpenPdfResult.Opened -> Unit
            }
        },
        onShare = { leg ->
            if (!TicketActions.sharePdf(context, fileStore, leg)) {
                snackbar.showSnackbar(context.getString(R.string.pdf_missing))
            }
        },
        onToggleCancelled = { leg -> viewModel.toggleCancelled(leg.id, !leg.isCancelled) },
        onDeleteRequest = { deleteLegId = it },
        onUnlink = viewModel::unlinkGroup,
        onResetPairing = viewModel::resetPairing,
        onLinkReturn = { linkSheet = true },
        onLinkSelect = { viewModel.linkReturn(it); linkSheet = false },
        linkSheetVisible = linkSheet,
        onDismissLinkSheet = { linkSheet = false },
        linkCandidates = state.linkCandidates,
    )

    deleteLegId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteLegId = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLeg(id)
                        deleteLegId = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteLegId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailContent(
    state: TripDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCopyPnr: (String) -> Unit,
    onGoToTicket: (com.example.tripchecker.domain.model.Ticket) -> Unit,
    onShare: (com.example.tripchecker.domain.model.Ticket) -> Unit,
    onToggleCancelled: (com.example.tripchecker.domain.model.Ticket) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onUnlink: () -> Unit,
    onResetPairing: () -> Unit,
    onLinkReturn: () -> Unit,
    onLinkSelect: (String) -> Unit,
    linkSheetVisible: Boolean,
    onDismissLinkSheet: () -> Unit,
    linkCandidates: List<com.example.tripchecker.domain.model.Ticket>,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_detail_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val group = state.group
        if (group == null) {
            Text(
                stringResource(R.string.trip_not_found),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val kindLabel =
                when (group.kind) {
                    JourneyKind.ROUND_TRIP -> stringResource(R.string.round_trip)
                    JourneyKind.ONE_WAY -> stringResource(R.string.one_way)
                    JourneyKind.CUSTOM -> stringResource(R.string.custom_trip)
                }
            AssistChip(onClick = {}, label = { Text(kindLabel) })
            if (group.status == JourneyStatus.IN_PROGRESS) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.in_progress)) })
            }
            group.legs.firstOrNull()?.extractionMethod?.let { method ->
                if (method == ExtractionMethod.OCR) {
                    Text(stringResource(R.string.ocr_hint))
                }
            }
            group.legs.forEach { leg ->
                LegSection(
                    leg = leg,
                    onCopyPnr = onCopyPnr,
                    onGoToTicket = { onGoToTicket(leg) },
                    onShare = { onShare(leg) },
                    onEdit = { onEdit(leg.id) },
                    onToggleCancelled = { onToggleCancelled(leg) },
                    onDelete = { onDeleteRequest(leg.id) },
                )
            }
            when (group.kind) {
                JourneyKind.ROUND_TRIP, JourneyKind.CUSTOM -> {
                    OutlinedButton(onClick = onUnlink, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.unlink))
                    }
                }
                JourneyKind.ONE_WAY -> {
                    OutlinedButton(onClick = onLinkReturn, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.link_return))
                    }
                }
            }
            if (group.isManual || group.legs.any { it.autoPairDisabled }) {
                OutlinedButton(onClick = onResetPairing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reset_pairing))
                }
            }
        }
    }
    LinkTicketSheet(
        visible = linkSheetVisible,
        candidates = linkCandidates,
        onDismiss = onDismissLinkSheet,
        onSelect = onLinkSelect,
    )
}
