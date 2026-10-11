package com.example.tripchecker.ui.importflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.importer.ImportItem
import com.example.tripchecker.importer.ImportItemState

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onDone: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onEditTicket: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImportContent(
        state = state,
        onDone = {
            viewModel.clearFinished()
            onDone()
        },
        onItemClick = { item ->
            when (val s = item.state) {
                is ImportItemState.Saved -> onOpenTrip(s.ticketId)
                is ImportItemState.Updated -> onOpenTrip(s.ticketId)
                is ImportItemState.NeedsReview -> onEditTicket(s.ticketId)
                else -> Unit
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContent(
    state: ImportUiState,
    onDone: () -> Unit,
    onItemClick: (ImportItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_screen_title)) },
                actions = {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.done))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.summary.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = state.summary,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(state.items) { item ->
                    ImportRow(item, onItemClick)
                }
            }
        }
    }
}

@Composable
private fun ImportRow(item: ImportItem, onItemClick: (ImportItem) -> Unit) {
    val statusText =
        when (val s = item.state) {
            ImportItemState.Queued -> stringResource(R.string.import_queued)
            ImportItemState.Reading -> stringResource(R.string.import_reading)
            is ImportItemState.Saved -> stringResource(R.string.import_saved)
            is ImportItemState.Updated -> stringResource(R.string.import_updated)
            is ImportItemState.Duplicate -> stringResource(R.string.import_duplicate)
            is ImportItemState.NeedsReview -> stringResource(R.string.import_needs_review)
            is ImportItemState.Failed -> s.reason
        }
    val clickable =
        item.state is ImportItemState.Saved ||
            item.state is ImportItemState.Updated ||
            item.state is ImportItemState.NeedsReview
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth().then(
                if (clickable) Modifier.clickable { onItemClick(item) } else Modifier,
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.displayName)
            if (item.state is ImportItemState.Reading || item.state is ImportItemState.Queued) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
            Text(statusText)
        }
    }
}
