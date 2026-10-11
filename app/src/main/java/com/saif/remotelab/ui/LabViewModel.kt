package com.saif.remotelab.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.saif.remotelab.LabApplication
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.LabRepository
import com.saif.remotelab.data.MonthSummary
import com.saif.remotelab.data.ParsedRow
import com.saif.remotelab.data.PlanPdfReader
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.parsePlanText
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModel(
    private val repository: LabRepository,
    private val pdfReader: PlanPdfReader
) : ViewModel() {

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

    private val _historyQuery = MutableStateFlow("")
    val historyQuery: StateFlow<String> = _historyQuery.asStateFlow()

    val history: StateFlow<List<Run>> =
        _historyQuery.flatMapLatest { repository.history(it.trim()) }.stateInVm(emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    /** Rows read from a PDF and waiting for the analyst to review them; null when no import is open. */
    private val _importRows = MutableStateFlow<List<ParsedRow>?>(null)
    val importRows: StateFlow<List<ParsedRow>?> = _importRows.asStateFlow()

    init {
        viewModelScope.launch { repository.seedInstruments() }
    }

    fun setQuery(value: String) { _query.value = value }
    fun selectMonth(month: YearMonth?) { _selectedMonth.value = month }
    fun setHistoryQuery(value: String) { _historyQuery.value = value }

    fun submitRun(draft: RunDraft) = say { repository.submitRun(draft) }
    fun startQueued(runId: Long) = say { repository.startQueued(runId) }
    fun finishRun(runId: Long) = say { repository.finishRun(runId) }
    fun removeQueued(runId: Long) = say { repository.removeQueued(runId) }
    fun setMaintenance(instrumentId: String, on: Boolean) = say { repository.setMaintenance(instrumentId, on) }
    fun addRecord(record: PlanRecord) = say { repository.addRecord(record) }
    fun deleteRecord(id: Long) = say { repository.deleteRecord(id) }

    fun readPlanPdf(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val text = pdfReader.readText(uri)
                val rows = parsePlanText(text)
                when {
                    text.isBlank() -> _events.send("No text found. Scanned (image) PDFs cannot be read.")
                    rows.isEmpty() -> _events.send("No plan lines with a product and a date were found in this PDF.")
                    else -> _importRows.value = rows
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send("Could not read the PDF: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _importing.value = false
            }
        }
    }

    fun removeImportRow(row: ParsedRow) = _importRows.update { rows -> rows?.filter { it !== row } }

    fun cancelImport() { _importRows.value = null }

    fun confirmImport() {
        val rows = _importRows.value ?: return
        _importRows.value = null
        say { repository.importRecords(rows.map { it.toRecord() }) }
    }

    private fun say(action: suspend () -> String) {
        viewModelScope.launch { _events.send(action()) }
    }

    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LabApplication
                LabViewModel(app.repository, app.pdfReader)
            }
        }
    }
}
