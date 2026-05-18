package com.example.livegeoguessr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class LiveGeoGuessrApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName
        val osmConfig = Configuration.getInstance()
        val basePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")
    }
}
