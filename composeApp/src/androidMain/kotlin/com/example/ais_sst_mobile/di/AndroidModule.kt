package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.prefs.provideSecureSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<com.russhwolf.settings.Settings> { provideSecureSettings(androidContext()) }
}