package com.example.tripchecker.ui.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.example.tripchecker.ui.common.StatusTone
import com.example.tripchecker.ui.theme.TripShapes

/** Colour tone of a [TagChip], each mapped to an M3 container role. */
enum class ChipTone { PRIMARY, SECONDARY, TERTIARY, ERROR, NEUTRAL, OUTLINED, FILLED_PRIMARY }

/** Maps a pure [StatusTone] to the matching [ChipTone]. */
fun StatusTone.toChipTone(): ChipTone =
    when (this) {
        StatusTone.PRIMARY -> ChipTone.PRIMARY
        StatusTone.SECONDARY -> ChipTone.SECONDARY
        StatusTone.TERTIARY -> ChipTone.TERTIARY
        StatusTone.ERROR -> ChipTone.ERROR
        StatusTone.NEUTRAL -> ChipTone.NEUTRAL
    }

/**
 * Non-interactive 28dp tag (corner 10). With [small] it is a 20dp, corner 6 chip using labelSmall.
 * [leadingIcon] and [showDot] are optional leading decorations.
 */
@Composable
fun TagChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    showDot: Boolean = false,
    small: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (tone) {
        ChipTone.PRIMARY -> colors.primaryContainer
        ChipTone.SECONDARY -> colors.secondaryContainer
        ChipTone.TERTIARY -> colors.tertiaryContainer
        ChipTone.ERROR -> colors.errorContainer
        ChipTone.NEUTRAL -> colors.surfaceContainerHighest
        ChipTone.OUTLINED -> androidx.compose.ui.graphics.Color.Transparent
        ChipTone.FILLED_PRIMARY -> colors.primary
    }
    val content = when (tone) {
        ChipTone.PRIMARY -> colors.onPrimaryContainer
        ChipTone.SECONDARY -> colors.onSecondaryContainer
        ChipTone.TERTIARY -> colors.onTertiaryContainer
        ChipTone.ERROR -> colors.onErrorContainer
        ChipTone.NEUTRAL -> colors.onSurface
        ChipTone.OUTLINED -> colors.onSurfaceVariant
        ChipTone.FILLED_PRIMARY -> colors.onPrimary
    }
    val border = if (tone == ChipTone.OUTLINED) BorderStroke(1.dp, colors.outlineVariant) else null
    val shape = RoundedCornerShape(if (small) TripShapes.joinedInner else 10.dp)
    val textStyle = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
    Surface(
        modifier = modifier,
        shape = shape,
        color = container,
        contentColor = content,
        border = border,
    ) {
        Row(
            modifier = Modifier
                .height(if (small) 20.dp else 28.dp)
                .padding(
                    start = if (small) 6.dp else if (leadingIcon != null || showDot) 8.dp else 12.dp,
                    end = if (small) 6.dp else 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showDot) {
                Box(Modifier.size(8.dp).background(content, CircleShape))
            }
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text, style = textStyle, maxLines = 1)
        }
    }
}
