package com.hapticpath.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            viewModel.stopSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAudioPermission()

        setContent {
            MainAppContent(viewModel = viewModel)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val downloadState by viewModel.downloadState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (downloadState) {
                is DownloadState.Completed -> {
                    when (currentScreen) {
                        is CurrentScreen.Session -> {
                            TabNavigationContainer(viewModel = viewModel)
                        }
                        is CurrentScreen.Summary -> {
                            val summary by viewModel.sessionSummary.collectAsState()
                            SummaryScreen(
                                summary = summary,
                                onStartNewSession = { viewModel.startSession() }
                            )
                        }
                    }
                }
                else -> {
                    DownloadScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TabNavigationContainer(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFF00E676)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Session", color = if (selectedTab == 0) Color(0xFF00E676) else Color.Gray) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Inställningar", color = if (selectedTab == 1) Color(0xFF00E676) else Color.Gray) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> SessionScreen(viewModel = viewModel)
                1 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}