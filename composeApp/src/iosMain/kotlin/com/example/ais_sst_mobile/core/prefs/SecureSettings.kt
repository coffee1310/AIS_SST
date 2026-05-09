package com.example.ais_sst_mobile.core.prefs

import com.russhwolf.settings.Settings
import com.russhwolf.settings.KeychainSettings
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun provideSecureSettings(): Settings {
    return KeychainSettings(service = "ais_sst_secure_prefs")
}