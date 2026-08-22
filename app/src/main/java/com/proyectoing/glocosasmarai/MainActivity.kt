package com.proyectoing.glocosasmarai

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import android.content.Context
import com.proyectoing.glocosasmarai.services.ReportGeneratorService
import com.proyectoing.glocosasmarai.services.LocalStorageService
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.chatbot.*
import com.proyectoing.glocosasmarai.models.Medication
import com.proyectoing.glocosasmarai.models.MissedMedicationSummary
import com.proyectoing.glocosasmarai.workers.MealReminderWorker
import com.proyectoing.glocosasmarai.services.MealAlarmReceiver
import com.proyectoing.glocosasmarai.database.entities.ChatMessageEntity
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronRight
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import com.proyectoing.glocosasmarai.services.AuthService
import com.proyectoing.glocosasmarai.services.BackupData
import com.proyectoing.glocosasmarai.services.DriveBackupService
import java.util.concurrent.TimeUnit
import android.net.Uri
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.filled.AddAPhoto

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GlocosaSmartAITheme {
                GlocosaSmartApp()
            }
        }
    }
}

fun scheduleMealAlarm(context: Context, timeString: String, mealType: String) {
    if (timeString.isBlank()) return

    try {
        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, MealAlarmReceiver::class.java).apply {
            putExtra("MEAL_TYPE", mealType)
        }

        val requestCode = mealType.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlocosaSmartApp() {
    val now = System.currentTimeMillis()
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    val authService = remember { AuthService(context) }
    var currentScreen by remember {
        mutableStateOf(if (authService.isLoggedIn) "HOME" else "LOGIN")
    }
    val driveBackupService = remember { DriveBackupService(context) }
    val localStorageService = remember { LocalStorageService(context) }
    val coroutineScope = rememberCoroutineScope()
    var globalChatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var globalServerStatus by remember { mutableStateOf<String?>(null) }
    var globalSavedConversations by remember { mutableStateOf(listOf<SavedConversation>()) }
    var globalCurrentConversationId by remember { mutableStateOf<String?>(null) }
    var globalSavedGlucoseEntries by remember { mutableStateOf(listOf<GlucoseEntry>()) }
    var globalSavedFoodEntries by remember { mutableStateOf(listOf<FoodEntry>()) }
    var globalSavedContacts by remember { mutableStateOf(listOf<EmergencyContact>()) }
    var globalUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var globalSavedMedications by remember { mutableStateOf(listOf<Medication>()) }
    var globalChatPrefill by remember { mutableStateOf<String?>(null) }
    var globalTempFoodDescription by rememberSaveable { mutableStateOf("") }
    var globalTempGlucoseValue by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        localStorageService.initializeDefaultSettings()

        launch {
            localStorageService.getAllGlucoseEntries().collect { entries ->
                globalSavedGlucoseEntries = entries
            }
        }
        launch {
            localStorageService.getAllFoodEntries().collect { entries ->
                globalSavedFoodEntries = entries
            }
        }
        launch {
            localStorageService.getAllEmergencyContacts().collect { contacts ->
                globalSavedContacts = contacts
            }
        }
        launch {
            localStorageService.getAllConversations().collect { conversations ->
                globalSavedConversations = conversations
            }
        }
        launch {
            localStorageService.getAllMedications().collect { medications ->
                globalSavedMedications = medications
            }
        }
        launch {
            localStorageService.getCurrentUserProfile().collect { profile ->
                globalUserProfile = profile
            }
        }
        showSplash = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            val darkPurpleBar = Color(0xFF383854)
            val lightPurpleIndicator = Color(0xFF6B5B8E)
            val iconTextColor = Color.White

            NavigationBar(
                containerColor = darkPurpleBar
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = lightPurpleIndicator,
                    selectedIconColor = iconTextColor,
                    selectedTextColor = iconTextColor,
                    unselectedIconColor = iconTextColor.copy(alpha = 0.7f),
                    unselectedTextColor = iconTextColor.copy(alpha = 0.7f)
                )

                NavigationBarItem(
                    selected = currentScreen == "HOME",
                    onClick = { currentScreen = "HOME" },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_GLUCOSE",
                    onClick = { currentScreen = "ADD_GLUCOSE" },
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                    label = { Text("Glucosa") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_FOOD",
                    onClick = { currentScreen = "ADD_FOOD" },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Comida") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_MEDICATION",
                    onClick = { currentScreen = "ADD_MEDICATION" },
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text("Medicina") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentScreen == "SETTINGS",
                    onClick = { currentScreen = "SETTINGS" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                    colors = itemColors
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSplash) {
                SplashScreen(
                    onSplashComplete = { showSplash = false }
                )
            } else {
                when (currentScreen) {
                    "LOGIN" -> LoginScreen(
                        authService = authService,
                        onLoginSuccess = { currentScreen = "HOME" }
                    )
                    "HOME" -> HomeScreen(
                        chatMessages = globalChatMessages,
                        serverStatus = globalServerStatus,
                        onServerStatusChange = { newState -> globalServerStatus = newState },
                        savedConversations = globalSavedConversations,
                        currentConversationId = globalCurrentConversationId,
                        onChatMessagesChange = { newMessages ->
                            globalChatMessages = newMessages.toList()
                            coroutineScope.launch {
                                if (globalCurrentConversationId != null) {
                                    localStorageService.saveChatMessages(newMessages)
                                }
                            }
                        },
                        onSavedConversationsChange = { newConversations ->
                            val oldConversations = globalSavedConversations
                            globalSavedConversations = newConversations
                            coroutineScope.launch {
                                if (newConversations.isEmpty()) {
                                    // Si la lista llega vacía (porque le diste borrar), eliminamos de la base de datos
                                    oldConversations.forEach { conversation ->
                                        localStorageService.deleteConversation(conversation)
                                    }
                                } else {
                                    // Comportamiento normal para guardar nuevas
                                    newConversations.forEach { conversation ->
                                        localStorageService.saveConversation(conversation)
                                    }
                                }
                            }
                        },
                        onCurrentConversationIdChange = { globalCurrentConversationId = it },
                        glucoseEntries = globalSavedGlucoseEntries,
                        savedMedications = globalSavedMedications,
                        onNavigate = { screenName -> currentScreen = screenName },
                        chatPrefill = globalChatPrefill,
                        onChatPrefillConsumed = { globalChatPrefill = null },
                        currentUserProfile = globalUserProfile
                    )
                    "ADD_GLUCOSE" -> AddGlucoseScreen(
                        savedGlucoseEntries = globalSavedGlucoseEntries,
                        onSavedGlucoseEntriesChange = { newEntries ->
                            val oldList = globalSavedGlucoseEntries
                            globalSavedGlucoseEntries = newEntries
                            coroutineScope.launch {
                                val entryToDelete = oldList.find { oldEntry ->
                                    newEntries.none { newEntry -> newEntry.id == oldEntry.id }
                                }
                                if (entryToDelete != null) {
                                    localStorageService.deleteGlucoseEntry(entryToDelete)
                                }
                                newEntries.forEach { entry ->
                                    localStorageService.saveGlucoseEntry(entry)
                                }
                            }
                        },
                        currentGlucoseValue = globalTempGlucoseValue,
                        onGlucoseValueChange = { globalTempGlucoseValue = it },
                        onAskAIAboutGlucose = { glucoseVal ->
                            globalChatPrefill ="Alimentos para estabilizar hipoglucemia (Mi nivel actual es: $glucoseVal mg/dL)"
                            currentScreen = "HOME"
                        }
                    )
                    "ADD_FOOD" -> AddFoodScreen(
                        savedFoodEntries = globalSavedFoodEntries,
                        onSavedFoodEntriesChange = { newEntries ->
                            val oldList = globalSavedFoodEntries
                            globalSavedFoodEntries = newEntries
                            coroutineScope.launch {
                                val entryToDelete = oldList.find { oldEntry ->
                                    newEntries.none { newEntry -> newEntry.id == oldEntry.id }
                                }
                                if (entryToDelete != null) {
                                    localStorageService.deleteFoodEntry(entryToDelete)
                                }
                                newEntries.forEach { entry ->
                                    localStorageService.saveFoodEntry(entry)
                                }
                            }
                        },
                        onAnalyzeWithAI = { foodDescription ->
                            globalChatPrefill = "Calcula las calorías, carbohidratos (g) y azúcares (g) aproximados de: $foodDescription"
                            currentScreen = "HOME"
                        },
                        currentDescription = globalTempFoodDescription,
                        onDescriptionChange = { globalTempFoodDescription = it }
                    )
                    "ADD_MEDICATION" -> MedicationScreen()

                    "SETTINGS" -> SettingsScreen(
                        savedContacts = globalSavedContacts,
                        authService = authService,
                        onSavedContactsChange = { newContacts ->
                            val oldList = globalSavedContacts
                            globalSavedContacts = newContacts

                            coroutineScope.launch {
                                val contactToDelete = oldList.find { oldContact ->
                                    newContacts.none { newContact -> newContact.id == oldContact.id }
                                }

                                if (contactToDelete != null) {
                                    localStorageService.deleteEmergencyContact(contactToDelete)
                                }

                                newContacts.forEach { contact ->
                                    localStorageService.saveEmergencyContact(contact)
                                }
                            }
                        },
                        savedGlucoseEntries = globalSavedGlucoseEntries,
                        savedFoodEntries = globalSavedFoodEntries,
                        savedMedications = globalSavedMedications,
                        currentUserProfile = globalUserProfile,
                        onUserProfileChange = { newProfile ->
                            globalUserProfile = newProfile
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    chatMessages: List<ChatMessage>,
    serverStatus: String?,
    onServerStatusChange: (String) -> Unit,
    savedConversations: List<SavedConversation>,
    currentConversationId: String?,
    onChatMessagesChange: (List<ChatMessage>) -> Unit,
    onSavedConversationsChange: (List<SavedConversation>) -> Unit,
    onCurrentConversationIdChange: (String?) -> Unit,
    glucoseEntries: List<GlucoseEntry> = emptyList(),
    savedMedications: List<Medication>,
    onNavigate: (String) -> Unit,
    chatPrefill: String?,
    onChatPrefillConsumed: () -> Unit,
    currentUserProfile: UserProfile? = null
) {
    var showChatHistory by rememberSaveable { mutableStateOf(false) }
    var userInput by rememberSaveable { mutableStateOf("") }
    var currentTip by rememberSaveable { mutableStateOf(getRandomHealthTip()) }
    var recentMissedMedications by remember { mutableStateOf<List<MissedMedicationSummary>>(emptyList()) }

    val context = LocalContext.current
    val isVerifying = serverStatus == null || serverStatus == "Verificando..."
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatPrefill) {
        if (chatPrefill != null) {
            userInput = chatPrefill
            onChatPrefillConsumed()
        }
    }

    LaunchedEffect(Unit) {
        if (serverStatus == null) {
            onServerStatusChange("Verificando...")
            try {
                ChatbotFunctions.getChatbotResponse(context, "ping", emptyList(), null)
                onServerStatusChange("Conectado")
            } catch (e: Exception) {
                onServerStatusChange("Sin conexion - Modo Offline")
            }
        }
    }

    LaunchedEffect(chatMessages) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    fun sendMessage() {
        if (userInput.isNotBlank()) {
            val userMessage = ChatMessage(userInput, true, System.currentTimeMillis())
            val messagesWithQuestion = chatMessages + userMessage
            onChatMessagesChange(messagesWithQuestion)

            val currentInput = userInput
            userInput = ""

            coroutineScope.launch {
                delay(150)

                var botMessage: ChatMessage
                var finalChatMessages: List<ChatMessage>

                if (serverStatus != "Conectado") {
                    val fullName = currentUserProfile?.name?.trim()
                    val firstName = fullName?.split(" ")?.firstOrNull()?.ifBlank { null }

                    val errorMessage = if (firstName != null) {
                        "¡Hola $firstName soy tu asistente personal GlucosaSmart IA! No tienes conexion al servidor intentalo mas tarde"
                    } else {
                        "¡Hola soy tu asistente personal GlucosaSmart IA! No tienes conexion al servidor intentalo mas tarde"
                    }
                    botMessage = ChatMessage(errorMessage, false, System.currentTimeMillis())
                } else {
                    val greetings = listOf(
                        "hola", "hola chat", "hola buenos dias", "hola buenas tardes",
                        "hola buenas noches", "buenos dias", "buenas tardes", "buenas noches"
                    )
                    val normalizedInput = currentInput.trim().lowercase()

                    if (normalizedInput in greetings) {
                        val fullName = currentUserProfile?.name?.trim()
                        val firstName = fullName?.split(" ")?.firstOrNull()?.ifBlank { null }

                        val greetingText = if (firstName != null) {
                            "¡Hola $firstName soy tu asistente personal GlucosaSmart IA! ¿Tienes alguna pregunta sobre tu glucosa o dieta?"
                        } else {
                            "¡Hola soy tu asistente personal GlucosaSmart IA! ¿Tienes alguna pregunta sobre tu glucosa o dieta?"
                        }

                        botMessage = ChatMessage(
                            text = greetingText,
                            isUser = false,
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        try {
                            val lastGlucoseLevel = glucoseEntries.maxByOrNull { it.timestamp }?.value
                            val botResponse = ChatbotFunctions.getChatbotResponse(
                                context = context,
                                userInput = currentInput,
                                chatMessages = messagesWithQuestion,
                                glucoseLevel = lastGlucoseLevel,
                                missedMedications = recentMissedMedications
                            )

                            botMessage = ChatMessage(botResponse, false, System.currentTimeMillis())
                            onServerStatusChange("Conectado")

                        } catch (e: Exception) {
                            onServerStatusChange("Sin conexion - No puedes utilizar el asistente")

                            val fullName = currentUserProfile?.name?.trim()
                            val firstName = fullName?.split(" ")?.firstOrNull()?.ifBlank { null }

                            val errorMessage = if (firstName != null) {
                                "¡Hola $firstName soy tu asistente personal GlucosaSmart IA! No tienes conexion al servidor intentalo mas tarde"
                            } else {
                                "¡Hola soy tu asistente personal GlucosaSmart IA! No tienes conexion al servidor intentalo mas tarde"
                            }

                            botMessage = ChatMessage(errorMessage, false, System.currentTimeMillis())
                        }
                    }
                }

                finalChatMessages = messagesWithQuestion + botMessage
                onChatMessagesChange(finalChatMessages)

                if (currentConversationId == null) {
                    val newConversationId = System.currentTimeMillis().toString()
                    onCurrentConversationIdChange(newConversationId)
                    val conversationTitle = if (currentInput.length > 30) currentInput.take(30) + "..." else currentInput
                    val newConversation = SavedConversation(newConversationId, conversationTitle, finalChatMessages, System.currentTimeMillis())
                    onSavedConversationsChange(savedConversations + newConversation)
                } else {
                    val updatedConversations = savedConversations.map {
                        if (it.id == currentConversationId) it.copy(messages = finalChatMessages) else it
                    }
                    onSavedConversationsChange(updatedConversations)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GlucosaSmart IA", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bienvenido a tu asistente de diabetes", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (serverStatus != null) {
            item {
                val isConnected = serverStatus == "Conectado"
                val isError = serverStatus!!.startsWith("Sin conexion")

                val containerColor = when {
                    isVerifying -> MaterialTheme.colorScheme.surfaceVariant
                    isError -> MaterialTheme.colorScheme.errorContainer
                    isConnected -> Color(0xFFC8E6C9)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    isVerifying -> MaterialTheme.colorScheme.onSurfaceVariant
                    isError -> MaterialTheme.colorScheme.onErrorContainer
                    isConnected -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val icon = when {
                    isVerifying -> null
                    isError -> Icons.Default.Warning
                    isConnected -> Icons.Default.CheckCircle
                    else -> null
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = contentColor)
                        } else {
                            icon?.let {
                                Icon(imageVector = it, contentDescription = null, tint = contentColor)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = serverStatus!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E0))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Asistente con IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (currentConversationId != null) {
                                Text("Conversación activa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { onChatMessagesChange(emptyList()); onCurrentConversationIdChange(null) }) {
                            Icon(Icons.Default.Add, "Nuevo chat")
                        }
                        IconButton(onClick = { showChatHistory = true }) {
                            Icon(Icons.Default.Menu, "Ver historial")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (chatMessages.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                reverseLayout = true
                            ) {
                                items(
                                    items = chatMessages.reversed(),
                                    key = { message -> "${message.timestamp}_${message.isUser}" }
                                ) { message ->
                                    ChatMessageItem(message = message)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            Text(
                                text = "¡Hola! Soy tu asistente. Escribe una pregunta para comenzar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe tu pregunta...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { sendMessage() }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD0E4FF)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consejo del Día", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { currentTip = getRandomHealthTip() }) {
                            Icon(Icons.Default.Refresh, "Nuevo consejo")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(currentTip, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("ADD_MEDICATION") },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Reloj",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Próximos Recordatorios del Día",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Ver medicamentos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val todayInMillis = today.timeInMillis

                    val activeReminders = savedMedications
                        .filter { it.endDate >= todayInMillis }
                        .sortedWith(compareBy({ it.hour }, { it.minute }))

                    val nextReminders = activeReminders.take(3)
                    val remainingRemindersCount = (activeReminders.size - nextReminders.size).coerceAtLeast(0)

                    if (activeReminders.isEmpty()) {
                        Text(
                            text = "No tienes recordatorios de medicamentos activos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "(Toca para añadir nuevos medicamentos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        nextReminders.forEach { med ->
                            val formattedTime = String.format("%02d:%02d", med.hour, med.minute)
                            val emoji = if (med.type == "Insulina") "💉" else "💊"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "${med.name} (${med.dose})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        if (remainingRemindersCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "y $remainingRemindersCount más...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "(Toca para ver y administrar)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showChatHistory) {
        AlertDialog(
            onDismissRequest = { showChatHistory = false },
            title = { Text("Historial de Conversaciones") },
            text = {
                if (savedConversations.isEmpty()) {
                    Text("No hay conversaciones guardadas aún.")
                } else {
                    LazyColumn {
                        items(savedConversations) { conversation ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    onCurrentConversationIdChange(conversation.id)
                                    onChatMessagesChange(ArrayList(conversation.messages))
                                    showChatHistory = false
                                },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(conversation.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Mensajes: ${conversation.messages.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Fecha: ${formatDate(conversation.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatHistory = false }) { Text("Cerrar") }
            },
            dismissButton = {
                if (savedConversations.isNotEmpty()) {
                    TextButton(onClick = { onSavedConversationsChange(emptyList()); showChatHistory = false }) {
                        Text("Limpiar historial")
                    }
                }
            }
        )
    }
}

@Composable
fun ConnectionStatusBanner(status: String, isVerifying: Boolean) {
    val backgroundColor = when {
        isVerifying -> MaterialTheme.colorScheme.tertiaryContainer
        status.startsWith("Sin conexion") -> MaterialTheme.colorScheme.errorContainer
        else -> Color(0xFFC8E6C9)
    }
    val contentColor = when {
        isVerifying -> MaterialTheme.colorScheme.onTertiaryContainer
        status.startsWith("Sin conexion") -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color(0xFF2E7D32)
    }
    val icon = when {
        isVerifying -> null
        status.startsWith("Sin conexion") -> Icons.Default.Error
        else -> Icons.Default.CheckCircle
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon!!,
                    contentDescription = null,
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGlucoseScreen(
    savedGlucoseEntries: List<GlucoseEntry>,
    onSavedGlucoseEntriesChange: (List<GlucoseEntry>) -> Unit,
    currentGlucoseValue: String,
    onGlucoseValueChange: (String) -> Unit,
    onAskAIAboutGlucose: (String) -> Unit
) {
    var glucoseNotes by rememberSaveable { mutableStateOf("") }
    var isBeforeMeal by rememberSaveable { mutableStateOf(true) }

    val context = LocalContext.current

    var editingEntry by rememberSaveable { mutableStateOf<GlucoseEntry?>(null) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var entryToDelete by rememberSaveable { mutableStateOf<GlucoseEntry?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showEmergencyAlert by rememberSaveable { mutableStateOf(false) }
    var showWarningAlert by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val availableDates = remember(savedGlucoseEntries) {
        savedGlucoseEntries
            .map {
                java.time.Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .toSet()
            .sortedDescending()
    }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    val filteredGlucoseEntries = remember(savedGlucoseEntries, selectedDate) {
        if (selectedDate == null) {
            savedGlucoseEntries
        } else {
            savedGlucoseEntries.filter {
                val entryDate = java.time.Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                entryDate == selectedDate
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Registro de Glucosa",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (currentGlucoseValue.isNotBlank()) {
                        onAskAIAboutGlucose(currentGlucoseValue)
                    } else {
                        Toast.makeText(context, "Escribe tu nivel de glucosa primero", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                enabled = true
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hipoglucemia", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            OutlinedTextField(
                value = currentGlucoseValue,
                onValueChange = onGlucoseValueChange,
                label = { Text("Valor de glucosa (mg/dL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "¿Es antes de la comida?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { isBeforeMeal = true }
                    ) {
                        RadioButton(
                            selected = isBeforeMeal,
                            onClick = { isBeforeMeal = true }
                        )
                        Text("Sí")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { isBeforeMeal = false }
                    ) {
                        RadioButton(
                            selected = !isBeforeMeal,
                            onClick = { isBeforeMeal = false }
                        )
                        Text("No")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = glucoseNotes,
                onValueChange = { glucoseNotes = it },
                label = { Text("Notas adicionales (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentGlucoseValue.isNotBlank()) {
                        val glucoseValueInt = currentGlucoseValue.toIntOrNull() ?: 0

                        val entry = GlucoseEntry(
                            id = System.currentTimeMillis(),
                            value = glucoseValueInt,
                            timestamp = System.currentTimeMillis(),
                            isBeforeMeal = isBeforeMeal,
                            notes = glucoseNotes.ifBlank { null }
                        )

                        when {
                            glucoseValueInt > 250 -> showEmergencyAlert = true
                            glucoseValueInt < 70 -> showWarningAlert = true
                        }

                        val updatedList = savedGlucoseEntries + entry
                        onSavedGlucoseEntriesChange(updatedList)

                        onGlucoseValueChange("")
                        glucoseNotes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Registro")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            var showDayDropdown by remember { mutableStateOf(false) }
            val selectedDayText = remember(selectedDate) {
                when {
                    selectedDate == null -> "Viendo registros de: Todos"
                    selectedDate == today -> "Viendo registros de: Hoy"
                    selectedDate == today.minusDays(1) -> "Viendo registros de: Ayer"
                    else -> "Viendo registros de: ${selectedDate!!.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { showDayDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(selectedDayText)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar día")
                }

                DropdownMenu(
                    expanded = showDayDropdown,
                    onDismissRequest = { showDayDropdown = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = { selectedDate = null; showDayDropdown = false }
                    )
                    availableDates.forEach { date ->
                        val label = when (date) {
                            today -> "Hoy"
                            today.minusDays(1) -> "Ayer"
                            else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { selectedDate = date; showDayDropdown = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Registros Guardados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(filteredGlucoseEntries.reversed()) { entry ->
            val isNormalRange = if (entry.isBeforeMeal) entry.value in 80..130 else entry.value in 80..200
            val backgroundColor = if (isNormalRange) androidx.compose.ui.graphics.Color(0xFFE8F5E8) else androidx.compose.ui.graphics.Color(0xFFFFEBEE)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${entry.value}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isNormalRange) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            )
                            Text(
                                text = "mg/dL",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { editingEntry = entry; showEditDialog = true }) {
                                Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { entryToDelete = entry; showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNormalRange) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (isNormalRange) "EN RANGO" else "FUERA DE RANGO",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (entry.isBeforeMeal) "Antes de comer" else "Después de comer",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!entry.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Notas: ${entry.notes ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = formatDate(entry.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showEditDialog && editingEntry != null) {
        EditGlucoseDialog(
            entry = editingEntry!!,
            onDismiss = { showEditDialog = false; editingEntry = null },
            onSave = { updatedEntry ->
                val updatedList = savedGlucoseEntries.map { if (it.id == updatedEntry.id) updatedEntry else it }
                onSavedGlucoseEntriesChange(updatedList)
                showEditDialog = false; editingEntry = null
            }
        )
    }

    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; entryToDelete = null },
            title = { Text("Eliminar Registro") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedList = savedGlucoseEntries.filter { it.id != entryToDelete!!.id }
                        onSavedGlucoseEntriesChange(updatedList)
                        showDeleteDialog = false; entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; entryToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showEmergencyAlert) {
        AlertDialog(
            onDismissRequest = { showEmergencyAlert = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¡ALERTA DE EMERGENCIA!", color = MaterialTheme.colorScheme.error) },
            text = { Text("Tu nivel de glucosa está muy alto (>250 mg/dL).") },
            confirmButton = { TextButton(onClick = { showEmergencyAlert = false }) { Text("Entendido") } }
        )
    }

    if (showWarningAlert) {
        AlertDialog(
            onDismissRequest = { showWarningAlert = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Advertencia", color = MaterialTheme.colorScheme.error) },
            text = { Text("Tu nivel de glucosa está bajo (<70 mg/dL).") },
            confirmButton = { TextButton(onClick = { showWarningAlert = false }) { Text("Entendido") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGlucoseDialog(
    entry: GlucoseEntry,
    onDismiss: () -> Unit,
    onSave: (GlucoseEntry) -> Unit
) {
    var glucoseValue by rememberSaveable { mutableStateOf(entry.value.toString()) }
    var glucoseNotes by rememberSaveable { mutableStateOf(entry.notes ?: "") }
    var isBeforeMeal by rememberSaveable { mutableStateOf(entry.isBeforeMeal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Editar Registro")
        },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = "¿Es antes de la comida?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = isBeforeMeal,
                                onClick = { isBeforeMeal = true }
                            )
                            Text("Sí")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = !isBeforeMeal,
                                onClick = { isBeforeMeal = false }
                            )
                            Text("No")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = glucoseValue,
                        onValueChange = { glucoseValue = it },
                        label = { Text("Valor de glucosa (mg/dL)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = glucoseNotes,
                        onValueChange = { glucoseNotes = it },
                        label = { Text("Notas adicionales") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (glucoseValue.isNotBlank()) {
                        val updatedEntry = entry.copy(
                            value = glucoseValue.toIntOrNull() ?: entry.value,
                            notes = glucoseNotes,
                            isBeforeMeal = isBeforeMeal
                        )
                        onSave(updatedEntry)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    savedFoodEntries: List<FoodEntry>,
    onSavedFoodEntriesChange: (List<FoodEntry>) -> Unit,
    onAnalyzeWithAI: (String) -> Unit,
    currentDescription: String,
    onDescriptionChange: (String) -> Unit
) {
    var foodCaloriesInput by rememberSaveable { mutableStateOf("") }
    var foodCarbsInput by rememberSaveable { mutableStateOf("") }
    var foodSugarsInput by rememberSaveable { mutableStateOf("") }

    var selectedMealType by rememberSaveable { mutableStateOf("Desayuno") }
    val context = LocalContext.current
    var showEditFoodDialog by rememberSaveable { mutableStateOf(false) }
    var foodEntryToEdit by rememberSaveable { mutableStateOf<FoodEntry?>(null) }

    val today = remember { LocalDate.now() }
    val availableDates = remember(savedFoodEntries) {
        savedFoodEntries
            .map {
                java.time.Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .toSet()
            .sortedDescending()
    }
    var selectedDate by remember(availableDates) {
        mutableStateOf(availableDates.firstOrNull { it == today } ?: availableDates.firstOrNull() ?: today)
    }
    val filteredFoodEntries = remember(savedFoodEntries, selectedDate) {
        savedFoodEntries.filter {
            val entryDate = java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            entryDate == selectedDate
        }
    }

    val dailyCalories = remember(filteredFoodEntries) {
        filteredFoodEntries.sumOf { it.calories ?: 0 }
    }
    val dailyCarbs = remember(filteredFoodEntries) {
        filteredFoodEntries.sumOf { it.carbohydrates ?: 0 }
    }
    val dailySugars = remember(filteredFoodEntries) {
        filteredFoodEntries.sumOf { it.sugars ?: 0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Registro de Comida", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Tipo de Comida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val mealTypes = listOf("Desayuno", "Almuerzo", "Comida", "Aperitivo", "Cena")
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = Color(0xFFFF9800),
                        selectedLabelColor = Color.White
                    )
                    mealTypes.forEach { mealType ->
                        FilterChip(
                            selected = selectedMealType == mealType,
                            onClick = { selectedMealType = mealType },
                            label = { Text(mealType) },
                            colors = chipColors
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = currentDescription,
                onValueChange = onDescriptionChange,
                label = { Text("Descripción de la comida") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (currentDescription.isNotBlank()) {
                        onAnalyzeWithAI(currentDescription)
                    } else {
                        Toast.makeText(context, "Escribe una descripción primero", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentDescription.isNotBlank()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analizar info nutricional con IA")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = foodCaloriesInput,
                onValueChange = { foodCaloriesInput = it },
                label = { Text("Calorías (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej: 450") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = foodCarbsInput,
                    onValueChange = { foodCarbsInput = it },
                    label = { Text("Carbohidratos (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = foodSugarsInput,
                    onValueChange = { foodSugarsInput = it },
                    label = { Text("Azúcar (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentDescription.isNotBlank()) {
                        val calInt = foodCaloriesInput.toIntOrNull()
                        val carbsInt = foodCarbsInput.toIntOrNull()
                        val sugarsInt = foodSugarsInput.toIntOrNull()
                        val hasData = calInt != null || carbsInt != null || sugarsInt != null

                        if (hasData) {
                            val entry = FoodEntry(
                                id = System.currentTimeMillis(),
                                type = selectedMealType,
                                description = currentDescription,
                                timestamp = getTimestampForSelectedDate(selectedDate, today),
                                calories = calInt,
                                carbohydrates = carbsInt,
                                sugars = sugarsInt,
                                notes = null
                            )
                            onSavedFoodEntriesChange(savedFoodEntries + entry)

                            onDescriptionChange("")
                            foodCaloriesInput = ""
                            foodCarbsInput = ""
                            foodSugarsInput = ""
                        } else {
                            Toast.makeText(context, "Debes ingresar al menos Calorías, Carbohidratos o Azúcar", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Escribe una descripción de la comida", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Registro")
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            var showDayDropdown by remember { mutableStateOf(false) }
            val selectedDayText = remember(selectedDate) {
                when (selectedDate) {
                    today -> "Viendo registros de: Hoy"
                    today.minusDays(1) -> "Viendo registros de: Ayer"
                    else -> "Viendo registros de: ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}"
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = { showDayDropdown = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text(selectedDayText); Icon(Icons.Default.ArrowDropDown, "Seleccionar día")
                }
                DropdownMenu(expanded = showDayDropdown, onDismissRequest = { showDayDropdown = false }, modifier = Modifier.fillMaxWidth()) {
                    if (availableDates.isEmpty()) {
                        DropdownMenuItem(text = { Text("Hoy (Sin registros)") }, onClick = { selectedDate = today; showDayDropdown = false })
                    } else {
                        availableDates.forEach { date ->
                            val label = when (date) { today -> "Hoy"; today.minusDays(1) -> "Ayer"; else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedDate = date; showDayDropdown = false })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (availableDates.isNotEmpty()) {
            item {
                Text("Registros Guardados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFd7e7fd)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            val title = when (selectedDate) {
                                today -> "Resumen total Nutricional (Hoy)"
                                today.minusDays(1) -> "Resumen total Nutricional (Ayer)"
                                else -> "Resumen total del ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM"))}"
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Calorías", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$dailyCalories",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("kcal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.Gray.copy(alpha = 0.3f)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Carbs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${dailyCarbs}g",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text("gramos", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.Gray.copy(alpha = 0.3f)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Azúcares", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${dailySugars}g",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                                Text("gramos", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(filteredFoodEntries.reversed()) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = entry.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { foodEntryToEdit = entry; showEditFoodDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { onSavedFoodEntriesChange(savedFoodEntries.filter { it.id != entry.id }) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calorías", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${entry.calories ?: 0}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("kcal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${entry.carbohydrates ?: 0}g",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text("gramos", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Azúcares", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${entry.sugars ?: 0}g",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Text("gramos", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = formatDate(entry.timestamp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditFoodDialog && foodEntryToEdit != null) {
        EditFoodDialog(
            entry = foodEntryToEdit!!,
            onDismiss = { showEditFoodDialog = false; foodEntryToEdit = null },
            onSave = { updatedEntry ->
                val updatedList = savedFoodEntries.map { if (it.id == updatedEntry.id) updatedEntry else it }
                onSavedFoodEntriesChange(updatedList)
                showEditFoodDialog = false; foodEntryToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    savedContacts: List<EmergencyContact>,
    onSavedContactsChange: (List<EmergencyContact>) -> Unit,
    savedGlucoseEntries: List<GlucoseEntry> = emptyList(),
    savedFoodEntries: List<FoodEntry> = emptyList(),
    savedMedications: List<Medication> = emptyList(),
    currentUserProfile: UserProfile? = null,
    onUserProfileChange: (UserProfile?) -> Unit = {},
    authService: AuthService? = null
) {
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactPhone by rememberSaveable { mutableStateOf("") }
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }
    var breakfastTime by rememberSaveable { mutableStateOf("08:00") }
    var lunchTime by rememberSaveable { mutableStateOf("14:00") }
    var dinnerTime by rememberSaveable { mutableStateOf("20:00") }
    val context = LocalContext.current
    val driveBackupService = remember { DriveBackupService(context) }

    var showMonthDropdown by remember { mutableStateOf(false) }
    var selectedReportMonthText by rememberSaveable { mutableStateOf<String?>("Seleccionar Mes") }

    var patientName by rememberSaveable { mutableStateOf("") }
    var patientAge by rememberSaveable { mutableStateOf("") }
    var patientWeight by rememberSaveable { mutableStateOf(currentUserProfile?.weight?.toString().orEmpty()) }
    var patientHeight by rememberSaveable { mutableStateOf(currentUserProfile?.height?.toString().orEmpty()) }
    var patientDiabetesType by rememberSaveable { mutableStateOf("Tipo 2") }
    var showPatientProfileDialog by rememberSaveable { mutableStateOf(false) }

    //val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStorageService = remember { LocalStorageService(context) }
    var isGeneratingReport by rememberSaveable { mutableStateOf(false) }
    var showReportSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var generatedReportFile by rememberSaveable { mutableStateOf<File?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Debes dar permiso para recibir recordatorios", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(showPatientProfileDialog, currentUserProfile) {
        if (currentUserProfile != null) {
            patientName = currentUserProfile.name ?: ""
            patientAge = currentUserProfile.age?.toString() ?: ""
            patientDiabetesType = currentUserProfile.diabetesType ?: "Tipo 2"
        }
    }

    LaunchedEffect(Unit) {
        breakfastTime = localStorageService.getMealTime("breakfast")
        lunchTime = localStorageService.getMealTime("lunch")
        dinnerTime = localStorageService.getMealTime("dinner")

        if (breakfastTime.isNotEmpty()) scheduleMealAlarm(context, breakfastTime, "Desayuno")
        if (lunchTime.isNotEmpty()) scheduleMealAlarm(context, lunchTime, "Comida")
        if (dinnerTime.isNotEmpty()) scheduleMealAlarm(context, dinnerTime, "Cena")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBDBDBD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            val cardColor = if (currentUserProfile?.name.isNullOrEmpty()) {
                Color(0xFFFFEBEE)
            } else {
                Color(0xFFE8F5E8)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    val buttonText = if (currentUserProfile?.name.isNullOrEmpty()) {
                        "Registrar datos"
                    } else {
                        "Editar"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Perfil del Paciente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showPatientProfileDialog = true }) {
                            Text(buttonText)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- FOTO DE PERFIL ---
                    val googlePhotoUrl = authService?.currentUser?.photoUrl?.toString()
                    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri -> selectedImageUri = uri }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            // Foto: primero intenta foto local, luego Google, luego inicial
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedImageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentScale = ContentScale.Crop
                                )
                            } else if (googlePhotoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(googlePhotoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de Google",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Sin foto — círculo con inicial del nombre
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUserProfile?.name
                                            ?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Ícono de cámara encima
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Cambiar foto",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Datos del perfil al lado de la foto
                        Column {
                            Text("Nombre: ${currentUserProfile?.name ?: "—"}")
                            Text("Edad: ${currentUserProfile?.age?.toString() ?: "—"}")
                            Text("Peso: ${currentUserProfile?.weight?.toString()?.let { "$it kg" } ?: "—"}")
                            Text("Estatura: ${currentUserProfile?.height?.toString()?.let { "$it m" } ?: "—"}")
                        }
                    }
                }
            }

            val contactsCardColor = if (savedContacts.isEmpty()) {
                Color(0xFFFFEBEE)
            } else {
                Color(0xFFFFF9E0)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = contactsCardColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contactos de Emergencia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { showAddContactDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar contacto"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (savedContacts.isEmpty()) {
                        Text(
                            text = "No hay contactos de emergencia configurados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savedContacts.forEach { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = contact.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onSavedContactsChange(savedContacts.filter { it.id != contact.id })
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar contacto",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, null, tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Horarios de Comida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Define tus horas habituales.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    fun updateTime(type: String, newTime: String) {
                        when(type) {
                            "breakfast" -> breakfastTime = newTime
                            "lunch" -> lunchTime = newTime
                            "dinner" -> dinnerTime = newTime
                        }

                        scope.launch {
                            localStorageService.saveMealTime(type, newTime)
                        }

                        val parts = newTime.split(":")
                        val hour = parts[0].toInt()
                        val minute = parts[1].toInt()

                        val now = Calendar.getInstance()
                        val target = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                        }

                        if (target.before(now)) {
                            target.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        val delay = target.timeInMillis - now.timeInMillis

                        val mealNameDisplay = when(type) {
                            "breakfast" -> "Desayuno"
                            "lunch" -> "Comida"
                            "dinner" -> "Cena"
                            else -> "Comida"
                        }

                        val data = workDataOf(
                            "MEAL_TYPE" to type,
                            "MEAL_NAME" to mealNameDisplay
                        )

                        val workRequest = OneTimeWorkRequestBuilder<MealReminderWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .addTag(type)
                            .build()

                        WorkManager.getInstance(context).enqueueUniqueWork(
                            "reminder_$type",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    }

                    fun showTimePicker(currentTime: String, type: String) {
                        val parts = currentTime.split(":")
                        val hour = parts[0].toIntOrNull() ?: 12
                        val minute = parts[1].toIntOrNull() ?: 0

                        android.app.TimePickerDialog(context, { _, h, m ->
                            val formatted = String.format("%02d:%02d", h, m)
                            updateTime(type, formatted)
                        }, hour, minute, true).show()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker(breakfastTime, "breakfast") }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Desayuno", style = MaterialTheme.typography.bodyLarge)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Text(breakfastTime, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker(lunchTime, "lunch") }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Comida", style = MaterialTheme.typography.bodyLarge)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Text(lunchTime, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker(dinnerTime, "dinner") }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cena", style = MaterialTheme.typography.bodyLarge)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Text(dinnerTime, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E0))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = Color(0xFFFFC107)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resultados del IMC",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val profileWeight = currentUserProfile?.weight ?: 0f
                        val profileHeight = currentUserProfile?.height ?: 0f
                        val ageInt = currentUserProfile?.age

                        val hasData = profileWeight > 0f && profileHeight > 0f

                        if (!hasData) {
                            Text(
                                text = "Faltan datos (Peso y Estatura) en tu Perfil para calcular el IMC.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showPatientProfileDialog = true }) {
                                Text("Ir a Registrar Datos")
                            }
                        } else {
                            val (imc, category) = calculateIMC(profileWeight, profileHeight, ageInt)
                            val imcColor = getIMCColor(category)

                            Text(
                                text = "Resultado basado en Perfil:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = imcColor.copy(alpha = 0.1f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "IMC",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "%.1f".format(imc),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = imcColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = imcColor),
                                        modifier = Modifier.align(Alignment.Start)
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFBCAAA4))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF6D4C41)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reportes Mensuales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val availableMonths = getDistinctMonths(savedGlucoseEntries, savedFoodEntries, savedMedications)
                    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

                    if (isGeneratingReport) {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6D4C41),
                                contentColor = Color.White
                            )
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando...")
                        }
                    } else if (availableMonths.isEmpty()) {
                        Text(
                            text = "No hay datos registrados para generar reportes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Button(
                            onClick = { showMonthDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6D4C41),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedReportMonthText ?: "Seleccionar Mes")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            availableMonths.forEach { (year, month) ->
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                }
                                val monthName = dateFormat.format(cal.time)

                                DropdownMenuItem(
                                    text = { Text("Reporte de $monthName") },
                                    onClick = {
                                        showMonthDropdown = false
                                        selectedReportMonthText = "Reporte de $monthName"

                                        generateReportForMonth(
                                            scope = scope,
                                            context = context,
                                            reportService = ReportGeneratorService(context),
                                            glucoseEntries = savedGlucoseEntries,
                                            foodEntries = savedFoodEntries,
                                            medications = savedMedications,
                                            contacts = savedContacts,
                                            profile = currentUserProfile,
                                            onStart = { isGeneratingReport = true },
                                            onSuccess = { file ->
                                                generatedReportFile = file
                                                showReportSuccessDialog = true
                                            },
                                            onFinish = { isGeneratingReport = false },
                                            year = year,
                                            month = month
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (generatedReportFile != null && !isGeneratingReport) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    generatedReportFile?.let { file ->
                                        val reportService = ReportGeneratorService(context)
                                        try {
                                            val success = reportService.downloadPdfFile(file)
                                            if (success) {
                                                Toast.makeText(context, "✅ Reporte guardado en Descargas", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "❌ Error al guardar", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6D4C41)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.scale(0.9f)
                                )
                                Text(text = "Descargar")
                            }

                            Button(
                                onClick = {
                                    generatedReportFile?.let { file ->
                                        val reportService = ReportGeneratorService(context)
                                        reportService.openPdfFile(file)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF673AB7)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.scale(0.9f)
                                )
                                Text(text = "Abrir")
                            }

                            Button(
                                onClick = {
                                    generatedReportFile?.let { file ->
                                        val reportService = ReportGeneratorService(context)
                                        reportService.sharePdfFile(file)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF546E7A)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.scale(0.9f)
                                )
                                Text(text = "Compartir")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NUEVA TARJETA DE RESPALDO EN DRIVE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Fondo verde suave
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Asegúrate de tener importado: import androidx.compose.material.icons.filled.CloudSync
                        // Si no lo tienes, puedes usar Icons.Default.Cloud
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Respaldo en la Nube",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para Guardar (Backup)
                    Button(
                        onClick = {
                            scope.launch {
                                val backup = BackupData(
                                    glucoseEntries = savedGlucoseEntries,
                                    foodEntries = savedFoodEntries,
                                    medications = savedMedications,
                                    emergencyContacts = savedContacts,
                                    userProfile = currentUserProfile
                                )
                                val result = driveBackupService.uploadBackup(backup)
                                if (result.isSuccess)
                                    Toast.makeText(context, "✅ Copia de seguridad guardado en Drive", Toast.LENGTH_SHORT).show()
                                else
                                    Toast.makeText(context, "❌ Error al guardar copia", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("☁️ Guardar copia de seguridad en Drive")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón para Restaurar
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = driveBackupService.downloadBackup()
                                if (result.isSuccess) {
                                    val data = result.getOrNull()!!

                                    // Restaurar cada lista a LocalStorageService
                                    data.glucoseEntries.forEach { localStorageService.saveGlucoseEntry(it) }
                                    data.foodEntries.forEach { localStorageService.saveFoodEntry(it) }
                                    // Guardamos los medicamentos (Asumiendo que tienes este método en tu LocalStorageService)
                                    // data.medications.forEach { localStorageService.saveMedication(it) }

                                    // Actualizamos el perfil y refrescamos la pantalla
                                    data.userProfile?.let {
                                        localStorageService.saveUserProfile(it)
                                        onUserProfileChange(it)
                                    }

                                    // Actualizamos la lista de contactos en la pantalla
                                    onSavedContactsChange(data.emergencyContacts)

                                    Toast.makeText(context, "✅ Datos restaurados", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Error al descargar el respaldo", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📥 Restaurar desde Drive")
                    }
                }
            }
            // ------------------------------------------
        }
    }

    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text("Agregar Contacto de Emergencia")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = { emergencyContactName = it },
                        label = { Text("Nombre del contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("Número de teléfono") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (emergencyContactName.isNotBlank() && emergencyContactPhone.isNotBlank()) {
                            val newContact = EmergencyContact(
                                id = System.currentTimeMillis(),
                                name = emergencyContactName,
                                phone = emergencyContactPhone
                            )
                            onSavedContactsChange(savedContacts + newContact)
                            emergencyContactName = ""
                            emergencyContactPhone = ""
                            showAddContactDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddContactDialog = false
                        emergencyContactName = ""
                        emergencyContactPhone = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPatientProfileDialog) {
        AlertDialog(
            onDismissRequest = { showPatientProfileDialog = false },
            title = {
                Text("Perfil del Paciente")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = patientAge,
                        onValueChange = { patientAge = it },
                        label = { Text("Edad") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = patientWeight,
                        onValueChange = { patientWeight = it },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = patientHeight,
                        onValueChange = { patientHeight = it },
                        label = { Text("Estatura (m)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        placeholder = { Text("Ej: 1.75") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val userProfile = UserProfile(
                            id = "default_user",
                            name = patientName.ifBlank { null },
                            age = patientAge.toIntOrNull(),
                            weight = patientWeight.toFloatOrNull(),
                            height = patientHeight.toFloatOrNull(),
                            diabetesType = patientDiabetesType.ifBlank { null }
                        )

                        scope.launch {
                            try {
                                val localStorageService = LocalStorageService(context)
                                localStorageService.saveUserProfile(userProfile)
                                onUserProfileChange(userProfile)
                                showPatientProfileDialog = false
                            } catch (e: Exception) {
                                showPatientProfileDialog = false
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPatientProfileDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showReportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showReportSuccessDialog = false },
            title = {
                Text("✅ Reporte Generado")
            },
            text = {
                Text("El reporte mensual se ha generado exitosamente. Puedes compartirlo o descargarlo.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showReportSuccessDialog = false }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onSave: (FoodEntry) -> Unit
) {
    var foodDescription by rememberSaveable { mutableStateOf(entry.description) }
    var foodCalories by rememberSaveable { mutableStateOf(entry.calories?.toString() ?: "") }
    var foodCarbs by rememberSaveable { mutableStateOf(entry.carbohydrates?.toString() ?: "") }
    var foodSugars by rememberSaveable { mutableStateOf(entry.sugars?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Registro de Comida") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = foodDescription,
                    onValueChange = { foodDescription = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = foodCalories,
                    onValueChange = { foodCalories = it },
                    label = { Text("Calorías (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = foodCarbs,
                        onValueChange = { foodCarbs = it },
                        label = { Text("Carbohidratos (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = foodSugars,
                        onValueChange = { foodSugars = it },
                        label = { Text("Azúcar (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (foodDescription.isNotBlank()) {
                        val newCals = foodCalories.toIntOrNull()
                        val newCarbs = foodCarbs.toIntOrNull()
                        val newSugars = foodSugars.toIntOrNull()

                        if (newCals != null || newCarbs != null || newSugars != null) {
                            val updatedEntry = entry.copy(
                                description = foodDescription,
                                calories = newCals,
                                carbohydrates = newCarbs,
                                sugars = newSugars
                            )
                            onSave(updatedEntry)
                        }
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun GlocosaSmartAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatHistoryDialog(
    // Pasamos todos los mensajes de la base de datos
    allMessages: List<ChatMessageEntity>,
    onDismissRequest: () -> Unit,
    // Devolvemos el ID seleccionado y la lista de mensajes que pertenecen a ese ID
    onConversationSelected: (String, List<ChatMessageEntity>) -> Unit
) {
    // Agrupamos todos los mensajes por su conversationId
    val conversationsMap = allMessages.groupBy { it.conversationId }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Historial de Chats", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            if (conversationsMap.isEmpty()) {
                Text("No hay chats guardados en el historial.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    // Iteramos sobre cada grupo (cada conversación)
                    items(conversationsMap.keys.toList()) { conversationId ->
                        val messagesInChat = conversationsMap[conversationId] ?: emptyList()
                        // Ordenamos para obtener el mensaje más reciente como vista previa
                        val lastMessage = messagesInChat.maxByOrNull { it.timestamp }

                        // Formatear la fecha del último mensaje
                        val dateStr = lastMessage?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(it.timestamp))
                        } ?: ""

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Al seleccionar, pasamos el ID y los mensajes ordenados por tiempo
                                    onConversationSelected(conversationId, messagesInChat.sortedBy { it.timestamp })
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Chat: ${conversationId.take(8)}...", // Muestra un fragmento del ID
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastMessage?.text ?: "Sin mensajes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis // Pone "..." si el texto es muy largo
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar")
            }
        }
    )
}


@Composable
fun LoginScreen(
    authService: AuthService,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegisterMode by remember { mutableStateOf(false) }

    // Launcher para Google Sign-In
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            isLoading = true
            val loginResult = authService.handleGoogleSignInResult(result.data)
            isLoading = false
            if (loginResult.isSuccess) onLoginSuccess()
            else errorMessage = "Error al iniciar con Google"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(32.dp))

        // Botón Google
        OutlinedButton(
            onClick = { googleLauncher.launch(authService.getGoogleSignInIntent()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Continuar con Google")
        }

        Spacer(Modifier.height(16.dp))
        Text("— o —", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    val result = if (isRegisterMode)
                        authService.registerWithEmail(email, password)
                    else
                        authService.loginWithEmail(email, password)
                    isLoading = false
                    if (result.isSuccess) onLoginSuccess()
                    else errorMessage = result.exceptionOrNull()?.message
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text(if (isRegisterMode) "Registrarme" else "Entrar")
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "¿Sin cuenta? Regístrate")
        }
    }
}

fun generateReportForMonth(
    scope: CoroutineScope,
    context: Context,
    reportService: ReportGeneratorService,
    glucoseEntries: List<GlucoseEntry>,
    foodEntries: List<FoodEntry>,
    medications: List<Medication>,
    contacts: List<EmergencyContact>,
    profile: UserProfile?,
    onStart: () -> Unit,
    onSuccess: (File) -> Unit,
    onFinish: () -> Unit,
    year: Int,
    month: Int
) {
    onStart()
    scope.launch {
        try {
            val localStorageService = LocalStorageService(context)
            val patientName = profile?.name.orEmpty().ifEmpty { "Usuario" }
            val patientAge = profile?.age ?: 0
            val patientWeight = profile?.weight
            val patientHeight = profile?.height
            val patientDiabetesType = profile?.diabetesType.orEmpty().ifEmpty { "Tipo 2" }
            val listaOlvidos = localStorageService.getMissedMedicationsReport(month, year).first()

            val reportFile = reportService.generateMonthlyReport(
                glucoseEntries = glucoseEntries,
                foodEntries = foodEntries,
                medications = medications,
                emergencyContacts = contacts,
                missedMedications = listaOlvidos,
                patientName = patientName,
                patientAge = patientAge,
                patientDiabetesType = patientDiabetesType,
                patientWeight = patientWeight,
                patientHeight = patientHeight,
                year = year,
                month = month
            )
            onSuccess(reportFile)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            onFinish()
        }
    }
}

fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

fun calculateCaloriesFromDescription(description: String, mealType: String): Int {
    val lowerDescription = description.lowercase()
    var baseCalories = when (mealType) {
        "Desayuno" -> 300
        "Almuerzo" -> 600
        "Comida" -> 500
        "Cena" -> 400
        "Aperitivo" -> 150
        else -> 250
    }

    when {
        lowerDescription.contains("pollo") || lowerDescription.contains("pavo") -> baseCalories += 50
        lowerDescription.contains("pescado") || lowerDescription.contains("atún") -> baseCalories += 30
        lowerDescription.contains("carne") || lowerDescription.contains("res") -> baseCalories += 100
        lowerDescription.contains("arroz") || lowerDescription.contains("pasta") -> baseCalories += 150
        lowerDescription.contains("pan") || lowerDescription.contains("tortilla") -> baseCalories += 80
        lowerDescription.contains("huevo") -> baseCalories += 70
        lowerDescription.contains("leche") || lowerDescription.contains("yogur") -> baseCalories += 60
        lowerDescription.contains("fruta") || lowerDescription.contains("manzana") -> baseCalories += 50
        lowerDescription.contains("verdura") || lowerDescription.contains("ensalada") -> baseCalories -= 50
        lowerDescription.contains("dulce") || lowerDescription.contains("postre") -> baseCalories += 120
        lowerDescription.contains("frito") || lowerDescription.contains("empanizado") -> baseCalories += 80
    }

    return baseCalories.coerceAtLeast(50)
}

fun calculateDailyCalories(foodEntries: List<FoodEntry>): Int {
    val today = java.time.LocalDate.now()
    return foodEntries
        .filter {
            val entryDate = java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            entryDate == today
        }
        .sumOf { it.calories ?: 0 }
}

fun getRandomHealthTip(): String {
    val tips = listOf(
        "💧 Bebe al menos 8 vasos de agua al día para mantener tu glucosa estable.",
        "🏃‍♂️ Realiza 30 minutos de ejercicio moderado diariamente.",
        "🍎 Come frutas y verduras ricas en fibra para controlar la glucosa.",
        "⏰ Mantén horarios regulares para tus comidas y medicamentos.",
        "📊 Revisa tu glucosa antes y después de las comidas principales.",
        "🥗 Incluye proteínas magras en cada comida para estabilizar la glucosa.",
        "😴 Duerme 7-8 horas por noche para mantener un buen control metabólico.",
        "🧘‍♀️ Practica técnicas de relajación para reducir el estrés.",
        "👟 Usa calzado cómodo y revisa tus pies diariamente.",
        "📱 Mantén un registro de tus niveles de glucosa en esta app.",
        "🥜 Consume frutos secos en porciones pequeñas como snack saludable.",
        "🚶‍♂️ Camina 10,000 pasos al día para mejorar tu sensibilidad a la insulina.",
        "🥛 Consume lácteos bajos en grasa para obtener calcio y proteínas.",
        "🌾 Elige granos enteros en lugar de refinados para mejor control glucémico.",
        "🍽️ Controla el tamaño de las porciones para mantener un peso saludable."
    )
    return tips.random()
}

fun calculateIMC(weight: Float, height: Float, age: Int? = null): Pair<Float, String> {
    val heightSquared = height * height
    val imc = weight / heightSquared

    val category = when {
        imc < 18.5f -> "Bajo de peso"
        imc < 25.0f -> "Saludable"
        imc < 30.0f -> "Sobrepeso"
        imc < 35.0f -> "Obesidad Clase 1"
        imc < 40.0f -> "Obesidad Clase 2"
        else -> "Obesidad Clase 3"
    }

    return Pair(imc, category)
}

fun getIMCColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category) {
        "Bajo de peso" -> androidx.compose.ui.graphics.Color(0xFFFFC107)
        "Saludable" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "Sobrepeso" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        "Obesidad Clase 1" -> androidx.compose.ui.graphics.Color(0xFFFF5722)
        "Obesidad Clase 2" -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
        "Obesidad Clase 3" -> androidx.compose.ui.graphics.Color(0xFFB71C1C)
        else -> androidx.compose.ui.graphics.Color.Gray
    }
}

private fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
}

private fun getDistinctMonths(
    glucose: List<GlucoseEntry>,
    food: List<FoodEntry>,
    meds: List<Medication>
): List<Pair<Int, Int>> {
    val distinctMonths = mutableSetOf<Pair<Int, Int>>()
    val cal = Calendar.getInstance()

    (glucose.map { it.timestamp } + food.map { it.timestamp } + meds.map { it.endDate }).forEach { timestamp ->
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        distinctMonths.add(Pair(year, month))
    }

    return distinctMonths.sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
}

private fun getTimestampForSelectedDate(selectedDate: LocalDate, today: LocalDate): Long {
    val now = LocalDateTime.now()
    val timestamp = if (selectedDate == today) {
        now
    } else {
        selectedDate.atTime(now.toLocalTime())
    }
    return timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}