package com.example.server

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class E2eeCryptoManager {
    private val secureRandom = SecureRandom()
    private val gcmTagLengthBits = 128
    private val ivLengthBytes = 12

    private var currentPasskey: String = ""
    private var cachedKeySpec: SecretKeySpec? = null

    @Synchronized
    fun setPasskey(passkey: String) {
        if (passkey != currentPasskey || cachedKeySpec == null) {
            currentPasskey = passkey
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(passkey.toByteArray(Charsets.UTF_8))
            cachedKeySpec = SecretKeySpec(keyBytes, "AES")
        }
    }

    /**
     * Encrypts plaintext bytes (e.g., JPEG image frame or PCM audio chunk)
     * Output format: [12-byte IV] + [Ciphertext with 16-byte GCM tag]
     */
    @Synchronized
    fun encryptFrame(plainBytes: ByteArray): ByteArray? {
        val keySpec = cachedKeySpec ?: return null
        return try {
            val iv = ByteArray(ivLengthBytes)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(gcmTagLengthBits, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

            val cipherBytes = cipher.doFinal(plainBytes)

            val result = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(cipherBytes, 0, result, iv.size, cipherBytes.size)
            result
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun decryptFrame(encryptedBytes: ByteArray): ByteArray? {
        val keySpec = cachedKeySpec ?: return null
        if (encryptedBytes.size < ivLengthBytes + 16) return null
        return try {
            val iv = ByteArray(ivLengthBytes)
            System.arraycopy(encryptedBytes, 0, iv, 0, ivLengthBytes)

            val cipherLength = encryptedBytes.size - ivLengthBytes
            val cipherBytes = ByteArray(cipherLength)
            System.arraycopy(encryptedBytes, ivLengthBytes, cipherBytes, 0, cipherLength)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(gcmTagLengthBits, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            null
        }
    }
}
