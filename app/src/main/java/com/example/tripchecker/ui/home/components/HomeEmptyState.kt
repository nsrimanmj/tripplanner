package com.example.tripchecker.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripchecker.R
import com.example.tripchecker.ui.home.HomePreviewSurface
import com.example.tripchecker.ui.theme.Dimens

/** Empty state: illustration, headline, body, a full-width "Import PDFs" button and an "Add manually" text button. */
@Composable
fun HomeEmptyState(
    onImport: () -> Unit,
    onAddManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_tickets),
            contentDescription = null,
            modifier = Modifier.size(width = 240.dp, height = 180.dp),
        )
        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.empty_home_message),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                shape = CircleShape,
            ) {
                Icon(painterResource(R.drawable.ic_description), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.import_pdfs), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onAddManually, modifier = Modifier.height(Dimens.minTouch)) {
                Text(stringResource(R.string.add_manually), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Preview(name = "Empty light", widthDp = 390, heightDp = 700)
@Composable
private fun HomeEmptyStateLightPreview() = HomePreviewSurface(dark = false) { HomeEmptyState({}, {}) }

@Preview(name = "Empty dark", widthDp = 390, heightDp = 700)
@Composable
private fun HomeEmptyStateDarkPreview() = HomePreviewSurface(dark = true) { HomeEmptyState({}, {}) }
