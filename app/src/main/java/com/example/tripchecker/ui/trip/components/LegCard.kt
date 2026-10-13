package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.JoinedColumn
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.common.components.TicketActionRow
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripShapes
import com.example.tripchecker.ui.trip.LegRole
import com.example.tripchecker.ui.trip.LegUi

/** Card for one ticket: overline, title, route timeline, chips, PNR, passengers, fare and actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegCard(
    leg: LegUi,
    onCopyPnr: (String) -> Unit,
    onGoToTicket: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onToggleCancelled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TripShapes.heroShape,
        color = colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.heroPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LegHeading(leg, onToggleCancelled, onDelete)
            if (leg.viaOcr) OcrBanner()
            RouteTimeline(leg.timeline)
            if (leg.classChip != null || leg.quotaChip != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    leg.classChip?.let { TagChip(it, ChipTone.NEUTRAL) }
                    leg.quotaChip?.let { TagChip(it, ChipTone.NEUTRAL) }
                }
            }
            leg.pnr?.let { pnr -> PnrBlock(pnr = pnr, onCopy = { onCopyPnr(pnr) }) }
            if (leg.passengers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text =
                            if (leg.passengers.size == 1) {
                                stringResource(R.string.trip_passenger)
                            } else {
                                stringResource(R.string.trip_passengers, leg.passengers.size)
                            },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    JoinedColumn {
                        leg.passengers.forEachIndexed { i, p ->
                            PassengerRow(p, shape = JoinedListDefaults.shape(i, leg.passengers.size))
                        }
                    }
                }
            }
            leg.totalFare?.let { fare ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.trip_total_fare),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        fare,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    )
                }
            }
            val hasPdf = leg.ticket.pdfFileName != null
            TicketActionRow(
                onOpen = if (hasPdf) onGoToTicket else null,
                onShare = if (hasPdf) onShare else null,
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun LegHeading(leg: LegUi, onToggleCancelled: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = overline(leg.role, leg.dateLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (leg.cancelled) {
                    TagChip(stringResource(R.string.cancelled), ChipTone.ERROR, small = true)
                }
            }
            Text(leg.title, style = MaterialTheme.typography.titleLarge)
        }
        TicketOptionsMenu(
            cancelled = leg.cancelled,
            onToggleCancelled = onToggleCancelled,
            onDelete = onDelete,
            modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
        )
    }
}

@Composable
private fun overline(role: LegRole, dateLabel: String?): String {
    val roleText =
        when (role) {
            LegRole.Outbound -> stringResource(R.string.trip_role_outbound)
            LegRole.Return -> stringResource(R.string.trip_role_return)
            LegRole.OneWay -> stringResource(R.string.trip_role_one_way)
            is LegRole.Numbered -> stringResource(R.string.trip_role_leg, role.number)
        }
    return if (dateLabel == null) roleText else stringResource(R.string.trip_overline, roleText, dateLabel)
}

@Composable
private fun TicketOptionsMenu(
    cancelled: Boolean,
    onToggleCancelled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.trip_ticket_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (cancelled) R.string.mark_active else R.string.mark_cancelled))
                },
                onClick = {
                    expanded = false
                    onToggleCancelled()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.trip_delete_ellipsis), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun OcrBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.ocr_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}
