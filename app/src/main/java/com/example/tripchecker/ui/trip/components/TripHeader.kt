package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.trip.PairingChip
import com.example.tripchecker.ui.trip.TripHeaderUi
import com.example.tripchecker.ui.trip.TripStatusLabel

/** Kind / status / pairing chips, route title and subtitle at the top of trip detail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripHeader(header: TripHeaderUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.headerGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (header.kind) {
                JourneyKind.ROUND_TRIP ->
                    TagChip(
                        text = stringResource(R.string.round_trip),
                        tone = ChipTone.SECONDARY,
                        leadingIcon = painterResource(R.drawable.ic_swap_horiz),
                    )
                JourneyKind.ONE_WAY -> TagChip(stringResource(R.string.one_way), ChipTone.OUTLINED)
                JourneyKind.CUSTOM -> TagChip(stringResource(R.string.custom_trip), ChipTone.OUTLINED)
            }
            val statusText =
                stringResource(
                    when (header.status) {
                        TripStatusLabel.UPCOMING -> R.string.trip_status_upcoming
                        TripStatusLabel.IN_PROGRESS -> R.string.trip_status_in_progress
                        TripStatusLabel.PAST -> R.string.trip_status_past
                    },
                )
            TagChip(
                text = statusText,
                tone = if (header.status == TripStatusLabel.PAST) ChipTone.NEUTRAL else ChipTone.PRIMARY,
                showDot = true,
            )
            header.pairingChip?.let { chip ->
                TagChip(
                    text =
                        stringResource(
                            if (chip == PairingChip.AUTO_PAIRED) {
                                R.string.trip_pairing_chip_auto
                            } else {
                                R.string.trip_pairing_chip_manual
                            },
                        ),
                    tone = ChipTone.OUTLINED,
                )
            }
        }
        Text(
            header.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )
        if (header.subtitle.isNotBlank()) {
            Text(
                header.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
