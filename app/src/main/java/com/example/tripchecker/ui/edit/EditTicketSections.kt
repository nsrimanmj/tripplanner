package com.example.tripchecker.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.tripchecker.R
import com.example.tripchecker.extraction.parser.TicketField
import com.example.tripchecker.ui.common.Formatters
import com.example.tripchecker.ui.edit.components.FormSectionCard
import com.example.tripchecker.ui.edit.components.FormTextField
import com.example.tripchecker.ui.edit.components.PassengerEditorCard
import com.example.tripchecker.ui.edit.components.PickerField
import com.example.tripchecker.ui.edit.components.StationFieldRow
import com.example.tripchecker.ui.edit.components.sectionTitleWithSuffix
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val PNR_LENGTH = 10

/** Callback type used by the section composables to transform the form. */
internal typealias FormChange = ((EditTicketForm) -> EditTicketForm) -> Unit

/** Ticket card: PNR, train number and train name. */
@Composable
internal fun TicketSection(state: EditTicketUiState, showErrors: Boolean, onChange: FormChange) {
    val form = state.form
    val pnrError = showErrors && TicketField.PNR in state.missingFields
    val trainError = showErrors && TicketField.TRAIN_NUMBER in state.missingFields
    FormSectionCard(stringResource(R.string.edit_section_ticket)) {
        FormTextField(
            value = form.pnr,
            onValueChange = { v -> onChange { it.copy(pnr = v.filter(Char::isDigit).take(PNR_LENGTH)) } },
            label = stringResource(R.string.edit_label_pnr),
            placeholder = stringResource(R.string.edit_hint_pnr),
            supportingText =
                if (pnrError) {
                    stringResource(if (state.pdfFileName != null) R.string.edit_pnr_missing_pdf else R.string.edit_required)
                } else {
                    null
                },
            isError = pnrError,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            FormTextField(
                value = form.trainNumber,
                onValueChange = { v -> onChange { it.copy(trainNumber = v.filter(Char::isDigit).take(6)) } },
                label =
                    stringResource(
                        if (trainError) R.string.edit_label_train_no_required else R.string.edit_label_train_no,
                    ),
                supportingText = if (trainError) stringResource(R.string.edit_required) else null,
                isError = trainError,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(0.4f),
            )
            FormTextField(
                value = form.trainName,
                onValueChange = { v -> onChange { it.copy(trainName = v) } },
                label = stringResource(R.string.edit_label_train_name),
                modifier = Modifier.weight(0.6f),
            )
        }
    }
}

/** Route card: From, Boarding (optional) and To stations. */
@Composable
internal fun RouteSection(state: EditTicketUiState, showErrors: Boolean, onChange: FormChange) {
    val form = state.form
    val nameLabel = stringResource(R.string.edit_label_station_name)
    FormSectionCard(stringResource(R.string.edit_section_route)) {
        StationFieldRow(
            codeLabel = stringResource(R.string.edit_label_from),
            code = form.fromCode,
            onCodeChange = { v -> onChange { it.copy(fromCode = v) } },
            nameLabel = nameLabel,
            name = form.fromName,
            onNameChange = { v -> onChange { it.copy(fromName = v) } },
            isError = showErrors && TicketField.FROM in state.missingFields,
        )
        StationFieldRow(
            codeLabel = stringResource(R.string.edit_label_boarding),
            code = form.boardingCode,
            onCodeChange = { v -> onChange { it.copy(boardingCode = v) } },
            nameLabel = nameLabel,
            name = form.boardingName,
            onNameChange = { v -> onChange { it.copy(boardingName = v) } },
            codePlaceholder = stringResource(R.string.edit_hint_code),
            namePlaceholder = stringResource(R.string.edit_hint_boarding_name),
            supportingText = stringResource(R.string.edit_boarding_supporting),
        )
        StationFieldRow(
            codeLabel = stringResource(R.string.edit_label_to),
            code = form.toCode,
            onCodeChange = { v -> onChange { it.copy(toCode = v) } },
            nameLabel = nameLabel,
            name = form.toName,
            onNameChange = { v -> onChange { it.copy(toName = v) } },
            isError = showErrors && TicketField.TO in state.missingFields,
        )
    }
}

