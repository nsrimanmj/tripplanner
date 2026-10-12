package com.example.tripchecker.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tripchecker.ui.home.HomeViewModel
import com.example.tripchecker.ui.importflow.ImportViewModel

/** ViewModel factory wired from [AppContainer]. */
fun createViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            HomeViewModel(container.ticketRepository, container.clock)
        }
        initializer {
            ImportViewModel(container.importManager)
        }
    }
