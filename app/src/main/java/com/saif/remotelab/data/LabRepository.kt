package com.saif.remotelab.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

/** Every mutation returns the message to show the analyst. */
class LabRepository(private val dao: LabDao) {

    val board: Flow<List<InstrumentState>> =
        combine(dao.instruments(), dao.activeRuns()) { instruments, runs ->
            instruments.map { instrument ->
                val mine = runs.filter { it.instrumentId == instrument.id }
                InstrumentState(
                    instrument = instrument,
                    running = mine.firstOrNull { it.state == RunState.Running },
                    queue = mine.filter { it.state == RunState.Queued }
                )
            }
        }

    val months: Flow<List<MonthSummary>> = dao.months()

    fun search(query: String): Flow<List<PlanRecord>> = dao.search(query)
    fun recordsOfMonth(month: YearMonth): Flow<List<PlanRecord>> = dao.recordsOfMonth(month)
    fun recordsOn(date: LocalDate): Flow<List<PlanRecord>> = dao.recordsOn(date)

    suspend fun seedInstruments() = dao.insertInstruments(DefaultInstruments)

    suspend fun submitRun(draft: RunDraft): String {
        val current = board.first()
        validateRun(draft, current).values.firstOrNull()?.let { return it }
        val target = current.first { it.instrument.id == draft.instrumentId }
        val now = System.currentTimeMillis()
        dao.insertRun(
            Run(
                instrumentId = target.instrument.id,
                productName = draft.productName.trim(),
                concentration = draft.concentration.trim(),
                batchNo = draft.batchNo.trim(),
                condition = draft.condition,
                months = if (draft.condition.hasIntervals) draft.months else emptySet(),
                tests = draft.tests,
                state = if (target.isFree) RunState.Running else RunState.Queued,
                createdAt = now,
                startedAt = now.takeIf { target.isFree }
            )
        )
        return if (target.isFree) "${draft.productName.trim()} started on ${target.instrument.id}"
        else "${draft.productName.trim()} added to the ${target.instrument.id} queue"
    }

    suspend fun startQueued(runId: Long): String {
        val run = dao.run(runId) ?: return "Run not found"
        val target = board.first().firstOrNull { it.instrument.id == run.instrumentId }
        if (target == null || !target.isFree) return "${run.instrumentId} is not available right now"
        dao.updateRun(run.copy(state = RunState.Running, startedAt = System.currentTimeMillis()))
        return "${run.productName} started on ${run.instrumentId}"
    }

    suspend fun finishRun(runId: Long): String {
        val run = dao.run(runId) ?: return "Run not found"
        dao.updateRun(run.copy(state = RunState.Finished, finishedAt = System.currentTimeMillis()))
        return "${run.productName} finished on ${run.instrumentId}"
    }

    suspend fun removeQueued(runId: Long): String {
        dao.deleteRun(runId)
        return "Removed from the queue"
    }

    suspend fun setMaintenance(instrumentId: String, on: Boolean): String {
        val target = board.first().firstOrNull { it.instrument.id == instrumentId } ?: return "Instrument not found"
        if (on && target.running != null) return "Finish the current run first"
        dao.updateInstrument(target.instrument.copy(maintenance = on))
        return if (on) "$instrumentId is under maintenance" else "$instrumentId is back in service"
    }

    fun history(query: String): Flow<List<Run>> = dao.finishedRuns(query)

    suspend fun addRecord(record: PlanRecord): String {
        dao.insertRecord(record)
        return "${record.productName} added to the ${record.monthPlan} plan"
    }

    /** Rows already in the archive (same product, batch, date, interval and test) are skipped. */
    suspend fun importRecords(records: List<PlanRecord>): String {
        var added = 0
        for (record in records) {
            val exists = dao.recordsOnOnce(record.dateOfAnalysis).any { it.copy(id = 0) == record.copy(id = 0) }
            if (!exists) {
                dao.insertRecord(record)
                added++
            }
        }
        val skipped = records.size - added
        return if (skipped == 0) "Imported $added records" else "Imported $added records, skipped $skipped already in the archive"
    }

    suspend fun deleteRecord(id: Long): String {
        dao.deleteRecord(id)
        return "Record deleted"
    }
}
