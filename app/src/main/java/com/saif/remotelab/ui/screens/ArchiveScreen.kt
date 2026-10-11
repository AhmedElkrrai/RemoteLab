package com.saif.remotelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.nextInterval
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.ChipGroup
import com.saif.remotelab.ui.components.EmptyHint
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

private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("ar-EG-u-nu-latn"))

@Composable
fun ArchiveScreen(
    months: List<MonthSummary>,
    query: String,
    selectedMonth: YearMonth?,
    records: List<PlanRecord>,
    onQueryChange: (String) -> Unit,
    onSelectMonth: (YearMonth?) -> Unit,
    onAddRecord: (PlanRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlanRecord?>(null) }
    val searching = query.isNotBlank()

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("ابحث باسم المستحضر، مثل Spectrabact") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                                if (searching) "نتائج البحث (${records.size})"
                                else "خطة ${selectedMonth?.format(MonthFormat)} (${records.size})"
                            )
                            if (!searching) {
                                TextButton(onClick = { onSelectMonth(null) }) { Text("كل الشهور") }
                            }
                        }
                    }
                    if (records.isEmpty()) {
                        item { EmptyHint(if (searching) "لا توجد سجلات بهذا الاسم." else "لا توجد سجلات في هذا الشهر.") }
                    }
                    val studies = records.groupBy { StudyKey(it.productName.trim().lowercase(), it.batchNo.trim(), it.condition) }
                    items(studies.entries.toList(), key = { it.key.toString() }) { (key, rows) ->
                        StudyCard(key.condition, rows, showForecast = searching, onDelete = { pendingDelete = it })
                    }
                }

                months.isEmpty() -> item {
                    EmptyHint("الأرشيف فارغ. أضف سجلات الخطة الشهرية بزر «إضافة سجل».")
                }

                else -> {
                    item { SectionTitle("الخطط الشهرية (${months.size})") }
                    items(months, key = { it.monthPlan.toString() }) { month ->
                        AppCard(tone = Tone.Brand, onClick = { onSelectMonth(month.monthPlan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(month.monthPlan.format(MonthFormat), fontWeight = FontWeight.Bold)
                                ToneBadge("${month.count} سجل", Tone.Brand)
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showForm = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("إضافة سجل") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
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

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف السجل؟") },
            text = { Text("${record.displayName} · Batch ${record.batchNo} · ${record.interval} · ${record.dateOfAnalysis}") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecord(record.id)
                    pendingDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") } }
        )
    }
}

private data class StudyKey(val product: String, val batch: String, val condition: StudyCondition)

/** All plan lines of one batch under one condition, i.e. the history of one stability study. */
@Composable
private fun StudyCard(
    condition: StudyCondition,
    rows: List<PlanRecord>,
    showForecast: Boolean,
    onDelete: (PlanRecord) -> Unit
) {
    val first = rows.first()
    AppCard(tone = if (condition == StudyCondition.Accelerated) Tone.Amber else Tone.Emerald) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(first.displayName, fontWeight = FontWeight.Bold)
                Text("Batch ${first.batchNo}", color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
            }
            ToneBadge(condition.label, if (condition == StudyCondition.Accelerated) Tone.Amber else Tone.Emerald)
        }
        HorizontalDivider(color = Palette.Line)
        rows.sortedBy { it.dateOfAnalysis }.forEach { record ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(record.dateOfAnalysis.toString(), Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                Text(record.interval, Modifier.weight(1f), color = Palette.TextSoft, style = MaterialTheme.typography.bodySmall)
                Text(record.test.label, Modifier.weight(1.3f), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onDelete(record) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف السجل", tint = Palette.Muted)
                }
            }
        }
        // Only meaningful over the full history, so it is hidden in the single-month view.
        if (showForecast) {
            nextInterval(condition, rows)?.let { next ->
                Text(
                    "الفترة التالية المتوقعة: ${next.months}M ${condition.short} · حوالي ${next.expected}",
                    color = Tone.Brand.fg,
                    style = MaterialTheme.typography.bodySmall
                )
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
    var test by rememberSaveable { mutableStateOf(TestType.Assay) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سجل جديد في الخطة") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(productName, { productName = it; error = null }, label = { Text("المستحضر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(concentration, { concentration = it }, label = { Text("التركيز") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(batchNo, { batchNo = it; error = null }, label = { Text("Batch No") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                FieldLabel("تاريخ التحليل")
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("$date  ·  خطة ${YearMonth.from(date).format(MonthFormat)}")
                }

                FieldLabel("نوع الدراسة")
                ChipGroup(
                    items = StudyCondition.entries,
                    isSelected = { it == condition },
                    label = { it.label },
                    onClick = {
                        condition = it
                        months = months.takeIf { m -> m in it.months }
                        error = null
                    }
                )
                if (condition.hasIntervals) {
                    FieldLabel("الفترة الزمنية")
                    ChipGroup(condition.months, { it == months }, { "${it}M" }, { months = it; error = null })
                }

                FieldLabel("نوع التحليل")
                ChipGroup(TestType.entries, { it == test }, { it.label }, { test = it })

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    productName.isBlank() -> "اكتب اسم المستحضر"
                    batchNo.isBlank() -> "اكتب رقم التشغيلة"
                    condition.hasIntervals && months == null -> "اختر الفترة الزمنية"
                    else -> null
                }
                if (error == null) {
                    onSubmit(
                        PlanRecord(
                            productName = productName.trim(),
                            concentration = concentration.trim(),
                            batchNo = batchNo.trim(),
                            dateOfAnalysis = date,
                            condition = condition,
                            months = months.takeIf { condition.hasIntervals },
                            test = test,
                            monthPlan = YearMonth.from(date)
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )

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
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("إلغاء") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
