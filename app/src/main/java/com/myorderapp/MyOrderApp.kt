package com.myorderapp

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.myorderapp.di.appModule
import com.myorderapp.di.networkModule
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
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
        val imageClient = OkHttpClient.Builder()
            .addInterceptor(DishImageHeaderInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(imageClient))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache").toOkioPath())
                    .maxSizePercent(0.03)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private object DishImageHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            val builder = request.newBuilder()
                .header("User-Agent", IMAGE_USER_AGENT)

            val host = request.url.host
            if (host.endsWith("chuimg.com") || host.endsWith("xiachufang.com")) {
                builder.header("Referer", "https://www.xiachufang.com/")
            }

            return chain.proceed(builder.build())
        }
    }

    private companion object {
        const val IMAGE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
    }
}
