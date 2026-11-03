package com.example.admin.features.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.icons.*
import com.example.admin.core.theme.*
import kotlinx.serialization.json.*

internal fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal val WorkspaceNavy: Color @Composable get() = MaterialTheme.colorScheme.onSurface
internal data class WorkspaceDestination(val title: String, val kind: String? = null, val group: String = "Response", val icon: OpsSymbol = OpsSymbol.Mission)
internal val workspaceDestinations = listOf(
    WorkspaceDestination("Overview", icon = OpsSymbol.Home),
    WorkspaceDestination("Missions", "MISSION"),
    WorkspaceDestination("People", icon = OpsSymbol.People),
    WorkspaceDestination("Applications", "APPLICATION", icon = OpsSymbol.People),
    WorkspaceDestination("Assignments", "ASSIGNMENT"),
    WorkspaceDestination("Reports", "REPORT", icon = OpsSymbol.Bell),
    WorkspaceDestination("Inbox", "MESSAGE", icon = OpsSymbol.Messages),
    WorkspaceDestination("Bulletins", "BULLETIN", icon = OpsSymbol.Bell),
    WorkspaceDestination("Deployment map", "MISSION", icon = OpsSymbol.Map),
    WorkspaceDestination("Sentinel", icon = OpsSymbol.Map),
    WorkspaceDestination("Assistance", icon = OpsSymbol.Heart),
    WorkspaceDestination("Weather analysis", icon = OpsSymbol.Chart),
    WorkspaceDestination("Resource planning", icon = OpsSymbol.Mission),
    WorkspaceDestination("Specialised", icon = OpsSymbol.Chart),
    WorkspaceDestination("Resources", "INVENTORY", "Readiness", OpsSymbol.More),
    WorkspaceDestination("Stock history", "STOCK_LEDGER", "Readiness"),
    WorkspaceDestination("Training", "COURSE", "Readiness", OpsSymbol.Book),
    WorkspaceDestination("Learning", "ENROLLMENT", "Readiness", OpsSymbol.Book),
    WorkspaceDestination("Qualifications", "CREDENTIAL", "Readiness", OpsSymbol.Shield),
    WorkspaceDestination("Hours", "HOURS", "Readiness", OpsSymbol.Clock),
    WorkspaceDestination("Insights", group = "Workspace", icon = OpsSymbol.Chart),
    WorkspaceDestination("Activity", "EVENT", "Workspace", OpsSymbol.Bell),
    WorkspaceDestination("Security", group = "Workspace", icon = OpsSymbol.Shield),
    WorkspaceDestination("Settings", group = "Workspace", icon = OpsSymbol.Settings),
    WorkspaceDestination("Support", "SUPPORT", "Workspace", OpsSymbol.Heart),
    WorkspaceDestination("Profile", group = "Workspace", icon = OpsSymbol.People),
    WorkspaceDestination("Team Pro", group = "Workspace", icon = OpsSymbol.People),
    WorkspaceDestination("Pro", group = "Workspace", icon = OpsSymbol.Chart),
)

internal data class WorkspaceArea(val title: String, val icon: OpsSymbol, val pages: List<String>)
internal val workspaceAreas = listOf(
    WorkspaceArea("Overview", OpsSymbol.Home, listOf("Overview")),
    WorkspaceArea("Missions", OpsSymbol.Mission, listOf("Missions", "Resource planning", "Applications", "Assignments", "Reports", "Hours", "Deployment map")),
    WorkspaceArea("People", OpsSymbol.People, listOf("People", "Training", "Learning", "Qualifications")),
    WorkspaceArea("Sentinel", OpsSymbol.Map, listOf("Sentinel", "Weather analysis", "Specialised", "Assistance")),
    WorkspaceArea("Communications", OpsSymbol.Messages, listOf("Inbox", "Bulletins")),
    WorkspaceArea("Resources", OpsSymbol.Box, listOf("Resources", "Stock history")),
    WorkspaceArea("Administration", OpsSymbol.Settings, listOf("Settings", "Security", "Activity", "Insights", "Team Pro")),
)
internal fun workspaceArea(tab: String) = workspaceAreas.firstOrNull { tab in it.pages }
internal fun pageLabel(tab: String) = when(tab) {
    "Learning" -> "Training reviews"; "Inbox" -> "Conversations"; "Security" -> "Dispatch controls"
    "Team Pro" -> "Sponsorship"; "Hours" -> "Contributions"; "Activity" -> "Audit trail"; "Sentinel" -> "Radar"
    else -> tab
}

