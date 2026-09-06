package com.example.admin.features.communications

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


@Composable fun CommunicationsCommandScreen(repository: AdminRepository) {
    val alerts by repository.alerts.collectAsState(); val people by repository.volunteers.collectAsState()
    var composing by rememberSaveable { mutableStateOf(false) }; var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf("All") }
    OpsPage {
        item { PageTitle("The right message, clearly", "Prepare a team update and review who needs to receive it.","COMMUNICATIONS") { GlassIconButton(OpsSymbol.Send, "Prepare a broadcast", { composing = true }) } }
        item { LiquidGlassAlert("Drafts on this device","Your messages and recipient selections are saved locally. Connect an approved communications gateway before sending operational alerts.") }
        item { Choices(listOf("All","Emergency alerts","Team updates"),category) { category = it }; SectionTitle("Broadcast log","${alerts.size} saved records") }
        val shown = alerts.filter { category == "All" || it.channel == category }
        if (shown.isEmpty()) item { EmptyState("No broadcasts here","Prepare an update to start this log.") }
        items(shown,key = { it.id }) { alert -> OpsCard {
            Actions { StatusBadge(alert.priority); LiquidGlassBadge(alert.deliveryStatus,GlassVariant.DARK) }
            SectionTitle(alert.title,alert.timestamp); Text(alert.message)
            KeyValue("Audience","${alert.targetAudience} · ${alert.recipientsCount} selected")
            OpsButton("View broadcast record",{ selected = alert.id },Modifier.fillMaxWidth(),true)
        } }
    }
    alerts.firstOrNull { it.id == selected }?.let { alert -> DetailSheet("Broadcast record",{ selected = null }) {
        SectionTitle(alert.title); StatusBadge(alert.priority); Text(alert.message); KeyValue("Delivery",alert.deliveryStatus); KeyValue("Prepared by",alert.sender)
        SectionTitle("Selected recipients")
        alert.recipientIds.forEach { id -> Text(people.firstOrNull { it.id == id }?.name ?: id) }
        if (alert.recipientIds.isEmpty()) Text("This sample record has no individually selected recipients.")
    } }
    if (composing) BroadcastEditor(repository) { composing = false }
}
@Composable private fun BroadcastEditor(repository: AdminRepository, close: () -> Unit) {
    val people by repository.volunteers.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }; var message by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf("high") }; var audience by rememberSaveable { mutableStateOf("All volunteers") }
    var channel by rememberSaveable { mutableStateOf("Emergency alerts") }; var review by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<MutationResult?>(null) }
    val recipients = remember(people,audience) { repository.recipients(audience) }
    DetailSheet(if (review) "Review your broadcast" else "Prepare a broadcast",close, onBack = if (review) ({ review = false }) else null) {
        if (review) {
            SectionTitle(title); StatusBadge(severity); Text(message); KeyValue("Audience","$audience · ${recipients.size} people"); recipients.forEach { Text(it.name,style = MaterialTheme.typography.bodyMedium) }
            LiquidGlassAlert("Local draft","Saving creates a local broadcast record. It does not send SMS, push, email or satellite messages.")
            Feedback(result)
            OpsButton("Save reviewed broadcast",{ result = repository.sendEmergencyAlert(title,message,severity,channel,audience); if (result?.success == true) close() },Modifier.fillMaxWidth(),enabled = recipients.isNotEmpty())
        } else {
            OpsField("Subject",title,{ title = it }); Choices(listOf("critical","high","info"),severity) { severity = it }
            SectionTitle("Channel"); Choices(listOf("Emergency alerts","Team updates"),channel) { channel = it }
            SectionTitle("Audience"); Choices(listOf("All volunteers","Deployed volunteers","Available volunteers","Medical volunteers"),audience) { audience = it }
            Text("${recipients.size} people selected",color = AdminColors.Slate)
            OpsField("Message and requested action",message,{ message = it },multiline = true,supporting = "Include the location, source and a contact for questions.")
            OpsButton("Review recipients & message",{ review = true },Modifier.fillMaxWidth(),enabled = title.trim().length >= 3 && message.trim().length >= 10)
        }
    }
}
