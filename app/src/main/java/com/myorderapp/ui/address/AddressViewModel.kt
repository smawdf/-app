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

    fun addAddress(
        contactName: String,
        contactPhone: String,
        addressLine1: String,
        addressLine2: String,
        tag: String
    ) {
        viewModelScope.launch {
            addressRepository.saveAddress(
                Address(
                    id = UUID.randomUUID().toString(),
                    userId = sessionManager.currentUserId.ifBlank { "guest" },
                    contactName = contactName.trim(),
                    contactPhone = contactPhone.trim(),
                    addressLine1 = addressLine1.trim(),
                    addressLine2 = addressLine2.trim(),
                    tag = tag.trim(),
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
