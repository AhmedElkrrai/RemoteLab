package com.saif.remotelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.EmptyState
import com.saif.remotelab.ui.components.KindTile
import com.saif.remotelab.ui.components.SectionTitle
import com.saif.remotelab.ui.components.StatusSummary
import com.saif.remotelab.ui.components.ToneBadge
import com.saif.remotelab.ui.components.details
import com.saif.remotelab.ui.components.tone
import com.saif.remotelab.ui.theme.Palette
import kotlinx.coroutines.delay

/** Read-only board: what is running, for how long, and what comes next on each instrument. */
@Composable
fun LiveStatusScreen(board: List<InstrumentState>, modifier: Modifier = Modifier) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val running = board.filter { it.status == InstrumentStatus.Running }
    val waiting = board.filter { it.status != InstrumentStatus.Running && it.queue.isNotEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { StatusSummary(board) }

        item { SectionTitle("Running now · ${running.size}") }
        if (running.isEmpty()) {
            item { EmptyState("No instruments are running", "Runs started from the Operation tab appear here.") }
        }
        items(running, key = { it.instrument.id }) { state ->
            val run = state.running ?: return@items
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    KindTile(state.instrument.kind, state.tone())
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.instrument.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(run.displayName, color = Palette.TextSoft, style = MaterialTheme.typography.bodyMedium)
                    }
                    run.startedAt?.let { ToneBadge(elapsedLabel(it, now), state.tone()) }
                }
                HorizontalDivider(color = Palette.Line)
                Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = state.queue.firstOrNull()?.let { "Next: ${it.displayName}  ·  ${it.interval}" }
                        ?: "Nothing queued after this run",
                    color = Palette.TextSoft,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (waiting.isNotEmpty()) {
            item { SectionTitle("Queued on a stopped instrument · ${waiting.size}") }
            items(waiting, key = { "waiting-${it.instrument.id}" }) { state ->
                InstrumentCard(state, onClick = null)
            }
        }
    }
}

private fun elapsedLabel(startedAt: Long, now: Long): String {
    val minutes = ((now - startedAt) / 60_000).coerceAtLeast(0)
    val hours = minutes / 60
    return when {
        minutes < 1 -> "Just started"
        hours < 1 -> "$minutes min"
        else -> "$hours h ${minutes % 60} min"
    }
}
