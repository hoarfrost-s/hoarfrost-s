package com.downloadmanager.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.downloadmanager.app.ui.component.AppTopBar
import com.downloadmanager.app.ui.component.SettingsGroupHeader
import com.downloadmanager.app.ui.component.SettingsItemNormal
import com.downloadmanager.app.ui.component.SettingsItemSlider
import com.downloadmanager.app.ui.component.SettingsItemSwitch
import com.downloadmanager.app.ui.theme.DownloadManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategoryManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(title = "设置")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsGroupHeader(title = "下载设置")
            SettingsItemSlider(
                title = "最大线程数",
                value = 3,
                valueRange = 1f..9f,
                icon = Icons.Default.Speed,
                valueLabel = "3 线程",
                onValueChange = {}
            )
            SettingsItemSwitch(
                title = "文件加密",
                subtitle = "开启后下载的文件将被加密存储",
                icon = Icons.Default.Lock,
                checked = false,
                onCheckedChange = {}
            )

            SettingsGroupHeader(title = "分类管理")
            SettingsItemNormal(
                title = "分类管理",
                subtitle = "管理下载文件的分类规则",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategoryManage
            )
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    DownloadManagerTheme {
        SettingsScreen(
            onNavigateToCategoryManage = {}
        )
    }
}
