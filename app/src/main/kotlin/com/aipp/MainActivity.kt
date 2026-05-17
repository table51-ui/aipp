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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.aipp.editor.EditorManager
import com.aipp.workspace.WorkspaceManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val logger = StructuredLogger()
    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var editorManager: EditorManager
    
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
        
        // Initialize editor manager
        editorManager = EditorManager(this) { tag, message ->
            logger.info(tag, message)
        }
        SubsystemManager.register("editor", editorManager)
        logger.info("MAIN", "EditorManager initialized")
        
        // Log system info
        logger.info("APP", "Application starting offline-first")
        logger.info("APP", "SDK ${android.os.Build.VERSION.SDK_INT}, Device ${android.os.Build.DEVICE}")
        logger.info("APP", "Package ${packageName}")
        
        // Set Compose content
        setContent {
            MainScreen(logger, workspaceManager, editorManager, this)
        }
        
        logger.info("MAIN", "UI rendered")
    }
}

@Composable
fun MainScreen(
    logger: StructuredLogger,
    workspaceManager: WorkspaceManager,
    editorManager: EditorManager,
    activity: Activity
) {
    val logs by logger.logs.collectAsState()
    val selectedFolder by workspaceManager.selectedFolderUri.collectAsState()
    val folderName by workspaceManager.folderName.collectAsState()
    val projectInfo by workspaceManager.projectInfo.collectAsState()
    val fileTree by workspaceManager.fileTree.collectAsState()
    val workspaceLoading by workspaceManager.isLoading.collectAsState()
    
    val openTabs by editorManager.openTabs.collectAsState()
    val activeTabId by editorManager.activeTabId.collectAsState()
    
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "AIPP - Engineering Workspace",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = folderName ?: "No folder selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Open Folder")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Folder")
                        }
                    }
                )
                
                // Main content area
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Left: File explorer
                    Column(
                        modifier = Modifier
                            .weight(0.3f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Files (${fileTree.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            FileExplorer(
                                files = fileTree,
                                isLoading = workspaceLoading,
                                isEmpty = selectedFolder == null,
                                onFileSelected = { file ->
                                    scope.launch {
                                        editorManager.openFile(file.uri, file.name)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Right: Editor and logs
                    Column(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight()
                    ) {
                        // Editor tabs
                        if (openTabs.isNotEmpty()) {
                            EditorTabs(
                                tabs = openTabs,
                                activeTabId = activeTabId,
                                onTabSelected = { tabId ->
                                    scope.launch {
                                        editorManager.switchTab(tabId)
                                    }
                                },
                                onTabClosed = { tabId ->
                                    scope.launch {
                                        editorManager.closeTab(tabId)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                        
                        // Editor + Logs tabs
                        var selectedTab by remember { mutableIntStateOf(0) }
                        val tabs = listOf("Editor", "Logs")
                        
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        
                        // Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    val activeTab = openTabs.find { it.id == activeTabId }
                                    if (activeTab != null) {
                                        EditorView(activeTab)
                                    } else {
                                        Text(
                                            text = "No file open",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                1 -> LogViewer(logs)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileExplorer(
    files: List<com.aipp.workspace.WorkspaceFile>,
    isLoading: Boolean,
    isEmpty: Boolean,
    onFileSelected: (com.aipp.workspace.WorkspaceFile) -> Unit
) {
    when {
        isEmpty -> {
            Text(
                text = "Open a folder to browse files",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            )
        }
        files.isEmpty() -> {
            Text(
                text = "No files found",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                items(files) { file ->
                    FileItem(
                        file = file,
                        onSelected = { onFileSelected(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileItem(
    file: com.aipp.workspace.WorkspaceFile,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!file.isDirectory) onSelected() }
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
                color = if (file.isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!file.isDirectory && file.size > 0) {
                Text(
                    text = "${file.size / 1024} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EditorTabs(
    tabs: List<com.aipp.editor.EditorTab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        tabs.forEach { tab ->
            Button(
                onClick = { onTabSelected(tab.id) },
                modifier = Modifier
                    .padding(2.dp)
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTabId == tab.id) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = tab.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                IconButton(
                    onClick = { onTabClosed(tab.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorView(tab: com.aipp.editor.EditorTab) {
    when {
        tab.isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            )
        }
        tab.error != null -> {
            Text(
                text = "Error: ${tab.error}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        else -> {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val lines = tab.content.split("\n")
                    val lineNumbers = (1..lines.size).map { it.toString() }
                    
                    items(lines.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            // Line number
                            Text(
                                text = lineNumbers[index],
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .width(40.dp)
                                    .padding(end = 8.dp),
                                fontSize = 10.sp
                            )
                            
                            // Code line
                            Text(
                                text = lines[index],
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogViewer(logs: List<StructuredLogger.LogEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Text(
                    text = "No logs yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs.size) { index ->
                val log = logs[index]
                Text(
                    text = log.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (log.level) {
                        StructuredLogger.LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                        StructuredLogger.LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                        StructuredLogger.LogLevel.WARN -> MaterialTheme.colorScheme.secondary
                        StructuredLogger.LogLevel.ERROR -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(2.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}
