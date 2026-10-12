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
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) {
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
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) {
            handleImportIntent(intent)
        }
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
        val fileProviderAuthority = "${packageName}.fileprovider"
        fun accept(uri: Uri?): Uri? {
            if (uri == null) return null
            if (uri.scheme != "content") return null
            if (uri.authority == fileProviderAuthority) return null
            return uri
        }
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(accept(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)))
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.mapNotNull { accept(it) }
                    ?: emptyList()
            }
            Intent.ACTION_VIEW -> listOfNotNull(accept(intent.data))
            else -> emptyList()
        }
    }
}
