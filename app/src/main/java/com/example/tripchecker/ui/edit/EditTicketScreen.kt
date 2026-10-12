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
            FormField(stringResource(R.string.field_train_name), state.form.trainName) { v ->
                onFormChange { it.copy(trainName = v) }
            }
            FormField(
                stringResource(R.string.field_from_code),
                state.form.fromCode,
                isError = state.saveAttempted && TicketField.FROM in state.missingFields,
            ) { v -> onFormChange { it.copy(fromCode = v.uppercase()) } }
            FormField(stringResource(R.string.field_from_name), state.form.fromName) { v ->
                onFormChange { it.copy(fromName = v) }
            }
            FormField(stringResource(R.string.field_boarding_code), state.form.boardingCode) { v ->
                onFormChange { it.copy(boardingCode = v.uppercase()) }
            }
            FormField(stringResource(R.string.field_boarding_name), state.form.boardingName) { v ->
                onFormChange { it.copy(boardingName = v) }
            }
            FormField(
                stringResource(R.string.field_to_code),
                state.form.toCode,
                isError = state.saveAttempted && TicketField.TO in state.missingFields,
            ) { v -> onFormChange { it.copy(toCode = v.uppercase()) } }
            FormField(stringResource(R.string.field_to_name), state.form.toName) { v ->
                onFormChange { it.copy(toName = v) }
            }
            TextButton(onClick = { dateTarget = DateTarget.Start }) {
                Text(stringResource(R.string.field_start_date, state.form.startDate?.toString() ?: "—"))
            }
            TextButton(onClick = { dateTarget = DateTarget.Departure }) {
                Text(stringResource(R.string.field_departure_date, state.form.departureDate?.toString() ?: "—"))
            }
            TextButton(onClick = { timeTarget = TimeTarget.Departure }) {
                Text(stringResource(R.string.field_departure_time, state.form.departureTime?.toString() ?: "—"))
            }
            TextButton(onClick = { dateTarget = DateTarget.Arrival }) {
                Text(stringResource(R.string.field_arrival_date, state.form.arrivalDate?.toString() ?: "—"))
            }
            TextButton(onClick = { timeTarget = TimeTarget.Arrival }) {
                Text(stringResource(R.string.field_arrival_time, state.form.arrivalTime?.toString() ?: "—"))
            }
            FormField(stringResource(R.string.field_class), state.form.travelClass) { v ->
                onFormChange { it.copy(travelClass = v) }
            }
            FormField(stringResource(R.string.field_quota), state.form.quota) { v ->
                onFormChange { it.copy(quota = v) }
            }
            FormField(stringResource(R.string.field_distance), state.form.distanceKm, KeyboardType.Number) { v ->
                onFormChange { it.copy(distanceKm = v) }
            }
            FormField(stringResource(R.string.field_fare), state.form.totalFareRupees, KeyboardType.Decimal) { v ->
                onFormChange { it.copy(totalFareRupees = v) }
            }
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
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
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
                                    DateTarget.Departure -> form.copy(departureDate = date)
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
        FormField(stringResource(R.string.field_passenger_name), row.name) { onChange(row.copy(name = it)) }
        FormField(stringResource(R.string.field_age), row.age, keyboardType = KeyboardType.Number) {
            onChange(row.copy(age = it))
        }
        FormField(stringResource(R.string.field_gender), row.gender) { onChange(row.copy(gender = it)) }
        FormField(stringResource(R.string.field_current_status), row.currentStatus) {
            onChange(row.copy(currentStatus = it))
        }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.remove_passenger)) }
    }
}

private enum class DateTarget { Start, Departure, Arrival }
private enum class TimeTarget { Departure, Arrival }
