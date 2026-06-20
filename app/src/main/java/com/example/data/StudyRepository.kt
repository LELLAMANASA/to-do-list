package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val db: AppDatabase) {
    private val courseDao = db.courseDao()
    private val taskDao = db.taskDao()
    private val attendanceDao = db.attendanceRecordDao()
    private val examDao = db.examDao()
    private val focusSessionDao = db.focusSessionDao()

    // Course Operations
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    suspend fun insertCourse(course: Course) = courseDao.insertCourse(course)
    suspend fun deleteCourse(course: Course) {
        // Cascade delete attendance records
        attendanceDao.deleteRecordsForSubject(course.id)
        courseDao.deleteCourse(course)
    }
    suspend fun getCourseById(id: Int): Course? = courseDao.getCourseById(id)

    // Task & Assignment Operations
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        val completionTime = if (isCompleted) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(taskId, isCompleted, completionTime)
    }

    // Attendance Operations
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
    fun getRecordsBySubject(subjectId: Int): Flow<List<AttendanceRecord>> = attendanceDao.getRecordsBySubject(subjectId)
    suspend fun insertAttendanceRecord(record: AttendanceRecord) = attendanceDao.insertRecord(record)
    suspend fun deleteAttendanceRecord(record: AttendanceRecord) = attendanceDao.deleteRecord(record)

    // Exam Operations
    val allExams: Flow<List<Exam>> = examDao.getAllExams()
    suspend fun insertExam(exam: Exam) = examDao.insertExam(exam)
    suspend fun deleteExam(exam: Exam) = examDao.deleteExam(exam)

    // Focus Session Operations
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    suspend fun insertSession(session: FocusSession) = focusSessionDao.insertSession(session)
    val totalFocusTime: Flow<Int?> = focusSessionDao.getTotalFocusTime()
}
