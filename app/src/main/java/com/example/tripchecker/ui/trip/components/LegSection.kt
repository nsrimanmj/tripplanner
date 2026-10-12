package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.Ticket
import com.example.tripchecker.ui.common.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegSection(
    leg: Ticket,
    onCopyPnr: (String) -> Unit,
    onGoToTicket: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onToggleCancelled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val titleStyle =
                if (leg.isCancelled) {
                    MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough)
                } else {
                    MaterialTheme.typography.titleMedium
                }
            Text(
                listOfNotNull(leg.trainNumber, leg.trainName).joinToString(" "),
                style = titleStyle,
            )
            RouteBlock(leg)
            leg.departureAt?.let { dep ->
                val arr = leg.arrivalAt
                val depLine = "${Formatters.formatDate(dep.atZone(com.example.tripchecker.extraction.parser.IrctcDates.IST).toLocalDate())} · ${Formatters.formatTime(dep)}"
                val arrLine =
                    arr?.let {
                        "${Formatters.formatDate(it.atZone(com.example.tripchecker.extraction.parser.IrctcDates.IST).toLocalDate())} · ${Formatters.formatTime(it)}"
                    }
                Text(stringResource(R.string.departure_label, depLine))
                if (arrLine != null) {
                    Text(stringResource(R.string.arrival_label, arrLine))
                    Text(stringResource(R.string.duration_label, Formatters.formatDuration(dep, arr)))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leg.travelClass?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                leg.quota?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                leg.distanceKm?.let { AssistChip(onClick = {}, label = { Text("$it km") }) }
                if (leg.isCancelled) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.cancelled)) })
                }
            }
            leg.pnr?.let { pnr ->
                TextButton(onClick = { onCopyPnr(pnr) }) {
                    Text(
                        stringResource(R.string.pnr_label, pnr),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            leg.passengers.forEach { PassengerRow(it) }
            leg.totalFarePaise?.let { Text(Formatters.formatMoneyPaise(it), style = MaterialTheme.typography.titleSmall) }
            if (leg.pdfFileName != null) {
                Button(onClick = onGoToTicket, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.go_to_ticket))
                }
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth(), enabled = leg.pdfFileName != null) {
                Text(stringResource(R.string.share_pdf))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = onToggleCancelled) {
                Text(
                    if (leg.isCancelled) stringResource(R.string.mark_active) else stringResource(R.string.mark_cancelled),
                )
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete_ticket))
            }
        }
    }
}

@Composable
private fun RouteBlock(leg: Ticket) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StationLine(stringResource(R.string.from_label), leg.from)
        if (leg.boarding != null && leg.boarding?.code != leg.from?.code) {
            StationLine(stringResource(R.string.boarding_label), leg.boarding)
        }
        StationLine(stringResource(R.string.to_label), leg.to)
    }
}

@Composable
private fun StationLine(label: String, station: com.example.tripchecker.domain.model.Station?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(station?.code ?: "—", style = MaterialTheme.typography.titleLarge)
        station?.name?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
