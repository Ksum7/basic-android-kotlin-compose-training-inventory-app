package com.example.inventory

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionUtil() {

    private val KEY_ALIAS = "file_encryption_key"
    private val IV_SIZE = 12
    private val TAG_SIZE = 16

    init {
        //deleteKeyIfExists()
        generateKeyIfNeeded()
    }

    private fun deleteKeyIfExists() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
        }
    }

    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .setKeySize(256)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun getDatabasePassphrase(): ByteArray {
        val secretKey = getSecretKey()
        return secretKey.encoded
            ?.takeIf { it.isNotEmpty() }
            ?: run {
                val salt = KEY_ALIAS.toByteArray()
                javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(javax.crypto.spec.PBEKeySpec(
                        KEY_ALIAS.toCharArray(), salt, 100000, 256
                    )).encoded
            }
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getSecretKey()
        val iv = ByteArray(IV_SIZE).apply { java.security.SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE * 8, iv))
        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(encryptedValue: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getSecretKey()
        val decodedBytes = Base64.decode(encryptedValue, Base64.DEFAULT)
        val iv = decodedBytes.copyOfRange(0, IV_SIZE)
        val encryptedPayload = decodedBytes.copyOfRange(IV_SIZE, decodedBytes.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE * 8, iv))
        val decryptedBytes = cipher.doFinal(encryptedPayload)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}