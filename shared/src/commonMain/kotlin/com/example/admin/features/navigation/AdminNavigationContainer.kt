package com.example.admin.features.navigation
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.painterResource
import admin.shared.generated.resources.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.admin.core.data.AdminRepository
import com.example.admin.core.icons.*
import com.example.admin.core.theme.*
import com.example.admin.features.dashboard.*
import com.example.admin.features.volunteers.*
import com.example.admin.features.missions.*
import com.example.admin.features.communications.*
import com.example.admin.features.map.*
import com.example.admin.features.skills.*
import com.example.admin.features.analytics.*
import com.example.admin.features.security.*
import com.example.admin.features.settings.*
import com.example.admin.features.help.*

enum class AdminTab(val title: String, val symbol: OpsSymbol) {
    OPERATIONS("Overview",OpsSymbol.Home), VOLUNTEERS("People",OpsSymbol.People), MISSIONS("Missions",OpsSymbol.Mission), COMMUNICATIONS("Messages",OpsSymbol.Messages),
    NOTIFICATIONS("Notifications",OpsSymbol.Bell), MORE("More",OpsSymbol.More), MAP("Deployment map",OpsSymbol.Map), SKILLS("Skills",OpsSymbol.Book), ANALYTICS("Insights",OpsSymbol.Chart), SECURITY("Security",OpsSymbol.Shield), SETTINGS("Settings",OpsSymbol.Settings), HELP("Help & support",OpsSymbol.Heart)
}
@Composable fun AdminNavigationContainer(repository: AdminRepository, onLogout: () -> Unit) {
    var tabName by rememberSaveable { mutableStateOf(AdminTab.OPERATIONS.name) }
    val tab = AdminTab.valueOf(tabName)
    val primary = listOf(AdminTab.OPERATIONS,AdminTab.VOLUNTEERS,AdminTab.MISSIONS,AdminTab.COMMUNICATIONS,AdminTab.MORE)
    val extra = listOf(AdminTab.NOTIFICATIONS,AdminTab.MAP,AdminTab.SKILLS,AdminTab.ANALYTICS,AdminTab.SECURITY,AdminTab.SETTINGS,AdminTab.HELP)
    var returnTab by rememberSaveable { mutableStateOf(AdminTab.OPERATIONS.name) }
    val warning by repository.storageWarning.collectAsState()
    fun navigate(next: AdminTab) { if (next in extra && tab !in extra) returnTab = tabName; tabName = next.name }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        val wide = maxWidth >= 840.dp
        Column {
            Column(Modifier.zIndex(1f).shadow(4.dp).background(AdminColors.Navy).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
            Row(Modifier.fillMaxWidth().background(AdminColors.Navy).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(Res.drawable.app_logo_monochrome), "VolunteerOps", Modifier.size(34.dp))
                Column(Modifier.weight(1f)) {
                    Text("VolunteerOps", style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
                    Text("Coordination · Practice workspace", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFDBFE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton({ navigate(AdminTab.NOTIFICATIONS) }) { OpsIcon(OpsSymbol.Bell, tint = Color.White, contentDescription = "Notifications") }
                IconButton({ navigate(AdminTab.SETTINGS) }) { OpsIcon(OpsSymbol.Settings, tint = Color.White, contentDescription = "Settings") }
            }
            }
            Row(Modifier.weight(1f)) {
                if (wide) Column(Modifier.width(208.dp).fillMaxHeight().background(Color.White).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (primary.filter { it != AdminTab.MORE } + extra).forEach { item ->
                        NavigationDrawerItem(label = { Text(item.title) }, selected = item == tab, onClick = { navigate(item) }, icon = { OpsIcon(item.symbol) })
                    }
                    TextButton(onLogout) { OpsIcon(OpsSymbol.Logout); Spacer(Modifier.width(8.dp)); Text("Leave workspace") }
                }
                Column(Modifier.weight(1f).fillMaxHeight().background(AdminColors.CanvasBg)) {
                    if (warning != null) Text(warning!!, Modifier.fillMaxWidth().background(Color(0xFFFFF7ED)).padding(12.dp), style = MaterialTheme.typography.bodySmall, color = AdminColors.WarningAmber)
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalPageBack provides if (tab in extra) ({ tabName = returnTab }) else null) {
                        when(tab) {
                            AdminTab.NOTIFICATIONS -> WorkspaceNotifications(repository, { navigate(AdminTab.COMMUNICATIONS) }, { navigate(AdminTab.SECURITY) })
                            AdminTab.OPERATIONS -> OperationsDashboardScreen(repository, { navigate(AdminTab.MISSIONS) }, { navigate(AdminTab.COMMUNICATIONS) }, { navigate(AdminTab.MAP) })
                            AdminTab.VOLUNTEERS -> VolunteerManagementScreen(repository)
                            AdminTab.MISSIONS -> MissionControlScreen(repository) { navigate(AdminTab.VOLUNTEERS) }
                            AdminTab.COMMUNICATIONS -> CommunicationsCommandScreen(repository)
                            AdminTab.MAP -> DeploymentMapScreen(repository)
                            AdminTab.SKILLS -> SkillsDatabaseScreen(repository)
                            AdminTab.ANALYTICS -> AnalyticsCommandScreen(repository)
                            AdminTab.SECURITY -> SecurityProtocolsScreen(repository)
                            AdminTab.SETTINGS -> AdminSettingsScreen(repository)
                            AdminTab.HELP -> HelpSupportScreen(repository)
                            AdminTab.MORE -> OpsPage {
                                item { PageTitle("Your coordination toolkit", "Resources for a well-prepared response.", "WORKSPACE") }
                                items(extra) { item ->
                                    Surface(onClick = { navigate(item) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp,AdminColors.BorderLight)) {
                                        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OpsIcon(item.symbol, tint = AdminColors.VolunteerBlue)
                                            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                            OpsIcon(OpsSymbol.ArrowRight, tint = AdminColors.Slate)
                                        }
                                    }
                                }
                                item { OpsButton("Leave practice workspace", onLogout, Modifier.fillMaxWidth(), true, symbol = OpsSymbol.Logout) }
                            }
                        } }
                    }
                }
            }
            if (!wide) NavigationBar(containerColor = Color.White, windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)) {
                primary.forEach { item -> NavigationBarItem(selected = tab == item || item == AdminTab.MORE && tab in extra, onClick = { navigate(item) }, icon = { LivingNavIcon(item.symbol, tab == item || item == AdminTab.MORE && tab in extra) }, label = { Text(item.title, style = MaterialTheme.typography.labelSmall, maxLines = 1) }) }
            }
        }
    }
}


@Composable private fun WorkspaceNotifications(repository: AdminRepository, onMessages: () -> Unit, onSecurity: () -> Unit) {
    val alerts by repository.alerts.collectAsState()
    val events by repository.securityEvents.collectAsState()
    OpsPage {
        item { PageTitle("Notifications", "Broadcast records and security activity saved in this practice workspace.") }
        item { SectionTitle("Team updates", "${alerts.size} local broadcast records") }
        if (alerts.isEmpty()) item { EmptyState("No team updates", "Prepared broadcasts will appear here.") }
        items(alerts, key = { "alert-${it.id}" }) { alert -> OpsCard {
            StatusBadge(alert.priority)
            SectionTitle(alert.title, alert.timestamp)
            Text(alert.message)
            KeyValue("Status", alert.deliveryStatus)
            OpsButton("Open communications", onMessages, secondary = true, symbol = OpsSymbol.Messages)
        } }
        item { SectionTitle("Security activity") }
        items(events, key = { "event-${it.id}" }) { event -> OpsCard {
            SectionTitle(event.type, event.timestamp)
            Text(event.message)
            OpsButton("Review security activity", onSecurity, secondary = true, symbol = OpsSymbol.Shield)
        } }
    }
}
