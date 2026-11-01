package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

@Composable internal fun MissionDossier(
    api:LiveApiTransport,path:String,mission:JsonObject,admin:Boolean,userId:String,busy:Boolean,
    send:(JsonObject)->Unit,modifier:Modifier=Modifier,
) {
    val p=mission["payload"]!!.jsonObject
    val tabs=if(admin)listOf("Brief","Applications","Team","Field reports","Conversation","Contributions") else listOf("Brief","Team","Field reports","Conversation")
    var section by remember(mission.text("id")){mutableStateOf("Brief")}
    var records by remember {mutableStateOf(JsonArray(emptyList()))}
    var selected by remember {mutableStateOf<JsonObject?>(null)}
    var loading by remember {mutableStateOf(false)}
    var failure by remember {mutableStateOf("")}
    var after by remember {mutableStateOf<Int?>(null)}
    var cursor by remember {mutableStateOf(0)}
    var text by remember {mutableStateOf("")}
    val kind=when(section){"Applications"->"APPLICATION";"Team"->"ASSIGNMENT";"Field reports"->"REPORT";"Conversation"->"MESSAGE";"Contributions"->"HOURS";else->null}
    LaunchedEffect(section,mission.text("version"),cursor) {
        if(kind!=null) {
            loading=true;failure=""
            try {
                val page=api.execute(ApiEndpoint(HttpMethod.Get,"$path/records",mapOf("kind" to kind,"parent_id" to mission.text("id"),"after" to cursor.toString())){it.jsonObject})
                val incoming=page["items"]!!.jsonArray
                records=if(cursor==0)incoming else JsonArray((records+incoming).distinctBy{it.jsonObject.text("id")})
                after=page["next_cursor"]?.jsonPrimitive?.intOrNull
            }catch(c:CancellationException){throw c}catch(e:Exception){records=JsonArray(emptyList());failure=workspaceError(e)}finally{loading=false}
        }
    }
    Column(modifier,verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){StatusPill(p.text("status"));Text("Revision ${mission.text("version")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Text(recordTitle(mission),style=MaterialTheme.typography.headlineMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(p.text("meeting_point"),style=MaterialTheme.typography.bodySmall)
            Text(displayTime(p.text("starts_at")),style=MaterialTheme.typography.bodySmall)
            Text("${p.text("assigned_count")} / ${p.text("capacity")} assigned",style=MaterialTheme.typography.labelMedium)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            tabs.forEach { name -> FilterChip(selected=section==name,onClick={section=name;selected=null;cursor=0;records=JsonArray(emptyList())},enabled=!busy,label={Text(name)}) }
        }
        if(section=="Brief") {
            Text(p.text("brief"),style=MaterialTheme.typography.bodyMedium)
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                FlowRow(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(36.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Column {Text("Schedule",style=MaterialTheme.typography.labelMedium);Text(displayTime(p.text("starts_at")));Text("to ${displayTime(p.text("ends_at"))}")}
                    Column {Text("Required skills",style=MaterialTheme.typography.labelMedium);Text((p["required_skills"] as? JsonArray).orEmpty().joinToString{it.jsonPrimitive.content}.ifBlank{"No specialist qualification required"})}
                }
            }
            Text("Next step",style=MaterialTheme.typography.titleMedium)
            Text(when(p.text("status")){"DRAFT"->"Stage this mission to open volunteer applications.";"STAGED"->"Review applications and assignments before activating.";"ACTIVE"->"Monitor check-ins and field reports. Complete assignments before closing the mission.";"PAUSED"->"Review conditions with the team before resuming.";else->"This mission is closed. Its records remain available."},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(admin)FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                (when(p.text("status")){"DRAFT"->listOf("STAGED","CANCELLED");"STAGED"->listOf("ACTIVE","CANCELLED");"ACTIVE"->listOf("PAUSED","COMPLETED","CANCELLED");"PAUSED"->listOf("ACTIVE","CANCELLED");else->emptyList()}).forEach { target ->
                    OutlinedButton(enabled=!busy,onClick={send(buildJsonObject{put("action","TRANSITION_MISSION");put("id",mission.text("id"));put("expected_version",mission["version"]!!);put("status",target)})}) {Text(when(target){"STAGED"->"Stage mission";"ACTIVE"->if(p.text("status")=="PAUSED")"Resume mission" else "Activate mission";"PAUSED"->"Pause mission";"COMPLETED"->"Complete mission";else->"Cancel mission"})}
                }
            }
            if(!admin&&p.text("status") in listOf("STAGED","ACTIVE"))Button(enabled=!busy,onClick={send(buildJsonObject{put("action","APPLY");put("id",mission.text("id"))})}){Text("Apply to join")}
        } else {
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(failure.isNotBlank())PortalNotice("Could not load ${section.lowercase()}",failure)
            selected?.let { child ->
                TextButton(onClick={selected=null},enabled=!busy){Text("Back to ${section.lowercase()}")}
                RecordReview(api,path,child,records,admin,userId,busy,send)
            } ?: run {
                if(records.isEmpty()&&!loading&&failure.isBlank())Text("No ${section.lowercase()} for this mission yet.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                records.forEach { item -> val child=item.jsonObject;val payload=child["payload"]!!.jsonObject
                    Surface(onClick={selected=child},enabled=!busy,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(if(kind in listOf("APPLICATION","ASSIGNMENT"))"Member ${child.text("ownerId")}" else recordTitle(child),style=MaterialTheme.typography.titleSmall)
                            if(kind=="MESSAGE")Text(payload.text("body")) else Text(recordSubtitle(child),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(payload.text("status").isNotBlank())StatusPill(payload.text("status"))
                        }
                    }
                }
                if(after!=null)TextButton(onClick={cursor=after!!},enabled=!loading&&!busy){Text("Load more")}
                if(section=="Conversation"&&p.text("status")!="DRAFT") {
                    OutlinedTextField(text,{text=it.take(4000)},label={Text("Message to the mission team")},modifier=Modifier.fillMaxWidth(),minLines=2)
                    Button(enabled=!busy&&text.isNotBlank(),onClick={send(buildJsonObject{put("action","MESSAGE");put("id",mission.text("id"));put("body",text)});text=""}){Text("Send message")}
                }
            }
        }
    }
}
