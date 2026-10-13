package com.example.tripchecker.ui.importflow.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.ui.common.components.IconTile
import com.example.tripchecker.ui.common.components.JoinedColumn
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.importflow.ImportRowKind
import com.example.tripchecker.ui.importflow.ImportRowModel
import com.example.tripchecker.ui.importflow.ImportRowTap
import com.example.tripchecker.ui.theme.TripShapes
import com.example.tripchecker.ui.theme.TripcheckerTheme

/** One import row: 40dp state tile, one-line file name and a state subtitle; tappable per [ImportRowModel.tap]. */
@Composable
fun ImportItemRow(
    row: ImportRowModel,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val rowModifier = modifier.fillMaxWidth()
    Surface(
        modifier = if (row.clickable) rowModifier.clickable(onClick = onClick) else rowModifier,
        shape = shape,
        color = colors.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StateTile(row.kind)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = row.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Subtitle(row)
            }
            if (row.showChevron) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun StateTile(kind: ImportRowKind) {
    val colors = MaterialTheme.colorScheme
    when (kind) {
        ImportRowKind.READING ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.secondaryContainer, RoundedCornerShape(TripShapes.tile)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = colors.primary,
                )
            }
        ImportRowKind.SAVED, ImportRowKind.SAVED_PAIRED, ImportRowKind.UPDATED ->
            IconTile(
                rememberVectorPainter(Icons.Default.Check), null,
                colors.primaryContainer, colors.onPrimaryContainer,
            )
        ImportRowKind.DUPLICATE ->
            IconTile(
                painterResource(R.drawable.ic_content_copy), null,
                colors.surfaceContainerHighest, colors.onSurfaceVariant,
            )
        ImportRowKind.NEEDS_REVIEW ->
            IconTile(
                rememberVectorPainter(Icons.Default.Edit), null,
                colors.tertiaryContainer, colors.onTertiaryContainer,
            )
        ImportRowKind.FAILED ->
            IconTile(
                painterResource(R.drawable.ic_error), null,
                colors.errorContainer, colors.onErrorContainer,
            )
        ImportRowKind.QUEUED ->
            IconTile(
                painterResource(R.drawable.ic_hourglass), null,
                colors.surfaceContainerHighest, colors.onSurfaceVariant,
            )
    }
}

@Composable
private fun Subtitle(row: ImportRowModel) {
    val colors = MaterialTheme.colorScheme
    val color =
        when (row.kind) {
            ImportRowKind.READING -> colors.primary
            ImportRowKind.FAILED -> colors.error
            else -> colors.onSurfaceVariant
        }
    Text(
        text = subtitleText(row),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (row.kind == ImportRowKind.READING) FontWeight.SemiBold else null,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun subtitleText(row: ImportRowModel): String =
    when (row.kind) {
        ImportRowKind.QUEUED -> stringResource(R.string.import_queued)
        ImportRowKind.READING -> stringResource(R.string.import_reading)
        ImportRowKind.SAVED -> {
            var text = stringResource(R.string.import_saved)
            row.route?.let { text = stringResource(R.string.import_separator, text, it) }
            val via =
                when (row.method) {
                    ExtractionMethod.OCR -> stringResource(R.string.import_row_via_ocr)
                    ExtractionMethod.TEXT_LAYER -> stringResource(R.string.import_row_via_text)
                    else -> null
                }
            if (via != null) text = stringResource(R.string.import_separator, text, via)
            text
        }
        ImportRowKind.SAVED_PAIRED -> stringResource(R.string.import_row_saved_paired)
        ImportRowKind.UPDATED -> stringResource(R.string.import_row_updated)
        ImportRowKind.DUPLICATE -> stringResource(R.string.import_duplicate)
        ImportRowKind.NEEDS_REVIEW -> stringResource(R.string.import_row_needs_review)
        ImportRowKind.FAILED -> row.failureReason.orEmpty()
    }

private val previewRows =
    listOf(
        ImportRowModel(
            "1", "madhuri_ticket.pdf", ImportRowKind.SAVED, ImportRowTap.OPEN_TRIP, "t1",
            route = "SNSI → BMT, 3 Aug", method = ExtractionMethod.OCR,
        ),
        ImportRowModel("2", "SC_SNSI_10Aug.pdf", ImportRowKind.SAVED_PAIRED, ImportRowTap.OPEN_TRIP, "t2"),
        ImportRowModel("3", "ERS_8751652729.pdf", ImportRowKind.DUPLICATE, ImportRowTap.OPEN_TRIP, "t3"),
        ImportRowModel("4", "scan_0812.pdf", ImportRowKind.NEEDS_REVIEW, ImportRowTap.EDIT_TICKET, "t4"),
        ImportRowModel("5", "IMG_2041.jpg", ImportRowKind.FAILED, ImportRowTap.NONE, failureReason = "Not a PDF"),
        ImportRowModel(
            "6", "ticket_22Aug_with_a_really_long_name_that_overflows.pdf", ImportRowKind.READING, ImportRowTap.NONE,
        ),
        ImportRowModel("7", "ticket_29Aug.pdf", ImportRowKind.QUEUED, ImportRowTap.NONE),
    )

@Composable
private fun RowsGallery() {
    JoinedColumn(Modifier.padding(12.dp)) {
        previewRows.forEachIndexed { i, row ->
            ImportItemRow(row, JoinedListDefaults.shape(i, previewRows.size), onClick = {})
        }
    }
}

@Preview(name = "Rows light", showBackground = true)
@Composable
private fun ImportItemRowLightPreview() {
    TripcheckerTheme(darkTheme = false) { RowsGallery() }
}

@Preview(name = "Rows dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportItemRowDarkPreview() {
    TripcheckerTheme(darkTheme = true) { RowsGallery() }
}
