package com.example.tripchecker

import android.app.Application
import com.example.tripchecker.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/** Application entry point; builds the manual DI graph. */
class TripCheckerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}
