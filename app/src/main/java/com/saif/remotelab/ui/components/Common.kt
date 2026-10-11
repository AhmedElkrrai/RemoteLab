package com.saif.remotelab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.InstrumentKind
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.InstrumentStatus
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.TestType
import com.saif.remotelab.ui.theme.Palette
import com.saif.remotelab.ui.theme.Tone

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun ToneBadge(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(tone.base.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = tone.fg,
        style = MaterialTheme.typography.labelMedium
    )
}

/** Card with an accent stripe on the start edge (the right side in RTL). */
@Composable
fun AppCard(
    tone: Tone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(tone.base)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp),
        color = Palette.TextSoft,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        color = Palette.Muted,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
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
        InstrumentKind.HPLC -> Tone.Slate
        InstrumentKind.GC -> Tone.Amber
        InstrumentKind.AAS -> Tone.Cyan
    }
}

fun InstrumentStatus.label(): String = when (this) {
    InstrumentStatus.Running -> "يعمل"
    InstrumentStatus.Idle -> "متاح"
    InstrumentStatus.Maintenance -> "صيانة"
}

fun Run.details(): String =
    listOfNotNull(
        batchNo.takeIf { it.isNotBlank() }?.let { "Batch $it" },
        interval,
        tests.joinToString(" + ") { it.label }
    ).joinToString(" · ")
