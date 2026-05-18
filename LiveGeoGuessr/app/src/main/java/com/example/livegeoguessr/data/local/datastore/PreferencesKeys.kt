package com.example.livegeoguessr.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey

object PreferencesKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val USE_MILES = booleanPreferencesKey("use_miles")
    val isLoggedIn = booleanPreferencesKey("is_logged_in")
}
