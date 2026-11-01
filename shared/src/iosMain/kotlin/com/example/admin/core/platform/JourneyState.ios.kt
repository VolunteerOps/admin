package com.example.admin.core.platform

import platform.Foundation.NSUserDefaults

private const val JourneyKey = "volunteerops_admin_onboarding_v1"
actual fun readJourneyState(): String? = NSUserDefaults.standardUserDefaults.stringForKey(JourneyKey)
actual fun writeJourneyState(value: String) { NSUserDefaults.standardUserDefaults.setObject(value, forKey = JourneyKey) }
