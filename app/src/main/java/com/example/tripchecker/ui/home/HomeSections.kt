package com.example.tripchecker.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.common.components.SectionHeader
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.tripchecker.ui.home.components.HomeSubtitle
import com.example.tripchecker.ui.home.components.HomeSwipeToDeleteBox
import com.example.tripchecker.ui.home.components.NeedsReviewRow
import com.example.tripchecker.ui.home.components.NextJourneyHero
import com.example.tripchecker.ui.home.components.PastTripRow
import com.example.tripchecker.ui.home.components.TripGroupCard
import com.example.tripchecker.ui.theme.AppShapes
import com.example.tripchecker.ui.theme.Dimens

/** Click handlers used by the Home list sections. */
internal class HomeActions(
    val onOpenTrip: (String) -> Unit,
    val onEditTicket: (String) -> Unit,
    val onGoToTicket: () -> Unit,
    val onShareTicket: () -> Unit,
    val onLinkReturn: (String) -> Unit,
    val onDeleteTicket: (String) -> Unit,
)

private val HeaderToContent = 10.dp

/** Adds the Home sections (subtitle, hero, needs review, upcoming, past) to a LazyColumn. */
internal fun LazyListScope.homeSections(
    state: HomeUiState,
    actions: HomeActions,
) {
    var sections = 0

    fun nextTop(): Dp = if (sections++ == 0) 12.dp else Dimens.sectionGap

    state.subtitle?.let { subtitle ->
        item(key = "subtitle", contentType = "subtitle") { HomeSubtitle(subtitle) }
        sections = 1
    }

    state.nextJourney?.let { next ->
        val top = nextTop()
        item(key = "next-header", contentType = "header") {
            SectionHeader(stringResource(R.string.home_next_journey), Modifier.padding(top = top, bottom = HeaderToContent))
        }
        item(key = "next-hero", contentType = "hero") {
            NextJourneyHero(
                journey = next,
                onOpen = { actions.onOpenTrip(next.ticketId) },
                onGoToTicket = actions.onGoToTicket,
                onShare = actions.onShareTicket,
            )
        }
    }

    if (state.needsReview.isNotEmpty()) {
        val top = nextTop()
        item(key = "review-header", contentType = "header") {
            SectionHeader(
                title = stringResource(R.string.needs_review),
                modifier = Modifier.padding(top = top, bottom = HeaderToContent),
                count = state.needsReview.size,
            )
        }
        itemsIndexed(state.needsReview, key = { _, row -> "review-${row.ticketId}" }, contentType = { _, _ -> "review" }) { index, row ->
            HomeSwipeToDeleteBox(
                enabled = HomeSwipeDeletePolicy.isSwipeableNeedsReview(),
                onDeleteRequested = { actions.onDeleteTicket(row.ticketId) },
                backgroundShape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp),
            ) {
                NeedsReviewRow(
                    item = row,
                    onClick = { actions.onEditTicket(row.ticketId) },
                )
            }
        }
    }

    if (state.upcoming.isNotEmpty()) {
        val top = nextTop()
        item(key = "upcoming-header", contentType = "header") {
            SectionHeader(stringResource(R.string.upcoming), Modifier.padding(top = top, bottom = HeaderToContent))
        }
        itemsIndexed(state.upcoming, key = { _, card -> "card-${card.key}" }, contentType = { _, _ -> "card" }) { index, card ->
            val swipeable = HomeSwipeDeletePolicy.isSwipeableGroupKind(card.kind)
            HomeSwipeToDeleteBox(
                enabled = swipeable,
                onDeleteRequested = { actions.onDeleteTicket(card.anchorTicketId) },
                backgroundShape = AppShapes.extraLarge,
                modifier = Modifier.padding(top = if (index > 0) Dimens.cardGap else 0.dp),
            ) {
                TripGroupCard(
                    card = card,
                    onOpen = { actions.onOpenTrip(card.anchorTicketId) },
                    onLinkReturn = { actions.onLinkReturn(card.anchorTicketId) },
                )
            }
        }
    }

    if (state.pastByMonth.isNotEmpty()) {
        val top = nextTop()
        item(key = "past-header", contentType = "header") {
            SectionHeader(stringResource(R.string.home_past), Modifier.padding(top = top, bottom = 4.dp))
        }
        state.pastByMonth.forEachIndexed { monthIndex, month ->
            item(key = "month-${month.header}", contentType = "month") {
                Text(
                    text = month.header,
                    modifier = Modifier.padding(start = 8.dp, top = if (monthIndex == 0) 4.dp else 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            itemsIndexed(month.rows, key = { _, row -> "past-${row.key}" }, contentType = { _, _ -> "past" }) { index, row ->
                val rowShape = JoinedListDefaults.shape(index, month.rows.size)
                val swipeable = HomeSwipeDeletePolicy.isSwipeableGroupKind(row.kind)
                HomeSwipeToDeleteBox(
                    enabled = swipeable,
                    onDeleteRequested = { actions.onDeleteTicket(row.anchorTicketId) },
                    backgroundShape = rowShape,
                    modifier = Modifier.padding(top = if (index > 0) JoinedListDefaults.Gap else 0.dp),
                ) {
                    PastTripRow(
                        trip = row,
                        shape = rowShape,
                        onClick = { actions.onOpenTrip(row.anchorTicketId) },
                    )
                }
            }
        }
    }
}
