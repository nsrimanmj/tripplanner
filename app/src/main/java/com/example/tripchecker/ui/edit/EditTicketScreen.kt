package com.example.tripchecker.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tripchecker.R
import com.example.tripchecker.extraction.parser.TicketField
import java.time.LocalTime

@Composable
fun EditTicketScreen(
    viewModel: EditTicketViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EditTicketContent(
        state = state,
        onBack = onBack,
        onFormChange = viewModel::updateForm,
        onSave = { viewModel.save(onBack) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTicketContent(
    state: EditTicketUiState,
    onBack: () -> Unit,
    onFormChange: ((EditTicketForm) -> EditTicketForm) -> Unit,
    onSave: () -> Unit,
) {
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isCreate) {
                            stringResource(R.string.create_ticket_title)
                        } else {
                            stringResource(R.string.edit_ticket_title)
                        },
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField(
                label = stringResource(R.string.field_pnr),
                value = state.form.pnr,
                onValueChange = { v -> onFormChange { it.copy(pnr = v) } },
                isError = state.saveAttempted && TicketField.PNR in state.missingFields,
            )
            FormField(
                label = stringResource(R.string.field_train_number),
                value = state.form.trainNumber,
                onValueChange = { v -> onFormChange { it.copy(trainNumber = v) } },
                isError = state.saveAttempted && TicketField.TRAIN_NUMBER in state.missingFields,
            )
            FormField(
                label = stringResource(R.string.field_train_name),
                value = state.form.trainName,
                onValueChange = { v -> onFormChange { it.copy(trainName = v) } },
            )
            FormField(
                label = stringResource(R.string.field_from_code),
                value = state.form.fromCode,
                onValueChange = { v -> onFormChange { it.copy(fromCode = v.uppercase()) } },
                isError = state.saveAttempted && TicketField.FROM in state.missingFields,
            )
            FormField(
                label = stringResource(R.string.field_from_name),
                value = state.form.fromName,
                onValueChange = { v -> onFormChange { it.copy(fromName = v) } },
            )
            FormField(
                label = stringResource(R.string.field_boarding_code),
                value = state.form.boardingCode,
                onValueChange = { v -> onFormChange { it.copy(boardingCode = v.uppercase()) } },
            )
            FormField(
                label = stringResource(R.string.field_boarding_name),
                value = state.form.boardingName,
                onValueChange = { v -> onFormChange { it.copy(boardingName = v) } },
            )
            FormField(
                label = stringResource(R.string.field_to_code),
                value = state.form.toCode,
                onValueChange = { v -> onFormChange { it.copy(toCode = v.uppercase()) } },
                isError = state.saveAttempted && TicketField.TO in state.missingFields,
            )
            FormField(
                label = stringResource(R.string.field_to_name),
                value = state.form.toName,
                onValueChange = { v -> onFormChange { it.copy(toName = v) } },
            )
            TextButton(onClick = { dateTarget = DateTarget.Start }) {
                Text(stringResource(R.string.field_start_date, state.form.startDate?.toString() ?: "—"))
            }
            val departureMissing =
                state.saveAttempted && TicketField.DEPARTURE_DATE in state.missingFields
            val departureErrorColor =
                if (departureMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            TextButton(onClick = { dateTarget = DateTarget.Departure }) {
                Text(
                    stringResource(R.string.field_departure_date, state.form.departureDate?.toString() ?: "—"),
                    color = departureErrorColor,
                )
            }
            TextButton(onClick = { timeTarget = TimeTarget.Departure }) {
                Text(
                    stringResource(R.string.field_departure_time, state.form.departureTime?.toString() ?: "—"),
                    color = departureErrorColor,
                )
            }
            TextButton(onClick = { dateTarget = DateTarget.Arrival }) {
                Text(stringResource(R.string.field_arrival_date, state.form.arrivalDate?.toString() ?: "—"))
            }
            TextButton(onClick = { timeTarget = TimeTarget.Arrival }) {
                Text(stringResource(R.string.field_arrival_time, state.form.arrivalTime?.toString() ?: "—"))
            }
            FormField(
                label = stringResource(R.string.field_class),
                value = state.form.travelClass,
                onValueChange = { v -> onFormChange { it.copy(travelClass = v) } },
            )
            FormField(
                label = stringResource(R.string.field_quota),
                value = state.form.quota,
                onValueChange = { v -> onFormChange { it.copy(quota = v) } },
            )
            FormField(
                label = stringResource(R.string.field_distance),
                value = state.form.distanceKm,
                onValueChange = { v -> onFormChange { it.copy(distanceKm = v) } },
                keyboardType = KeyboardType.Number,
            )
            FormField(
                label = stringResource(R.string.field_fare),
                value = state.form.totalFareRupees,
                onValueChange = { v -> onFormChange { it.copy(totalFareRupees = v) } },
                keyboardType = KeyboardType.Decimal,
            )
            Text(stringResource(R.string.passengers_header), modifier = Modifier.padding(top = 8.dp))
            state.form.passengers.forEachIndexed { index, row ->
                PassengerEditor(
                    row = row,
                    onChange = { updated ->
                        onFormChange { form ->
                            form.copy(passengers = form.passengers.toMutableList().also { it[index] = updated })
                        }
                    },
                    onRemove = {
                        onFormChange { form ->
                            form.copy(passengers = form.passengers.filterIndexed { i, _ -> i != index })
                        }
                    },
                )
            }
            TextButton(
                onClick = {
                    onFormChange { it.copy(passengers = it.passengers + PassengerFormRow()) }
                },
            ) { Text(stringResource(R.string.add_passenger)) }
            if (state.validationFailed) {
                Text(
                    missingFieldsMessage(state.missingFields),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.saveErrorMessage?.let {
                Text(
                    stringResource(R.string.edit_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(
                    if (state.isLoading) {
                        stringResource(R.string.edit_loading)
                    } else {
                        stringResource(R.string.save)
                    },
                )
            }
        }
    }

    dateTarget?.let { target ->
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val date = EditTicketMapper.datePickerMillisToLocalDate(millis)
                            onFormChange { form ->
                                when (target) {
                                    DateTarget.Start -> form.copy(startDate = date)
                                    DateTarget.Departure ->
                                        form.copy(
                                            departureDate = date,
                                            departureTime = form.departureTime ?: LocalTime.of(9, 0),
                                            startDate = form.startDate ?: date,
                                        )
                                    DateTarget.Arrival -> form.copy(arrivalDate = date)
                                }
                            }
                        }
                        dateTarget = null
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    timeTarget?.let { target ->
        val initial = when (target) {
            TimeTarget.Departure -> state.form.departureTime ?: LocalTime.of(9, 0)
            TimeTarget.Arrival -> state.form.arrivalTime ?: LocalTime.of(18, 0)
        }
        val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        DatePickerDialog(
            onDismissRequest = { timeTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val time = LocalTime.of(timeState.hour, timeState.minute)
                        onFormChange { form ->
                            when (target) {
                                TimeTarget.Departure -> form.copy(departureTime = time)
                                TimeTarget.Arrival -> form.copy(arrivalTime = time)
                            }
                        }
                        timeTarget = null
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { timeTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            TimePicker(state = timeState)
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
    )
}

@Composable
private fun PassengerEditor(
    row: PassengerFormRow,
    onChange: (PassengerFormRow) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        FormField(
            label = stringResource(R.string.field_passenger_name),
            value = row.name,
            onValueChange = { onChange(row.copy(name = it)) },
        )
        FormField(
            label = stringResource(R.string.field_age),
            value = row.age,
            onValueChange = { onChange(row.copy(age = it)) },
            keyboardType = KeyboardType.Number,
        )
        FormField(
            label = stringResource(R.string.field_gender),
            value = row.gender,
            onValueChange = { onChange(row.copy(gender = it)) },
        )
        FormField(
            label = stringResource(R.string.field_current_status),
            value = row.currentStatus,
            onValueChange = { onChange(row.copy(currentStatus = it)) },
        )
        TextButton(onClick = onRemove) { Text(stringResource(R.string.remove_passenger)) }
    }
}

@Composable
private fun missingFieldsMessage(missing: Set<TicketField>): String {
    val labels =
        buildList {
            if (TicketField.PNR in missing) add(stringResource(R.string.field_pnr))
            if (TicketField.TRAIN_NUMBER in missing) add(stringResource(R.string.field_train_number))
            if (TicketField.FROM in missing) add(stringResource(R.string.field_from_code))
            if (TicketField.TO in missing) add(stringResource(R.string.field_to_code))
            if (TicketField.DEPARTURE_DATE in missing) {
                add(stringResource(R.string.field_departure_date_short))
            }
        }
    return if (labels.isEmpty()) {
        stringResource(R.string.edit_save_validation_error)
    } else {
        stringResource(R.string.edit_missing_only, labels.joinToString(", "))
    }
}

private enum class DateTarget { Start, Departure, Arrival }
private enum class TimeTarget { Departure, Arrival }
