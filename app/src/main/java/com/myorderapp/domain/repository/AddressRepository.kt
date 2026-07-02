package com.myorderapp.domain.repository

import com.myorderapp.domain.model.Address
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun observeAddresses(): Flow<List<Address>>
    suspend fun saveAddress(address: Address)
    suspend fun deleteAddress(id: String)
    suspend fun getDefaultAddress(): Address?
}
