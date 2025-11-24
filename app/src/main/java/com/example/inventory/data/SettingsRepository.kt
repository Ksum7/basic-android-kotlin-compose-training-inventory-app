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
import com.example.inventory.EncryptionUtil

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE)

    private val encryptionUtil = EncryptionUtil()

    private val encryptedHideSensitiveKey = "enc_hide_sensitive"
    private val encryptedAllowShareKey = "enc_allow_share"
    private val encryptedUseDefaultQuantityKey = "enc_use_default_quantity"
    private val encryptedDefaultQuantityKey = "enc_default_quantity"

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
        prefs.edit() { putString(encryptedHideSensitiveKey, encryptionUtil.encrypt(value.toString())) }
    }

    fun updateAllowShare(value: Boolean) {
        prefs.edit() { putString(encryptedAllowShareKey, encryptionUtil.encrypt(value.toString())) }
    }

    fun updateUseDefaultQuantity(value: Boolean) {
        prefs.edit() { putString(encryptedUseDefaultQuantityKey, encryptionUtil.encrypt(value.toString())) }
    }

    fun updateDefaultQuantity(value: Int) {
        prefs.edit() { putString(encryptedDefaultQuantityKey, encryptionUtil.encrypt(value.toString())) }
    }

    private fun tryDecryptBoolean(encrypted: String?): Boolean {
        return try {
            encrypted?.let { encryptionUtil.decrypt(it).toBoolean() } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryDecryptInt(encrypted: String?): Int {
        return try {
            encrypted?.let { encryptionUtil.decrypt(it).toInt() } ?: 1
        } catch (e: Exception) {
            1
        }
    }
}