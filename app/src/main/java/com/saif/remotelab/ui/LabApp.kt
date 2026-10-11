package com.saif.remotelab.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saif.remotelab.ui.screens.ArchiveScreen
import com.saif.remotelab.ui.screens.HistoryScreen
import com.saif.remotelab.ui.screens.LiveStatusScreen
import com.saif.remotelab.ui.screens.OperationScreen
import com.saif.remotelab.ui.theme.Palette

private enum class Tab(val label: String, val title: String, val icon: ImageVector) {
    Operation("Operation", "Operation", Icons.Filled.Home),
    Live("Live", "Live status", Icons.Filled.PlayArrow),
    Archive("Archive", "Plan archive", Icons.Filled.DateRange),
    History("History", "Run history", Icons.Filled.CheckCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabApp(viewModel: LabViewModel = viewModel(factory = LabViewModel.Factory)) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex]
    val snackbar = remember { SnackbarHostState() }

    val board by viewModel.board.collectAsStateWithLifecycle()
    val todayRecords by viewModel.todayRecords.collectAsStateWithLifecycle()
    val months by viewModel.months.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importRows by viewModel.importRows.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val historyQuery by viewModel.historyQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "THABAT QC LAB",
                            color = Palette.Brand,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(tab.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Bg)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Palette.Deep) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tabIndex = item.ordinal },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.Operation -> OperationScreen(
                board = board,
                todayRecords = todayRecords,
                onSubmitRun = viewModel::submitRun,
                onStartQueued = viewModel::startQueued,
                onFinishRun = viewModel::finishRun,
                onRemoveQueued = viewModel::removeQueued,
                onSetMaintenance = viewModel::setMaintenance,
                modifier = modifier
            )

            Tab.Live -> LiveStatusScreen(board = board, modifier = modifier)

            Tab.Archive -> ArchiveScreen(
                months = months,
                query = query,
                selectedMonth = selectedMonth,
                records = records,
                importing = importing,
                importRows = importRows,
                onQueryChange = viewModel::setQuery,
                onSelectMonth = viewModel::selectMonth,
                onAddRecord = viewModel::addRecord,
                onDeleteRecord = viewModel::deleteRecord,
                onPickPdf = viewModel::readPlanPdf,
                onRemoveImportRow = viewModel::removeImportRow,
                onCancelImport = viewModel::cancelImport,
                onConfirmImport = viewModel::confirmImport,
                modifier = modifier
            )

            Tab.History -> HistoryScreen(
                runs = history,
                query = historyQuery,
                onQueryChange = viewModel::setHistoryQuery,
                modifier = modifier
            )
        }
    }
}
