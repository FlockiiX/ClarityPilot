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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val myCustomGreen = Color(0xFF0CB446)

        val myAppColorScheme = lightColorScheme(
            primary = myCustomGreen,
            onPrimary = Color.White,
            secondary = myCustomGreen,
            tertiary = myCustomGreen,

            primaryContainer = Color(0xFFDDF5E4),
            onPrimaryContainer = Color(0xFF00210B)
        )
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
            "{}"
        }
    }
}

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

    Scaffold(
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
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState
        ) { pageIndex ->
            tabs[pageIndex].screen()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewScreen() {
    var timelineData by remember { mutableStateOf<TimelineData>(emptyMap()) }
    LaunchedEffect(Unit) {
        while (true) {
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

            if (newData != timelineData) {
                timelineData = newData
            }

            delay(10_000)
        }
    }

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
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
            ) {
                val endY = if (isLast) center.y else size.height

                drawLine(
                    color = lineColor,
                    start = Offset(x = center.x, y = 0f),
                    end = Offset(x = center.x, y = endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(32.dp),
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
                    )
                }
            }
        }

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
fun loadJsonFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        "{}"
    }
}

data class AccountOption(
    val id: String,
    val name: String,
    val icon: Int,
    val color: Color,
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

@Composable
fun ClickableTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    onClickLabel: String? = null,
    icon: ImageVector? = null,
    suffix: String? = null,
    placeholder: String? = null
) {
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
            .semantics {
                role = Role.Button
                if (onClickLabel != null) {
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

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
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
                .selectableGroup(),
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
                        .selectable(
                            selected = isSelected,
                            onClick = { onGenderSelected(gender) },
                            role = Role.RadioButton
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
                        contentDescription = null,
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

            val actionVerb = if (account.initialConnected) "Disconnect" else "Login to"
            val a11yDescription = "$actionVerb ${account.name}"

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