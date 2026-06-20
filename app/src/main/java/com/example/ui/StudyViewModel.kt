package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

enum class PomodoroMode { FOCUS, BREAK }

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    // --- Core Database flows ---
    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<Exam>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<FocusSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusTimeMinutes: StateFlow<Int> = repository.totalFocusTime
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- State UI controls ---
    // Premium themes support: Custom Sunset, Study Emerald, Cosmic Slate, Lavender Chill
    private val _currentTheme = MutableStateFlow("Bento Grid")
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow("All")
    val selectedPriorityFilter: StateFlow<String> = _selectedPriorityFilter.asStateFlow()

    // --- Daily Goals ---
    private val _dailyGoalTarget = MutableStateFlow(5)
    val dailyGoalTarget: StateFlow<Int> = _dailyGoalTarget.asStateFlow()

    // --- Pomodoro Timer State ---
    private val _focusTimeLeft = MutableStateFlow(1500) // 25 mins in seconds
    val focusTimeLeft: StateFlow<Int> = _focusTimeLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(PomodoroMode.FOCUS)
    val timerMode: StateFlow<PomodoroMode> = _timerMode.asStateFlow()

    private val _customFocusDuration = MutableStateFlow(25) // minutes
    val customFocusDuration: StateFlow<Int> = _customFocusDuration.asStateFlow()

    private val _customBreakDuration = MutableStateFlow(5) // minutes
    val customBreakDuration: StateFlow<Int> = _customBreakDuration.asStateFlow()

    private var pomodoroJob: Job? = null

    // --- Background Ambient Music Music State ---
    private val _isAmbientPlaying = MutableStateFlow(false)
    val isAmbientPlaying: StateFlow<Boolean> = _isAmbientPlaying.asStateFlow()

    private val _ambientVolume = MutableStateFlow(0.4f)
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    // --- Gemini AI State ---
    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiRecommendationText = MutableStateFlow<String?>(null)
    val aiRecommendationText: StateFlow<String?> = _aiRecommendationText.asStateFlow()

    // --- Speech Voice State ---
    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _voiceResult = MutableStateFlow<String?>(null)
    val voiceResult: StateFlow<String?> = _voiceResult.asStateFlow()

    // --- Profile & Photo States (Persisted) ---
    private val prefs = application.getSharedPreferences("student_profile_prefs", Context.MODE_PRIVATE)

    private val _profileName = MutableStateFlow(prefs.getString("profile_name", "Ishaan") ?: "Ishaan")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileEmail = MutableStateFlow(prefs.getString("profile_email", "lellamanasa07@gmail.com") ?: "lellamanasa07@gmail.com")
    val profileEmail: StateFlow<String> = _profileEmail.asStateFlow()

    private val _profileAge = MutableStateFlow(prefs.getString("profile_age", "20") ?: "20")
    val profileAge: StateFlow<String> = _profileAge.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(prefs.getString("profile_photo_uri", null))
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private val _profileProfession = MutableStateFlow(prefs.getString("profile_profession", "Student") ?: "Student")
    val profileProfession: StateFlow<String> = _profileProfession.asStateFlow()

    fun copyImageToLocal(uriString: String): String? {
        try {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, "profile_photo_persisted.jpg")
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            inputStream.close()
            return Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun updateProfile(name: String, email: String, age: String, photoUri: String?, profession: String = "Student") {
        var finalPhotoUri = photoUri
        if (photoUri != null && photoUri.startsWith("content://")) {
            val copied = copyImageToLocal(photoUri)
            if (copied != null) {
                finalPhotoUri = copied
            }
        }
        _profileName.value = name
        _profileEmail.value = email
        _profileAge.value = age
        _profilePhotoUri.value = finalPhotoUri
        _profileProfession.value = profession
        prefs.edit().apply {
            putString("profile_name", name)
            putString("profile_email", email)
            putString("profile_age", age)
            putString("profile_photo_uri", finalPhotoUri)
            putString("profile_profession", profession)
            apply()
        }
    }

    init {
        // Init block - Ambient music autoplay is disabled to preserve user preference
    }

    // --- Theme Control ---
    fun setTheme(theme: String) {
        _currentTheme.value = theme
    }

    // --- Search & Filters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setPriorityFilter(priority: String) {
        _selectedPriorityFilter.value = priority
    }

    fun setDailyGoalTarget(target: Int) {
        _dailyGoalTarget.value = target
    }

    // --- CRUD Operations Course ---
    fun addCourse(name: String, code: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCourse(Course(name = name, code = code, colorHex = colorHex))
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCourse(course)
        }
    }

    // --- CRUD Operations Task ---
    fun addTask(
        title: String,
        description: String = "",
        priority: String = "Medium",
        dueDate: Long = System.currentTimeMillis() + 86400000,
        category: String = "Study",
        isAssignment: Boolean = false,
        subjectId: Int? = null,
        isRecurring: Boolean = false,
        recurringInterval: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(
                Task(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    category = category,
                    isAssignment = isAssignment,
                    subjectId = subjectId,
                    isRecurring = isRecurring,
                    recurringInterval = recurringInterval
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(task)
        }
    }

    fun updateTaskCompletion(task: Task, isNowCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaskCompletion(task.id, isNowCompleted)
            
            if (isNowCompleted) {
                // Play energizing visual milestone chime!
                withContext(Dispatchers.Main) {
                    AudioPlayerHelper.playCompletionChime()
                }

                // Handle task regeneration if recurring
                if (task.isRecurring && task.recurringInterval != null) {
                    val offset = when (task.recurringInterval) {
                        "daily" -> 86400000L
                        "weekly" -> 7 * 86400000L
                        else -> 86400000L // Default tomorrow
                    }
                    val newDueDate = task.dueDate + offset
                    repository.insertTask(
                        task.copy(
                            id = 0, // Generate new ID
                            dueDate = newDueDate,
                            isCompleted = false,
                            completionTime = null
                        )
                    )
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    // --- CRUD Operations Attendance ---
    fun markAttendance(subjectId: Int, isPresent: Boolean, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = if (isPresent) "PRESENT" else "ABSENT"
            repository.insertAttendanceRecord(
                AttendanceRecord(subjectId = subjectId, date = date, status = status)
            )
        }
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttendanceRecord(record)
        }
    }

    // --- CRUD Operations Exam ---
    fun addExam(title: String, subjectId: Int?, date: Long, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExam(Exam(title = title, subjectId = subjectId, examDate = date, notes = notes))
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExam(exam)
        }
    }

    // --- Pomodoro Action System ---
    fun startPomodoro() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        pomodoroJob = viewModelScope.launch {
            while (_isTimerRunning.value && _focusTimeLeft.value > 0) {
                delay(1000)
                _focusTimeLeft.value -= 1
            }
            if (_focusTimeLeft.value == 0) {
                // Period complete! Play Zen Bell frequency notification
                AudioPlayerHelper.playZenBell()
                transitionPomodoroMode()
            }
        }
    }

    fun pausePomodoro() {
        _isTimerRunning.value = false
        pomodoroJob?.cancel()
        pomodoroJob = null
    }

    fun resetPomodoro() {
        pausePomodoro()
        val defaultMinutes = if (_timerMode.value == PomodoroMode.FOCUS) _customFocusDuration.value else _customBreakDuration.value
        _focusTimeLeft.value = defaultMinutes * 60
    }

    fun updatePomodoroDurations(focusMins: Int, breakMins: Int) {
        _customFocusDuration.value = focusMins.coerceAtLeast(1)
        _customBreakDuration.value = breakMins.coerceAtLeast(1)
        resetPomodoro()
    }

    private fun transitionPomodoroMode() {
        pausePomodoro()
        if (_timerMode.value == PomodoroMode.FOCUS) {
            // Log active focus session to Room DB database
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertSession(
                    FocusSession(
                        durationMinutes = _customFocusDuration.value,
                        category = "Pomodoro"
                    )
                )
            }
            _timerMode.value = PomodoroMode.BREAK
            _focusTimeLeft.value = _customBreakDuration.value * 60
        } else {
            _timerMode.value = PomodoroMode.FOCUS
            _focusTimeLeft.value = _customFocusDuration.value * 60
        }
        // Auto-start next transition cycle automatically
        startPomodoro()
    }

    // --- Ambient Music Management ---
    fun toggleAmbientMusic(explicitStart: Boolean? = null) {
        val nextState = explicitStart ?: !_isAmbientPlaying.value
        _isAmbientPlaying.value = nextState
        if (nextState) {
            AudioPlayerHelper.startAmbientDrone(_ambientVolume.value)
        } else {
            AudioPlayerHelper.stopAmbientDrone()
        }
    }

    fun setAmbientMusicVolume(volume: Float) {
        _ambientVolume.value = volume
        AudioPlayerHelper.setVolume(volume)
    }

    // --- Speech/Voice Input Integration ---
    fun setListeningState(isListening: Boolean) {
        _isVoiceListening.value = isListening
    }

    fun processVoiceCommand(text: String) {
        _voiceResult.value = text
        _isAILoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            val profession = _profileProfession.value
            val systemPrompt = """
                You are a friendly, highly intelligent Productivity AI Assistant specializing in supporting someone in the profession/role: $profession.
                Analyze the user's vocal/text input instruction: "$text".
                Determine if the user's intent is to:
                1. Create or schedule a professional, academic, or personal task matching their profession ($profession) or standard chores. If so, set "type" as "TASK".
                2. Ask a general question, search tips, or chat related to their field ($profession) or productivity. If so, set "type" as "CHAT".

                Respond ONLY with a valid JSON object matching this schema:
                {
                  "type": "TASK" or "CHAT",
                  "title": "A short descriptive title suitable for a task",
                  "description": "Any brief details or advice personalized for a $profession",
                  "priority": "High", "Medium", or "Low",
                  "category": "Study", "Work", "Personal", or "Shopping",
                  "isAssignment": true/false,
                  "reply": "A beautiful, deeply encouraging, helpful response tailored to their profession ($profession) or their question, utilizing professional lingo, clean tips or study/work methods (e.g., Feynman technique, pomodoro, agile sprints) if appropriate."
                }

                Respond ONLY with a valid JSON object. No extra text or markdown code blocks wrapping it.
            """.trimIndent()

            try {
                val aiResponseRaw = GeminiClient.generateAIResponse(systemPrompt, isJson = true)
                if (!aiResponseRaw.isNullOrBlank()) {
                    val cleanJsonStr = aiResponseRaw.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()
                    
                    val obj = JSONObject(cleanJsonStr)
                    val type = obj.optString("type", "TASK")
                    val reply = obj.optString("reply", "")

                    if (type.equals("CHAT", ignoreCase = true)) {
                        _aiRecommendationText.value = reply
                    } else {
                        val t = obj.optString("title", text)
                        val desc = obj.optString("description", "")
                        val prio = obj.optString("priority", "Medium")
                        val cat = obj.optString("category", "Study")
                        val isAss = obj.optBoolean("isAssignment", false)

                        withContext(Dispatchers.IO) {
                            repository.insertTask(
                                Task(
                                    title = t,
                                    description = desc,
                                    priority = prio,
                                    category = cat,
                                    isAssignment = isAss,
                                    dueDate = System.currentTimeMillis() + 86400000 // tomorrow
                                )
                            )
                        }
                        
                        _aiRecommendationText.value = if (reply.isNotBlank()) reply else "Created AI Task: \"$t\" ($cat, Priority: $prio)"
                    }
                } else {
                    processRegexVoiceCommand(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                processRegexVoiceCommand(text)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun generateQuickSubtasks(promptText: String) {
        _isAILoading.value = true
        _aiError.value = null
        _aiRecommendationText.value = null

        viewModelScope.launch {
            val targetTaskTitle = if (promptText.contains("Mini Project", ignoreCase = true)) {
                "Mini Project Prototype"
            } else {
                promptText.replace("?", "").replace("Break down ", "").trim()
            }

            // Create parent task
            val parentTask = Task(
                title = targetTaskTitle,
                description = "Complex coursework topic analyzed by AI",
                priority = "High",
                category = "Study",
                isAssignment = true
            )
            
            try {
                val parentId = withContext(Dispatchers.IO) {
                    repository.insertTask(parentTask)
                }
                
                val prompt = """
                    Analyze this academic task: "$targetTaskTitle".
                    As a student specialist, break down this complex task into an ordered sequence of 3 to 4 clear, actionable sub-tasks.
                    Provide the result ONLY as a JSON array of strings representing the sub-tasks titles. Do not include markdown code block syntax. No extra text structure.
                    Example structure:
                    [
                      "Research core project guidelines",
                      "Draft structural database layout",
                      "Build core prototype features",
                      "Perform final testing"
                    ]
                """.trimIndent()

                val apiResponse = GeminiClient.generateAIResponse(prompt, isJson = true)
                if (!apiResponse.isNullOrBlank()) {
                    val cleanJsonStr = apiResponse.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()
                    
                    val array = JSONArray(cleanJsonStr)
                    withContext(Dispatchers.IO) {
                        for (i in 0 until array.length()) {
                            val subTaskTitle = array.getString(i)
                            repository.insertTask(
                                Task(
                                    title = "Subtask: $subTaskTitle",
                                    description = "Subtask generated for \"$targetTaskTitle\"",
                                    priority = "Medium",
                                    category = "Study",
                                    isAssignment = false,
                                    subjectId = parentId.toInt(),
                                    dueDate = System.currentTimeMillis() + (i + 1) * 3600000 * 2
                                )
                            )
                        }
                    }
                    _aiRecommendationText.value = "Successfully generated ${array.length()} subtasks for \"$targetTaskTitle\"!"
                } else {
                    // Fallback
                    generateFallbackSubtasks(targetTaskTitle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                generateFallbackSubtasks(targetTaskTitle)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun generateFallbackSubtasks(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val steps = listOf("Planning & initial research", "Core feature implementation", "Validation & testing")
            steps.forEach { step ->
                repository.insertTask(
                    Task(
                        title = "Subtask: $step",
                        description = "Subtask generated for \"$title\"",
                        priority = "Medium",
                        category = "Study",
                        isAssignment = false
                    )
                )
            }
            _aiRecommendationText.value = "Fallback subtasks created successfully for \"$title\"!"
        }
    }

    private fun processRegexVoiceCommand(text: String) {
        // Fallback robust local regex analyzer
        val lower = text.lowercase()
        var priority = "Medium"
        if (lower.contains("urgent") || lower.contains("high") || lower.contains("important")) {
            priority = "High"
        } else if (lower.contains("low") || lower.contains("easy")) {
            priority = "Low"
        }

        var category = "Study"
        if (lower.contains("personal") || lower.contains("home") || lower.contains("clean")) {
            category = "Personal"
        } else if (lower.contains("buy") || lower.contains("store") || lower.contains("shopping")) {
            category = "Shopping"
        } else if (lower.contains("work") || lower.contains("job")) {
            category = "Work"
        }

        val isAssignment = lower.contains("assignment") || lower.contains("homework") || lower.contains("project")

        addTask(
            title = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            description = "Captured via speech commands",
            priority = priority,
            category = category,
            isAssignment = isAssignment
        )
        _aiRecommendationText.value = "Created Voice Task: \"$text\""
    }

    fun dismissAIRecommendation() {
        _aiRecommendationText.value = null
        _aiError.value = null
    }

    // --- AI Feature 1: Intelligent Subtask Breakdown ---
    fun requestTaskBreakdown(parentTask: Task) {
        _isAILoading.value = true
        _aiError.value = null
        _aiRecommendationText.value = null

        viewModelScope.launch {
            val prompt = """
                Analyze this academic task: "${parentTask.title}" (Description: "${parentTask.description}").
                As a student specialist, break down this complex task into an ordered sequence of 3 to 5 clear, actionable subtasks.
                Provide the result ONLY as a JSON array of strings representing the subtasks titles. Do not include markdown code block syntax.
                Example structure:
                [
                  "Read introduction chapters",
                  "Outline main points of reference",
                  "Draft first section and review"
                ]
            """.trimIndent()

            try {
                val response = GeminiClient.generateAIResponse(prompt, isJson = true)
                if (!response.isNullOrBlank()) {
                    val cleanJsonStr = response.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()
                    
                    val array = JSONArray(cleanJsonStr)
                    withContext(Dispatchers.IO) {
                        for (i in 0 until array.length()) {
                            val subtaskTitle = array.getString(i)
                            // Add subtasks staggered by hours
                            repository.insertTask(
                                Task(
                                    title = "Subtask: $subtaskTitle",
                                    description = "Subtask generated for \"${parentTask.title}\"",
                                    priority = parentTask.priority,
                                    category = parentTask.category,
                                    isAssignment = parentTask.isAssignment,
                                    subjectId = parentTask.subjectId,
                                    dueDate = parentTask.dueDate - ((array.length() - i) * 7200000) // Spaced out prior to parent due date
                                )
                            )
                        }
                    }
                    _aiRecommendationText.value = "AI successfully broke down task into ${array.length()} subtasks and saved them to your planner!"
                } else {
                    _aiError.value = "No response from Gemini API models."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiError.value = "Error: ${e.localizedMessage ?: "Failed to generate smart subtasks"}"
            } finally {
                _isAILoading.value = false
            }
        }
    }

    // --- AI Feature 2: Smart Priority Recommendation ---
    fun requestSmartPrioritySuggestion(task: Task, onSuggested: (String, String) -> Unit) {
        _isAILoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dueStr = dateFormat.format(Date(task.dueDate))
            
            val prompt = """
                Recommend the optimal academic priority level (strictly "High", "Medium", or "Low") and provide a one-sentence reasoning for the student task:
                Title: "${task.title}"
                Description: "${task.description}"
                Due Date: $dueStr
                
                Respond strictly as a JSON object containing keys "suggestedPriority" and "reasoning".
                Example:
                {
                  "suggestedPriority": "High",
                  "reasoning": "This assignment represents a heavy grade weight and has an upcoming immediate deadline."
                }
            """.trimIndent()

            try {
                val response = GeminiClient.generateAIResponse(prompt, isJson = true)
                if (!response.isNullOrBlank()) {
                    val cleanJsonStr = response.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()
                    val obj = JSONObject(cleanJsonStr)
                    val level = obj.optString("suggestedPriority", "Medium")
                    val reason = obj.optString("reasoning", "Recommended by priority assistant.")
                    
                    onSuggested(level, reason)
                } else {
                    _aiError.value = "No priority recommendation from intelligence service."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiError.value = e.localizedMessage ?: "AI analysis failed."
            } finally {
                _isAILoading.value = false
            }
        }
    }

    // --- AI Feature 3: Spaced-Repetition Study Schedule Generator ---
    fun requestSpacedRepetitionSchedule(exam: Exam, courseName: String) {
        _isAILoading.value = true
        _aiError.value = null
        _aiRecommendationText.value = null

        viewModelScope.launch {
            val examDateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            val examDateStr = examDateFormat.format(Date(exam.examDate))

            val prompt = """
                You are a cognitive psychology and student coaching assistant.
                Design a Spaced Repetition Study Schedule preparing for the exam "${exam.title}" in course "$courseName" which is on $examDateStr.
                Define exactly 3 micro study tasks over the days leading up to the exam utilizing active study methodologies (e.g., active recall, spaced repetition).
                
                Respond ONLY as a valid JSON array of objects representing these tasks. Return no wrapping markdown formatting.
                Schema of each object in the array:
                {
                  "title": "Short title of review activity",
                  "description": "Short explanation of active study method to use",
                  "daysBeforeExam": 1
                }
                
                Make daysBeforeExam progressive numbers like 3, 2, 1.
            """.trimIndent()

            try {
                val response = GeminiClient.generateAIResponse(prompt, isJson = true)
                if (!response.isNullOrBlank()) {
                    val cleanJsonStr = response.trim()
                        .removePrefix("```json")
                        .removeSuffix("```")
                        .trim()
                    
                    val array = JSONArray(cleanJsonStr)
                    withContext(Dispatchers.IO) {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val title = obj.getString("title")
                            val desc = obj.getString("description")
                            val daysBefore = obj.getInt("daysBeforeExam")
                            
                            val studyDate = exam.examDate - (daysBefore * 86400000L)
                            
                            repository.insertTask(
                                Task(
                                    title = "Study Prep: $title",
                                    description = "$desc [Scheduled spaced repetition]",
                                    priority = "High",
                                    category = "Study",
                                    dueDate = studyDate,
                                    subjectId = exam.subjectId
                                )
                            )
                        }
                    }
                    _aiRecommendationText.value = "AI created a spaced repetition roadmap with ${array.length()} tasks in your planner to prepare for $courseName!"
                } else {
                    _aiError.value = "Could not produce scheduled reviews."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiError.value = e.localizedMessage ?: "Failed to program spaced study schedule."
            } finally {
                _isAILoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AudioPlayerHelper.stopAmbientDrone()
    }
}

// --- Factory Configuration ---
class StudyViewModelFactory(
    private val application: Application,
    private val repository: StudyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
