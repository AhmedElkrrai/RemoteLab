package com.saif.remotelab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.remotelab.data.InstrumentKind
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.TestType
import com.saif.remotelab.ui.theme.Palette
import com.saif.remotelab.ui.theme.Tone

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun ToneBadge(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(tone.base.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = tone.fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

/** Pill with a leading dot, used for instrument status. */
@Composable
fun StatusBadge(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(tone.base.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(tone.base)
        )
        Text(text, color = tone.fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Square tile naming the instrument type, tinted by the instrument's current tone. */
@Composable
fun KindTile(kind: InstrumentKind, tone: Tone, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tone.base.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (kind) {
                InstrumentKind.HPLC -> "HPLC"
                InstrumentKind.GC -> "GC"
                InstrumentKind.AAS -> "AAS"
            },
            color = tone.fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatTile(label: String, count: Int, tone: Tone, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, CardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(count.toString(), color = tone.fg, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatusSummary(board: List<InstrumentState>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Running", board.count { it.status == InstrumentStatus.Running }, Tone.Emerald, Modifier.weight(1f))
        StatTile("Available", board.count { it.status == InstrumentStatus.Idle }, Tone.Brand, Modifier.weight(1f))
        StatTile("Maint.", board.count { it.status == InstrumentStatus.Maintenance }, Tone.Rose, Modifier.weight(1f))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 10.dp, bottom = 2.dp),
        color = Palette.Muted,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, color = Palette.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = isSelected(item),
                onClick = { onClick(item) },
                label = { Text(label(item)) }
            )
        }
    }
}

/** Related-only runs are purple, every other run emerald. */
fun Run.tone(): Tone = if (tests == setOf(TestType.Related)) Tone.Purple else Tone.Emerald

fun InstrumentState.tone(): Tone = when (status) {
    InstrumentStatus.Running -> running?.tone() ?: Tone.Emerald
    InstrumentStatus.Maintenance -> Tone.Rose
    InstrumentStatus.Idle -> when (instrument.kind) {
        InstrumentKind.HPLC -> Tone.Brand
        InstrumentKind.GC -> Tone.Amber
        InstrumentKind.AAS -> Tone.Cyan
    }
}

fun InstrumentStatus.label(): String = when (this) {
    InstrumentStatus.Running -> "Running"
    InstrumentStatus.Idle -> "Available"
    InstrumentStatus.Maintenance -> "Maintenance"
}

fun Run.details(): String =
    listOfNotNull(
        batchNo.takeIf { it.isNotBlank() }?.let { "Batch $it" },
        interval,
        tests.joinToString(" + ") { it.label }
    ).joinToString("  ·  ")
