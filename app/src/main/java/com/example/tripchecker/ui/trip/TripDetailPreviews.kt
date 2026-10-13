package com.example.tripchecker.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.domain.model.JourneyGroup
import com.example.tripchecker.ui.theme.TripcheckerTheme
import com.example.tripchecker.ui.trip.components.LegCard
import com.example.tripchecker.ui.trip.components.LinkReturnSheetContent
import com.example.tripchecker.ui.trip.components.PairingInfoCard
import com.example.tripchecker.ui.trip.components.StayConnector
import com.example.tripchecker.ui.trip.components.TripHeader

@Composable
private fun PreviewScreen(state: TripDetailUiState, linkSheetVisible: Boolean = false) {
    TripDetailContent(
        state = state,
        snackbarHostState = remember { SnackbarHostState() },
        onBack = {},
        onEdit = {},
        onCopyPnr = {},
        onGoToTicket = {},
        onShare = {},
        onToggleCancelled = {},
        onDeleteRequest = {},
        onUnlink = {},
        onResetPairing = {},
        onLinkReturn = {},
        onLinkConfirm = {},
        linkSheetVisible = linkSheetVisible,
        onDismissLinkSheet = {},
    )
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun PreviewTripDetailRoundTripLight() {
    TripcheckerTheme { PreviewScreen(TripDetailPreviewData.roundTripState) }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun PreviewTripDetailRoundTripDark() {
    TripcheckerTheme(darkTheme = true) { PreviewScreen(TripDetailPreviewData.roundTripState) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailOneWayLight() {
    TripcheckerTheme { PreviewScreen(TripDetailPreviewData.oneWayState) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTripDetailOneWayDark() {
    TripcheckerTheme(darkTheme = true) { PreviewScreen(TripDetailPreviewData.oneWayState) }
}

@Composable
private fun Gallery(group: JourneyGroup) {
    val state = TripDetailPreviewData.roundTripState
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TripHeader(TripDetailUiMapper.header(group))
        LegCard(state.legs[1], {}, {}, {}, {}, {}, {})
        StayConnector("6 days in Hyderabad")
        PairingInfoCard(TripDetailUiMapper.pairingCard(group), {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PreviewTripComponentsManualLight() {
    TripcheckerTheme { Gallery(TripDetailPreviewData.manualCustom) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PreviewTripComponentsManualDark() {
    TripcheckerTheme(darkTheme = true) { Gallery(TripDetailPreviewData.manualCustom) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLinkReturnSheetLight() {
    TripcheckerTheme {
        LinkReturnSheetContent(TripDetailPreviewData.linkSheet, "a", {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLinkReturnSheetDark() {
    TripcheckerTheme(darkTheme = true) {
        LinkReturnSheetContent(TripDetailPreviewData.linkSheet, null, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLinkReturnSheetEmpty() {
    TripcheckerTheme {
        LinkReturnSheetContent(LinkSheetUi("Hyderabad", emptyList()), null, {}, {}, {})
    }
}
