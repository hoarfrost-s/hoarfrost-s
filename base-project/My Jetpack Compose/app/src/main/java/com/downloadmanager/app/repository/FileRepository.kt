package com.downloadmanager.app.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun ensureDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists()) {
                dir.isDirectory
            } else {
                dir.mkdirs()
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveFile(from: String, to: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = File(from)
            val dest = File(to)
            if (!source.exists()) return@withContext false
            dest.parentFile?.mkdirs()
            source.renameTo(dest)
        } catch (e: Exception) {
            false
        }
    }

    fun getAvailableStorageSpace(path: String): Long {
        return try {
            val file = File(path)
            val parent = if (file.isDirectory) file else file.parentFile
            parent?.freeSpace ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun getFileUri(filePath: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}
