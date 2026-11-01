package com.example.admin.core.platform
/** Separate noncredential intro/preview draft. Never contains email, passwords or ID tokens. */
expect fun readJourneyState(): String?
expect fun writeJourneyState(value: String)
