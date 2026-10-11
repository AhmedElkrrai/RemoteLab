package com.saif.remotelab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.saif.remotelab.LabApplication
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.LabRepository
import com.saif.remotelab.data.MonthSummary
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.RunDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModel(private val repository: LabRepository) : ViewModel() {

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    val board: StateFlow<List<InstrumentState>> = repository.board.stateInVm(emptyList())

    val todayRecords: StateFlow<List<PlanRecord>> = repository.recordsOn(LocalDate.now()).stateInVm(emptyList())

    val months: StateFlow<List<MonthSummary>> = repository.months.stateInVm(emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedMonth = MutableStateFlow<YearMonth?>(null)
    val selectedMonth: StateFlow<YearMonth?> = _selectedMonth.asStateFlow()

    /** A typed query wins over a selected month. */
    val records: StateFlow<List<PlanRecord>> =
        combine(_query, _selectedMonth) { query, month -> query.trim() to month }
            .flatMapLatest { (query, month) ->
                when {
                    query.isNotEmpty() -> repository.search(query)
                    month != null -> repository.recordsOfMonth(month)
                    else -> flowOf(emptyList())
                }
            }
            .stateInVm(emptyList())

    init {
        viewModelScope.launch { repository.seedInstruments() }
    }

    fun setQuery(value: String) { _query.value = value }
    fun selectMonth(month: YearMonth?) { _selectedMonth.value = month }

    fun submitRun(draft: RunDraft) = say { repository.submitRun(draft) }
    fun startQueued(runId: Long) = say { repository.startQueued(runId) }
    fun finishRun(runId: Long) = say { repository.finishRun(runId) }
    fun removeQueued(runId: Long) = say { repository.removeQueued(runId) }
    fun setMaintenance(instrumentId: String, on: Boolean) = say { repository.setMaintenance(instrumentId, on) }
    fun addRecord(record: PlanRecord) = say { repository.addRecord(record) }
    fun deleteRecord(id: Long) = say { repository.deleteRecord(id) }

    private fun say(action: suspend () -> String) {
        viewModelScope.launch { _events.send(action()) }
    }

    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    companion object {
        val Factory = viewModelFactory {
            initializer { LabViewModel((this[APPLICATION_KEY] as LabApplication).repository) }
        }
    }
}
