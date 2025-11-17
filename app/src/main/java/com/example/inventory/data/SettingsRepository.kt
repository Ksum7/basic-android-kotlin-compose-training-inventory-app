package com.example.inventory.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE)

    private val KEY_ALIAS = "settings_encryption_key"
    private val IV_SIZE = 12 // GCM IV size in bytes
    private val TAG_SIZE = 16 // GCM tag size in bytes

    private val encryptedHideSensitiveKey = "enc_hide_sensitive"
    private val encryptedAllowShareKey = "enc_allow_share"
    private val encryptedUseDefaultQuantityKey = "enc_use_default_quantity"
    private val encryptedDefaultQuantityKey = "enc_default_quantity"

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

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getSecretKey()
        val iv = ByteArray(IV_SIZE).apply { java.security.SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE * 8, iv))
        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encryptedBytes, Base64.DEFAULT)
    }

    private fun decrypt(encryptedValue: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getSecretKey()
        val decodedBytes = Base64.decode(encryptedValue, Base64.DEFAULT)
        val iv = decodedBytes.copyOfRange(0, IV_SIZE)
        val encryptedPayload = decodedBytes.copyOfRange(IV_SIZE, decodedBytes.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE * 8, iv))
        val decryptedBytes = cipher.doFinal(encryptedPayload)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    data class Settings(
        val hideSensitive: Boolean = false,
        val allowShare: Boolean = true,
        val useDefaultQuantity: Boolean = false,
        val defaultQuantity: Int = 1
    )

    fun getSettings(): Settings = Settings(
        hideSensitive = tryDecryptBoolean(prefs.getString(encryptedHideSensitiveKey, null)),
        allowShare = tryDecryptBoolean(prefs.getString(encryptedAllowShareKey, null)),
        useDefaultQuantity = tryDecryptBoolean(prefs.getString(encryptedUseDefaultQuantityKey, null)),
        defaultQuantity = tryDecryptInt(prefs.getString(encryptedDefaultQuantityKey, null))
    )

    fun updateHideSensitive(value: Boolean) {
        prefs.edit() { putString(encryptedHideSensitiveKey, encrypt(value.toString())) }
    }

    fun updateAllowShare(value: Boolean) {
        prefs.edit() { putString(encryptedAllowShareKey, encrypt(value.toString())) }
    }

    fun updateUseDefaultQuantity(value: Boolean) {
        prefs.edit() { putString(encryptedUseDefaultQuantityKey, encrypt(value.toString())) }
    }

    fun updateDefaultQuantity(value: Int) {
        prefs.edit() { putString(encryptedDefaultQuantityKey, encrypt(value.toString())) }
    }

    private fun tryDecryptBoolean(encrypted: String?): Boolean {
        return try {
            encrypted?.let { decrypt(it).toBoolean() } ?: false
        } catch (e: Exception) {
            false // Fallback on error
        }
    }

    private fun tryDecryptInt(encrypted: String?): Int {
        return try {
            encrypted?.let { decrypt(it).toInt() } ?: 1
        } catch (e: Exception) {
            1 // Fallback on error
        }
    }
}