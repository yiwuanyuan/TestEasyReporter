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

data class EquipmentListState(
    val equipment: List<EquipmentEntity> = emptyList(),
    val filter: String = "all",  // all | active | expired | expiring
    val isLoading: Boolean = true,
)

data class EquipmentEditState(
    val name: String = "",
    val type: String = "",
    val model: String = "",
    val serialNo: String = "",
    val calibrationDueDate: Long = 0,
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EquipmentListViewModel @Inject constructor(
    private val repo: EquipmentRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow("all")

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<EquipmentListState> = _filter.flatMapLatest { filter ->
        when (filter) {
            "active" -> repo.getByStatusFlow("active")
            "expired" -> repo.getByStatusFlow("expired")
            "expiring" -> {
                flow {
                    val threshold = System.currentTimeMillis() + 7L * 24 * 3600 * 1000
                    emit(repo.getAllFlow().first().filter { it.calibrationDueDate in 1..threshold })
                }
            }
            else -> repo.getAllFlow()
        }
    }.map { list ->
        EquipmentListState(equipment = list, filter = _filter.value, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EquipmentListState())

    fun setFilter(filter: String) { _filter.value = filter }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
}

@HiltViewModel
class EquipmentEditViewModel @Inject constructor(
    private val repo: EquipmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EquipmentEditState())
    val state: StateFlow<EquipmentEditState> = _state.asStateFlow()

    private var editId: Long? = null

    fun load(id: Long?) {
        editId = id
        if (id == null) return
        viewModelScope.launch {
            repo.getById(id)?.let { e ->
                _state.update {
                    EquipmentEditState(
                        name = e.name, type = e.type, model = e.model,
                        serialNo = e.serialNo, calibrationDueDate = e.calibrationDueDate,
                        notes = e.notes,
                    )
                }
            }
        }
    }

    fun updateField(transform: (EquipmentEditState) -> EquipmentEditState) {
        _state.update { transform(it).copy(error = null) }
    }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "设备名称不能为空") }; return }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = EquipmentEntity(
                id = editId ?: 0,
                name = s.name, type = s.type, model = s.model, serialNo = s.serialNo,
                calibrationDueDate = s.calibrationDueDate,
                status = if (s.calibrationDueDate > 0 && s.calibrationDueDate < System.currentTimeMillis()) "expired" else "active",
                notes = s.notes,
            )
            if (editId != null) repo.update(entity) else repo.insert(entity)
            onSuccess()
        }
    }
}
