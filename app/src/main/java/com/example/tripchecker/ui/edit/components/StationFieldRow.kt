package com.example.tripchecker.ui.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.ui.theme.TripcheckerTheme

/**
 * One station: a 120dp upper-cased bold code field next to a station-name field.
 * [supportingText] appears under the name field; [isError] marks the code field.
 */
@Composable
fun StationFieldRow(
    codeLabel: String,
    code: String,
    onCodeChange: (String) -> Unit,
    nameLabel: String,
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    codePlaceholder: String? = null,
    namePlaceholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FormTextField(
                value = code,
                onValueChange = {
                    onCodeChange(it.uppercase().filter { c -> c.isLetterOrDigit() }.take(MAX_CODE_LENGTH))
                },
                label = codeLabel,
                placeholder = codePlaceholder,
                isError = isError,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(CODE_FIELD_WIDTH),
            )
            FormTextField(
                value = name,
                onValueChange = onNameChange,
                label = nameLabel,
                placeholder = namePlaceholder,
                modifier = Modifier.weight(1f),
            )
        }
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

private const val MAX_CODE_LENGTH = 6

/** Wide enough for the unfocused "Boarding" label plus 16dp content padding on each side. */
private val CODE_FIELD_WIDTH = 120.dp

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun StationFieldRowPreview() {
    TripcheckerTheme {
        Column {
            StationFieldRow("From *", "SNSI", {}, "Station name", "SAINAGAR SHIRDI", {})
            StationFieldRow(
                "Boarding", "", {}, "Station name", "", {},
                codePlaceholder = "Code",
                namePlaceholder = "Same as From",
                supportingText = "Optional · only if you board at a different station",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun StationFieldRowDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        StationFieldRow("To *", "", {}, "Station name", "", {}, isError = true)
    }
}
