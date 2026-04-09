package com.example.ais_sst_mobile

import android.app.Application
import com.example.ais_sst_mobile.di.initKoin

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}