package com.saif.remotelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.validateRun
import com.saif.remotelab.ui.components.ChipGroup
import com.saif.remotelab.ui.components.label
import com.saif.remotelab.ui.theme.Palette

/** Product → interval → instrument → test. Starts the run, or queues it when the instrument is busy. */
@Composable
fun RunFormDialog(
    initial: RunDraft,
    board: List<InstrumentState>,
    onDismiss: () -> Unit,
    onSubmit: (RunDraft) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val target = board.firstOrNull { it.instrument.id == draft.instrumentId }

    fun edit(next: RunDraft) {
        draft = next
        error = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تشغيل جديد") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.productName,
                    onValueChange = { edit(draft.copy(productName = it)) },
                    label = { Text("المستحضر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.concentration,
                    onValueChange = { edit(draft.copy(concentration = it)) },
                    label = { Text("التركيز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.batchNo,
                    onValueChange = { edit(draft.copy(batchNo = it)) },
                    label = { Text("Batch No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FieldLabel("نوع الدراسة")
                ChipGroup(
                    items = StudyCondition.entries,
                    isSelected = { it == draft.condition },
                    label = { it.label },
                    onClick = { edit(draft.copy(condition = it, months = draft.months.takeIf { m -> m in it.months })) }
                )
                if (draft.condition.hasIntervals) {
                    FieldLabel("الفترة الزمنية")
                    ChipGroup(
                        items = draft.condition.months,
                        isSelected = { it == draft.months },
                        label = { "${it}M" },
                        onClick = { edit(draft.copy(months = it)) }
                    )
                }

                FieldLabel("الجهاز")
                ChipGroup(
                    items = board,
                    isSelected = { it.instrument.id == draft.instrumentId },
                    label = { "${it.instrument.id} · ${it.status.label()}" },
                    onClick = { picked ->
                        // Tests belong to an instrument kind, so drop the ones the new instrument cannot run.
                        edit(
                            draft.copy(
                                instrumentId = picked.instrument.id,
                                tests = draft.tests.filter { it.kind == picked.instrument.kind }.toSet()
                            )
                        )
                    }
                )

                if (target != null) {
                    FieldLabel("نوع التحليل")
                    ChipGroup(
                        items = TestType.entries.filter { it.kind == target.instrument.kind },
                        isSelected = { it in draft.tests },
                        label = { it.label },
                        onClick = { edit(draft.copy(tests = if (it in draft.tests) draft.tests - it else draft.tests + it)) }
                    )
                    if (!target.isFree) {
                        Text(
                            "الجهاز مشغول، سيُضاف التحليل إلى قائمة الانتظار.",
                            color = Palette.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = validateRun(draft, board)
                if (error == null) onSubmit(draft)
            }) {
                Text(if (target == null || target.isFree) "ابدأ التشغيل" else "أضف للانتظار")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, color = Palette.TextSoft, style = MaterialTheme.typography.labelLarge)
}
