package com.saif.remotelab.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.MonthSummary
import com.saif.remotelab.data.ParsedRow
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.nextInterval
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.ChipGroup
import com.saif.remotelab.ui.components.EmptyState
import com.saif.remotelab.ui.components.FieldLabel
import com.saif.remotelab.ui.components.FormScreen
import com.saif.remotelab.ui.components.FormSection
import com.saif.remotelab.ui.components.FormTextField
import com.saif.remotelab.ui.components.SectionTitle
import com.saif.remotelab.ui.components.ToneBadge
import com.saif.remotelab.ui.theme.Palette
import com.saif.remotelab.ui.theme.Tone
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun ArchiveScreen(
    months: List<MonthSummary>,
    query: String,
    selectedMonth: YearMonth?,
    records: List<PlanRecord>,
    importing: Boolean,
    importRows: List<ParsedRow>?,
    onQueryChange: (String) -> Unit,
    onSelectMonth: (YearMonth?) -> Unit,
    onAddRecord: (PlanRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onPickPdf: (Uri) -> Unit,
    onRemoveImportRow: (ParsedRow) -> Unit,
    onCancelImport: () -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlanRecord?>(null) }
    val searching = query.isNotBlank()
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickPdf(uri)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by product or batch") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    enabled = !importing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (importing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Import plan PDF", fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(
                    onClick = { showForm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Add record")
                }
            }
        }

        when {
            searching || selectedMonth != null -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(
                            if (searching) "Search results · ${records.size}"
                            else "${selectedMonth?.format(MonthFormat)} · ${records.size}"
                        )
                        if (!searching) {
                            TextButton(onClick = { onSelectMonth(null) }) { Text("All months") }
                        }
                    }
                }
                if (records.isEmpty()) {
                    item {
                        if (searching) EmptyState("No matches", "No record has a product or batch containing \"$query\".")
                        else EmptyState("No records", "This monthly plan has no records.")
                    }
                }
                val studies = records.groupBy { StudyKey(it.productName.trim().lowercase(), it.batchNo.trim(), it.condition) }
                items(studies.entries.toList(), key = { it.key.toString() }) { (key, rows) ->
                    StudyCard(key.condition, rows, showForecast = searching, onDelete = { pendingDelete = it })
                }
            }

            months.isEmpty() -> item {
                EmptyState("The archive is empty", "Import a monthly plan PDF, or add records one by one.")
            }

            else -> {
                item { SectionTitle("Monthly plans · ${months.size}") }
                items(months, key = { it.monthPlan.toString() }) { month ->
                    AppCard(onClick = { onSelectMonth(month.monthPlan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(month.monthPlan.format(MonthFormat), fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${month.count} ${if (month.count == 1) "record" else "records"}",
                                    color = Palette.Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        RecordFormDialog(
            initialDate = selectedMonth?.takeIf { it != YearMonth.now() }?.atDay(1) ?: LocalDate.now(),
            onDismiss = { showForm = false },
            onSubmit = {
                onAddRecord(it)
                showForm = false
            }
        )
    }

    importRows?.let { rows ->
        ImportReview(rows, onRemove = onRemoveImportRow, onDismiss = onCancelImport, onConfirm = onConfirmImport)
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this record?") },
            text = {
                Text("${record.displayName} · Batch ${record.batchNo} · ${record.interval} · ${record.dateOfAnalysis.format(DateFormat)}")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecord(record.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

private data class StudyKey(val product: String, val batch: String, val condition: StudyCondition?)

/** All plan lines of one batch under one condition, i.e. the history of one stability study. */
@Composable
private fun StudyCard(
    condition: StudyCondition?,
    rows: List<PlanRecord>,
    showForecast: Boolean,
    onDelete: (PlanRecord) -> Unit
) {
    val first = rows.first()
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(first.displayName, fontWeight = FontWeight.SemiBold)
                Text(
                    if (first.batchNo.isBlank()) "Batch not set" else "Batch ${first.batchNo}",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when (condition) {
                null -> ToneBadge("Study not set", Tone.Slate)
                StudyCondition.Accelerated -> ToneBadge(condition.label, Tone.Amber)
                else -> ToneBadge(condition.label, Tone.Emerald)
            }
        }
        HorizontalDivider(color = Palette.Line)
        rows.sortedBy { it.dateOfAnalysis }.forEach { record ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(record.dateOfAnalysis.format(DateFormat), Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                Text(record.interval, Modifier.weight(1.1f), color = Palette.TextSoft, style = MaterialTheme.typography.bodySmall)
                Text(record.testLabel, Modifier.weight(1.2f), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onDelete(record) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete record", tint = Palette.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        // Only meaningful over the full history, so it is hidden in the single-month view.
        if (showForecast && condition != null) {
            nextInterval(condition, rows)?.let { next ->
                Text(
                    "Next expected: ${next.months}M ${condition.short}, around ${next.expected.format(DateFormat)}",
                    color = Tone.Brand.fg,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Shows what was read from the PDF so wrong lines can be dropped before anything is saved. */
@Composable
private fun ImportReview(
    rows: List<ParsedRow>,
    onRemove: (ParsedRow) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    FormScreen(
        title = "Review import",
        submitLabel = if (rows.size == 1) "Import 1 record" else "Import ${rows.size} records",
        problems = emptyList(),
        onDismiss = onDismiss,
        onSubmit = { if (rows.isEmpty()) onDismiss() else onConfirm() }
    ) {
        Text(
            "These lines were read from the PDF. Check the product, batch and date, and remove any line that was read wrongly.",
            color = Palette.Muted,
            style = MaterialTheme.typography.bodySmall
        )
        rows.forEach { row ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            listOf(row.productName, row.concentration).filter { it.isNotBlank() }.joinToString(" "),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${if (row.batchNo.isBlank()) "Batch not found" else "Batch ${row.batchNo}"}  ·  ${row.date.format(DateFormat)}",
                            color = Palette.TextSoft,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onRemove(row) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove line", tint = Palette.Muted)
                    }
                }
                Text("PDF line: ${row.source}", color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordFormDialog(initialDate: LocalDate, onDismiss: () -> Unit, onSubmit: (PlanRecord) -> Unit) {
    var productName by rememberSaveable { mutableStateOf("") }
    var concentration by rememberSaveable { mutableStateOf("") }
    var batchNo by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var condition by rememberSaveable { mutableStateOf(StudyCondition.Accelerated) }
    var months by rememberSaveable { mutableStateOf<Int?>(null) }
    var test by rememberSaveable { mutableStateOf<TestType?>(null) }
    var attempted by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    fun problem(missing: Boolean, message: String) = message.takeIf { attempted && missing }
    val productError = problem(productName.isBlank(), "Enter the product name")
    val concentrationError = problem(concentration.isBlank(), "Enter the concentration")
    val batchError = problem(batchNo.isBlank(), "Enter the batch number")
    val intervalError = problem(condition.hasIntervals && months == null, "Select a time interval")
    val testError = problem(test == null, "Select a test type")

    FormScreen(
        title = "New plan record",
        submitLabel = "Save record",
        problems = listOfNotNull(
            productError?.let { "Product: $it" },
            concentrationError?.let { "Concentration: $it" },
            batchError?.let { "Batch No: $it" },
            intervalError?.let { "Time interval: $it" },
            testError?.let { "Test type: $it" }
        ),
        onDismiss = onDismiss,
        onSubmit = {
            attempted = true
            val chosenTest = test
            val complete = productName.isNotBlank() && concentration.isNotBlank() && batchNo.isNotBlank() &&
                (!condition.hasIntervals || months != null)
            if (complete && chosenTest != null) {
                onSubmit(
                    PlanRecord(
                        productName = productName.trim(),
                        concentration = concentration.trim(),
                        batchNo = batchNo.trim(),
                        dateOfAnalysis = date,
                        condition = condition,
                        months = months.takeIf { condition.hasIntervals },
                        test = chosenTest,
                        monthPlan = YearMonth.from(date)
                    )
                )
            }
        }
    ) {
        FormSection("Product") {
            FormTextField(productName, { productName = it }, "Product name", productError)
            FormTextField(concentration, { concentration = it }, "Concentration", concentrationError)
            FormTextField(batchNo, { batchNo = it }, "Batch No", batchError)
        }

        FormSection("Schedule") {
            FieldLabel("Date of analysis")
            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("${date.format(DateFormat)}  ·  ${YearMonth.from(date).format(MonthFormat)} plan")
            }

            FieldLabel("Study type")
            ChipGroup(
                items = StudyCondition.entries,
                isSelected = { it == condition },
                label = { it.label },
                onClick = {
                    condition = it
                    months = months.takeIf { m -> m in it.months }
                }
            )
            if (condition.hasIntervals) {
                FieldLabel("Time interval", intervalError)
                ChipGroup(condition.months, { it == months }, { "${it}M" }, { months = it })
            }

            FieldLabel("Test type", testError)
            ChipGroup(TestType.entries, { it == test }, { it.label }, { test = it })
        }
    }

    if (pickingDate) {
        // The picker works in UTC midnights, so convert through UTC on both sides.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
