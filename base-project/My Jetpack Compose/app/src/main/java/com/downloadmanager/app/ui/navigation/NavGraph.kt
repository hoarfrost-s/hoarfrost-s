package com.downloadmanager.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.downloadmanager.app.ui.download.DownloadsScreen
import com.downloadmanager.app.ui.screen.CategoryManageScreen
import com.downloadmanager.app.ui.screen.HomeScreen
import com.downloadmanager.app.ui.screen.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: com.downloadmanager.app.viewmodel.HomeViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.Home.route,
        modifier = modifier
    ) {
        composable(route = Destinations.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = {
                    navController.navigate(Destinations.Settings.route)
                },
                onNavigateToDownloads = { categoryId ->
                    navController.navigate(Destinations.Downloads.createRoute(categoryId))
                }
            )
        }
        composable(
            route = Destinations.Downloads.routeWithArgs,
            arguments = listOf(
                navArgument(Destinations.Downloads.categoryIdArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString(Destinations.Downloads.categoryIdArg)
            DownloadsScreen(
                categoryId = categoryId
            )
        }
        composable(route = Destinations.Settings.route) {
            SettingsScreen(
                onNavigateToCategoryManage = {
                    navController.navigate(Destinations.CategoryManage.route)
                }
            )
        }
        composable(route = Destinations.CategoryManage.route) {
            CategoryManageScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
