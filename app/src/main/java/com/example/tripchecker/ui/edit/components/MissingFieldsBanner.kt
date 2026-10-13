package com.example.tripchecker.ui.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.tripchecker.R
import com.example.tripchecker.domain.model.ExtractionMethod
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.theme.TripcheckerTheme

/**
 * Error-container banner listing the required fields still missing, where they were read from,
 * and an "Open PDF" pill when [onOpenPdf] is non-null. The caller shows it only while [missing] is non-empty.
 */
@Composable
fun MissingFieldsBanner(
    missing: List<TicketField>,
    sourceFileName: String?,
    extractionMethod: ExtractionMethod?,
    onOpenPdf: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val names = missing.map { missingFieldLabel(it) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp).size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.edit_banner_title, missing.size, missing.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(names.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                val source = sourceLine(sourceFileName, extractionMethod)
                if (source != null || onOpenPdf != null) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            source.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (onOpenPdf != null) OpenPdfPill(onOpenPdf)
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenPdfPill(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.edit_open_pdf),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun missingFieldLabel(field: TicketField): String =
    when (field) {
        TicketField.PNR -> stringResource(R.string.edit_missing_pnr)
        TicketField.TRAIN_NUMBER -> stringResource(R.string.edit_missing_train_number)
        TicketField.FROM -> stringResource(R.string.edit_missing_from)
        TicketField.TO -> stringResource(R.string.edit_missing_to)
        TicketField.DEPARTURE_DATE -> stringResource(R.string.edit_missing_departure)
        else -> field.name
    }

@Composable
private fun sourceLine(fileName: String?, method: ExtractionMethod?): String? {
    if (fileName.isNullOrBlank()) return null
    return when (method) {
        ExtractionMethod.OCR -> stringResource(R.string.edit_banner_source_ocr, fileName)
        ExtractionMethod.TEXT_LAYER -> stringResource(R.string.edit_banner_source_text, fileName)
        else -> stringResource(R.string.edit_banner_source_plain, fileName)
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun MissingFieldsBannerPreview() {
    TripcheckerTheme {
        MissingFieldsBanner(
            missing = listOf(TicketField.PNR, TicketField.DEPARTURE_DATE),
            sourceFileName = "scan_0812.pdf",
            extractionMethod = ExtractionMethod.OCR,
            onOpenPdf = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun MissingFieldsBannerDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        MissingFieldsBanner(
            missing = listOf(TicketField.TRAIN_NUMBER),
            sourceFileName = null,
            extractionMethod = null,
            onOpenPdf = null,
        )
    }
}
