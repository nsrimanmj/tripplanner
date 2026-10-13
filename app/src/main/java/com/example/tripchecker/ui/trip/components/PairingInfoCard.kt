package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.theme.AppShapes
import com.example.tripchecker.ui.trip.PairingCardUi
import com.example.tripchecker.ui.trip.PairingVariant

/** Outlined footer card explaining how the legs are paired, with Unlink / Link return / Reset actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PairingInfoCard(
    card: PairingCardUi,
    onUnlink: () -> Unit,
    onLinkReturn: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.extraLarge,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val title =
                when (card.variant) {
                    PairingVariant.AUTO -> stringResource(R.string.trip_pairing_auto_title)
                    PairingVariant.MANUAL -> stringResource(R.string.trip_pairing_manual_title)
                    PairingVariant.ONE_WAY -> stringResource(R.string.trip_pairing_none_title)
                }
            val body =
                when (card.variant) {
                    PairingVariant.AUTO ->
                        if (card.destinationCity.isBlank()) {
                            stringResource(R.string.trip_pairing_auto_body_generic)
                        } else {
                            stringResource(R.string.trip_pairing_auto_body, card.destinationCity)
                        }
                    PairingVariant.MANUAL -> stringResource(R.string.trip_pairing_manual_body)
                    PairingVariant.ONE_WAY -> stringResource(R.string.trip_pairing_none_body)
                }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (card.showLinkReturn) {
                    FilledTonalButton(onClick = onLinkReturn) {
                        Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.trip_link_return_button),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (card.showUnlink) {
                    OutlinedButton(onClick = onUnlink) {
                        Icon(painterResource(R.drawable.ic_link_off), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.trip_unlink_tickets),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (card.showReset) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.reset_pairing)) }
                }
            }
        }
    }
}
