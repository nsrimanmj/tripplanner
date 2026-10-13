package com.example.tripchecker.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.ui.common.PdfOpenFeedback
import com.example.tripchecker.ui.edit.components.MissingFieldsBanner
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripcheckerTheme
import java.time.LocalDate
import java.time.LocalTime

/** Stateful edit / review / create screen; wires the ViewModel and the "Open PDF" action. */
@Composable
fun EditTicketScreen(
    viewModel: EditTicketViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val messages =
        PdfOpenFeedback.Messages(
            noPdfApp = stringResource(R.string.no_pdf_app),
            sharePdf = stringResource(R.string.share_pdf),
            pdfMissing = stringResource(R.string.pdf_missing),
        )
    val fileStore = viewModel.fileStore
    val onOpenPdf: (() -> Unit)? =
        if (fileStore != null && state.pdfFileName != null) {
            {
                PdfOpenFeedback.openByFileName(scope, snackbar, context, fileStore, state.pdfFileName, messages)
            }
        } else {
            null
        }
    EditTicketContent(
        state = state,
        onBack = onBack,
        onFormChange = viewModel::updateForm,
        onSave = { viewModel.save(onBack) },
        onOpenPdf = onOpenPdf,
        snackbarHostState = snackbar,
    )
}

/** Stateless edit form: top bar, missing-fields banner, section cards and the bottom save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTicketContent(
    state: EditTicketUiState,
    onBack: () -> Unit,
    onFormChange: ((EditTicketForm) -> EditTicketForm) -> Unit,
    onSave: () -> Unit,
    onOpenPdf: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val requestClose = { if (state.isDirty) confirmDiscard = true else onBack() }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    val title =
        stringResource(
            when {
                state.isCreate -> R.string.create_ticket_title
                state.needsReview -> R.string.edit_title_review
                else -> R.string.edit_ticket_title
            },
        )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.edit_close))
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = !state.isLoading,
                        modifier = Modifier.padding(end = Dimens.screenGutter),
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.screenGutter, vertical = Dimens.cardGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
            ) {
                val missing = state.missingRequired
                if (missing.isNotEmpty()) {
                    MissingFieldsBanner(
                        missing = missing,
                        sourceFileName = state.originalFileName,
                        extractionMethod = state.extractionMethod,
                        onOpenPdf = onOpenPdf,
                    )
                }
                val showErrors = !state.isCreate || state.saveAttempted
                TicketSection(state, showErrors, onFormChange)
                RouteSection(state, showErrors, onFormChange)
                ScheduleSection(state, showErrors, { dateTarget = it }, { timeTarget = it })
                ClassFareSection(state.form, onFormChange)
                PassengersSection(state.form, onFormChange)
                state.saveErrorMessage?.let {
                    Text(
                        stringResource(R.string.edit_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                ) { Text(stringResource(R.string.edit_save_ticket), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text(stringResource(R.string.edit_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.edit_discard_keep))
                }
            },
        )
    }

    dateTarget?.let { target ->
        EditDatePickerDialog(
            initial =
                when (target) {
                    DateTarget.Departure -> state.form.departureDate
                    DateTarget.Arrival -> state.form.arrivalDate
                },
            onDismiss = { dateTarget = null },
            onPicked = { date ->
                onFormChange { form ->
                    when (target) {
                        DateTarget.Departure ->
                            form.copy(departureDate = date, startDate = form.startDate ?: date)
                        DateTarget.Arrival -> form.copy(arrivalDate = date)
                    }
                }
            },
        )
    }

    timeTarget?.let { target ->
        EditTimePickerDialog(
            initial =
                when (target) {
                    TimeTarget.Departure -> state.form.departureTime ?: LocalTime.of(9, 0)
                    TimeTarget.Arrival -> state.form.arrivalTime ?: LocalTime.of(18, 0)
                },
            onDismiss = { timeTarget = null },
            onPicked = { time ->
                onFormChange { form ->
                    when (target) {
                        TimeTarget.Departure -> form.copy(departureTime = time)
                        TimeTarget.Arrival -> form.copy(arrivalTime = time)
                    }
                }
            },
        )
    }
}

private fun previewState(complete: Boolean): EditTicketUiState {
    val form =
        EditTicketForm(
            pnr = if (complete) "4523198760" else "",
            trainNumber = "17001",
            trainName = "SNSI SC EXPRESS",
            fromCode = "SNSI",
            fromName = "SAINAGAR SHIRDI",
            toCode = "BMT",
            toName = "BEGAMPET",
            departureDate = if (complete) LocalDate.of(2026, 8, 4) else null,
            departureTime = if (complete) LocalTime.of(17, 0) else null,
            arrivalDate = LocalDate.of(2026, 8, 5),
            arrivalTime = LocalTime.of(7, 59),
            travelClass = "3A",
            quota = "TQ",
            distanceKm = "772",
            totalFareRupees = "1494.05",
            passengers =
                listOf(
                    PassengerFormRow("p1", "MADHURI", "26", "F", "CNF/B4/33/LOWER"),
                    PassengerFormRow("p2", "RAVI", "30", "M", "RLWL/5"),
                ),
        )
    return EditTicketUiState(
        isCreate = false,
        isLoading = false,
        form = form,
        missingFields = EditTicketMapper.missingFields(form),
        needsReview = !complete,
        pdfFileName = "t.pdf",
        originalFileName = "scan_0812.pdf",
        extractionMethod = ExtractionMethod.OCR,
    )
}

@Preview(name = "Edit review light", showBackground = true, heightDp = 1800)
@Composable
private fun EditTicketContentPreview() {
    TripcheckerTheme {
        EditTicketContent(previewState(complete = false), onBack = {}, onFormChange = {}, onSave = {}, onOpenPdf = {})
    }
}

@Preview(name = "Edit complete dark", showBackground = true, heightDp = 1800)
@Composable
private fun EditTicketContentDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        EditTicketContent(previewState(complete = true), onBack = {}, onFormChange = {}, onSave = {})
    }
}

@Preview(name = "Create empty", showBackground = true, heightDp = 1600)
@Composable
private fun CreateTicketPreview() {
    val form = EditTicketForm()
    TripcheckerTheme {
        EditTicketContent(
            EditTicketUiState(
                isCreate = true,
                isLoading = false,
                form = form,
                missingFields = EditTicketMapper.missingFields(form),
            ),
            onBack = {},
            onFormChange = {},
            onSave = {},
        )
    }
}
