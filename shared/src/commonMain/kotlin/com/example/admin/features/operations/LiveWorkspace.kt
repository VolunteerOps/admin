@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import com.example.admin.core.platform.readWorkspaceRoute
import com.example.admin.core.platform.writeWorkspaceRoute
import com.example.admin.features.auth.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.uuid.Uuid

@Composable fun LiveWorkspace(onLogout: () -> Unit) {
    val api = remember { LiveApiTransport(FirebaseTokenProvider {
        when (val r = IdentityServices.firebase.refreshIdentity()) { is IdentityResult.SignedIn -> r.identity.idToken; else -> null }
    }) }
    DisposableEffect(api) { onDispose { api.close() } }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(workspaceDestinations.firstOrNull{it.title.lowercase().replace(' ','-')==readWorkspaceRoute()}?.title ?: "Overview") }
    var focusRecord by remember { mutableStateOf<String?>(null) }
    var org by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var planningAssessment by remember(org, region) { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf("") }
    var memberships by remember { mutableStateOf(JsonArray(emptyList())) }
    var regions by remember { mutableStateOf(JsonArray(emptyList())) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(tab) {
        val route=tab.lowercase().replace(' ','-')
        if(readWorkspaceRoute()!=null && readWorkspaceRoute()!=route)writeWorkspaceRoute(route)
    }
    LaunchedEffect(Unit) {
        if(readWorkspaceRoute()!=null)while(true){
            delay(200)
            val route=readWorkspaceRoute()
            val destination=workspaceDestinations.firstOrNull{it.title.lowercase().replace(' ','-')==route}?.title
            if(destination!=null&&destination!=tab){if(locked)writeWorkspaceRoute(tab.lowercase().replace(' ','-'),replace=true)else {focusRecord=null;tab=destination}}
        }
    }
    LaunchedEffect(reload) {
        loading = true; error = ""
        try {
            val me = api.execute(ApiEndpoint(HttpMethod.Get, "/api/auth/me") { it.jsonObject })
            userId = me["user"]?.jsonObject?.text("id").orEmpty()
            memberships = JsonArray(me["memberships"]!!.jsonArray.filter { it.jsonObject.text("access") == "ALLOWED" })
            if (memberships.none { it.jsonObject.text("organization_id") == org }) org = memberships.firstOrNull()?.jsonObject?.text("organization_id").orEmpty()
        } catch (c: CancellationException) { throw c }
        catch (e: Exception) { memberships=JsonArray(emptyList());org="";error=workspaceError(e) }
        finally { loading = false }
    }
    LaunchedEffect(org, reload) {
        regions=JsonArray(emptyList());region=""
        if (org.isNotBlank()) {
            loading=true
            try { regions=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/sentinel/regions"){it.jsonArray}); region=regions.firstOrNull()?.jsonObject?.text("id").orEmpty() }
            catch(c:CancellationException){throw c}
            catch(e:Exception){error=workspaceError(e)}
            finally{loading=false}
        }
    }
    val admin=memberships.firstOrNull { it.jsonObject.text("organization_id")==org }?.jsonObject?.text("role") in listOf("ORG_ADMIN","COORDINATOR")
    WorkspaceShell(tab,memberships,regions,org,region,locked,{focusRecord=null;tab=it},{focusRecord=null;region="";regions=JsonArray(emptyList());org=it},{focusRecord=null;region=it},{
        scope.launch { try { IdentityServices.firebase.signOut(); com.example.admin.features.subscriptions.SubscriptionServices.controller.identify(null);onLogout() } catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)} }
    },Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank()) { Text(error,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error);TextButton(onClick={reload++}){Text("Retry workspace access")} }
            key(org,region,tab) {
                when(tab) {
                    "Profile" -> LiveAccountScreen(onSessionEnded=onLogout)
                    "Pro" -> com.example.admin.features.subscriptions.ResQProScreen()
                    else -> if(org.isBlank() || region.isBlank()) WorkspacePage("Your workspace",if(loading)"Loading workspace..." else "Choose an approved workspace and region to continue.",Modifier.fillMaxSize()) { Button(onClick={tab="Profile"}){Text("Manage invitations and account")} }
                    else when(tab) {
                        "Team Pro" -> TeamSponsorship(api,org)
                        "Sentinel" -> SentinelWorkspace(api,org,region)
                        "Assistance" -> SafetyWorkspace(api,org,region,{locked=it},focusRecord)
                        "Weather analysis","Resource planning" -> PlanningWorkspace(api,org,region,tab=="Weather analysis",{destination,assessment->planningAssessment=assessment;tab=destination},{locked=it},Modifier.fillMaxSize(),planningAssessment)
                        "Overview" -> if(admin)CoordinationOverview(api,org,region,{destination,id->focusRecord=id;tab=destination},Modifier.fillMaxSize()) else WorkspaceInsights(api,org,region,false,{tab=it},Modifier.fillMaxSize())
                        "Insights" -> WorkspaceInsights(api,org,region,false,{tab=it},Modifier.fillMaxSize())
                        "People" -> WorkspacePeople(api,org,region,Modifier.fillMaxSize())
                        "Security" -> WorkspaceSecurity(api,org,region,admin,{locked=it},Modifier.fillMaxSize())
                        "Settings" -> WorkspaceSettings(api,org,Modifier.fillMaxSize())
                        else -> RecordWorkspace(api,org,region,tab,admin,userId,{locked=it},Modifier.fillMaxSize(),focusRecord)
                    }
                }
            }
        }
    }
}

