package com.example.tripchecker.ui.trip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.common.PdfOpenFeedback
import com.example.tripchecker.ui.common.TicketActions
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.trip.components.LegCard
import com.example.tripchecker.ui.trip.components.LinkReturnSheet
import com.example.tripchecker.ui.trip.components.PairingInfoCard
import com.example.tripchecker.ui.trip.components.StayConnector
import com.example.tripchecker.ui.trip.components.TripHeader
import kotlinx.coroutines.launch

/**
 * Trip detail for the group containing the anchor ticket. When [openLinkSheet] is true and the trip
 * is one-way, the link-return sheet opens once on entry.
 */
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    fileStore: TicketFileStore,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    openLinkSheet: Boolean = false,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pnrCopiedMessage = stringResource(R.string.pnr_copied)
    val pdfMessages =
        PdfOpenFeedback.Messages(
            noPdfApp = stringResource(R.string.no_pdf_app),
            sharePdf = stringResource(R.string.share_pdf),
            pdfMissing = stringResource(R.string.pdf_missing),
        )
    var deleteLegId by rememberSaveable { mutableStateOf<String?>(null) }
    var linkSheet by rememberSaveable { mutableStateOf(false) }
    var linkSheetAutoOpened by rememberSaveable { mutableStateOf(false) }
    val isOneWay = state.group?.kind == JourneyKind.ONE_WAY

    LaunchedEffect(openLinkSheet, isOneWay) {
        if (openLinkSheet && isOneWay && !linkSheetAutoOpened) {
            linkSheetAutoOpened = true
            linkSheet = true
        }
    }
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
            scope.launch { snackbar.showSnackbar(pnrCopiedMessage) }
        },
        onGoToTicket = { leg ->
            PdfOpenFeedback.openTicketPdf(scope, snackbar, context, fileStore, leg, pdfMessages)
        },
        onShare = { leg ->
            if (!TicketActions.sharePdf(context, fileStore, leg)) {
                scope.launch { snackbar.showSnackbar(pdfMessages.pdfMissing) }
            }
        },
        onToggleCancelled = { leg -> viewModel.toggleCancelled(leg.id, !leg.isCancelled) },
        onDeleteRequest = { deleteLegId = it },
        onUnlink = viewModel::unlinkGroup,
        onResetPairing = viewModel::resetPairing,
        onLinkReturn = { linkSheet = true },
        onLinkConfirm = {
            viewModel.linkReturn(it)
            linkSheet = false
        },
        linkSheetVisible = linkSheet,
        onDismissLinkSheet = { linkSheet = false },
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

/** Stateless trip detail: transparent top bar, header, leg cards with stay connectors, pairing card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailContent(
    state: TripDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCopyPnr: (String) -> Unit,
    onGoToTicket: (Ticket) -> Unit,
    onShare: (Ticket) -> Unit,
    onToggleCancelled: (Ticket) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onUnlink: () -> Unit,
    onResetPairing: () -> Unit,
    onLinkReturn: () -> Unit,
    onLinkConfirm: (String) -> Unit,
    linkSheetVisible: Boolean,
    onDismissLinkSheet: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TripOptionsMenu(state.menu, onUnlink, onLinkReturn, onResetPairing)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val header = state.header
        val pairing = state.pairing
        if (state.group == null || header == null || pairing == null) {
            Text(
                stringResource(R.string.trip_not_found),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            TripHeader(header)
            state.legs.forEachIndexed { i, leg ->
                if (i > 0) state.stays.getOrNull(i - 1)?.let { StayConnector(it) }
                LegCard(
                    leg = leg,
                    onCopyPnr = onCopyPnr,
                    onGoToTicket = { onGoToTicket(leg.ticket) },
                    onShare = { onShare(leg.ticket) },
                    onEdit = { onEdit(leg.ticket.id) },
                    onToggleCancelled = { onToggleCancelled(leg.ticket) },
                    onDelete = { onDeleteRequest(leg.ticket.id) },
                    modifier = Modifier.padding(horizontal = Dimens.screenGutter),
                )
            }
            PairingInfoCard(
                card = pairing,
                onUnlink = onUnlink,
                onLinkReturn = onLinkReturn,
                onReset = onResetPairing,
                modifier = Modifier.padding(horizontal = Dimens.screenGutter),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
    LinkReturnSheet(
        visible = linkSheetVisible,
        sheet = state.linkSheet,
        onDismiss = onDismissLinkSheet,
        onConfirm = onLinkConfirm,
    )
}

@Composable
private fun TripOptionsMenu(
    menu: TripMenuUi,
    onUnlink: () -> Unit,
    onLinkReturn: () -> Unit,
    onResetPairing: () -> Unit,
) {
    if (menu.isEmpty) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.trip_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (menu.unlink) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.trip_unlink_tickets)) },
                    onClick = {
                        expanded = false
                        onUnlink()
                    },
                )
            }
            if (menu.linkReturn) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.link_return)) },
                    onClick = {
                        expanded = false
                        onLinkReturn()
                    },
                )
            }
            if (menu.resetPairing) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reset_pairing)) },
                    onClick = {
                        expanded = false
                        onResetPairing()
                    },
                )
            }
        }
    }
}
