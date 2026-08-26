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

enum class RouteStatus { FLASH, TOP, ZONE, PROJECT }

enum class StatusFilter { NONE, FLASH, TOP, ZONE, PROJECT }

enum class SortColumn { ROUTE, FLASH, TOP, ZONE, PROJECT }

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
    val database = remember { TicklistDatabase.getDatabase(context) }
    val routeDao = database.routeDao()
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val routes by routeDao.observeAllRoutes().collectAsState(initial = emptyList())

    val allFilters = setOf(
        StatusFilter.NONE,
        StatusFilter.FLASH,
        StatusFilter.TOP,
        StatusFilter.ZONE,
        StatusFilter.PROJECT
    )

    var selectedFilters by remember { mutableStateOf(allFilters) }
    var sortColumn by remember { mutableStateOf(SortColumn.ROUTE) }
    var sortAscending by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showRouteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var editingRouteNumber by remember { mutableStateOf<Int?>(null) }
    var routeName by remember { mutableStateOf("") }
    var routeDifficulty by remember { mutableStateOf("") }
    var routeStatus by remember { mutableStateOf<RouteStatus?>(null) }
    var routeStatusChangedAt by remember { mutableStateOf<Long?>(null) }
    var routeCompletedDate by remember { mutableStateOf<Long?>(null) }

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
        .filter {
            routeFilter(it.status)?.let { filter ->
                filter in selectedFilters
            } == true
        }
        .sortedWith(routeComparator(sortColumn, sortAscending))

    val topCount = routes.count {
        it.status == "TOP" || it.status == "FLASH"
    }
    val flashCount = routes.count { it.status == "FLASH" }
    val zoneCount = routes.count { it.status == "ZONE" }

    fun openRoute(route: RouteEntity) {
        scope.launch {
            val currentRoute = routeDao.getRoute(route.number) ?: route

            editingRouteNumber = currentRoute.number
            routeName = currentRoute.name
            routeDifficulty = currentRoute.difficulty
            routeStatus = currentRoute.status?.let {
                runCatching { RouteStatus.valueOf(it) }.getOrNull()
            }
            routeStatusChangedAt = currentRoute.statusChangedAt
            routeCompletedDate = currentRoute.completedDate
            showRouteDialog = true
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${displayedRoutes.size} von ${routes.size} Routen",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Status 1,5 Sekunden gedrückt halten",
                    style = MaterialTheme.typography.bodySmall
                )

                Box {
                    OutlinedButton(
                        onClick = { showFilterMenu = true },
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 2.dp
                        )
                    ) {
                        Text("Filter", fontSize = 11.sp)
                    }

                    FilterMenu(
                        expanded = showFilterMenu,
                        selectedFilters = selectedFilters,
                        allFilters = allFilters,
                        onDismiss = { showFilterMenu = false },
                        onFiltersChanged = { selectedFilters = it }
                    )
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
                    showRouteDialog = true
                }
            ) {
                Text("+", fontSize = 24.sp)
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
                    onStatusChange = { newStatus ->
                        val now = System.currentTimeMillis()
                        val newCompletedDate =
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
                                completedDate = newCompletedDate
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

    if (showRouteDialog) {
        RouteDialog(
            context = context,
            title = if (editingRouteNumber == null) {
                "Route hinzufügen"
            } else {
                "Route bearbeiten"
            },
            routeName = routeName,
            routeDifficulty = routeDifficulty,
            routeStatus = routeStatus,
            statusChangedAt = routeStatusChangedAt,
            completedDate = routeCompletedDate,
            onNameChange = { routeName = it },
            onDifficultyChange = { routeDifficulty = it },
            onStatusChange = { routeStatus = it },
            onCompletedDateChange = { routeCompletedDate = it },
            onDelete = if (editingRouteNumber != null) {
                { showDeleteDialog = true }
            } else {
                null
            },
            onDismiss = {
                showRouteDialog = false
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

                        val savedStatusChangedAt =
                            existingRoute?.statusChangedAt
                                ?: routeStatusChangedAt
                                ?: now

                        val savedCompletedDate =
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
                                    statusChangedAt = savedStatusChangedAt,
                                    completedDate = savedCompletedDate
                                )
                            )
                        } else {
                            routeDao.updateRouteWithDates(
                                number = number,
                                name = routeName,
                                difficulty = routeDifficulty,
                                status = routeStatus?.name,
                                statusChangedAt = savedStatusChangedAt,
                                completedDate = savedCompletedDate
                            )
                        }

                        showRouteDialog = false
                        editingRouteNumber = null
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Route löschen?") },
            text = {
                Text("Diese Route wird dauerhaft aus der lokalen Datenbank gelöscht.")
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
                        showRouteDialog = false
                        editingRouteNumber = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
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
        FilterCheckboxItem("Flash", StatusFilter.FLASH in selectedFilters) {
            onFiltersChanged(selectedFilters.toggleFilter(StatusFilter.FLASH))
        }

        FilterCheckboxItem("Top", StatusFilter.TOP in selectedFilters) {
            onFiltersChanged(selectedFilters.toggleFilter(StatusFilter.TOP))
        }

        FilterCheckboxItem("Zone", StatusFilter.ZONE in selectedFilters) {
            onFiltersChanged(selectedFilters.toggleFilter(StatusFilter.ZONE))
        }

        FilterCheckboxItem("Projekt", StatusFilter.PROJECT in selectedFilters) {
            onFiltersChanged(selectedFilters.toggleFilter(StatusFilter.PROJECT))
        }

        FilterCheckboxItem("Ohne", StatusFilter.NONE in selectedFilters) {
            onFiltersChanged(selectedFilters.toggleFilter(StatusFilter.NONE))
        }

        DropdownMenuItem(
            text = { Text("Alle") },
            onClick = { onFiltersChanged(allFilters) }
        )

        DropdownMenuItem(
            text = { Text("Keine") },
            onClick = { onFiltersChanged(emptySet()) }
        )

        DropdownMenuItem(
            text = { Text("Geschaffte aus") },
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
fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onCheckedChange,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = { onCheckedChange() }
            )
        }
    )
}

