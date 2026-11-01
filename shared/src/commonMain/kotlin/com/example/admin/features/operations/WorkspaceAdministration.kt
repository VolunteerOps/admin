@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import com.example.admin.features.auth.EnterpriseAccountPanel
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.uuid.Uuid

@Composable internal fun WorkspacePeople(api:LiveApiTransport,org:String,region:String,modifier:Modifier=Modifier) {
    val scope=rememberCoroutineScope()
    var members by remember{mutableStateOf(listOf<JsonObject>())};var more by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)};var notice by remember{mutableStateOf("")};var search by remember{mutableStateOf("")}
    var email by remember{mutableStateOf("")};var reason by remember{mutableStateOf("")};var invite by remember{mutableStateOf(false)}
    var selected by remember{mutableStateOf<JsonObject?>(null)};var confirm by remember{mutableStateOf(false)}
    suspend fun load(append:Boolean=false){
        val page=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/memberships",if(append)mapOf("after" to members.last().text("id"))else emptyMap()){it.jsonArray}).map{it.jsonObject}
        members=(if(append)members+page else page).distinctBy{it.text("id")};more=page.size==100
    }
    fun run(action:suspend()->Unit){if(busy)return;busy=true;notice="";scope.launch{try{action()}catch(c:CancellationException){throw c}catch(e:Exception){if(e is ApiFailure&&e.status in listOf(401,403)){members=emptyList();selected=null};notice=workspaceError(e)}finally{busy=false}}}
    LaunchedEffect(Unit){run{load()}}
    WorkspacePage("People & readiness","Workspace roster. Invitations grant only the selected region; verified qualifications are managed in Qualifications.",modifier) {
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(notice.isNotBlank())Text(notice)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(search,{search=it},label={Text("Search loaded members")},modifier=Modifier.weight(1f));TextButton(onClick={run{load()}},enabled=!busy){Text("Refresh")};Button(onClick={invite=!invite},enabled=!busy){Text("Invite volunteer")}}
        if(invite){
            OutlinedTextField(email,{email=it},label={Text("Volunteer email")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(reason,{reason=it},label={Text("Invitation reason (10–1000 characters)")},modifier=Modifier.fillMaxWidth())
            Button(enabled=!busy&&email.contains('@')&&reason.trim().length in 10..1000,onClick={run{api.execute(ApiEndpoint(HttpMethod.Post,"/api/organizations/$org/invitations"){it},buildJsonObject{put("recipient_email",email);put("role","VOLUNTEER");put("region_ids",JsonArray(listOf(JsonPrimitive(region))));put("reason",reason)});invite=false;email="";reason="";notice="Invitation queued. Delivery status is available in Settings recruitment."}}){Text("Send invitation")}
        }
        members.filter{val u=it["user"]!!.jsonObject;search.isBlank()||u.text("fullName").contains(search,true)||u.text("email").contains(search,true)}.forEach { member ->
            Surface(onClick={selected=member;reason=""},modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.small) {
                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){
                    Column(Modifier.weight(1f)){Text(member["user"]!!.jsonObject.text("fullName"),style=MaterialTheme.typography.titleMedium);Text(member["user"]!!.jsonObject.text("email"))}
                    Text(member.text("role").lowercase().replace('_',' '));Text(member.text("status").lowercase())
                }
            }
        }
        if(members.isEmpty()&&!busy&&notice.isBlank())Text("No members returned for this workspace.")
        if(more)TextButton(onClick={run{load(true)}},enabled=!busy){Text("Load more members")}
        selected?.let { member ->
            Text("Access review: "+member["user"]!!.jsonObject.text("fullName"),style=MaterialTheme.typography.titleLarge)
            Text("Only an authorized organization administrator can suspend access. Assignments and audit history remain recorded.")
            OutlinedTextField(reason,{reason=it},label={Text("Reason for suspending access")},modifier=Modifier.fillMaxWidth())
            Button(onClick={confirm=true},enabled=!busy&&member.text("status")=="ACTIVE"&&reason.trim().length in 10..1000){Text("Review suspension")}
        }
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Suspend this membership?")},text={Text("The member will lose access to this workspace. Reason: $reason")},confirmButton={Button(onClick={confirm=false;val member=selected!!;run{api.execute(ApiEndpoint(HttpMethod.Patch,"/api/organizations/$org/memberships/"+member.text("id")){it},buildJsonObject{put("status","SUSPENDED");put("expected_version",member["version"]!!);put("reason",reason)});selected=null;load();notice="Membership suspended."}}){Text("Suspend access")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
}

