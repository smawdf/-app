package com.myorderapp.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.repository.SINGLE_SHOP_ID
import com.myorderapp.data.repository.SingleShopRepository
import com.myorderapp.domain.model.CartItem
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.repository.CartRepository
import com.myorderapp.domain.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class OrderingUiState(
    val shopName: String = "我的小店",
    val shopCoverUrl: String = "",
    val categories: List<MenuCategory> = emptyList(),
    val selectedCategory: String = "",
    val searchQuery: String = "",
    val menuItems: List<MenuItem> = emptyList(),
    val cartState: CartState = CartState()
) {
    val visibleItems: List<MenuItem>
        get() {
            val categoryFiltered = if (selectedCategory.isBlank()) {
                menuItems
            } else {
                menuItems.filter { it.categoryId == selectedCategory }
            }
            val query = searchQuery.trim()
            return if (query.isBlank()) {
                categoryFiltered
            } else {
                categoryFiltered.filter { item ->
                    item.name.contains(query, ignoreCase = true) ||
                        item.subtitle.contains(query, ignoreCase = true) ||
                        item.description.contains(query, ignoreCase = true)
                }
            }
        }
}

class OrderingViewModel(
    private val singleShopRepository: SingleShopRepository,
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            singleShopRepository.ensureSeedMenu()
            combine(
                singleShopRepository.getShopById(SINGLE_SHOP_ID),
                menuRepository.getMenuCategories(SINGLE_SHOP_ID),
                menuRepository.getMenuItems(SINGLE_SHOP_ID),
                cartRepository.observeCart()
            ) { shop, categories, items, cart ->
                val previous = _uiState.value.selectedCategory
                val selected = when {
                    categories.any { it.id == previous } -> previous
                    else -> categories.firstOrNull()?.id.orEmpty()
                }
                OrderingUiState(
                    shopName = shop?.name ?: singleShopRepository.getShopName(),
                    shopCoverUrl = shop?.coverUrl.orEmpty(),
                    categories = categories,
                    selectedCategory = selected,
                    searchQuery = _uiState.value.searchQuery,
                    menuItems = items,
                    cartState = cart
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addToCart(item: MenuItem) {
        viewModelScope.launch {
            cartRepository.addItem(
                CartItem(
                    id = "",
                    shopId = SINGLE_SHOP_ID,
                    shopName = singleShopRepository.getShopName(),
                    shopCoverUrl = singleShopRepository.getShopImageUrl(),
                    minOrderPrice = 0.0,
                    deliveryFee = 0.0,
                    menuItemId = item.id,
                    menuItemName = item.name,
                    menuItemImageUrl = item.imageUrl,
                    unitPrice = item.price,
                    quantity = 1
                )
            )
        }
    }

    fun increase(menuItemId: String) {
        val item = _uiState.value.cartState.items.firstOrNull { it.menuItemId == menuItemId } ?: return
        viewModelScope.launch {
            cartRepository.addItem(item.copy(quantity = 1))
        }
    }

    fun decrease(menuItemId: String) {
        viewModelScope.launch {
            cartRepository.decrementItem(menuItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }
}
