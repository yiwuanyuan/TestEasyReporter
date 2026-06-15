package com.aerosun.heliumleakdetector.ui.record.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.data.mapper.toTestInput
import com.aerosun.heliumleakdetector.data.mapper.toTestResult
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.domain.repository.EquipmentRepository
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val entity: DetectionRecordEntity? = null,
    val input: TestInput? = null,
    val result: TestResult? = null,
    val error: String? = null,
    val equipment: List<EquipmentEntity> = emptyList(),
)

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val equipmentRepository: EquipmentRepository,
) : ViewModel() {

    private var recordId: Long = -1
    private val _refreshTrigger = MutableStateFlow(0L)
    private val _state = MutableStateFlow(DetailUiState())

    init {
        // 监听刷新触发器
        viewModelScope.launch {
            _refreshTrigger.collect {
                if (it > 0 && recordId > 0) {
                    val entity = recordRepository.getById(recordId)
                    if (entity != null) {
                        _state.update { s -> s.copy(entity = entity) }
                        loadEquipment(entity.equipmentIds)
                    }
                }
            }
        }
    }
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun loadRecord(id: Long) {
        recordId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val entity = recordRepository.getById(id)
            if (entity == null) {
                _state.update { it.copy(isLoading = false, error = "记录不存在 (id=$id)") }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    entity = entity,
                    input = entity.toTestInput(),
                    result = entity.toTestResult(),
                )
            }
            loadEquipment(entity.equipmentIds)
        }
    }

    private suspend fun loadEquipment(eqJson: String) {
        if (eqJson.isBlank()) {
            _state.update { it.copy(equipment = emptyList()) }
            return
        }
        val ids: List<Long> = try {
            val type = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(eqJson, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
        if (ids.isEmpty()) return

        val selected = try {
            val all = equipmentRepository.getAllFlow().first()
            all.filter { it.id in ids }
        } catch (_: Exception) { emptyList() }
        _state.update { it.copy(equipment = selected) }
    }

    /** 刷新设备数据（EquipmentSelector 返回后调用） */
    fun refreshEquipment() {
        _refreshTrigger.update { System.currentTimeMillis() }
    }

    fun getSavedEquipmentIds(): Set<Long> {
        val eqJson = _state.value.entity?.equipmentIds ?: ""
        if (eqJson.isBlank()) return emptySet()
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            val list: List<Long> = Gson().fromJson(eqJson, type) ?: emptyList()
            list.toSet()
        } catch (_: Exception) { emptySet() }
    }

    /** 保存更新后的设备选择 */
    fun updateEquipment(ids: Set<Long>) {
        viewModelScope.launch {
            val entity = _state.value.entity ?: return@launch
            val json = if (ids.isEmpty()) "" else Gson().toJson(ids.toList())
            val updated = entity.copy(equipmentIds = json)
            recordRepository.update(updated)
            _state.update { it.copy(entity = updated) }
            loadEquipment(json)
        }
    }
}
