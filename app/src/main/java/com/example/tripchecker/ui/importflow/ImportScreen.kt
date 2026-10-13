package com.example.tripchecker.ui.importflow

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.importflow.components.ImportItemRow
import com.example.tripchecker.ui.importflow.components.ImportSummaryCard
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripcheckerTheme

/**
 * Stateful Import screen. [onDone] clears finished items first; [onBack] (default [onDone] without clearing)
 * is the top-bar back arrow.
 */
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onDone: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
    onBack: () -> Unit = onDone,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImportContent(
        state = state,
        onBack = onBack,
        onDone = {
            viewModel.clearFinished()
            onDone()
        },
        onRowClick = { row ->
            val id = row.targetTicketId
            if (id != null) {
                when (row.tap) {
                    ImportRowTap.OPEN_TRIP -> onOpenTrip(id)
                    ImportRowTap.EDIT_TICKET -> onEditTicket(id)
                    ImportRowTap.NONE -> Unit
                }
            }
        },
    )
}

/** Stateless Import screen: summary card, joined file rows and the footer with Done. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContent(
    state: ImportUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRowClick: (ImportRowModel) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.import_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(JoinedListDefaults.Gap),
                contentPadding = PaddingValues(start = Dimens.screenGutter, end = Dimens.screenGutter, top = 4.dp),
            ) {
                item {
                    ImportSummaryCard(state.summary, Modifier.padding(bottom = 12.dp))
                }
                itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
                    ImportItemRow(
                        row = row,
                        shape = JoinedListDefaults.shape(index, state.rows.size),
                        onClick = { onRowClick(row) },
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.import_footer_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.done), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private val previewState =
    ImportUiState(
        rows = listOf(
            ImportRowModel(
                "1", "madhuri_ticket.pdf", ImportRowKind.SAVED, ImportRowTap.OPEN_TRIP, "t1",
                route = "SNSI → BMT, 3 Aug", method = ExtractionMethod.OCR,
            ),
            ImportRowModel("2", "SC_SNSI_10Aug.pdf", ImportRowKind.SAVED_PAIRED, ImportRowTap.OPEN_TRIP, "t2"),
            ImportRowModel("3", "ERS_8751652729.pdf", ImportRowKind.DUPLICATE, ImportRowTap.OPEN_TRIP, "t3"),
            ImportRowModel("4", "scan_0812.pdf", ImportRowKind.NEEDS_REVIEW, ImportRowTap.EDIT_TICKET, "t4"),
            ImportRowModel("5", "IMG_2041.jpg", ImportRowKind.FAILED, ImportRowTap.NONE, failureReason = "Not a PDF"),
            ImportRowModel("6", "ticket_22Aug.pdf", ImportRowKind.READING, ImportRowTap.NONE),
            ImportRowModel("7", "ticket_29Aug.pdf", ImportRowKind.QUEUED, ImportRowTap.NONE),
        ),
        summary = ImportSummary(total = 7, finished = 5, saved = 2, duplicate = 1, needsReview = 1, failed = 1),
    )

@Preview(name = "Import light", showBackground = true, heightDp = 844)
@Composable
private fun ImportContentLightPreview() {
    TripcheckerTheme(darkTheme = false) { ImportContent(previewState, {}, {}, {}) }
}

@Preview(name = "Import dark", showBackground = true, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportContentDarkPreview() {
    TripcheckerTheme(darkTheme = true) { ImportContent(previewState, {}, {}, {}) }
}
