package com.example.tripchecker.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.ui.common.PdfOpenFeedback
import com.example.tripchecker.ui.common.TicketActions
import com.example.tripchecker.ui.home.components.AddTicketFab
import com.example.tripchecker.ui.home.components.AddTicketMenu
import com.example.tripchecker.ui.home.components.HomeEmptyState
import com.example.tripchecker.ui.home.components.HomeTopBar
import com.example.tripchecker.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Stateful Home screen: collects [HomeViewModel] state and wires PDF open/share plumbing. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    fileStore: TicketFileStore,
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
    onPdfsSelected: (List<Uri>) -> Unit,
    onAddManual: () -> Unit,
    onLinkReturn: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pdfMessages =
        PdfOpenFeedback.Messages(
            noPdfApp = stringResource(R.string.no_pdf_app),
            sharePdf = stringResource(R.string.share_pdf),
            pdfMissing = stringResource(R.string.pdf_missing),
        )
    HomeContent(
        state = state,
        snackbarHostState = snackbar,
        onOpenTrip = onOpenTrip,
        onEditTicket = onEditTicket,
        onPdfsSelected = onPdfsSelected,
        onAddManual = onAddManual,
        onGoToTicket = {
            PdfOpenFeedback.openByFileName(
                scope = scope,
                snackbar = snackbar,
                context = context,
                fileStore = fileStore,
                pdfFileName = state.nextJourney?.pdfFileName,
                messages = pdfMessages,
            )
        },
        onShareTicket = {
            if (!TicketActions.sharePdfByFileName(context, fileStore, state.nextJourney?.pdfFileName)) {
                scope.launch { snackbar.showSnackbar(pdfMessages.pdfMissing) }
            }
        },
        onLinkReturn = onLinkReturn,
    )
}

/** Stateless Home content: large app bar, journey list or empty state, and the Add-ticket FAB menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
    onPdfsSelected: (List<Uri>) -> Unit,
    onAddManual: () -> Unit,
    onGoToTicket: () -> Unit = {},
    onShareTicket: () -> Unit = {},
    onLinkReturn: (String) -> Unit = {},
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val pdfLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) onPdfsSelected(uris)
        }
    val launchPicker = { pdfLauncher.launch(arrayOf("application/pdf")) }
    val actions =
        HomeActions(
            onOpenTrip = onOpenTrip,
            onEditTicket = onEditTicket,
            onGoToTicket = onGoToTicket,
            onShareTicket = onShareTicket,
            onLinkReturn = onLinkReturn,
        )
    val showFab = !state.isEmpty
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { HomeTopBar(scrollBehavior) },
            floatingActionButton = {
                if (showFab && !menuOpen) {
                    AddTicketFab(
                        expanded = !(listState.canScrollBackward && listState.isScrollInProgress),
                        onClick = { menuOpen = true },
                    )
                }
            },
        ) { padding ->
            if (state.isEmpty) {
                HomeEmptyState(
                    onImport = launchPicker,
                    onAddManually = onAddManual,
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = Dimens.screenGutter, end = Dimens.screenGutter, bottom = 96.dp),
                ) {
                    homeSections(state, actions)
                }
            }
        }
        AddTicketMenu(
            visible = menuOpen && showFab,
            onDismiss = { menuOpen = false },
            onImportPdfs = {
                menuOpen = false
                launchPicker()
            },
            onAddManually = {
                menuOpen = false
                onAddManual()
            },
        )
    }
}