@Composable internal fun WorkspaceSecurity(api:LiveApiTransport,org:String,region:String,admin:Boolean,onLock:(Boolean)->Unit,modifier:Modifier=Modifier) {
    val scope=rememberCoroutineScope();val path="/api/organizations/$org/regions/$region/operations"
    var control by remember{mutableStateOf<JsonObject?>(null)};var audit by remember{mutableStateOf(listOf<JsonObject>())};var more by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)};var notice by remember{mutableStateOf("")};var reason by remember{mutableStateOf("")};var confirm by remember{mutableStateOf(false)}
    var pending by remember{mutableStateOf<Pair<String,JsonObject>?>(null)}
    DisposableEffect(Unit){onDispose{onLock(false)}}
    LaunchedEffect(busy,pending){onLock(busy||pending!=null)}
    suspend fun load(){control=api.execute(ApiEndpoint(HttpMethod.Get,path+"/dispatch"){it.jsonObject})}
    fun run(action:suspend()->Unit){if(busy)return;busy=true;notice="";scope.launch{try{action()}catch(c:CancellationException){throw c}catch(e:Exception){notice=workspaceError(e)}finally{busy=false}}}
    fun save(){
        val request=pending?:Pair(Uuid.random().toString(),buildJsonObject{put("action","SET_DISPATCH");put("paused",control?.get("paused")?.jsonPrimitive?.booleanOrNull!=true);put("expected_version",control?.get("version")?:JsonPrimitive(0));put("reason",reason)})
        pending=request
        run{try{api.execute(ApiEndpoint(HttpMethod.Post,path+"/commands",headers=mapOf("Idempotency-Key" to request.first)){it},request.second);pending=null;load();reason="";notice="Regional dispatch updated."}catch(e:ApiFailure){if(e.status in 400..499&&e.status!=429)pending=null;throw e}}
    }
    LaunchedEffect(Unit){run{load()}}
    WorkspacePage("Security & dispatch","Regional dispatch controls and organization audit history.",modifier) {
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(notice.isNotBlank())Text(notice)
        TextButton(onClick={run{load()}},enabled=!busy&&pending==null){Text("Refresh dispatch state")}
        control?.let { c ->
            val paused=c["paused"]?.jsonPrimitive?.booleanOrNull==true
            Text(if(paused)"Dispatch is paused" else "Dispatch is active",style=MaterialTheme.typography.headlineSmall)
            Text("A pause blocks new assignments and mission activation in this region. Existing responders can still check in, complete work and submit reports.")
            if(c.text("reason").isNotBlank())Text("Last reason: "+c.text("reason"))
            if(admin){OutlinedTextField(reason,{reason=it},label={Text("Reason for this change")},modifier=Modifier.fillMaxWidth());Button(onClick={confirm=true},enabled=!busy&&pending==null&&reason.trim().length in 10..1000){Text(if(paused)"Resume dispatch" else "Pause regional dispatch")}}
        }
        if(pending!=null){Text("Result unconfirmed. Retry safely with the same request.");TextButton(onClick={save()},enabled=!busy){Text("Retry dispatch change")};TextButton(onClick={pending=null;run{load()}},enabled=!busy){Text("Stop retrying & refresh")}}
        HorizontalDivider()
        Text("Organization audit",style=MaterialTheme.typography.headlineSmall)
        Text("Organization administrators with unrestricted membership can read the audit trail. Regional changes are also visible in Activity.")
        TextButton(onClick={run{audit=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/audit"){it.jsonArray}).map{it.jsonObject};more=audit.size==100}},enabled=!busy){Text("Load audit history")}
        audit.forEach{row->Text(row.text("action")+" · "+row.text("createdAt"));Text(row.text("reason"));HorizontalDivider()}
        if(more)TextButton(onClick={run{val page=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/audit",mapOf("after" to audit.last().text("id"))){it.jsonArray}).map{it.jsonObject};audit=(audit+page).distinctBy{it.text("id")};more=page.size==100}},enabled=!busy){Text("Load more audit events")}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Change regional dispatch?")},text={Text("This affects all coordinators in the selected region. Reason: $reason")},confirmButton={Button(onClick={confirm=false;save()}){Text("Confirm dispatch change")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
}

@Composable internal fun WorkspaceSettings(api:LiveApiTransport,org:String,modifier:Modifier=Modifier) {
    WorkspacePage("Workspace settings","Manage recruitment, institution verification and workspace units. Personal preferences are in My account.",modifier) {
        EnterpriseAccountPanel(org,false,{method,path,body->api.execute(ApiEndpoint(method,path){it},body)})
    }
}
