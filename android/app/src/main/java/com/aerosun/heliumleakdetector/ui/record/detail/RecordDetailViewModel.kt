package com.aerosun.heliumleakdetector.ui.record.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.mapper.toTestInput
import com.aerosun.heliumleakdetector.data.mapper.toTestResult
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val entity: DetectionRecordEntity? = null,
    val input: TestInput? = null,
    val result: TestResult? = null,
    val error: String? = null,
)

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun loadRecord(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val entity = recordRepository.getById(id)
            if (entity == null) {
                _state.update {
                    it.copy(isLoading = false, error = "记录不存在 (id=$id)")
                }
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
        }
    }
}
