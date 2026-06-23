package com.downloadmanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.downloadmanager.app.repository.SettingsRepository
import com.downloadmanager.app.ui.component.AppTopBar
import com.downloadmanager.app.ui.component.BottomNavBar
import com.downloadmanager.app.ui.navigation.Destinations
import com.downloadmanager.app.ui.navigation.NavGraph
import com.downloadmanager.app.ui.theme.DownloadManagerTheme
import com.downloadmanager.app.util.ClipboardHelper
import com.downloadmanager.app.util.NotificationHelper
import com.downloadmanager.app.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var clipboardHelper: ClipboardHelper

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private lateinit var homeViewModel: HomeViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationHelper.createNotificationChannel()
        requestNotificationPermission()

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.downloadmanager.app.repository.SettingsData()
            )

            homeViewModel = viewModel()

            DownloadManagerTheme(
                themeMode = settings.themeMode
            ) {
                DownloadManagerApp(homeViewModel = homeViewModel)
            }
        }

        handleSendIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendIntent(intent)
        handleNotificationAction(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::homeViewModel.isInitialized) {
            homeViewModel.checkClipboard()
        }
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                val url = extractUrl(sharedText)
                if (url != null && ::homeViewModel.isInitialized) {
                    homeViewModel.handleShareUrl(url)
                }
            }
        }
    }

    private fun handleNotificationAction(intent: Intent?) {
        if (intent == null || intent.action == null) return

        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val action = intent.action ?: return

        if (!::homeViewModel.isInitialized) return

        when (action) {
            NotificationHelper.ACTION_OPEN -> {
                homeViewModel.openFileFromNotification(taskId)
            }
            NotificationHelper.ACTION_SHARE -> {
                homeViewModel.shareFileFromNotification(taskId)
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        return urlPattern.find(text)?.value
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun DownloadManagerApp(
    homeViewModel: HomeViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.Home.route

    val showBottomBar = currentRoute in Destinations.bottomNavItems.map { it.route }
    val currentDestination = when (currentRoute) {
        Destinations.Home.route -> Destinations.Home
        Destinations.Downloads.route -> Destinations.Downloads
        Destinations.Settings.route -> Destinations.Settings
        Destinations.CategoryManage.route -> Destinations.CategoryManage
        else -> Destinations.Home
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = androidx.compose.ui.res.stringResource(id = currentDestination.titleRes),
                showBackButton = !showBottomBar,
                onBackClick = { navController.navigateUp() }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavGraph(
                navController = navController,
                homeViewModel = homeViewModel
            )
        }
    }
}
