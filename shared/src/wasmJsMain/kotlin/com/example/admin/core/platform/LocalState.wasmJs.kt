@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.example.admin.core.platform

import kotlin.JsFun

@JsFun("() => globalThis.localStorage.getItem('volunteerops_admin_snapshot_v1')")
private external fun readBrowserState(): String?
@JsFun("(value) => globalThis.localStorage.setItem('volunteerops_admin_snapshot_v1', value)")
private external fun writeBrowserState(value: String)
actual fun readLocalState(): String? = readBrowserState()
actual fun writeLocalState(value: String) = writeBrowserState(value)
