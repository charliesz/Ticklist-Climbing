package com.charlie.ticklist

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charlie.ticklist.data.RouteEntity
import com.charlie.ticklist.data.TicklistDatabase
import com.charlie.ticklist.ui.theme.TicklistClimbingTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RouteStatus {
    FLASH,
    TOP,
    ZONE,
    PROJECT
}

enum class StatusFilter {
    NONE,
    FLASH,
    TOP,
    ZONE,
    PROJECT
}

enum class SortColumn {
    ROUTE,
    FLASH,
    TOP,
    ZONE,
    PROJECT
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TicklistClimbingTheme {
                TicklistHomeScreen()
            }
        }
    }
}

@Composable
fun TicklistHomeScreen() {
    val context = LocalContext.current
    val database = remember {
        TicklistDatabase.getDatabase(context)
    }

    val routeDao = database.routeDao()
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val routes by routeDao
        .observeAllRoutes()
        .collectAsState(initial = emptyList())

    val allFilters = remember {
        setOf(
            StatusFilter.NONE,
            StatusFilter.FLASH,
            StatusFilter.TOP,
            StatusFilter.ZONE,
            StatusFilter.PROJECT
        )
    }

    var selectedFilters by remember {
        mutableStateOf(allFilters)
    }

    var sortColumn by remember {
        mutableStateOf(SortColumn.ROUTE)
    }

    var sortAscending by remember {
        mutableStateOf(true)
    }

    var selectionMode by remember {
        mutableStateOf(false)
    }

    var selectedRouteNumbers by remember {
        mutableStateOf<Set<Int>>(emptySet())
    }

    var showFilterMenu by remember {
        mutableStateOf(false)
    }

    var showSingleRouteDialog by remember {
        mutableStateOf(false)
    }

    var showBulkEditDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var editingRouteNumber by remember {
        mutableStateOf<Int?>(null)
    }

    var routeName by remember {
        mutableStateOf("")
    }

    var routeDifficulty by remember {
        mutableStateOf("")
    }

    var routeStatus by remember {
        mutableStateOf<RouteStatus?>(null)
    }

    var routeStatusChangedAt by remember {
        mutableStateOf<Long?>(null)
    }

    var routeCompletedDate by remember {
        mutableStateOf<Long?>(null)
    }

    var bulkStatusEnabled by remember {
        mutableStateOf(false)
    }

    var bulkStatus by remember {
        mutableStateOf<RouteStatus?>(null)
    }

    var bulkDateEnabled by remember {
        mutableStateOf(false)
    }

    var bulkDate by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(Unit) {
        if (routeDao.countRoutes() == 0) {
            routeDao.insertRoutes(
                (1..90).map { number ->
                    RouteEntity(
                        number = number,
                        name = "%02d".format(number),
                        difficulty = "",
                        status = null,
                        statusChangedAt = null,
                        completedDate = null
                    )
                }
            )
        }
    }

    val displayedRoutes = routes
        .filter { route ->
            routeFilter(route.status) in selectedFilters
        }
        .sortedWith(
            routeComparator(
                column = sortColumn,
                ascending = sortAscending
            )
        )

    val topCount = routes.count {
        it.status == "TOP" || it.status == "FLASH"
    }

    val flashCount = routes.count {
        it.status == "FLASH"
    }

    val zoneCount = routes.count {
        it.status == "ZONE"
    }

    fun openRoute(route: RouteEntity) {
        scope.launch {
            val currentRoute = routeDao.getRoute(route.number) ?: route

            editingRouteNumber = currentRoute.number
            routeName = currentRoute.name
            routeDifficulty = currentRoute.difficulty
            routeStatus = currentRoute.status.toRouteStatus()
            routeStatusChangedAt = currentRoute.statusChangedAt
            routeCompletedDate = currentRoute.completedDate
            showSingleRouteDialog = true
        }
    }

    fun toggleRouteSelection(number: Int) {
        selectedRouteNumbers =
            if (number in selectedRouteNumbers) {
                selectedRouteNumbers - number
            } else {
                selectedRouteNumbers + number
            }
    }

    fun resetBulkEditState() {
        bulkStatusEnabled = false
        bulkStatus = null
        bulkDateEnabled = false
        bulkDate = null
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
            ) {
                if (selectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedRouteNumbers.size} ausgewählt",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row {
                            TextButton(
                                onClick = {
                                    if (selectedRouteNumbers.isNotEmpty()) {
                                        resetBulkEditState()
                                        showBulkEditDialog = true
                                    }
                                }
                            ) {
                                Text("Bearbeiten")
                            }

                            TextButton(
                                onClick = {
                                    selectedRouteNumbers = emptySet()
                                    selectionMode = false
                                }
                            ) {
                                Text("Fertig")
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${displayedRoutes.size} von ${routes.size} Routen",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Lange drücken: Route bearbeiten",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectionMode = true
                                selectedRouteNumbers = emptySet()
                            },
                            contentPadding = PaddingValues(
                                horizontal = 10.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text(
                                text = "Bearbeiten",
                                fontSize = 11.sp
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = {
                                    showFilterMenu = true
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 2.dp
                                )
                            ) {
                                Text(
                                    text = "Filter",
                                    fontSize = 11.sp
                                )
                            }

                            FilterMenu(
                                expanded = showFilterMenu,
                                selectedFilters = selectedFilters,
                                allFilters = allFilters,
                                onDismiss = {
                                    showFilterMenu = false
                                },
                                onFiltersChanged = {
                                    selectedFilters = it
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            CalculationBar(
                topCount = topCount,
                flashCount = flashCount,
                zoneCount = zoneCount,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRouteNumber = null
                    routeName = ""
                    routeDifficulty = ""
                    routeStatus = null
                    routeStatusChangedAt = null
                    routeCompletedDate = null
                    showSingleRouteDialog = true
                }
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RouteTableHeader(
                    sortColumn = sortColumn,
                    sortAscending = sortAscending,
                    onSort = { column ->
                        if (sortColumn == column) {
                            sortAscending = !sortAscending
                        } else {
                            sortColumn = column
                            sortAscending = true
                        }
                    }
                )
            }

            items(
                items = displayedRoutes,
                key = { it.number }
            ) { route ->

                RouteRow(
                    route = route,
                    selectionMode = selectionMode,
                    selected = route.number in selectedRouteNumbers,
                    onSelectedChange = {
                        toggleRouteSelection(route.number)

                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                    },
                    onStatusChange = { newStatus ->
                        val now = System.currentTimeMillis()

                        val completedDate =
                            if (
                                newStatus == RouteStatus.TOP ||
                                newStatus == RouteStatus.FLASH
                            ) {
                                route.completedDate ?: now
                            } else {
                                null
                            }

                        scope.launch {
                            routeDao.updateRouteWithDates(
                                number = route.number,
                                name = route.name,
                                difficulty = route.difficulty,
                                status = newStatus.name,
                                statusChangedAt = now,
                                completedDate = completedDate
                            )

                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                        }
                    },
                    onEdit = {
                        openRoute(route)
                    }
                )
            }
        }
    }

    if (showSingleRouteDialog) {
        SingleRouteDialog(
            context = context,
            title = if (editingRouteNumber == null) {
                "Route hinzufügen"
            } else {
                "Route bearbeiten"
            },
            name = routeName,
            difficulty = routeDifficulty,
            status = routeStatus,
            statusChangedAt = routeStatusChangedAt,
            completedDate = routeCompletedDate,
            onNameChanged = {
                routeName = it
            },
            onDifficultyChanged = {
                routeDifficulty = it
            },
            onStatusChanged = {
                routeStatus = it
            },
            onCompletedDateChanged = {
                routeCompletedDate = it
            },
            onDelete = if (editingRouteNumber != null) {
                {
                    showDeleteDialog = true
                }
            } else {
                null
            },
            onDismiss = {
                showSingleRouteDialog = false
                editingRouteNumber = null
            },
            onSave = {
                if (routeName.isNotBlank()) {
                    scope.launch {
                        val number = editingRouteNumber
                        val now = System.currentTimeMillis()

                        val existingRoute = routes.firstOrNull {
                            it.number == number
                        }

                        val statusChangedAt =
                            existingRoute?.statusChangedAt
                                ?: routeStatusChangedAt
                                ?: now

                        val completedDate =
                            if (
                                routeStatus == RouteStatus.TOP ||
                                routeStatus == RouteStatus.FLASH
                            ) {
                                routeCompletedDate
                            } else {
                                null
                            }

                        if (number == null) {
                            routeDao.insertRoute(
                                RouteEntity(
                                    number = routes.nextRouteNumber(),
                                    name = routeName,
                                    difficulty = routeDifficulty,
                                    status = routeStatus?.name,
                                    statusChangedAt = statusChangedAt,
                                    completedDate = completedDate
                                )
                            )
                        } else {
                            routeDao.updateRouteWithDates(
                                number = number,
                                name = routeName,
                                difficulty = routeDifficulty,
                                status = routeStatus?.name,
                                statusChangedAt = statusChangedAt,
                                completedDate = completedDate
                            )
                        }

                        showSingleRouteDialog = false
                        editingRouteNumber = null
                    }
                }
            }
        )
    }

    if (showBulkEditDialog) {
        BulkEditDialog(
            context = context,
            selectedCount = selectedRouteNumbers.size,
            statusEnabled = bulkStatusEnabled,
            status = bulkStatus,
            dateEnabled = bulkDateEnabled,
            date = bulkDate,
            onStatusEnabledChanged = {
                bulkStatusEnabled = it
            },
            onStatusChanged = {
                bulkStatus = it
            },
            onDateEnabledChanged = {
                bulkDateEnabled = it
            },
            onDateChanged = {
                bulkDate = it
            },
            onDismiss = {
                showBulkEditDialog = false
            },
            onSave = {
                scope.launch {
                    val now = System.currentTimeMillis()

                    routes
                        .filter {
                            it.number in selectedRouteNumbers
                        }
                        .forEach { route ->

                            val status =
                                if (bulkStatusEnabled) {
                                    bulkStatus?.name
                                } else {
                                    route.status
                                }

                            val completedDate =
                                if (bulkDateEnabled) {
                                    bulkDate
                                } else {
                                    route.completedDate
                                }

                            val statusChangedAt =
                                if (bulkStatusEnabled) {
                                    now
                                } else {
                                    route.statusChangedAt
                                }

                            routeDao.updateRouteWithDates(
                                number = route.number,
                                name = route.name,
                                difficulty = route.difficulty,
                                status = status,
                                statusChangedAt = statusChangedAt,
                                completedDate = completedDate
                            )
                        }

                    selectedRouteNumbers = emptySet()
                    selectionMode = false
                    showBulkEditDialog = false
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Route löschen?")
            },
            text = {
                Text(
                    "Diese Route wird dauerhaft aus der lokalen Datenbank gelöscht."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val number = editingRouteNumber

                        if (number != null) {
                            routes.firstOrNull {
                                it.number == number
                            }?.let { route ->
                                scope.launch {
                                    routeDao.deleteRoute(route)
                                }
                            }
                        }

                        showDeleteDialog = false
                        showSingleRouteDialog = false
                        editingRouteNumber = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun FilterMenu(
    expanded: Boolean,
    selectedFilters: Set<StatusFilter>,
    allFilters: Set<StatusFilter>,
    onDismiss: () -> Unit,
    onFiltersChanged: (Set<StatusFilter>) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        FilterItem(
            label = "Flash",
            checked = StatusFilter.FLASH in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggleFilter(StatusFilter.FLASH)
            )
        }

        FilterItem(
            label = "Top",
            checked = StatusFilter.TOP in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggleFilter(StatusFilter.TOP)
            )
        }

        FilterItem(
            label = "Zone",
            checked = StatusFilter.ZONE in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggleFilter(StatusFilter.ZONE)
            )
        }

        FilterItem(
            label = "Projekt",
            checked = StatusFilter.PROJECT in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggleFilter(StatusFilter.PROJECT)
            )
        }

        FilterItem(
            label = "Ohne",
            checked = StatusFilter.NONE in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggleFilter(StatusFilter.NONE)
            )
        }

        DropdownMenuItem(
            text = {
                Text("Alle")
            },
            onClick = {
                onFiltersChanged(allFilters)
            }
        )

        DropdownMenuItem(
            text = {
                Text("Keine")
            },
            onClick = {
                onFiltersChanged(emptySet())
            }
        )

        DropdownMenuItem(
            text = {
                Text("Abgeschlossene aus")
            },
            onClick = {
                onFiltersChanged(
                    setOf(
                        StatusFilter.NONE,
                        StatusFilter.ZONE,
                        StatusFilter.PROJECT
                    )
                )
            }
        )
    }
}

@Composable
fun FilterItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(label)
        },
        onClick = onClick,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onClick()
                }
            )
        }
    )
}

