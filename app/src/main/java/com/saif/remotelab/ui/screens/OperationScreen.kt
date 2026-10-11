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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.toDraft
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.SectionTitle
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (todayRecords.isNotEmpty()) {
                item { SectionTitle("عينات اليوم في الخطة (${todayRecords.size})") }
                items(todayRecords, key = { "record-${it.id}" }) { record ->
                    AppCard(tone = Tone.Brand, onClick = { formDraft = record.toDraft() }) {
                        Text(record.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            "Batch ${record.batchNo} · ${record.interval} · ${record.test.label}",
                            color = Palette.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item { SectionTitle("الأجهزة (${board.size})") }
            items(board, key = { it.instrument.id }) { state ->
                InstrumentCard(state, onClick = { openInstrumentId = state.instrument.id })
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formDraft = RunDraft() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("تشغيل جديد") },
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
    AppCard(tone = state.tone(), onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(state.instrument.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(state.instrument.kind.label, color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
            }
            ToneBadge(state.status.label(), state.tone())
        }
        state.running?.let { run ->
            Text(run.displayName, fontWeight = FontWeight.Medium)
            Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
        }
        state.queue.firstOrNull()?.let { next ->
            val more = if (state.queue.size > 1) " (+${state.queue.size - 1})" else ""
            Text(
                "التالي: ${next.displayName}$more",
                color = Palette.TextSoft,
                style = MaterialTheme.typography.bodySmall
            )
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.instrument.id} · ${state.instrument.kind.label}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                ToneBadge(state.status.label(), state.tone())
            }

            SectionTitle("التشغيل الحالي")
            val running = state.running
            if (running == null) {
                Text("لا يوجد تشغيل الآن.", color = Palette.Muted)
            } else {
                RunRow(running) {
                    Button(onClick = { onFinishRun(running.id) }) { Text("إنهاء") }
                }
            }

            SectionTitle("قائمة الانتظار (${state.queue.size})")
            if (state.queue.isEmpty()) {
                Text("القائمة فارغة.", color = Palette.Muted)
            }
            state.queue.forEachIndexed { index, run ->
                RunRow(run) {
                    // Only the head of the queue can start, and only on a free instrument.
                    if (index == 0 && state.isFree) {
                        TextButton(onClick = { onStartQueued(run.id) }) { Text("ابدأ") }
                    }
                    TextButton(onClick = { onRemoveQueued(run.id) }) { Text("حذف") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onNewRun, modifier = Modifier.weight(1f)) {
                    Text(if (state.isFree) "تشغيل جديد" else "إضافة للانتظار")
                }
                if (state.status != InstrumentStatus.Running) {
                    OutlinedButton(
                        onClick = { onSetMaintenance(!state.instrument.maintenance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.instrument.maintenance) "إنهاء الصيانة" else "تحت الصيانة")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunRow(run: Run, actions: @Composable () -> Unit) {
    AppCard(tone = run.tone()) {
        Text(run.displayName, fontWeight = FontWeight.Medium)
        Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
    }
}
