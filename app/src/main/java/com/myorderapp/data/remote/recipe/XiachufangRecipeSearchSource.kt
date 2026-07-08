package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class XiachufangRecipeSearchSource(
    private val client: OkHttpClient
) {
    suspend fun search(query: String, limit: Int = 8): List<ExternalDishImageResult> {
        val keyword = query.trim()
        if (keyword.isBlank()) return emptyList()

        val encodedKeyword = URLEncoder.encode(keyword, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("https://www.xiachufang.com/search/?keyword=$encodedKeyword")
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", "https://www.xiachufang.com/")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .build()
        val html = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Xiachufang HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }
        }
        return parseSearchResults(html, limit)
    }

    internal fun parseSearchResults(html: String, limit: Int = 8): List<ExternalDishImageResult> {
        return recipeIdRegex.findAll(html)
            .mapNotNull { match ->
                val recipeId = match.groupValues.getOrNull(1).orEmpty()
                val block = html.surroundingRecipeBlock(match.range.first)
                parseRecipeBlock(recipeId, block)
            }
            .distinctBy { it.id }
            .take(limit)
            .toList()
    }

    private fun parseRecipeBlock(recipeId: String, block: String): ExternalDishImageResult? {
        if (recipeId.isBlank()) return null

        val title = altTitleRegex.find(block)?.groupValues?.getOrNull(1)
            ?: titleLinkRegex(recipeId).find(block)?.groupValues?.getOrNull(1)
            ?: return null
        val imageUrl = imageAttributeRegex.findAll(block)
            .mapNotNull { it.groupValues.getOrNull(1) ?: it.groupValues.getOrNull(2) }
            .map { it.htmlDecode().normalizedImageUrl() }
            .firstOrNull { it.contains("chuimg.com") || it.startsWith("https://") }
            ?: return null

        return ExternalDishImageResult(
            id = recipeId,
            name = title.htmlDecode().stripHtmlTags().trim(),
            subtitle = "下厨房图文菜谱",
            description = "https://www.xiachufang.com/recipe/$recipeId/",
            imageUrl = imageUrl,
            source = "xiachufang"
        )
    }

    private fun String.surroundingRecipeBlock(anchorIndex: Int): String {
        val classIndex = lastIndexOf("class=\"recipe", anchorIndex)
        val blockStart = if (classIndex >= 0) lastIndexOf("<div", classIndex).coerceAtLeast(0) else -1
        val nextClassIndex = indexOf("class=\"recipe", anchorIndex + 1)
        val nextBlockStart = if (nextClassIndex >= 0) lastIndexOf("<div", nextClassIndex).takeIf { it > anchorIndex } else null
        val start = if (blockStart >= 0) blockStart else (anchorIndex - 1400).coerceAtLeast(0)
        val end = nextBlockStart ?: (anchorIndex + 2600).coerceAtMost(length)
        return substring(start, end)
    }

    private fun String.htmlDecode(): String {
        return replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun String.stripHtmlTags(): String {
        return replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), " ")
    }

    private fun String.normalizedImageUrl(): String {
        val value = trim()
            .substringBefore(",")
            .substringBefore(" ")
        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("http://") -> value.replaceFirst("http://", "https://")
            else -> value
        }
    }

    private companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
        val recipeIdRegex = Regex("""href="/recipe/(\d+)/""")
        val altTitleRegex = Regex("""alt="([^"]+)""")
        val imageAttributeRegex = Regex("""(?:data-src|data-original|src|srcset)=["']([^"']+)["']""")
        fun titleLinkRegex(recipeId: String) =
            Regex("""<a[^>]+href="/recipe/${Regex.escape(recipeId)}/"[^>]*>\s*([^<][\s\S]*?)\s*</a>""")
    }
}

