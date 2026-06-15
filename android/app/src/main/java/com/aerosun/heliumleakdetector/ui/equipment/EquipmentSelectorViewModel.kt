package com.aerosun.heliumleakdetector.ui.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.domain.repository.EquipmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EquipmentSelectorState(
    val equipment: List<EquipmentEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val currentTab: String = "检漏仪",
    val isLoading: Boolean = true,
)

@HiltViewModel
class EquipmentSelectorViewModel @Inject constructor(
    private val repo: EquipmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EquipmentSelectorState())
    val state: StateFlow<EquipmentSelectorState> = _state.asStateFlow()

    init {
        loadEquipment()
    }

    fun setPreselectedIds(ids: Set<Long>) {
        _state.update { it.copy(selectedIds = ids) }
    }

    private fun loadEquipment() {
        viewModelScope.launch {
            repo.getAllFlow().collect { list ->
                _state.update { it.copy(equipment = list, isLoading = false) }
            }
        }
    }

    fun selectTab(tab: String) {
        _state.update { it.copy(currentTab = tab) }
    }

    fun toggleSelection(id: Long) {
        _state.update { state ->
            val newSet = if (state.selectedIds.contains(id))
                state.selectedIds - id
            else
                state.selectedIds + id
            state.copy(selectedIds = newSet)
        }
    }

    val tabs = listOf("检漏仪", "标准漏孔", "温度计", "氦浓度计", "其他")
}
