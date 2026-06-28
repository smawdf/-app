package com.myorderapp.ui.search

import com.myorderapp.data.remote.recipe.JisuRecipeApi
import com.myorderapp.data.remote.recipe.JisuRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JisuRecipeResponse
import com.myorderapp.data.remote.recipe.JuheRecipeApi
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheRecipeResponse
import com.myorderapp.data.remote.recipe.TianRecipeApi
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianRecipeResponse
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.usecase.DualRecipeSearchUseCase
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchViewModelTest {

    @Test
    fun `query changes are debounced and only latest query searches`() = runBlocking {
        val repository = RecordingDishRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val viewModel = SearchViewModel(
            dishRepository = repository,
            dualSearch = noNetworkSearchUseCase(repository),
            searchDebounceMs = 100L,
            searchScope = scope
        )

        try {
            viewModel.onQueryChanged("ma")
            delay(50L)
            viewModel.onQueryChanged("mapo")
            delay(75L)

            assertTrue(repository.searchQueries.isEmpty())

            delay(150L)

            assertEquals(listOf("mapo"), repository.searchQueries.toList())
        } finally {
            scope.cancel()
        }
    }

    private class RecordingDishRepository : DishRepository {
        val searchQueries: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override fun getAllDishes(): Flow<List<Dish>> = flowOf(emptyList())

        override fun getDishesByCategory(category: String): Flow<List<Dish>> = flowOf(emptyList())

        override fun searchDishes(query: String): Flow<List<Dish>> {
            searchQueries.add(query)
            return flowOf(emptyList())
        }

        override suspend fun getDishById(id: String): Dish? = null

        override suspend fun cacheSearchResult(dish: Dish) = Unit

        override suspend fun addDish(dish: Dish): String = dish.id

        override suspend fun updateDish(dish: Dish) = Unit

        override suspend fun deleteDish(id: String) = Unit

        override fun getRecentDishes(limit: Int): Flow<List<Dish>> = flowOf(emptyList())
    }

    private fun noNetworkSearchUseCase(repository: DishRepository): DualRecipeSearchUseCase =
        DualRecipeSearchUseCase(
            juheDataSource = JuheRecipeRemoteDataSource(
                api = object : JuheRecipeApi {
                    override suspend fun searchRecipes(
                        apiKey: String,
                        word: String,
                        num: Int,
                        page: Int
                    ): JuheRecipeResponse = error("Network API should not be called")
                },
                apiKey = ""
            ),
            tianDataSource = TianRecipeRemoteDataSource(
                api = object : TianRecipeApi {
                    override suspend fun searchRecipes(
                        apiKey: String,
                        word: String,
                        num: Int,
                        page: Int
                    ): TianRecipeResponse = error("Network API should not be called")
                },
                apiKey = ""
            ),
            jisuDataSource = JisuRecipeRemoteDataSource(
                api = object : JisuRecipeApi {
                    override suspend fun searchRecipes(
                        keyword: String,
                        num: Int,
                        start: Int,
                        appkey: String
                    ): JisuRecipeResponse = error("Network API should not be called")
                },
                apiKey = ""
            ),
            dishRepository = repository
        )
}
