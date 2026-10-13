package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.ui.common.StatusTones
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.DateBadge
import com.example.tripchecker.ui.common.components.JoinedColumn
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.common.components.toChipTone
import com.example.tripchecker.ui.home.HomePreviewData
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.home.LegRowUi
import com.example.tripchecker.ui.home.LegSeatKind
import com.example.tripchecker.ui.home.TripCardUi
import com.example.tripchecker.ui.theme.AppShapes
import com.example.tripchecker.ui.theme.Dimens

/**
 * Upcoming / in-progress group card: kind chip + date range, city title (round trip and custom),
 * joined leg rows, and for one-way trips a "No return yet / Link return" footer.
 */
@Composable
fun TripGroupCard(
    card: TripCardUi,
    onOpen: () -> Unit,
    onLinkReturn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val openLabel = stringResource(R.string.home_open_trip)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.extraLarge,
        color = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            Column(
                modifier = Modifier.clickable(onClickLabel = openLabel, onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CardHeaderRow(card)
                card.title?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                JoinedColumn {
                    card.legs.forEachIndexed { index, leg ->
                        LegRow(leg, JoinedListDefaults.shape(index, card.legs.size))
                    }
                }
            }
            if (card.kind == JourneyKind.ONE_WAY) {
                OneWayFooter(onLinkReturn)
            }
        }
    }
}

@Composable
private fun CardHeaderRow(card: TripCardUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when (card.kind) {
                JourneyKind.ROUND_TRIP ->
                    TagChip(
                        text = stringResource(R.string.round_trip),
                        tone = ChipTone.SECONDARY,
                        leadingIcon = painterResource(R.drawable.ic_swap_horiz),
                    )
                JourneyKind.ONE_WAY -> TagChip(stringResource(R.string.one_way), ChipTone.OUTLINED)
                JourneyKind.CUSTOM -> TagChip(stringResource(R.string.custom_trip), ChipTone.OUTLINED)
            }
            if (card.inProgress) {
                TagChip(stringResource(R.string.journey_in_progress), ChipTone.PRIMARY, showDot = true)
            }
        }
        Text(
            text = card.dateRange,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegRow(
    leg: LegRowUi,
    shape: Shape,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leg.date != null) {
                DateBadge(leg.date, highlighted = leg.highlighted)
            } else {
                Box(Modifier.width(48.dp).height(56.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = leg.route,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (leg.cancelled) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (leg.cancelled) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = leg.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LegTrailing(leg)
        }
    }
}

@Composable
private fun LegTrailing(leg: LegRowUi) {
    val colors = MaterialTheme.colorScheme
    when {
        leg.cancelled -> TagChip(stringResource(R.string.cancelled), ChipTone.ERROR)
        leg.seat.kind == LegSeatKind.STATUS_CHIP ->
            TagChip(leg.seat.primary, StatusTones.toneOf(leg.seat.statusKind).toChipTone())
        leg.seat.kind == LegSeatKind.SEAT ->
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    leg.seat.primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                val sub =
                    if (leg.seat.othersCount > 0) {
                        pluralStringResource(R.plurals.home_other_passengers, leg.seat.othersCount, leg.seat.othersCount)
                    } else {
                        leg.seat.berthType
                    }
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
        else -> Unit
    }
}

@Composable
private fun OneWayFooter(onLinkReturn: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.home_no_return_yet),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onLinkReturn) {
            Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(6.dp))
            Text(stringResource(R.string.home_link_return), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Preview(name = "Trip cards light", widthDp = 390)
@Composable
private fun TripGroupCardLightPreview() = HomePreviewSurface(dark = false) { CardsPreviewBody() }

@Preview(name = "Trip cards dark", widthDp = 390)
@Composable
private fun TripGroupCardDarkPreview() = HomePreviewSurface(dark = true) { CardsPreviewBody() }

@Composable
private fun CardsPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
        TripGroupCard(HomePreviewData.roundTripCard, onOpen = {}, onLinkReturn = {})
        TripGroupCard(HomePreviewData.oneWayWaitlistCard, onOpen = {}, onLinkReturn = {})
        TripGroupCard(HomePreviewData.customInProgressCard, onOpen = {}, onLinkReturn = {})
    }
}
