package com.myorderapp

import android.app.Application
import com.myorderapp.di.appModule
import com.myorderapp.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyOrderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyOrderApp)
            modules(appModule, networkModule)
        }
    }
}
