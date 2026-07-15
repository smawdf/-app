package com.myorderapp.ui.couple

import androidx.lifecycle.ViewModel
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class CoupleMenuViewModel(
    private val profileRepository: ProfileRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val _pairInfo = MutableStateFlow(PairInfo())
    val pairInfo = _pairInfo.asStateFlow()

    suspend fun refreshWhileActive() {
        while (coroutineContext.isActive) {
            refresh()
            delay(ORDER_REFRESH_INTERVAL_MS)
        }
    }

    suspend fun refresh() {
        runCatching { profileRepository.loadProfile() }
        runCatching { profileRepository.touchPresence() }
        runCatching { profileRepository.getPairInfo() }
            .onSuccess {
                _pairInfo.value = it
                if (it.noticeId.isNotBlank()) profileRepository.acknowledgePairEvent(it.noticeId)
            }
        runCatching { orderRepository.refreshOrders() }
    }

    private companion object {
        const val ORDER_REFRESH_INTERVAL_MS = 15_000L
    }
}
