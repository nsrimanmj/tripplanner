package com.example.tripchecker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material 3 shape scale used by the app theme. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Extra corner radii (and ready-made shapes) from the design that are not part of [AppShapes]. */
object TripShapes {
    val hero = 32.dp
    val tile = 14.dp
    val squareButton = 18.dp
    val joinedInner = 6.dp
    val joinedOuter = 20.dp

    val heroShape = RoundedCornerShape(hero)
    val tileShape = RoundedCornerShape(tile)
    val squareButtonShape = RoundedCornerShape(squareButton)
}
