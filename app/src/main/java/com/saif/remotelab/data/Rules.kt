package com.saif.remotelab.data

import java.time.LocalDate

/** Returns an Arabic error message, or null when the draft can be started or queued. */
fun validateRun(draft: RunDraft, board: List<InstrumentState>): String? {
    if (draft.productName.isBlank()) return "اكتب اسم المستحضر"
    val target = board.firstOrNull { it.instrument.id == draft.instrumentId } ?: return "اختر الجهاز"
    if (draft.tests.isEmpty()) return "اختر نوع التحليل"
    val kind = target.instrument.kind
    draft.tests.firstOrNull { it.kind != kind }?.let {
        return "${it.label} لا يُجرى على جهاز ${kind.label}"
    }
    if (draft.condition.hasIntervals && draft.months == null) return "اختر الفترة الزمنية"

    val duplicate = board.flatMap { listOfNotNull(it.running) + it.queue }.any { run ->
        run.productName.trim().equals(draft.productName.trim(), ignoreCase = true) &&
            run.batchNo.trim().equals(draft.batchNo.trim(), ignoreCase = true) &&
            run.condition == draft.condition &&
            run.months == draft.months &&
            run.tests.any { it in draft.tests }
    }
    return if (duplicate) "هذا التحليل قيد التشغيل أو في قائمة الانتظار بالفعل" else null
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
