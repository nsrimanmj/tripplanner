package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.TicketActionRow
import com.example.tripchecker.ui.home.HomePreviewData
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.home.NextJourneyUi
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripShapes
import com.example.tripchecker.ui.theme.TripTypography

/**
 * Hero card for the next departing leg: overline + countdown, route row, train line, seat panel and actions.
 * Tapping the card body calls [onOpen]; the action row is hidden when the ticket has no PDF.
 */
@Composable
fun NextJourneyHero(
    journey: NextJourneyUi,
    onOpen: () -> Unit,
    onGoToTicket: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = TripShapes.heroShape,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.heroPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeroTopRow(journey)
            HeroRouteRow(journey)
            Text(
                text = journey.trainLine,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            journey.seat?.let { HeroSeatPanel(it) }
            if (journey.pdfFileName != null) {
                TicketActionRow(
                    onOpen = onGoToTicket,
                    onShare = onShare,
                    shareContainerColor = colors.surfaceContainerLowest,
                    shareContentColor = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun HeroTopRow(journey: NextJourneyUi) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = journey.overline,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
                maxLines = 1,
            )
            Text(text = journey.countdown, style = TripTypography.countdown, maxLines = 1)
        }
        Box(
            modifier = Modifier.size(52.dp).background(colors.primary, TripShapes.squareButtonShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_train),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun HeroRouteRow(journey: NextJourneyUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationColumn(
            code = journey.fromCode,
            name = journey.fromName,
            time = { Text(journey.departureTime, style = MaterialTheme.typography.titleMedium) },
            horizontalAlignment = Alignment.Start,
        )
        RouteConnector(journey.duration, Modifier.weight(1f))
        StationColumn(
            code = journey.toCode,
            name = journey.toName,
            time = {
                val text =
                    buildAnnotatedString {
                        append(journey.arrivalTime)
                        journey.dayOffset?.let {
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    baselineShift = BaselineShift.Superscript,
                                ),
                            ) { append(it) }
                        }
                    }
                Text(text, style = MaterialTheme.typography.titleMedium)
            },
            horizontalAlignment = Alignment.End,
        )
    }
}

@Composable
private fun StationColumn(
    code: String,
    name: String,
    time: @Composable () -> Unit,
    horizontalAlignment: Alignment.Horizontal,
) {
    Column(
        modifier = Modifier.widthIn(max = 132.dp),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(code, style = TripTypography.stationCodeHero, maxLines = 1, softWrap = false)
        if (name.isNotBlank()) {
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (horizontalAlignment == Alignment.End) TextAlign.End else TextAlign.Start,
            )
        }
        time()
    }
}

/** Hollow dot, line, arrow, line, filled dot, with the duration underneath. */
@Composable
private fun RouteConnector(
    duration: String?,
    modifier: Modifier = Modifier,
) {
    val content = LocalContentColor.current
    Column(
        modifier = modifier.padding(bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(8.dp).border(2.dp, content, CircleShape))
            ConnectorLine(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            ConnectorLine(Modifier.weight(1f))
            Box(Modifier.size(8.dp).background(content, CircleShape))
        }
        if (duration != null) {
            Text(duration, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 1)
        }
    }
}

@Composable
private fun ConnectorLine(modifier: Modifier) {
    Box(
        modifier
            .height(2.dp)
            .alpha(0.5f)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Preview(name = "Hero light", widthDp = 390)
@Composable
private fun NextJourneyHeroLightPreview() = HomePreviewSurface(dark = false) { HeroPreviewBody() }

@Preview(name = "Hero dark", widthDp = 390)
@Composable
private fun NextJourneyHeroDarkPreview() = HomePreviewSurface(dark = true) { HeroPreviewBody() }

@Composable
private fun HeroPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
        NextJourneyHero(HomePreviewData.hero, onOpen = {}, onGoToTicket = {}, onShare = {})
        NextJourneyHero(HomePreviewData.heroWaitlist, onOpen = {}, onGoToTicket = {}, onShare = {})
    }
}
