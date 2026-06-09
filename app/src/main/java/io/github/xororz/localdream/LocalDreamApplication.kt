package io.github.xororz.localdream

import android.app.Application

/**
 * LocalDream Application class - Integrated into KuaiHuiAI
 * This class initializes the LocalDream backend services
 */
class LocalDreamApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize LocalDream backend
        // The actual backend is started by BackendService
    }
}
