package com.downloadmanager.app.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.downloadmanager.app.ui.navigation.Destinations
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        Destinations.bottomNavItems.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(id = destination.titleRes)
                    )
                },
                label = {
                    Text(text = stringResource(id = destination.titleRes))
                }
            )
        }
    }
}

@Preview
@Composable
private fun BottomNavBarPreview() {
    DownloadManagerTheme {
        BottomNavBar(
            currentRoute = Destinations.Home.route,
            onNavigate = {}
        )
    }
}
