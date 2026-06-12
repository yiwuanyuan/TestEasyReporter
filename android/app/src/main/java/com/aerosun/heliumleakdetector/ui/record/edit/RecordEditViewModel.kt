package com.aerosun.heliumleakdetector.ui.record.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.mapper.toEntity
import com.aerosun.heliumleakdetector.data.mapper.toTestInput
import com.aerosun.heliumleakdetector.data.mapper.toTestResult
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import com.aerosun.heliumleakdetector.domain.usecase.CalculateHeliumLeakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    private val calculateUseCase: CalculateHeliumLeakUseCase,
    private val recordRepository: RecordRepository,
) : ViewModel() {

    data class EditUiState(
        val input: TestInput = TestInput(),
        val result: TestResult? = null,
        val showResult: Boolean = false,
        val isCalculating: Boolean = false,
        val isSaving: Boolean = false,
        val validationErrors: Map<String, String> = emptyMap(),
        val snackbarMessage: String? = null,
    )

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    sealed class EditEvent {
        data class NavigateBack(val recordId: Long) : EditEvent()
        data class ShowError(val message: String) : EditEvent()
    }

    private var recordId: Long? = null
    private var existingRecord: DetectionRecordEntity? = null

    /** 初始化：如果是编辑模式，从数据库加载已有记录 */
    fun initialize(id: Long?) {
        recordId = id
        if (id == null) return
        viewModelScope.launch {
            recordRepository.getById(id)?.let { entity ->
                existingRecord = entity
                _state.update { it.copy(input = entity.toTestInput(), result = entity.toTestResult()) }
            }
        }
    }

    /** 更新某个输入字段 */
    fun onInputChanged(updated: TestInput) {
        _state.update { it.copy(input = updated, result = null, showResult = false, validationErrors = emptyMap()) }
    }

    /** 表单校验 → 调用计算引擎 */
    fun onCalculate() {
        val input = _state.value.input
        val errors = validateForm(input)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCalculating = true) }
            try {
                val result = calculateUseCase(input)
                _state.update { it.copy(result = result, isCalculating = false, showResult = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isCalculating = false) }
                _events.send(EditEvent.ShowError(e.message ?: "计算失败"))
            }
        }
    }

    /** 保存记录到数据库 */
    fun onSave() {
        val stateVal = _state.value
        val result = stateVal.result ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = stateVal.input.toEntity(
                result = result,
                id = recordId ?: 0,
                createdAt = existingRecord?.createdAt ?: System.currentTimeMillis(),
            )
            val savedId = if (recordId != null) {
                recordRepository.update(entity)
                recordId!!
            } else {
                recordRepository.insert(entity)
            }
            _state.update { it.copy(isSaving = false) }
            _events.send(EditEvent.NavigateBack(savedId))
        }
    }

    fun onDismissResult() {
        _state.update { it.copy(showResult = false) }
    }

    private fun validateForm(input: TestInput): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (input.reportNo.isBlank()) e["reportNo"] = "报告编号不能为空"
        if (input.q0Mantissa <= 0) e["q0Mantissa"] = "尾数必须 > 0"
        if (input.i0Mantissa <= 0) e["i0Mantissa"] = "尾数必须 > 0"
        if (input.iMantissa <= 0) e["iMantissa"] = "尾数必须 > 0"
        if (input.m0Mantissa <= 0) e["m0Mantissa"] = "尾数必须 > 0"
        if (input.m1Mantissa <= 0) e["m1Mantissa"] = "尾数必须 > 0"
        if (input.m2Mantissa <= 0) e["m2Mantissa"] = "尾数必须 > 0"
        if (input.temperature !in 0.0..50.0) e["temperature"] = "温度应在 0~50°C"
        if (input.tgPercent !in 0.1..100.0) e["tgPercent"] = "浓度应在 0.1~100%"
        return e
    }
}
