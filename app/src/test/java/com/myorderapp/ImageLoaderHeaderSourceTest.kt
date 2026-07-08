package com.myorderapp

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageLoaderHeaderSourceTest {

    @Test
    fun `image loader sends headers required by xiachufang images`() {
        val source = Files.readString(
            listOf(
                Paths.get("src/main/java/com/myorderapp/MyOrderApp.kt"),
                Paths.get("app/src/main/java/com/myorderapp/MyOrderApp.kt")
            ).first { Files.exists(it) }
        )

        assertTrue(source.contains("DishImageHeaderInterceptor"))
        assertTrue(source.contains("User-Agent"))
        assertTrue(source.contains("Referer"))
        assertTrue(source.contains("chuimg.com"))
        assertTrue(source.contains("xiachufang.com"))
        assertTrue(source.contains("OkHttpNetworkFetcherFactory(imageClient)"))
    }
}
