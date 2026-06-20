package com.example.ui

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StudyAppUI(viewModel: StudyViewModel) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isAILoading by viewModel.isAILoading.collectAsStateWithLifecycle()
    val aiRecommendationText by viewModel.aiRecommendationText.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Dashboard") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show AI status snackbars dynamically
    LaunchedEffect(aiRecommendationText, aiError) {
        aiRecommendationText?.let {
            snackbarHostState.showSnackbar(it)
        }
        aiError?.let {
            snackbarHostState.showSnackbar("AI Error: $it")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val tabs = listOf(
                    Triple("Dashboard", Icons.Filled.Home, Icons.Filled.Home),
                    Triple("Tasks", Icons.Filled.List, Icons.Filled.List),
                    Triple("Focus", Icons.Filled.PlayArrow, Icons.Filled.PlayArrow),
                    Triple("Courses", Icons.Filled.Check, Icons.Filled.Check),
                    Triple("Exams", Icons.Filled.Notifications, Icons.Filled.Notifications)
                )

                tabs.forEach { (name, filledIcon, outlinedIcon) ->
                    val isSelected = activeTab == name
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = name },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = name
                            )
                        },
                        label = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_tab_$name")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            // Study Header with Theme Selector and Live Ambient Music Controller
            AppStudyHeader(viewModel = viewModel)

            // AI Progress indicator overlay if loaded
            if (isAILoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Main Active Content Screen Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    },
                    label = "tab_animation"
                ) { targetState ->
                    when (targetState) {
                        "Dashboard" -> DashboardScreen(viewModel) { activeTab = it }
                        "Tasks" -> TasksScreen(viewModel)
                        "Focus" -> PomodoroScreen(viewModel)
                        "Courses" -> CoursesScreen(viewModel)
                        "Exams" -> ExamsScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppStudyHeader(viewModel: StudyViewModel) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isVoiceDialogShowing by remember { mutableStateOf(false) }
    var isProfileDialogShowing by remember { mutableStateOf(false) }

    val isBento = currentTheme == "Bento Grid"

    val initials = remember(profileName) {
        if (profileName.isNotBlank()) {
            val words = profileName.trim().split("\\s+".toRegex())
            if (words.size >= 2) {
                "${words[0].take(1)}${words[1].take(1)}".uppercase()
            } else {
                profileName.take(2).uppercase()
            }
        } else {
            "IS"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .shadow(if (isBento) 0.dp else 4.dp, if (isBento) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isBento) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = if (isBento) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBento) "STUDY PERFORMANCE DECK" else "Academic Desk",
                        style = if (isBento) MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF6750A4)
                        ) else MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Hey, $profileName",
                        style = if (isBento) MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C1B1F)
                        ) else MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isBento) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Voice recognition micro quick trigger & User Avatar initial
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isVoiceDialogShowing = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isBento) Color(0xFFEADDFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .testTag("voice_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "AI Assistant Trigger",
                            tint = if (isBento) Color(0xFF21005D) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Rounded Avatar loading uploaded image or initials
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isBento) Color(0xFFEADDFF) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { isProfileDialogShowing = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoUri.isNullOrBlank()) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(model = Uri.parse(profilePhotoUri!!)),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isBento) Color(0xFF21005D) else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // Interactive Voice dialog supporting voice/text simulations seamlessly
    if (isVoiceDialogShowing) {
        VoiceInputDialogSim(
            onDismiss = { isVoiceDialogShowing = false },
            onCommandResult = { text ->
                viewModel.processVoiceCommand(text)
                isVoiceDialogShowing = false
            }
        )
    }

    if (isProfileDialogShowing) {
        ProfileEditDialog(
            viewModel = viewModel,
            onDismiss = { isProfileDialogShowing = false }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileEditDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val currentName by viewModel.profileName.collectAsStateWithLifecycle()
    val currentEmail by viewModel.profileEmail.collectAsStateWithLifecycle()
    val currentAge by viewModel.profileAge.collectAsStateWithLifecycle()
    val currentPhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val currentProfession by viewModel.profileProfession.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    var nameState by remember { mutableStateOf(currentName) }
    var emailState by remember { mutableStateOf(currentEmail) }
    var ageState by remember { mutableStateOf(currentAge) }
    var photoUriState by remember { mutableStateOf(currentPhotoUri) }
    var professionState by remember { mutableStateOf(currentProfession) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriState = uri.toString()
        }
    }

    val isNameValid = nameState.isNotBlank()
    val isAgeValid = ageState.isNotBlank()
    val isFormValid = isNameValid && isAgeValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Edit Student Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Editable Photo representation
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUriState.isNullOrBlank()) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(model = Uri.parse(photoUriState!!)),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = "Default avatar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text("Upload", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = { Text("Student Name (* Mandatory)") },
                        isError = !isNameValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = emailState,
                        onValueChange = { emailState = it },
                        label = { Text("Gmail Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = ageState,
                        onValueChange = { ageState = it },
                        label = { Text("Age (* Mandatory)") },
                        isError = !isAgeValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Profession Category Pick selection
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "My Profession Category (*):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val professions = listOf("Student", "Software Developer", "Engineer", "Banking", "Other")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            professions.forEach { p ->
                                val isSelected = professionState == p
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { professionState = p },
                                    label = { Text(p, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // Academic Theme Picker option embedded inside Profile Dialog
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Academic Mood Theme:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val themes = listOf("Cosmic Slate", "Emerald Study", "Lavender Chill", "Light Mode", "Dark Mode")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            themes.forEach { t ->
                                val isActive = currentTheme == t
                                FilterChip(
                                    selected = isActive,
                                    onClick = { viewModel.setTheme(t) },
                                    label = { Text(t, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                if (!isFormValid) {
                    item {
                        Text(
                            text = "* Name and Age are mandatory fields",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (isFormValid) {
                                    viewModel.updateProfile(
                                        name = nameState,
                                        email = emailState,
                                        age = ageState,
                                        photoUri = photoUriState,
                                        profession = professionState
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Profile", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Dialog providing simulation trigger list and microphone/text input options
@Composable
fun VoiceInputDialogSim(onDismiss: () -> Unit, onCommandResult: (String) -> Unit) {
    val context = LocalContext.current
    var simText by remember { mutableStateOf("") }
    var voiceRecordingStatusText by remember { mutableStateOf("Ready to receive vocal or text command...") }
    var isListeningStateActive by remember { mutableStateOf(false) }
    var typedQuery by remember { mutableStateOf("") }

    val helper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { txt ->
                onCommandResult(txt)
            },
            onError = { err ->
                voiceRecordingStatusText = "Voice recognition error: $err"
                isListeningStateActive = false
            },
            onStateChange = { active ->
                isListeningStateActive = active
                if (active) {
                    voiceRecordingStatusText = "Listening for task commands..."
                    AudioPlayerHelper.playZenBell()
                } else {
                    voiceRecordingStatusText = "Recording finished, analyzing text speech..."
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isListeningStateActive) {
                helper.stopListening()
            } else {
                helper.startListening()
            }
        } else {
            voiceRecordingStatusText = "Permission denied: Recording microphone access is required."
        }
    }

    DisposableEffect(Unit) {
        onDispose { helper.release() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Personal Specialist",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListeningStateActive) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .clickable {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                if (isListeningStateActive) {
                                    helper.stopListening()
                                } else {
                                    helper.startListening()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListeningStateActive) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = "Voice Record mic",
                        modifier = Modifier.size(32.dp),
                        tint = if (isListeningStateActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = voiceRecordingStatusText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))
                
                // TYPE AND SEARCH / ASK COMPONENT (User Request feature!)
                Text(
                    text = "Type and Chat or Search:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = typedQuery,
                    onValueChange = { typedQuery = it },
                    placeholder = { Text("Ask study tips or search commands...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (typedQuery.isNotBlank()) {
                                    onCommandResult(typedQuery)
                                }
                            },
                            enabled = typedQuery.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send text prompt to AI",
                                tint = if (typedQuery.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Quick simulated preset commands:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))
                val voicePresets = listOf(
                    "How to study better using Feynman technique?",
                    "High priority assignment: Study chemistry organic formulas",
                    "Personal task: Purchase clean laundry detergent"
                )

                voicePresets.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onCommandResult(preset) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "\"$preset\"",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// --- TAB A: Quick dialog helpers for DashboardScreen ---
@Composable
fun QuickAttendanceLogDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean) -> Unit
) {
    var selectedCourseIndex by remember { mutableStateOf(0) }
    var isPresent by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Attendance Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (courses.isEmpty()) {
                    Text(
                        text = "Please add at least one subject/course in the Courses tab before tracking attendance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("Dismiss")
                    }
                } else {
                    Text(
                        text = "Select subject and mark present or absent:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        courses.forEachIndexed { idx, course ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCourseIndex = idx },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCourseIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = course.name,
                                        fontWeight = if (selectedCourseIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (selectedCourseIndex == idx) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isPresent = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPresent) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Present")
                            }
                        }

                        Button(
                            onClick = { isPresent = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isPresent) Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Absent")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(courses[selectedCourseIndex].id, isPresent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Log Status")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyGoalConfigDialog(
    initialTarget: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var pickerTarget by remember { mutableStateOf(initialTarget) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Configure Daily Task Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Set how many task completions you aim for daily. Staying consistent keeps your study index high!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = { if (pickerTarget > 1) pickerTarget-- },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrement", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Text(
                        text = "$pickerTarget Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(
                        onClick = { pickerTarget++ },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increment", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(pickerTarget)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Target")
                    }
                }
            }
        }
    }
}

// --- TAB A: Dashboard View ---
@Composable
fun DashboardScreen(viewModel: StudyViewModel, onTabSelected: (String) -> Unit) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val dailyGoalTarget by viewModel.dailyGoalTarget.collectAsStateWithLifecycle()
    val focusTimeMinutes by viewModel.totalFocusTimeMinutes.collectAsStateWithLifecycle()
    val aiRecommendationText by viewModel.aiRecommendationText.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    var isAttendanceDialogShowing by remember { mutableStateOf(false) }
    var isGoalDialogShowing by remember { mutableStateOf(false) }

    // Scroll state tracking to minimize assistant when scrolling up
    val listState = rememberLazyListState()
    val isScrollingUp = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
    var forceAIExpanded by remember { mutableStateOf(false) }

    // Reset manual toggle when returning to top of screen
    LaunchedEffect(isScrollingUp) {
        if (!isScrollingUp) {
            forceAIExpanded = false
        }
    }

    // Determine tasks completed today
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val completedToday = tasks.filter { it.isCompleted && (it.completionTime ?: 0) >= todayStart }
    val progressPercent = if (dailyGoalTarget > 0) {
        (completedToday.size.toFloat() / dailyGoalTarget).coerceIn(0f, 1f)
    } else 0f

    val isBento = currentTheme == "Bento Grid"

    // Help Dialog launchers
    if (isAttendanceDialogShowing) {
        QuickAttendanceLogDialog(
            courses = courses,
            onDismiss = { isAttendanceDialogShowing = false },
            onSave = { courseId, isPresent ->
                viewModel.markAttendance(courseId, isPresent)
                isAttendanceDialogShowing = false
            }
        )
    }

    if (isGoalDialogShowing) {
        DailyGoalConfigDialog(
            initialTarget = dailyGoalTarget,
            onDismiss = { isGoalDialogShowing = false },
            onSave = { newTarget ->
                viewModel.setDailyGoalTarget(newTarget)
                isGoalDialogShowing = false
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcome Text
        item {
            Text(
                text = if (isBento) "Bento Activity Deck" else "Welcome back, Scholar!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = if (isBento) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onBackground
            )
        }

        // Row containing double Bento panels
        item {
            ExamBentoBlock(exams, courses, currentTheme)
        }

        // Midsection layout containing focus metrics and goals side-by-side or stacked as grids
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PomodoroBentoBlock(focusTimeMinutes, currentTheme) {
                        onTabSelected("Focus")
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AttendanceBentoBlock(courses, attendanceRecords, currentTheme) {
                        isAttendanceDialogShowing = true
                    }
                    DailyGoalBentoBlock(completedToday.size, dailyGoalTarget, progressPercent, currentTheme) {
                        isGoalDialogShowing = true
                    }
                }
            }
        }

        // Bottom AI Recommendation Bento block
        item {
            AIBentoBlock(
                viewModel = viewModel,
                recommendationText = aiRecommendationText,
                currentTheme = currentTheme,
                isMinimized = isScrollingUp && !forceAIExpanded,
                onToggleExpand = { forceAIExpanded = !forceAIExpanded }
            )
        }
    }
}

// --- TAB B: Task Manager / AI Study Planner ---
@Composable
fun TasksScreen(viewModel: StudyViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()

    var isAddingTaskShowing by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }
    var selectedTaskForEdit by remember { mutableStateOf<Task?>(null) }

    // Filtration computation
    val filteredTasks = tasks.filter { task ->
        val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory.equals("All", ignoreCase = true) || 
                task.category.trim().equals(selectedCategory.trim(), ignoreCase = true)
        val matchesPriority = selectedPriority.equals("All", ignoreCase = true) || 
                task.priority.trim().equals(selectedPriority.trim(), ignoreCase = true)
        matchesSearch && matchesCategory && matchesPriority
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Toolbar with Quick Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("task_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { isAddingTaskShowing = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Task")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Navigation Row
        Text(text = "Category Filter:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cats = listOf("All", "Study", "Work", "Personal", "Shopping")
            items(cats) { cat ->
                val isActive = selectedCategory == cat
                FilterChip(
                    selected = isActive,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    modifier = Modifier.testTag("cat_filter_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List View
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Empty state icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your schedule is clear! Create a task to track assignments or study intervals.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current
                    
                    var suggestedPriorityState by remember { mutableStateOf<String?>(null) }
                    var reasoningState by remember { mutableStateOf<String?>(null) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTaskForDetail = task }
                            .shadow(2.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Completed & Title & Priority Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(0.7f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateTaskCompletion(task, isChecked)
                                        },
                                        modifier = Modifier.testTag("task_checkbox_${task.id}")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val badgeColor = when (task.priority) {
                                        "High" -> Color(0xFFEF4444)
                                        "Medium" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = task.priority,
                                            color = badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            // Optional Description Note
                            if (task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }

                            // Secondary meta info labels: Course, Category, Due date
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val courseIndicator = courses.find { it.id == task.subjectId }
                                    if (courseIndicator != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(android.graphics.Color.parseColor(courseIndicator.colorHex)).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = courseIndicator.code,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(android.graphics.Color.parseColor(courseIndicator.colorHex))
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = task.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Due Date indicator
                                val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                                Text(
                                    text = "Due: " + sdf.format(Date(task.dueDate)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.dueDate < System.currentTimeMillis() && !task.isCompleted) Color(0xFFEF4444)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            // Intelligent AI Task Helpers buttons
                            if (!task.isCompleted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. AI Breakdown sparks button
                                    Button(
                                        onClick = { viewModel.requestTaskBreakdown(task) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "AI Breakdown subtasks spark",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Breakdown", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 2. AI Priority recommend analyzer
                                    Button(
                                        onClick = {
                                            viewModel.requestSmartPrioritySuggestion(task) { level, reason ->
                                                suggestedPriorityState = level
                                                reasoningState = reason
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                            contentColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("AI Priority Suggest", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Trash clean action
                                    IconButton(
                                        onClick = { viewModel.deleteTask(task) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete task action",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Interactive display of suggested priority if evaluated
                                if (suggestedPriorityState != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Spark logo",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "AI Suggests: $suggestedPriorityState Priority",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = reasoningState ?: "",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            TextButton(
                                                onClick = {
                                                    viewModel.updateTask(task.copy(priority = suggestedPriorityState!!))
                                                    suggestedPriorityState = null
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.align(Alignment.End).height(24.dp)
                                            ) {
                                                Text("Apply Suggested", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingTaskShowing) {
        AddTaskDialog(
            courses = courses,
            onDismiss = { isAddingTaskShowing = false },
            onSave = { title, desc, prio, cat, isAss, spaceId, rec, interval, pickedDueDate ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    priority = prio,
                    category = cat,
                    isAssignment = isAss,
                    subjectId = spaceId,
                    isRecurring = rec,
                    recurringInterval = interval,
                    dueDate = pickedDueDate
                )
                isAddingTaskShowing = false
            }
        )
    }

    if (selectedTaskForDetail != null) {
        TaskDetailDialog(
            task = selectedTaskForDetail!!,
            courses = courses,
            onDismiss = { selectedTaskForDetail = null },
            onEditClick = {
                selectedTaskForEdit = selectedTaskForDetail
                selectedTaskForDetail = null
            },
            onDeleteClick = {
                viewModel.deleteTask(selectedTaskForDetail!!)
                selectedTaskForDetail = null
            }
        )
    }

    if (selectedTaskForEdit != null) {
        EditTaskDialog(
            task = selectedTaskForEdit!!,
            courses = courses,
            onDismiss = { selectedTaskForEdit = null },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                selectedTaskForEdit = null
            }
        )
    }
}

// dialog facilitating creation of custom tasks, assignment flags and courses linkages
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean, Int?, Boolean, String?, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var category by remember { mutableStateOf("Study") }
    var isAssignment by remember { mutableStateOf(false) }
    var subjectId by remember { mutableStateOf<Int?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringInterval by remember { mutableStateOf("daily") }
    
    var selectedDueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000L) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Plan New Task/Assignment",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title (*)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_task_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes & descriptions Inside Tasks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Select Priority:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val prios = listOf("High", "Medium", "Low")
                        prios.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val cats = listOf("Study", "Work", "Personal", "Shopping")
                        cats.forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Is This An Assignment?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = isAssignment, onCheckedChange = { isAssignment = it })
                    }
                }

                item {
                    Text("Linked Subject/Course (Optional):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        courses.forEach { course ->
                            val isSelected = subjectId == course.id
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { subjectId = if (isSelected) null else course.id },
                                label = { Text(course.code, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDueDate }
                    val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        
                        val timeSetListener = android.app.TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            selectedDueDate = calendar.timeInMillis
                        }
                        android.app.TimePickerDialog(
                            context,
                            timeSetListener,
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }

                    Button(
                        onClick = {
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date and Time", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Due Date/Time Picker", fontSize = 11.sp)
                    }
                    Text(
                        text = "Selected: " + SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(selectedDueDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recurring Auto-Regenerate Task?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    }
                }

                if (isRecurring) {
                    item {
                        Text("Interval:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val intervals = listOf("daily", "weekly")
                            intervals.forEach { intv ->
                                FilterChip(
                                    selected = recurringInterval == intv,
                                    onClick = { recurringInterval = intv },
                                    label = { Text(intv, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(
                                        title,
                                        description,
                                        priority,
                                        category,
                                        isAssignment,
                                        subjectId,
                                        isRecurring,
                                        if (isRecurring) recurringInterval else null,
                                        selectedDueDate
                                    )
                                }
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Plan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailDialog(
    task: Task,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Task Information Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = task.title,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val badgeColor = when (task.priority) {
                        "High" -> Color(0xFFEF4444)
                        "Medium" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Priority: ${task.priority}", color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = task.category, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (task.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Description Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = task.description,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val linkedCourse = courses.find { it.id == task.subjectId }
                if (linkedCourse != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(android.graphics.Color.parseColor(linkedCourse.colorHex)).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(linkedCourse.colorHex)))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Linked Course: ${linkedCourse.name} (${linkedCourse.code})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(android.graphics.Color.parseColor(linkedCourse.colorHex))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Due constraints", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Due Constraint: " + SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(task.dueDate)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Task")
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Close") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit details", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit details")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTaskDialog(
    task: Task,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var priority by remember { mutableStateOf(task.priority) }
    var category by remember { mutableStateOf(task.category) }
    var isAssignment by remember { mutableStateOf(task.isAssignment) }
    var subjectId by remember { mutableStateOf(task.subjectId) }
    var isRecurring by remember { mutableStateOf(task.isRecurring) }
    var recurringInterval by remember { mutableStateOf(task.recurringInterval ?: "daily") }
    
    var selectedDueDate by remember { mutableStateOf(task.dueDate) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Edit Planned Task",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title (*)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes & descriptions Inside Tasks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Select Priority:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val prios = listOf("High", "Medium", "Low")
                        prios.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val cats = listOf("Study", "Work", "Personal", "Shopping")
                        cats.forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Is This An Assignment?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = isAssignment, onCheckedChange = { isAssignment = it })
                    }
                }

                item {
                    Text("Linked Subject/Course (Optional):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        courses.forEach { course ->
                            val isSelected = subjectId == course.id
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { subjectId = if (isSelected) null else course.id },
                                label = { Text(course.code, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDueDate }
                    val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        
                        val timeSetListener = android.app.TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            selectedDueDate = calendar.timeInMillis
                        }
                        android.app.TimePickerDialog(
                            context,
                            timeSetListener,
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }

                    Button(
                        onClick = {
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date and Time", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Due Date/Time Picker", fontSize = 11.sp)
                    }
                    Text(
                        text = "Selected: " + SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(selectedDueDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recurring Auto-Regenerate Task?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    }
                }

                if (isRecurring) {
                    item {
                        Text("Interval:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val intervals = listOf("daily", "weekly")
                            intervals.forEach { intv ->
                                FilterChip(
                                    selected = recurringInterval == intv,
                                    onClick = { recurringInterval = intv },
                                    label = { Text(intv, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(
                                        task.copy(
                                            title = title,
                                            description = description,
                                            priority = priority,
                                            category = category,
                                            isAssignment = isAssignment,
                                            subjectId = subjectId,
                                            isRecurring = isRecurring,
                                            recurringInterval = if (isRecurring) recurringInterval else null,
                                            dueDate = selectedDueDate
                                        )
                                    )
                                }
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Update Plan")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB C: Pomodoro Focus Timer ---
@Composable
fun PomodoroScreen(viewModel: StudyViewModel) {
    val focusTimeLeft by viewModel.focusTimeLeft.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val mode by viewModel.timerMode.collectAsStateWithLifecycle()
    val customFocus by viewModel.customFocusDuration.collectAsStateWithLifecycle()
    val customBreak by viewModel.customBreakDuration.collectAsStateWithLifecycle()

    val totalDurationSeconds = if (mode == PomodoroMode.FOCUS) customFocus * 60 else customBreak * 60
    val progress = if (totalDurationSeconds > 0) focusTimeLeft.toFloat() / totalDurationSeconds else 0f

    val min = focusTimeLeft / 60
    val sec = focusTimeLeft % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", min, sec)

    val activeTimerColor = if (mode == PomodoroMode.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (mode == PomodoroMode.FOCUS) "Focus Arena Core" else "Well Deserved Break",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (mode == PomodoroMode.FOCUS) "Lock in study and block interruptions" else "Stand up, stretch and hydrate",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Circular Timer Visualiser
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                // Background track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // Timed progress bar
                drawArc(
                    color = activeTimerColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (mode == PomodoroMode.FOCUS) "FOCUS" else "BREAK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mode == PomodoroMode.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 2.sp
                )
            }
        }

        // Interactive Timer Controller buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Refresh button
                IconButton(
                    onClick = { viewModel.resetPomodoro() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset timer")
                }

                // Play/Pause button
                Button(
                    onClick = { if (isRunning) viewModel.pausePomodoro() else viewModel.startPomodoro() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == PomodoroMode.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.height(56.dp).width(140.dp).testTag("timer_toggle_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = "Control action"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Start",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Customizable duration setups Row Chips
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Adjust Durations (minutes):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        Pair("Sprint (20m)", 20),
                        Pair("Standard (25m)", 25),
                        Pair("Double study (45m)", 45)
                    )
                    presets.forEach { (label, duration) ->
                        val isSelected = customFocus == duration
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePomodoroDurations(duration, customBreak) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

// --- TAB D: My Course Hub ---
@Composable
fun CoursesScreen(viewModel: StudyViewModel) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()

    var isAddingCourseShowing by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Track Courses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Log attendance regularly to avoid alerts",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = { isAddingCourseShowing = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_course_btn")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add course")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Course")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Courses scrolling List
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No course registered yet. Form new academic subjects to trace performance.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(courses) { course ->
                    val records = attendanceRecords.filter { it.subjectId == course.id }
                    val presents = records.count { it.status == "PRESENT" }
                    val total = records.size
                    val percentage = if (total > 0) (presents.toFloat() / total * 100) else 100f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingCourse = course }
                            .shadow(2.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header matching colors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(course.colorHex)))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = course.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(android.graphics.Color.parseColor(course.colorHex))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Attendance indicators and stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Attendance Status: ${String.format(Locale.getDefault(), "%.1f", percentage)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (percentage < 75) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Class Logged: $presents / $total sessions total",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // PRESENT Action
                                    Button(
                                        onClick = { viewModel.markAttendance(course.id, true) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("present_${course.id}")
                                    ) {
                                        Text("Present", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // ABSENT Action
                                    Button(
                                        onClick = { viewModel.markAttendance(course.id, false) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("absent_${course.id}")
                                    ) {
                                        Text("Absent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Interactive historical attendance list row
                            if (records.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Recent history tracks:", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(records.take(5)) { rec ->
                                        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                                        val isPres = rec.status == "PRESENT"
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isPres) Color(0xFF10B981).copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                )
                                                .clickable { viewModel.deleteAttendanceRecord(rec) }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = sdf.format(Date(rec.date)) + " " + if (isPres) "✓" else "✗",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPres) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            // Secondary settings edit/trash action
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                                IconButton(
                                    onClick = { editingCourse = course },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit academic course",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.deleteCourse(course) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Drop database syllabus course",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingCourseShowing) {
        AddCourseDialog(
            onDismiss = { isAddingCourseShowing = false },
            onSave = { name, code, hex ->
                viewModel.addCourse(name, code, hex)
                isAddingCourseShowing = false
            }
        )
    }

    if (editingCourse != null) {
        EditCourseDialog(
            course = editingCourse!!,
            onDismiss = { editingCourse = null },
            onSave = { course ->
                viewModel.updateCourse(course)
                editingCourse = null
            },
            onDelete = { course ->
                viewModel.deleteCourse(course)
                editingCourse = null
            }
        )
    }
}

@Composable
fun EditCourseDialog(
    course: Course,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit
) {
    var name by remember { mutableStateOf(course.name) }
    var code by remember { mutableStateOf(course.code) }
    var selectedColor by remember { mutableStateOf(course.colorHex) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Course Syllabus",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name (*)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Department Code (e.g., CS-101)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Label Theme Palette:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                val colors = listOf("#4F46E5", "#06B6D4", "#10B981", "#F59E0B", "#F43F5E", "#8B5CF6")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = if (selectedColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDelete(course) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(course.copy(name = name, code = code, colorHex = selectedColor))
                                }
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCourseDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4F46E5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Course Syllabus",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name (*)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_course_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Department Code (e.g., CS-101)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Label Theme Palette:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                val colors = listOf("#4F46E5", "#06B6D4", "#10B981", "#F59E0B", "#F43F5E", "#8B5CF6")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = if (selectedColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, code, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Course")
                    }
                }
            }
        }
    }
}

// --- TAB E: Exam Countdowns ---
@Composable
fun ExamsScreen(viewModel: StudyViewModel) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var isAddingExamShowing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Exam Deadlines Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aptitude schedules and spaced repetition generation",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = { isAddingExamShowing = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_exam_btn")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add exam")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exam")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (exams.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exams on direct horizon! Add schedules when dates publish.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exams) { exam ->
                    val diff = exam.examDate - System.currentTimeMillis()
                    val daysRemaining = (diff / 86400000).coerceAtLeast(0)
                    val course = courses.find { it.id == exam.subjectId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exam.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Course: " + (course?.name ?: "General syllabus"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$daysRemaining Days",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (daysRemaining < 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = "Remaining", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (exam.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${exam.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Revision scheduler auto pilot button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.requestSpacedRepetitionSchedule(exam, course?.name ?: "Active Review Module") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Revision Planner",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Study Schedule Generator", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteExam(exam) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete Exam countdown",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingExamShowing) {
        AddExamDialog(
            courses = courses,
            onDismiss = { isAddingExamShowing = false },
            onSave = { title, writtenCourse, daysInAdvance, notes ->
                val futureDate = System.currentTimeMillis() + (daysInAdvance * 86400000L)
                val matched = courses.find { it.code.trim().equals(writtenCourse.trim(), ignoreCase = true) || it.name.trim().equals(writtenCourse.trim(), ignoreCase = true) }
                if (matched != null) {
                    viewModel.addExam(title, matched.id, futureDate, notes)
                } else if (writtenCourse.isNotBlank()) {
                    val codeClean = writtenCourse.trim()
                    viewModel.addCourse(name = codeClean, code = codeClean, colorHex = "#6750A4")
                    viewModel.addExam(title, null, futureDate, "$notes ($codeClean)")
                } else {
                    viewModel.addExam(title, null, futureDate, notes)
                }
                isAddingExamShowing = false
            }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddExamDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var courseSearchText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Calendar variable to save selected exam date
    var selectedExamDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 5 * 86400000L) }
    val context = LocalContext.current

    val computedDaysInAdvance = ((selectedExamDateMillis - System.currentTimeMillis()) / 86400000L).toInt().coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Track Exam Milestone",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Exam Name (*)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_exam_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Calendar Picker Integration for Days Select
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Exam date (*):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedExamDateMillis }
                        val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            selectedExamDateMillis = calendar.timeInMillis
                        }
                        
                        Button(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    dateSetListener,
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Exam Calendar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Calendar Date", fontSize = 11.sp)
                        }
                        
                        Text(
                            text = "Selected Exam Date: " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedExamDateMillis)) + " ($computedDaysInAdvance days in future)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Topic Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Subject / Course Selection box to write custom text
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = courseSearchText,
                            onValueChange = { courseSearchText = it },
                            label = { Text("Subject/Course Code (* Type/Write)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        if (courses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Quick Options (Tap to autofill):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                courses.forEach { course ->
                                    val isSelected = courseSearchText.trim().equals(course.code.trim(), ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { courseSearchText = course.code },
                                        label = { Text(course.code, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(title, courseSearchText, computedDaysInAdvance, notes)
                                }
                            },
                            enabled = title.isNotBlank() && courseSearchText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Exam")
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
//                       BENTO GRID DESIGN THEME BLOCKS
// =========================================================================

@Composable
fun ExamBentoBlock(
    exams: List<Exam>,
    courses: List<Course>,
    currentTheme: String
) {
    val isBento = currentTheme == "Bento Grid"
    
    val cardBg = if (isBento) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.surface
    val contentColor = if (isBento) Color(0xFF410002) else MaterialTheme.colorScheme.onSurface
    val shape = if (isBento) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    val borderStroke = if (isBento) BorderStroke(1.dp, Color(0xFFF9DEDC)) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isBento) 0.dp else 2.dp, shape),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = shape,
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBento) Color(0xFF410002) else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "URGENT",
                        color = if (isBento) Color.White else MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                
                if (exams.isNotEmpty()) {
                    val firstExam = exams.first()
                    val diff = firstExam.examDate - System.currentTimeMillis()
                    val daysRemaining = (diff / 86400000).coerceAtLeast(0)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DAYS LEFT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%02d", daysRemaining),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exams.isEmpty()) {
                Text(
                    text = "No upcoming exams registered yet.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = "Register details inside the syllabus tracker to activate reminders.",
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.6f)
                )
            } else {
                val firstExam = exams.first()
                val courseName = courses.find { it.id == firstExam.subjectId }?.name ?: "General Subject"
                Text(
                    text = firstExam.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    lineHeight = 24.sp
                )
                Text(
                    text = "$courseName • Hall ${firstExam.id % 500 + 102}",
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PomodoroBentoBlock(
    focusTimeMinutes: Int,
    currentTheme: String,
    onClick: () -> Unit = {}
) {
    val isBento = currentTheme == "Bento Grid"
    
    val cardBg = if (isBento) Color(0xFFD1E1FF) else MaterialTheme.colorScheme.surface
    val contentColor = if (isBento) Color(0xFF001D49) else MaterialTheme.colorScheme.onSurface
    val shape = if (isBento) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)

    val trackColor = if (isBento) Color(0xFFADC6FF) else Color.LightGray.copy(alpha = 0.3f)
    val indicatorColor = if (isBento) Color(0xFF005AC1) else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .clickable { onClick() }
            .shadow(if (isBento) 0.dp else 2.dp, shape),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STUDY FOCUS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            // Minimal radial circle
            Box(
                modifier = Modifier.size(65.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = indicatorColor,
                        startAngle = -90f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${focusTimeMinutes}m",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = contentColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isBento) Color(0xFF005AC1) else MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FOCUS",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun AttendanceBentoBlock(
    courses: List<Course>,
    attendanceRecords: List<AttendanceRecord>,
    currentTheme: String,
    onClick: () -> Unit = {}
) {
    val isBento = currentTheme == "Bento Grid"
    
    val cardBg = if (isBento) Color(0xFFE8DEF8) else MaterialTheme.colorScheme.surface
    val contentColor = if (isBento) Color(0xFF21005D) else MaterialTheme.colorScheme.onSurface
    val shape = if (isBento) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)

    // Calculate aggregated attendance
    val totalPresents = attendanceRecords.count { it.status == "PRESENT" }
    val totalRecords = attendanceRecords.size
    val aggregatePercent = if (totalRecords > 0) {
        (totalPresents.toFloat() / totalRecords * 100).toInt()
    } else 85

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .clickable { onClick() }
            .shadow(if (isBento) 0.dp else 2.dp, shape),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ATTENDANCE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$aggregatePercent%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isBento) Color(0xFFD0BCFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(1.5.dp, contentColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun DailyGoalBentoBlock(
    completedTasks: Int,
    targetTasks: Int,
    progressPercent: Float,
    currentTheme: String,
    onClick: () -> Unit = {}
) {
    val isBento = currentTheme == "Bento Grid"
    
    val cardBg = if (isBento) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isBento) Color(0xFF49454F) else MaterialTheme.colorScheme.onSurface
    val shape = if (isBento) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    val borderStroke = if (isBento) BorderStroke(1.dp, Color(0xFFCAC4D0)) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .clickable { onClick() }
            .shadow(if (isBento) 0.dp else 2.dp, shape),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = shape,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY GOAL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$completedTasks/$targetTasks Tasks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBento) Color(0xFF1C1B1F) else contentColor
                )
            }

            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(if (isBento) Color(0xFFE7E0EC) else Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progressPercent.coerceIn(0.1f, 1f))
                        .clip(CircleShape)
                        .background(if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun AIBentoBlock(
    viewModel: StudyViewModel,
    recommendationText: String?,
    currentTheme: String,
    isMinimized: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    val isBento = currentTheme == "Bento Grid"
    
    val cardBg = if (isBento) Color(0xFFF3EDF7) else MaterialTheme.colorScheme.surface
    val contentColor = if (isBento) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurface
    val shape = if (isBento) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    val borderStroke = if (isBento) BorderStroke(1.dp, Color(0xFFE7E0EC)) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .shadow(if (isBento) 0.dp else 2.dp, shape),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = shape,
        border = borderStroke
    ) {
        if (isMinimized) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 8.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Student Assistant (Minimized - Tap to expand)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Assistant",
                    tint = if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (isBento) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 16.dp, y = (-16).dp)
                            .size(96.dp)
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp).background(Color(0xFFD0BCFF).copy(alpha = 0.3f), CircleShape))
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "AI ASSISTANT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBento) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (recommendationText.isNullOrBlank()) "Break down Mini Project Prototype into sub-tasks?" else recommendationText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.dismissAIRecommendation() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("WRONG 👎", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.dismissAIRecommendation() },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF6750A4)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("LATER", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.generateQuickSubtasks(recommendationText ?: "Break down Mini Project Prototype into sub-tasks") },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("GENERATE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

