@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.uuid.Uuid

private data class SafetyRequest(val path:String,val key:String,val body:JsonObject)

@Composable internal fun SafetyWorkspace(api:LiveApiTransport,org:String,region:String,onLock:(Boolean)->Unit,initialId:String?=null) {
    val base="/api/organizations/$org/regions/$region/safety"
    val scope=rememberCoroutineScope()
    var rows by remember{mutableStateOf(JsonArray(emptyList()))};var next by remember{mutableStateOf<Int?>(null)}
    var selected by remember{mutableStateOf<JsonObject?>(null)};var history by remember{mutableStateOf(JsonArray(emptyList()))}
    var historyNext by remember{mutableStateOf<Int?>(null)}
    var members by remember{mutableStateOf(JsonArray(emptyList()))}
    var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("OPEN")};var reason by remember{mutableStateOf("")}
    var action by remember{mutableStateOf("ACKNOWLEDGE")};var recipient by remember{mutableStateOf("")}
    var pending by remember{mutableStateOf<SafetyRequest?>(null)};var confirm by remember{mutableStateOf(false)}
    var intake by remember{mutableStateOf(false)};var summary by remember{mutableStateOf("")};var affected by remember{mutableStateOf("")}
    var severity by remember{mutableStateOf("HIGH")};var source by remember{mutableStateOf("PHONE")}
    val locked=busy||pending!=null
    DisposableEffect(Unit){onDispose{onLock(false)}};LaunchedEffect(locked){onLock(locked)}
    fun name(id:String)=members.firstOrNull{it.jsonObject.text("userId")==id}?.jsonObject?.get("user")?.jsonObject?.text("fullName").orEmpty().ifBlank{"Member $id"}
    suspend fun load(append:Boolean=false){
        val page=api.execute(ApiEndpoint(HttpMethod.Get,"$base/cases",buildMap{put("status",status);if(append)put("after",next.toString())}){it.jsonObject})
        rows=if(append)JsonArray((rows+page["items"]!!.jsonArray).distinctBy{it.jsonObject.text("id")})else page["items"]!!.jsonArray;next=page["next_cursor"]?.jsonPrimitive?.intOrNull
    }
    suspend fun detail(id:String,append:Boolean=false){
        if(selected?.text("id")!=id){reason="";action="ACKNOWLEDGE";recipient="";confirm=false}
        val page=api.execute(ApiEndpoint(HttpMethod.Get,"$base/cases/$id",if(append)mapOf("after" to historyNext.toString())else emptyMap()){it.jsonObject})
        selected=page["case"]!!.jsonObject;history=if(append)JsonArray(history+page["items"]!!.jsonArray)else page["items"]!!.jsonArray;historyNext=page["next_cursor"]?.jsonPrimitive?.intOrNull
    }
    fun run(block:suspend()->Unit){if(busy)return;busy=true;error="";scope.launch{try{block()}catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}
    fun send(path:String,body:JsonObject){val request=pending?:SafetyRequest(path,Uuid.random().toString(),body);pending=request;run{
        try{val result=api.execute(ApiEndpoint(HttpMethod.Post,request.path,headers=mapOf("Idempotency-Key" to request.key)){it.jsonObject},request.body);pending=null;intake=false;reason="";summary="";detail(result.text("id"));load()}
        catch(e:ApiFailure){if(e.status in 400..499&&e.status!=429)pending=null;throw e}
    }}
    LaunchedEffect(status,initialId){run{load();if(initialId!=null)detail(initialId)}}
    LaunchedEffect(org){try{members=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/memberships"){it.jsonArray})}catch(c:CancellationException){throw c}catch(_:Exception){}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Assistance",style=MaterialTheme.typography.headlineLarge);Text("Requests, ownership & response follow-through",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Button(enabled=!locked,onClick={intake=true;selected=null}){Text("Record request")}}
        Text("A recorded request is not confirmation that help has been dispatched.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotBlank())PortalNotice("Action not completed",error)
        pending?.let { p->TextButton(enabled=!busy,onClick={send(p.path,p.body)}){Text("Retry unconfirmed request")};TextButton(enabled=!busy,onClick={pending=null;run{load()}}){Text("Stop retrying & refresh")}}
        if(intake)Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Record an assistance request",style=MaterialTheme.typography.titleLarge)
                WorkspacePicker("Affected member",members.filter{it.jsonObject.text("status")=="ACTIVE"}.map{it.jsonObject.text("userId") to name(it.jsonObject.text("userId"))},affected,!locked,{affected=it})
                WorkspacePicker("Received through",listOf("PHONE" to "Phone","RADIO" to "Radio"),source,!locked,{source=it})
                WorkspacePicker("Priority",listOf("URGENT" to "Urgent · review within 5 min","HIGH" to "High · review within 15 min","STANDARD" to "Standard · review within 1 hour"),severity,!locked,{severity=it})
                OutlinedTextField(summary,{summary=it.take(3000)},label={Text("Situation and help requested")},minLines=3,enabled=!locked,modifier=Modifier.fillMaxWidth())
                Row{Button(enabled=!locked&&affected.toIntOrNull()!=null&&summary.trim().length>=10,onClick={send("$base/cases",buildJsonObject{put("summary",summary);put("affected_user_id",affected.toInt());put("severity",severity);put("source",source)})}){Text("Record request")};TextButton(enabled=!locked,onClick={intake=false}){Text("Cancel")}}
            }
        }
        selected?.let { record->val p=record["payload"]!!.jsonObject
            Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
                Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()){StatusPill(p.text("severity"));Spacer(Modifier.width(8.dp));StatusPill(p.text("status"));Spacer(Modifier.weight(1f));TextButton(enabled=!locked,onClick={selected=null}){Text("Close case")}}
                    Text(name(record.text("ownerId")),style=MaterialTheme.typography.headlineSmall)
                    Text(p.text("summary"))
                    Text("Received ${displayTime(p.text("opened_at"))} · ${p.text("source").lowercase()}",style=MaterialTheme.typography.bodySmall)
                    Text(if(p.text("coordinator_id").isBlank()||p.text("coordinator_id")=="null")"Unclaimed · acknowledgement due ${displayTime(p.text("response_due_at"))}" else "Case owner: ${name(p.text("coordinator_id"))}",style=MaterialTheme.typography.labelMedium)
                    (p["location"] as? JsonObject)?.let { l->Text("Shared location: ${l.text("latitude")}, ${l.text("longitude")}",style=MaterialTheme.typography.bodySmall)}
                    if(p.text("status")!="RESOLVED"){
                        val options=buildList{if(p.text("status")=="OPEN")add("ACKNOWLEDGE" to "Acknowledge & take ownership");if(p.text("status")=="ACKNOWLEDGED")add("START_RESPONSE" to "Record response in progress");if(p.text("status")!="OPEN"){add("HANDOVER" to "Hand over responsibility");add("RESOLVE" to "Resolve case")};add("ESCALATE" to "Record escalation")}
                        val chosen=action.takeIf{a->options.any{it.first==a}}?:options.first().first
                        WorkspacePicker("Decision",options,chosen,!locked,{action=it;confirm=false})
                        if(chosen=="HANDOVER")WorkspacePicker("Receiving coordinator",members.filter{it.jsonObject.text("status")=="ACTIVE"&&it.jsonObject.text("role") in listOf("ORG_ADMIN","COORDINATOR")}.map{it.jsonObject.text("userId") to name(it.jsonObject.text("userId"))},recipient,!locked,{recipient=it})
                        OutlinedTextField(reason,{reason=it.take(1500);confirm=false},label={Text("Decision and follow-up")},enabled=!locked,minLines=2,modifier=Modifier.fillMaxWidth().semantics { contentDescription = "Decision and follow-up" })
                        if(confirm)Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Text(options.first{it.first==chosen}.second,style=MaterialTheme.typography.titleMedium)
                                Text(reason,style=MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Button(enabled=!locked&&reason.trim().length>=10&&(chosen!="HANDOVER"||recipient.toIntOrNull()!=null),onClick={confirm=false;send("$base/cases/${record.text("id")}/decisions",buildJsonObject{put("action",chosen);put("reason",reason);put("expected_version",record["version"]!!);if(chosen=="HANDOVER")put("coordinator_id",recipient.toInt())})}){Text("Confirm decision")}
                                    TextButton(enabled=!locked,onClick={confirm=false}){Text("Go back")}
                                }
                            }
                        }else Button(enabled=!locked&&reason.trim().length>=10&&(chosen!="HANDOVER"||recipient.toIntOrNull()!=null),onClick={action=chosen;confirm=true}){Text("Review decision")}
                    }
                    HorizontalDivider();Text("Case timeline",style=MaterialTheme.typography.titleMedium)
                    history.forEach { event->val e=event.jsonObject["payload"]!!.jsonObject
                        Text(e.text("action").lowercase().replace('_',' ').replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.titleSmall)
                        Text(e.text("reason").ifBlank{e.text("summary")},style=MaterialTheme.typography.bodyMedium)
                        Text("${displayTime(e.text("at"))} · ${name(e.text("actor_id"))}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if(historyNext!=null)TextButton(enabled=!locked,onClick={run{detail(record.text("id"),true)}}){Text("Earlier / further activity")}
                    TextButton(enabled=!locked,onClick={run{detail(record.text("id"))}}){Text("Refresh case")}
                }
            }
        }
        Row(Modifier.fillMaxWidth()){WorkspacePicker("Queue",listOf("OPEN" to "Unacknowledged","ACKNOWLEDGED" to "Acknowledged","IN_PROGRESS" to "Response in progress","RESOLVED" to "Resolved"),status,!locked,{status=it});Spacer(Modifier.weight(1f));TextButton(enabled=!locked,onClick={run{load()}}){Text("Refresh")}}
        if(rows.isEmpty()&&!busy)Text("No ${status.lowercase().replace('_',' ')} cases in this region.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { item->val record=item.jsonObject;val p=record["payload"]!!.jsonObject
            Surface(onClick={run{detail(record.text("id"))}},enabled=!locked,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
                Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){Column(Modifier.weight(1f)){Text(name(record.text("ownerId")),style=MaterialTheme.typography.titleMedium);Text(p.text("title"),style=MaterialTheme.typography.bodySmall)};StatusPill(p.text("severity"))}
            }
        }
        if(next!=null)TextButton(enabled=!locked,onClick={run{load(true)}}){Text("Load more cases")}
    }
}
