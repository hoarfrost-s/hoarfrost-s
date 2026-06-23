package com.downloadmanager.app.encrypt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class AESFileEncryptor @Inject constructor() : FileEncryptor {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        private const val OBFUSCATION_SUFFIX = ".enc"
        private const val BUFFER_SIZE = 4096
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 10000
        private val SALT = byteArrayOf(
            0x44, 0x6f, 0x77, 0x6e, 0x6c, 0x6f, 0x61, 0x64,
            0x4d, 0x61, 0x6e, 0x61, 0x67, 0x65, 0x72, 0x53
        )
    }

    private var secretKey: SecretKey? = null
    override var isInitialized: Boolean = false
        private set

    override fun initialize(password: String) {
        secretKey = generateKey(password)
        isInitialized = true
    }

    override fun generateKey(password: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(
            password.toCharArray(),
            SALT,
            ITERATION_COUNT,
            KEY_SIZE
        )
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    override fun obfuscateFileName(originalName: String): String {
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(originalName.toByteArray(Charsets.UTF_8))
        return encoded + OBFUSCATION_SUFFIX
    }

    override fun deobfuscateFileName(obfuscatedName: String): String {
        val nameWithoutSuffix = obfuscatedName.removeSuffix(OBFUSCATION_SUFFIX)
        val decoded = Base64.getUrlDecoder().decode(nameWithoutSuffix)
        return String(decoded, Charsets.UTF_8)
    }

    override suspend fun encrypt(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        val key = secretKey ?: return@withContext false
        if (!source.exists()) return@withContext false

        var fileInputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        var cipherOutputStream: CipherOutputStream? = null

        try {
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            fileInputStream = FileInputStream(source)
            fileOutputStream = FileOutputStream(destination)
            fileOutputStream.write(iv)

            cipherOutputStream = CipherOutputStream(fileOutputStream, cipher)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead)
            }

            cipherOutputStream.flush()
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                cipherOutputStream?.close()
                fileOutputStream?.close()
                fileInputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun decrypt(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        val key = secretKey ?: return@withContext false
        if (!source.exists()) return@withContext false
        if (source.length() < IV_SIZE) return@withContext false

        var fileInputStream: FileInputStream? = null
        var cipherInputStream: CipherInputStream? = null
        var fileOutputStream: FileOutputStream? = null

        try {
            fileInputStream = FileInputStream(source)
            val iv = ByteArray(IV_SIZE)
            val ivRead = fileInputStream.read(iv)
            if (ivRead != IV_SIZE) return@withContext false

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            cipherInputStream = CipherInputStream(fileInputStream, cipher)
            fileOutputStream = FileOutputStream(destination)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (cipherInputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
            }

            fileOutputStream.flush()
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                fileOutputStream?.close()
                cipherInputStream?.close()
                fileInputStream?.close()
            } catch (_: Exception) {
            }
        }
    }
}
