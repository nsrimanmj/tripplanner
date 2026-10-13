package com.example.tripchecker.ui.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.ui.theme.Dimens
import com.example.tripchecker.ui.theme.TripcheckerTheme

/** Rounded `surfaceContainerLow` card with a `titleMedium` heading, wrapping one group of form fields. */
@Composable
fun FormSectionCard(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            content()
        }
    }
}

/** Convenience overload for a plain-string title. */
@Composable
fun FormSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = FormSectionCard(AnnotatedString(title), modifier, content)

/** Title with a lighter suffix, e.g. "Schedule" followed by " · IST". */
@Composable
fun sectionTitleWithSuffix(title: String, suffix: String): AnnotatedString =
    buildAnnotatedString {
        append(title)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" · $suffix") }
    }

@Preview(showBackground = true)
@Composable
private fun FormSectionCardPreview() {
    TripcheckerTheme {
        FormSectionCard(title = "Ticket") { Text("Fields go here") }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormSectionCardDarkPreview() {
    TripcheckerTheme(darkTheme = true) {
        FormSectionCard(title = sectionTitleWithSuffix("Schedule", "IST")) { Text("Fields go here") }
    }
}
