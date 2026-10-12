package com.example.tripchecker.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.domain.model.JourneyKind
import androidx.compose.ui.platform.LocalContext
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.ui.common.Formatters
import com.example.tripchecker.ui.common.PdfOpenFeedback

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    fileStore: TicketFileStore,
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
    onPdfsSelected: (List<android.net.Uri>) -> Unit,
    onAddManual: () -> Unit,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
    onPdfsSelected: (List<android.net.Uri>) -> Unit,
    onAddManual: () -> Unit,
    onGoToTicket: () -> Unit = {},
) {
    var fabExpanded by remember { mutableStateOf(false) }
    val pdfLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) onPdfsSelected(uris)
        }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            Column {
                DropdownMenu(expanded = fabExpanded, onDismissRequest = { fabExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_pdfs)) },
                        onClick = {
                            fabExpanded = false
                            pdfLauncher.launch(arrayOf("application/pdf"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_manually)) },
                        onClick = {
                            fabExpanded = false
                            onAddManual()
                        },
                    )
                }
                FloatingActionButton(onClick = { fabExpanded = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_ticket))
                }
            }
        },
    ) { padding ->
        if (state.isEmpty) {
            EmptyHomeState(
                modifier = Modifier.padding(padding),
                onImport = { pdfLauncher.launch(arrayOf("application/pdf")) },
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.nextJourney?.let { next ->
                item {
                    NextJourneyCard(next, onOpenTrip, onGoToTicket)
                }
            }
            if (state.needsReview.isNotEmpty()) {
                item { Text(stringResource(R.string.needs_review), style = MaterialTheme.typography.titleMedium) }
                items(state.needsReview) { ticket ->
                    ElevatedCard(
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                onEditTicket(ticket.id)
                            },
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(R.string.needs_review_row, ticket.originalFileName ?: ticket.id),
                        )
                    }
                }
            }
            if (state.upcoming.isNotEmpty()) {
                item { Text(stringResource(R.string.upcoming), style = MaterialTheme.typography.titleMedium) }
                items(state.upcoming) { group ->
                    JourneyCard(group, onOpenTrip)
                }
            }
            state.pastByMonth.forEach { month ->
                item { Text(month.header, style = MaterialTheme.typography.titleMedium) }
                items(month.groups) { group ->
                    JourneyCard(group, onOpenTrip)
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier, onImport: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.empty_home_message), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onImport) {
            Text(stringResource(R.string.import_pdfs))
        }
    }
}

@Composable
private fun NextJourneyCard(
    next: NextJourneyUi,
    onOpenTrip: (String) -> Unit,
    onGoToTicket: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onOpenTrip(next.ticketId) },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(next.countdown, style = MaterialTheme.typography.labelLarge)
            Text(next.trainLine, style = MaterialTheme.typography.titleMedium)
            Text(next.route, style = MaterialTheme.typography.titleLarge)
            Text(next.departureTime)
            Text(next.berthSummary, style = MaterialTheme.typography.headlineSmall)
            if (next.pdfFileName != null) {
                TextButton(onClick = onGoToTicket) {
                    Text(stringResource(R.string.go_to_ticket))
                }
            }
        }
    }
}

@Composable
private fun JourneyCard(group: JourneyGroup, onOpenTrip: (String) -> Unit) {
    val first = group.legs.first()
    val label =
        when (group.kind) {
            JourneyKind.ROUND_TRIP -> stringResource(R.string.round_trip)
            JourneyKind.ONE_WAY -> stringResource(R.string.one_way)
            JourneyKind.CUSTOM -> stringResource(R.string.custom_trip)
        }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onOpenTrip(first.id) },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            group.legs.forEach { leg ->
                val dep = leg.departureAt?.let { Formatters.formatTime(it) } ?: "—"
                Text("${leg.origin?.code} → ${leg.to?.code} · $dep · ${leg.trainNumber ?: ""}")
            }
        }
    }
}
