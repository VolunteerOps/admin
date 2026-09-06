package com.example.admin.core.platform

/** Durable device-local demonstration data. This store is never used for credentials. */
expect fun readLocalState(): String?
expect fun writeLocalState(value: String)
