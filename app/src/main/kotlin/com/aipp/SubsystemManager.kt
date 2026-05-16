package com.aipp

import android.content.Context
import com.aipp.core.logging.StructuredLogger

/**
 * Simple dependency injection container.
 * No frameworks, no reflection, no annotation processing.
 * Constructor injection only.
 */
object SubsystemManager {
    
    private var logger: StructuredLogger? = null
    
    /**
     * Initialize all subsystems
     */
    fun initialize(context: Context) {
        if (logger != null) {
            return // Already initialized
        }
        
        // Create logger (first subsystem)
        logger = StructuredLogger()
        logger!!.initialize(context.cacheDir)
        logger!!.info("SUBSYSTEM_MANAGER", "Subsystems initialized")
    }
    
    /**
     * Get logger instance
     */
    fun getLogger(): StructuredLogger {
        return logger ?: throw IllegalStateException("SubsystemManager not initialized. Call initialize() first.")
    }
    
    /**
     * Reset (for testing)
     */
    fun reset() {
        logger = null
    }
}
