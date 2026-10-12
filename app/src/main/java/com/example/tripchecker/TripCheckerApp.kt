package com.example.tripchecker

import android.app.Application
import com.example.tripchecker.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Application entry point; builds the manual DI graph. */
class TripCheckerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
        container.appScope.launch(Dispatchers.IO) {
            container.ticketFileStore.clearOrphanStaging()
            val referenced = container.ticketRepository.referencedPdfFileNames()
            container.ticketFileStore.deleteUnreferencedTicketPdfs(referenced)
        }
    }
}
