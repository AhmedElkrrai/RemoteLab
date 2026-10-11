package com.saif.remotelab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth

enum class InstrumentKind(val label: String) {
    HPLC("HPLC"),
    GC("GC"),
    AAS("Atomic Absorption")
}

enum class TestType(val label: String, val kind: InstrumentKind) {
    Assay("Assay", InstrumentKind.HPLC),
    Related("Related Substances", InstrumentKind.HPLC),
    ResidualSolvents("Residual Solvents", InstrumentKind.GC),
    HeavyMetals("Heavy Metals", InstrumentKind.AAS)
}

/** [months] is the standard pull schedule; empty for studies that have no time intervals. */
enum class StudyCondition(val label: String, val short: String, val months: List<Int>) {
    Accelerated("Accelerated", "Acc", listOf(0, 3, 6)),
    LongTerm("Long Term", "Long", listOf(0, 3, 6, 12, 18, 24, 36)),
    Validation("Validation", "Validation", emptyList()),
    Verification("Verification", "Verification", emptyList());

    val hasIntervals: Boolean get() = months.isNotEmpty()
}

fun intervalLabel(condition: StudyCondition, months: Int?): String =
    if (condition.hasIntervals && months != null) "${months}M ${condition.short}" else condition.label

enum class RunState { Running, Queued, Finished }

enum class InstrumentStatus { Running, Idle, Maintenance }

@Entity(tableName = "instruments")
data class Instrument(
    @PrimaryKey val id: String,
    val kind: InstrumentKind,
    val sortOrder: Int,
    val maintenance: Boolean = false,
    val note: String = ""
)

/** One table covers the current run, the queue and the history of an instrument. */
@Entity(tableName = "runs", indices = [Index("instrumentId")])
data class Run(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instrumentId: String,
    val productName: String,
    val concentration: String,
    val batchNo: String,
    val condition: StudyCondition,
    val months: Int?,
    val tests: Set<TestType>,
    val state: RunState,
    val createdAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null
) {
    val interval: String get() = intervalLabel(condition, months)
    val displayName: String get() = listOf(productName, concentration).filter { it.isNotBlank() }.joinToString(" ")
}

/** One line of a monthly stability plan. The archive is the collection of all of them. */
@Entity(tableName = "plan_records", indices = [Index("productName"), Index("monthPlan")])
data class PlanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val concentration: String,
    val batchNo: String,
    val dateOfAnalysis: LocalDate,
    val condition: StudyCondition,
    val months: Int?,
    val test: TestType,
    val monthPlan: YearMonth
) {
    val interval: String get() = intervalLabel(condition, months)
    val displayName: String get() = listOf(productName, concentration).filter { it.isNotBlank() }.joinToString(" ")
}

data class MonthSummary(val monthPlan: YearMonth, val count: Int)

data class InstrumentState(
    val instrument: Instrument,
    val running: Run?,
    val queue: List<Run>
) {
    val status: InstrumentStatus
        get() = when {
            running != null -> InstrumentStatus.Running
            instrument.maintenance -> InstrumentStatus.Maintenance
            else -> InstrumentStatus.Idle
        }
    val isFree: Boolean get() = status == InstrumentStatus.Idle
}

/** What the run form is editing before it becomes a [Run]. */
data class RunDraft(
    val instrumentId: String? = null,
    val productName: String = "",
    val concentration: String = "",
    val batchNo: String = "",
    val condition: StudyCondition = StudyCondition.Accelerated,
    val months: Int? = null,
    val tests: Set<TestType> = emptySet()
)

fun PlanRecord.toDraft() = RunDraft(
    productName = productName,
    concentration = concentration,
    batchNo = batchNo,
    condition = condition,
    months = months,
    tests = setOf(test)
)

val DefaultInstruments: List<Instrument> = listOf(
    Instrument("Dwa36", InstrumentKind.HPLC, 0),
    Instrument("Dwa37", InstrumentKind.HPLC, 1),
    Instrument("Dwa47", InstrumentKind.HPLC, 2),
    Instrument("Dwa51", InstrumentKind.HPLC, 3),
    Instrument("Dwa52", InstrumentKind.HPLC, 4),
    Instrument("Dwa02", InstrumentKind.HPLC, 5),
    Instrument("GC", InstrumentKind.GC, 6),
    Instrument("Atomic", InstrumentKind.AAS, 7)
)
