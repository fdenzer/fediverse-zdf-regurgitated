package com.example.fediversezdfregurgitated.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// This class handles secure storage of the OAuth access token using EncryptedSharedPreferences.
class SecureTokenStorage(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "auth_token_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val KEY_ALIAS = "fediverse_app_master_key" // Alias for the master key in Android Keystore
    }

    // Specification for the master key used by EncryptedSharedPreferences
    private val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        setKeySize(256)
    }.build()

    private val masterKey = MasterKey.Builder(context, KEY_ALIAS)
        .setKeyGenParameterSpec(keyGenParameterSpec)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String) {
        with(sharedPreferences.edit()) {
            putString(ACCESS_TOKEN_KEY, token)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    fun clearAccessToken() {
        with(sharedPreferences.edit()) {
            remove(ACCESS_TOKEN_KEY)
            apply()
        }
    }

    fun hasAccessToken(): Boolean {
        return sharedPreferences.contains(ACCESS_TOKEN_KEY)
    }
}
