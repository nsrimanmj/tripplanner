package com.example.tripchecker.di

import android.content.Context
import androidx.room.Room
import com.example.tripchecker.data.db.AppDatabase
import com.example.tripchecker.data.file.TicketFileStore
import com.example.tripchecker.data.repo.OfflineTicketRepository
import com.example.tripchecker.data.repo.TicketRepository
import com.example.tripchecker.domain.ObserveJourneysUseCase
import com.example.tripchecker.extraction.CompositeTextExtractor
import com.example.tripchecker.extraction.OcrTextExtractor
import com.example.tripchecker.extraction.PdfBoxTextExtractor
import com.example.tripchecker.extraction.parser.IrctcErsParser
import com.example.tripchecker.importer.ImportManager
import com.example.tripchecker.importer.TicketImporter
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import com.example.tripchecker.importer.ImportEnqueueRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import java.time.Clock

/** Manual dependency container for the app. */
class AppContainer(
    appContext: Context,
) {
    private val context = appContext.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    val ticketParser: IrctcErsParser = IrctcErsParser()

    private val database: AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tripchecker.db").build()

    val ticketFileStore: TicketFileStore = TicketFileStore(context)

    val ticketRepository: TicketRepository =
        OfflineTicketRepository(
            dao = database.ticketDao(),
            fileStore = ticketFileStore,
            clock = clock,
        )

    val observeJourneysUseCase: ObserveJourneysUseCase =
        ObserveJourneysUseCase(ticketRepository, clock)

    private val textExtractor =
        CompositeTextExtractor(PdfBoxTextExtractor(), OcrTextExtractor())

    val ticketImporter: TicketImporter =
        TicketImporter(
            repository = ticketRepository,
            fileStore = ticketFileStore,
            extractor = textExtractor,
            parsers = listOf(ticketParser),
            clock = clock,
        )

    private val _openImportEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openImportEvents: SharedFlow<Unit> = _openImportEvents.asSharedFlow()

    val appScope: CoroutineScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e("TripChecker", "Unhandled coroutine error", throwable)
                },
        )

    val importManager: ImportManager =
        ImportManager(
            fileStore = ticketFileStore,
            importer = ticketImporter,
            scope = appScope,
        )

    val viewModelFactory: ViewModelProvider.Factory = createViewModelFactory(this)

    fun enqueueImports(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val capped = uris.take(50)
        importManager.enqueue(
            capped.map { uri ->
                ImportEnqueueRequest(
                    uriString = uri.toString(),
                    displayName = uri.lastPathSegment ?: "ticket.pdf",
                )
            },
        )
        _openImportEvents.tryEmit(Unit)
    }
}
