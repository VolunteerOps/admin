package com.example.admin.features.volunteers

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


@Composable fun VolunteerManagementScreen(repository: AdminRepository) {
    val people by repository.volunteers.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val shown = people.filter { (filter == "all" || it.status == filter) && listOf(it.name,it.email,it.location,it.skills.joinToString()).any { value -> value.contains(query,true) } }
    OpsPage {
        item { PageTitle("People make the response", "Find the right skills. Understand the person. Make a clear assignment.", "VOLUNTEER REGISTRY") { GlassIconButton(OpsSymbol.Add, "Add volunteer", { adding = true }) } }
        item { SearchBox(query, { query = it }, "Search people, skills or places"); Choices(listOf("all","available","deployed","standby","unavailable"), filter) { filter = it } }
        item { Text("${shown.size} people · ${people.count { it.status == "available" }} available", style = MaterialTheme.typography.labelMedium, color = AdminColors.Slate) }
        if (shown.isEmpty()) item { EmptyState() }
        items(shown, key = { it.id }) { person -> PersonCard(person) { selected = person.id } }
    }
    selected?.let { VolunteerDossier(repository, it, { selected = null }) }
    if (adding) AddVolunteerSheet(repository) { adding = false }
}
@Composable fun PersonCard(person: VolunteerRecord, onOpen: () -> Unit) {
    OpsCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(AdminColors.SoftSky, CircleShape), contentAlignment = Alignment.Center) { Text(person.name.split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = AdminColors.VolunteerBlue, style = MaterialTheme.typography.titleLarge) }
            Column(Modifier.weight(1f)) { Text(person.name, style = MaterialTheme.typography.titleLarge); Text(person.location, style = MaterialTheme.typography.bodyMedium, color = AdminColors.Slate) }
        }
        Actions { StatusBadge(person.status); LiquidGlassBadge("${person.deploymentsCount} missions", GlassVariant.DARK) }
        Tags(person.skills)
        if (person.rating > 0) Text("${person.rating} / 5 · sample coordinator rating", style = MaterialTheme.typography.bodySmall, color = AdminColors.Slate)
        OpsButton("View volunteer", onOpen, Modifier.fillMaxWidth(), true, symbol = OpsSymbol.ArrowRight)
    }
}
@Composable private fun AddVolunteerSheet(repository: AdminRepository, close: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }; var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }; var location by rememberSaveable { mutableStateOf("") }
    var skills by rememberSaveable { mutableStateOf("") }; var result by remember { mutableStateOf<MutationResult?>(null) }
    DetailSheet("Add a volunteer", close) {
        Text("Add a person to the local registry. This does not create or invite an account.")
        OpsField("Full name", name, { name = it }); OpsField("Email", email, { email = it }); OpsField("Phone", phone, { phone = it }); OpsField("Location", location, { location = it }); OpsField("Skills, separated by commas", skills, { skills = it })
        Feedback(result)
        OpsButton("Add to registry", { result = repository.addVolunteer(name,email,phone,location,skills.split(',')); if (result?.success == true) close() }, Modifier.fillMaxWidth())
    }
}
@Composable fun VolunteerDossier(repository: AdminRepository, id: String, close: () -> Unit) {
    val people by repository.volunteers.collectAsState(); val assignments by repository.assignments.collectAsState(); val missions by repository.missions.collectAsState()
    val person = people.firstOrNull { it.id == id } ?: return
    var dispatch by remember { mutableStateOf(false) }; var release by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<MutationResult?>(null) }
    if (dispatch) { DeploymentSheet(repository,id,{ dispatch = false }); return }
    DetailSheet("Volunteer dossier", close) {
        SectionTitle(person.name,person.location); StatusBadge(person.status); Feedback(result)
        KeyValue("Email",person.email,OpsSymbol.Mail); KeyValue("Phone",person.phone,OpsSymbol.Phone); KeyValue("Emergency contact",person.emergencyContact,OpsSymbol.Heart)
        SectionTitle("Skills & readiness"); Tags(person.skills)
        Text(person.certifications.joinToString("\n").ifBlank { "No verified credentials recorded. Confirm qualifications with your organisation before assignment." })
        if (person.status != "deployed") { SectionTitle("Availability"); Choices(listOf("available","standby","unavailable"),person.status) { result = repository.updateVolunteerStatus(id,it) } }
        SectionTitle("Assignment history")
        val history = assignments.filter { it.volunteerId == id }
        if (history.isEmpty()) Text("No assignments recorded in this workspace.",color = AdminColors.Slate)
        history.reversed().forEach { assignment -> KeyValue(missions.firstOrNull { it.id == assignment.missionId }?.title ?: assignment.missionId,"${assignment.role} · ${if (assignment.active) "Active" else "Released"}\n${assignment.notes}") }
        when(person.status) {
            "available","standby" -> OpsButton("Assign to a mission", { dispatch = true },Modifier.fillMaxWidth(),symbol = OpsSymbol.Mission)
            "deployed" -> if (!release) OpsButton("Release assignment", { release = true },Modifier.fillMaxWidth(),true)
                else { LiquidGlassAlert("Confirm handover", "Release only after confirming the volunteer has handed over their work. This updates the local registry and mission capacity.",GlassVariant.WARNING); OpsButton("Confirm release", { result = repository.releaseVolunteer(id); release = false },Modifier.fillMaxWidth(),danger = true) }
        }
    }
}
@Composable fun DeploymentSheet(repository: AdminRepository, volunteerId: String, close: () -> Unit, initialMission: String? = null) {
    val people by repository.volunteers.collectAsState(); val missions by repository.missions.collectAsState()
    val person = people.firstOrNull { it.id == volunteerId } ?: return
    var target by rememberSaveable { mutableStateOf(initialMission ?: "") }; var role by rememberSaveable { mutableStateOf("") }; var notes by rememberSaveable { mutableStateOf("") }
    var reviewed by remember { mutableStateOf(false) }; var result by remember { mutableStateOf<MutationResult?>(null) }
    DetailSheet("Assign ${person.name}",close) {
        Text("Choose an active mission with an open place. Confirm the person’s suitability and briefing before recording the assignment.")
        val choices = missions.filter { it.status == "active" && it.volunteersAssigned < it.requiredVolunteers }
        if (choices.isEmpty()) EmptyState("No open assignments","Activate a planned mission or review current capacity.")
        choices.forEach { mission -> Row(Modifier.fillMaxWidth().clickable { target = mission.id },verticalAlignment = Alignment.CenterVertically) { RadioButton(target == mission.id,{ target = mission.id }); Column(Modifier.weight(1f)) { Text(mission.title,style = MaterialTheme.typography.titleMedium); Text("${mission.requiredVolunteers - mission.volunteersAssigned} places · ${mission.location}",style = MaterialTheme.typography.bodyMedium,color = AdminColors.Slate) } } }
        OpsField("Assigned role",role,{ role = it }); OpsField("Briefing and meeting instructions",notes,{ notes = it },multiline = true)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(reviewed,{ reviewed = it }); Text("I have reviewed availability, required skills and the briefing.",Modifier.weight(1f)) }
        Feedback(result)
        OpsButton("Record local assignment",{ result = repository.deployVolunteer(volunteerId,target,role,notes); if (result?.success == true) close() },Modifier.fillMaxWidth(),enabled = reviewed && target.isNotBlank())
    }
}
