package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.network.createHttpClient
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.core.settings.createSettings
import com.example.ais_sst_mobile.data.repository.AuthRepositoryImpl
import com.example.ais_sst_mobile.data.repository.DictionaryRepositoryImpl
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import com.example.ais_sst_mobile.presentation.auth.LoginScreenModel
import com.example.ais_sst_mobile.presentation.auth.RegisterScreenModel
import com.example.ais_sst_mobile.presentation.home.HomeScreenModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.russhwolf.settings.Settings

val coreModule = module {
    single { createHttpClient() }

    single<Settings> { createSettings() }

    single { SessionManager(get()) }
}

val appModule = module {
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }

    // ScreenModels
    factory { LoginScreenModel(get(), get()) }
    factory { RegisterScreenModel(dictionaryRepository = get()) }
    factory { HomeScreenModel() }
}

private var isKoinInitialized = false

fun initKoin() {
    if (!isKoinInitialized) {
        startKoin {
            modules(coreModule, appModule)
        }
        isKoinInitialized = true
    }
}