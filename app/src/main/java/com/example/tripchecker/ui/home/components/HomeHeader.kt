package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.theme.Dimens

/**
 * Large collapsing app bar titled "Trip Checker" (headlineLarge, easing to titleLarge when collapsed).
 * Search and More-options actions are intentionally absent until M7 features exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography
    val title =
        lerp(typography.headlineLarge, typography.titleLarge, scrollBehavior.state.collapsedFraction)
    LargeTopAppBar(
        title = { Text(stringResource(R.string.app_name), style = title, maxLines = 1) },
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        scrollBehavior = scrollBehavior,
    )
}

/** Header subtitle under the app bar title, e.g. "Sat, 1 Aug · 2 trips ahead". */
@Composable
fun HomeSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = Dimens.headerGutter - Dimens.screenGutter),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