@Composable
fun SingleRouteDialog(
    context: Context,
    title: String,
    name: String,
    difficulty: String,
    status: RouteStatus?,
    statusChangedAt: Long?,
    completedDate: Long?,
    onNameChanged: (String) -> Unit,
    onDifficultyChanged: (String) -> Unit,
    onStatusChanged: (RouteStatus?) -> Unit,
    onCompletedDateChanged: (Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = {
                        Text("Name oder Nummer")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = difficulty,
                    onValueChange = onDifficultyChanged,
                    label = {
                        Text("Schwierigkeit")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Status")

                StatusChoice("Kein Status", status == null) {
                    onStatusChanged(null)
                }

                StatusChoice("Flash", status == RouteStatus.FLASH) {
                    onStatusChanged(RouteStatus.FLASH)
                }

                StatusChoice("Top", status == RouteStatus.TOP) {
                    onStatusChanged(RouteStatus.TOP)
                }

                StatusChoice("Zone", status == RouteStatus.ZONE) {
                    onStatusChanged(RouteStatus.ZONE)
                }

                StatusChoice("Projekt", status == RouteStatus.PROJECT) {
                    onStatusChanged(RouteStatus.PROJECT)
                }

                if (statusChangedAt != null) {
                    Text(
                        text = "Status eingetragen: ${
                            formatDateTime(statusChangedAt)
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (
                    status == RouteStatus.TOP ||
                    status == RouteStatus.FLASH
                ) {
                    Text(
                        text = "Erfolgsdatum: ${
                            completedDate?.let {
                                formatDate(it)
                            } ?: "nicht eingetragen"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context = context,
                                currentDate = completedDate,
                                onDateSelected = onCompletedDateChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (completedDate == null) {
                                "Erfolgsdatum wählen"
                            } else {
                                "Erfolgsdatum ändern"
                            }
                        )
                    }

                    if (completedDate != null) {
                        TextButton(
                            onClick = {
                                onCompletedDateChanged(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Erfolgsdatum löschen")
                        }
                    }
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Route löschen")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun BulkEditDialog(
    context: Context,
    selectedCount: Int,
    statusEnabled: Boolean,
    status: RouteStatus?,
    dateEnabled: Boolean,
    date: Long?,
    onStatusEnabledChanged: (Boolean) -> Unit,
    onStatusChanged: (RouteStatus?) -> Unit,
    onDateEnabledChanged: (Boolean) -> Unit,
    onDateChanged: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("$selectedCount Routen bearbeiten")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = statusEnabled,
                        onCheckedChange = onStatusEnabledChanged
                    )

                    Text("Status ändern")
                }

                if (statusEnabled) {
                    StatusChoice("Kein Status", status == null) {
                        onStatusChanged(null)
                    }

                    StatusChoice("Flash", status == RouteStatus.FLASH) {
                        onStatusChanged(RouteStatus.FLASH)
                    }

                    StatusChoice("Top", status == RouteStatus.TOP) {
                        onStatusChanged(RouteStatus.TOP)
                    }

                    StatusChoice("Zone", status == RouteStatus.ZONE) {
                        onStatusChanged(RouteStatus.ZONE)
                    }

                    StatusChoice("Projekt", status == RouteStatus.PROJECT) {
                        onStatusChanged(RouteStatus.PROJECT)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dateEnabled,
                        onCheckedChange = onDateEnabledChanged
                    )

                    Text("Erfolgsdatum ändern")
                }

                if (dateEnabled) {
                    Text(
                        text = "Datum: ${
                            date?.let {
                                formatDate(it)
                            } ?: "nicht gesetzt"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context = context,
                                currentDate = date,
                                onDateSelected = onDateChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Datum auswählen")
                    }

                    if (date != null) {
                        TextButton(
                            onClick = {
                                onDateChanged(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Datum löschen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Auf $selectedCount anwenden")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun StatusChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }
    }
}

@Composable
fun CalculationBar(
    topCount: Int,
    flashCount: Int,
    zoneCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Text(
            text = "Top: $topCount ($flashCount Flash) · Zone: $zoneCount",
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 8.dp
            )
        )
    }
}

@Composable
fun RouteTableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSort: (SortColumn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortHeader(
            "Route",
            SortColumn.ROUTE,
            sortColumn,
            sortAscending,
            Modifier.weight(1.15f),
            onSort
        )

        SortHeader(
            "Flash",
            SortColumn.FLASH,
            sortColumn,
            sortAscending,
            Modifier.weight(1f),
            onSort
        )

        SortHeader(
            "Top",
            SortColumn.TOP,
            sortColumn,
            sortAscending,
            Modifier.weight(1f),
            onSort
        )

        SortHeader(
            "Zone",
            SortColumn.ZONE,
            sortColumn,
            sortAscending,
            Modifier.weight(1f),
            onSort
        )

        SortHeader(
            "Projekt",
            SortColumn.PROJECT,
            sortColumn,
            sortAscending,
            Modifier.weight(1f),
            onSort
        )
    }
}

@Composable
fun SortHeader(
    text: String,
    column: SortColumn,
    current: SortColumn,
    ascending: Boolean,
    modifier: Modifier,
    onClick: (SortColumn) -> Unit
) {
    TextButton(
        onClick = {
            onClick(column)
        },
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        val arrow = if (column == current) {
            if (ascending) " ↑" else " ↓"
        } else {
            ""
        }

        Text(
            text = text + arrow,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RouteRow(
    route: RouteEntity,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit
) {
    var rowProgress by remember(route.number) {
        mutableStateOf(0f)
    }

    val background =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            onSelectedChange()
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .pointerInput(route.number, selectionMode) {
                            detectTapGestures(
                                onTap = {
                                    if (selectionMode) {
                                        onSelectedChange()
                                    }
                                },
                                onLongPress = {
                                    if (!selectionMode) {
                                        onEdit()
                                    }
                                }
                            )
                        }
                ) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (route.difficulty.isNotBlank()) {
                        Text(
                            text = route.difficulty,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                StatusButton(
                    "Flash",
                    route.status == "FLASH",
                    Modifier.weight(1f),
                    { rowProgress = it },
                    {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.FLASH)
                    }
                )

                StatusButton(
                    "Top",
                    route.status == "TOP",
                    Modifier.weight(1f),
                    { rowProgress = it },
                    {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.TOP)
                    }
                )

                StatusButton(
                    "Zone",
                    route.status == "ZONE",
                    Modifier.weight(1f),
                    { rowProgress = it },
                    {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.ZONE)
                    }
                )

                StatusButton(
                    "Projekt",
                    route.status == "PROJECT",
                    Modifier.weight(1f),
                    { rowProgress = it },
                    {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.PROJECT)
                    }
                )
            }
        }

        if (rowProgress > 0f) {
            BorderProgress(
                progress = rowProgress,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit
) {
    val progress = remember {
        Animatable(0f)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        } else {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(label) {
                    detectTapGestures(
                        onPress = {
                            coroutineScope {
                                var completed = false

                                progress.snapTo(0f)
                                onProgress(0f)

                                val animationJob = launch {
                                    progress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(1500)
                                    ) {
                                        onProgress(value)
                                    }

                                    completed = true
                                }

                                val released = tryAwaitRelease()

                                if (completed && released) {
                                    onComplete()
                                    progress.snapTo(0f)
                                } else {
                                    animationJob.cancel()
                                    progress.snapTo(0f)
                                    onProgress(0f)
                                }
                            }
                        }
                    )
                }
        )
    }
}

@Composable
fun BorderProgress(
    progress: Float,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
    ) {
        val stroke = 4.dp.toPx()
        val radius = 12.dp.toPx()
        val path = AndroidPath()

        path.addRoundRect(
            RectF(
                stroke / 2f,
                stroke / 2f,
                size.width - stroke / 2f,
                size.height - stroke / 2f
            ),
            radius,
            radius,
            AndroidPath.Direction.CW
        )

        val measure = PathMeasure(path, false)
        val progressPath = AndroidPath()

        measure.getSegment(
            0f,
            measure.length * progress.coerceIn(0f, 1f),
            progressPath,
            true
        )

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = stroke
            this.color = color.toArgb()
        }

        drawIntoCanvas {
            it.nativeCanvas.drawPath(progressPath, paint)
        }
    }
}

fun routeFilter(status: String?): StatusFilter? {
    return when (status) {
        null -> StatusFilter.NONE
        "FLASH" -> StatusFilter.FLASH
        "TOP" -> StatusFilter.TOP
        "ZONE" -> StatusFilter.ZONE
        "PROJECT" -> StatusFilter.PROJECT
        else -> null
    }
}

fun routeComparator(
    column: SortColumn,
    ascending: Boolean
): Comparator<RouteEntity> {
    val comparator = when (column) {
        SortColumn.ROUTE -> compareBy<RouteEntity> {
            it.number
        }

        SortColumn.FLASH -> compareByDescending<RouteEntity> {
            it.status == "FLASH"
        }.thenBy {
            it.number
        }

        SortColumn.TOP -> compareByDescending<RouteEntity> {
            it.status == "TOP"
        }.thenBy {
            it.number
        }

        SortColumn.ZONE -> compareByDescending<RouteEntity> {
            it.status == "ZONE"
        }.thenBy {
            it.number
        }

        SortColumn.PROJECT -> compareByDescending<RouteEntity> {
            it.status == "PROJECT"
        }.thenBy {
            it.number
        }
    }

    return if (ascending) comparator else comparator.reversed()
}

fun Set<StatusFilter>.toggleFilter(
    filter: StatusFilter
): Set<StatusFilter> {
    return if (filter in this) {
        this - filter
    } else {
        this + filter
    }
}

fun List<RouteEntity>.nextRouteNumber(): Int {
    return if (isEmpty()) {
        1
    } else {
        maxOf {
            it.number
        } + 1
    }
}

fun String?.toRouteStatus(): RouteStatus? {
    return this?.let {
        runCatching {
            RouteStatus.valueOf(it)
        }.getOrNull()
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy, HH:mm",
                Locale.getDefault()
    ).format(Date(timestamp))
}

fun showDatePicker(
    context: Context,
    currentDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val calendar = Calendar.getInstance()

    if (currentDate != null) {
        calendar.timeInMillis = currentDate
    }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            onDateSelected(selectedDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
