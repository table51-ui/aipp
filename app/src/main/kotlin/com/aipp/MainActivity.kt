package com.aipp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aipp.core.logging.StructuredLogger

/**
 * Main activity - entry point of the application.
 * 
 * Responsibilities:
 * - Initialize subsystems
 * - Set up Compose UI
 * - Emit startup logs
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize subsystems
        SubsystemManager.initialize(this)
        val logger = SubsystemManager.getLogger()
        
        logger.info("MAIN_ACTIVITY", "onCreate called")
        logger.info("MAIN_ACTIVITY", "Setting up Compose UI")
        
        // Set Compose content
        setContent {
            MainActivityScreen(logger)
        }
        
        logger.info("MAIN_ACTIVITY", "UI rendering started")
    }
    
    override fun onStart() {
        super.onStart()
        SubsystemManager.getLogger().debug("MAIN_ACTIVITY", "onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        SubsystemManager.getLogger().debug("MAIN_ACTIVITY", "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        SubsystemManager.getLogger().debug("MAIN_ACTIVITY", "onPause called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SubsystemManager.getLogger().info("MAIN_ACTIVITY", "onDestroy called")
    }
}
