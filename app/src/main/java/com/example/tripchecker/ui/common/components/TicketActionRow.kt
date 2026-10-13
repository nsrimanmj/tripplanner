package com.example.tripchecker.ui.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripShapes

/**
 * "Go to ticket" 56dp primary pill filling the width, plus 56dp square Share and optional outlined Edit buttons.
 * When [onOpen] is null the pill and Share are hidden (no PDF to open).
 */
@Composable
fun TicketActionRow(
    onOpen: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    shareContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    shareContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpen != null) {
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f).height(Dimens.buttonHeight),
                shape = CircleShape,
            ) {
                Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(20.dp))
                Box(Modifier.size(8.dp))
                Text(stringResource(R.string.common_go_to_ticket), style = MaterialTheme.typography.titleMedium)
            }
            if (onShare != null) {
                SquareActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = stringResource(R.string.common_share_ticket_pdf),
                    onClick = onShare,
                    containerColor = shareContainerColor,
                    contentColor = shareContentColor,
                )
            }
        } else if (onEdit != null) {
            Box(Modifier.weight(1f))
        }
        if (onEdit != null) {
            SquareActionButton(
                icon = Icons.Default.Edit,
                contentDescription = stringResource(R.string.common_edit_ticket),
                onClick = onEdit,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun SquareActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(Dimens.buttonHeight),
        shape = TripShapes.squareButtonShape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}
