package com.example.tripchecker.ui.trip.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.common.components.ChipTone
import com.example.tripchecker.ui.common.components.JoinedListDefaults
import com.example.tripchecker.ui.common.components.TagChip
import com.example.tripchecker.ui.trip.LinkCandidateUi
import com.example.tripchecker.ui.trip.LinkSheetUi

/** Modal sheet to pick a one-way ticket to link as the return leg; nothing is linked until "Link tickets". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkReturnSheet(
    visible: Boolean,
    sheet: LinkSheetUi,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!visible) return
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LinkReturnSheetContent(
            sheet = sheet,
            selectedId = selectedId,
            onSelect = { selectedId = it },
            onCancel = onDismiss,
            onConfirm = { selectedId?.let(onConfirm) },
        )
    }
}

/** Stateless body of [LinkReturnSheet]: title, subtitle, single-choice rows and the Cancel / Link actions. */
@Composable
fun LinkReturnSheetContent(
    sheet: LinkSheetUi,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.trip_link_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text =
                    if (sheet.destinationCity.isBlank()) {
                        stringResource(R.string.trip_link_subtitle_generic)
                    } else {
                        stringResource(R.string.trip_link_subtitle, sheet.destinationCity)
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sheet.candidates.isEmpty()) {
            Text(
                stringResource(R.string.trip_link_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(JoinedListDefaults.Gap),
            ) {
                itemsIndexed(sheet.candidates, key = { _, c -> c.ticketId }) { index, c ->
                    CandidateRow(
                        candidate = c,
                        selected = c.ticketId == selectedId,
                        shape = JoinedListDefaults.shape(index, sheet.candidates.size),
                        onSelect = { onSelect(c.ticketId) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = onConfirm,
                enabled = selectedId != null,
                shape = CircleShape,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.trip_link_confirm), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: LinkCandidateUi,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = shape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLowest,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.weight(1f)) {
                Text(candidate.route, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (candidate.meta.isNotBlank()) {
                    Text(
                        candidate.meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (candidate.likelyReturn) {
                    TagChip(
                        text = stringResource(R.string.trip_link_likely),
                        tone = if (selected) ChipTone.FILLED_PRIMARY else ChipTone.PRIMARY,
                    )
                }
                candidate.relativeDays?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
