package com.myorderapp.data.local

import android.content.Context
import com.myorderapp.ui.search.ExternalDishImageResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate
import kotlin.math.abs

data class BimissingRecipeAssetDto(
    val recipes: List<BimissingRecipeDto> = emptyList()
)

data class BimissingRecipeDto(
    val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val ingredients: List<String> = emptyList(),
    val imageUrl: String = ""
)

class BimissingRecipeAssetSource(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val recipes: List<BimissingRecipeDto> by lazy {
        runCatching {
            val json = context.assets.open(ASSET_NAME)
                .bufferedReader()
                .use { it.readText() }
            moshi.adapter(BimissingRecipeAssetDto::class.java)
                .fromJson(json)
                ?.recipes
                .orEmpty()
                .filter { it.name.hasChineseText() }
        }.getOrDefault(emptyList())
    }

    fun search(query: String, limit: Int = 30): List<ExternalDishImageResult> {
        val normalizedQuery = query.normalizedSearchText()
        if (normalizedQuery.isBlank()) return emptyList()

        val exact = recipes.asSequence()
            .filter { it.name.normalizedSearchText() == normalizedQuery }
            .map { it.toExternalResult() }
            .take(limit)
            .toList()
        if (exact.isNotEmpty()) return exact

        val containsMatches = recipes.asSequence()
            .filter { recipe ->
                recipe.name.normalizedSearchText().contains(normalizedQuery) ||
                    recipe.subtitle.normalizedSearchText().contains(normalizedQuery) ||
                    recipe.ingredients.any { it.normalizedSearchText().contains(normalizedQuery) }
            }
            .map { it.toExternalResult() }
            .take(limit)
            .toList()
        if (containsMatches.isNotEmpty()) return containsMatches

        return recipes.asSequence()
            .mapNotNull { recipe ->
                val score = recipe.fuzzyMatchScore(normalizedQuery)
                if (score > 0) recipe to score else null
            }
            .sortedByDescending { it.second }
            .map { it.first.toExternalResult() }
            .take(limit)
            .toList()
    }

    fun dailyRecommendation(date: LocalDate = LocalDate.now()): ExternalDishImageResult? {
        val candidates = recipes
            .filter { it.name.isNotBlank() }
            .filterNot { it.imageUrl.isLegacyRecipeImageUrl() }
            .ifEmpty { recipes.filter { it.name.isNotBlank() } }
        if (candidates.isEmpty()) return null

        val index = abs(date.toEpochDay().toInt()) % candidates.size
        return candidates[index].toExternalResult()
    }

    fun fatLossRecommendation(date: LocalDate = LocalDate.now()): ExternalDishImageResult? {
        val candidates = recipes
            .filter { recipe ->
                val text = (listOf(recipe.name, recipe.subtitle) + recipe.ingredients)
                    .joinToString(" ")
                    .normalizedSearchText()
                FatLossTokens.any { token -> text.contains(token) } &&
                    FatLossAvoidTokens.none { token -> text.contains(token) }
            }
            .filterNot { it.imageUrl.isLegacyRecipeImageUrl() }
            .ifEmpty { search("\u51cf\u8102", limit = 20).map { result ->
                BimissingRecipeDto(
                    id = result.id,
                    name = result.name,
                    subtitle = result.subtitle,
                    imageUrl = result.imageUrl.orEmpty()
                )
            } }
            .ifEmpty { recipes.filter { it.name.isNotBlank() } }
        if (candidates.isEmpty()) return null

        val index = abs((date.toEpochDay() + 17).toInt()) % candidates.size
        return candidates[index].toExternalResult()
    }

    private fun BimissingRecipeDto.toExternalResult(): ExternalDishImageResult {
        return ExternalDishImageResult(
            id = id.ifBlank { name },
            name = name,
            subtitle = subtitle.ifBlank { ingredients.take(4).joinToString("、") },
            description = ingredients.joinToString("、"),
            category = ingredients.take(3).joinToString("、"),
            imageUrl = imageUrl.takeIf { it.isNotBlank() && !it.isLegacyRecipeImageUrl() },
            source = "bimissing"
        )
    }

    private fun String.normalizedSearchText(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }

    private fun String.hasChineseText(): Boolean = any { it in '\u4e00'..'\u9fff' }

    private fun String.isLegacyRecipeImageUrl(): Boolean {
        val value = lowercase()
        return value.contains("res.hoto.cn") || value.contains("hoto.cn/")
    }

    private fun BimissingRecipeDto.fuzzyMatchScore(normalizedQuery: String): Int {
        val haystack = (listOf(name, subtitle) + ingredients)
            .joinToString(" ")
            .normalizedSearchText()
        if (haystack.isBlank()) return 0

        val tokenGroups = normalizedQuery.dishMatchTokenGroups()
        if (tokenGroups.isEmpty()) return 0

        val matchedGroups = tokenGroups.count { group ->
            group.any { token -> haystack.contains(token) }
        }
        val requiredGroups = if (tokenGroups.size <= 2) tokenGroups.size else tokenGroups.size - 1
        return if (matchedGroups >= requiredGroups && matchedGroups >= 2) {
            60 + matchedGroups
        } else {
            0
        }
    }

    private fun String.dishMatchTokenGroups(): List<List<String>> {
        val normalizedValue = normalizedSearchText()
        if (normalizedValue.isBlank()) return emptyList()

        val groups = mutableListOf<List<String>>()
        CookingMethods
            .filter { normalizedValue.contains(it) }
            .forEach { groups += listOf(it) }
        DishSynonymTokenGroups
            .filter { group -> group.any { normalizedValue.contains(it) } }
            .forEach { groups += it }
        KnownFoodTokens
            .filter { token ->
                normalizedValue.contains(token) &&
                    groups.none { group -> group.any { it.contains(token) || token.contains(it) } }
            }
            .forEach { groups += listOf(it) }

        return groups.distinctBy { it.joinToString("|") }
    }

    private companion object {
        const val ASSET_NAME = "bimissing_recipes.json"
        val CookingMethods = listOf(
            "\u7ea2\u70e7",
            "\u6e05\u84b8",
            "\u7cd6\u918b",
            "\u9178\u83dc",
            "\u6c34\u716e",
            "\u9999\u714e",
            "\u6cb9\u7116",
            "\u9ec4\u7116",
            "\u51c9\u62cc",
            "\u7096",
            "\u7092"
        )
        val KnownFoodTokens = listOf(
            "\u756a\u8304",
            "\u897f\u7ea2\u67ff",
            "\u9e21\u86cb",
            "\u6392\u9aa8",
            "\u5c0f\u6392",
            "\u9ca4\u9c7c",
            "\u9c7c",
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
        val DishSynonymTokenGroups = listOf(
            listOf("\u756a\u8304", "\u897f\u7ea2\u67ff"),
            listOf("\u9e21\u86cb", "\u7092\u86cb", "\u86cb"),
            listOf("\u6392\u9aa8", "\u5c0f\u6392"),
            listOf("\u9ca4\u9c7c", "\u9c7c")
        )
        val FatLossTokens = listOf(
            "\u51cf\u8102",
            "\u4f4e\u8102",
            "\u8f7b\u98df",
            "\u5065\u8eab",
            "\u5065\u5eb7",
            "\u8425\u517b",
            "\u897f\u5170\u82b1",
            "\u9e21\u80f8",
            "\u867e",
            "\u852c\u83dc",
            "\u751f\u83dc",
            "\u6c99\u62c9"
        )
        val FatLossAvoidTokens = listOf(
            "\u6cb9\u70b8",
            "\u70b8",
            "\u7cd6",
            "\u86cb\u7cd5",
            "\u9a6c\u82ac",
            "\u5976\u6cb9"
        )
    }
}
