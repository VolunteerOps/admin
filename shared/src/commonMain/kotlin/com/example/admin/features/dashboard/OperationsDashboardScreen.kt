package com.example.admin.features.dashboard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.admin.core.data.AdminRepository
import com.example.admin.core.model.*
import com.example.admin.core.icons.*
import com.example.admin.core.theme.*

@Composable fun OperationsDashboardScreen(repository: AdminRepository, onNavigateToMissions: () -> Unit, onNavigateToCommunications: () -> Unit, onNavigateToMap: () -> Unit) {
    val incidents by repository.incidents.collectAsState()
    val people by repository.volunteers.collectAsState()
    val missions by repository.missions.collectAsState()
    val resources by repository.resources.collectAsState()
    val paused by repository.isSystemLockdown.collectAsState()
    var incidentId by remember { mutableStateOf<String?>(null) }
    var resourceId by remember { mutableStateOf<String?>(null) }
    val active = missions.filter { it.status == "active" }
    val gaps = active.sumOf { it.requiredVolunteers - it.volunteersAssigned }
    OpsPage {
        item { PageTitle("A clear view of your response", "People, priorities and the next useful action.", "OVERVIEW / ELENA VANCE") }
        item {
            Surface(color = AdminColors.Navy, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (paused) "Practice dispatch is paused" else "$gaps places need the right people", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(if (paused) "Resume the local dispatch exercise in Security when you are ready." else "Review the briefing and match available volunteers to your active missions.", color = Color(0xFFCBD5E1))
                    OpsButton("Review mission staffing", onNavigateToMissions, symbol = OpsSymbol.ArrowRight)
                }
            }
        }
        item { Metrics(listOf(
            Metric(incidents.count { it.status == "active" }.toString(),"Open incidents","In this workspace",OpsSymbol.Warning),
            Metric(people.count { it.status == "deployed" }.toString(),"People assigned","${people.size} registered volunteers",OpsSymbol.People),
            Metric(active.size.toString(),"Active missions","${missions.count { it.status == "pending" }} planned",OpsSymbol.Mission),
            Metric(incidents.sumOf { it.casualtiesReported }.toString(),"People needing support","Sample incident records",OpsSymbol.Heart)
        )) }
        item { SectionTitle("Needs attention", "Open an incident to review its status and support needs.") }
        items(incidents.filter { it.status != "resolved" }, key = { it.id }) { incident ->
            OpsCard {
                Actions { StatusBadge(incident.severity); StatusBadge(incident.status) }
                SectionTitle(incident.title, incident.region)
                Text("${incident.casualtiesReported} people needing support · ${incident.teamsDeployed} teams assigned", style = MaterialTheme.typography.bodyMedium, color = AdminColors.Slate)
                OpsButton("Review incident", { incidentId = incident.id }, secondary = true, symbol = OpsSymbol.ArrowRight)
            }
        }
        item {
            OpsCard {
                SectionTitle("Volunteer readiness", "Assignment status from the current registry")
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    val assigned = people.count { it.status == "deployed" }
                    Canvas(Modifier.size(88.dp)) {
                        drawArc(AdminColors.SoftSky, 0f, 360f, false, style = Stroke(14.dp.toPx()))
                        drawArc(AdminColors.VolunteerBlue, -90f, if (people.isEmpty()) 0f else 360f * assigned / people.size, false, style = Stroke(14.dp.toPx()))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("$assigned assigned", style = MaterialTheme.typography.titleMedium)
                        Text("${people.count { it.status == "available" }} available")
                        Text("${people.count { it.status == "standby" }} on standby", color = AdminColors.Slate)
                    }
                }
                OpsButton("View response teams", onNavigateToMap, secondary = true, symbol = OpsSymbol.Map)
            }
        }
        item { SectionTitle("Supplies at a glance", "Manage local stock and record allocations to incidents.") }
        items(resources, key = { it.id }) { resource ->
            OpsCard {
                SectionTitle(resource.name)
                Capacity(resource.available, resource.total, "${resource.unit} available")
                OpsButton("Manage stock", { resourceId = resource.id }, secondary = true, symbol = OpsSymbol.Box)
            }
        }
        item { OpsButton("Prepare a team update", onNavigateToCommunications, Modifier.fillMaxWidth(), symbol = OpsSymbol.Messages) }
    }
    incidents.find { it.id == incidentId }?.let { incident ->
        var status by remember(incident.id) { mutableStateOf(incident.status) }
        var count by remember(incident.id) { mutableStateOf(incident.casualtiesReported.toString()) }
        var result by remember { mutableStateOf<MutationResult?>(null) }
        DetailSheet("Incident review", { incidentId = null }) {
            SectionTitle(incident.title, incident.region)
            Text(incident.id, style = MaterialTheme.typography.bodySmall, color = AdminColors.Slate)
            Text("Use verified information from the response lead to update this sample record.")
            Choices(listOf("active","contained","resolved"), status) { status = it }
            OpsField("People needing support", count, { count = it }, numeric = true)
            KeyValue("Assigned response teams", incident.teamsDeployed.toString(), OpsSymbol.People)
            Feedback(result)
            OpsButton("Save incident update", { result = repository.updateIncident(incident.id,status,count.toIntOrNull() ?: -1) }, Modifier.fillMaxWidth())
            OpsButton("Manage response teams", { incidentId = null; onNavigateToMap() }, Modifier.fillMaxWidth(), secondary = true)
        }
    }
    resources.find { it.id == resourceId }?.let { resource ->
        var mode by remember { mutableStateOf("Allocate") }
        var quantity by remember { mutableStateOf("") }
        var target by remember { mutableStateOf(incidents.firstOrNull { it.status != "resolved" }?.id ?: "") }
        var result by remember { mutableStateOf<MutationResult?>(null) }
        DetailSheet("Manage supplies", { resourceId = null }) {
            SectionTitle(resource.name, "${resource.available} of ${resource.total} ${resource.unit} available")
            Choices(listOf("Allocate","Restock"), mode) { mode = it; result = null }
            if (mode == "Allocate") {
                Text("Choose an incident", style = MaterialTheme.typography.titleMedium)
                incidents.filter { it.status != "resolved" }.forEach { incident -> FilterChip(target == incident.id, { target = incident.id }, { Text(incident.title) }) }
            }
            OpsField("Quantity (${resource.unit})", quantity, { quantity = it }, numeric = true)
            Feedback(result)
            OpsButton(if (mode == "Allocate") "Record allocation" else "Record restock", {
                result = if (mode == "Allocate") repository.dispatchResource(resource.id,target,quantity.toIntOrNull() ?: 0) else repository.restockResource(resource.id,quantity.toIntOrNull() ?: 0)
                if (result?.success == true) quantity = ""
            }, Modifier.fillMaxWidth())
        }
    }
}
