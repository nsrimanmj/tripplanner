package com.example.tripchecker.ui.common.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.ui.theme.TripcheckerTheme
import java.time.LocalDate

@Composable
private fun ComponentsGallery() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.background(colors.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeader("Upcoming", count = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TagChip("Round trip", ChipTone.SECONDARY, leadingIcon = painterResource(R.drawable.ic_swap_horiz))
            TagChip("Upcoming", ChipTone.PRIMARY, showDot = true)
            TagChip("Auto-paired", ChipTone.OUTLINED)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TagChip("Third AC", ChipTone.NEUTRAL)
            TagChip("Review", ChipTone.TERTIARY)
            TagChip("Cancelled", ChipTone.ERROR)
            TagChip("Today", ChipTone.FILLED_PRIMARY)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(BerthStatus("CNF", "B4", 33, "Lower"), StatusChipVariant.HERO)
            StatusChip(BerthStatus("RAC"), StatusChipVariant.HERO)
            StatusChip(BerthStatus("RLWL", waitlistNumber = 5), StatusChipVariant.HERO)
            StatusChip(BerthStatus("CAN"), StatusChipVariant.HERO)
            StatusChip(null, StatusChipVariant.HERO)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(BerthStatus("CNF"), StatusChipVariant.ROW)
            StatusChip(BerthStatus("RAC"), StatusChipVariant.ROW)
            StatusChip(BerthStatus("GNWL"), StatusChipVariant.ROW)
            StatusChip(BerthStatus("CAN"), StatusChipVariant.ROW)
            StatusChip(BerthStatus("XYZ"), StatusChipVariant.ROW)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateBadge(LocalDate.of(2026, 8, 3), highlighted = true)
            DateBadge(LocalDate.of(2026, 8, 10), highlighted = false)
            IconTile(painterResource(R.drawable.ic_train), null, colors.primaryContainer, colors.onPrimaryContainer)
            IconTile(
                painterResource(R.drawable.ic_event), null,
                colors.secondaryContainer, colors.onSecondaryContainer, circle = true,
            )
        }
        JoinedColumn {
            repeat(3) { i ->
                androidx.compose.material3.Surface(
                    shape = JoinedListDefaults.shape(i, 3),
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Joined item ${i + 1}", Modifier.padding(16.dp)) }
            }
        }
        TicketActionRow(onOpen = {}, onShare = {}, onEdit = {})
        TicketActionRow(onOpen = null, onShare = null, onEdit = {})
    }
}

@Preview(name = "Components light", widthDp = 380)
@Composable
private fun ComponentsLightPreview() {
    TripcheckerTheme(darkTheme = false) { ComponentsGallery() }
}

@Preview(name = "Components dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsDarkPreview() {
    TripcheckerTheme(darkTheme = true) { ComponentsGallery() }
}
