package com.downloadmanager.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableSetOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.downloadmanager.common.TaskStatus
import com.downloadmanager.common.UrlParser
import com.downloadmanager.ui.category.CategoryScreen
import com.downloadmanager.ui.home.HomeScreen
import com.downloadmanager.ui.home.TaskDetailScreen
import com.downloadmanager.ui.home.TaskViewModel
import com.downloadmanager.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for shared URL from intent
        val sharedUrl = handleShareIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DownloadManagerApp(sharedUrl = sharedUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle share intent when app is already running
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent): String? {
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null && UrlParser.isValidUrl(sharedText)) {
                return sharedText
            }
        }
        return null
    }
}

sealed class BottomNavItem(val route: String, val title: String) {
    object Home : BottomNavItem("home", "首页")
    object Category : BottomNavItem("category", "分类")
    object Settings : BottomNavItem("settings", "设置")
}

@Composable
fun DownloadManagerApp(sharedUrl: String?) {
    val navController = rememberNavController()
    val navItems = listOf(BottomNavItem.Home, BottomNavItem.Category, BottomNavItem.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check clipboard for URL
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    remember {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (text != null && UrlParser.isValidUrl(text)) {
                clipboardUrl = text
            }
        }
    }

    val initialUrl = sharedUrl ?: clipboardUrl

    // APK auto-install: watch for completed APK downloads and trigger installation
    val taskViewModel: TaskViewModel = hiltViewModel()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val installedApkTaskIds = remember { mutableSetOf<String>() }

    LaunchedEffect(allTasks) {
        allTasks.forEach { task ->
            if (task.status == TaskStatus.COMPLETED &&
                task.savePath.endsWith(".apk", ignoreCase = true) &&
                task.id !in installedApkTaskIds
            ) {
                installedApkTaskIds.add(task.id)
                ApkInstaller.installApk(context, task.savePath)
            }
        }
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute in navItems.map { it.route }
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        BottomNavItem.Home -> Icons.Default.Home
                                        BottomNavItem.Category -> Icons.Default.Folder
                                        BottomNavItem.Settings -> Icons.Default.Settings
                                    },
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(BottomNavItem.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { taskId ->
                        navController.navigate("detail/$taskId")
                    }
                )
            }
            composable(BottomNavItem.Category.route) {
                CategoryScreen(
                    onNavigateToDetail = { taskId ->
                        navController.navigate("detail/$taskId")
                    }
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "detail/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                TaskDetailScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}