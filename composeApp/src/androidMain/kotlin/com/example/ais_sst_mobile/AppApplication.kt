package com.example.ais_sst_mobile

import android.app.Application
import com.example.ais_sst_mobile.di.initKoin
import org.koin.android.ext.koin.androidContext

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@AppApplication)
        }
    }
}