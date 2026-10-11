package com.saif.remotelab.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

/** Every mutation returns the Arabic message to show the analyst. */
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
        validateRun(draft, current)?.let { return it }
        val target = current.first { it.instrument.id == draft.instrumentId }
        val now = System.currentTimeMillis()
        dao.insertRun(
            Run(
                instrumentId = target.instrument.id,
                productName = draft.productName.trim(),
                concentration = draft.concentration.trim(),
                batchNo = draft.batchNo.trim(),
                condition = draft.condition,
                months = draft.months.takeIf { draft.condition.hasIntervals },
                tests = draft.tests,
                state = if (target.isFree) RunState.Running else RunState.Queued,
                createdAt = now,
                startedAt = now.takeIf { target.isFree }
            )
        )
        return if (target.isFree) "بدأ تشغيل ${draft.productName.trim()} على ${target.instrument.id}"
        else "أُضيف ${draft.productName.trim()} لقائمة انتظار ${target.instrument.id}"
    }

    suspend fun startQueued(runId: Long): String {
        val run = dao.run(runId) ?: return "التشغيل غير موجود"
        val target = board.first().firstOrNull { it.instrument.id == run.instrumentId }
        if (target == null || !target.isFree) return "الجهاز غير متاح الآن"
        dao.updateRun(run.copy(state = RunState.Running, startedAt = System.currentTimeMillis()))
        return "بدأ تشغيل ${run.productName} على ${run.instrumentId}"
    }

    suspend fun finishRun(runId: Long): String {
        val run = dao.run(runId) ?: return "التشغيل غير موجود"
        dao.updateRun(run.copy(state = RunState.Finished, finishedAt = System.currentTimeMillis()))
        return "انتهى تشغيل ${run.productName} على ${run.instrumentId}"
    }

    suspend fun removeQueued(runId: Long): String {
        dao.deleteRun(runId)
        return "حُذف من قائمة الانتظار"
    }

    suspend fun setMaintenance(instrumentId: String, on: Boolean): String {
        val target = board.first().firstOrNull { it.instrument.id == instrumentId } ?: return "الجهاز غير موجود"
        if (on && target.running != null) return "أنهِ التشغيل الحالي أولاً"
        dao.updateInstrument(target.instrument.copy(maintenance = on))
        return if (on) "$instrumentId تحت الصيانة" else "$instrumentId عاد للخدمة"
    }

    suspend fun addRecord(record: PlanRecord): String {
        dao.insertRecord(record)
        return "أُضيف ${record.productName} إلى خطة ${record.monthPlan}"
    }

    suspend fun deleteRecord(id: Long): String {
        dao.deleteRecord(id)
        return "حُذف السجل"
    }
}
