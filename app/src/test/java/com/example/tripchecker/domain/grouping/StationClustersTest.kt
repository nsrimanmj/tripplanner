package com.example.tripchecker.domain.grouping

import org.junit.Assert.assertEquals
import org.junit.Test

class StationClustersTest {
    @Test
    fun `BMT and SC share Hyderabad city`() {
        assertEquals(StationClusters.cityOf("BMT"), StationClusters.cityOf("SC"))
    }

    @Test
    fun `unknown code maps to itself`() {
        assertEquals("XYZ", StationClusters.cityOf("XYZ"))
    }
}
