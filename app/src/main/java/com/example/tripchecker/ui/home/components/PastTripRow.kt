package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.JourneyKind
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.IconTile
import com.example.tripchecker.ui.common.components.JoinedColumn
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.home.HomePreviewData
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.home.PastTripUi

/** Compact past-trip row: circular kind icon, title, subtitle, and a chevron or "Cancelled" chip. */
@Composable
fun PastTripRow(
    trip: PastTripUi,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val icon: Painter =
        if (trip.kind == JourneyKind.ONE_WAY) {
            rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        } else {
            painterResource(R.drawable.ic_swap_horiz)
        }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
    ) {
        Row(
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 68.dp)
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(
                painter = icon,
                contentDescription = null,
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurfaceVariant,
                circle = true,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (trip.cancelled) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (trip.cancelled) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trip.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trip.cancelled) {
                TagChip(stringResource(R.string.cancelled), ChipTone.ERROR)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Preview(name = "Past rows light", widthDp = 390)
@Composable
private fun PastTripRowLightPreview() = HomePreviewSurface(dark = false) { PastPreviewBody() }

@Preview(name = "Past rows dark", widthDp = 390)
@Composable
private fun PastTripRowDarkPreview() = HomePreviewSurface(dark = true) { PastPreviewBody() }

@Composable
private fun PastPreviewBody() {
    val rows = HomePreviewData.pastJuly
    JoinedColumn(modifier = Modifier.padding(12.dp)) {
        rows.forEachIndexed { i, row ->
            PastTripRow(row, JoinedListDefaults.shape(i, rows.size), onClick = {})
        }
    }
}
