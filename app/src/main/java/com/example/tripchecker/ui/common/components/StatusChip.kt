package com.example.tripchecker.ui.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.BerthStatus
import com.example.tripchecker.ui.common.StatusKind
import com.example.tripchecker.ui.common.StatusTones

/** Where a [StatusChip] is shown: the large hero panel or a compact passenger row. */
enum class StatusChipVariant { HERO, ROW }

/**
 * Chip for a passenger's current [status], coloured via [StatusTones].
 * HERO shows "Confirmed" with a check icon; ROW shows the compact 20dp "CNF" form.
 */
@Composable
fun StatusChip(
    status: BerthStatus?,
    variant: StatusChipVariant,
    modifier: Modifier = Modifier,
) {
    val kind = StatusTones.kindOf(status)
    val tone = StatusTones.toneOf(kind).toChipTone()
    val hero = variant == StatusChipVariant.HERO
    val rawCode = status?.code?.trim().orEmpty()
    val label = when (kind) {
        StatusKind.CONFIRMED -> stringResource(if (hero) R.string.common_status_confirmed else R.string.common_status_cnf)
        StatusKind.RAC -> stringResource(R.string.common_status_rac)
        StatusKind.WAITLIST -> stringResource(R.string.common_status_waitlist)
        StatusKind.CANCELLED -> stringResource(R.string.common_status_cancelled)
        StatusKind.UNKNOWN -> rawCode.ifEmpty { stringResource(R.string.common_status_unknown) }
    }
    val icon = if (hero && kind == StatusKind.CONFIRMED) rememberVectorPainter(Icons.Default.Check) else null
    TagChip(
        text = label,
        tone = tone,
        modifier = modifier,
        leadingIcon = icon,
        small = !hero,
    )
}
