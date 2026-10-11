package com.saif.remotelab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.EmptyHint
import com.saif.remotelab.ui.components.SectionTitle
import com.saif.remotelab.ui.components.ToneBadge
import com.saif.remotelab.ui.components.details
import com.saif.remotelab.ui.components.tone
import com.saif.remotelab.ui.theme.Palette
import com.saif.remotelab.ui.theme.Tone
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
    val idle = board.filter { it.status == InstrumentStatus.Idle }
    val maintenance = board.filter { it.status == InstrumentStatus.Maintenance }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountTile("يعمل", running.size, Tone.Emerald, Modifier.weight(1f))
                CountTile("متاح", idle.size, Tone.Slate, Modifier.weight(1f))
                CountTile("صيانة", maintenance.size, Tone.Rose, Modifier.weight(1f))
            }
        }

        item { SectionTitle("يعمل الآن") }
        if (running.isEmpty()) {
            item { EmptyHint("لا توجد أجهزة تعمل الآن.") }
        }
        items(running, key = { it.instrument.id }) { state ->
            val run = state.running ?: return@items
            AppCard(tone = state.tone()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.instrument.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    run.startedAt?.let { ToneBadge(elapsedLabel(it, now), state.tone()) }
                }
                Text(run.displayName, fontWeight = FontWeight.Medium)
                Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = state.queue.firstOrNull()?.let { "التالي: ${it.displayName} · ${it.interval}" }
                        ?: "لا يوجد تحليل تالٍ في الانتظار",
                    color = Palette.TextSoft,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        val waiting = (idle + maintenance).filter { it.queue.isNotEmpty() }
        if (waiting.isNotEmpty()) {
            item { SectionTitle("تحاليل تنتظر جهازاً متوقفاً") }
            items(waiting, key = { "waiting-${it.instrument.id}" }) { state ->
                InstrumentCard(state, onClick = null)
            }
        }
    }
}

@Composable
private fun CountTile(label: String, count: Int, tone: Tone, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Card)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(count.toString(), color = tone.fg, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun elapsedLabel(startedAt: Long, now: Long): String {
    val minutes = ((now - startedAt) / 60_000).coerceAtLeast(0)
    val hours = minutes / 60
    return when {
        minutes < 1 -> "بدأ الآن"
        hours < 1 -> "منذ $minutes د"
        else -> "منذ $hours س ${minutes % 60} د"
    }
}
