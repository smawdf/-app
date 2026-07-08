package com.myorderapp.data.remote.recipe

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiachufangRecipeSearchSourceTest {

    @Test
    fun `parse search results extracts title image and recipe url`() {
        val html = """
            <div class="recipe recipe-215-horizontal pure-g image-link display-block">
                <a href="/recipe/100352761/" target="_blank">
                    <div class="cover pure-u">
                        <img src="placeholder.png" data-src="https://i2.chuimg.com/f3067a6e886111e6b87c0242ac110003_640w_640h.jpg?imageView2/1/w/215/h/136/interlace/1/q/75" alt="鱼香肉丝" />
                    </div>
                </a>
                <div class="info pure-u">
                    <p class="name">
                        <a href="/recipe/100352761/" target="_blank">鱼香肉丝</a>
                    </p>
                </div>
            </div>
        """.trimIndent()

        val results = XiachufangRecipeSearchSource(client = OkHttpClient())
            .parseSearchResults(html)

        assertEquals(1, results.size)
        assertEquals("鱼香肉丝", results.first().name)
        assertEquals("xiachufang", results.first().source)
        assertEquals("https://www.xiachufang.com/recipe/100352761/", results.first().description)
        assertTrue(results.first().imageUrl.orEmpty().contains("chuimg.com"))
    }

    @Test
    fun `parse search results handles compact recipe blocks and keeps matching image nearby`() {
        val html = """
            <div class="recipe recipe-215-horizontal">
                <a href="/recipe/111111/"><img data-src="//i2.chuimg.com/aaaa_640w_640h.jpg" alt="别的菜" /></a>
            </div>
            <div class="recipe recipe-215-horizontal">
                <a href="/recipe/104672609/" class="cover-link">
                    <img srcset="//i2.chuimg.com/cd2022e5260f43fbba37aa82ce03e610_1128w_1128h.jpg?imageView2/1/w/215/h/136/interlace/1/q/75 1x" alt="豉汁蒸排骨" />
                </a>
                <a class="name" href="/recipe/104672609/">豉汁蒸排骨</a>
            </div>
        """.trimIndent()

        val results = XiachufangRecipeSearchSource(client = OkHttpClient())
            .parseSearchResults(html, limit = 20)

        val dish = results.first { it.name == "豉汁蒸排骨" }
        assertEquals("104672609", dish.id)
        assertTrue(dish.imageUrl.orEmpty().startsWith("https://i2.chuimg.com/cd2022e5260f43fbba37aa82ce03e610"))
    }

    @Test
    fun `parse red braised carp search result keeps xiachufang image`() {
        val html = """
            <div class="recipe recipe-215-horizontal pure-g image-link display-block">
                <a href="/recipe/100042453/" data-click-tracking-url="" data-expose-tracking-url="" target="_blank">
                    <div class="cover pure-u">
                        <img src="data:image/png;base64,AAAA" data-src="https://i2.chuimg.com/7aecf572880111e6a9a10242ac110002_4288w_2848h.jpg?imageView2/1/w/215/h/136/interlace/1/q/75" width="215" height="136" alt="红烧鲤鱼" />
                    </div>
                </a>
                <div class="info pure-u">
                    <p class="name">
                        <a href="/recipe/100042453/" data-click-tracking-url="" data-expose-tracking-url="" target="_blank">
                            红烧鲤鱼
                        </a>
                    </p>
                </div>
            </div>
        """.trimIndent()

        val results = XiachufangRecipeSearchSource(client = OkHttpClient())
            .parseSearchResults(html, limit = 20)

        val dish = results.first { it.name == "红烧鲤鱼" }
        assertEquals("100042453", dish.id)
        assertEquals("xiachufang", dish.source)
        assertTrue(dish.imageUrl.orEmpty().startsWith("https://i2.chuimg.com/7aecf572880111e6a9a10242ac110002"))
    }
}
