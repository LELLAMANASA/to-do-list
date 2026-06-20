package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.ui.StudyAppUI
import com.example.ui.StudyViewModel
import com.example.ui.StudyViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = StudyRepository(db)
        val factory = StudyViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[StudyViewModel::class.java]

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themeName = currentTheme) {
                StudyAppUI(viewModel = viewModel)
            }
        }
    }
}
