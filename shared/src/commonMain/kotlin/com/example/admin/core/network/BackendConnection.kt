package com.example.admin.core.network

/** Public API origin only. Authentication and authorization always run on the backend. */
object BackendConnection {
    const val DEFAULT_API_ORIGIN = "https://resq-api-staging.up.railway.app"
    var apiOrigin: String = DEFAULT_API_ORIGIN
        private set
    val sentinelUrl: String get() = apiOrigin + "/sentinel"

    /** Platform startup can override the public origin for a staging or local build. */
    fun configure(origin: String) {
        val normalized = origin.trim().trimEnd('/')
        val match = Regex("^(https?)://([A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?)(?::([0-9]{1,5}))?$").matchEntire(normalized)
        require(match != null) { "Backend address must be an origin without credentials, paths, query or fragment" }
        require(match.groupValues[1] == "https" || match.groupValues[2] in setOf("localhost", "127.0.0.1")) { "Backend address must use HTTPS" }
        require(match.groupValues[3].isEmpty() || match.groupValues[3].toInt() in 1..65535) { "Invalid backend port" }
        apiOrigin = normalized
    }
}
