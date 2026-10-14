package com.example.tripchecker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tripchecker.di.AppContainer
import com.example.tripchecker.ui.home.HomeScreen
import com.example.tripchecker.ui.home.HomeViewModel
import com.example.tripchecker.ui.importflow.ImportScreen
import com.example.tripchecker.ui.importflow.ImportViewModel
import com.example.tripchecker.ui.trip.TripDetailScreen
import com.example.tripchecker.ui.trip.TripDetailViewModel
import com.example.tripchecker.ui.edit.EditTicketScreen

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    startOnImport: Boolean = false,
    onPdfsSelected: (List<android.net.Uri>) -> Unit,
) {
    if (startOnImport) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            navController.navigate(Import) { launchSingleTop = true }
        }
    }
    androidx.compose.runtime.LaunchedEffect(container) {
        container.openImportEvents.collect {
            navController.navigate(Import) { launchSingleTop = true }
        }
    }
    val navAnimDurationMs = rememberNavAnimationDurationMillis()
    NavHost(
        navController = navController,
        startDestination = Home,
        enterTransition = { appEnterTransition(navAnimDurationMs) },
        exitTransition = { appExitTransition(navAnimDurationMs) },
        popEnterTransition = { appPopEnterTransition(navAnimDurationMs) },
        popExitTransition = { appPopExitTransition(navAnimDurationMs) },
    ) {
        composable<Home> {
            val vm: HomeViewModel = viewModel(factory = container.viewModelFactory)
            HomeScreen(
                viewModel = vm,
                fileStore = container.ticketFileStore,
                onOpenTrip = { navController.navigate(TripDetail(it)) },
                onEditTicket = { navController.navigate(EditTicket(it)) },
                onPdfsSelected = { uris ->
                    onPdfsSelected(uris)
                    navController.navigate(Import) { launchSingleTop = true }
                },
                onAddManual = { navController.navigate(EditTicket(null)) },
                onLinkReturn = { navController.navigate(TripDetail(it, openLinkSheet = true)) },
            )
        }
        composable<Import> {
            val vm: ImportViewModel = viewModel(factory = container.viewModelFactory)
            ImportScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
                onOpenTrip = { navController.navigate(TripDetail(it)) },
                onEditTicket = { navController.navigate(EditTicket(it)) },
            )
        }
        composable<TripDetail> { entry ->
            val route = entry.toRoute<TripDetail>()
            val vm: TripDetailViewModel =
                viewModel(factory = TripDetailViewModel.Factory(container, route.anchorTicketId))
            TripDetailScreen(
                viewModel = vm,
                fileStore = container.ticketFileStore,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EditTicket(it)) },
                openLinkSheet = route.openLinkSheet,
            )
        }
        composable<EditTicket> { entry ->
            val route = entry.toRoute<EditTicket>()
            val editVm: com.example.tripchecker.ui.edit.EditTicketViewModel =
                viewModel(
                    key = route.ticketId ?: "create",
                    factory = com.example.tripchecker.ui.edit.EditTicketViewModel.Factory(container, route.ticketId),
                )
            EditTicketScreen(
                viewModel = editVm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
