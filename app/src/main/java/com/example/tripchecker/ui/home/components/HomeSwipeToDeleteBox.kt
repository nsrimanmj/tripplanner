package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.theme.AppShapes

/**
 * End-to-start swipe wrapper for single-ticket Home rows. Crossing the threshold calls
 * [onDeleteRequested] and snaps back until the user confirms in the delete dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSwipeToDeleteBox(
    enabled: Boolean,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundShape: Shape = AppShapes.extraLarge,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val deleteActionLabel = stringResource(R.string.delete)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDeleteRequested()
                    false
                } else {
                    false
                }
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        modifier =
            modifier.semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(deleteActionLabel) {
                            onDeleteRequested()
                            true
                        },
                    )
            },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer, backgroundShape)
                        .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = deleteActionLabel,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}

@Preview(name = "Swipe delete background light", widthDp = 390)
@Composable
private fun HomeSwipeToDeletePreviewLight() =
    HomePreviewSurface(dark = false) {
        HomeSwipeToDeleteBox(enabled = true, onDeleteRequested = {}, backgroundShape = RoundedCornerShape(24.dp)) {
            NeedsReviewRow(
                item = com.example.tripchecker.ui.home.HomePreviewData.needsReview,
                onClick = {},
            )
        }
    }

@Preview(name = "Swipe delete background dark", widthDp = 390)
@Composable
private fun HomeSwipeToDeletePreviewDark() =
    HomePreviewSurface(dark = true) {
        HomeSwipeToDeleteBox(enabled = true, onDeleteRequested = {}, backgroundShape = RoundedCornerShape(24.dp)) {
            NeedsReviewRow(
                item = com.example.tripchecker.ui.home.HomePreviewData.needsReview,
                onClick = {},
            )
        }
    }
