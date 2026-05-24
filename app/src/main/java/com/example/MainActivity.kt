package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.repository.StoryRepository
import com.example.ui.StoryScreen
import com.example.ui.StoryViewModel
import com.example.ui.StoryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Initialize SQLite cache Database
    val database = AppDatabase.getDatabase(applicationContext)
    val storyDao = database.storyDao()

    // 2. Initialize Network APIs & Repository Pattern
    val hackerNewsApi = RetrofitClient.hackerNewsApi
    val geminiApi = RetrofitClient.geminiApi
    val repository = StoryRepository(hackerNewsApi, geminiApi, storyDao)

    // 3. Thread-safe standard lifecycle ViewModel setup
    val viewModel = ViewModelProvider(
      this,
      StoryViewModelFactory(repository)
    )[StoryViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          StoryScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

