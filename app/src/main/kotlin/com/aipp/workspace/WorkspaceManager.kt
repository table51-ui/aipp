package com.aipp.workspace

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Represents a file or folder in the workspace.
 */
data class WorkspaceFile(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val size: Long = 0,
    val lastModified: Long = 0
)

/**
 * Represents detected project type and metadata.
 */
data class ProjectInfo(
    val type: ProjectType,
    val name: String,
    val description: String
)

enum class ProjectType {
    ANDROID_GRADLE,
    REACT_NATIVE,
    WEB,
    FLUTTER,
    KOTLIN,
    UNKNOWN
}

/**
 * Manages workspace: folder selection, file operations, project detection.
 * 
 * Uses Android Storage Access Framework (SAF) for secure folder access.
 * No MANAGE_EXTERNAL_STORAGE permission required.
 * 
 * File I/O is offloaded to Dispatchers.IO for safety.
 */
class WorkspaceManager(
    private val context: Context,
    private val logger: (tag: String, message: String) -> Unit
) {
    
    // Maximum safe file size: 1MB
    companion object {
        private const val MAX_FILE_SIZE = 1024L * 1024L // 1MB
    }
    
    private val _selectedFolderUri = MutableStateFlow<Uri?>(null)
    val selectedFolderUri: StateFlow<Uri?> = _selectedFolderUri.asStateFlow()
    
    private val _folderName = MutableStateFlow<String?>(null)
    val folderName: StateFlow<String?> = _folderName.asStateFlow()
    
    private val _projectInfo = MutableStateFlow<ProjectInfo?>(null)
    val projectInfo: StateFlow<ProjectInfo?> = _projectInfo.asStateFlow()
    
    private val _fileTree = MutableStateFlow<List<WorkspaceFile>>(emptyList())
    val fileTree: StateFlow<List<WorkspaceFile>> = _fileTree.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Set the workspace folder from SAF URI.
     */
    suspend fun setWorkspaceFolder(uri: Uri) {
        try {
            _isLoading.emit(true)
            logger("WORKSPACE", "Setting workspace folder: $uri")
            
            // Verify URI is a directory
            val docFile = DocumentFile.fromTreeUri(context, uri)
            if (docFile == null || !docFile.isDirectory) {
                logger("WORKSPACE", "ERROR: Invalid or non-directory URI")
                _isLoading.emit(false)
                return
            }
            
            _selectedFolderUri.emit(uri)
            _folderName.emit(docFile.name ?: "Workspace")
            
            // Detect project type
            detectProjectType(docFile)
            
            // Build file tree
            val files = enumerateFiles(docFile, maxDepth = 3)
            _fileTree.emit(files)
            
            logger("WORKSPACE", "Workspace loaded: ${files.size} items")
            _isLoading.emit(false)
        } catch (e: Exception) {
            logger("WORKSPACE", "ERROR loading workspace: ${e.message}")
            _isLoading.emit(false)
        }
    }
    
    /**
     * Detect project type by examining folder structure.
     */
    private suspend fun detectProjectType(docFile: DocumentFile) {
        val files = docFile.listFiles().map { it.name ?: "" }.toSet()
        
        val projectType = when {
            files.contains("build.gradle") || files.contains("build.gradle.kts") -> {
                ProjectType.ANDROID_GRADLE
            }
            files.contains("app.json") || files.contains("package.json") && files.contains("react-native") -> {
                ProjectType.REACT_NATIVE
            }
            files.contains("package.json") -> {
                ProjectType.WEB
            }
            files.contains("pubspec.yaml") -> {
                ProjectType.FLUTTER
            }
            files.contains("build.gradle.kts") || files.contains("build.gradle") -> {
                ProjectType.KOTLIN
            }
            else -> ProjectType.UNKNOWN
        }
        
        val info = ProjectInfo(
            type = projectType,
            name = docFile.name ?: "Unknown",
            description = "Detected: ${projectType.name}"
        )
        
        _projectInfo.emit(info)
        logger("WORKSPACE", "Project type detected: ${projectType.name}")
    }
    
    /**
     * Recursively enumerate files in folder.
     */
    private suspend fun enumerateFiles(
        docFile: DocumentFile,
        maxDepth: Int,
        currentDepth: Int = 0,
        path: String = ""
    ): List<WorkspaceFile> {
        if (currentDepth >= maxDepth) return emptyList()
        
        val files = mutableListOf<WorkspaceFile>()
        
        try {
            val children = docFile.listFiles()
            for (child in children) {
                // Skip hidden files
                if (child.name?.startsWith(".") == true) continue
                // Skip common build folders
                if (child.name in listOf("node_modules", "build", ".gradle", "dist", ".git")) continue
                
                val childPath = if (path.isEmpty()) child.name ?: "" else "$path/${child.name}"
                
                files.add(
                    WorkspaceFile(
                        uri = child.uri,
                        name = child.name ?: "Unknown",
                        isDirectory = child.isDirectory,
                        path = childPath,
                        size = if (child.isFile) child.length() else 0,
                        lastModified = child.lastModified()
                    )
                )
                
                // Recurse into directories
                if (child.isDirectory && currentDepth < maxDepth - 1) {
                    files.addAll(enumerateFiles(child, maxDepth, currentDepth + 1, childPath))
                }
            }
        } catch (e: Exception) {
            logger("WORKSPACE", "Error enumerating files: ${e.message}")
        }
        
        return files
    }
    
    /**
     * Read file content (for small files only).
     * Enforces MAX_FILE_SIZE limit.
     * 
     * Executes on Dispatchers.IO to prevent main thread blocking.
     */
    suspend fun readFileContent(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                
                // Read with size limit
                stream.use { inputStream ->
                    val buffer = ByteArray(8192)
                    val output = StringBuilder()
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytesRead += bytesRead
                        
                        if (totalBytesRead > MAX_FILE_SIZE) {
                            logger("WORKSPACE", "File exceeds safe limit ($MAX_FILE_SIZE bytes), truncating")
                            output.append(String(buffer, 0, bytesRead))
                            break
                        }
                        
                        output.append(String(buffer, 0, bytesRead))
                    }
                    
                    output.toString()
                }
            } catch (e: Exception) {
                logger("WORKSPACE", "Error reading file: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Clear workspace.
     */
    suspend fun clearWorkspace() {
        _selectedFolderUri.emit(null)
        _folderName.emit(null)
        _projectInfo.emit(null)
        _fileTree.emit(emptyList())
        logger("WORKSPACE", "Workspace cleared")
    }
}
