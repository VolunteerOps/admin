package com.example.admin.core.platform

import android.content.Context
import android.content.SharedPreferences

object JourneyStateStorage {
    private var preferences: SharedPreferences? = null
    fun initialize(context: Context) { preferences = context.applicationContext.getSharedPreferences("volunteerops_admin", Context.MODE_PRIVATE) }
    fun read(): String? = preferences?.getString("onboarding_v1", null)
    fun write(value: String) {
        val store = checkNotNull(preferences) { "Local state storage has not been initialized" }
        check(store.edit().putString("onboarding_v1", value).commit()) { "Unable to save changes on this device" }
    }
}
actual fun readJourneyState(): String? = JourneyStateStorage.read()
actual fun writeJourneyState(value: String) = JourneyStateStorage.write(value)
