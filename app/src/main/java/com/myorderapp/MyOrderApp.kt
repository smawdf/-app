package com.myorderapp

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.myorderapp.di.appModule
import com.myorderapp.di.networkModule
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyOrderApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyOrderApp)
            modules(appModule, networkModule)
        }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(OkHttpClient()))
            }
            .crossfade(true)
            .build()
    }
}
