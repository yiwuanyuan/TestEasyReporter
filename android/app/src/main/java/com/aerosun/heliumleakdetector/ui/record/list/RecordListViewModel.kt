package com.aerosun.heliumleakdetector.ui.record.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    enum class Filter { ALL, PASS, FAIL, FAVORITES }

    data class ListUiState(
        val records: List<DetectionRecordEntity> = emptyList(),
        val filter: Filter = Filter.ALL,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val isLoading: Boolean = true,
        val selectedDate: String? = null,       // yyyy.MM.dd 格式
        val showDatePicker: Boolean = false,
        val datesWithRecords: Set<String> = emptySet(),  // 有记录的日期
    )

    private val _filter = MutableStateFlow(Filter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedDate = MutableStateFlow<String?>(null)

    private data class FilterParams(
        val filter: Filter, val query: String,
        val date: String?, val dates: Set<String>,
        val isSearchActive: Boolean,
    )

    private val _datesFlow = recordRepository.getAllDatesFlow()
        .map { dates -> dates.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ListUiState> = combine(
        _filter, _searchQuery, _selectedDate, _datesFlow, _isSearchActive,
    ) { filter, query, date, dates, isSearchActive ->
        FilterParams(filter, query, date, dates, isSearchActive)
    }.flatMapLatest { (filter, query, date, dates, isSearchActive) ->
        val recordsFlow = when {
            date != null -> recordRepository.getByDateFlow(date)
            query.isNotBlank() -> recordRepository.searchFlow(query)
            filter == Filter.FAVORITES -> recordRepository.getFavoritesFlow()
            filter == Filter.PASS -> recordRepository.getByAcceptableFlow(true)
            filter == Filter.FAIL -> recordRepository.getByAcceptableFlow(false)
            else -> recordRepository.getAllFlow()
        }
        recordsFlow.map { records ->
            ListUiState(
                records = records,
                filter = filter,
                searchQuery = query,
                isSearchActive = isSearchActive,
                isLoading = false,
                selectedDate = date,
                datesWithRecords = dates,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState(isLoading = true),
    )

    fun onFilterChanged(filter: Filter) { _filter.value = filter }
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onSearchToggled(active: Boolean) { _isSearchActive.value = active }

    fun onDateSelected(date: String?) {
        _selectedDate.value = date
    }

    fun showDatePicker(show: Boolean) {
        _selectedDate.update { it }  // 保持原值
    }

    fun onDeleteRecord(id: Long) {
        viewModelScope.launch { recordRepository.softDelete(id) }
    }

    fun onToggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            val record = recordRepository.getById(id) ?: return@launch
            recordRepository.update(record.copy(isFavorite = !current, updatedAt = System.currentTimeMillis()))
        }
    }
}
