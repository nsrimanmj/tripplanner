package com.example.tripchecker.ui.edit

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tripchecker.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Which date field a date picker dialog is editing. */
internal enum class DateTarget { Departure, Arrival }

/** Which time field a time picker dialog is editing. */
internal enum class TimeTarget { Departure, Arrival }

/** M3 date picker dialog; [initial] is shown preselected and the result is converted from UTC-midnight millis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onPicked(EditTicketMapper.datePickerMillisToLocalDate(it)) }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

/** M3 time picker (24-hour) in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPicked: (LocalTime) -> Unit,
) {
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_select_time)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onPicked(LocalTime.of(timeState.hour, timeState.minute))
                    onDismiss()
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        TimePicker(state = timeState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
