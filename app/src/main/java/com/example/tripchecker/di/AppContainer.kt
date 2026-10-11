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
import com.example.tripchecker.importer.TicketImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/** Manual dependency container for the app. */
class AppContainer(
    appContext: Context,
) {
    private val context = appContext.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

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
            parsers = listOf(IrctcErsParser()),
            clock = clock,
        )

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
