package com.downloadmanager.app.encrypt

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlin.io.hex

class AESFileEncryptorTest {

    private lateinit var encryptor: AESFileEncryptor
    private lateinit var tempDir: File
    private val testPassword = "testPassword123"

    @Before
    fun setup() {
        encryptor = AESFileEncryptor()
        tempDir = createTempDir(prefix = "aes_test_")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testEncryptDecryptRoundTrip() = runBlocking {
        encryptor.initialize(testPassword)

        val originalContent = "Hello, World! This is a test message."
        val sourceFile = File(tempDir, "test.txt")
        val encryptedFile = File(tempDir, "test.enc")
        val decryptedFile = File(tempDir, "test_decrypted.txt")

        sourceFile.writeText(originalContent)

        val encryptResult = encryptor.encrypt(sourceFile, encryptedFile)
        assertTrue("Encryption should succeed", encryptResult)
        assertTrue("Encrypted file should exist", encryptedFile.exists())
        assertNotEquals(
            "Encrypted file should be different from source",
            sourceFile.readText(),
            encryptedFile.readText()
        )

        val decryptResult = encryptor.decrypt(encryptedFile, decryptedFile)
        assertTrue("Decryption should succeed", decryptResult)
        assertTrue("Decrypted file should exist", decryptedFile.exists())
        assertEquals(
            "Decrypted content should match original",
            originalContent,
            decryptedFile.readText()
        )
    }

    @Test
    fun testFileNameObfuscation() {
        val originalName = "document.pdf"
        val obfuscated = encryptor.obfuscateFileName(originalName)
        val deobfuscated = encryptor.deobfuscateFileName(obfuscated)

        assertNotEquals("Obfuscated name should differ from original", originalName, obfuscated)
        assertTrue("Obfuscated name should end with .enc", obfuscated.endsWith(".enc"))
        assertEquals("Deobfuscated name should match original", originalName, deobfuscated)
    }

    @Test
    fun testEncryptEmptyFile() = runBlocking {
        encryptor.initialize(testPassword)

        val sourceFile = File(tempDir, "empty.txt")
        val encryptedFile = File(tempDir, "empty.enc")
        val decryptedFile = File(tempDir, "empty_decrypted.txt")

        sourceFile.writeText("")

        val encryptResult = encryptor.encrypt(sourceFile, encryptedFile)
        assertTrue("Empty file encryption should succeed", encryptResult)
        assertTrue("Encrypted empty file should exist", encryptedFile.exists())

        val decryptResult = encryptor.decrypt(encryptedFile, decryptedFile)
        assertTrue("Empty file decryption should succeed", decryptResult)
        assertEquals("Decrypted empty file should be empty", "", decryptedFile.readText())
    }

    @Test
    fun testEncryptSmallFile() = runBlocking {
        encryptor.initialize(testPassword)

        val size = 1024
        val sourceFile = File(tempDir, "small.bin")
        val encryptedFile = File(tempDir, "small.enc")
        val decryptedFile = File(tempDir, "small_decrypted.bin")

        val content = ByteArray(size) { it.toByte() }
        sourceFile.writeBytes(content)

        val encryptResult = encryptor.encrypt(sourceFile, encryptedFile)
        assertTrue("Small file encryption should succeed", encryptResult)

        val decryptResult = encryptor.decrypt(encryptedFile, decryptedFile)
        assertTrue("Small file decryption should succeed", decryptResult)
        assertTrue("Decrypted content should match original", content.contentEquals(decryptedFile.readBytes()))
    }

    @Test
    fun testEncryptLargeFile() = runBlocking {
        encryptor.initialize(testPassword)

        val size = 1024 * 1024
        val sourceFile = File(tempDir, "large.bin")
        val encryptedFile = File(tempDir, "large.enc")
        val decryptedFile = File(tempDir, "large_decrypted.bin")

        val content = ByteArray(size) { (it % 256).toByte() }
        sourceFile.writeBytes(content)

        val originalHash = calculateHash(sourceFile)

        val encryptResult = encryptor.encrypt(sourceFile, encryptedFile)
        assertTrue("Large file encryption should succeed", encryptResult)

        val decryptResult = encryptor.decrypt(encryptedFile, decryptedFile)
        assertTrue("Large file decryption should succeed", decryptResult)

        val decryptedHash = calculateHash(decryptedFile)
        assertEquals("Large file hash should match after round trip", originalHash, decryptedHash)
    }

    @Test
    fun testDecryptWithWrongKey() = runBlocking {
        val sourceFile = File(tempDir, "test.txt")
        val encryptedFile = File(tempDir, "test.enc")
        val decryptedFile = File(tempDir, "test_decrypted.txt")

        sourceFile.writeText("Sensitive data")

        encryptor.initialize(testPassword)
        encryptor.encrypt(sourceFile, encryptedFile)

        val wrongEncryptor = AESFileEncryptor()
        wrongEncryptor.initialize("wrongPassword")

        val decryptResult = wrongEncryptor.decrypt(encryptedFile, decryptedFile)
        assertFalse("Decryption with wrong key should fail", decryptResult)
    }

    @Test
    fun testObfuscatedFileNamePreservesExtension() {
        val originalName = "image.png"
        val obfuscated = encryptor.obfuscateFileName(originalName)

        assertFalse(
            "Obfuscated name should not contain original extension",
            obfuscated.contains(".png", ignoreCase = true)
        )
        assertTrue("Obfuscated name should end with .enc", obfuscated.endsWith(".enc"))
    }

    @Test
    fun testInitializeSetsFlag() {
        assertFalse("Should not be initialized before initialize()", encryptor.isInitialized)

        encryptor.initialize(testPassword)
        assertTrue("Should be initialized after initialize()", encryptor.isInitialized)
    }

    @Test
    fun testEncryptDecryptLargeDataIntegrity() = runBlocking {
        encryptor.initialize(testPassword)

        val size = 512 * 1024
        val sourceFile = File(tempDir, "integrity_test.bin")
        val encryptedFile = File(tempDir, "integrity_test.enc")
        val decryptedFile = File(tempDir, "integrity_test_decrypted.bin")

        val random = java.util.Random(42)
        val content = ByteArray(size)
        random.nextBytes(content)
        sourceFile.writeBytes(content)

        val originalHash = calculateHash(sourceFile)

        val encryptResult = encryptor.encrypt(sourceFile, encryptedFile)
        assertTrue("Encryption should succeed", encryptResult)
        assertTrue(
            "Encrypted file should be larger than IV size",
            encryptedFile.length() > 12
        )

        val decryptResult = encryptor.decrypt(encryptedFile, decryptedFile)
        assertTrue("Decryption should succeed", decryptResult)

        val decryptedHash = calculateHash(decryptedFile)
        assertEquals("Data integrity should be preserved", originalHash, decryptedHash)
        assertEquals("File size should match after round trip", sourceFile.length(), decryptedFile.length())
    }

    private fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        return digest.digest(bytes).hex
    }
}
