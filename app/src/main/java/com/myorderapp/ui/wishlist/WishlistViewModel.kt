package com.myorderapp.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.WishlistItem
import com.myorderapp.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WishlistUiState(
    val items: List<WishlistItem> = emptyList(),
    val selectedTab: String = "待尝试",
    val isLoading: Boolean = true
)

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun onTabSelected(tab: String) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val statusFilter = when (_uiState.value.selectedTab) {
                "待尝试" -> "pending"
                "已尝试" -> "tried"
                "已放弃" -> "rejected"
                else -> null
            }
            wishlistRepository.getWishlistItems(statusFilter).collect { items ->
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            }
        }
    }

    fun markTried(id: String) {
        viewModelScope.launch {
            wishlistRepository.updateStatus(id, "tried")
        }
    }

    fun markRejected(id: String) {
        viewModelScope.launch {
            wishlistRepository.updateStatus(id, "rejected")
        }
    }

    fun removeItem(id: String) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(id)
        }
    }
}
