package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.network.createHttpClient
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.core.settings.createSettings
import com.example.ais_sst_mobile.data.repository.AuthRepositoryImpl
import com.example.ais_sst_mobile.data.repository.DictionaryRepositoryImpl
import com.example.ais_sst_mobile.data.repository.UserRepositoryImpl
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import com.example.ais_sst_mobile.presentation.auth.LoginScreenModel
import com.example.ais_sst_mobile.presentation.auth.RegisterScreenModel
import com.example.ais_sst_mobile.presentation.home.HomeScreenModel
import com.example.ais_sst_mobile.presentation.profile.ProfileScreenModel
import com.example.ais_sst_mobile.presentation.profile.my_data.MyDataScreenModel
import com.example.ais_sst_mobile.presentation.profile.requests.AccountRequestsScreenModel
import com.example.ais_sst_mobile.presentation.profile.requests.RequestDetailsScreenModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.russhwolf.settings.Settings

val coreModule = module {
    single { createHttpClient(get()) }

    single<Settings> { createSettings() }

    single { SessionManager(get()) }
}

val appModule = module {
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }

    // ScreenModels
    factory { LoginScreenModel(get(), get()) }
    factory { RegisterScreenModel(get(), get()) }
    factory { HomeScreenModel(get()) }
    factory { ProfileScreenModel(get(), get()) }
    factory { AccountRequestsScreenModel(get()) }
    factory { MyDataScreenModel(get()) }
    factory { RequestDetailsScreenModel(get()) }

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