package com.claritypilot


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    // Dein Permission Launcher (unverändert übernommen)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Hier könntest du auf die Entscheidung reagieren (optional)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val myCustomGreen = Color(0xFF0CB446)

        // 2. Erstelle das Farbschema (wir überschreiben hier die wichtigsten Farben)
        // Wenn du 'secondary' und 'tertiary' nicht setzt, bleiben sie evtl. lila/anders.
        // Um sicherzugehen, dass ALLES grünlich wirkt, setzen wir die Hauptfarben hier gleich.
        val myAppColorScheme = lightColorScheme(
            primary = myCustomGreen,
            onPrimary = Color.White, // Text auf dem grünen Button
            secondary = myCustomGreen, // Sekundäre Elemente auch grün
            tertiary = myCustomGreen,

            // Optional: Damit Hintergründe (wie bei "Connected") einen passenden hellen Ton haben:
            primaryContainer = Color(0xFFDDF5E4), // Ein sehr helles Grün passend zu deinem Ton
            onPrimaryContainer = Color(0xFF00210B) // Dunkler Text auf hellem Container
        )
        // Hier startet die Compose UI mit den zwei Reitern
        // (Ersetzt setContentView(R.layout.widget_initial_layout))
        setContent {
            MaterialTheme(
              //  colorScheme = myAppColorScheme
            ) {
                MainTabScreen()
            }
        }
    }
}

suspend fun loadJsonFromUrl(urlString: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            "{}" // Leeres JSON bei Fehler zurückgeben
        }
    }
}


@Composable
fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp), // Etwas Abstand
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Abstand zwischen Icon und Text
    ) {
        // --- Icon ---
        // Wir verwenden Coil, um das Bild von einer URL zu laden
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.icon) // Fallback-Icon (GitHub)
                .crossfade(true)
                .build(),
            contentDescription = item.type,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)

        )

        // --- Beschreibung ---
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // Nimmt den verfügbaren Platz ein
        )

        // --- Dauer ---
        Text(
            text = item.duration,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Etwas dezenter
        )
    }
}

// Datenklasse für die Tab-Konfiguration
data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

@Composable
fun MainTabScreen() {
    val tabs = listOf(
        TabItem("History", Icons.Filled.History) { ViewScreen() },
        TabItem("Settings", Icons.Filled.Settings) { SettingsScreen() }
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Passen Sie den Scaffold an:
    Scaffold(
        // Die TabRow wird jetzt hier in der bottomBar platziert
        bottomBar = {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) }
                    )
                }
            }
        }
        // Der topBar-Slot bleibt leer
    ) { innerPadding ->
        // Der wischbare Bereich (HorizontalPager)
        // WICHTIG: Das innerPadding vom Scaffold wird jetzt automatisch den
        // Platz für die untere Leiste freihalten.
        HorizontalPager(
            // Hinweis: Der Column-Wrapper ist hier nicht unbedingt nötig.
            // Das padding kann direkt am Pager angewendet werden.
            state = pagerState
        ) { pageIndex ->
            tabs[pageIndex].screen()
        }
    }
}

// --- Beispiel Inhalt für den "View" Screen ---

