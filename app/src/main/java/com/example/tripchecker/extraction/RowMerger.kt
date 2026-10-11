package com.example.tripchecker.extraction

import com.example.tripchecker.extraction.model.TextLine

/** Merges OCR lines into reading-order rows with two-space column gaps. */
object RowMerger {
    fun merge(lines: List<TextLine>): String {
        if (lines.isEmpty()) return ""
        val sorted = lines.sortedBy { it.centerY }
        val heights = sorted.map { it.height }.filter { it > 0 }
        val medianHeight =
            if (heights.isEmpty()) {
                10f
            } else {
                heights.sorted()[heights.size / 2]
            }
        val threshold = medianHeight * 0.5f
        val rows = mutableListOf<MutableList<TextLine>>()
        val rowCenters = mutableListOf<Float>()
        for (line in sorted) {
            val cy = line.centerY
            val rowIndex =
                rowCenters.indexOfFirst { kotlin.math.abs(it - cy) <= threshold }
            if (rowIndex >= 0) {
                rows[rowIndex].add(line)
                rowCenters[rowIndex] = rows[rowIndex].map { it.centerY }.average().toFloat()
            } else {
                rows.add(mutableListOf(line))
                rowCenters.add(cy)
            }
        }
        return rows
            .map { row ->
                row.sortedBy { it.left }.joinToString("  ") { it.text.trim() }
            }
            .joinToString("\n")
    }
}
