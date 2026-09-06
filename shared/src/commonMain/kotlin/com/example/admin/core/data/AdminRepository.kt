package com.example.admin.core.data

import com.example.admin.core.model.*
import com.example.admin.core.platform.readLocalState
import com.example.admin.core.platform.writeLocalState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local practice workspace. No authentication, dispatch network or map provider is configured. */
class AdminRepository(private val readState: () -> String? = ::readLocalState, private val writeState: (String) -> Unit = ::writeLocalState) {
    private val _adminUser = MutableStateFlow(AdminUser("ADM-1", "Elena Vance", "elena@example.org", "Response coordinator", "Practice workspace"))
    val adminUser = _adminUser.asStateFlow()
    private val _incidents = MutableStateFlow(listOf(
        IncidentRecord("INC-081", "Riverbend flooding", "Central Valley", "critical", 14, 1, "active", "Sample incident"),
        IncidentRecord("INC-082", "Pine Ridge wildfire", "Northern Forest", "high", 0, 1, "active", "Sample incident"),
        IncidentRecord("INC-083", "Northvale storm relief", "Northvale", "medium", 3, 0, "active", "Sample incident"),
        IncidentRecord("INC-079", "Coastal recovery", "Eastern Harbor", "medium", 8, 0, "contained", "Sample incident")
    ))
    val incidents = _incidents.asStateFlow()
    private val _volunteers = MutableStateFlow(listOf(
        VolunteerRecord("VOL-101", "Sarah Johnson", "sarah@example.org", "+1 555 0101", "Central Valley", "available", 4.9, 15, listOf("Medical", "Triage"), certifications = listOf("EMT-B (sample)", "First aid (sample)")),
        VolunteerRecord("VOL-102", "Michael Chen", "michael@example.org", "+1 555 0102", "Northern Forest", "deployed", 4.8, 23, listOf("Search & Rescue", "Engineering"), recentAssignment = "Pine Ridge evacuation"),
        VolunteerRecord("VOL-103", "Emma Rodriguez", "emma@example.org", "+1 555 0103", "Central Valley", "available", 4.7, 12, listOf("Logistics", "Communications")),
        VolunteerRecord("VOL-104", "James Wilson", "james@example.org", "+1 555 0104", "Northvale", "unavailable", 4.6, 8, listOf("Water sanitation", "Logistics")),
        VolunteerRecord("VOL-105", "Lisa Thompson", "lisa@example.org", "+1 555 0105", "Eastern Harbor", "available", 4.9, 19, listOf("Logistics", "Incident coordination")),
        VolunteerRecord("VOL-106", "David Park", "david@example.org", "+1 555 0106", "Northvale", "standby", 4.8, 14, listOf("Search & Rescue", "First aid")),
        VolunteerRecord("VOL-107", "Amina Al-Mansoor", "amina@example.org", "+1 555 0107", "Central Valley", "deployed", 4.9, 27, listOf("Medical", "Mental health"), recentAssignment = "Riverbend medical support")
    ))
    val volunteers = _volunteers.asStateFlow()
    private val _missions = MutableStateFlow(listOf(
        MissionDispatch("MIS-901", "Riverbend medical support", "Support the relief camp team with intake, supplies and family assistance. Follow the assigned coordinator's briefing before entering the response area.", "active", "critical", "Riverbend relief camp", 1, 6, "05 Sep, 08:30", "8 hours", listOf("Medical", "Logistics"), "Elena Vance"),
        MissionDispatch("MIS-902", "Pine Ridge evacuation", "Help registered response teams coordinate the community assembly point and transport staging.", "active", "high", "Pine Ridge community hall", 1, 4, "05 Sep, 09:00", "6 hours", listOf("Search & Rescue", "Communications"), "Lisa Thompson"),
        MissionDispatch("MIS-903", "Northvale supply distribution", "Prepare food, water and hygiene kits for collection at the community centre.", "pending", "medium", "Northvale community centre", 0, 5, "06 Sep, 09:00", "4 hours", listOf("Logistics"), "Emma Rodriguez"),
        MissionDispatch("MIS-904", "Coastal shelter setup", "Temporary shelter assembly and inventory reconciliation.", "completed", "medium", "Eastern Harbor", 0, 3, "03 Sep, 10:00", "6 hours", listOf("Engineering"), "Elena Vance")
    ))
    val missions = _missions.asStateFlow()
    private val _assignments = MutableStateFlow(listOf(DeploymentAssignment("DEP-1", "VOL-102", "MIS-902", "Assembly point support", "Check in with the coordinator."), DeploymentAssignment("DEP-2", "VOL-107", "MIS-901", "Medical support", "Attend the relief camp briefing.")))
    val assignments = _assignments.asStateFlow()
    private val _tacticalUnits = MutableStateFlow(listOf(
        TacticalUnit("UNIT-1", "Riverbend support", "Medical", 4, .48f, .36f, "deployed", "INC-081"),
        TacticalUnit("UNIT-2", "Pine Ridge response", "Search & Rescue", 6, .28f, .72f, "deployed", "INC-082"),
        TacticalUnit("UNIT-3", "Supply transport", "Logistics", 3, .72f, .55f, "standby", ""),
        TacticalUnit("UNIT-4", "Community support", "Communications", 2, .63f, .18f, "standby", "")
    ))
    val tacticalUnits = _tacticalUnits.asStateFlow()
    private val _resources = MutableStateFlow(listOf(ResourceItem("RES-1", "First aid kits", 42, 60, "kits"), ResourceItem("RES-2", "Drinking water", 120, 200, "cases"), ResourceItem("RES-3", "Portable radios", 18, 24, "radios"), ResourceItem("RES-4", "Shelter packs", 14, 30, "packs")))
    val resources = _resources.asStateFlow()
    private val _skills = MutableStateFlow(listOf(
        SkillItem("SKL-1", "Medical", "Medical", 2, 1, "critical", 0.0, "Qualified medical support coordinated by the clinical lead."),
        SkillItem("SKL-2", "Search & Rescue", "Search & Rescue", 2, 1, "high", 0.0, "Accredited search and rescue support."),
        SkillItem("SKL-3", "Logistics", "Logistics", 3, 2, "high", 0.0, "Stock handling, transport and distribution."),
        SkillItem("SKL-4", "Communications", "Communications", 1, 1, "medium", 0.0, "Briefings, message routing and radio coordination."),
        SkillItem("SKL-5", "Engineering", "Engineering", 1, 0, "medium", 0.0, "Qualified infrastructure and shelter support.")
    ))
    val skills = _skills.asStateFlow()
    private val _alerts = MutableStateFlow(listOf(EmergencyAlert("ALT-1", "Riverbend briefing updated", "The relief camp briefing has moved to the north entrance. Check your assignment with the coordinator before travel.", "Sample update", "high", "Emergency alerts", 2, 0, recipientIds = listOf("VOL-101", "VOL-107"), targetAudience = "Medical volunteers", deliveryStatus = "Sample record")))
    val alerts = _alerts.asStateFlow()
    private val _securityEvents = MutableStateFlow(emptyList<SecurityEvent>())
    val securityEvents = _securityEvents.asStateFlow()
    private val _settings = MutableStateFlow(AdminSettings())
    val settings = _settings.asStateFlow()
    private val _isSystemLockdown = MutableStateFlow(false)
    val isSystemLockdown = _isSystemLockdown.asStateFlow()
    private val _storageWarning = MutableStateFlow<String?>(null)
    val storageWarning = _storageWarning.asStateFlow()
    private val _analyticsMessages = MutableStateFlow(listOf(AnalyticsMessage("INS-1", false, "Ask about staffing, missions, supplies or incidents. Answers are calculated from this local workspace; no AI service is connected.", "Local insight")))
    val analyticsMessages = _analyticsMessages.asStateFlow()
    val deploymentTrends = MutableStateFlow(listOf(DeploymentTrend("Apr", 18, 4, 5.2f), DeploymentTrend("May", 22, 5, 4.8f), DeploymentTrend("Jun", 19, 3, 5.1f), DeploymentTrend("Jul", 28, 6, 4.5f), DeploymentTrend("Aug", 32, 7, 4.2f))).asStateFlow()
    val helpArticles = MutableStateFlow(listOf(
        HelpArticle("H-1", "Prepare a mission briefing", "Coordination", "Guide", "3 min", "", "Give every volunteer the information they need before travel.", listOf("Confirm the location, meeting point, shift duration and named coordinator.", "Check each assigned volunteer's role, availability and required credentials with your organisation.", "Record access restrictions, equipment and the organisation's escalation contact.", "Review the briefing with the response lead, then use your approved communications channel to distribute it.")),
        HelpArticle("H-2", "Assign and release volunteers", "People", "Guide", "2 min", "", "Keep the registry and mission capacity consistent.", listOf("Open People and filter by skills and availability.", "Read the volunteer's dossier and confirm suitability with the coordinator.", "Choose an active mission with an open place; add a role and briefing notes.", "Complete the mission or release the assignment when the shift ends. The volunteer returns to available.")),
        HelpArticle("H-3", "Review an urgent update", "Communications", "Checklist", "2 min", "", "Check the audience and content before distributing an alert.", listOf("Use a short, specific subject and an appropriate priority.", "Include the source, location, requested action and contact for questions.", "Review the selected recipients. This practice app stores the draft locally and does not send it.", "Use your organisation's approved gateway to deliver the verified message and track acknowledgements.")),
        HelpArticle("H-4", "Account and device readiness", "Safety", "Checklist", "3 min", "", "Prepare the app and its integrations for field use.", listOf("This build uses sample records. Connect a verified identity provider and backend before operational use.", "Confirm access policies, credential verification, recovery and session expiry in that provider.", "Test connectivity, location permissions and your organisation's approved emergency contacts.", "Follow your organisation's incident procedures. This library is a product workflow guide, not clinical or rescue instruction."))
    )).asStateFlow()

