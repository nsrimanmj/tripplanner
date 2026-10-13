package com.example.tripchecker.ui.importflow.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.importflow.ImportSummary
import com.example.tripchecker.ui.theme.TripcheckerTheme

/** Card with "X of Y", the reading status, a determinate progress bar and non-zero count chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportSummaryCard(summary: ImportSummary, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val progressDescription = stringResource(R.string.import_summary_progress_description)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.import_summary_progress, summary.finished, summary.total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (summary.inProgress) {
                            R.string.import_summary_reading_one_at_a_time
                        } else {
                            R.string.import_summary_all_done
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            LinearProgressIndicator(
                progress = { summary.fraction },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = progressDescription },
                color = colors.primary,
                trackColor = colors.secondaryContainer,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (summary.saved > 0) {
                    TagChip(stringResource(R.string.import_chip_saved, summary.saved), ChipTone.PRIMARY)
                }
                if (summary.updated > 0) {
                    TagChip(stringResource(R.string.import_chip_updated, summary.updated), ChipTone.PRIMARY)
                }
                if (summary.duplicate > 0) {
                    TagChip(stringResource(R.string.import_chip_duplicate, summary.duplicate), ChipTone.NEUTRAL)
                }
                if (summary.needsReview > 0) {
                    TagChip(
                        stringResource(R.string.import_chip_needs_review, summary.needsReview),
                        ChipTone.TERTIARY,
                    )
                }
                if (summary.failed > 0) {
                    TagChip(stringResource(R.string.import_chip_failed, summary.failed), ChipTone.ERROR)
                }
            }
        }
    }
}

private val previewSummary =
    ImportSummary(total = 7, finished = 5, saved = 2, duplicate = 1, needsReview = 1, failed = 1)

@Preview(name = "Summary light", showBackground = true)
@Composable
private fun ImportSummaryCardLightPreview() {
    TripcheckerTheme(darkTheme = false) { ImportSummaryCard(previewSummary, Modifier.padding(12.dp)) }
}

@Preview(name = "Summary dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportSummaryCardDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        ImportSummaryCard(previewSummary.copy(finished = 7), Modifier.padding(12.dp))
    }
}
