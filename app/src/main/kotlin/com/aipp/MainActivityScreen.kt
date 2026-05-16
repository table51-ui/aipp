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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main UI screen using Jetpack Compose.
 * 
 * Displays:
 * - App status
 * - Real-time logs with color-coding
 */
@Composable
fun MainActivityScreen(logger: StructuredLogger) {
    
    // State for logs and status
    var logs by remember { mutableStateOf(emptyList<StructuredLogger.LogEntry>()) }
    var appStatus by remember { mutableStateOf("Initializing...") }
    
    // Update logs every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            logs = logger.getBufferedLogs()
            appStatus = "Running - ${logs.size} logs collected"
            delay(500)
        }
    }
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6200EE),
            surface = Color(0xFF121212),
            background = Color(0xFF121212)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "AIPP - Offline-First Engineering Workspace",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appStatus,
                            fontSize = 12.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                }
                
                // Logs Section Label
                Text(
                    text = "Runtime Logs (${logs.size} entries)",
                    fontSize = 13.sp,
                    color = Color(0xFFB0BEC5),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Logs List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0D0D0D))
                        .padding(4.dp)
                ) {
                    items(logs) { logEntry ->
                        LogLine(logEntry)
                    }
                }
            }
        }
    }
}

/**
 * Single log line component
 */
@Composable
fun LogLine(entry: StructuredLogger.LogEntry) {
    val levelColor = when (entry.level) {
        StructuredLogger.LogLevel.DEBUG -> Color(0xFF90CAF9)   // Blue
        StructuredLogger.LogLevel.INFO -> Color(0xFF81C784)    // Green
        StructuredLogger.LogLevel.WARN -> Color(0xFFFFB74D)    // Orange
        StructuredLogger.LogLevel.ERROR -> Color(0xFFE57373)   // Red
    }
    
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val timeStr = timeFormat.format(Date(entry.timestamp))
    val logText = "[$timeStr] [${entry.subsystem}] [${entry.level}] ${entry.message}"
    
    Text(
        text = logText,
        fontSize = 10.sp,
        color = levelColor,
        modifier = Modifier.padding(vertical = 1.dp),
        maxLines = 2
    )
}
