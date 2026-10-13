package com.example.tripchecker.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripchecker.ui.theme.TripShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WeekdayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

/** At least 48x56dp weekday / day / month badge; [highlighted] uses primaryContainer, otherwise a neutral surface. */
@Composable
fun DateBadge(
    date: LocalDate,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (highlighted) colors.primaryContainer else colors.surfaceContainerHighest
    val content = if (highlighted) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(
        // Minimum size only: the badge grows with large font scales instead of clipping the month.
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 56.dp),
        shape = TripShapes.tileShape,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val small = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp)
            Text(date.format(WeekdayFormat).uppercase(Locale.ENGLISH), style = small, textAlign = TextAlign.Center)
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Text(date.format(MonthFormat).uppercase(Locale.ENGLISH), style = small, textAlign = TextAlign.Center)
        }
    }
}

/** Rounded-square (corner 14) or circular icon tile with the given colours. */
@Composable
fun IconTile(
    painter: Painter,
    contentDescription: String?,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    circle: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, if (circle) CircleShape else RoundedCornerShape(TripShapes.tile)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Section header: titleSmall on onSurfaceVariant, with an optional tertiary count pill. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .defaultMinSize(minWidth = 20.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }
}
