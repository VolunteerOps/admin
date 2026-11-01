package com.example.admin.core.platform

import kotlinx.browser.window

actual fun readWorkspaceRoute(): String? = window.location.hash.removePrefix("#/")
actual fun writeWorkspaceRoute(route: String, replace: Boolean) {
    if(replace)window.history.replaceState(null,"","#/$route") else window.location.hash="/$route"
}