@Composable
fun RouteDialog(
    context: Context,
    title: String,
    routeName: String,
    routeDifficulty: String,
    routeStatus: RouteStatus?,
    statusChangedAt: Long?,
    completedDate: Long?,
    onNameChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onStatusChange: (RouteStatus?) -> Unit,
    onCompletedDateChange: (Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = onNameChange,
                    label = { Text("Name oder Nummer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = routeDifficulty,
                    onValueChange = onDifficultyChange,
                    label = { Text("Schwierigkeit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium
                )

                StatusSelectionButton(
                    "Kein Status",
                    routeStatus == null
                ) {
                    onStatusChange(null)
                }

                StatusSelectionButton(
                    "Flash",
                    routeStatus == RouteStatus.FLASH
                ) {
                    onStatusChange(RouteStatus.FLASH)
                }

                StatusSelectionButton(
                    "Top",
                    routeStatus == RouteStatus.TOP
                ) {
                    onStatusChange(RouteStatus.TOP)
                }

                StatusSelectionButton(
                    "Zone",
                    routeStatus == RouteStatus.ZONE
                ) {
                    onStatusChange(RouteStatus.ZONE)
                }

                StatusSelectionButton(
                    "Projekt",
                    routeStatus == RouteStatus.PROJECT
                ) {
                    onStatusChange(RouteStatus.PROJECT)
                }

                if (statusChangedAt != null) {
                    Text(
                        text = "Status eingetragen: ${formatDateTime(statusChangedAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (
                    routeStatus == RouteStatus.TOP ||
                    routeStatus == RouteStatus.FLASH
                ) {
                    Text(
                        text = "Erfolgsdatum: ${
                            completedDate?.let { formatDate(it) }
                                ?: "nicht eingetragen"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context = context,
                                currentDate = completedDate,
                                onDateSelected = onCompletedDateChange
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
                            onClick = { onCompletedDateChange(null) },
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
            Button(onClick = onSave) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun StatusSelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
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
    currentColumn: SortColumn,
    ascending: Boolean,
    modifier: Modifier,
    onClick: (SortColumn) -> Unit
) {
    TextButton(
        onClick = { onClick(column) },
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        val arrow = if (column == currentColumn) {
            if (ascending) " ↑" else " ↓"
        } else {
            ""
        }

        Text(
            text = text + arrow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@Composable
fun RouteRow(
    route: RouteEntity,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit
) {
    var rowProgress by remember(route.number) {
        mutableStateOf(0f)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
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
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .pointerInput(route.number) {
                            detectTapGestures(
                                onLongPress = { onEdit() }
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
    val progress = remember { Animatable(0f) }

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        val strokeWidth = 4.dp.toPx()
        val radius = 12.dp.toPx()
        val path = AndroidPath()

        path.addRoundRect(
            RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                size.width - strokeWidth / 2f,
                size.height - strokeWidth / 2f
            ),
            radius,
            radius,
            AndroidPath.Direction.CW
        )

        val pathMeasure = PathMeasure(path, false)
        val progressPath = AndroidPath()

        pathMeasure.getSegment(
            0f,
            pathMeasure.length * progress.coerceIn(0f, 1f),
            progressPath,
            true
        )

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = color.toArgb()

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(progressPath, paint)
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
        SortColumn.ROUTE -> compareBy { it.number }

        SortColumn.FLASH -> compareByDescending<RouteEntity> {
            it.status == "FLASH"
        }.thenBy { it.number }

        SortColumn.TOP -> compareByDescending<RouteEntity> {
            it.status == "TOP"
        }.thenBy { it.number }

        SortColumn.ZONE -> compareByDescending<RouteEntity> {
            it.status == "ZONE"
        }.thenBy { it.number }

        SortColumn.PROJECT -> compareByDescending<RouteEntity> {
            it.status == "PROJECT"
        }.thenBy { it.number }
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
    return if (isEmpty()) 1 else maxOf { it.number } + 1
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
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
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
