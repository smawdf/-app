package com.myorderapp.data.remote.recipe

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class BingDishImageSourceTest {

    @Test
    fun `parse image urls extracts bing murl values`() {
        val html = """
            <a class="iusc" m='{"murl":"https:\/\/example.com\/hongshao-paigu.jpg","turl":"https://www.bing.com/th?id=1"}'></a>
            <a class="iusc" m="{&quot;murl&quot;:&quot;https://img.example.cn/paigu.webp&quot;,&quot;turl&quot;:&quot;https://www.bing.com/th?id=2&quot;}"></a>
        """.trimIndent()

        val urls = BingDishImageSource(OkHttpClient())
            .parseImageUrls(html, limit = 8)

        assertEquals(
            listOf(
                "https://example.com/hongshao-paigu.jpg",
                "https://img.example.cn/paigu.webp"
            ),
            urls
        )
    }

    @Test
    fun `parse image urls decodes percent encoded mediaurl fallback`() {
        val html = """
            <a href="/images/search?view=detailV2&mediaurl=https%3A%2F%2Fimg.example.cn%2Ftangcu-liyu.jpg%3Fsize%3Dlarge&expw=800"></a>
        """.trimIndent()

        val urls = BingDishImageSource(OkHttpClient())
            .parseImageUrls(html, limit = 8)

        assertEquals(
            listOf("https://img.example.cn/tangcu-liyu.jpg?size=large"),
            urls
        )
    }

    @Test
    fun `parse image candidates keeps dish images and rejects unrelated bing images`() {
        val html = """
            <a class="iusc" m='{"murl":"https:\/\/img.example.cn\/fanqie-jidan.jpg","t":"番茄鸡蛋的家常做法"}'></a>
            <a class="iusc" m='{"murl":"https:\/\/img.example.cn\/anime-girl.jpg","t":"二次元少女壁纸"}'></a>
        """.trimIndent()

        val candidates = BingDishImageSource(OkHttpClient())
            .parseImageCandidates(html, dishName = "番茄鸡蛋", limit = 8)

        assertEquals(
            listOf(
                BingDishImageSource.ImageCandidate(
                    url = "https://img.example.cn/fanqie-jidan.jpg",
                    title = "番茄鸡蛋的家常做法"
                )
            ),
            candidates
        )
    }
}
