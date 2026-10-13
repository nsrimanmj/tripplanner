package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.components.IconTile
import com.example.tripchecker.ui.home.HomePreviewData
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.home.NeedsReviewUi

/** Tertiary row for a ticket that needs manual completion; tapping opens its edit form. */
@Composable
fun NeedsReviewRow(
    item: NeedsReviewUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val title =
        if (item.fileName != null) {
            stringResource(R.string.home_needs_review_title, item.fileName)
        } else {
            stringResource(R.string.home_needs_review_title_unnamed)
        }
    val subtitle = subtitleFor(item)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 72.dp)
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(
                painter = rememberVectorPainter(Icons.Default.Edit),
                contentDescription = null,
                containerColor = colors.tertiary,
                contentColor = colors.onTertiary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun subtitleFor(item: NeedsReviewUi): String {
    if (item.missing.isEmpty()) return stringResource(R.string.home_needs_review_subtitle)
    val labels = item.missing.map { stringResource(missingLabel(it)) }.joinToString(", ")
    val listed =
        if (item.moreMissing > 0) {
            stringResource(R.string.home_needs_review_more, labels, item.moreMissing)
        } else {
            labels
        }
    return stringResource(R.string.home_needs_review_subtitle_missing, listed)
}

private fun missingLabel(field: TicketField): Int =
    when (field) {
        TicketField.PNR -> R.string.home_missing_pnr
        TicketField.TRAIN_NUMBER -> R.string.home_missing_train_number
        TicketField.FROM -> R.string.home_missing_from
        TicketField.TO -> R.string.home_missing_to
        else -> R.string.home_missing_departure
    }

@Preview(name = "Needs review light", widthDp = 390)
@Composable
private fun NeedsReviewLightPreview() = HomePreviewSurface(dark = false) { NeedsReviewPreviewBody() }

@Preview(name = "Needs review dark", widthDp = 390)
@Composable
private fun NeedsReviewDarkPreview() = HomePreviewSurface(dark = true) { NeedsReviewPreviewBody() }

@Composable
private fun NeedsReviewPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
        NeedsReviewRow(HomePreviewData.needsReview, onClick = {})
        NeedsReviewRow(HomePreviewData.needsReviewMany, onClick = {})
    }
}
