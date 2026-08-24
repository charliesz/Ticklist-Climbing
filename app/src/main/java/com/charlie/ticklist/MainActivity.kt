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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class ClimbingRoute(
    val number: Int,
    val name: String,
    val difficulty: String,
    val status: RouteStatus? = null
)

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
    val routes = remember {
        mutableStateListOf(
            *List(90) { index ->
                val number = index + 1

                ClimbingRoute(
                    number = number,
                    name = String.format("%02d", number),
                    difficulty = "",
                    status = null
                )
            }.toTypedArray()
        )
    }

    var sortMode by remember {
        mutableStateOf(SortMode.NUMBER)
    }

    var filterStatus by remember {
        mutableStateOf<RouteStatus?>(null)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    var showFilterMenu by remember {
        mutableStateOf(false)
    }

    var showRouteDialog by remember {
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

    val displayedRoutes = routes
        .filter { route ->
            filterStatus == null || route.status == filterStatus
        }
        .let { filteredRoutes ->
            when (sortMode) {
                SortMode.NUMBER -> {
                    filteredRoutes.sortedBy { it.number }
                }

                SortMode.STATUS -> {
                    filteredRoutes.sortedWith(
                        compareBy<ClimbingRoute> {
                            statusOrder(it.status)
                        }.thenBy {
                            it.number
                        }
                    )
                }
            }
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
                Text(
                    text = "${displayedRoutes.size} von ${routes.size} Routen",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Status 2 Sekunden gedrückt halten",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
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
                                text = "Sortierung: ${
                                    if (sortMode == SortMode.NUMBER) {
                                        "Nummer"
                                    } else {
                                        "Status"
                                    }
                                }",
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
                                text = "Filter: ${
                                    filterStatus?.let { statusText(it) }
                                        ?: "Alle"
                                }",
                                fontSize = 11.sp
                            )
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = {
                                showFilterMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Alle")
                                },
                                onClick = {
                                    filterStatus = null
                                    showFilterMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Ohne Status")
                                },
                                onClick = {
                                    filterStatus = null
                                    showFilterMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Flash")
                                },
                                onClick = {
                                    filterStatus = RouteStatus.FLASH
                                    showFilterMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Top")
                                },
                                onClick = {
                                    filterStatus = RouteStatus.TOP
                                    showFilterMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Zone")
                                },
                                onClick = {
                                    filterStatus = RouteStatus.ZONE
                                    showFilterMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Projekt")
                                },
                                onClick = {
                                    filterStatus = RouteStatus.PROJECT
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RouteTableHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 6.dp,
                    end = 6.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayedRoutes) { route ->
                    RouteRow(
                        route = route,
                        onStatusChange = { newStatus ->
                            val index = routes.indexOfFirst {
                                it.number == route.number
                            }

                            if (index >= 0) {
                                routes[index] = route.copy(
                                    status = newStatus
                                )
                            }
                        },
                        onEdit = {
                            editingRouteNumber = route.number
                            routeName = route.name
                            routeDifficulty = route.difficulty
                            routeStatus = route.status
                            showRouteDialog = true
                        },
                        onDelete = {
                            routes.removeAll {
                                it.number == route.number
                            }
                        }
                    )
                }
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
            onDismiss = {
                showRouteDialog = false
                editingRouteNumber = null
            },
            onSave = {
                if (routeName.isNotBlank()) {
                    val number = editingRouteNumber

                    if (number == null) {
                        val nextNumber =
                            if (routes.isEmpty()) {
                                1
                            } else {
                                routes.maxOf { it.number } + 1
                            }

                        routes.add(
                            ClimbingRoute(
                                number = nextNumber,
                                name = routeName,
                                difficulty = routeDifficulty,
                                status = routeStatus
                            )
                        )
                    } else {
                        val index = routes.indexOfFirst {
                            it.number == number
                        }

                        if (index >= 0) {
                            routes[index] = routes[index].copy(
                                name = routeName,
                                difficulty = routeDifficulty,
                                status = routeStatus
                            )
                        }
                    }

                    showRouteDialog = false
                    editingRouteNumber = null
                }
            }
        )
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

                Text(
                    text = "Status manuell festlegen",
                    style = MaterialTheme.typography.titleMedium
                )

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
fun RouteTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Route",
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.labelLarge
        )

        HeaderText("Flash")
        HeaderText("Top")
        HeaderText("Zone")
        HeaderText("Projekt")

        Text(
            text = "Optionen",
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HeaderText(text: String) {
    Text(
        text = text,
        modifier = Modifier.width(60.dp),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun RouteRow(
    route: ClimbingRoute,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = 6.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(68.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            StatusButton(
                label = "Flash",
                selected = route.status == RouteStatus.FLASH
            ) {
                onStatusChange(RouteStatus.FLASH)
            }

            StatusButton(
                label = "Top",
                selected = route.status == RouteStatus.TOP
            ) {
                onStatusChange(RouteStatus.TOP)
            }

            StatusButton(
                label = "Zone",
                selected = route.status == RouteStatus.ZONE
            ) {
                onStatusChange(RouteStatus.ZONE)
            }

            StatusButton(
                label = "Projekt",
                selected = route.status == RouteStatus.PROJECT
            ) {
                onStatusChange(RouteStatus.PROJECT)
            }

            Column(
                modifier = Modifier.width(92.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(
                        horizontal = 2.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        text = "Editieren",
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(
                        horizontal = 2.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        text = "Löschen",
                        fontSize = 11.sp
                    )
                }
            }
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
    onClick: () -> Unit
) {
    val progress = remember {
        Animatable(0f)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(44.dp)
            .padding(horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 2.dp,
                    vertical = 0.dp
                ),
                colors = ButtonDefaults.buttonColors()
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
                contentPadding = PaddingValues(
                    horizontal = 2.dp,
                    vertical = 0.dp
                )
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
                                val animationJob = launch {
                                    progress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 2000
                                        )
                                    )
                                }

                                val releasedSuccessfully =
                                    tryAwaitRelease()

                                animationJob.cancel()

                                if (
                                    releasedSuccessfully &&
                                    progress.value >= 0.999f
                                ) {
                                    onClick()
                                }

                                progress.snapTo(0f)
                            }
                        }
                    )
                }
        )

        if (progress.value > 0f) {
            BorderProgress(
                progress = progress.value,
                color = primaryColor
            )
        }
    }
}

@Composable
fun BorderProgress(
    progress: Float,
    color: Color
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 3.dp.toPx()
        val cornerRadius = 12.dp.toPx()

        val borderPath = AndroidPath()

        borderPath.addRoundRect(
            RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                size.width - strokeWidth / 2f,
                size.height - strokeWidth / 2f
            ),
            cornerRadius,
            cornerRadius,
            AndroidPath.Direction.CW
        )

        val pathMeasure = PathMeasure(
            borderPath,
            false
        )

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

fun statusOrder(status: RouteStatus?): Int {
    return when (status) {
        null -> 0
        RouteStatus.PROJECT -> 1
        RouteStatus.ZONE -> 2
        RouteStatus.TOP -> 3
        RouteStatus.FLASH -> 4
    }
}

fun statusText(status: RouteStatus): String {
    return when (status) {
        RouteStatus.FLASH -> "Flash"
        RouteStatus.TOP -> "Top"
        RouteStatus.ZONE -> "Zone"
        RouteStatus.PROJECT -> "Projekt"
    }
}
