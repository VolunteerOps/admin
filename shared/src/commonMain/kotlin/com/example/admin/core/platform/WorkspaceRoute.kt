package com.example.admin.core.platform

/** URL contains only the destination, never tenant identifiers or credentials. */
expect fun readWorkspaceRoute(): String?
expect fun writeWorkspaceRoute(route: String, replace: Boolean = false)
