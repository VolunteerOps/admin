package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@Composable internal fun WorkspaceInsights(api:LiveApiTransport,org:String,region:String,overview:Boolean,navigate:(String)->Unit,modifier:Modifier=Modifier) {
    val now=remember{Clock.System.now()}
    var from by remember{mutableStateOf((now-7.days).toString())}
    var to by remember{mutableStateOf(now.toString())}
    var reload by remember{mutableStateOf(0)}
    var data by remember{mutableStateOf<JsonObject?>(null)}
    var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(true)}
    var exported by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
    val valid=runCatching{ val a=Instant.parse(from);val b=Instant.parse(to);b>a&&b-a<=31.days }.getOrDefault(false)
    LaunchedEffect(reload) {
        busy=true;error="";exported=""
        try{data=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/regions/$region/operations/metrics",mapOf("from" to from,"to" to to)){it.jsonObject})}
        catch(c:CancellationException){throw c}catch(e:Exception){data=null;error=workspaceError(e)}finally{busy=false}
    }
    WorkspacePage(if(overview)"Your response, in focus." else "Operational insights","Mission outcomes and contributions for the selected region.",modifier) {
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(from,{from=it},label={Text("From (UTC)")},singleLine=true)
            OutlinedTextField(to,{to=it},label={Text("To (UTC)")},singleLine=true)
            Button(onClick={reload++},enabled=!busy&&valid){Text("Update summary")}
        }
        if(!valid)Text("Enter ISO timestamps with a positive interval of at most 31 days.",color=MaterialTheme.colorScheme.error)
        data?.let { result ->
            Text("${result.text("from")} — ${result.text("to")}",style=MaterialTheme.typography.bodySmall)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns=if(maxWidth>=800.dp)3 else if(maxWidth>=320.dp)2 else 1
            FlowRow(maxItemsInEachRow=columns,horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                listOf("active_missions" to "Active missions","completed_missions" to "Completed missions","approved_hours" to "Approved hours","pending_hour_reviews" to "Hours awaiting review","report_count" to "Field reports","mission_count" to "Total missions").forEach { (key,label) ->
                    Surface(color=Color.White,shape=MaterialTheme.shapes.medium,modifier=Modifier.weight(1f)) {
                        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(result.text(key),style=MaterialTheme.typography.headlineLarge,color=WorkspaceNavy);Text(label)}
                    }
                }
            }
            }
            Text("Counts use records created in this interval and their current status. They are not lifetime totals or a historical state snapshot.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(enabled=!busy,onClick={busy=true;error="";scope.launch{try{
                exported=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/regions/$region/operations/metrics/export",mapOf("from" to result.text("from"),"to" to result.text("to")),rawText=true){it.jsonPrimitive.content})
            }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}){Text("Export this summary as CSV")}
            if(exported.isNotBlank()) {
                OutlinedTextField(exported,{},readOnly=true,label={Text("CSV export")},modifier=Modifier.fillMaxWidth().heightIn(max=240.dp))
                TextButton(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString(exported))}){Text("Copy CSV")}
            }
        }
        if(overview) {
            Text("Keep the response moving",style=MaterialTheme.typography.headlineSmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                listOf("Missions","Applications","Reports","Hours","Resources").forEach { page -> OutlinedButton(onClick={navigate(page)}){Text("Review ${page.lowercase()}")} }
            }
        }
    }
}
