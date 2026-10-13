package com.example.tripchecker.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.home.HomePreviewSurface

/** "Add ticket" extended FAB (corner 18); collapses to icon-only when [expanded] is false. */
@Composable
fun AddTicketFab(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.add_ticket)
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Default.Add, contentDescription = if (expanded) null else label) },
        text = { Text(label, style = MaterialTheme.typography.titleMedium) },
    )
}

/**
 * Hand-built FAB menu (material3 1.4 has no FloatingActionButtonMenu): a full-screen surface scrim,
 * two right-aligned pill items ("Import PDFs", "Add manually") and a round close button where the FAB was.
 * Back and a scrim tap call [onDismiss].
 */
@Composable
fun AddTicketMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onImportPdfs: () -> Unit,
    onAddManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(120)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val itemEnter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f) + slideInVertically { it / 2 }
                val itemExit = fadeOut(tween(100)) + scaleOut(targetScale = 0.9f) + slideOutVertically { it / 2 }
                MenuPill(
                    label = stringResource(R.string.import_pdfs),
                    icon = painterResource(R.drawable.ic_description),
                    onClick = onImportPdfs,
                    modifier = Modifier.animateEnterExit(enter = itemEnter, exit = itemExit),
                )
                MenuPill(
                    label = stringResource(R.string.add_manually),
                    icon = rememberVectorPainter(Icons.Default.Edit),
                    onClick = onAddManually,
                    modifier = Modifier.animateEnterExit(enter = itemEnter, exit = itemExit),
                )
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 4.dp).size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.home_menu_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuPill(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(name = "Fab menu light", widthDp = 390, heightDp = 500)
@Composable
private fun AddTicketMenuLightPreview() =
    HomePreviewSurface(dark = false) { AddTicketMenu(true, {}, {}, {}) }

@Preview(name = "Fab menu dark", widthDp = 390, heightDp = 500)
@Composable
private fun AddTicketMenuDarkPreview() =
    HomePreviewSurface(dark = true) { AddTicketMenu(true, {}, {}, {}) }

@Preview(name = "Fab collapsed and expanded", widthDp = 390)
@Composable
private fun AddTicketFabPreview() =
    HomePreviewSurface(dark = false) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AddTicketFab(expanded = true, onClick = {})
            AddTicketFab(expanded = false, onClick = {})
        }
    }
