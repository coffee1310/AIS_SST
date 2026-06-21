package com.example.ais_sst_mobile.core.prefs

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import java.io.File

fun provideSecureSettings(context: Context): Settings {
    val prefName = "secure_auth_prefs"

    return try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            prefName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        SharedPreferencesSettings(encryptedPrefs)

    } catch (e: Exception) {
        Log.e("SecureSettings", "Ошибка расшифровки SharedPreferences. Файл поврежден или восстановлен из бэкапа без ключа. Создаем новый...", e)

        context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()

        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefFile = File(dir, "$prefName.xml")
        if (prefFile.exists()) {
            prefFile.delete()
        }

        val newMasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val newEncryptedPrefs = EncryptedSharedPreferences.create(
            context,
            prefName,
            newMasterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        SharedPreferencesSettings(newEncryptedPrefs)
    }
}