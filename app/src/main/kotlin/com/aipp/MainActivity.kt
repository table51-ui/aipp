package com.aipp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aipp.core.logging.StructuredLogger

/**
 * Main activity - entry point of the application.
 * 
 * Responsibilities:
 * - Initialize subsystems on startup
 * - Set up Compose UI
 * - Emit startup logs
 * - NO network calls, NO cloud dependencies
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var logger: StructuredLogger
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize subsystems
            SubsystemManager.initialize(this)
            logger = SubsystemManager.getLogger()
            
            logger.info("MAIN_ACTIVITY", "onCreate: App initializing")
            logger.info("MAIN_ACTIVITY", "onCreate: Setting Compose content")
            
            // Set Compose content
            setContent {
                MainActivityScreen(logger)
            }
            
            logger.info("MAIN_ACTIVITY", "onCreate: Compose UI set")
        } catch (e: Exception) {
            logger.error("MAIN_ACTIVITY", "onCreate: Failed to initialize", e)
            throw e
        }
    }
    
    override fun onStart() {
        super.onStart()
        logger.debug("MAIN_ACTIVITY", "onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        logger.debug("MAIN_ACTIVITY", "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        logger.debug("MAIN_ACTIVITY", "onPause called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        logger.info("MAIN_ACTIVITY", "onDestroy called")
    }
}
