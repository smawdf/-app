package com.myorderapp.data.repository

import com.myorderapp.domain.model.Dish
import org.junit.Assert.assertEquals
import org.junit.Test

class DishMergePolicyTest {
    @Test
    fun `logged out merge excludes cloud dishes`() {
        val local = listOf(dish("local-1", "番茄炒蛋", "builtin"))
        val cloud = listOf(dish("cloud-1", "云端红烧肉", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = false)

        assertEquals(listOf("local-1"), merged.map { it.id })
    }

    @Test
    fun `logged in merge keeps cloud first and local fallback`() {
        val local = listOf(dish("local-1", "番茄炒蛋", "builtin"))
        val cloud = listOf(dish("cloud-1", "云端红烧肉", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1", "local-1"), merged.map { it.id })
    }

    @Test
    fun `logged in merge deduplicates by normalized name with cloud winning`() {
        val local = listOf(dish("local-1", " 番茄炒蛋 ", "builtin"))
        val cloud = listOf(dish("cloud-1", "番茄炒蛋", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1"), merged.map { it.id })
    }

    @Test
    fun `blank dish names fall back to id for duplicate key`() {
        val local = listOf(dish("local-1", "", "builtin"))
        val cloud = listOf(dish("cloud-1", "", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1", "local-1"), merged.map { it.id })
    }

    private fun dish(id: String, name: String, source: String): Dish =
        Dish(id = id, name = name, source = source)
}
