package com.aipp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.aipp.workspace.WorkspaceManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val logger = StructuredLogger()
    private lateinit var workspaceManager: WorkspaceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        logger.info("MAIN", "MainActivity.onCreate() starting")
        
        // Initialize subsystem manager
        SubsystemManager.register("logger", logger)
        logger.info("MAIN", "SubsystemManager initialized")
        
        // Initialize workspace manager
        workspaceManager = WorkspaceManager(this) { tag, message ->
            logger.info(tag, message)
        }
        SubsystemManager.register("workspace", workspaceManager)
        logger.info("MAIN", "WorkspaceManager initialized")
        
        // Log system info
        logger.info("APP", "Application starting offline-first")
        logger.info("APP", "SDK ${android.os.Build.VERSION.SDK_INT}, Device ${android.os.Build.DEVICE}")
        logger.info("APP", "Package ${packageName}")
        
        // Set Compose content
        setContent {
            MainScreen(logger, workspaceManager, this)
        }
        
        logger.info("MAIN", "UI rendered")
    }
}

@Composable
fun MainScreen(
    logger: StructuredLogger,
    workspaceManager: WorkspaceManager,
    activity: Activity
) {
    val logs by logger.logs.collectAsState()
    val selectedFolder by workspaceManager.selectedFolderUri.collectAsState()
    val folderName by workspaceManager.folderName.collectAsState()
    val projectInfo by workspaceManager.projectInfo.collectAsState()
    val fileTree by workspaceManager.fileTree.collectAsState()
    val isLoading by workspaceManager.isLoading.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            logger.info("UI", "Folder selected: $uri")
            activity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                workspaceManager.setWorkspaceFolder(uri)
            }
        }
    }
    
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "AIPP - AI Engineering Workspace",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Offline-First Mobile Development",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Open Folder")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Folder")
                    }
                }
                
                // Workspace info
                if (selectedFolder != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = folderName ?: "Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (projectInfo != null) {
                                Text(
                                    text = projectInfo!!.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = "Files: ${fileTree.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                // Main content tabs
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("Files", "Logs")
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> FileTreeView(fileTree, isLoading, selectedFolder == null)
                        1 -> LogViewer(logs)
                    }
                }
            }
        }
    }
}

@Composable
fun FileTreeView(
    files: List<com.aipp.workspace.WorkspaceFile>,
    isLoading: Boolean,
    isEmpty: Boolean
) {
    when {
        isEmpty -> {
            Text(
                text = "No folder selected. Click 'Open Folder' to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
        files.isEmpty() -> {
            Text(
                text = "No files found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(files) { file ->
                    FileItemView(file)
                }
            }
        }
    }
}

@Composable
fun FileItemView(file: com.aipp.workspace.WorkspaceFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = file.isDirectory) { }
            .padding(8.dp)
    ) {
        Text(
            text = if (file.isDirectory) "📁" else "📄",
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!file.isDirectory && file.size > 0) {
                Text(
                    text = "${file.size / 1024} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LogViewer(logs: List<StructuredLogger.LogEntry>) {
    Box(
        modifier = Modifier.fillMaxSize()
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
