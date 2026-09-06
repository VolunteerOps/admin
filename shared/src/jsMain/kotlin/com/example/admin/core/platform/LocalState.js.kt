package com.example.admin.core.platform

actual fun readLocalState(): String? = js("globalThis.localStorage.getItem('volunteerops_admin_snapshot_v1')") as String?
actual fun writeLocalState(value: String) { js("globalThis.localStorage.setItem('volunteerops_admin_snapshot_v1', value)") }
