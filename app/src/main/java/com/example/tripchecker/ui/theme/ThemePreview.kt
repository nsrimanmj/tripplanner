package com.example.tripchecker.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun roles(c: ColorScheme): List<Pair<String, Color>> = listOf(
    "primary" to c.primary,
    "onPrimary" to c.onPrimary,
    "primaryContainer" to c.primaryContainer,
    "onPrimaryContainer" to c.onPrimaryContainer,
    "inversePrimary" to c.inversePrimary,
    "secondary" to c.secondary,
    "onSecondary" to c.onSecondary,
    "secondaryContainer" to c.secondaryContainer,
    "onSecondaryContainer" to c.onSecondaryContainer,
    "tertiary" to c.tertiary,
    "onTertiary" to c.onTertiary,
    "tertiaryContainer" to c.tertiaryContainer,
    "onTertiaryContainer" to c.onTertiaryContainer,
    "error" to c.error,
    "onError" to c.onError,
    "errorContainer" to c.errorContainer,
    "onErrorContainer" to c.onErrorContainer,
    "background" to c.background,
    "onBackground" to c.onBackground,
    "surface" to c.surface,
    "onSurface" to c.onSurface,
    "surfaceVariant" to c.surfaceVariant,
    "onSurfaceVariant" to c.onSurfaceVariant,
    "surfaceTint" to c.surfaceTint,
    "surfaceDim" to c.surfaceDim,
    "surfaceBright" to c.surfaceBright,
    "surfaceContainerLowest" to c.surfaceContainerLowest,
    "surfaceContainerLow" to c.surfaceContainerLow,
    "surfaceContainer" to c.surfaceContainer,
    "surfaceContainerHigh" to c.surfaceContainerHigh,
    "surfaceContainerHighest" to c.surfaceContainerHighest,
    "outline" to c.outline,
    "outlineVariant" to c.outlineVariant,
    "inverseSurface" to c.inverseSurface,
    "inverseOnSurface" to c.inverseOnSurface,
    "scrim" to c.scrim,
)

/** Scrollable sheet showing every colour role swatch and every type style, for visual QA. */
@Composable
fun ThemePaletteSheet(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "displaySmall" to type.displaySmall,
        "headlineLarge" to type.headlineLarge,
        "headlineMedium" to type.headlineMedium,
        "headlineSmall" to type.headlineSmall,
        "titleLarge" to type.titleLarge,
        "titleMedium" to type.titleMedium,
        "titleSmall" to type.titleSmall,
        "bodyLarge" to type.bodyLarge,
        "bodyMedium" to type.bodyMedium,
        "bodySmall" to type.bodySmall,
        "labelLarge" to type.labelLarge,
        "labelMedium" to type.labelMedium,
        "labelSmall" to type.labelSmall,
        "countdown" to TripTypography.countdown,
        "stationCodeHero" to TripTypography.stationCodeHero,
        "stationCode" to TripTypography.stationCode,
        "seatValue" to TripTypography.seatValue,
        "pnr" to TripTypography.pnr,
    )
    Column(
        modifier
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Colour roles", style = type.titleMedium, color = scheme.onSurface)
        roles(scheme).forEach { (name, color) ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(width = 56.dp, height = 28.dp)
                        .background(color, RoundedCornerShape(6.dp))
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(6.dp)),
                )
                Text(name, style = type.bodySmall, color = scheme.onSurface)
            }
        }
        Box(Modifier.height(Dimens.sectionGap))
        Text("Type styles", style = type.titleMedium, color = scheme.onSurface)
        styles.forEach { (name, style) ->
            Text(name, style = style, color = scheme.onSurface)
        }
    }
}

@Preview(name = "Palette light", widthDp = 360, heightDp = 1800)
@Composable
private fun ThemePaletteLightPreview() {
    TripcheckerTheme(darkTheme = false) { ThemePaletteSheet() }
}

@Preview(
    name = "Palette dark",
    widthDp = 360,
    heightDp = 1800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ThemePaletteDarkPreview() {
    TripcheckerTheme(darkTheme = true) { ThemePaletteSheet() }
}
