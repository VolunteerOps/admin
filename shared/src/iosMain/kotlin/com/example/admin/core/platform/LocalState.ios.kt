package com.example.admin.core.platform

import platform.Foundation.NSUserDefaults

private const val StateKey = "volunteerops_admin_snapshot_v1"
actual fun readLocalState(): String? = NSUserDefaults.standardUserDefaults.stringForKey(StateKey)
actual fun writeLocalState(value: String) { NSUserDefaults.standardUserDefaults.setObject(value, forKey = StateKey) }
