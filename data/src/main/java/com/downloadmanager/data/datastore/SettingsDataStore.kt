package com.downloadmanager.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.downloadmanager.domain.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigRepository {

    private object Keys {
        val MAX_CONCURRENCY = intPreferencesKey("max_concurrency")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val SPEED_LIMIT = longPreferencesKey("speed_limit")
        val DEFAULT_SAVE_PATH = stringPreferencesKey("default_save_path")
        val CELLULAR_AUTO_LIMIT = booleanPreferencesKey("cellular_auto_limit")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val THEME_MODE = intPreferencesKey("theme_mode")
    }

    override val maxConcurrency: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_CONCURRENCY] ?: 3
    }

    override val threadCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.THREAD_COUNT] ?: 4
    }

    override val speedLimit: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.SPEED_LIMIT] ?: 0L
    }

    override val defaultSavePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_SAVE_PATH] ?: "Download/DM"
    }

    override val cellularAutoLimit: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CELLULAR_AUTO_LIMIT] ?: true
    }

    override val maxRetries: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_RETRIES] ?: 3
    }

    override val themeMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: 0
    }

    override suspend fun setMaxConcurrency(value: Int) {
        context.dataStore.edit { it[Keys.MAX_CONCURRENCY] = value }
    }

    override suspend fun setThreadCount(value: Int) {
        context.dataStore.edit { it[Keys.THREAD_COUNT] = value }
    }

    override suspend fun setSpeedLimit(bytesPerSecond: Long) {
        context.dataStore.edit { it[Keys.SPEED_LIMIT] = bytesPerSecond }
    }

    override suspend fun setDefaultSavePath(path: String) {
        context.dataStore.edit { it[Keys.DEFAULT_SAVE_PATH] = path }
    }

    override suspend fun setCellularAutoLimit(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CELLULAR_AUTO_LIMIT] = enabled }
    }

    override suspend fun setMaxRetries(value: Int) {
        context.dataStore.edit { it[Keys.MAX_RETRIES] = value }
    }

    override suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }
}