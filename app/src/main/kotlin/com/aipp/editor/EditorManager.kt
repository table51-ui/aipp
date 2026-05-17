package com.aipp.editor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a single open editor tab.
 */
data class EditorTab(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Manages open files and editor tabs.
 * 
 * Responsibilities:
 * - Read file content from workspace
 * - Manage open tabs (open, close, switch)
 * - Track active tab
 * - Handle errors
 */
class EditorManager(
    private val context: Context,
    private val logger: (tag: String, message: String) -> Unit
) {
    
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()
    
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    
    /**
     * Open a file in the editor.
     * Creates a new tab or switches to existing tab.
     */
    suspend fun openFile(uri: Uri, fileName: String) {
        try {
            logger("EDITOR", "Opening file: $fileName")
            
            val tabId = uri.toString()
            val existingTab = _openTabs.value.find { it.id == tabId }
            
            if (existingTab != null) {
                // Tab already open, just switch to it
                _activeTabId.emit(tabId)
                logger("EDITOR", "Switched to tab: $fileName")
                return
            }
            
            // Create new tab with loading state
            val loadingTab = EditorTab(
                id = tabId,
                uri = uri,
                fileName = fileName,
                isLoading = true
            )
            
            val updatedTabs = _openTabs.value + loadingTab
            _openTabs.emit(updatedTabs)
            _activeTabId.emit(tabId)
            
            // Read file content
            val content = readFileContent(uri)
            
            if (content != null) {
                // Success: update tab with content
                val completedTab = EditorTab(
                    id = tabId,
                    uri = uri,
                    fileName = fileName,
                    content = content,
                    isLoading = false
                )
                
                val updatedWithContent = _openTabs.value.map {
                    if (it.id == tabId) completedTab else it
                }
                _openTabs.emit(updatedWithContent)
                logger("EDITOR", "File loaded: $fileName (${content.length} chars)")
            } else {
                // Error: update tab with error state
                val errorTab = EditorTab(
                    id = tabId,
                    uri = uri,
                    fileName = fileName,
                    isLoading = false,
                    error = "Failed to read file"
                )
                
                val updatedWithError = _openTabs.value.map {
                    if (it.id == tabId) errorTab else it
                }
                _openTabs.emit(updatedWithError)
                logger("EDITOR", "Error reading file: $fileName")
            }
        } catch (e: Exception) {
            logger("EDITOR", "Exception opening file: ${e.message}")
        }
    }
    
    /**
     * Close a tab by ID.
     */
    suspend fun closeTab(tabId: String) {
        try {
            val updatedTabs = _openTabs.value.filter { it.id != tabId }
            _openTabs.emit(updatedTabs)
            
            // If closed tab was active, switch to another
            if (_activeTabId.value == tabId) {
                _activeTabId.emit(updatedTabs.firstOrNull()?.id)
            }
            
            logger("EDITOR", "Tab closed: $tabId")
        } catch (e: Exception) {
            logger("EDITOR", "Exception closing tab: ${e.message}")
        }
    }
    
    /**
     * Switch to a specific tab by ID.
     */
    suspend fun switchTab(tabId: String) {
        try {
            if (_openTabs.value.any { it.id == tabId }) {
                _activeTabId.emit(tabId)
                logger("EDITOR", "Switched to tab: $tabId")
            }
        } catch (e: Exception) {
            logger("EDITOR", "Exception switching tab: ${e.message}")
        }
    }
    
    /**
     * Read file content from URI.
     * Returns null if read fails.
     */
    private suspend fun readFileContent(uri: Uri): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            logger("EDITOR", "Error reading file content: ${e.message}")
            null
        }
    }
    
    /**
     * Get tab by ID.
     */
    fun getTab(tabId: String): EditorTab? {
        return _openTabs.value.find { it.id == tabId }
    }
    
    /**
     * Clear all tabs.
     */
    suspend fun clearTabs() {
        _openTabs.emit(emptyList())
        _activeTabId.emit(null)
        logger("EDITOR", "All tabs cleared")
    }
}
