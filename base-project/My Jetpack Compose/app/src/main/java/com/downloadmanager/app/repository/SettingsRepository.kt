package com.downloadmanager.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsData(
    val threadCount: Int = 3,
    val maxConcurrent: Int = 3,
    val globalSpeedLimit: Long = 0L,
    val downloadRootPath: String = "/sdcard/DownloadManager",
    val enableEncryption: Boolean = true,
    val themeMode: String = "system",
    val enableClipboardDetect: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val THREAD_COUNT = intPreferencesKey("setting_thread_count")
        val MAX_CONCURRENT = intPreferencesKey("setting_max_concurrent")
        val GLOBAL_SPEED_LIMIT = longPreferencesKey("setting_global_speed_limit")
        val DOWNLOAD_ROOT_PATH = stringPreferencesKey("setting_download_root_path")
        val ENABLE_ENCRYPTION = booleanPreferencesKey("setting_enable_encryption")
        val THEME_MODE = stringPreferencesKey("setting_theme_mode")
        val ENABLE_CLIPBOARD_DETECT = booleanPreferencesKey("setting_enable_clipboard_detect")
    }

    val settingsFlow: Flow<SettingsData> = context.dataStore.data.map { preferences ->
        SettingsData(
            threadCount = preferences[PreferencesKeys.THREAD_COUNT] ?: 3,
            maxConcurrent = preferences[PreferencesKeys.MAX_CONCURRENT] ?: 3,
            globalSpeedLimit = preferences[PreferencesKeys.GLOBAL_SPEED_LIMIT] ?: 0L,
            downloadRootPath = preferences[PreferencesKeys.DOWNLOAD_ROOT_PATH]
                ?: "/sdcard/DownloadManager",
            enableEncryption = preferences[PreferencesKeys.ENABLE_ENCRYPTION] ?: true,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
            enableClipboardDetect = preferences[PreferencesKeys.ENABLE_CLIPBOARD_DETECT] ?: true
        )
    }

    suspend fun setThreadCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THREAD_COUNT] = count
        }
    }

    suspend fun setMaxConcurrent(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_CONCURRENT] = count
        }
    }

    suspend fun setGlobalSpeedLimit(bytesPerSecond: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOBAL_SPEED_LIMIT] = bytesPerSecond
        }
    }

    suspend fun setDownloadRootPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_ROOT_PATH] = path
        }
    }

    suspend fun setEnableEncryption(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_ENCRYPTION] = enable
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setEnableClipboardDetect(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_CLIPBOARD_DETECT] = enable
        }
    }
}
