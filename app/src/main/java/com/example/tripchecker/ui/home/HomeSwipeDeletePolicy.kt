package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.model.JourneyKind

/** Which Home list rows support swipe-to-delete (single-ticket rows only). */
object HomeSwipeDeletePolicy {
    /** Needs-review rows always represent one ticket. */
    fun isSwipeableNeedsReview(): Boolean = true

    /** Upcoming and past rows are swipeable only for one-way groups. */
    fun isSwipeableGroupKind(kind: JourneyKind): Boolean = kind == JourneyKind.ONE_WAY
}
