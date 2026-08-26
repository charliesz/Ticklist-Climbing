package com.charlie.ticklist

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

enum class RouteStatus {
    FLASH,
    TOP,
    ZONE,
    PROJECT
}

enum class SortMode {
    NUMBER,
    STATUS
}

enum class StatusFilter {
    NONE,
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

    val allFilters = setOf(
        StatusFilter.NONE,
        StatusFilter.FLASH,
        StatusFilter.TOP,
        StatusFilter.ZONE,
        StatusFilter.PROJECT
    )

    var selectedFilters by remember {
        mutableStateOf(allFilters)
    }

    var sortMode by remember {
        mutableStateOf(SortMode.NUMBER)
    }

    var showFilterMenu by remember {
        mutableStateOf(false)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    var showRouteDialog by remember {
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

    LaunchedEffect(Unit) {
        if (routeDao.countRoutes() == 0) {
            routeDao.insertRoutes(
                (1..90).map { number ->
                    RouteEntity(
                        number = number,
                        name = "%02d".format(number),
                        difficulty = "",
                        status = null
                    )
                }
            )
        }
    }

    val displayedRoutes = routes
        .filter { route ->
            val routeFilter = when (route.status) {
                null -> StatusFilter.NONE
                "FLASH" -> StatusFilter.FLASH
                "TOP" -> StatusFilter.TOP
                "ZONE" -> StatusFilter.ZONE
                "PROJECT" -> StatusFilter.PROJECT
                else -> null
            }

            routeFilter != null && routeFilter in selectedFilters
        }
        .let { filteredRoutes ->
            when (sortMode) {
                SortMode.NUMBER -> {
                    filteredRoutes.sortedBy { it.number }
                }

                SortMode.STATUS -> {
                    filteredRoutes.sortedWith(
                        compareBy<RouteEntity> {
                            statusOrder(it.status)
                        }.thenBy {
                            it.number
                        }
                    )
                }
            }
        }

    val topCount = routes.count {
        it.status == "TOP" || it.status == "FLASH"
    }

    val flashCount = routes.count {
        it.status == "FLASH"
    }

    val zoneCount = routes.count {
        it.status == "ZONE"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Text(
                    text = "${displayedRoutes.size} von ${routes.size} Routen",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Status 1,5 Sekunden gedrückt halten",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = {
                                showSortMenu = true
                            },
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text(
                                text = if (sortMode == SortMode.NUMBER) {
                                    "Sortierung: Nummer"
                                } else {
                                    "Sortierung: Status"
                                },
                                fontSize = 11.sp
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = {
                                showSortMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Nach Nummer")
                                },
                                onClick = {
                                    sortMode = SortMode.NUMBER
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Nach Status")
                                },
                                onClick = {
                                    sortMode = SortMode.STATUS
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    Box {
                        OutlinedButton(
                            onClick = {
                                showFilterMenu = true
                            },
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
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
                    showRouteDialog = true
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
                RouteTableHeader()
            }

            items(
                items = displayedRoutes,
                key = { it.number }
            ) { route ->

                RouteRow(
                    route = route,
                    onStatusChange = { newStatus ->
                        scope.launch {
                            routeDao.updateRoute(
                                route.copy(
                                    status = newStatus.name
                                )
                            )

                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                        }
                    },
                    onEdit = {
                        editingRouteNumber = route.number
                        routeName = route.name
                        routeDifficulty = route.difficulty
                        routeStatus = route.status?.let {
                            runCatching {
                                RouteStatus.valueOf(it)
                            }.getOrNull()
                        }
                        showRouteDialog = true
                    }
                )
            }
        }
    }

    if (showRouteDialog) {
        RouteDialog(
            title = if (editingRouteNumber == null) {
                "Route hinzufügen"
            } else {
                "Route bearbeiten"
            },
            routeName = routeName,
            routeDifficulty = routeDifficulty,
            routeStatus = routeStatus,
            onNameChange = {
                routeName = it
            },
            onDifficultyChange = {
                routeDifficulty = it
            },
            onStatusChange = {
                routeStatus = it
            },
            onDelete = if (editingRouteNumber != null) {
                {
                    showDeleteDialog = true
                }
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

                        if (number == null) {
                            val nextNumber =
                                if (routes.isEmpty()) {
                                    1
                                } else {
                                    routes.maxOf {
                                        it.number
                                    } + 1
                                }

                            routeDao.insertRoute(
                                RouteEntity(
                                    number = nextNumber,
                                    name = routeName,
                                    difficulty = routeDifficulty,
                                    status = routeStatus?.name
                                )
                            )
                        } else {
                            routeDao.updateRoute(
                                RouteEntity(
                                    number = number,
                                    name = routeName,
                                    difficulty = routeDifficulty,
                                    status = routeStatus?.name
                                )
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
                            val route = routes.firstOrNull {
                                it.number == number
                            }

                            if (route != null) {
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
        FilterCheckboxItem(
            label = "Flash",
            checked = StatusFilter.FLASH in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggle(StatusFilter.FLASH)
            )
        }

        FilterCheckboxItem(
            label = "Top",
            checked = StatusFilter.TOP in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggle(StatusFilter.TOP)
            )
        }

        FilterCheckboxItem(
            label = "Zone",
            checked = StatusFilter.ZONE in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggle(StatusFilter.ZONE)
            )
        }

        FilterCheckboxItem(
            label = "Projekt",
            checked = StatusFilter.PROJECT in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggle(StatusFilter.PROJECT)
            )
        }

        FilterCheckboxItem(
            label = "Ohne",
            checked = StatusFilter.NONE in selectedFilters
        ) {
            onFiltersChanged(
                selectedFilters.toggle(StatusFilter.NONE)
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
                Text("Geschaffte aus")
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
fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(label)
        },
        onClick = onCheckedChange,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onCheckedChange()
                }
            )
        }
    )
}

fun Set<StatusFilter>.toggle(
    filter: StatusFilter
): Set<StatusFilter> {
    return if (filter in this) {
        this - filter
    } else {
        this + filter
    }
}

@Composable
fun RouteDialog(
    title: String,
    routeName: String,
    routeDifficulty: String,
    routeStatus: RouteStatus?,
    onNameChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onStatusChange: (RouteStatus?) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = onNameChange,
                    label = {
                        Text("Name oder Nummer")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = routeDifficulty,
                    onValueChange = onDifficultyChange,
                    label = {
                        Text("Schwierigkeit")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Status")

                StatusSelectionButton(
                    text = "Kein Status",
                    selected = routeStatus == null
                ) {
                    onStatusChange(null)
                }

                StatusSelectionButton(
                    text = "Flash",
                    selected = routeStatus == RouteStatus.FLASH
                ) {
                    onStatusChange(RouteStatus.FLASH)
                }

                StatusSelectionButton(
                    text = "Top",
                    selected = routeStatus == RouteStatus.TOP
                ) {
                    onStatusChange(RouteStatus.TOP)
                }

                StatusSelectionButton(
                    text = "Zone",
                    selected = routeStatus == RouteStatus.ZONE
                ) {
                    onStatusChange(RouteStatus.ZONE)
                }

                StatusSelectionButton(
                    text = "Projekt",
                    selected = routeStatus == RouteStatus.PROJECT
                ) {
                    onStatusChange(RouteStatus.PROJECT)
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
fun RouteTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Route",
            modifier = Modifier.weight(1.15f),
            style = MaterialTheme.typography.labelLarge
        )

        HeaderText("Flash", Modifier.weight(1f))
        HeaderText("Top", Modifier.weight(1f))
        HeaderText("Zone", Modifier.weight(1f))
        HeaderText("Projekt", Modifier.weight(1f))
    }
}

@Composable
fun HeaderText(
    text: String,
    modifier: Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
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
                                onLongPress = {
                                    onEdit()
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
                    label = "Flash",
                    selected = route.status == "FLASH",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.FLASH)
                    }
                )

                StatusButton(
                    label = "Top",
                    selected = route.status == "TOP",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.TOP)
                    }
                )

                StatusButton(
                    label = "Zone",
                    selected = route.status == "ZONE",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.ZONE)
                    }
                )

                StatusButton(
                    label = "Projekt",
                    selected = route.status == "PROJECT",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
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
                                        animationSpec = tween(
                                            durationMillis = 1500
                                        )
                                    ) {
                                        onProgress(value)
                                    }

                                    completed = true
                                }

                                val released =
                                    tryAwaitRelease()

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
            canvas.nativeCanvas.drawPath(
                progressPath,
                paint
            )
        }
    }
}

fun statusOrder(status: String?): Int {
    return when (status) {
        null -> 0
        "PROJECT" -> 1
        "ZONE" -> 2
        "TOP" -> 3
        "FLASH" -> 4
        else -> 5
    }
}
