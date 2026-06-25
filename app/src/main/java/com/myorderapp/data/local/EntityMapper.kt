package com.myorderapp.data.local

import com.myorderapp.data.local.entity.*
import com.myorderapp.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object EntityMapper {
    private val json = Json { ignoreUnknownKeys = true }

    // Dish ↔ DishEntity
    fun Dish.toEntity() = DishEntity(
        id = id, pairId = pairId, name = name, source = source,
        externalId = externalId, externalSource = externalSource,
        category = category, imageUrl = imageUrl,
        cookStepsJson = json.encodeToString(cookSteps),
        ingredientsJson = json.encodeToString(ingredients),
        difficulty = difficulty, cookTimeMin = cookTimeMin,
        whoLikesJson = json.encodeToString(whoLikes),
        rating = rating, notes = notes, createdBy = createdBy,
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun DishEntity.toDomain() = Dish(
        id = id, pairId = pairId, name = name, source = source,
        externalId = externalId, externalSource = externalSource,
        category = category, imageUrl = imageUrl,
        cookSteps = json.decodeFromString(cookStepsJson),
        ingredients = json.decodeFromString(ingredientsJson),
        difficulty = difficulty, cookTimeMin = cookTimeMin,
        whoLikes = json.decodeFromString(whoLikesJson),
        rating = rating, notes = notes, createdBy = createdBy,
        createdAt = createdAt, updatedAt = updatedAt
    )

    // Meal ↔ MealEntity
    fun Meal.toEntity() = MealEntity(
        id = id, pairId = pairId, mealType = mealType, date = date,
        status = status, createdBy = createdBy, confirmedAt = confirmedAt,
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun MealEntity.toDomain() = Meal(
        id = id, pairId = pairId, mealType = mealType, date = date,
        status = status, createdBy = createdBy, confirmedAt = confirmedAt,
        createdAt = createdAt, updatedAt = updatedAt
    )

    // MealItem ↔ MealItemEntity
    fun MealItem.toEntity() = MealItemEntity(
        id = id, mealId = mealId, dishId = dishId, dishName = dishName,
        dishCategory = dishCategory, dishImageUrl = dishImageUrl,
        cookTimeMin = cookTimeMin, difficulty = difficulty,
        chosenBy = chosenBy, chosenByName = chosenByName,
        quantity = quantity, notes = notes
    )

    fun MealItemEntity.toDomain() = MealItem(
        id = id, mealId = mealId, dishId = dishId, dishName = dishName,
        dishCategory = dishCategory, dishImageUrl = dishImageUrl,
        cookTimeMin = cookTimeMin, difficulty = difficulty,
        chosenBy = chosenBy, chosenByName = chosenByName,
        quantity = quantity, notes = notes
    )

    // Profile ↔ ProfileEntity
    fun Profile.toEntity() = ProfileEntity(
        userId = userId, pairId = pairId, nickname = nickname,
        avatarUrl = avatarUrl,
        tastePrefsJson = json.encodeToString(tastePrefs),
        allergiesJson = json.encodeToString(allergies),
        createdAt = createdAt, updatedAt = updatedAt
    )

    fun ProfileEntity.toDomain() = Profile(
        userId = userId, pairId = pairId, nickname = nickname,
        avatarUrl = avatarUrl,
        tastePrefs = json.decodeFromString(tastePrefsJson),
        allergies = json.decodeFromString(allergiesJson),
        createdAt = createdAt, updatedAt = updatedAt
    )
}
