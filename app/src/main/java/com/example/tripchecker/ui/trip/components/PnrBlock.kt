package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.Formatters
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripTypography

/** PNR label with the grouped number in monospace and a 48dp copy button; the caller copies the raw (ungrouped) PNR in [onCopy]. */
@Composable
fun PnrBlock(pnr: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.trip_pnr_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Formatters.formatPnr(pnr),
                    style = TripTypography.pnr,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Surface(
                onClick = onCopy,
                modifier = Modifier.size(Dimens.minTouch),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_content_copy),
                        contentDescription = stringResource(R.string.trip_copy_pnr),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
