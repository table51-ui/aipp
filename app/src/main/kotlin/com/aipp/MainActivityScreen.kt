package com.aipp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipp.core.logging.StructuredLogger
import kotlinx.coroutines.delay

/**
 * Main UI screen using Jetpack Compose.
 * 
 * Displays:
 * - App status
 * - Real-time logs
 * - Initialization state
 */
@Composable
fun MainActivityScreen(logger: StructuredLogger) {
    
    // State
    var logs by remember { mutableStateOf(emptyList<StructuredLogger.LogEntry>()) }
    var appStatus by remember { mutableStateOf("Initializing...") }
    
    // Update logs every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            logs = logger.getBufferedLogs()
            appStatus = "Running - ${logs.size} logs"
            delay(500)
        }
    }
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "AIPP - Offline-First Mobile AI Engineering Workspace",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = appStatus,
                            fontSize = 12.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                }
                
                // Logs
                Text(
                    text = "Runtime Logs (${logs.size} entries)",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1E1E1E))
                        .padding(4.dp)
                ) {
                    items(logs) { entry ->
                        LogEntryView(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryView(entry: StructuredLogger.LogEntry) {
    val levelColor = when (entry.level) {
        StructuredLogger.LogLevel.DEBUG -> Color(0xFF90CAF9)
        StructuredLogger.LogLevel.INFO -> Color(0xFF81C784)
        StructuredLogger.LogLevel.WARN -> Color(0xFFFFB74D)
        StructuredLogger.LogLevel.ERROR -> Color(0xFFE57373)
    }
    
    Text(
        text = entry.toString(),
        fontSize = 10.sp,
        color = levelColor,
        modifier = Modifier.padding(vertical = 2.dp),
        maxLines = 3
    )
}