@Composable internal fun WorkspaceShell(
    tab: String, memberships: JsonArray, regions: JsonArray, org: String, region: String,
    locked: Boolean, onNavigate: (String) -> Unit, onOrganization: (String) -> Unit,
    onRegion: (String) -> Unit, onLogout: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val appearance = LocalPortalAppearance.current
    val area = workspaceArea(tab)
    var account by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        val wide = maxWidth >= 1000.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) WorkspaceNavigation(tab, locked, onNavigate, Modifier.width(216.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!wide) TextButton(onClick = { menu = true }, enabled = !locked) { OpsIcon(OpsSymbol.More,Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Menu") }
                        WorkspacePicker("Workspace", memberships.map { it.jsonObject.text("organization_id") to it.jsonObject.text("organization_name") }, org, !locked, onOrganization)
                        WorkspacePicker("Region", regions.map { it.jsonObject.text("id") to it.jsonObject.text("name") }, region, !locked, onRegion)
                        TextButton(onClick = appearance.toggle) { Text(if(appearance.dark) "Light mode" else "Dark mode") }
                        Box {
                            TextButton(onClick = { account = true }, enabled = !locked) { OpsIcon(OpsSymbol.Profile,Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("Account") }
                            DropdownMenu(account,{account=false}) {
                                listOf("Profile","Pro","Support").forEach { page -> DropdownMenuItem(text={Text(page)},onClick={account=false;onNavigate(page)}) }
                                HorizontalDivider()
                                DropdownMenuItem(text={Text("Sign out")},onClick={account=false;onLogout()})
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if(area != null && area.pages.size > 1) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        area.pages.forEach { page ->
                            FilterChip(selected=tab==page,onClick={onNavigate(page)},enabled=!locked,label={Text(pageLabel(page))})
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            }
        }
        if (menu) AlertDialog(onDismissRequest = { menu = false }, confirmButton = { TextButton(onClick = { menu = false }) { Text("Close menu") } }, text = {
            WorkspaceNavigation(tab, locked, { menu = false; onNavigate(it) }, Modifier.fillMaxWidth().heightIn(max = 560.dp))
        })
    }
}

@Composable private fun WorkspaceNavigation(tab: String, locked: Boolean, navigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.padding(horizontal=12.dp,vertical=22.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OpsIcon(OpsSymbol.Shield,Modifier.size(28.dp),MaterialTheme.colorScheme.primary)
            Text("ResQOps", style = MaterialTheme.typography.headlineSmall)
        }
        Text("COORDINATION",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(start=12.dp,bottom=10.dp))
        workspaceAreas.forEach { area ->
            if(area.title=="Administration")HorizontalDivider(Modifier.padding(vertical=14.dp),color=MaterialTheme.colorScheme.outlineVariant)
            val selected=tab in area.pages
                Surface(onClick = { navigate(area.pages.first()) }, enabled=!locked, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, contentColor=if(selected)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OpsIcon(area.icon, Modifier.size(20.dp))
                        Text(area.title, style = MaterialTheme.typography.labelLarge)
                    }
                }
        }
        Spacer(Modifier.height(28.dp))
        Box(Modifier.fillMaxWidth().height(120.dp).background(ResQPalette.Konjo,MaterialTheme.shapes.medium)) {
            ContourPattern(Modifier.fillMaxSize())
            Text("Prepared.\nTogether.",Modifier.align(Alignment.CenterStart).padding(18.dp),style=MaterialTheme.typography.titleSmall,color=Color.White)
        }
        TextButton(onClick={navigate("Support")},enabled=!locked){OpsIcon(OpsSymbol.Heart,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Help & support")}
    }
}

@Composable internal fun WorkspacePicker(label: String, options: List<Pair<String, String>>, selected: String, enabled: Boolean, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty()) { Text(options.firstOrNull { it.first == selected }?.second?.ifBlank { label } ?: "Select $label") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name.ifBlank { label }) }, onClick = { expanded = false; onSelect(id) }) }
        }
    }
}

@Composable internal fun WorkspacePage(title: String, description: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = WorkspaceNavy)
        if(description.isNotBlank())Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        content()
    }
}
