package com.aipp.core.logging

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Structured logging system.
 * 
 * Every log has:
 * - timestamp
 * - subsystem tag (e.g., [WORKSPACE], [EDITOR], [RUNTIME])
 * - severity level (DEBUG, INFO, WARN, ERROR)
 * - message
 * 
 * Logs are:
 * - written to Logcat
 * - stored in memory (circular buffer)
 * - persisted to local file
 * - observable by UI layers
 */
class StructuredLogger {
    
    data class LogEntry(
        val timestamp: Long,
        val subsystem: String,
        val level: LogLevel,
        val message: String,
        val throwable: Throwable? = null
    ) {
        override fun toString(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
            val error = if (throwable != null) " | ${throwable.javaClass.simpleName}: ${throwable.message}" else ""
            return "[$time] [$subsystem] [${level.name}] $message$error"
        }
    }
    
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    private val buffer = CopyOnWriteArrayList<LogEntry>()
    private val maxBufferSize = 500 // Keep last 500 logs in memory
    private var logFile: File? = null
    
    /**
     * Initialize logger with file storage
     */
    fun initialize(cacheDir: File) {
        logFile = File(cacheDir, "aipp_runtime.log").apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
        info("LOGGER", "Logging initialized. File: ${logFile?.absolutePath}")
    }
    
    /**
     * Log at DEBUG level
     */
    fun debug(subsystem: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, subsystem, message, throwable)
    }
    
    /**
     * Log at INFO level
     */
    fun info(subsystem: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, subsystem, message, throwable)
    }
    
    /**
     * Log at WARN level
     */
    fun warn(subsystem: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, subsystem, message, throwable)
    }
    
    /**
     * Log at ERROR level
     */
    fun error(subsystem: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, subsystem, message, throwable)
    }
    
    /**
     * Core logging implementation
     */
    private fun log(
        level: LogLevel,
        subsystem: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            subsystem = subsystem,
            level = level,
            message = message,
            throwable = throwable
        )
        
        // Write to Logcat
        val logcatTag = "AIPP/$subsystem"
        when (level) {
            LogLevel.DEBUG -> Log.d(logcatTag, message, throwable)
            LogLevel.INFO -> Log.i(logcatTag, message, throwable)
            LogLevel.WARN -> Log.w(logcatTag, message, throwable)
            LogLevel.ERROR -> Log.e(logcatTag, message, throwable)
        }
        
        // Add to in-memory buffer
        buffer.add(entry)
        if (buffer.size > maxBufferSize) {
            buffer.removeAt(0)
        }
        
        // Write to file (async to avoid blocking)
        logFile?.let { file ->
            try {
                file.appendText(entry.toString() + "\n")
            } catch (e: Exception) {
                // Silently ignore file write errors to prevent log spam
            }
        }
    }
    
    /**
     * Get all buffered logs
     */
    fun getBufferedLogs(): List<LogEntry> {
        return buffer.toList()
    }
    
    /**
     * Get logs for a specific subsystem
     */
    fun getLogsForSubsystem(subsystem: String): List<LogEntry> {
        return buffer.filter { it.subsystem == subsystem }
    }
    
    /**
     * Get logs at or above a severity level
     */
    fun getLogsBySeverity(level: LogLevel): List<LogEntry> {
        val severityOrder = listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
        val levelIndex = severityOrder.indexOf(level)
        return buffer.filter { severityOrder.indexOf(it.level) >= levelIndex }
    }
    
    /**
     * Clear in-memory buffer
     */
    fun clearBuffer() {
        buffer.clear()
    }
    
    /**
     * Get last N logs
     */
    fun getLastLogs(count: Int): List<LogEntry> {
        val startIndex = maxOf(0, buffer.size - count)
        return buffer.subList(startIndex, buffer.size).toList()
    }
    
    /**
     * Get log file path
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
}
