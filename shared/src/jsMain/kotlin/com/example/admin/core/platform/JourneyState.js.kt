package com.example.admin.core.platform

actual fun readJourneyState(): String? = js("globalThis.localStorage.getItem('volunteerops_admin_onboarding_v1')") as String?
actual fun writeJourneyState(value: String) { js("globalThis.localStorage.setItem('volunteerops_admin_onboarding_v1', value)") }
