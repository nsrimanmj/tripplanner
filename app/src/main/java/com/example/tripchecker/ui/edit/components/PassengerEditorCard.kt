package com.example.tripchecker.ui.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.edit.PassengerFormRow
import com.example.tripchecker.ui.edit.StatusReadback
import com.example.tripchecker.ui.theme.TripcheckerTheme

/** One editable passenger on a `surfaceContainerLowest` card: name, age, M/F/T and a status with live read-back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerEditorCard(
    number: Int,
    row: PassengerFormRow,
    onChange: (PassengerFormRow) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.edit_passenger_title, number),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.edit_remove_passenger, number),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FormTextField(
                value = row.name,
                onValueChange = { onChange(row.copy(name = it)) },
                label = stringResource(R.string.edit_label_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                FormTextField(
                    value = row.age,
                    onValueChange = { onChange(row.copy(age = it.filter(Char::isDigit).take(3))) },
                    label = stringResource(R.string.edit_label_age),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(96.dp),
                )
                GenderSegments(
                    gender = row.gender,
                    onGenderChange = { onChange(row.copy(gender = it)) },
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
            }
            StatusField(
                value = row.currentStatus,
                onValueChange = { onChange(row.copy(currentStatus = it)) },
            )
        }
    }
}

@Composable
private fun StatusField(value: String, onValueChange: (String) -> Unit) {
    val readback = StatusReadback.of(value)
    val supporting =
        when (readback) {
            StatusReadback.Result.Empty -> null
            is StatusReadback.Result.Recognised ->
                stringResource(R.string.edit_status_reads_as, readback.summary)
            StatusReadback.Result.Unrecognised -> stringResource(R.string.edit_status_unrecognised)
        }
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.edit_label_status),
        supportingText = supporting,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderSegments(gender: String, onGenderChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val options =
        listOf(
            "M" to (R.string.edit_gender_male_short to R.string.edit_gender_male),
            "F" to (R.string.edit_gender_female_short to R.string.edit_gender_female),
            "T" to (R.string.edit_gender_third_short to R.string.edit_gender_third),
        )
    val current = gender.trim().uppercase().take(1)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (code, labels) ->
            val description = stringResource(labels.second)
            SegmentedButton(
                selected = current == code,
                onClick = { onGenderChange(if (current == code) "" else code) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.height(48.dp).semantics { contentDescription = description },
            ) {
                Text(stringResource(labels.first))
            }
        }
    }
}

private val previewRow =
    PassengerFormRow(id = "p1", name = "MADHURI", age = "26", gender = "F", currentStatus = "CNF/B4/33/LOWER")

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PassengerEditorCardPreview() {
    TripcheckerTheme {
        PassengerEditorCard(number = 1, row = previewRow, onChange = {}, onRemove = {})
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PassengerEditorCardDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        PassengerEditorCard(number = 2, row = previewRow.copy(currentStatus = "xyz", gender = ""), onChange = {}, onRemove = {})
    }
}
