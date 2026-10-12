package com.example.tripchecker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.example.tripchecker.ui.navigation.AppNavHost
import com.example.tripchecker.ui.theme.TripcheckerTheme

/** Single activity: Compose navigation and PDF share/view intents. */
class MainActivity : ComponentActivity() {
    private var navigateImportOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            handleImportIntent(intent)?.let { navigateImportOnStart = true }
        }
        val container = (application as TripCheckerApp).container
        setContent {
            TripcheckerTheme {
                AppNavHost(
                    container = container,
                    startOnImport = navigateImportOnStart,
                    onPdfsSelected = { uris -> container.enqueueImports(uris) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?): List<Uri>? {
        val container = (application as TripCheckerApp).container
        val uris = extractPdfUris(intent)
        if (uris.isNotEmpty()) {
            container.enqueueImports(uris)
            return uris
        }
        return null
    }

    private fun extractPdfUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: emptyList()
            }
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            else -> emptyList()
        }
    }
}
