package com.saif.remotelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.toDraft
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.KindTile
import com.saif.remotelab.ui.components.SectionTitle
import com.saif.remotelab.ui.components.StatusBadge
import com.saif.remotelab.ui.components.StatusSummary
import com.saif.remotelab.ui.components.ToneBadge
import com.saif.remotelab.ui.components.details
import com.saif.remotelab.ui.components.label
import com.saif.remotelab.ui.components.tone
import com.saif.remotelab.ui.theme.Palette
import com.saif.remotelab.ui.theme.Tone

@Composable
fun OperationScreen(
    board: List<InstrumentState>,
    todayRecords: List<PlanRecord>,
    onSubmitRun: (RunDraft) -> Unit,
    onStartQueued: (Long) -> Unit,
    onFinishRun: (Long) -> Unit,
    onRemoveQueued: (Long) -> Unit,
    onSetMaintenance: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var openInstrumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var formDraft by remember { mutableStateOf<RunDraft?>(null) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { StatusSummary(board) }

            if (todayRecords.isNotEmpty()) {
                item { SectionTitle("Due today in the plan · ${todayRecords.size}") }
                items(todayRecords, key = { "record-${it.id}" }) { record ->
                    AppCard(onClick = { formDraft = record.toDraft() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(record.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Batch ${record.batchNo}  ·  ${record.interval}  ·  ${record.testLabel}",
                                    color = Palette.Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            ToneBadge("Start", Tone.Brand)
                        }
                    }
                }
            }

            item { SectionTitle("Instruments · ${board.size}") }
            items(board, key = { it.instrument.id }) { state ->
                InstrumentCard(state, onClick = { openInstrumentId = state.instrument.id })
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formDraft = RunDraft() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New run", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    board.firstOrNull { it.instrument.id == openInstrumentId }?.let { state ->
        InstrumentSheet(
            state = state,
            onDismiss = { openInstrumentId = null },
            onNewRun = { formDraft = RunDraft(instrumentId = state.instrument.id) },
            onStartQueued = onStartQueued,
            onFinishRun = onFinishRun,
            onRemoveQueued = onRemoveQueued,
            onSetMaintenance = { onSetMaintenance(state.instrument.id, it) }
        )
    }

    formDraft?.let { draft ->
        RunFormDialog(
            initial = draft,
            board = board,
            onDismiss = { formDraft = null },
            onSubmit = {
                onSubmitRun(it)
                formDraft = null
            }
        )
    }
}

@Composable
internal fun InstrumentCard(state: InstrumentState, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val running = state.running
    AppCard(onClick = onClick, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KindTile(state.instrument.kind, state.tone())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.instrument.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = running?.displayName ?: "No active run",
                    color = if (running != null) Palette.TextSoft else Palette.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusBadge(state.status.label(), state.tone())
        }
        if (running != null || state.queue.isNotEmpty()) {
            HorizontalDivider(color = Palette.Line)
            running?.let {
                Text(it.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
            }
            state.queue.firstOrNull()?.let { next ->
                val more = if (state.queue.size > 1) "  (+${state.queue.size - 1} more)" else ""
                Text("Next: ${next.displayName}$more", color = Palette.TextSoft, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstrumentSheet(
    state: InstrumentState,
    onDismiss: () -> Unit,
    onNewRun: () -> Unit,
    onStartQueued: (Long) -> Unit,
    onFinishRun: (Long) -> Unit,
    onRemoveQueued: (Long) -> Unit,
    onSetMaintenance: (Boolean) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.Bg) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                KindTile(state.instrument.kind, state.tone())
                Column(Modifier.weight(1f)) {
                    Text(state.instrument.id, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.instrument.kind.label, color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                }
                StatusBadge(state.status.label(), state.tone())
            }

            SectionTitle("Current run")
            val running = state.running
            if (running == null) {
                Text("Nothing is running on this instrument.", color = Palette.Muted)
            } else {
                RunRow(running) {
                    TextButton(onClick = { onFinishRun(running.id) }) { Text("Finish run") }
                }
            }

            SectionTitle("Queue · ${state.queue.size}")
            if (state.queue.isEmpty()) {
                Text("The queue is empty.", color = Palette.Muted)
            }
            state.queue.forEachIndexed { index, run ->
                RunRow(run) {
                    // Only the head of the queue can start, and only on a free instrument.
                    if (index == 0 && state.isFree) {
                        TextButton(onClick = { onStartQueued(run.id) }) { Text("Start") }
                    }
                    TextButton(onClick = { onRemoveQueued(run.id) }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNewRun,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (state.isFree) "New run" else "Add to queue")
                }
                if (state.status != InstrumentStatus.Running) {
                    OutlinedButton(
                        onClick = { onSetMaintenance(!state.instrument.maintenance) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (state.instrument.maintenance) "End maintenance" else "Maintenance")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunRow(run: Run, actions: @Composable () -> Unit) {
    AppCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(run.displayName, fontWeight = FontWeight.SemiBold)
            Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
    }
}
