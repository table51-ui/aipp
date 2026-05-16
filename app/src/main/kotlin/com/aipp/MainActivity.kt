package com.aipp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    
    private val logger = StructuredLogger()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        logger.info("MAIN", "MainActivity.onCreate() starting")
        
        // Initialize subsystem manager
        SubsystemManager.register("logger", logger)
        logger.info("MAIN", "SubsystemManager initialized")
        
        // Log system info
        logger.info("APP", "Application starting offline-first")
        logger.info("APP", "SDK ${android.os.Build.VERSION.SDK_INT}, Device ${android.os.Build.DEVICE}")
        logger.info("APP", "Package ${packageName}")
        
        // Set Compose content
        setContent {
            MainScreen(logger)
        }
        
        logger.info("MAIN", "UI rendered")
    }
}

@Composable
fun MainScreen(logger: StructuredLogger) {
    val logs by logger.logs.collectAsState()
    
    LaunchedEffect(Unit) {
        logger.info("UI", "MainScreen composed")
    }
    
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "AIPP - AI Engineering Workspace (Offline)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Status
                Text(
                    text = "Status: Running (Offline)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Logs (${logs.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Log display
                LogViewer(logs = logs)
            }
        }
    }
}

@Composable
fun LogViewer(logs: List<StructuredLogger.LogEntry>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        if (logs.isEmpty()) {
            Text(
                text = "No logs yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs.size) { index ->
                    val log = logs[index]
                    Text(
                        text = log.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (log.level) {
                            StructuredLogger.LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                            StructuredLogger.LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                            StructuredLogger.LogLevel.WARN -> MaterialTheme.colorScheme.secondary
                            StructuredLogger.LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
