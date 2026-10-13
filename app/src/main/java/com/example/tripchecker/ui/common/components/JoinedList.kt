package com.example.tripchecker.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tripchecker.ui.theme.TripShapes

/** Shapes for M3 "connected" lists: first 20/20/6/6, middle 6, last 6/6/20/20, single 20. */
object JoinedListDefaults {
    val Gap: Dp = 4.dp

    /** Shape for the item at [index] of [count] items. */
    fun shape(
        index: Int,
        count: Int,
        outer: Dp = TripShapes.joinedOuter,
        inner: Dp = TripShapes.joinedInner,
    ): Shape = when {
        count <= 1 -> RoundedCornerShape(outer)
        index <= 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomEnd = inner, bottomStart = inner)
        index >= count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomEnd = outer, bottomStart = outer)
        else -> RoundedCornerShape(inner)
    }
}

/** Column with [JoinedListDefaults.Gap] spacing whose children call [JoinedListDefaults.shape] themselves. */
@Composable
fun JoinedColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(JoinedListDefaults.Gap),
        content = content,
    )
}
