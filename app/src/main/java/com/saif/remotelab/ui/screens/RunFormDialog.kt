package com.saif.remotelab.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.RunField
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.toggleInterval
import com.saif.remotelab.data.validateRun
import com.saif.remotelab.ui.components.ChipGroup
import com.saif.remotelab.ui.components.FieldLabel
import com.saif.remotelab.ui.components.FormScreen
import com.saif.remotelab.ui.components.FormSection
import com.saif.remotelab.ui.components.FormTextField
import com.saif.remotelab.ui.components.label
import com.saif.remotelab.ui.theme.Palette

/** Starts the run, or queues it when the chosen instrument is busy. */
@Composable
fun RunFormDialog(
    initial: RunDraft,
    board: List<InstrumentState>,
    onDismiss: () -> Unit,
    onSubmit: (RunDraft) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    // Problems stay hidden until the first submit attempt, then update as the analyst fixes them.
    var attempted by remember { mutableStateOf(false) }
    val problems = if (attempted) validateRun(draft, board) else emptyMap()
    val target = board.firstOrNull { it.instrument.id == draft.instrumentId }

    FormScreen(
        title = "New run",
        submitLabel = if (target == null || target.isFree) "Start run" else "Add to queue",
        problems = problems.map { (field, message) ->
            if (field == RunField.Duplicate) message else "${field.label}: $message"
        },
        onDismiss = onDismiss,
        onSubmit = {
            attempted = true
            if (validateRun(draft, board).isEmpty()) onSubmit(draft)
        }
    ) {
        FormSection("Product") {
            FormTextField(draft.productName, { draft = draft.copy(productName = it) }, "Product name", problems[RunField.Product])
            FormTextField(draft.concentration, { draft = draft.copy(concentration = it) }, "Concentration", problems[RunField.Concentration])
            FormTextField(draft.batchNo, { draft = draft.copy(batchNo = it) }, "Batch No", problems[RunField.Batch])
        }

        FormSection("Study") {
            FieldLabel("Study type")
            ChipGroup(
                items = StudyCondition.entries,
                isSelected = { it == draft.condition },
                label = { it.label },
                onClick = { draft = draft.copy(condition = it, months = draft.months.filter { m -> m in it.months }.toSet()) }
            )
            if (draft.condition.hasIntervals) {
                FieldLabel("Time interval", problems[RunField.Interval])
                ChipGroup(
                    items = draft.condition.months,
                    isSelected = { it in draft.months },
                    label = { "${it}M" },
                    onClick = { draft = draft.copy(months = toggleInterval(draft.months, it)) }
                )
                Text(
                    "Pick one interval. 3M and 6M can be run together.",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        FormSection("Instrument") {
            FieldLabel("Instrument", problems[RunField.Instrument])
            ChipGroup(
                items = board,
                isSelected = { it.instrument.id == draft.instrumentId },
                label = { "${it.instrument.id} · ${it.status.label()}" },
                onClick = { picked ->
                    // Tests belong to an instrument kind, so drop the ones the new instrument cannot run.
                    draft = draft.copy(
                        instrumentId = picked.instrument.id,
                        tests = draft.tests.filter { it.kind == picked.instrument.kind }.toSet()
                    )
                }
            )
            if (target != null && !target.isFree) {
                Text(
                    "${target.instrument.id} is busy, so this run will wait in its queue.",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FieldLabel("Test type", problems[RunField.Tests])
            if (target == null) {
                Text(
                    "Select an instrument to see the tests it can run.",
                    color = Palette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                ChipGroup(
                    items = TestType.entries.filter { it.kind == target.instrument.kind },
                    isSelected = { it in draft.tests },
                    label = { it.label },
                    onClick = { draft = draft.copy(tests = if (it in draft.tests) draft.tests - it else draft.tests + it) }
                )
            }
        }
    }
}
