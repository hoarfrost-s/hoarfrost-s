package com.downloadmanager.app.encrypt

import java.io.File
import javax.crypto.SecretKey

interface FileEncryptor {

    suspend fun encrypt(source: File, destination: File): Boolean

    suspend fun decrypt(source: File, destination: File): Boolean

    fun generateKey(password: String): SecretKey

    fun obfuscateFileName(originalName: String): String

    fun deobfuscateFileName(obfuscatedName: String): String

    fun initialize(password: String)

    val isInitialized: Boolean
}
