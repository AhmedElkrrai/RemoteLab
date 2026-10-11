package com.saif.remotelab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saif.remotelab.data.Run
import com.saif.remotelab.ui.components.AppCard
import com.saif.remotelab.ui.components.EmptyState
import com.saif.remotelab.ui.components.SectionTitle
import com.saif.remotelab.ui.components.ToneBadge
import com.saif.remotelab.ui.components.details
import com.saif.remotelab.ui.components.tone
import com.saif.remotelab.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FinishedFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

/** Every finished run, newest first. */
@Composable
fun HistoryScreen(
    runs: List<Run>,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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

        item { SectionTitle("Completed runs · ${runs.size}") }
        if (runs.isEmpty()) {
            item {
                if (query.isBlank()) EmptyState("No completed runs yet", "A run appears here after you finish it on its instrument.")
                else EmptyState("No matches", "No completed run has a product or batch containing \"$query\".")
            }
        }
        items(runs, key = { it.id }) { run ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(run.displayName, fontWeight = FontWeight.SemiBold)
                        Text(run.details(), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    ToneBadge(run.instrumentId, run.tone())
                }
                HorizontalDivider(color = Palette.Line)
                Text(
                    listOfNotNull(
                        run.finishedAt?.let { "Finished ${format(it)}" },
                        duration(run)?.let { "Ran for $it" }
                    ).joinToString("  ·  "),
                    color = Palette.TextSoft,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun format(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(FinishedFormat)

private fun duration(run: Run): String? {
    val start = run.startedAt ?: return null
    val end = run.finishedAt ?: return null
    val minutes = ((end - start) / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}
