package com.myorderapp.ui.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.repository.AddressRepository
import com.myorderapp.data.remote.supabase.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AddressUiState(
    val addresses: List<Address> = emptyList()
)

class AddressViewModel(
    private val addressRepository: AddressRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            addressRepository.observeAddresses().collect { addresses ->
                _uiState.value = AddressUiState(addresses = addresses)
            }
        }
    }

    fun addSampleAddress() {
        viewModelScope.launch {
            addressRepository.saveAddress(
                Address(
                    id = UUID.randomUUID().toString(),
                    userId = sessionManager.currentUserId.ifBlank { "guest" },
                    contactName = "阿达",
                    contactPhone = "13800138000",
                    addressLine1 = "橙子街 12 号",
                    addressLine2 = "502 室",
                    tag = "家",
                    isDefault = _uiState.value.addresses.isEmpty()
                )
            )
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            addressRepository.deleteAddress(id)
        }
    }
}
