package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String = "",
    val colorHex: String = "#4F46E5" // Default indigo accent
) : Serializable

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: String = "Medium", // High, Medium, Low
    val dueDate: Long = System.currentTimeMillis() + 86400000, // Default tomorrow
    val category: String = "Study", // Study, Work, Personal, Shopping
    val isCompleted: Boolean = false,
    val isAssignment: Boolean = false,
    val subjectId: Int? = null, // Link to course
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null, // "daily", "weekly", "custom"
    val completionTime: Long? = null
) : Serializable

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val date: Long, // Date timestamp in millis
    val status: String // "PRESENT" or "ABSENT"
) : Serializable

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subjectId: Int? = null,
    val examDate: Long,
    val notes: String = ""
) : Serializable

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int? = null,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String? = "Focus"
) : Serializable
