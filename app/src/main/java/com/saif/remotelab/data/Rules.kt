package com.saif.remotelab.data

import java.time.LocalDate

/** The parts of the run form a problem can belong to. [label] is how the summary names a missing one. */
enum class RunField(val label: String) {
    Product("Product"),
    Concentration("Concentration"),
    Batch("Batch No"),
    Interval("Time interval"),
    Instrument("Instrument"),
    Tests("Test type"),
    Duplicate("Duplicate")
}

/** A run covers one interval, or 3M and 6M together. No other pair is allowed. */
fun isValidIntervalSelection(months: Set<Int>): Boolean = months.size == 1 || months == setOf(3, 6)

/** Tapping an interval that cannot be combined with the current selection replaces it. */
fun toggleInterval(selected: Set<Int>, month: Int): Set<Int> = when {
    month in selected -> selected - month
    isValidIntervalSelection(selected + month) -> selected + month
    else -> setOf(month)
}

/** Every problem with the draft, keyed by field in form order. Empty when it can be started or queued. */
fun validateRun(draft: RunDraft, board: List<InstrumentState>): Map<RunField, String> {
    val problems = linkedMapOf<RunField, String>()
    if (draft.productName.isBlank()) problems[RunField.Product] = "Enter the product name"
    if (draft.concentration.isBlank()) problems[RunField.Concentration] = "Enter the concentration"
    if (draft.batchNo.isBlank()) problems[RunField.Batch] = "Enter the batch number"

    if (draft.condition.hasIntervals) {
        when {
            draft.months.isEmpty() -> problems[RunField.Interval] = "Select a time interval"
            !isValidIntervalSelection(draft.months) -> problems[RunField.Interval] = "Only 3M and 6M can be combined"
            !draft.condition.months.containsAll(draft.months) ->
                problems[RunField.Interval] = "Not a ${draft.condition.label} interval"
        }
    }

    val target = board.firstOrNull { it.instrument.id == draft.instrumentId }
    if (target == null) problems[RunField.Instrument] = "Select an instrument"

    val wrongTest = target?.let { t -> draft.tests.firstOrNull { it.kind != t.instrument.kind } }
    when {
        draft.tests.isEmpty() -> problems[RunField.Tests] = "Select at least one test type"
        wrongTest != null -> problems[RunField.Tests] =
            "${wrongTest.label} cannot run on ${target.instrument.kind.label}"
    }

    if (problems.isEmpty()) {
        val duplicate = board.flatMap { listOfNotNull(it.running) + it.queue }.any { run ->
            run.productName.trim().equals(draft.productName.trim(), ignoreCase = true) &&
                run.batchNo.trim().equals(draft.batchNo.trim(), ignoreCase = true) &&
                run.condition == draft.condition &&
                (!draft.condition.hasIntervals || run.months.any { it in draft.months }) &&
                run.tests.any { it in draft.tests }
        }
        if (duplicate) problems[RunField.Duplicate] = "This analysis is already running or queued"
    }
    return problems
}

data class NextInterval(val months: Int, val expected: LocalDate)

/**
 * The interval that follows the latest one in [records] (all of one batch and one condition),
 * with a date estimated from that latest record.
 */
fun nextInterval(condition: StudyCondition, records: List<PlanRecord>): NextInterval? {
    val last = records.filter { it.months != null }.maxByOrNull { it.months!! } ?: return null
    val lastMonths = last.months!!
    val next = condition.months.firstOrNull { it > lastMonths } ?: return null
    return NextInterval(next, last.dateOfAnalysis.plusMonths((next - lastMonths).toLong()))
}
