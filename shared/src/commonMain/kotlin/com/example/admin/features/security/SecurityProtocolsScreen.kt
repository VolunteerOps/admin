package com.example.admin.features.security

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


@Composable fun SecurityProtocolsScreen(repository: AdminRepository) {
    val paused by repository.isSystemLockdown.collectAsState(); val events by repository.securityEvents.collectAsState()
    val preferences by repository.settings.collectAsState()
    var confirm by remember {mutableStateOf(false)}; var result by remember {mutableStateOf<MutationResult?>(null)}
    var filter by rememberSaveable {mutableStateOf("All")}
    OpsPage {
        item {PageTitle("A workspace you understand","Know which controls are active and which require your organisation’s services.","SECURITY & READINESS")}
        item {OpsCard {
            StatusBadge(if(paused) "paused" else "active"); SectionTitle("Local dispatch control",if(paused) "New assignments are paused" else "Practice assignments are enabled")
            Text("This device control prevents new volunteer, team and resource assignments during the exercise. Existing records stay available.")
            OpsButton(if(paused) "Resume practice dispatch" else "Pause practice dispatch",{confirm=true},Modifier.fillMaxWidth(),danger=!paused)
        }}
        item {Feedback(result)}
        item {OpsCard {
            SectionTitle("Identity & access"); KeyValue("Current session","Local demonstration · no authenticated account",OpsSymbol.Profile)
            KeyValue("Two-factor authentication","${if(preferences.twoFactorAuth) "Requested" else "Not requested"} in policy preferences; not enforced without an identity provider",OpsSymbol.Shield)
            KeyValue("Session expiry","${preferences.sessionTimeoutMinutes} minutes preferred; no live authenticated session",OpsSymbol.Clock)
            KeyValue("Data location","On this device. Platform application storage is used; no credentials are stored.",OpsSymbol.Box)
        }}
        item {LiquidGlassAlert("Before operational use","Connect and test identity, roles, credential verification, session expiry, access-controlled communications and an approved delivery gateway. The local controls are not an organisation-wide lockdown.",GlassVariant.WARNING)}
        item {SectionTitle("Workspace activity","Actions recorded on this device"); Choices(listOf("All","Recent 10"),filter) {filter=it}}
        items(if(filter=="Recent 10") events.take(10) else events,key={it.id}) { event -> OpsCard {
            Text(event.message,style=MaterialTheme.typography.titleMedium); Text(event.timestamp,style=MaterialTheme.typography.bodySmall,color=AdminColors.Slate); LiquidGlassBadge(event.status,GlassVariant.DARK)
        }}
    }
    if(confirm) DetailSheet(if(paused) "Resume dispatch?" else "Pause new assignments?",{confirm=false}) {
        Text(if(paused) "New practice assignments can be recorded again on this device." else "New assignments and resource allocations will be paused. You can still review records and release volunteers.")
        OpsButton(if(paused) "Resume dispatch" else "Confirm pause",{result=repository.triggerLockdown(!paused); confirm=false},Modifier.fillMaxWidth(),danger=!paused)
    }
}
