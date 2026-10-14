package com.example.tripchecker.ui.home

import com.example.tripchecker.domain.model.JourneyKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSwipeDeletePolicyTest {
    @Test
    fun `needs review rows are swipeable`() {
        assertTrue(HomeSwipeDeletePolicy.isSwipeableNeedsReview())
    }

    @Test
    fun `only one-way group rows are swipeable`() {
        assertTrue(HomeSwipeDeletePolicy.isSwipeableGroupKind(JourneyKind.ONE_WAY))
        assertFalse(HomeSwipeDeletePolicy.isSwipeableGroupKind(JourneyKind.ROUND_TRIP))
        assertFalse(HomeSwipeDeletePolicy.isSwipeableGroupKind(JourneyKind.CUSTOM))
    }
}
