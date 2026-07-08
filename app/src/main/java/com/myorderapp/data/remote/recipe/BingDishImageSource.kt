package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageResult
import com.myorderapp.ui.search.ExternalDishImageSearchResult
import com.myorderapp.ui.search.ExternalDishImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

class BingDishImageSource(
    private val client: OkHttpClient
) : ExternalDishImageSource {

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        val dishName = query.trim()
        if (dishName.isBlank()) return ExternalDishImageSearchResult()

        return runCatching {
            val encodedQuery = URLEncoder.encode(
                "$dishName \u83dc\u8c31 \u56fe\u7247 \u5bb6\u5e38\u83dc",
                Charsets.UTF_8.name()
            )
            val request = Request.Builder()
                .url("https://www.bing.com/images/search?q=$encodedQuery&first=1&form=HDRSC2")
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://www.bing.com/")
                .build()

            val html = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use ""
                    response.body?.string().orEmpty()
                }
            }

            ExternalDishImageSearchResult(
                primary = parseImageCandidates(html, dishName = dishName, limit = 8).mapIndexed { index, candidate ->
                    ExternalDishImageResult(
                        id = "bing:${dishName.normalizedId()}:$index",
                        name = candidate.title.ifBlank { dishName },
                        subtitle = "\u7f51\u7edc\u56fe\u7247\u53c2\u8003",
                        description = "\u5fc5\u5e94\u56fe\u7247\u641c\u7d22",
                        imageUrl = candidate.url,
                        source = "bing"
                    )
                }
            )
        }.getOrDefault(ExternalDishImageSearchResult())
    }

    internal fun parseImageUrls(html: String, limit: Int = 8): List<String> {
        return parseImageCandidates(html = html, dishName = "", limit = limit)
            .map { it.url }
    }

    internal fun parseImageCandidates(html: String, dishName: String, limit: Int = 8): List<ImageCandidate> {
        if (html.isBlank()) return emptyList()

        val metadataCandidates = imageMetadataRegex.findAll(html)
            .mapNotNull { match ->
                val metadata = match.groupValues.getOrNull(2)
                    ?.decodeBingImageText()
                    .orEmpty()
                val url = metadata.extractJsonString("murl")
                    ?.decodeBingImageUrl()
                    ?.takeIf { it.looksUsableImageUrl() }
                    ?: return@mapNotNull null
                ImageCandidate(
                    url = url,
                    title = metadata.extractJsonString("t")
                        ?.decodeBingImageText()
                        .orEmpty()
                )
            }

        val plainUrls = plainMurlRegex.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1) }
        val escapedUrls = escapedMurlRegex.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1) }
        val htmlEncodedUrls = htmlEncodedMurlRegex.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1) }
        val mediaUrls = mediaUrlRegex.findAll(html)
            .mapNotNull { it.groupValues.getOrNull(1) }

        val urlOnlyCandidates = (plainUrls + escapedUrls + htmlEncodedUrls + mediaUrls)
            .map { it.decodeBingImageUrl() }
            .filter { it.looksUsableImageUrl() }
            .map { ImageCandidate(url = it, title = "") }

        return (metadataCandidates + urlOnlyCandidates)
            .distinctBy { it.url }
            .filter { it.looksRelevantToDish(dishName) }
            .take(limit)
            .toList()
    }

    private fun String.decodeBingImageUrl(): String {
        val decoded = decodeBingImageText()
        return if (decoded.contains("%")) {
            runCatching { URLDecoder.decode(decoded, Charsets.UTF_8.name()) }
                .getOrDefault(decoded)
        } else {
            decoded
        }
    }

    private fun String.decodeBingImageText(): String {
        return replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("\\u002f", "/")
            .trim()
    }

    private fun String.looksUsableImageUrl(): Boolean {
        val value = lowercase()
        return startsWith("https://") &&
            !value.contains(".svg") &&
            !value.contains("bing.com/th") &&
            (
                value.contains(".jpg") ||
                    value.contains(".jpeg") ||
                    value.contains(".png") ||
                    value.contains(".webp") ||
                    value.contains("image") ||
                    value.contains("photo")
                )
    }

    private fun String.normalizedId(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), "-")
    }

    private fun ImageCandidate.looksRelevantToDish(dishName: String): Boolean {
        val normalizedDishName = dishName.normalizedSearchText()
        if (normalizedDishName.isBlank()) return true

        val normalizedTitle = title.normalizedSearchText()
        val normalizedUrl = url.normalizedSearchText()
        val tokens = normalizedDishName.foodTokens()
        return normalizedTitle.contains(normalizedDishName) ||
            normalizedDishName.contains(normalizedTitle) && normalizedTitle.length >= 2 ||
            tokens.any { token -> normalizedTitle.contains(token) || normalizedUrl.contains(token) }
    }

    private fun String.foodTokens(): List<String> {
        return KnownFoodTokens
            .filter { contains(it) }
            .sortedByDescending { it.length }
            .ifEmpty { takeIf { length >= 2 }?.let { listOf(it) }.orEmpty() }
    }

    private fun String.normalizedSearchText(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }

    private fun String.extractJsonString(key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"(.*?)"""")
        return pattern.find(this)?.groupValues?.getOrNull(1)
    }

    internal data class ImageCandidate(
        val url: String,
        val title: String
    )

    private companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
        val imageMetadataRegex = Regex("""\bm=(['"])(.*?)\1""")
        val plainMurlRegex = Regex(""""murl"\s*:\s*"(.*?)"""")
        val escapedMurlRegex = Regex("""\\"murl\\"\s*:\s*\\"(.*?)\\"""")
        val htmlEncodedMurlRegex = Regex("""murl&quot;:&quot;(.*?)&quot;""")
        val mediaUrlRegex = Regex("""mediaurl=([^&"']+)""")
        val KnownFoodTokens = listOf(
            "\u756a\u8304",
            "\u897f\u7ea2\u67ff",
            "\u9e21\u86cb",
            "\u9ca4\u9c7c",
            "\u9c7c",
            "\u6392\u9aa8",
            "\u9e21\u7fc5",
            "\u725b\u8089",
            "\u7f8a\u8089",
            "\u732a\u8089",
            "\u8c46\u8150",
            "\u571f\u8c46",
            "\u8304\u5b50",
            "\u867e",
            "\u87f9"
        )
    }
}
