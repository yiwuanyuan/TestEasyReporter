package com.aerosun.heliumleakdetector.ui.record.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.mapper.toEntity
import com.aerosun.heliumleakdetector.data.mapper.toTestInput
import com.aerosun.heliumleakdetector.data.mapper.toTestResult
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.aerosun.heliumleakdetector.domain.repository.EquipmentRepository
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import com.aerosun.heliumleakdetector.domain.usecase.CalculateHeliumLeakUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    private val calculateUseCase: CalculateHeliumLeakUseCase,
    private val recordRepository: RecordRepository,
    private val equipmentRepository: EquipmentRepository,
) : ViewModel() {

    data class EditUiState(
        val input: TestInput = TestInput(),
        val result: TestResult? = null,
        val showResult: Boolean = false,
        val isCalculating: Boolean = false,
        val isSaving: Boolean = false,
        val validationErrors: Map<String, String> = emptyMap(),
        val snackbarMessage: String? = null,
        val selectedEquipmentIds: Set<Long> = emptySet(),
        val selectedEquipment: List<EquipmentEntity> = emptyList(),
        val savedRecordId: Long = -1,
        // 原始字符串输入（避免 Double.toString() 的 ".0" 尾缀问题）
        val tempText: String = "20",
        val humidityText: String = "40",
        val tResponseText: String = "50",
        val tgPercentText: String = "100",
    )

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    sealed class EditEvent {
        data class NavigateBack(val recordId: Long) : EditEvent()
        data class ShowError(val message: String) : EditEvent()
        data class OpenEquipmentSelector(val currentIds: Set<Long>) : EditEvent()
    }

    private var recordId: Long? = null
    private var existingRecord: DetectionRecordEntity? = null

    fun initialize(id: Long?) {
        recordId = id
        if (id == null) return
        viewModelScope.launch {
            recordRepository.getById(id)?.let { entity ->
                existingRecord = entity
                val ids = parseEquipmentIds(entity.equipmentIds)
                _state.update {
                    it.copy(
                        input = entity.toTestInput(),
                        result = entity.toTestResult(),
                        selectedEquipmentIds = ids,
                        savedRecordId = id,
                        // 初始化原始文本字段
                        tempText = entity.temperature.toString().replace(".0", ""),
                        humidityText = entity.humidity.toString().replace(".0", ""),
                        tResponseText = entity.tResponse.toString().replace(".0", ""),
                        tgPercentText = entity.tgPercent.toString().replace(".0", ""),
                    )
                }
                loadEquipmentList(ids)
            }
        }
    }

    fun onInputChanged(updated: TestInput) {
        _state.update { it.copy(input = updated, result = null, showResult = false, validationErrors = emptyMap()) }
    }

    /** 更新原始文本字段（温度、湿度、反应时间、氦浓度），避免 Double.toString() 的 .0 尾缀问题 */
    fun onTextChanged(field: String, text: String) {
        _state.update { state ->
            val input = state.input
            val newInput = when (field) {
                "temperature" -> input.copy(temperature = text.toDoubleOrNull() ?: input.temperature)
                "humidity" -> input.copy(humidity = text.toDoubleOrNull() ?: input.humidity)
                "tResponse" -> input.copy(tResponse = text.toDoubleOrNull() ?: input.tResponse)
                "tgPercent" -> input.copy(tgPercent = text.toDoubleOrNull() ?: input.tgPercent)
                else -> input
            }
            val newText = when (field) {
                "temperature" -> text
                "humidity" -> text
                "tResponse" -> text
                "tgPercent" -> text
                else -> ""
            }
            state.copy(
                input = newInput,
                tempText = if (field == "temperature") text else state.tempText,
                humidityText = if (field == "humidity") text else state.humidityText,
                tResponseText = if (field == "tResponse") text else state.tResponseText,
                tgPercentText = if (field == "tgPercent") text else state.tgPercentText,
                result = null,
                showResult = false,
                validationErrors = emptyMap(),
            )
        }
    }

    fun onCalculate() {
        val s = _state.value
        // 从原始文本解析最新值
        val parsedInput = s.input.copy(
            temperature = s.tempText.toDoubleOrNull() ?: s.input.temperature,
            humidity = s.humidityText.toDoubleOrNull() ?: s.input.humidity,
            tResponse = s.tResponseText.toDoubleOrNull() ?: s.input.tResponse,
            tgPercent = s.tgPercentText.toDoubleOrNull() ?: s.input.tgPercent,
        )
        val errors = validateForm(parsedInput)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(input = parsedInput, validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(input = parsedInput, isCalculating = true) }
            try {
                val result = calculateUseCase(parsedInput)
                _state.update { it.copy(result = result, isCalculating = false, showResult = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isCalculating = false) }
                _events.send(EditEvent.ShowError(e.message ?: "计算失败"))
            }
        }
    }

    fun onRequestEquipmentSelection() {
        viewModelScope.launch {
            _events.send(EditEvent.OpenEquipmentSelector(_state.value.selectedEquipmentIds))
        }
    }

    fun onEquipmentSelected(ids: Set<Long>) {
        _state.update { it.copy(selectedEquipmentIds = ids) }
        loadEquipmentList(ids)
    }

    private fun loadEquipmentList(ids: Set<Long>) {
        if (ids.isEmpty()) {
            _state.update { it.copy(selectedEquipment = emptyList()) }
            return
        }
        viewModelScope.launch {
            val all = try {
                equipmentRepository.getAllFlow().first()
            } catch (_: Exception) { emptyList() }
            val selected = all.filter { it.id in ids }
            _state.update { it.copy(selectedEquipment = selected) }
        }
    }

    fun onSave() {
        val stateVal = _state.value
        val result = stateVal.result ?: return
        val eqIdsJson = Gson().toJson(stateVal.selectedEquipmentIds.toList())

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = stateVal.input.toEntity(
                result = result,
                id = recordId ?: 0,
                createdAt = existingRecord?.createdAt ?: System.currentTimeMillis(),
                equipmentIds = eqIdsJson,
            )
            val savedId = if (recordId != null) {
                recordRepository.update(entity)
                _state.update { it.copy(savedRecordId = recordId!!) }
                recordId!!
            } else {
                val newId = recordRepository.insert(entity)
                recordId = newId
                _state.update { it.copy(savedRecordId = newId) }
                newId
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

    private fun parseEquipmentIds(json: String): Set<Long> {
        if (json.isBlank()) return emptySet()
        return try {
            Gson().fromJson(json, List::class.java)?.mapNotNull {
                (it as? Double)?.toLong() ?: (it as? Long)
            }?.toSet() ?: emptySet()
        } catch (_: Exception) { emptySet() }
    }
}