internal fun workspaceError(e:Exception):String = if(e is ApiFailure) when(e.code) {
    "VERSION_CONFLICT","STALE_VERSION" -> "This record changed. Refresh and review the latest version before trying again."
    "DISPATCH_PAUSED" -> "Regional dispatch is paused. A coordinator can resume it in Security."
    "MFA_REQUIRED","RECENT_AUTH_REQUIRED" -> "Sign in again with your institution's required MFA to continue."
    "CAPABILITY_REQUIRED","MEMBERSHIP_REQUIRED","SCOPE_REQUIRED" -> "Your current membership does not allow this action in this workspace."
    else -> "Request not completed: ${e.code}. Refresh and try again."
} else "Could not connect. Your change has not been confirmed."

@Composable private fun RecordWorkspace(api:LiveApiTransport,org:String,region:String,tab:String,admin:Boolean,userId:String,onLock:(Boolean)->Unit,modifier:Modifier=Modifier,initialRecord:String?=null) {
    val path="/api/organizations/$org/regions/$region/operations"
    val kind=workspaceDestinations.first { it.title==tab }.kind!!
    val scope=rememberCoroutineScope()
    var records by remember { mutableStateOf(JsonArray(emptyList())) }
    var next by remember { mutableStateOf<Int?>(null) }
    var selected by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<JsonObject?>(null) }
    var pending by remember { mutableStateOf<Pair<String,JsonObject>?>(null) }
    var reload by remember { mutableStateOf(0) }
    DisposableEffect(Unit){onDispose{onLock(false)}}
    LaunchedEffect(busy,pending){onLock(busy||pending!=null)}
    suspend fun load(append:Boolean=false) {
        loading=true;error=""
        try {
            val params=buildMap {put("kind",kind);put("after",if(append)(next?:0).toString() else "0");if(query.length>=2)put("q",query);if(status.isNotBlank())put("status",status)}
            val page=api.execute(ApiEndpoint(HttpMethod.Get,path+"/records",params){it.jsonObject})
            val incoming=page["items"]!!.jsonArray
            records=if(append)JsonArray((records+incoming).distinctBy{it.jsonObject.text("id")})else incoming
            next=page["next_cursor"]?.jsonPrimitive?.intOrNull
        } catch(c:CancellationException){throw c}
        catch(e:Exception){if(e is ApiFailure&&e.status in listOf(401,403)){records=JsonArray(emptyList());selected=null};error=workspaceError(e)}
        finally{loading=false}
    }
    LaunchedEffect(search){delay(350);query=search.trim().take(100)}
    LaunchedEffect(reload,query,status){load()}
    LaunchedEffect(initialRecord) {
        if(initialRecord!=null)try {
            val record=api.execute(ApiEndpoint(HttpMethod.Get,"$path/records/$initialRecord"){it.jsonObject})
            if(record.text("kind")==kind)selected=record
        }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}
    }
    fun send(command:JsonObject) {
        if(busy)return
        val request=pending?:Pair(Uuid.random().toString(),command)
        pending=request;busy=true;error="";notice=""
        scope.launch {
            try { api.execute(ApiEndpoint(HttpMethod.Post,path+"/commands",headers=mapOf("Idempotency-Key" to request.first)){it},request.second);pending=null;selected=null;create=false;notice="Change saved to your workspace.";load() }
            catch(c:CancellationException){throw c}
            catch(e:Exception){error=workspaceError(e);if(e is ApiFailure&&e.status in 400..499&&e.status!=429){pending=null;if(e.status in listOf(401,403)){records=JsonArray(emptyList());selected=null}}}
            finally{busy=false}
        }
    }
    fun review(command:JsonObject) { if(command.text("action") in listOf("TRANSITION_MISSION","REVOKE_CREDENTIAL","MODERATE_MESSAGE","WITHDRAW_BULLETIN","PUBLISH_BULLETIN")) confirm=command else send(command) }
    BoxWithConstraints(modifier) {
        val wide=maxWidth>=1000.dp
        val compact=maxWidth<600.dp
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)){Text(pageLabel(tab),style=MaterialTheme.typography.headlineLarge)}
                if((admin&&tab in listOf("Missions","Resources","Training","Bulletins"))||tab=="Support")Button(onClick={create=true;selected=null},enabled=!busy&&pending==null){Text(when(tab){"Missions"->"Plan mission";"Resources"->"Add resource";"Training"->"Create course";"Bulletins"->"Write bulletin";else->"New case"})}
            }
            if(loading||busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
            if(notice.isNotBlank())Text(notice,color=ColorTeal)
            if(pending!=null) {
                Text("The result is unconfirmed. Retry the same request safely, or stop retrying and refresh before making another change.")
                Row { TextButton(onClick={send(pending!!.second)},enabled=!busy){Text("Retry pending change")};TextButton(onClick={pending=null;reload++},enabled=!busy){Text("Stop retrying & refresh")} }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(search,{search=it.take(100)},label={Text("Search ${pageLabel(tab).lowercase()}")},singleLine=true,modifier=if(compact)Modifier.fillMaxWidth()else Modifier.weight(1f))
                WorkspacePicker("Status",listOf("" to "All statuses")+recordStatuses(kind).map{it to it.lowercase().replace('_',' ')},status,!busy,{status=it})
                TextButton(onClick={reload++},enabled=!loading&&!busy&&pending==null){Text("Refresh")}
            }
            if(search.trim().length==1)Text("Enter at least two characters to search.",style=MaterialTheme.typography.bodySmall)
            val filtered=records.map{it.jsonObject}
            val detail: @Composable ()->Unit = {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    TextButton(onClick={selected=null;create=false},enabled=!busy&&pending==null){Text(if(tab=="Missions")"Back to missions" else "Close panel")}
                    if(create) when(tab){"Missions"->MissionCreation(busy||pending!=null,::review);"Resources"->ResourceCreation(!busy&&pending==null,::review);"Training"->CourseCreation(api,path,!busy&&pending==null,::review);"Support"->SupportCreation(!busy&&pending==null,::review);"Bulletins"->BulletinCreation(!busy&&pending==null,::review)}
                    selected?.let { record ->
                        val latest=records.firstOrNull{it.jsonObject.text("id")==record.text("id")}?.jsonObject
                        if(latest!=null&&latest["version"]!=record["version"]) {Text("A newer version is available. Reopen this record before saving.");TextButton(onClick={selected=latest}){Text("Review latest version")}}
                        key(record.text("id"),record.text("version")){RecordReview(api,path,record,records,admin,userId,busy||pending!=null||(latest!=null&&latest["version"]!=record["version"]),::review)}
                    }
                }
            }
            if(tab=="Missions"&&(selected!=null||create))Surface(Modifier.weight(1f).fillMaxWidth(),shape=MaterialTheme.shapes.medium){detail()}
            else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if(tab=="Deployment map") MissionLocations(filtered,Modifier.fillMaxWidth().height(300.dp))
                    RecordTable(filtered,selected?.text("id"),wide,!busy&&pending==null,{selected=it;create=false},Modifier.weight(1f).fillMaxWidth())
                    if(filtered.isEmpty()&&!loading)Text(if(error.isNotBlank())"Records could not be loaded." else if(query.isNotBlank()||status.isNotBlank())"No results match your filters." else "No ${pageLabel(tab).lowercase()} in this region yet.",Modifier.padding(16.dp))
                    Row {Text("${filtered.size} loaded records",Modifier.weight(1f).padding(12.dp));if(next!=null)TextButton(onClick={scope.launch{load(true)}},enabled=!loading&&!busy){Text("Load more")}}
                }
                if(wide&&(selected!=null||create))Surface(Modifier.width(410.dp).fillMaxHeight(),shape=MaterialTheme.shapes.medium){detail()}
            }
            if(tab!="Missions"&&!wide&&(selected!=null||create)) androidx.compose.ui.window.Dialog(onDismissRequest={if(!busy&&pending==null){selected=null;create=false}}) { Surface(Modifier.fillMaxWidth().heightIn(max=720.dp),shape=MaterialTheme.shapes.medium){detail()} }
        }
    }
    confirm?.let { command -> AlertDialog(onDismissRequest={confirm=null},title={Text("Confirm change")},text={Text("${command.text("action").lowercase().replace('_',' ')}${command.text("status").takeIf{it.isNotBlank()}?.let{": ${it.lowercase()}"}.orEmpty()}. This changes the shared record for your team.")},confirmButton={Button(onClick={confirm=null;send(command)}){Text("Confirm")}},dismissButton={TextButton(onClick={confirm=null}){Text("Go back")}}) }
}
