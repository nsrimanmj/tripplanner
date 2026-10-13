package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.theme.TripTypography
import com.example.tripchecker.ui.trip.TimelineStopUi
import com.example.tripchecker.ui.trip.TimelineUi

private val TimeColumn = 64.dp
private val RailColumn = 20.dp
private val RailWidth = 2.dp
private val DotSize = 14.dp
private val DotTopInset = 7.dp

/**
 * Three-column route timeline (time, rail, station) with a hollow departure ring, a filled arrival dot
 * and a middle chip. Handles the boarding-differs and arrival-not-available variants.
 */
@Composable
fun RouteTimeline(timeline: TimelineUi, modifier: Modifier = Modifier) {
    val rail = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier.fillMaxWidth()) {
        TimelineRow(modifier = Modifier.heightIn(min = 56.dp), time = {
            TimeBlock(timeline.departure.time, timeline.departure.date)
        }, rail = {
            Spacer(Modifier.height(DotTopInset))
            Box(
                Modifier.size(DotSize).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
            Box(Modifier.weight(1f).width(RailWidth).background(rail))
        }) {
            StationBlock(timeline.departure) {
                timeline.bookedFromCode?.let { code ->
                    TagChip(
                        text = stringResource(R.string.trip_boarding_here, code),
                        tone = ChipTone.PRIMARY,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                    )
                }
            }
        }
        TimelineRow(modifier = Modifier.heightIn(min = 52.dp), time = {}, rail = {
            Box(Modifier.weight(1f).width(RailWidth).background(rail))
        }) {
            timeline.middleLabel?.let { label ->
                Box(Modifier.fillMaxHeight().padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    TagChip(
                        text = label,
                        tone = ChipTone.NEUTRAL,
                        leadingIcon =
                            if (!timeline.arrivalNotAvailable && timeline.departure.time != null) {
                                painterResource(R.drawable.ic_schedule)
                            } else {
                                null
                            },
                    )
                }
            }
        }
        TimelineRow(
            modifier = Modifier,
            time = {
                if (timeline.arrivalNotAvailable) {
                    TimeBlock(null, stringResource(R.string.trip_arrival_na))
                } else {
                    TimeBlock(timeline.arrival.time, timeline.arrival.date)
                }
            },
            rail = {
                Box(Modifier.width(RailWidth).height(DotTopInset).background(rail))
                Box(Modifier.size(DotSize).background(MaterialTheme.colorScheme.primary, CircleShape))
            },
        ) {
            StationBlock(timeline.arrival)
        }
    }
}

@Composable
private fun TimelineRow(
    modifier: Modifier,
    time: @Composable () -> Unit,
    rail: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
    station: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(TimeColumn)) { time() }
        Column(
            modifier = Modifier.width(RailColumn).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = rail,
        )
        Column(Modifier.weight(1f)) { station() }
    }
}

@Composable
private fun TimeBlock(time: String?, date: String?) {
    Text(
        time ?: "—",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        softWrap = false,
    )
    if (date != null) {
        Text(
            date,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StationBlock(stop: TimelineStopUi, extra: @Composable () -> Unit = {}) {
    Text(
        stop.code,
        style = TripTypography.stationCode,
        maxLines = 1,
        softWrap = false,
    )
    if (stop.name.isNotBlank()) {
        Text(
            stop.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    extra()
}
