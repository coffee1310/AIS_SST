package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.network.createHttpClient
import com.example.ais_sst_mobile.core.prefs.SessionManager
// Удали импорт старого createSettings, если он был
import com.example.ais_sst_mobile.data.repository.*
import com.example.ais_sst_mobile.domain.repository.*
import com.example.ais_sst_mobile.presentation.auth.*
import com.example.ais_sst_mobile.presentation.home.*
import com.example.ais_sst_mobile.presentation.profile.*
import com.example.ais_sst_mobile.presentation.profile.my_data.*
import com.example.ais_sst_mobile.presentation.profile.requests.*
import com.example.ais_sst_mobile.presentation.sectors.*
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.core.module.Module

expect fun platformModule(): Module
val coreModule = module {
    single { createHttpClient(get()) }
    single { SessionManager(get()) }
}

val appModule = module {
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<SectorsRepository> { SectorsRepositoryImpl(get()) }

    // ScreenModels
    factory { LoginScreenModel(get(), get()) }
    factory { RegisterScreenModel(get(), get()) }
    factory { HomeScreenModel(get()) }
    factory { ProfileScreenModel(get(), get()) }
    factory { AccountRequestsScreenModel(get()) }
    factory { MyDataScreenModel(get()) }
    factory { RequestDetailsScreenModel(get()) }
    factory { SectorsScreenModel(get(), get(), get()) }
    factory { SectorDetailsScreenModel(get(), get()) }

}

private var isKoinInitialized = false

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    if (!isKoinInitialized) {
        startKoin {
            appDeclaration()
            modules(platformModule(), coreModule, appModule)
        }
        isKoinInitialized = true
    }
}