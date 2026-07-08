package com.myorderapp.ui.candy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.CandyCoinRecord
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CandyCoinsUiState(
    val profile: Profile? = null,
    val pairInfo: PairInfo = PairInfo(),
    val records: List<CandyCoinRecord> = emptyList(),
    val message: String? = null
)

class CandyCoinsViewModel(
    private val profileRepository: ProfileRepository,
    private val ledgerRepository: CandyCoinLedgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CandyCoinsUiState())
    val uiState: StateFlow<CandyCoinsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile)
            }
        }
        viewModelScope.launch {
            ledgerRepository.observeRecords().collect { records ->
                _uiState.value = _uiState.value.copy(records = records)
            }
        }
        refreshPairInfo()
    }

    fun recharge(amount: Int) {
        if (amount <= 0) return
        viewModelScope.launch {
            val success = profileRepository.addPartnerCandyCoins(amount)
            refreshPairInfo()
            _uiState.value = _uiState.value.copy(
                message = if (success) {
                    "已给吃货充值 $amount 枚糖糖币"
                } else {
                    "充值失败，请确认已登录、已绑定，并已执行糖糖币数据库脚本"
                }
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun refreshPairInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pairInfo = profileRepository.getPairInfo())
        }
    }
}
