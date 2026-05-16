package com.aipp.editor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents an open editor tab.
 */
data class EditorTab(
    val id: String,
    val fileName: String,
    val fileUri: Uri,
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val language: String = detectLanguage(fileName)
) {
    companion object {
        fun detectLanguage(fileName: String): String {
            return when {
                fileName.endsWith(".kt") -> "kotlin"
                fileName.endsWith(".java") -> "java"
                fileName.endsWith(".ts") -> "typescript"
                fileName.endsWith(".tsx") -> "typescript"
                fileName.endsWith(".js") -> "javascript"
                fileName.endsWith(".jsx") -> "javascript"
                fileName.endsWith(".xml") -> "xml"
                fileName.endsWith(".json") -> "json"
                fileName.endsWith(".gradle") -> "gradle"
                fileName.endsWith(".gradle.kts") -> "kotlin"
                fileName.endsWith(".py") -> "python"
                fileName.endsWith(".sh") -> "bash"
                fileName.endsWith(".md") -> "markdown"
                fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> "yaml"
                else -> "text"
            }
        }
    }
}

/**
 * Manages open editor tabs and file content.
 */
class EditorManager(
    private val context: Context,
    private val logger: (tag: String, message: String) -> Unit
) {
    
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()
    
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    
    private val tabMap = mutableMapOf<String, EditorTab>()
    
    /**
     * Open a file in editor.
     */
    suspend fun openFile(fileUri: Uri, fileName: String) {
        try {
            val tabId = fileUri.toString().hashCode().toString()
            
            // Check if already open
            if (tabMap.containsKey(tabId)) {
                _activeTabId.emit(tabId)
                logger("EDITOR", "File already open: $fileName")
                return
            }
            
            // Create loading tab
            val loadingTab = EditorTab(
                id = tabId,
                fileName = fileName,
                fileUri = fileUri,
                isLoading = true
            )
            
            tabMap[tabId] = loadingTab
            updateTabsFlow()
            logger("EDITOR", "Loading file: $fileName")
            
            // Read file content
            val content = readFileContent(fileUri)
            if (content != null) {
                val loadedTab = loadingTab.copy(
                    content = content,
                    isLoading = false
                )
                tabMap[tabId] = loadedTab
                _activeTabId.emit(tabId)
                updateTabsFlow()
                logger("EDITOR", "File opened: $fileName (${content.length} chars)")
            } else {
                val errorTab = loadingTab.copy(
                    isLoading = false,
                    error = "Failed to read file"
                )
                tabMap[tabId] = errorTab
                updateTabsFlow()
                logger("EDITOR", "ERROR: Failed to read $fileName")
            }
        } catch (e: Exception) {
            logger("EDITOR", "ERROR opening file: ${e.message}")
        }
    }
    
    /**
     * Close a tab.
     */
    suspend fun closeTab(tabId: String) {
        tabMap.remove(tabId)
        updateTabsFlow()
        
        // Switch to another tab if needed
        if (_activeTabId.value == tabId) {
            val nextTab = tabMap.values.firstOrNull()
            _activeTabId.emit(nextTab?.id)
        }
        
        logger("EDITOR", "Tab closed: $tabId")
    }
    
    /**
     * Switch active tab.
     */
    suspend fun switchTab(tabId: String) {
        if (tabMap.containsKey(tabId)) {
            _activeTabId.emit(tabId)
            logger("EDITOR", "Switched to tab: $tabId")
        }
    }
    
    /**
     * Get active tab content.
     */
    fun getActiveTab(): EditorTab? {
        val activeId = _activeTabId.value ?: return null
        return tabMap[activeId]
    }
    
    /**
     * Read file content from URI.
     */
    private suspend fun readFileContent(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val content = inputStream.bufferedReader().use { it.readText() }
            
            // Limit to 1MB for safety
            if (content.length > 1_000_000) {
                logger("EDITOR", "WARNING: File too large, truncating to 1MB")
                content.take(1_000_000)
            } else {
                content
            }
        } catch (e: Exception) {
            logger("EDITOR", "Error reading file: ${e.message}")
            null
        }
    }
    
    /**
     * Close all tabs.
     */
    suspend fun closeAll() {
        tabMap.clear()
        _activeTabId.emit(null)
        updateTabsFlow()
        logger("EDITOR", "All tabs closed")
    }
    
    /**
     * Get line count for a tab.
     */
    fun getLineCount(tabId: String): Int {
        return tabMap[tabId]?.content?.lines()?.size ?: 0
    }
    
    /**
     * Update tabs flow
     */
    private suspend fun updateTabsFlow() {
        _openTabs.emit(tabMap.values.toList())
    }
}
