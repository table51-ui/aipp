package com.aipp

import com.aipp.core.logging.StructuredLogger

/**
 * Simple dependency injection container.
 * 
 * Design:
 * - No frameworks (no Hilt, no Dagger)
 * - No reflection or annotation processing
 * - Constructor injection only
 * - Thread-safe singleton pattern
 * 
 * Usage:
 *   SubsystemManager.initialize(context)
 *   val logger = SubsystemManager.getLogger()
 */
object SubsystemManager {
    
    @Volatile
    private var logger: StructuredLogger? = null
    
    private val lock = Any()
    
    /**
     * Initialize all subsystems.
     * Safe to call multiple times (idempotent).
     */
    fun initialize(context: android.content.Context) {
        synchronized(lock) {
            if (logger != null) {
                return // Already initialized
            }
            
            // Create and initialize logger
            logger = StructuredLogger()
            logger!!.initialize(context.cacheDir)
            logger!!.info("SUBSYSTEM_MANAGER", "Subsystems initialized")
        }
    }
    
    /**
     * Get logger instance.
     * Throws if not initialized.
     */
    fun getLogger(): StructuredLogger {
        return logger ?: throw IllegalStateException(
            "SubsystemManager not initialized. Call initialize(context) first."
        )
    }
    
    /**
     * Reset to initial state (for testing only).
     */
    fun reset() {
        synchronized(lock) {
            logger = null
        }
    }
}
