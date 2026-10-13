package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.StatusChip
import com.example.tripchecker.ui.common.components.StatusChipVariant
import com.example.tripchecker.ui.home.HomePreviewData
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.home.SeatPanelUi
import com.example.tripchecker.ui.theme.TripTypography

/**
 * "Your seat" card of the hero: coach / berth / type columns, or one large value for RAC and waitlist,
 * with the status chip on the right of the label.
 */
@Composable
fun HeroSeatPanel(
    seat: SeatPanelUi,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val label =
        when {
            seat.passengerName.isBlank() -> stringResource(R.string.home_hero_seat_label)
            seat.othersCount > 0 ->
                stringResource(R.string.home_hero_seat_label_more, seat.passengerName, seat.othersCount)
            else -> stringResource(R.string.home_hero_seat_label_named, seat.passengerName)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(seat.status, StatusChipVariant.HERO)
            }
            if (seat.bigValue != null) {
                Text(seat.bigValue, style = TripTypography.seatValue, maxLines = 1)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeatColumn(stringResource(R.string.home_seat_coach), seat.coach, Modifier.weight(1f))
                    SeatColumn(stringResource(R.string.home_seat_berth), seat.berth, Modifier.weight(1f))
                    SeatColumn(stringResource(R.string.home_seat_type), seat.berthType, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SeatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TripTypography.seatValue, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(name = "Seat panel light", widthDp = 360)
@Composable
private fun HeroSeatPanelLightPreview() = HomePreviewSurface(dark = false) { SeatPanelPreviewBody() }

@Preview(name = "Seat panel dark", widthDp = 360)
@Composable
private fun HeroSeatPanelDarkPreview() = HomePreviewSurface(dark = true) { SeatPanelPreviewBody() }

@Composable
private fun SeatPanelPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
        HeroSeatPanel(HomePreviewData.seatConfirmed)
        HeroSeatPanel(HomePreviewData.seatWaitlist)
    }
}
