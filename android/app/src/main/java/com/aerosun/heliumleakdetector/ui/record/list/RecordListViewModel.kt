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

/**
 * 记录列表 ViewModel。
 *
 * 响应式数据流：Room DAO Flow → stateIn → Compose collectAsStateWithLifecycle()
 */
@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    // ====== 筛选状态 ======
    enum class Filter { ALL, PASS, FAIL, FAVORITES }

    data class ListUiState(
        val records: List<DetectionRecordEntity> = emptyList(),
        val filter: Filter = Filter.ALL,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val isLoading: Boolean = true,
    )

    private val _filter = MutableStateFlow(Filter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ListUiState> = combine(
        _filter,
        _searchQuery,
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        when {
            query.isNotBlank() -> recordRepository.searchFlow(query)
            filter == Filter.FAVORITES -> recordRepository.getFavoritesFlow()
            filter == Filter.PASS -> recordRepository.getByAcceptableFlow(true)
            filter == Filter.FAIL -> recordRepository.getByAcceptableFlow(false)
            else -> recordRepository.getAllFlow()
        }
    }.map { records ->
        ListUiState(
            records = records,
            filter = _filter.value,
            searchQuery = _searchQuery.value,
            isSearchActive = _isSearchActive.value,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState(isLoading = true),
    )

    // ====== 用户操作 ======

    fun onFilterChanged(filter: Filter) { _filter.value = filter }
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onSearchToggled(active: Boolean) { _isSearchActive.value = active }
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
