package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.network.createHttpClient
import com.example.ais_sst_mobile.core.settings.createSettings
import com.example.ais_sst_mobile.data.repository.AuthRepositoryImpl
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.example.ais_sst_mobile.presentation.auth.LoginScreenModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val coreModule = module {
    single { createHttpClient() }
    single { createSettings() }
}

val featureModule = module {
    // Repository
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // ScreenModels (в Voyager ScreenModel создаются через factory)
    factory { LoginScreenModel(get()) }
}

fun initKoin() {
    startKoin {
        modules(coreModule, featureModule)
    }
}