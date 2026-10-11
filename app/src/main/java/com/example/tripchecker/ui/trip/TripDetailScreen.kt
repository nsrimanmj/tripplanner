package com.example.tripchecker.ui.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.tripchecker.ui.common.Formatters

@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripDetailContent(state, onBack, onEdit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailContent(
    state: TripDetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
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
    ) { padding ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(16.dp),
        ) {
            val group = state.group
            if (group == null) {
                Text(stringResource(R.string.trip_not_found))
                return@Column
            }
            group.legs.forEach { leg ->
                Text("${leg.trainNumber ?: ""} ${leg.trainName ?: ""}")
                Text("${leg.origin?.code} → ${leg.to?.code}")
                leg.departureAt?.let { Text(Formatters.formatTime(it)) }
                Text("PNR: ${leg.pnr ?: "—"}")
                TextButton(onClick = { onEdit(leg.id) }) {
                    Text(stringResource(R.string.edit))
                }
            }
        }
    }
}
