package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripchecker.domain.model.Passenger
@Composable
fun PassengerRow(passenger: Passenger, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            passenger.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = listOfNotNull(passenger.age?.toString(), passenger.gender).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.bodySmall)
        }
        val current = passenger.current
        val currentLine =
            when {
                current?.waitlistNumber != null -> "${current.code} ${current.waitlistNumber}"
                current != null ->
                    listOfNotNull(current.code, current.coach, current.berthNumber?.toString(), current.berthType)
                        .joinToString(" / ")
                else -> passenger.currentStatusRaw ?: "—"
            }
        Text(currentLine, style = MaterialTheme.typography.bodyLarge)
        passenger.bookingStatusRaw?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
