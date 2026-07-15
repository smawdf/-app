package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.AddressDao
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAddressRepository(
    private val addressDao: AddressDao,
    private val session: SessionManager
) : AddressRepository {

    override fun observeAddresses(): Flow<List<Address>> {
        return addressDao.observeAddresses(activeUserId()).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveAddress(address: Address) {
        if (address.isDefault) {
            addressDao.clearDefaultFlag(activeUserId())
        }
        addressDao.upsert(address.toEntity())
    }

    override suspend fun deleteAddress(id: String) {
        addressDao.deleteById(id, activeUserId())
    }

    override suspend fun getDefaultAddress(): Address? {
        return addressDao.getDefaultAddress(activeUserId())?.toDomain()
    }

    private fun activeUserId(): String = session.currentUserId.ifBlank { "guest" }
}
