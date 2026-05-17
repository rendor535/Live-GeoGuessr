package com.example.livegeoguessr.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey

object PreferencesKeys {
    val yes = booleanPreferencesKey("yes")
    val isLoggedIn = booleanPreferencesKey("is_logged_in")
}