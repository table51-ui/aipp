package com.aipp

import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight dependency injection and service registry.
 * 
 * No reflection, no annotations, no framework.
 * Single authority for all subsystems.
 */
object SubsystemManager {
    
    private val services = ConcurrentHashMap<String, Any>()
    
    /**
     * Register a service instance
     */
    fun <T : Any> register(name: String, instance: T): T {
        services[name] = instance
        return instance
    }
    
    /**
     * Retrieve a registered service
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(name: String): T? {
        return services[name] as? T
    }
    
    /**
     * Retrieve a service or throw
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getRequired(name: String): T {
        return services[name] as? T
            ?: throw NoSuchElementException("Service '$name' not registered")
    }
    
    /**
     * Check if service is registered
     */
    fun isRegistered(name: String): Boolean {
        return services.containsKey(name)
    }
    
    /**
     * Get all registered service names
     */
    fun getRegisteredServices(): List<String> {
        return services.keys.toList()
    }
    
    /**
     * Clear all services (testing only)
     */
    fun clear() {
        services.clear()
    }
}
