package com.example.ais_sst_mobile.di

import com.example.ais_sst_mobile.core.network.createHttpClient
import com.example.ais_sst_mobile.core.notifications.NotificationService
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.repository.*
import com.example.ais_sst_mobile.domain.repository.*
import com.example.ais_sst_mobile.presentation.auth.*
import com.example.ais_sst_mobile.presentation.events.create.CreateEventScreenModel
import com.example.ais_sst_mobile.presentation.home.*
import com.example.ais_sst_mobile.presentation.home.details.AvailableEventDetailsScreenModel
import com.example.ais_sst_mobile.presentation.home.details.EventDetailsScreenModel
import com.example.ais_sst_mobile.presentation.home.details.UpcomingEventDetailsScreenModel
import com.example.ais_sst_mobile.presentation.profile.*
import com.example.ais_sst_mobile.presentation.profile.activist.ActivistProfileScreenModel
import com.example.ais_sst_mobile.presentation.profile.board.BoardScreenModel
import com.example.ais_sst_mobile.presentation.profile.event_roles.EventRolesScreenModel
import com.example.ais_sst_mobile.presentation.profile.event_roles.create.CreateRoleScreenModel
import com.example.ais_sst_mobile.presentation.profile.event_roles.edit.EditRoleScreenModel
import com.example.ais_sst_mobile.presentation.profile.my_data.*
import com.example.ais_sst_mobile.presentation.profile.requests.*
import com.example.ais_sst_mobile.presentation.sectors.*
import com.example.ais_sst_mobile.presentation.sectors.create.CreateSectorScreenModel
import com.example.ais_sst_mobile.presentation.sectors.edit.EditSectorScreenModel
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
    single<EventsRepository> { EventsRepositoryImpl(get()) }
    single { NotificationService(get(), get()) }

    // ScreenModels
    factory { LoginScreenModel(get(), get()) }
    factory { RegisterScreenModel(get(), get()) }
    factory { HomeScreenModel(get(), get(), get()) }
    factory { ProfileScreenModel(get(), get()) }
    factory { AccountRequestsScreenModel(get()) }
    factory { MyDataScreenModel(get()) }
    factory { RequestDetailsScreenModel(get()) }
    factory { SectorsScreenModel(get(), get(), get()) }
    factory { SectorDetailsScreenModel(get(), get()) }
    factory { ActivistProfileScreenModel(get(), get()) }
    factory { CreateSectorScreenModel(get(), get()) }
    factory { BoardScreenModel(get(), get(), get()) }
    factory { EditSectorScreenModel(get(), get()) }
    factory { EventRolesScreenModel(get()) }
    factory { CreateRoleScreenModel(get(), get()) }
    factory { EditRoleScreenModel(get(), get()) }
    factory { CoordinatorHomeScreenModel(get(), get()) }
    factory { UpcomingEventDetailsScreenModel(get()) }
    factory { EventDetailsScreenModel(get(), get()) }
    factory { CreateEventScreenModel(get(), get(), get()) }
    factory { ChairmanHomeScreenModel(get(), get()) }
    factory { AvailableEventDetailsScreenModel(get(), get()) }
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