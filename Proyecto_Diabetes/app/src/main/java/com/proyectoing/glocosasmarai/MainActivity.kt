package com.proyectoing.glocosasmarai
/**
 *
 *Este archivo es el controlador principal de toda la aplicación.
 *Es el punto de partida que decide qué mostrar en la pantalla y cómo interactuar con el usuario.
 *
 */
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope // <-- AÑADE ESTA LÍNEA
import android.content.Context
import com.proyectoing.glocosasmarai.services.ReportGeneratorService
import com.proyectoing.glocosasmarai.services.ChatbotJsonService
import com.proyectoing.glocosasmarai.services.LocalStorageService
import com.proyectoing.glocosasmarai.models.*
import com.proyectoing.glocosasmarai.chatbot.*
import com.proyectoing.glocosasmarai.models.Medication
import java.io.File
import androidx.compose.ui.graphics.Color // Para el color verde
import androidx.compose.material.icons.filled.CheckCircle // Para el ícono de éxito
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import android.widget.Toast
import java.util.Calendar // <-- AÑADIDO
import java.text.SimpleDateFormat // <-- AÑADE ESTA LÍNEA
import java.util.Locale
import androidx.compose.material.icons.filled.Medication // <-- Para pastillas (frasco)
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Schedule // Icono de reloj
import androidx.compose.material.icons.filled.ChevronRight

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlocosaSmartApp() {
    // prueba de meses
    //------------------------------------------------------------
    val now = System.currentTimeMillis()
    val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000) // Aproximadamente 30 días
    val twoMonthsAgo = now - (60L * 24 * 60 * 60 * 1000)
    //------------------------------------------------------------
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("HOME") }
    
    val context = LocalContext.current
    val localStorageService = remember { LocalStorageService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // --- ESTADO GLOBAL (CON MEDICAMENTOS) ---
    var globalChatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var globalSavedConversations by remember { mutableStateOf(listOf<SavedConversation>()) }
    var globalCurrentConversationId by remember { mutableStateOf<String?>(null) }
    //Prueba
    //------------------------------------------------------------------------------------
    val initialGlucose = listOf(
        GlucoseEntry(
            id = twoMonthsAgo, // Usamos la marca como ID
            value = 110,
            timestamp = twoMonthsAgo + 1000, // Septiembre 2025
            isBeforeMeal = true,
            notes = "Septiembre: Glucosa normal"
        ),
        GlucoseEntry(
            id = oneMonthAgo, // Usamos la marca como ID
            value = 140,
            timestamp = oneMonthAgo + 1000, // Octubre 2025
            isBeforeMeal = true,
            notes = "Octubre: Glucosa alta"
        )
    )
    var globalSavedGlucoseEntries by remember { mutableStateOf(initialGlucose) }
    //------------------------------------------------------------------------------------
    //var globalSavedGlucoseEntries by remember { mutableStateOf(listOf<GlucoseEntry>()) }
    //------------------------------------------------------------------------------------
    var globalSavedFoodEntries by remember { mutableStateOf(listOf<FoodEntry>()) }
    var globalSavedContacts by remember { mutableStateOf(listOf<EmergencyContact>()) }
    var globalUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var globalSavedMedications by remember { mutableStateOf(listOf<Medication>()) } 
    var globalChatPrefill by remember { mutableStateOf<String?>(null) }
    
    // Cargar datos desde el almacenamiento local al iniciar
    LaunchedEffect(Unit) {
        localStorageService.initializeDefaultSettings()
        //Prueba
        //-------------------------------------------------------------------------
        launch { localStorageService.getAllGlucoseEntries().collect { entries ->
            if (entries.isNotEmpty()) {
                globalSavedGlucoseEntries = entries
            } else {
                // Si la DB está vacía, al menos cargamos los datos de prueba
                // para que el reporte dinámico funcione.
                globalSavedGlucoseEntries = initialGlucose
            }
        } }
        //--------------------------------------------------------------------------
        //original
        /*
        launch { localStorageService.getAllGlucoseEntries().collect { entries ->
            globalSavedGlucoseEntries = entries // <-- Aquí se reemplazaban
        } }
         */
        launch { localStorageService.getAllFoodEntries().collect { entries ->
            globalSavedFoodEntries = entries
        } }
        launch { localStorageService.getAllEmergencyContacts().collect { contacts ->
            globalSavedContacts = contacts
        } }
        launch { localStorageService.getAllConversations().collect { conversations ->
            globalSavedConversations = conversations
        } }
        launch { localStorageService.getAllMedications().collect { medications -> 
            globalSavedMedications = medications
        } }
        launch { localStorageService.getCurrentUserProfile().collect { profile ->
            globalUserProfile = profile
        } }
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
            // 1. Definimos los colores manualmente (inspirados en tu imagen)
            val darkPurpleBar = Color(0xFF383854)   // Color de fondo para toda la barra
            val lightPurpleIndicator = Color(0xFF6B5B8E) // Color para el ítem SELECCIONADO
            val iconTextColor = Color.White          // Color para TODO el texto e íconos

            NavigationBar(
                // 2. Aplicamos el color de fondo oscuro
                containerColor = darkPurpleBar
            ) {
                // 3. Define los colores para los ítems
                val itemColors = NavigationBarItemDefaults.colors(
                    // Color del "óvalo" indicador
                    indicatorColor = lightPurpleIndicator,

                    // Color para el ícono y texto cuando están SELECCIONADOS
                    selectedIconColor = iconTextColor,
                    selectedTextColor = iconTextColor,

                    // Color para el ícono y texto cuando NO están seleccionados
                    unselectedIconColor = iconTextColor.copy(alpha = 0.7f), // Un poco más tenues
                    unselectedTextColor = iconTextColor.copy(alpha = 0.7f)
                )

                // 4. Aplica esos colores a CADA NavigationBarItem
                NavigationBarItem(
                    selected = currentScreen == "HOME",
                    onClick = { currentScreen = "HOME" },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    colors = itemColors // <-- APLICAR
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_GLUCOSE",
                    onClick = { currentScreen = "ADD_GLUCOSE" },
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                    label = { Text("Glucosa") },
                    colors = itemColors // <-- APLICAR
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_FOOD",
                    onClick = { currentScreen = "ADD_FOOD" },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Comida") },
                    colors = itemColors // <-- APLICAR
                )
                NavigationBarItem(
                    selected = currentScreen == "ADD_MEDICATION",
                    onClick = { currentScreen = "ADD_MEDICATION" },
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text("Medicina") },
                    colors = itemColors // <-- APLICAR
                )
                NavigationBarItem(
                    selected = currentScreen == "SETTINGS",
                    onClick = { currentScreen = "SETTINGS" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                    colors = itemColors // <-- APLICAR
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
                // --- CONTENIDO PRINCIPAL (CON TODAS LAS FUNCIONES) ---
                when (currentScreen) {
                    "HOME" -> HomeScreen(
                        chatMessages = globalChatMessages,
                        savedConversations = globalSavedConversations,
                        currentConversationId = globalCurrentConversationId,
                        onChatMessagesChange = { newMessages ->
                            globalChatMessages = newMessages
                            coroutineScope.launch {
                                if (globalCurrentConversationId != null) {
                                    localStorageService.saveChatMessages(newMessages, globalCurrentConversationId!!)
                                }
                            }
                        },
                        onSavedConversationsChange = { newConversations ->
                            globalSavedConversations = newConversations
                            coroutineScope.launch {
                                newConversations.forEach { conversation ->
                                    localStorageService.saveConversation(conversation)
                                }
                            }
                        },
                        onCurrentConversationIdChange = { globalCurrentConversationId = it },
                        glucoseEntries = globalSavedGlucoseEntries,
                        savedMedications = globalSavedMedications,
                        onNavigate = { screenName -> currentScreen = screenName },
                        chatPrefill = globalChatPrefill,
                        onChatPrefillConsumed = { globalChatPrefill = null }
                    )
                    "ADD_GLUCOSE" -> AddGlucoseScreen(
                        savedGlucoseEntries = globalSavedGlucoseEntries,
                        onSavedGlucoseEntriesChange = { newEntries ->
                            globalSavedGlucoseEntries = newEntries
                            coroutineScope.launch {
                                newEntries.forEach { entry ->
                                    localStorageService.saveGlucoseEntry(entry)
                                }
                            }
                        }
                    )
                    "ADD_FOOD" -> AddFoodScreen(
                        savedFoodEntries = globalSavedFoodEntries,
                        // --- ESTA LÓGICA ES NUEVA ---
                        onSavedFoodEntriesChange = { newEntries ->
                            // 1. Guarda la lista vieja
                            val oldList = globalSavedFoodEntries
                            // 2. Actualiza la UI de inmediato
                            globalSavedFoodEntries = newEntries

                            coroutineScope.launch {
                                // 3. Busca el item que fue eliminado
                                //    (El que está en oldList pero no en newEntries)
                                val entryToDelete = oldList.find { oldEntry ->
                                    newEntries.none { newEntry -> newEntry.id == oldEntry.id }
                                }

                                // 4. Si encontramos uno, lo borramos de la base de datos
                                if (entryToDelete != null) {
                                    // Asumo que tu servicio tiene esta función, basado en tu MedicationScreen.kt
                                    localStorageService.deleteFoodEntry(entryToDelete)
                                }

                                // 5. Guardamos/Actualizamos el resto (para que funcionen las ediciones)
                                newEntries.forEach { entry ->
                                    localStorageService.saveFoodEntry(entry)
                                }
                            }
                        },
                        // --- ACTUALIZADO (Sin chequeo de red) ---
                        onAnalyzeWithAI = { foodDescription ->
                            globalChatPrefill = "Qué calorías tiene esta comida: $foodDescription"
                            currentScreen = "HOME"
                        }
                    )
                    // --- CORREGIDO: Llama a tu pantalla real ---
                    "ADD_MEDICATION" -> MedicationScreen()
                    
                    "SETTINGS" -> SettingsScreen(
                        savedContacts = globalSavedContacts,
                        // --- APLICAMOS LA MISMA CORRECCIÓN AQUÍ ---
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
    savedConversations: List<SavedConversation>,
    currentConversationId: String?,
    onChatMessagesChange: (List<ChatMessage>) -> Unit,
    onSavedConversationsChange: (List<SavedConversation>) -> Unit,
    onCurrentConversationIdChange: (String?) -> Unit,
    glucoseEntries: List<GlucoseEntry> = emptyList(),
    // --- Parámetros de la app (sin cambios) ---
    savedMedications: List<Medication>,
    onNavigate: (String) -> Unit,
    chatPrefill: String?,
    onChatPrefillConsumed: () -> Unit
) {
    var showChatHistory by rememberSaveable { mutableStateOf(false) }
    var userInput by rememberSaveable { mutableStateOf("") }
    var currentTip by rememberSaveable { mutableStateOf(getRandomHealthTip()) }

    // --- INICIO DE CAMBIOS (Lógica de Banner Fijo) ---

    // 1. Estados para manejar la conexión
    var isVerifying by remember { mutableStateOf(true) }
    var serverStatus by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // 2. CoroutineScope (sin cambios)
    val coroutineScope = rememberCoroutineScope()

    // 3. Lógica para el mensaje pre-llenado (sin cambios)
    LaunchedEffect(chatPrefill) {
        if (chatPrefill != null) {
            userInput = chatPrefill
            onChatPrefillConsumed()
        }
    }

    // 4. Verificación de conexión inicial (¡CON BANNER FIJO!)
    LaunchedEffect(Unit) {
        isVerifying = true
        serverStatus = "Verificando..."
        try {
            ChatbotFunctions.getChatbotResponse("ping", emptyList(), null)

            // --- ¡CAMBIO IMPORTANTE! ---
            serverStatus = "Conectado" // Éxito. Se queda verde.
            isVerifying = false
            // NO MÁS 'delay' NI 'serverStatus = null'
            // --- FIN DEL CAMBIO ---

        } catch (e: Exception) {
            // Error, mostrar banner de "Sin conexion" (este se queda fijo)
            serverStatus = "Sin conexion - usando Respuestas automaticas"
            isVerifying = false
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // 5. Función centralizada para enviar mensajes (CORRECCIÓN DE ORDEN DEFINITIVA)
    fun sendMessage() {
        if (userInput.isNotBlank()) {

            // 1. CREA TU PREGUNTA
            val userMessage = ChatMessage(userInput, true, System.currentTimeMillis())

            // 2. AÑADE SÓLO LA PREGUNTA a la lista
            val messagesWithQuestion = chatMessages + userMessage

            // 3. ACTUALIZA LA UI SÓLO CON TU PREGUNTA
            onChatMessagesChange(messagesWithQuestion)

            val currentInput = userInput
            userInput = ""

            // 4. Inicia la corutina para buscar la RESPUESTA
            coroutineScope.launch {

                // Damos un respiro (150ms) a la UI para que dibuje la pregunta
                // Esto es clave para que la "agrupación" de Compose no ocurra.
                delay(150)

                var botMessage: ChatMessage
                var finalChatMessages: List<ChatMessage>

                try {
                    // --- 5A. INTENTA CONEXIÓN REAL ---
                    val lastGlucoseLevel = glucoseEntries.maxByOrNull { it.timestamp }?.value
                    // Pasamos la lista con la pregunta para que el bot tenga el contexto MÁS reciente
                    val botResponse = ChatbotFunctions.getChatbotResponse(currentInput, messagesWithQuestion, lastGlucoseLevel)

                    botMessage = ChatMessage(botResponse, false, System.currentTimeMillis())
                    serverStatus = "Conectado"
                    isVerifying = false

                } catch (e: Exception) {
                    // --- 5B. FALLO (SIN CONEXIÓN) ---
                    serverStatus = "Sin conexion - usando Respuestas automaticas"
                    isVerifying = false

                    // Generar la respuesta simulada
                    val chatbotService = ChatbotJsonService()
                    val fallbackResponse = chatbotService.generateSimulatedResponse(currentInput, glucoseEntries.maxByOrNull { it.timestamp }?.value)
                    val chatbotOutput = chatbotService.parseChatbotResponse(fallbackResponse)
                    val simulatedMessage = chatbotService.extractResponseMessage(chatbotOutput)

                    botMessage = ChatMessage(simulatedMessage, false, System.currentTimeMillis())
                }

                // --- 6. ACTUALIZA LA UI CON LA RESPUESTA ---
                // Ahora creamos la lista final que tiene la pregunta + la respuesta
                finalChatMessages = messagesWithQuestion + botMessage
                onChatMessagesChange(finalChatMessages)

                // --- 7. GUARDA LA CONVERSACIÓN COMPLETA ---
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

    // --- FIN DE CAMBIOS ---

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp)
    ) {
        item {
            // Header (sin cambios)
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

        // --- Banner de 3 estados (sin cambios en la UI, solo en la lógica de arriba) ---
        if (serverStatus != null) {
            item {
                val isConnected = serverStatus == "Conectado"
                val isError = serverStatus!!.startsWith("Sin conexion")

                val containerColor = when {
                    isVerifying -> MaterialTheme.colorScheme.surfaceVariant
                    isError -> MaterialTheme.colorScheme.errorContainer
                    isConnected -> Color(0xFFC8E6C9) // Verde suave
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    isVerifying -> MaterialTheme.colorScheme.onSurfaceVariant
                    isError -> MaterialTheme.colorScheme.onErrorContainer
                    isConnected -> Color(0xFF2E7D32) // Verde oscuro
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val icon = when {
                    isVerifying -> null // Se usa el Spinner
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
        // --- FIN DEL BANNER ---


        item {
            // Chatbot Section
            Card(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E0))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Chatbot Header (sin cambios)
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

                    // Chat Messages (sin cambios)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Ocupa todo el espacio disponible
                    ) {
                        // Muestra los mensajes si la conversación ya empezó
                        if (chatMessages.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(), // Llena el Box
                                state = listState,
                                reverseLayout = true
                            ) {
                                items(chatMessages.reversed()) { message ->
                                    ChatMessageItem(message = message)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        // Muestra el mensaje de bienvenida si el chat está vacío
                        else {
                            Text(
                                text = "¡Hola! Soy tu asistente. Escribe una pregunta para comenzar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.Center) // Centra el texto
                                    .padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Section (sin cambios)
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

        // ... (El resto de tu código de HomeScreen: Consejo del Día, Acciones Rápidas, Diálogo) ...

        // Sección de consejos aleatorios
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

        // Sección de Acciones Rápidas
        // --- CAMBIO: Sección de Próximos Recordatorios (CON EMOJIS 💊 💉) ---
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
                    // 2. Encabezado de la tarjeta (sin cambios)
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

                    // 3. Lógica de filtrado (sin cambios)
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val todayInMillis = today.timeInMillis

                    val activeReminders = savedMedications
                        .filter { it.endDate >= todayInMillis }
                        .sortedWith(compareBy({ it.hour }, { it.minute }))

                    val nextReminders = activeReminders.take(3)
                    val remainingRemindersCount = (activeReminders.size - nextReminders.size).coerceAtLeast(0)

                    // 4. Mostrar los recordatorios
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
                                    // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
                                    color = MaterialTheme.colorScheme.onTertiaryContainer // <-- Decía "onTertiyContainer"
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
        // --- FIN DEL CAMBIO ---
        // --- FIN DEL CAMBIO ---
    }

    // Diálogo para mostrar historial de conversaciones (sin cambios)
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
                                    onChatMessagesChange(conversation.messages)
                                    onCurrentConversationIdChange(conversation.id)
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

// --- AÑADIDO: Banner de estado de conexión ---
@Composable
fun ConnectionStatusBanner(status: String, isVerifying: Boolean) {
    val backgroundColor = when {
        isVerifying -> MaterialTheme.colorScheme.tertiaryContainer
        status.startsWith("Sin conexion") -> MaterialTheme.colorScheme.errorContainer
        else -> Color(0xFFC8E6C9) // Verde (Conectado)
    }
    val contentColor = when {
        isVerifying -> MaterialTheme.colorScheme.onTertiaryContainer
        status.startsWith("Sin conexion") -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color(0xFF2E7D32)
    }
    val icon = when {
        isVerifying -> null // No hay ícono
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
    onSavedGlucoseEntriesChange: (List<GlucoseEntry>) -> Unit
) {
    var glucoseValue by rememberSaveable { mutableStateOf("") }
    var glucoseNotes by rememberSaveable { mutableStateOf("") }
    var isBeforeMeal by rememberSaveable { mutableStateOf(true) }
    var editingEntry by rememberSaveable { mutableStateOf<GlucoseEntry?>(null) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showEmergencyAlert by rememberSaveable { mutableStateOf(false) }
    var showWarningAlert by rememberSaveable { mutableStateOf(false) }
    
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
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        item {
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
                label = { Text("Notas adicionales (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (glucoseValue.isNotBlank()) {
                        val glucoseValueInt = glucoseValue.toIntOrNull() ?: 0
                        
                        val entry = GlucoseEntry(
                            id = System.currentTimeMillis(),
                            value = glucoseValueInt,
                            timestamp = System.currentTimeMillis(),
                            isBeforeMeal = isBeforeMeal,
                            notes = glucoseNotes.ifBlank { null }
                        )
                        
                        when {
                            glucoseValueInt > 250 -> {
                                showEmergencyAlert = true
                            }
                            glucoseValueInt < 70 -> {
                                showWarningAlert = true
                            }
                        }
                        
                        val updatedList = savedGlucoseEntries + entry
                        onSavedGlucoseEntriesChange(updatedList)
                        
                        glucoseValue = ""
                        glucoseNotes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Registro")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Registros Guardados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(savedGlucoseEntries.reversed()) { entry ->
            val isNormalRange = if (entry.isBeforeMeal) {
                entry.value in 80..130
            } else {
                entry.value in 80..200
            }
            
            val backgroundColor = if (isNormalRange) {
                androidx.compose.ui.graphics.Color(0xFFE8F5E8)
            } else {
                androidx.compose.ui.graphics.Color(0xFFFFEBEE)
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
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
                                color = if (isNormalRange) {
                                    androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                } else {
                                    androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                }
                            )
                            Text(
                                text = "mg/dL",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                editingEntry = entry
                                showEditDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar registro",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNormalRange) {
                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            } else {
                                androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            }
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (entry.isBeforeMeal) "Antes de comer" else "Después de comer",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (!entry.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notas: ${entry.notes ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = formatDate(entry.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    if (showEditDialog && editingEntry != null) {
        EditGlucoseDialog(
            entry = editingEntry!!,
            onDismiss = { 
                showEditDialog = false
                editingEntry = null
            },
            onSave = { updatedEntry ->
                val updatedList = savedGlucoseEntries.map { 
                    if (it.id == updatedEntry.id) updatedEntry else it 
                }
                onSavedGlucoseEntriesChange(updatedList)
                showEditDialog = false
                editingEntry = null
            }
        )
    }
    
    if (showEmergencyAlert) {
        AlertDialog(
            onDismissRequest = { showEmergencyAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¡ALERTA DE EMERGENCIA!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tu nivel de glucosa está muy alto (>250 mg/dL)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se enviará una alerta a tus contactos de emergencia.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showEmergencyAlert = false
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
    
    if (showWarningAlert) {
        AlertDialog(
            onDismissRequest = { showWarningAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Advertencia",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tu nivel de glucosa está bajo (<70 mg/dL)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recomendaciones:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• Consume algo con azúcar rápido (ej. jugo)")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWarningAlert = false }
                ) {
                    Text("Entendido")
                }
            }
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

// --- AddFoodScreen (CON LÍMITES DE CALORÍAS Y BOTÓN IA) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    savedFoodEntries: List<FoodEntry>,
    onSavedFoodEntriesChange: (List<FoodEntry>) -> Unit,
    onAnalyzeWithAI: (String) -> Unit
) {
    var foodDescription by rememberSaveable { mutableStateOf("") }
    var foodNotes by rememberSaveable { mutableStateOf("") } // Este campo es para Calorías
    var selectedMealType by rememberSaveable { mutableStateOf("Desayuno") }
    var showHighCalorieAlert by rememberSaveable { mutableStateOf(false) }
    var showDailyCalorieAlert by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var showEditFoodDialog by rememberSaveable { mutableStateOf(false) }
    var foodEntryToEdit by rememberSaveable { mutableStateOf<FoodEntry?>(null) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Registro de Comida",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tipo de Comida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val mealTypes = listOf("Desayuno", "Almuerzo", "Comida", "Aperitivo", "Cena")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // --- 1. Define los colores personalizados ---
                    val chipColors = FilterChipDefaults.filterChipColors(
                        // Colores cuando NO está seleccionado (para que resalte)
                        containerColor = MaterialTheme.colorScheme.surface, // Blanco/Gris claro
                        labelColor = MaterialTheme.colorScheme.onSurface,   // Texto oscuro

                        // Colores cuando SÍ está seleccionado
                        selectedContainerColor = Color(0xFFFF9800), // <-- Naranja
                        selectedLabelColor = Color.White              // <-- Texto blanco
                    )
                    // --- Fin del cambio ---

                    mealTypes.forEach { mealType ->
                        FilterChip(
                            selected = selectedMealType == mealType,
                            onClick = { selectedMealType = mealType },
                            label = { Text(mealType) },
                            colors = chipColors // <-- 2. Aplica los colores aquí
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            OutlinedTextField(
                value = foodDescription,
                onValueChange = { foodDescription = it },
                label = { Text("Descripción de la comida") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Botón "Analizar con IA" (SIN CHEQUEO DE RED) ---
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { 
                    if (foodDescription.isNotBlank()) {
                        onAnalyzeWithAI(foodDescription)
                    } else {
                        Toast.makeText(context, "Escribe una descripción primero", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = foodDescription.isNotBlank() // Solo se activa si hay texto
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analizar calorías con IA")
            }
            // --- FIN ---
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = foodNotes,
                onValueChange = { foodNotes = it },
                label = { Text("Calorías") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej: 450 (dejar vacío para calcular automáticamente)") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (foodDescription.isNotBlank()) {
                        val calories = if (foodNotes.isBlank()) {
                            calculateCaloriesFromDescription(foodDescription, selectedMealType).toString()
                        } else {
                            foodNotes
                        }
                        
                        val caloriesInt = calories.toIntOrNull() ?: 0
                        
                        // --- LÍMITE DE CALORÍAS CORREGIDO ---
                        if (caloriesInt > 700) {
                            showHighCalorieAlert = true
                        }
                        
                        val entry = FoodEntry(
                            id = System.currentTimeMillis(),
                            type = selectedMealType,
                            description = foodDescription,
                            timestamp = System.currentTimeMillis(),
                            calories = calories.toIntOrNull(),
                            carbohydrates = null,
                            notes = null
                        )
                        
                        val updatedEntries = savedFoodEntries + entry
                        onSavedFoodEntriesChange(updatedEntries)
                        
                        // --- LÍMITE DE CALORÍAS CORREGIDO ---
                        val dailyCalories = calculateDailyCalories(updatedEntries)
                        if (dailyCalories > 2000) {
                            showDailyCalorieAlert = true
                        }
                        
                        foodDescription = ""
                        foodNotes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Registro")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (savedFoodEntries.isNotEmpty()) {
            item {
                Text(
                    text = "Registros Guardados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                val dailyCalories = calculateDailyCalories(savedFoodEntries)
                // --- LÍMITE DE CALORÍAS CORREGIDO ---
                val isHighCalories = dailyCalories > 2000
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHighCalories) {
                            androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                        } else {
                            androidx.compose.ui.graphics.Color(0xFFE8F5E8)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isHighCalories) {
                                    androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                } else {
                                    androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Calorías del Día",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "$dailyCalories calorías",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighCalories) {
                                androidx.compose.ui.graphics.Color(0xFFD32F2F)
                            } else {
                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            }
                        )
                        
                        if (isHighCalories) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Límite diario de 2000 kcal superado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            items(savedFoodEntries.reversed()) { entry ->
                val entryCalories = entry.calories ?: 0
                // --- LÍMITE DE CALORÍAS CORREGIDO ---
                val isHighCalories = entryCalories > 700
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHighCalories) {
                            androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                        } else {
                            androidx.compose.ui.graphics.Color(0xFFE8F5E8)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = entry.type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    foodEntryToEdit = entry
                                    showEditFoodDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    // Esta es la lógica de borrado.
                                    // Actualiza la lista principal, lo que disparará el guardado en GlocosaSmartApp
                                    onSavedFoodEntriesChange(savedFoodEntries.filter { it.id != entry.id })
                                }) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (entry.calories != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${entry.calories} calorías",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighCalories) {
                                        androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDate(entry.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // --- TEXTOS DE ALERTA DE CALORÍAS CORREGIDOS ---
    if (showHighCalorieAlert) {
        AlertDialog(
            onDismissRequest = { showHighCalorieAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¡Alto en Calorías!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Esta comida tiene más de 700 calorías, lo cual es alto para una sola comida.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Considera reducir la porción la próxima vez.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHighCalorieAlert = false }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
    
    if (showDailyCalorieAlert) {
        AlertDialog(
            onDismissRequest = { showDailyCalorieAlert = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¡Exceso de Calorías Diarias!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Has superado las 2,000 calorías recomendadas para el día.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Prioriza alimentos más ligeros el resto del día.")
                    Text("• Consulta con tu médico o nutricionista.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDailyCalorieAlert = false }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
    if (showEditFoodDialog && foodEntryToEdit != null) {
        EditFoodDialog(
            entry = foodEntryToEdit!!,
            onDismiss = {
                showEditFoodDialog = false
                foodEntryToEdit = null
            },
            onSave = { updatedEntry ->
                // Actualiza la lista principal con la entrada modificada
                val updatedList = savedFoodEntries.map {
                    if (it.id == updatedEntry.id) updatedEntry else it
                }
                onSavedFoodEntriesChange(updatedList)

                // Cierra el diálogo
                showEditFoodDialog = false
                foodEntryToEdit = null
            }
        )
    }

}

// SettingsScreen (sin cambios)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    savedContacts: List<EmergencyContact>,
    onSavedContactsChange: (List<EmergencyContact>) -> Unit,
    savedGlucoseEntries: List<GlucoseEntry> = emptyList(),
    savedFoodEntries: List<FoodEntry> = emptyList(),
    savedMedications: List<Medication> = emptyList(),
    currentUserProfile: UserProfile? = null,
    onUserProfileChange: (UserProfile?) -> Unit = {}
) {
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactPhone by rememberSaveable { mutableStateOf("") }
    var showAddContactDialog by rememberSaveable { mutableStateOf(false) }
    
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var showIMCResult by rememberSaveable { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var selectedReportMonthText by rememberSaveable { mutableStateOf<String?>("Seleccionar Mes") }
    
    var patientName by rememberSaveable { mutableStateOf("") }
    var patientAge by rememberSaveable { mutableStateOf("") }
    var patientWeight by rememberSaveable { mutableStateOf(currentUserProfile?.weight?.toString().orEmpty()) }
    var patientHeight by rememberSaveable { mutableStateOf(currentUserProfile?.height?.toString().orEmpty()) }
    var patientDiabetesType by rememberSaveable { mutableStateOf("Tipo 2") }
    var showPatientProfileDialog by rememberSaveable { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGeneratingReport by rememberSaveable { mutableStateOf(false) }
    var showReportSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var generatedReportFile by rememberSaveable { mutableStateOf<File?>(null) }
    
    LaunchedEffect(showPatientProfileDialog, currentUserProfile) {
        if (showPatientProfileDialog && currentUserProfile != null) {
            patientName = currentUserProfile.name ?: ""
            patientAge = currentUserProfile.age?.toString() ?: ""
            patientDiabetesType = currentUserProfile.diabetesType ?: "Tipo 2"
        } else if (!showPatientProfileDialog && currentUserProfile != null) {
            patientName = currentUserProfile.name ?: ""
            patientAge = currentUserProfile.age?.toString() ?: ""
            patientDiabetesType = currentUserProfile.diabetesType ?: "Tipo 2"
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
                Color(0xFFFFEBEE) // Rojo Pálido (No registrado)
            } else {
                Color(0xFFE8F5E8) // Verde Pálido (Registrado)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                // --- 2. Aplica el color a la tarjeta ---
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
                            // --- 2. Usa la variable aquí ---
                            Text(buttonText)
                            // --- FIN DEL CAMBIO ---
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nombre: ${currentUserProfile?.name ?: "—"}")
                    Text("Edad: ${currentUserProfile?.age?.toString() ?: "—"}")
                    Text("Tipo de Diabetes: ${currentUserProfile?.diabetesType ?: "—"}")
                    Text("Peso: ${currentUserProfile?.weight?.toString()?.let { "$it kg" } ?: "—"}")
                    Text("Estatura: ${currentUserProfile?.height?.toString()?.let { "$it m" } ?: "—"}")
                }
            }

            val contactsCardColor = if (savedContacts.isEmpty()) {
                Color(0xFFFFEBEE) // Rojo Pálido (No registrados)
            } else {
                Color(0xFFFFF9E0) // Amarillo Pálido (Registrados)
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

                        // --- NUEVA LÓGICA: LEE DIRECTAMENTE DEL PERFIL ---
                        val profileWeight = currentUserProfile?.weight ?: 0f
                        val profileHeight = currentUserProfile?.height ?: 0f
                        val ageInt = currentUserProfile?.age

                        val hasData = profileWeight > 0f && profileHeight > 0f

                        if (!hasData) {
                            // Mensaje si faltan datos
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
                            // Cálculo automático
                            val (imc, category) = calculateIMC(profileWeight, profileHeight, ageInt)
                            val imcColor = getIMCColor(category)

                            Text(
                                text = "Resultado basado en Perfil:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // --- MUESTRA LA TARJETA DE RESULTADO INMEDIATAMENTE ---
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
                            // --- FIN DE LA TARJETA ---
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

                    // --- LÓGICA DE BOTONES DINÁMICOS Y MENÚ DESPLEGABLE ---

                    val availableMonths = getDistinctMonths(savedGlucoseEntries, savedFoodEntries, savedMedications)
                    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

                    if (isGeneratingReport) {
                        // Muestra el indicador de carga cuando está generando
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
                        // Mensaje si no hay datos
                        Text(
                            text = "No hay datos registrados para generar reportes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 1. Botón Principal (se usa como ancla para el menú)
                        Button(
                            onClick = { showMonthDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6D4C41), // Café Oscuro
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

                        // 2. Menú Desplegable
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

                                        // --- AÑADE ESTAS LÍNEAS ---
                                        selectedReportMonthText = "Reporte de $monthName"
                                        // --- FIN DE LA ADICIÓN ---

                                        // 3. Ejecuta la lógica de generación
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

                    // Botón de "Compartir" (solo aparece si ya se generó un reporte)
                    if (generatedReportFile != null && !isGeneratingReport) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- CONTENEDOR DE LOS DOS BOTONES ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // --- 1. BOTÓN DE DESCARGA ---
                            Button(
                                onClick = {
                                    generatedReportFile?.let { file ->
                                        val reportService = ReportGeneratorService(context)
                                        try {
                                            // Asumimos formato PDF para la descarga
                                            val success = reportService.downloadPdfFile(file)
                                            if (success) {
                                                Toast.makeText(context, "✅ Reporte descargado en: ${reportService.getDownloadPath()}", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "❌ Error al descargar el reporte", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                // Ocupa la mitad del espacio menos el padding
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6D4C41) // Café Oscuro
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Descargar")
                            }

                            // --- 2. BOTÓN DE COMPARTIR ---
                            Button(
                                onClick = {
                                    generatedReportFile?.let { file ->
                                        val reportService = ReportGeneratorService(context)
                                        reportService.sharePdfFile(file)
                                    }
                                },
                                modifier = Modifier.weight(1f), // Ocupa la otra mitad
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartir")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Diálogos de SettingsScreen
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
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = patientDiabetesType,
                        onValueChange = { patientDiabetesType = it },
                        label = { Text("Tipo de Diabetes") },
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
    // Estados para los campos de texto
    var foodDescription by rememberSaveable { mutableStateOf(entry.description) }
    var foodCalories by rememberSaveable { mutableStateOf(entry.calories?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Registro de Comida") },
        text = {
            Column {
                // Campo para editar la descripción
                OutlinedTextField(
                    value = foodDescription,
                    onValueChange = { foodDescription = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Campo para editar las calorías
                OutlinedTextField(
                    value = foodCalories,
                    onValueChange = { foodCalories = it },
                    label = { Text("Calorías") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (foodDescription.isNotBlank()) {
                        // Crea el objeto actualizado
                        val updatedEntry = entry.copy(
                            description = foodDescription,
                            calories = foodCalories.toIntOrNull()
                        )
                        onSave(updatedEntry) // Llama a la función de guardado
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

// ChatMessageItem
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
            val patientName = profile?.name.orEmpty().ifEmpty { "Usuario" } // <-- CORREGIDO
            val patientAge = profile?.age ?: 0 // Si edad es nula, usa 0
            val patientWeight = profile?.weight
            val patientHeight = profile?.height
            val patientDiabetesType = profile?.diabetesType.orEmpty().ifEmpty { "Tipo 2" }

            val reportFile = reportService.generateMonthlyReport(
                glucoseEntries = glucoseEntries,
                foodEntries = foodEntries,
                medications = medications,
                emergencyContacts = contacts,
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

// formatDate
fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

// calculateCaloriesFromDescription
fun calculateCaloriesFromDescription(description: String, mealType: String): Int {
    val lowerDescription = description.lowercase()
    var baseCalories: Int
    
    baseCalories = when (mealType) {
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

// calculateDailyCalories (CORREGIDO para usar 'calories')
fun calculateDailyCalories(foodEntries: List<FoodEntry>): Int {
    val today = java.time.LocalDate.now()
    return foodEntries
        .filter { 
            val entryDate = java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            entryDate == today
        }
        .sumOf { it.calories ?: 0 } // <-- Corregido
}

// getRandomHealthTip
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

// calculateIMC
fun calculateIMC(weight: Float, height: Float, age: Int? = null): Pair<Float, String> {
    val heightSquared = height * height
    val imc = weight / heightSquared
    
    println("IMC calculado: $imc")
    
    val category = when {
        imc < 18.5f -> "Bajo de peso"
        imc < 25.0f -> "Saludable"
        imc < 30.0f -> "Sobrepeso"
        imc < 35.0f -> "Obesidad Clase 1"
        imc < 40.0f -> "Obesidad Clase 2"
        else -> "Obesidad Clase 3"
    }
    
    println("Categoría asignada: $category")
    return Pair(imc, category)
}

// getIMCColor
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

/**
 * Comprueba si un timestamp (en milisegundos) pertenece a un año y mes específicos.
 */
private fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
}

/**
 * Revisa todas las listas de datos y devuelve una lista de meses únicos (Año, Mes)
 * donde hay al menos un registro.
 */
private fun getDistinctMonths(
    glucose: List<GlucoseEntry>,
    food: List<FoodEntry>,
    meds: List<Medication>
): List<Pair<Int, Int>> {
    val distinctMonths = mutableSetOf<Pair<Int, Int>>()
    val cal = Calendar.getInstance()

    // Usamos endDate de meds, timestamp de las otras
    (glucose.map { it.timestamp } + food.map { it.timestamp } + meds.map { it.endDate }).forEach { timestamp ->
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        distinctMonths.add(Pair(year, month))
    }

    // Devuelve la lista ordenada, del más reciente al más antiguo
    return distinctMonths.sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
}