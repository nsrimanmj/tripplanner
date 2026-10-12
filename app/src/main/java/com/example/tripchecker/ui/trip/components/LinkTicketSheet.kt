package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.common.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkTicketSheet(
    visible: Boolean,
    candidates: List<Ticket>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.link_return_title))
            if (candidates.isEmpty()) {
                Text(stringResource(R.string.link_return_empty))
            } else {
                LazyColumn {
                    items(candidates, key = { it.id }) { ticket ->
                        val dep = ticket.departureAt?.let { Formatters.formatTime(it) } ?: "—"
                        Text(
                            modifier =
                                Modifier.fillMaxWidth().clickable { onSelect(ticket.id) }.padding(vertical = 12.dp),
                            text = "${ticket.origin?.code} → ${ticket.to?.code} · $dep · ${ticket.trainNumber ?: ""}",
                        )
                    }
                }
            }
        }
    }
}