    init { runCatching { readState()?.let(::restore) }.onFailure { _storageWarning.value = "Saved workspace could not be read. Sample records are shown." } }
    private fun fail(message: String) = MutationResult(false, message)
    private var sequence = 0L
    private fun id(prefix: String) = "$prefix-${timestamp()}-${++sequence}"
    private fun audit(message: String) { _securityEvents.value = (listOf(SecurityEvent(id("EV"), "workspace_change", "info", message, timestamp(), "Recorded")) + _securityEvents.value).take(200) }
    private fun done(message: String): MutationResult { audit(message); runCatching { writeState(snapshot()) }.onSuccess { _storageWarning.value = null }.onFailure { _storageWarning.value = "Device storage is unavailable. Changes will last for this session only." }; return MutationResult(true, message) }
    fun addVolunteer(name: String, email: String, phone: String, location: String, skills: List<String>): MutationResult {
        if (name.trim().length < 2 || !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) || location.isBlank()) return fail("Enter a name, valid email and location.")
        if (_volunteers.value.any { it.email.equals(email.trim(), true) }) return fail("A volunteer with that email already exists.")
        _volunteers.value += VolunteerRecord(id("VOL"), name.trim(), email.trim(), phone.trim(), location.trim(), "available", 0.0, 0, skills.map(String::trim).filter(String::isNotBlank).distinct())
        return done("Volunteer added.")
    }
    fun deployVolunteer(volunteerId: String, missionId: String, role: String, notes: String): MutationResult {
        if (_isSystemLockdown.value) return fail("Practice dispatch is paused. Resume it in Security.")
        val vol = _volunteers.value.find { it.id == volunteerId } ?: return fail("Volunteer no longer exists.")
        val mission = _missions.value.find { it.id == missionId } ?: return fail("Select a mission.")
        if (vol.status !in listOf("available", "standby") || _assignments.value.any { it.volunteerId == volunteerId && it.active }) return fail("This volunteer is already assigned or unavailable.")
        if (mission.status != "active" || mission.volunteersAssigned >= mission.requiredVolunteers) return fail("Choose an active mission with an open place.")
        if (role.isBlank()) return fail("Enter the volunteer's role.")
        _assignments.value += DeploymentAssignment(id("DEP"), volunteerId, missionId, role.trim(), notes.trim())
        _volunteers.value = _volunteers.value.map { if (it.id == volunteerId) it.copy(status = "deployed", deploymentsCount = it.deploymentsCount + 1, recentAssignment = mission.title) else it }
        reconcile()
        return done("${vol.name} assigned to ${mission.title}.")
    }
    fun releaseVolunteer(volunteerId: String): MutationResult {
        if (_assignments.value.none { it.volunteerId == volunteerId && it.active }) return fail("There is no active assignment.")
        _assignments.value = _assignments.value.map { if (it.volunteerId == volunteerId && it.active) it.copy(active = false) else it }
        _volunteers.value = _volunteers.value.map { if (it.id == volunteerId) it.copy(status = "available") else it }; reconcile()
        return done("Assignment released; volunteer is available.")
    }
    fun updateVolunteerStatus(volunteerId: String, newStatus: String): MutationResult {
        if (newStatus !in listOf("available", "standby", "unavailable")) return fail("Choose a valid availability.")
        if (_assignments.value.any { it.active && it.volunteerId == volunteerId }) return fail("Release the active assignment first.")
        if (_volunteers.value.none { it.id == volunteerId }) return fail("Volunteer no longer exists.")
        _volunteers.value = _volunteers.value.map { if (it.id == volunteerId) it.copy(status = newStatus) else it }; return done("Availability updated.")
    }
    private fun reconcile() { _missions.value = _missions.value.map { m -> m.copy(volunteersAssigned = _assignments.value.count { it.active && it.missionId == m.id }) } }
    fun saveMission(originalId: String?, title: String, description: String, priority: String, location: String, requiredVolunteers: Int, estimatedDuration: String, skillsRequired: List<String>, coordinator: String, startTime: String): MutationResult {
        if (title.trim().length < 3 || description.isBlank() || location.isBlank() || coordinator.isBlank() || startTime.isBlank() || estimatedDuration.isBlank()) return fail("Complete the mission title, briefing, location, schedule and coordinator.")
        if (requiredVolunteers !in 1..10000) return fail("Capacity must be between 1 and 10,000.")
        if (priority !in listOf("critical", "high", "medium", "low")) return fail("Choose a valid priority.")
        val old = _missions.value.find { it.id == originalId }
        if (originalId != null && old == null) return fail("Mission no longer exists.")
        if (old?.status == "completed") return fail("Completed missions are read-only. Create a new mission.")
        if (requiredVolunteers < (old?.volunteersAssigned ?: 0)) return fail("Capacity cannot be lower than the current staffing.")
        val mission = MissionDispatch(old?.id ?: id("MIS"), title.trim(), description.trim(), old?.status ?: "pending", priority, location.trim(), old?.volunteersAssigned ?: 0, requiredVolunteers, startTime.trim(), estimatedDuration.trim(), skillsRequired.map(String::trim).filter(String::isNotBlank).distinct(), coordinator.trim())
        _missions.value = if (old == null) listOf(mission) + _missions.value else _missions.value.map { if (it.id == old.id) mission else it }
        return done(if (old == null) "Mission created in planned status." else "Mission updated.")
    }
    fun updateMissionStatus(missionId: String, status: String): MutationResult {
        val m = _missions.value.find { it.id == missionId } ?: return fail("Mission no longer exists.")
        val allowed = when (m.status) { "pending" -> setOf("active"); "active" -> setOf("paused", "completed"); "paused" -> setOf("active", "completed"); else -> emptySet() }
        if (status !in allowed) return fail("This lifecycle transition is unavailable.")
        if (_isSystemLockdown.value && status == "active") return fail("Practice dispatch is paused.")
        if (status == "completed") {
            val people = _assignments.value.filter { it.missionId == missionId && it.active }.map { it.volunteerId }.toSet()
            _assignments.value = _assignments.value.map { if (it.missionId == missionId) it.copy(active = false) else it }
            _volunteers.value = _volunteers.value.map { if (it.id in people) it.copy(status = "available") else it }
        }
        _missions.value = _missions.value.map { if (it.id == missionId) it.copy(status = status) else it }; reconcile()
        return done(when(status) { "completed" -> "Mission completed and volunteers released."; "paused" -> "Mission paused; existing assignments are retained."; else -> "Mission activated." })
    }
    fun updateIncident(id: String, status: String, people: Int): MutationResult {
        if (status !in listOf("active", "contained", "resolved") || people !in 0..100000) return fail("Enter a valid incident status and people count.")
        if (_incidents.value.none { it.id == id }) return fail("Incident no longer exists.")
        if (status == "resolved" && _tacticalUnits.value.any { it.assignedIncident == id && it.status == "deployed" }) return fail("Release the assigned teams in Deployment map before resolving this incident.")
        _incidents.value = _incidents.value.map { if (it.id == id) it.copy(status = status, casualtiesReported = people) else it }; return done("Incident record updated.")
    }
    fun dispatchResource(resourceId: String, incidentId: String, quantity: Int): MutationResult {
        if (_isSystemLockdown.value) return fail("Practice dispatch is paused.")
        val r = _resources.value.find { it.id == resourceId } ?: return fail("Select a resource.")
        val incident = _incidents.value.find { it.id == incidentId && it.status != "resolved" } ?: return fail("Select an open incident.")
        if (quantity <= 0 || quantity > r.available) return fail("Quantity must be between 1 and ${r.available}.")
        _resources.value = _resources.value.map { if (it.id == resourceId) it.copy(available = it.available - quantity) else it }; return done("Allocated $quantity ${r.unit} to ${incident.title} in this workspace.")
    }
    fun restockResource(resourceId: String, quantity: Int): MutationResult {
        val r = _resources.value.find { it.id == resourceId } ?: return fail("Select a resource.")
        if (quantity <= 0 || quantity > r.total - r.available) return fail("Restock quantity exceeds the stock capacity.")
        _resources.value = _resources.value.map { if (it.id == resourceId) it.copy(available = it.available + quantity) else it }; return done("Stock count updated.")
    }
    fun assignUnit(unitId: String, incidentId: String): MutationResult {
        if (_isSystemLockdown.value && incidentId.isNotBlank()) return fail("Practice dispatch is paused.")
        if (_tacticalUnits.value.none { it.id == unitId }) return fail("Team no longer exists.")
        if (incidentId.isNotBlank() && _incidents.value.none { it.id == incidentId && it.status != "resolved" }) return fail("Select an open incident.")
        _tacticalUnits.value = _tacticalUnits.value.map { if (it.id == unitId) it.copy(assignedIncident = incidentId, status = if (incidentId.isBlank()) "standby" else "deployed") else it }
        _incidents.value = _incidents.value.map { i -> i.copy(teamsDeployed = _tacticalUnits.value.count { it.assignedIncident == i.id && it.status == "deployed" }) }
        return done("Team assignment updated locally.")
    }
    fun saveSkill(id: String?, name: String, category: String, demand: String, description: String): MutationResult {
        if (name.trim().length < 2 || category.isBlank() || description.isBlank() || demand !in listOf("critical", "high", "medium", "low")) return fail("Complete the skill name, category, demand and description.")
        if (_skills.value.any { it.id != id && it.name.equals(name.trim(), true) }) return fail("That skill already exists.")
        val old = _skills.value.find { it.id == id }
        if (id != null && old == null) return fail("Skill no longer exists.")
        val skill = SkillItem(id ?: id("SKL"), name.trim(), category, 0, 0, demand, 0.0, description.trim())
        _skills.value = if (old == null) _skills.value + skill else _skills.value.map { if (it.id == id) skill else it }
        if (old != null && old.name != skill.name) {
            _volunteers.value = _volunteers.value.map { v -> v.copy(skills = v.skills.map { if (it.equals(old.name, true)) skill.name else it }) }
            _missions.value = _missions.value.map { m -> m.copy(skillsRequired = m.skillsRequired.map { if (it.equals(old.name, true)) skill.name else it }) }
        }
        return done("Skill saved.")
    }
    fun recipients(audience: String): List<VolunteerRecord> = _volunteers.value.filter { when (audience) { "Deployed volunteers" -> it.status == "deployed"; "Available volunteers" -> it.status in listOf("available", "standby"); "Medical volunteers" -> it.skills.any { s -> s.contains("Medical", true) }; else -> true } }
    fun sendEmergencyAlert(title: String, message: String, priority: String, channel: String, targetAudience: String): MutationResult {
        if (title.trim().length < 3 || message.trim().length < 10) return fail("Add a subject and a message of at least 10 characters.")
        val people = recipients(targetAudience)
        if (people.isEmpty()) return fail("No volunteers match this audience.")
        if (priority !in listOf("critical", "high", "info") || channel !in listOf("Emergency alerts", "Team updates")) return fail("Choose a supported priority and channel.")
        if (targetAudience !in listOf("All volunteers", "Deployed volunteers", "Available volunteers", "Medical volunteers")) return fail("Choose a supported audience.")
        _alerts.value = listOf(EmergencyAlert(id("ALT"), title.trim(), message.trim(), timestamp(), priority, channel, people.size, 0, targetAudience = targetAudience, recipientIds = people.map { it.id })) + _alerts.value
        return done("Broadcast draft saved locally. No messages were sent.")
    }
    fun triggerLockdown(active: Boolean): MutationResult { _isSystemLockdown.value = active; return done(if (active) "Practice dispatch paused on this device." else "Practice dispatch resumed.") }
    fun updateSettings(value: AdminSettings): MutationResult {
        if (value.organizationName.isBlank() || value.timezone.isBlank() || value.sessionTimeoutMinutes !in 5..120) return fail("Add an organisation, timezone and a timeout of 5–120 minutes.")
        _settings.value = value; return done("Preferences saved on this device.")
    }
    fun sendAnalyticsQuery(query: String): MutationResult {
        if (query.isBlank()) return fail("Enter a question.")
        val q = query.lowercase(); val active = _missions.value.filter { it.status == "active" }
        val response = when {
            listOf("staff", "volunteer", "people", "capacity").any { it in q } -> "${_volunteers.value.count { it.status == "deployed" }} of ${_volunteers.value.size} volunteers are assigned. ${_volunteers.value.count { it.status in listOf("available", "standby") }} are available or on standby. Active missions have ${active.sumOf { it.requiredVolunteers - it.volunteersAssigned }} open places."
            listOf("suppl", "resource", "stock").any { it in q } -> _resources.value.joinToString("\n") { "${it.name}: ${it.available} of ${it.total} ${it.unit} available." }
            listOf("incident", "triage").any { it in q } -> "${_incidents.value.count { it.status == "active" }} active incidents; ${_incidents.value.sumOf { it.casualtiesReported }} people recorded as needing support across the sample incidents. Confirm the incident records before using these figures."
            "mission" in q -> "${active.size} active, ${_missions.value.count { it.status == "pending" }} planned and ${_missions.value.count { it.status == "completed" }} completed missions.\n" + active.joinToString("\n") { "${it.title}: ${it.volunteersAssigned}/${it.requiredVolunteers} assigned." }
            else -> "I can calculate staffing, mission capacity, stock and incident totals from the local records. Try ‘How many places need staffing?’ No prediction or external data is available."
        }
        _analyticsMessages.value = (_analyticsMessages.value + listOf(AnalyticsMessage(id("Q"), true, query.trim(), "You"), AnalyticsMessage(id("A"), false, response, "Calculated locally"))).takeLast(60)
        return done("Local insight calculated.")
    }

    // Nested, length-prefixed strings preserve delimiters, Unicode and multiline notes without dependencies.
    private fun pack(values: List<String>): String = values.joinToString("") { "${it.length}:$it" }
    private fun row(vararg values: Any?): String = pack(values.map { it.toString() })
    private fun unpack(value: String): List<String> { val result = mutableListOf<String>(); var p = 0; while (p < value.length) { val end = value.indexOf(':', p); require(end >= p); val n = value.substring(p, end).toInt(); require(n >= 0 && n <= value.length - end - 1); p = end + 1; result += value.substring(p, p + n); p += n }; return result }
    fun snapshot(): String = pack(listOf("VolunteerOpsAdmin:2",
        pack(_volunteers.value.map { row(it.id,it.name,it.email,it.phone,it.location,it.status,it.rating,it.deploymentsCount,pack(it.skills),it.availability,it.emergencyContact,pack(it.certifications),it.recentAssignment) }),
        pack(_missions.value.map { row(it.id,it.title,it.description,it.status,it.priority,it.location,it.volunteersAssigned,it.requiredVolunteers,it.startTime,it.estimatedDuration,pack(it.skillsRequired),it.coordinator) }),
        pack(_assignments.value.map { row(it.id,it.volunteerId,it.missionId,it.role,it.notes,it.active) }),
        pack(_incidents.value.map { row(it.id,it.title,it.region,it.severity,it.casualtiesReported,it.teamsDeployed,it.status,it.timestamp) }),
        pack(_resources.value.map { row(it.id,it.name,it.available,it.total,it.unit) }),
        pack(_tacticalUnits.value.map { row(it.id,it.name,it.unitType,it.personnelCount,it.latNormalized,it.lngNormalized,it.status,it.assignedIncident) }),
        pack(_skills.value.map { row(it.id,it.name,it.category,it.volunteerCount,it.availableCount,it.demand,it.rating,it.description) }),
        pack(_alerts.value.map { row(it.id,it.title,it.message,it.timestamp,it.priority,it.channel,it.recipientsCount,it.responsesCount,it.sender,it.targetAudience,pack(it.recipientIds),it.deliveryStatus) }),
        _settings.value.let { row(it.organizationName,it.timezone,it.language,it.emailAlerts,it.smsAlerts,it.pushAlerts,it.emergencyBroadcastAutoForward,it.twoFactorAuth,it.sessionTimeoutMinutes,it.apiAccessEnabled,it.webhooksEnabled) },
        _isSystemLockdown.value.toString(),
        pack(_securityEvents.value.map { row(it.id,it.type,it.severity,it.message,it.timestamp,it.status,it.ipAddress) }),
        pack(_analyticsMessages.value.map { row(it.id,it.isFromUser,it.text,it.timestamp) })
    ))
    private fun restore(value: String) {
        val s = unpack(value); require(s.size == 13 && s[0] == "VolunteerOpsAdmin:2")
        fun rows(i: Int) = unpack(s[i]).map(::unpack)
        val people = rows(1).map { VolunteerRecord(it[0],it[1],it[2],it[3],it[4],it[5],it[6].toDouble(),it[7].toInt(),unpack(it[8]),it[9],it[10],unpack(it[11]),it[12]) }
        val jobs = rows(2).map { MissionDispatch(it[0],it[1],it[2],it[3],it[4],it[5],it[6].toInt(),it[7].toInt(),it[8],it[9],unpack(it[10]),it[11]) }
        val assignments = rows(3).map { DeploymentAssignment(it[0],it[1],it[2],it[3],it[4],it[5].toBooleanStrict()) }
        val incidents = rows(4).map { IncidentRecord(it[0],it[1],it[2],it[3],it[4].toInt(),it[5].toInt(),it[6],it[7]) }
        val resources = rows(5).map { ResourceItem(it[0],it[1],it[2].toInt(),it[3].toInt(),it[4]) }
        val units = rows(6).map { TacticalUnit(it[0],it[1],it[2],it[3].toInt(),it[4].toFloat(),it[5].toFloat(),it[6],it[7]) }
        val skills = rows(7).map { SkillItem(it[0],it[1],it[2],it[3].toInt(),it[4].toInt(),it[5],it[6].toDouble(),it[7]) }
        val alerts = rows(8).map { EmergencyAlert(it[0],it[1],it[2],it[3],it[4],it[5],it[6].toInt(),it[7].toInt(),it[8],it[9],unpack(it[10]),it[11]) }
        val p = unpack(s[9]); val prefs = AdminSettings(p[0],p[1],p[2],p[3].toBooleanStrict(),p[4].toBooleanStrict(),p[5].toBooleanStrict(),p[6].toBooleanStrict(),p[7].toBooleanStrict(),p[8].toInt(),p[9].toBooleanStrict(),p[10].toBooleanStrict())
        val paused = s[10].toBooleanStrict()
        val events = rows(11).map { SecurityEvent(it[0],it[1],it[2],it[3],it[4],it[5],it[6]) }
        val insights = rows(12).map { AnalyticsMessage(it[0],it[1].toBooleanStrict(),it[2],it[3]) }
        require(people.map { it.id }.distinct().size == people.size && jobs.map { it.id }.distinct().size == jobs.size)
        require(jobs.all { it.requiredVolunteers > 0 && it.status in listOf("pending","active","paused","completed") })
        require(resources.all { it.available in 0..it.total })
        require(assignments.all { a -> people.any { it.id == a.volunteerId } && jobs.any { it.id == a.missionId } })
        require(assignments.filter { it.active }.map { it.volunteerId }.distinct().size == assignments.count { it.active })
        require(jobs.all { m -> assignments.count { it.active && it.missionId == m.id } <= m.requiredVolunteers })
        _volunteers.value = people; _missions.value = jobs; _assignments.value = assignments; _incidents.value = incidents; _resources.value = resources; _tacticalUnits.value = units; _skills.value = skills; _alerts.value = alerts; _settings.value = prefs; _isSystemLockdown.value = paused; _securityEvents.value = events; _analyticsMessages.value = insights; reconcile()
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun timestamp(): String = kotlin.time.Clock.System.now().toString()
