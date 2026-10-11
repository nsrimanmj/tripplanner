package com.example.tripchecker.extraction

import com.example.tripchecker.extraction.model.TextLine
import org.junit.Assert.assertEquals
import org.junit.Test

class RowMergerTest {
    @Test
    fun `merges three station cells in reading order`() {
        val lines =
            listOf(
                TextLine("BEGAMPET (BMT)", 400f, 100f, 500f, 120f),
                TextLine("SAINAGAR SHIRDI (SNSI)", 0f, 100f, 180f, 120f),
                TextLine("SAINAGAR SHIRDI (SNSI)", 200f, 100f, 380f, 120f),
            )
        val merged = RowMerger.merge(lines)
        assertEquals(
            "SAINAGAR SHIRDI (SNSI)  SAINAGAR SHIRDI (SNSI)  BEGAMPET (BMT)",
            merged,
        )
    }

    @Test
    fun `tolerates vertical jitter`() {
        val lines =
            listOf(
                TextLine("A", 0f, 100f, 20f, 120f),
                TextLine("B", 30f, 104f, 50f, 124f),
            )
        assertEquals("A  B", RowMerger.merge(lines))
    }

    @Test
    fun `keeps separate rows`() {
        val lines =
            listOf(
                TextLine("row1", 0f, 0f, 40f, 20f),
                TextLine("row2", 0f, 80f, 40f, 100f),
            )
        assertEquals("row1\nrow2", RowMerger.merge(lines))
    }
}
