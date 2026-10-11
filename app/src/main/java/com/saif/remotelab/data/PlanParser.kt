package com.saif.remotelab.data

import java.time.LocalDate
import java.time.YearMonth

/** One plan line recognised in imported text. [source] is the original line, kept for review. */
data class ParsedRow(
    val productName: String,
    val concentration: String,
    val batchNo: String,
    val date: LocalDate,
    val condition: StudyCondition?,
    val months: Int?,
    val test: TestType?,
    val source: String
) {
    fun toRecord() = PlanRecord(
        productName = productName,
        concentration = concentration,
        batchNo = batchNo,
        dateOfAnalysis = date,
        condition = condition,
        months = months.takeIf { condition?.hasIntervals == true },
        test = test,
        monthPlan = YearMonth.from(date)
    )
}

private val IsoDate = Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""")
private val DayFirstDate = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{4}|\d{2})\b""")
private val NamedMonthDate = Regex("""\b(\d{1,2})[\s-]+([A-Za-z]{3,9})\.?[\s,-]+(\d{4})\b""")
private val MonthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

private val IntervalPattern = Regex("""\b(\d{1,2})\s*M(?:onths?)?\b""", RegexOption.IGNORE_CASE)
private val BatchLabel = Regex(
    """\b(?:batch|lot|b\.\s?n\.?)\s*(?:no\.?|number|#)?\s*[:\-]?\s*([A-Za-z0-9][A-Za-z0-9/\-]*\d[A-Za-z0-9/\-]*)""",
    RegexOption.IGNORE_CASE
)
private val ConditionPatterns = listOf(
    Regex("""\b(?:accelerated|acc)\b\.?""", RegexOption.IGNORE_CASE) to StudyCondition.Accelerated,
    Regex("""\blong(?:[\s-]?term)?\b""", RegexOption.IGNORE_CASE) to StudyCondition.LongTerm,
    Regex("""\bvalidation\b""", RegexOption.IGNORE_CASE) to StudyCondition.Validation,
    Regex("""\bverification\b""", RegexOption.IGNORE_CASE) to StudyCondition.Verification
)
private val TestPatterns = listOf(
    Regex("""\bassay\b""", RegexOption.IGNORE_CASE) to TestType.Assay,
    Regex("""\b(?:related(?:\s+substances?)?|impurit(?:y|ies))\b""", RegexOption.IGNORE_CASE) to TestType.Related,
    Regex("""\bresidual(?:\s+solvents?)?\b""", RegexOption.IGNORE_CASE) to TestType.ResidualSolvents,
    Regex("""\bheavy(?:\s+metals?)?\b""", RegexOption.IGNORE_CASE) to TestType.HeavyMetals
)

/** Every line that carries a date and a product name becomes a row; all other lines are ignored. */
fun parsePlanText(text: String): List<ParsedRow> = text.lines().mapNotNull(::parsePlanLine)

/**
 * Best-effort reading of one table line. Dates are read day-first. The product name is the
 * leading run of words, what follows it is the concentration, and the batch number is whatever
 * follows a "Batch"/"Lot" label or, failing that, the last token that contains a digit.
 */
fun parsePlanLine(raw: String): ParsedRow? {
    var rest = raw.trim()
    if (rest.isEmpty()) return null

    val (dateText, date) = findDate(rest) ?: return null
    rest = rest.replace(dateText, " ")

    var months: Int? = null
    IntervalPattern.find(rest)?.let {
        months = it.groupValues[1].toInt()
        rest = rest.removeRange(it.range)
    }
    var condition: StudyCondition? = null
    for ((pattern, value) in ConditionPatterns) {
        val match = pattern.find(rest) ?: continue
        condition = value
        rest = rest.removeRange(match.range)
        break
    }
    var test: TestType? = null
    for ((pattern, value) in TestPatterns) {
        val match = pattern.find(rest) ?: continue
        test = value
        rest = rest.removeRange(match.range)
        break
    }
    var batch: String? = null
    BatchLabel.find(rest)?.let {
        batch = it.groupValues[1]
        rest = rest.removeRange(it.range)
    }

    var tokens = rest.split(Regex("""[\s|]+""")).map { it.trim(',', ';', ':') }.filter { it.isNotEmpty() }
    // A leading row number ("12" or "12.") is not part of the product name.
    if (tokens.size > 1 && tokens[0].matches(Regex("""\d{1,3}[.)]?""")) && tokens[1].first().isLetter()) {
        tokens = tokens.drop(1)
    }
    if (batch == null) {
        val index = tokens.indexOfLast { token -> token.length >= 3 && token.any { it.isDigit() } }
        if (index > 0) {
            batch = tokens[index]
            tokens = tokens.filterIndexed { i, _ -> i != index }
        }
    }

    val nameTokens = tokens.takeWhile { it.first().isLetter() }
    if (nameTokens.isEmpty()) return null
    // A word next to a date and nothing else is a header such as "Issued: 01/04/2026", not a plan line.
    val nothingElse = batch == null && tokens.size == nameTokens.size && months == null && condition == null && test == null
    if (nothingElse) return null

    return ParsedRow(
        productName = nameTokens.joinToString(" "),
        concentration = tokens.drop(nameTokens.size).joinToString(" "),
        batchNo = batch.orEmpty(),
        date = date,
        condition = condition,
        months = months,
        test = test,
        source = raw.trim()
    )
}

private fun findDate(line: String): Pair<String, LocalDate>? {
    IsoDate.find(line)?.let { m ->
        val (y, mo, d) = m.destructured
        dateOrNull(y.toInt(), mo.toInt(), d.toInt())?.let { return m.value to it }
    }
    DayFirstDate.find(line)?.let { m ->
        val (d, mo, y) = m.destructured
        val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
        dateOrNull(year, mo.toInt(), d.toInt())?.let { return m.value to it }
    }
    NamedMonthDate.find(line)?.let { m ->
        val (d, name, y) = m.destructured
        val month = MonthNames.indexOf(name.take(3).lowercase()) + 1
        if (month > 0) dateOrNull(y.toInt(), month, d.toInt())?.let { return m.value to it }
    }
    return null
}

private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
    runCatching { LocalDate.of(year, month, day) }.getOrNull()
