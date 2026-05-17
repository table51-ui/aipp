package com.aipp

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging system.
 * 
 * Logs to both Logcat (Android) and in-memory buffer.
 * In-memory buffer accessible to UI for real-time log display.
 * 
 * Thread-safe.
 */
class StructuredLogger {
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val logBuffer = mutableListOf<LogEntry>()
    private val maxBufferSize = 500
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    data class LogEntry(
        val tag: String,
        val level: LogLevel,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        
        override fun toString(): String {
            return "[$formattedTime] [${level.name}] [$tag] $message"
        }
    }
    
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    fun debug(tag: String, message: String) {
        log(tag, LogLevel.DEBUG, message)
    }
    
    fun info(tag: String, message: String) {
        log(tag, LogLevel.INFO, message)
    }
    
    fun warn(tag: String, message: String) {
        log(tag, LogLevel.WARN, message)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message (${throwable.javaClass.simpleName}: ${throwable.message})"
        } else {
            message
        }
        log(tag, LogLevel.ERROR, fullMessage)
    }
    
    private fun log(tag: String, level: LogLevel, message: String) {
        val logcatTag = "[AIPP/$tag]"
        when (level) {
            LogLevel.DEBUG -> Log.d(logcatTag, message)
            LogLevel.INFO -> Log.i(logcatTag, message)
            LogLevel.WARN -> Log.w(logcatTag, message)
            LogLevel.ERROR -> Log.e(logcatTag, message)
        }
        
        val entry = LogEntry(tag, level, message)
        synchronized(logBuffer) {
            logBuffer.add(entry)
            if (logBuffer.size > maxBufferSize) {
                logBuffer.removeAt(0)
            }
            _logs.value = logBuffer.toList()
        }
    }
    
    fun clearLogs() {
        synchronized(logBuffer) {
            logBuffer.clear()
            _logs.value = emptyList()
        }
    }
    
    fun getAllLogs(): List<LogEntry> {
        synchronized(logBuffer) {
            return logBuffer.toList()
        }
    }
}
