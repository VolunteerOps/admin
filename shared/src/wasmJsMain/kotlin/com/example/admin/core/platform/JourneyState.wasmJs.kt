@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.example.admin.core.platform

import kotlin.JsFun

@JsFun("() => globalThis.localStorage.getItem('volunteerops_admin_onboarding_v1')")
private external fun readJourneyBrowserState(): String?
@JsFun("(value) => globalThis.localStorage.setItem('volunteerops_admin_onboarding_v1', value)")
private external fun writeJourneyBrowserState(value: String)
actual fun readJourneyState(): String? = readJourneyBrowserState()
actual fun writeJourneyState(value: String) = writeJourneyBrowserState(value)
