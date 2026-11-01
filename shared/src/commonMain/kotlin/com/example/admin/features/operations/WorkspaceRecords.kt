package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*
import kotlin.time.Clock

internal val ColorTeal = Color(0xFF0F766E)
internal fun recordStatuses(kind:String):List<String> = com.example.admin.business.OperationsPolicy.statuses(kind)
internal fun recordTitle(record:JsonObject):String {
    val p=record["payload"]!!.jsonObject
    return listOf("title","mission_title","course_title","skill","action","body").firstNotNullOfOrNull { p.text(it).takeIf(String::isNotBlank) }
        ?: record.text("kind").lowercase().replace('_',' ')
}
internal fun recordSubtitle(record:JsonObject):String {
    val p=record["payload"]!!.jsonObject
    return when(record.text("kind")) {
        "MISSION" -> "${p.text("meeting_point")} · ${p.text("assigned_count")}/${p.text("capacity")} assigned · ${p.text("starts_at") }"
        "INVENTORY" -> "${(p["quantity"]?.jsonPrimitive?.intOrNull?:0)-(p["reserved"]?.jsonPrimitive?.intOrNull?:0)} available · ${p.text("reserved")} reserved · ${p.text("unit")} · ${p.text("location")}"
        "HOURS" -> "${p.text("submitted_hours")} submitted · ${p.text("approved_hours").ifBlank{"awaiting review"}} approved"
        "CREDENTIAL" -> "Valid until ${p.text("valid_until")} · Member ${record.text("ownerId") }"
        "EVENT" -> "${p.text("record_kind")} · Version ${p.text("version")}"
        "STOCK_LEDGER" -> "Change ${p.text("delta")} · ${p.text("reason")}"
        else -> p.text("brief").ifBlank{p.text("body")}.ifBlank{p.text("skill")}.ifBlank{"Member ${record.text("ownerId")}"}
    }
}
internal fun recordDescription(tab:String)=when(tab) {
    "Bulletins" -> "Published notices for members of this region. Publication does not confirm email, SMS or push delivery."
    "Missions" -> "Plan, stage and coordinate your region’s response."
    "Applications" -> "Review volunteer applications and reserve supplies when assigning."
    "Assignments" -> "Follow the team from assignment through check-in and completion."
    "Reports" -> "Review field evidence and record an accountable triage decision."
    "Inbox" -> "Mission conversations. Open a mission to start a new message."
    "Resources" -> "Stock available for the response, with reservations and movement history."
    "Training" -> "Publish courses and build a prepared, qualified team."
    "Learning" -> "Review training submissions and track lesson progress."
    "Qualifications" -> "Verified skills, expiry dates and credential revocation."
    "Hours" -> "Reconcile contributions. A different coordinator approves submitted hours."
    "Activity" -> "Recorded changes in this region. Refresh to load recent activity."
    "Deployment map" -> "Mission meeting points, not responder tracking. Load more to include additional missions."
    "Support" -> "Workspace support and guidance. Use your established emergency channel for urgent response."
    else -> "Review your region’s saved activity."
}

@Composable internal fun BulletinCreation(enabled:Boolean,send:(JsonObject)->Unit) {
    var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var urgent by remember{mutableStateOf(false)}
    Text("Publish a regional bulletin",style=MaterialTheme.typography.headlineSmall)
    Text("Audience: members with access to the current region. This publishes in the workspace; it does not send emergency alerts to devices.")
    OutlinedTextField(title,{title=it},label={Text("Title")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(body,{body=it},label={Text("Message and requested action")},minLines=4,modifier=Modifier.fillMaxWidth())
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(urgent,{urgent=it});Text("Mark as urgent")}
    Button(enabled=enabled&&title.trim().length in 3..160&&body.trim().length in 10..4000,onClick={send(buildJsonObject{put("action","PUBLISH_BULLETIN");put("title",title);put("body",body);put("priority",if(urgent)"URGENT"else "NOTICE")})}){Text("Review publication")}
}

@Composable internal fun RecordTable(records:List<JsonObject>,selected:String?,wide:Boolean,enabled:Boolean,onSelect:(JsonObject)->Unit,modifier:Modifier=Modifier) {
    Surface(modifier,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        LazyColumn {
            item { Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text("Details",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);Text("Status",Modifier.width(if(wide)140.dp else 100.dp),style=MaterialTheme.typography.labelMedium);if(wide)Text("Updated",Modifier.width(110.dp),style=MaterialTheme.typography.labelMedium)};HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant) }
            items(records,key={it.text("id")}) { record ->
                val p=record["payload"]!!.jsonObject
                Surface(onClick={onSelect(record)},enabled=enabled,color=if(selected==record.text("id"))MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(recordTitle(record),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium)
                            Text(recordSubtitle(record),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(Modifier.width(if(wide)140.dp else 100.dp)){StatusPill(p.text("status").ifBlank{if(record.text("kind")=="INVENTORY")"IN_STOCK" else "RECORDED"})}
                        if(wide)Text(record.text("updatedAt").take(10),Modifier.width(110.dp),style=MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable internal fun SupportCreation(enabled:Boolean,send:(JsonObject)->Unit) {
    var subject by remember{mutableStateOf("")};var body by remember{mutableStateOf("")}
    Text("Open a support case",style=MaterialTheme.typography.headlineSmall)
    OutlinedTextField(subject,{subject=it},label={Text("Subject")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(body,{body=it},label={Text("How can we help?")},minLines=4,modifier=Modifier.fillMaxWidth())
    Button(enabled=enabled&&subject.trim().length in 3..160&&body.trim().length in 10..5000,onClick={send(buildJsonObject{put("action","SUPPORT");put("title",subject);put("body",body)})}){Text("Submit case")}
}

@Composable internal fun MissionLocations(records:List<JsonObject>,modifier:Modifier=Modifier) {
    val data=buildJsonObject {
        put("type","resq.evidence");put("expires_at",Clock.System.now().toEpochMilliseconds()+45000)
        put("points",buildJsonArray { records.forEach { record -> val p=record["payload"]!!.jsonObject
            if(p["latitude"]?.jsonPrimitive?.doubleOrNull!=null&&p["longitude"]?.jsonPrimitive?.doubleOrNull!=null) add(buildJsonObject { put("kind","mission");put("latitude",p["latitude"]!!);put("longitude",p["longitude"]!!);put("label",recordTitle(record)+" / "+p.text("status")) })
        } })
    }
    EvidenceMap(data.toString(),modifier)
}
