package com.myorderapp.data.local

import com.myorderapp.data.local.entity.AddressEntity
import com.myorderapp.data.local.entity.CartItemEntity
import com.myorderapp.data.local.entity.DishEntity
import com.myorderapp.data.local.entity.MealEntity
import com.myorderapp.data.local.entity.MealItemEntity
import com.myorderapp.data.local.entity.OrderEntity
import com.myorderapp.data.local.entity.OrderItemEntity
import com.myorderapp.data.local.entity.ProfileEntity
import com.myorderapp.data.local.entity.WishlistEntity
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartItem
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.model.OrderItem
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.OrderTimelineEntry
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.model.WishlistItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EntityMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun Dish.toEntity() = DishEntity(
        id = id,
        pairId = pairId,
        name = name,
        source = source,
        externalId = externalId,
        externalSource = externalSource,
        category = category,
        imageUrl = imageUrl,
        cookStepsJson = json.encodeToString(cookSteps),
        ingredientsJson = json.encodeToString(ingredients),
        difficulty = difficulty,
        cookTimeMin = cookTimeMin,
        whoLikesJson = json.encodeToString(whoLikes),
        rating = rating,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun DishEntity.toDomain() = Dish(
        id = id,
        pairId = pairId,
        name = name,
        source = source,
        externalId = externalId,
        externalSource = externalSource,
        category = category,
        imageUrl = imageUrl,
        cookSteps = json.decodeFromString(cookStepsJson),
        ingredients = json.decodeFromString(ingredientsJson),
        difficulty = difficulty,
        cookTimeMin = cookTimeMin,
        whoLikes = json.decodeFromString(whoLikesJson),
        rating = rating,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun Meal.toEntity() = MealEntity(
        id = id,
        pairId = pairId,
        mealType = mealType,
        date = date,
        status = status,
        createdBy = createdBy,
        confirmedAt = confirmedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MealEntity.toDomain() = Meal(
        id = id,
        pairId = pairId,
        mealType = mealType,
        date = date,
        status = status,
        createdBy = createdBy,
        confirmedAt = confirmedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MealItem.toEntity() = MealItemEntity(
        id = id,
        mealId = mealId,
        dishId = dishId,
        dishName = dishName,
        dishCategory = dishCategory,
        dishImageUrl = dishImageUrl,
        cookTimeMin = cookTimeMin,
        difficulty = difficulty,
        chosenBy = chosenBy,
        chosenByName = chosenByName,
        quantity = quantity,
        notes = notes
    )

    fun MealItemEntity.toDomain() = MealItem(
        id = id,
        mealId = mealId,
        dishId = dishId,
        dishName = dishName,
        dishCategory = dishCategory,
        dishImageUrl = dishImageUrl,
        cookTimeMin = cookTimeMin,
        difficulty = difficulty,
        chosenBy = chosenBy,
        chosenByName = chosenByName,
        quantity = quantity,
        notes = notes
    )

    fun Profile.toEntity() = ProfileEntity(
        userId = userId,
        pairId = pairId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        tastePrefsJson = json.encodeToString(tastePrefs),
        allergiesJson = json.encodeToString(allergies),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun ProfileEntity.toDomain() = Profile(
        userId = userId,
        pairId = pairId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        tastePrefs = json.decodeFromString(tastePrefsJson),
        allergies = json.decodeFromString(allergiesJson),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun WishlistItem.toEntity() = WishlistEntity(
        id = id,
        pairId = pairId,
        dishId = dishId,
        dishName = dishName,
        dishCategory = dishCategory,
        dishImageUrl = dishImageUrl,
        externalSource = externalSource,
        addedBy = addedBy,
        addedByName = addedByName,
        status = status,
        notes = notes,
        createdAt = createdAt
    )

    fun WishlistEntity.toDomain() = WishlistItem(
        id = id,
        pairId = pairId,
        dishId = dishId,
        dishName = dishName,
        dishCategory = dishCategory,
        dishImageUrl = dishImageUrl,
        externalSource = externalSource,
        addedBy = addedBy,
        addedByName = addedByName,
        status = status,
        notes = notes,
        createdAt = createdAt
    )

    fun CartItem.toEntity() = CartItemEntity(
        id = id,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        minOrderPrice = minOrderPrice,
        deliveryFee = deliveryFee,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        note = note,
        addedAt = addedAt
    )

    fun CartItemEntity.toDomain() = CartItem(
        id = id,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        minOrderPrice = minOrderPrice,
        deliveryFee = deliveryFee,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        note = note,
        addedAt = addedAt
    )

    fun Address.toEntity() = AddressEntity(
        id = id,
        userId = userId,
        contactName = contactName,
        contactPhone = contactPhone,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        tag = tag,
        isDefault = isDefault
    )

    fun AddressEntity.toDomain() = Address(
        id = id,
        userId = userId,
        contactName = contactName,
        contactPhone = contactPhone,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        tag = tag,
        isDefault = isDefault
    )

    fun OrderRecord.toEntity() = OrderEntity(
        id = id,
        userId = userId,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        status = status,
        addressSnapshot = addressSnapshot,
        buyerNote = buyerNote,
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        totalPrice = totalPrice,
        createdAt = createdAt
    )

    fun OrderEntity.toDomain(items: List<OrderItem>) = OrderRecord(
        id = id,
        userId = userId,
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = shopCoverUrl,
        status = status,
        addressSnapshot = addressSnapshot,
        buyerNote = buyerNote,
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        totalPrice = totalPrice,
        createdAt = createdAt,
        items = items,
        timeline = buildTimeline(status, createdAt)
    )

    fun OrderItem.toEntity() = OrderItemEntity(
        id = id,
        orderId = orderId,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal
    )

    fun OrderItemEntity.toDomain() = OrderItem(
        id = id,
        orderId = orderId,
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemImageUrl = menuItemImageUrl,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal
    )

    private fun buildTimeline(status: String, createdAt: String): List<OrderTimelineEntry> {
        val ordered = listOf(
            "submitted" to "已提交",
            "confirmed" to "饲养员已确认",
            "delivering" to "准备中",
            "completed" to "已完成"
        )
        val statusOrder = ordered.map { it.first }
        val currentIndex = statusOrder.indexOf(status).coerceAtLeast(0)
        return ordered.mapIndexed { index, pair ->
            OrderTimelineEntry(
                title = pair.second,
                timestamp = createdAt,
                isCompleted = index <= currentIndex
            )
        }
    }
}
