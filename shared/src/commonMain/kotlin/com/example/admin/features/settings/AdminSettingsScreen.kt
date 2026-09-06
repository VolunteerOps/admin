package com.example.admin.features.settings

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


@Composable fun AdminSettingsScreen(repository: AdminRepository) {
    val saved by repository.settings.collectAsState()
    var organization by rememberSaveable(saved.organizationName) {mutableStateOf(saved.organizationName)}
    var timezone by rememberSaveable(saved.timezone) {mutableStateOf(saved.timezone)}
    var timeout by rememberSaveable(saved.sessionTimeoutMinutes) {mutableStateOf(saved.sessionTimeoutMinutes.toString())}
    var result by remember {mutableStateOf<MutationResult?>(null)}
    OpsPage {
        item {PageTitle("Set up your workspace","Preferences for your organisation’s response.","SETTINGS")}
        item {Feedback(result)}
        item {OpsCard {
            SectionTitle("Organisation"); OpsField("Organisation name",organization,{organization=it}); OpsField("Timezone",timezone,{timezone=it},supporting="For example, Africa/Nairobi. Include timezone in mission briefings.")
            OpsButton("Save organisation details",{result=repository.updateSettings(saved.copy(organizationName=organization,timezone=timezone))},Modifier.fillMaxWidth())
        }}
        item {OpsCard {
            SectionTitle("Notification preferences","These are stored preferences. No external gateway is connected.")
            ToggleRow("Email","Mission summaries and nonurgent updates",saved.emailAlerts) {result=repository.updateSettings(saved.copy(emailAlerts=it))}
            ToggleRow("SMS","Urgent text-message preference",saved.smsAlerts) {result=repository.updateSettings(saved.copy(smsAlerts=it))}
            ToggleRow("Push","Updates on connected devices",saved.pushAlerts) {result=repository.updateSettings(saved.copy(pushAlerts=it))}
        }}
        item {OpsCard {
            SectionTitle("Preferred access policy","These preferences must be enforced by a connected identity provider.")
            ToggleRow("Require two-factor authentication","Policy preference for live accounts",saved.twoFactorAuth) {result=repository.updateSettings(saved.copy(twoFactorAuth=it))}
            OpsField("Session timeout, minutes",timeout,{timeout=it},numeric=true,supporting="Choose between 5 and 120 minutes.")
            OpsButton("Save policy preference",{result=repository.updateSettings(saved.copy(sessionTimeoutMinutes=timeout.toIntOrNull() ?: 0))},Modifier.fillMaxWidth(),true)
        }}
        item {OpsCard {SectionTitle("Language preference"); Choices(listOf("English","Spanish","French"),saved.language) {result=repository.updateSettings(saved.copy(language=it))}; Text("Contact language preference. The current interface is in English.",color=AdminColors.Slate)}}
        item {LiquidGlassAlert("Integrations","API access, webhooks and message forwarding are not connected. No switch on this device enables a remote service.")}
    }
}
