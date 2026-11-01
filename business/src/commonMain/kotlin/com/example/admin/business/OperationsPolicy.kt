package com.example.admin.business

/** Shared native/web rules. The server always rechecks permissions and allocations. */
object OperationsPolicy {
    fun validEmail(value: String): Boolean = value.trim().let { it.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it) }
    fun canCoordinate(role: String, active: Boolean, approved: Boolean, organization: String): Boolean =
        active && approved && organization.isNotBlank() && role.uppercase() in setOf("ORG_ADMIN", "COORDINATOR", "TEAM_LEAD")
    fun statuses(kind: String): List<String> = when(kind) {
        "MISSION" -> listOf("DRAFT","STAGED","ACTIVE","PAUSED","COMPLETED","CANCELLED")
        "APPLICATION" -> listOf("APPLIED","WAITLISTED","ACCEPTED","WITHDRAWN")
        "ASSIGNMENT" -> listOf("ASSIGNED","CHECKED_IN","COMPLETED","CANCELLED")
        "REPORT" -> listOf("SUBMITTED","REVIEWING","ACTION_REQUIRED","RESOLVED")
        "HOURS" -> listOf("PENDING","APPROVED")
        "COURSE" -> listOf("DRAFT","PUBLISHED")
        "ENROLLMENT" -> listOf("IN_PROGRESS","AWAITING_REVIEW","COMPLETED")
        "CREDENTIAL" -> listOf("VERIFIED","REVOKED")
        "SUPPORT" -> listOf("OPEN","IN_PROGRESS","RESOLVED")
        "BULLETIN" -> listOf("PUBLISHED","WITHDRAWN")
        else -> emptyList()
    }
    fun missionTransitions(status: String): List<String> = when(status) {
        "DRAFT" -> listOf("STAGED","CANCELLED")
        "STAGED" -> listOf("ACTIVE","CANCELLED")
        "ACTIVE" -> listOf("PAUSED","COMPLETED","CANCELLED")
        "PAUSED" -> listOf("ACTIVE","CANCELLED")
        else -> emptyList()
    }
    fun availableStock(quantity: Int, reserved: Int): Int = (quantity-reserved).coerceAtLeast(0)
    fun coverage(assigned: Int, capacity: Int): Int = if(capacity<=0) 0 else (100.0*assigned/capacity).toInt().coerceIn(0,100)
    fun missionError(title: String, brief: String, capacity: Int, start: Double, end: Double, latitude: Double, longitude: Double): String = when {
        title.trim().length !in 3..160 -> "Use a mission title of 3–160 characters."
        brief.trim().length !in 10..5000 -> "Add a brief of at least 10 characters."
        capacity !in 1..1000 -> "Capacity must be between 1 and 1,000."
        !start.isFinite() || !end.isFinite() || end<=start || end-start>86_400_000 -> "Choose an end after the start, within 24 hours."
        !latitude.isFinite() || latitude !in -90.0..90.0 || !longitude.isFinite() || longitude !in -180.0..180.0 -> "Choose valid coordinates for the meeting point."
        else -> ""
    }
}