/** Schedule · IST card: departure and arrival, each a read-only date field and time field opening a picker. */
@Composable
internal fun ScheduleSection(
    state: EditTicketUiState,
    showErrors: Boolean,
    onPickDate: (DateTarget) -> Unit,
    onPickTime: (TimeTarget) -> Unit,
) {
    val form = state.form
    val departureError = showErrors && TicketField.DEPARTURE_DATE in state.missingFields
    val calendar = painterResource(R.drawable.ic_event)
    val clock = painterResource(R.drawable.ic_schedule)
    val pickDate = stringResource(R.string.edit_pick_date_action)
    val pickTime = stringResource(R.string.edit_pick_time_action)
    FormSectionCard(
        sectionTitleWithSuffix(
            stringResource(R.string.edit_section_schedule),
            stringResource(R.string.edit_section_schedule_zone),
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PickerField(
                value = form.departureDate?.let { Formatters.formatDateWithYear(it) }.orEmpty(),
                label = stringResource(R.string.edit_label_departure_date),
                placeholder = stringResource(R.string.edit_hint_select_date),
                trailingIcon = calendar,
                onClickLabel = pickDate,
                onClick = { onPickDate(DateTarget.Departure) },
                supportingText = if (departureError) stringResource(R.string.edit_departure_supporting) else null,
                isError = departureError && form.departureDate == null,
                modifier = Modifier.weight(0.6f),
            )
            PickerField(
                value = form.departureTime?.format(timeFormat).orEmpty(),
                label = stringResource(R.string.edit_label_time_required),
                placeholder = stringResource(R.string.edit_hint_time),
                trailingIcon = clock,
                onClickLabel = pickTime,
                onClick = { onPickTime(TimeTarget.Departure) },
                isError = departureError && form.departureTime == null,
                modifier = Modifier.weight(0.4f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PickerField(
                value = form.arrivalDate?.let { Formatters.formatDateWithYear(it) }.orEmpty(),
                label = stringResource(R.string.edit_label_arrival_date),
                placeholder = stringResource(R.string.edit_hint_select_date),
                trailingIcon = calendar,
                onClickLabel = pickDate,
                onClick = { onPickDate(DateTarget.Arrival) },
                modifier = Modifier.weight(0.6f),
            )
            PickerField(
                value = form.arrivalTime?.format(timeFormat).orEmpty(),
                label = stringResource(R.string.edit_label_time),
                placeholder = stringResource(R.string.edit_hint_time),
                trailingIcon = clock,
                onClickLabel = pickTime,
                onClick = { onPickTime(TimeTarget.Arrival) },
                modifier = Modifier.weight(0.4f),
            )
        }
    }
}

/** Class and fare card: a 2x2 grid of class, quota, distance and total fare. */
@Composable
internal fun ClassFareSection(form: EditTicketForm, onChange: FormChange) {
    FormSectionCard(stringResource(R.string.edit_section_class_fare)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                value = form.travelClass,
                onValueChange = { v -> onChange { it.copy(travelClass = v) } },
                label = stringResource(R.string.edit_label_class),
                modifier = Modifier.weight(1f),
            )
            FormTextField(
                value = form.quota,
                onValueChange = { v -> onChange { it.copy(quota = v) } },
                label = stringResource(R.string.edit_label_quota),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                value = form.distanceKm,
                onValueChange = { v -> onChange { it.copy(distanceKm = v.filter(Char::isDigit)) } },
                label = stringResource(R.string.edit_label_distance),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            FormTextField(
                value = form.totalFareRupees,
                onValueChange = { v -> onChange { it.copy(totalFareRupees = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) } },
                label = stringResource(R.string.edit_label_fare),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Passengers card: one editor per passenger plus a full-width "Add passenger" tonal button. */
@Composable
internal fun PassengersSection(form: EditTicketForm, onChange: FormChange) {
    FormSectionCard(stringResource(R.string.edit_section_passengers)) {
        form.passengers.forEachIndexed { index, row ->
            PassengerEditorCard(
                number = index + 1,
                row = row,
                onChange = { updated ->
                    onChange { f -> f.copy(passengers = f.passengers.toMutableList().also { it[index] = updated }) }
                },
                onRemove = {
                    onChange { f -> f.copy(passengers = f.passengers.filterIndexed { i, _ -> i != index }) }
                },
            )
        }
        FilledTonalButton(
            onClick = { onChange { it.copy(passengers = it.passengers + PassengerFormRow()) } },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.edit_add_passenger),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
