package com.example.admin.features.missions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.data.*
import com.example.admin.core.model.*
import com.example.admin.core.theme.*
import com.example.admin.core.icons.*


import com.example.admin.features.volunteers.DeploymentSheet

@Composable fun MissionControlScreen(repository: AdminRepository, onPeople: () -> Unit = {}) {
    val missions by repository.missions.collectAsState(); val people by repository.volunteers.collectAsState(); val assignments by repository.assignments.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("all") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var editor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }; var assignPerson by rememberSaveable { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<MutationResult?>(null) }; var completing by remember { mutableStateOf(false) }
    val shown = missions.filter { (filter == "all" || it.status == filter) && (it.title.contains(query,true) || it.location.contains(query,true)) }
    OpsPage {
        item { PageTitle("From a plan to a response", "Manage the brief, the people and the handover.", "MISSION CONTROL") { GlassIconButton(OpsSymbol.Add, "Create mission", { editingId = null; editor = true }) } }
        item { SearchBox(query,{ query = it },"Search missions or locations"); Choices(listOf("all","active","pending","paused","completed"),filter) { filter = it } }
        if (shown.isEmpty()) item { EmptyState() }
        items(shown,key = { it.id }) { mission -> OpsCard {
            Actions { StatusBadge(mission.priority); StatusBadge(mission.status) }
            SectionTitle(mission.title,mission.location); KeyValue("Coordinator",mission.coordinator,OpsSymbol.People)
            Capacity(mission.volunteersAssigned,mission.requiredVolunteers)
            OpsButton("Open mission brief", { selected = mission.id; result = null; completing = false },Modifier.fillMaxWidth(),true,symbol = OpsSymbol.ArrowRight)
        } }
    }
    val mission = missions.firstOrNull { it.id == selected }
    if (editor) MissionEditor(repository,missions.firstOrNull { it.id == editingId }) { editor = false }
    else if (assignPerson != null) DeploymentSheet(repository,assignPerson!!,{ assignPerson = null },selected)
    else if (mission != null) DetailSheet("Mission brief",{ selected = null }) {
        Actions { StatusBadge(mission.priority); StatusBadge(mission.status) }; SectionTitle(mission.title,mission.location); Feedback(result)
        Text(mission.description); KeyValue("Coordinator",mission.coordinator); KeyValue("Schedule","${mission.startTime} · ${mission.estimatedDuration}"); Tags(mission.skillsRequired); Capacity(mission.volunteersAssigned,mission.requiredVolunteers)
        SectionTitle("Assigned people")
        val team = assignments.filter { it.missionId == mission.id && it.active }
        if (team.isEmpty()) Text("No one assigned yet.",color = AdminColors.Slate)
        team.forEach { a -> KeyValue(people.firstOrNull { it.id == a.volunteerId }?.name ?: a.volunteerId,"${a.role}\n${a.notes}") }
        if (mission.status == "active" && mission.volunteersAssigned < mission.requiredVolunteers) {
            SectionTitle("Available to assign")
            val available = people.filter { it.status in listOf("available","standby") }
            if (available.isEmpty()) OpsButton("Review volunteer registry",onPeople,secondary = true)
            available.forEach { person -> OpsButton(person.name,{ assignPerson = person.id },Modifier.fillMaxWidth(),true,symbol = OpsSymbol.Add) }
        }
        if (mission.status != "completed") {
            OpsButton("Edit briefing",{ editingId = mission.id; editor = true },Modifier.fillMaxWidth(),true,symbol = OpsSymbol.Edit)
            if (mission.status in listOf("pending","paused")) OpsButton(if (mission.status == "paused") "Resume mission" else "Activate mission",{ result = repository.updateMissionStatus(mission.id,"active") },Modifier.fillMaxWidth())
            if (mission.status == "active") OpsButton("Pause new assignments",{ result = repository.updateMissionStatus(mission.id,"paused") },Modifier.fillMaxWidth(),true)
            if (mission.status in listOf("active","paused")) {
                if (!completing) OpsButton("Complete mission",{ completing = true },Modifier.fillMaxWidth(),true)
                else { LiquidGlassAlert("Confirm the handover","Completing this mission releases its ${team.size} assigned volunteers. The mission becomes read-only.",GlassVariant.WARNING); OpsButton("Confirm completion",{ result = repository.updateMissionStatus(mission.id,"completed"); completing = false },Modifier.fillMaxWidth(),danger = true) }
            }
        }
    }
}
@Composable private fun MissionEditor(repository: AdminRepository, mission: MissionDispatch?, close: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(mission?.title ?: "") }; var description by rememberSaveable { mutableStateOf(mission?.description ?: "") }
    var location by rememberSaveable { mutableStateOf(mission?.location ?: "") }; var capacity by rememberSaveable { mutableStateOf(mission?.requiredVolunteers?.toString() ?: "") }
    var priority by rememberSaveable { mutableStateOf(mission?.priority ?: "medium") }; var coordinator by rememberSaveable { mutableStateOf(mission?.coordinator ?: "") }
    var schedule by rememberSaveable { mutableStateOf(mission?.startTime ?: "") }; var duration by rememberSaveable { mutableStateOf(mission?.estimatedDuration ?: "") }
    var skills by rememberSaveable { mutableStateOf(mission?.skillsRequired?.joinToString(", ") ?: "") }; var result by remember { mutableStateOf<MutationResult?>(null) }
    DetailSheet(if (mission == null) "Plan a mission" else "Edit the mission",close) {
        OpsField("Mission title",title,{ title = it }); Choices(listOf("critical","high","medium","low"),priority) { priority = it }
        OpsField("Meeting point and location",location,{ location = it }); OpsField("Briefing, safety and equipment",description,{ description = it },multiline = true)
        OpsField("Named coordinator",coordinator,{ coordinator = it }); OpsField("Start date and time",schedule,{ schedule = it },supporting = "Include the local timezone.")
        OpsField("Expected duration",duration,{ duration = it }); OpsField("Volunteer capacity",capacity,{ capacity = it },numeric = true); OpsField("Required skills, separated by commas",skills,{ skills = it })
        Feedback(result)
        OpsButton("Save mission",{ result = repository.saveMission(mission?.id,title,description,priority,location,capacity.toIntOrNull() ?: 0,duration,skills.split(','),coordinator,schedule); if (result?.success == true) close() },Modifier.fillMaxWidth())
    }
}
