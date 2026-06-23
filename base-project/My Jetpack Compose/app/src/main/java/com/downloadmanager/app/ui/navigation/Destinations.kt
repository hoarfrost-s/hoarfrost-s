package com.downloadmanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.downloadmanager.app.R

sealed class Destinations(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : Destinations(
        route = "home",
        titleRes = R.string.nav_home,
        icon = Icons.Default.Home
    )

    data object Downloads : Destinations(
        route = "downloads",
        titleRes = R.string.nav_downloads,
        icon = Icons.Default.Download
    ) {
        const val categoryIdArg = "categoryId"
        val routeWithArgs = "$route/{$categoryIdArg}"

        fun createRoute(categoryId: String? = null): String {
            return if (categoryId != null) {
                "$route/$categoryId"
            } else {
                route
            }
        }
    }

    data object Settings : Destinations(
        route = "settings",
        titleRes = R.string.nav_settings,
        icon = Icons.Default.Settings
    )

    data object CategoryManage : Destinations(
        route = "category_manage",
        titleRes = R.string.nav_category_manage,
        icon = Icons.Default.Category
    )

    companion object {
        val bottomNavItems = listOf(Home, Downloads, Settings)
    }
}
