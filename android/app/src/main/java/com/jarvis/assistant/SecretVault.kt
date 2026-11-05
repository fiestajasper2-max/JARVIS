package com.jarvis.assistant

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecretVault(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_secure_settings",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasKey(): Boolean = !prefs.getString(KEY, null).isNullOrBlank()
    fun getKey(): String? = prefs.getString(KEY, null)
    fun saveKey(value: String) { prefs.edit().putString(KEY, value).apply() }
    fun clearKey() { prefs.edit().remove(KEY).apply() }

    private companion object { const val KEY = "openrouter_api_key" }
}