// DURCH DIESE NEUE VERSION:
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewScreen() {
    val context = LocalContext.current

    // State für die Daten
    var timelineData by remember { mutableStateOf<TimelineData>(emptyMap()) }

    // Polling Logik: Alle 10 Sekunden checken
    LaunchedEffect(Unit) {
        while (true) {
            // 1. Daten im Hintergrund laden
            val jsonString = loadJsonFromUrl("https://clarity-pilot.com/user/1/timeline/android")
            val json = Json { ignoreUnknownKeys = true }
            val newData = if (jsonString.isNotBlank()) {
                try {
                    json.decodeFromString<TimelineData>(jsonString)
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            // 2. "Silent Update": Nur zuweisen, wenn sich wirklich was geändert hat.
            // Kotlin Maps vergleichen Inhalte automatisch korrekt (equals).
            // Dadurch flackert nichts und der Scroll-Status bleibt erhalten.
            if (newData != timelineData) {
                timelineData = newData
            }

            // 3. 10 Sekunden warten
            delay(10_000)
        }
    }

    // Design Konstanten
    val timelineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
            .padding(paddingValues)
    ) {
        if (timelineData.isEmpty()) {
            item {
                // Nur ganz am Anfang (beim allerersten Start) zeigen wir "Loading"
                // Später bei Updates sieht man das hier nicht mehr.
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = Color.Gray)
                }
            }
        } else {
            timelineData.forEach { (day, activities) ->
                stickyHeader {
                    TimelineDateHeader(day = day)
                }

                items(activities.size) { index ->
                    val activity = activities[index]
                    // Prüfen ob es das letzte Item der GRUPPE (des Tages) ist
                    val isLastItemOfDay = index == activities.lastIndex

                    ModernTimelineItem(
                        item = activity,
                        isLast = isLastItemOfDay,
                        lineColor = timelineColor,
                        dotColor = dotColor
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }   }
}

// --- KOMPONENTEN ---

@Composable
fun TimelineDateHeader(day: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = day.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ModernTimelineItem(
    item: ActivityItem,
    isLast: Boolean,
    lineColor: Color,
    dotColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // --- SPALTE 1: Die Timeline (Grafik) ---
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // A. Die Linie
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
            ) {
                // LOGIK-ÄNDERUNG HIER:
                // Start: Immer oben (0f)
                // Ende: Wenn es das letzte Item ist -> Mitte (center.y). Sonst -> Ganz unten (size.height).
                val endY = if (isLast) center.y else size.height

                drawLine(
                    color = lineColor,
                    start = Offset(x = center.x, y = 0f),
                    end = Offset(x = center.x, y = endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // B. Der Punkt (Knoten)
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surface,
               // border = androidx.compose.foundation.BorderStroke(2.dp, dotColor),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(32.dp),
                //shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.icon)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(item.iconSize.dp),
                        //contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // --- SPALTE 2: Der Inhalt (Karte) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp, end = 8.dp)
        ) {
            ElevatedCard(
                onClick = { /* Optional */ },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = item.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
// DIESE FUNKTION HINZUFÜGEN:
fun loadJsonFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        "{}" // Leeres JSON bei Fehler zurückgeben
    }
}

// --- Beispiel Inhalt für den "Settings" Screen ---
data class AccountOption(
    val id: String,
    val name: String,
    val icon: Int, // In der Praxis hier eher 'resId: Int' verwenden
    val color: Color, // Markenfarbe für den visuellen Touch
    val initialConnected: Boolean
)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    // ... (STATE LOGIK BLEIBT GLEICH WIE VORHER) ...
    // Kopiere hier deine State-Variablen (accounts, name, gender, birthDateMillis, etc.) rein
    // Ich kürze das hier ab, da sich an der Logik nichts geändert hat.
    val accounts = remember {
        mutableStateListOf(
            AccountOption("github", "GitHub", R.drawable.github, Color(0xFF333333), prefs.getBoolean("account_github", true)),
            AccountOption("teams", "Microsoft Teams", R.drawable.teams, Color(0xFFDB4437), prefs.getBoolean("account_teams", false)),
            AccountOption("jira", "Jira", R.drawable.jira, Color(0xFFDB4437), prefs.getBoolean("account_jira", false)),
            AccountOption("slack", "Slack", R.drawable.slack, Color(0xFF4A154B), prefs.getBoolean("account_slack", false)),
            AccountOption("googlefit", "Google Fit", R.drawable.googlefit, Color(0xFF4A154B), prefs.getBoolean("account_googlefit", false))
        )
    }
    var name by remember { mutableStateOf(prefs.getString("profile_name", "Alex Doe") ?: "Alex Doe") }
    var gender by remember { mutableStateOf(prefs.getString("profile_gender", "Male") ?: "Male") }
    var birthDateMillis by remember { mutableStateOf(if (prefs.getLong("profile_birthdate", -1L) == -1L) null else prefs.getLong("profile_birthdate", -1L)) }
    var weight by remember { mutableDoubleStateOf(prefs.getFloat("profile_weight", 65.5f).toDouble()) }
    var height by remember { mutableIntStateOf(prefs.getInt("profile_height", 172)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showWeightPicker by remember { mutableStateOf(false) }
    var showHeightPicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val birthDateString = if (birthDateMillis != null) dateFormatter.format(Date(birthDateMillis!!)) else ""


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- LINKED ACCOUNTS ---
            item {
                SectionHeader(title = "Linked Accounts")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    accounts.forEach { account ->
                        AccountItem(
                            account = account,
                            onToggle = {
                                val newState = !account.initialConnected
                                val index = accounts.indexOf(account)
                                accounts[index] = account.copy(initialConnected = newState)
                                prefs.edit().putBoolean("account_${account.id}", newState).apply()
                            }
                        )
                    }
                }
            }

            // --- MEDICAL PROFILE ---
            item {
                SectionHeader(title = "Medical Profile")
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                        ModernTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                prefs.edit().putString("profile_name", newName).apply()
                            },
                            label = "Full Name",
                            icon = Icons.Outlined.Person
                        )

                        GenderSelection(
                            selectedGender = gender,
                            onGenderSelected = { newGender ->
                                gender = newGender
                                prefs.edit().putString("profile_gender", newGender).apply()
                            }
                        )

                        // Hier Accessibility verbessert: onClickLabel
                        ClickableTextField(
                            value = birthDateString,
                            label = "Date of Birth",
                            icon = Icons.Outlined.DateRange,
                            placeholder = "Select Date",
                            onClick = { showDatePicker = true },
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                ClickableTextField(
                                    value = String.format("%.1f", weight),
                                    label = "Weight",
                                    suffix = "kg",
                                    onClick = { showWeightPicker = true },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ClickableTextField(
                                    value = "$height",
                                    label = "Height",
                                    suffix = "cm",
                                    onClick = { showHeightPicker = true },
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }

    // ... (HIER FOLGEN DIE GLEICHEN DIALOGE/SHEETS WIE VORHER) ...
    // ... showDatePicker, showHeightPicker, showWeightPicker Code hier einfügen ...
    // (Der Dialog-Code war bereits gut, da native Komponenten meist accessible sind)

    // Kurzer Platzhalter für die Dialoge der Vollständigkeit halber, damit der Code kompiliert:
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = birthDateMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { birthDateMillis = datePickerState.selectedDateMillis; if(birthDateMillis != null) prefs.edit().putLong("profile_birthdate", birthDateMillis!!).apply(); showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showHeightPicker) {
        ModalBottomSheet(onDismissRequest = { showHeightPicker = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
                Text("Select Height", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    NativeWheelPicker(value = height, min = 50, max = 250, onValueChange = { height = it; prefs.edit().putInt("profile_height", it).apply() })
                    Text("cm", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
                }
                Button(onClick = { showHeightPicker = false }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Done") }
            }
        }
    }

    if (showWeightPicker) {
        ModalBottomSheet(onDismissRequest = { showWeightPicker = false }) {
            // ... Weight Picker Code wie vorher ...
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
                Text("Select Weight", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    val wholePart = weight.toInt()
                    NativeWheelPicker(value = wholePart, min = 20, max = 200, onValueChange = { newWhole ->
                        val decimalPart = weight - weight.toInt(); weight = newWhole + decimalPart; prefs.edit().putFloat("profile_weight", weight.toFloat()).apply()
                    })
                    Text(".", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    val decimalPartAsInt = ((weight * 10).toInt() % 10)
                    NativeWheelPicker(value = decimalPartAsInt, min = 0, max = 9, onValueChange = { newDecimal ->
                        val currentWhole = weight.toInt(); weight = currentWhole + (newDecimal / 10.0); prefs.edit().putFloat("profile_weight", weight.toFloat()).apply()
                    })
                    Text("kg", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
                }
                Button(onClick = { showWeightPicker = false }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Done") }
            }
        }
    }
}
// --- HELPER COMPONENTS ---

// Ein Textfeld, das aussieht wie Input, aber auf Klick reagiert (read-only)
@Composable
fun ClickableTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    onClickLabel: String? = null, // Neu: Was passiert beim Klick?
    icon: ImageVector? = null,
    suffix: String? = null,
    placeholder: String? = null
) {
    // InteractionSource für Ripple-Effekt behalten wir
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                onClick()
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = true,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (icon != null) {
            { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        suffix = if (suffix != null) {
            { Text(suffix, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            // WICHTIG: Semantics überschreiben
            .semantics {
                role = Role.Button // Screenreader sagt "Button Date of Birth"
                if (onClickLabel != null) {
                    // Screenreader sagt "Double tap to Change date of birth"
                    this.onClick(label = onClickLabel) {
                        onClick()
                        true
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        interactionSource = interactionSource
    )
}

// Das native Scroll-Rad (Der geheime Trick für gute UX)
@Composable
fun NativeWheelPicker(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    AndroidView(
        modifier = Modifier.width(64.dp),
        factory = { context ->
            // TRICK: Wir zwingen den Picker in ein "Light"-Theme.
            // Das sorgt dafür, dass die Schrift schwarz/dunkel und gut lesbar ist.
            val wrappedContext =
                ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light)

            NumberPicker(wrappedContext).apply {
                minValue = min
                maxValue = max
                this.value = value
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        update = { view ->
            if (view.value != value) {
                view.value = value
            }
        }
    )
}

// --- Helper Composables für UI Konsistenz ---
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() } // WICHTIG: Screenreader springen hierhin
    )
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    suffix: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (icon != null) {
            { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        suffix = if (suffix != null) {
            { Text(suffix, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // Weiche Ecken
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        )
    )
}
@Composable
fun GenderSelection(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(), // Gruppiert die Elemente
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val genders = listOf("Male", "Female", "Other")
            genders.forEach { gender ->
                val isSelected = selectedGender == gender
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(100))
                        .border(1.dp, borderColor, RoundedCornerShape(100))
                        .background(containerColor)
                        // WICHTIG: selectable statt clickable nutzen!
                        .selectable(
                            selected = isSelected,
                            onClick = { onGenderSelected(gender) },
                            role = Role.RadioButton // Screenreader sagt "Radio Button Male, Selected"
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
@Composable
fun AccountItem(
    account: AccountOption,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = account.icon),
                        contentDescription = null, // Dekorativ, da der Name daneben steht
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (account.initialConnected) Color(0xFF4CAF50) else Color.Gray
                        val statusText = if (account.initialConnected) "Connected" else "Disconnected"
                        // Status Punkt für Screenreader ignorieren, Text reicht
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Button Beschriftung für Accessibility verbessern
            val actionVerb = if (account.initialConnected) "Disconnect" else "Login to"
            val a11yDescription = "$actionVerb ${account.name}" // z.B. "Disconnect GitHub"

            if (account.initialConnected) {
                OutlinedButton(
                    onClick = onToggle,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.semantics { contentDescription = a11yDescription }
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.semantics { contentDescription = a11yDescription }
                ) {
                    Text("Connect")
                }
            }
        }
    }
}