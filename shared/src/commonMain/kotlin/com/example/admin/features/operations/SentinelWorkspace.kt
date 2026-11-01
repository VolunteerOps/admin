package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.time.Clock

@Composable internal fun SentinelWorkspace(api:LiveApiTransport,org:String,region:String) {
    var forecast by remember(org,region){mutableStateOf<JsonObject?>(null)}
    var hazards by remember(org,region){mutableStateOf(JsonArray(emptyList()))}
    var missions by remember(org,region){mutableStateOf(JsonArray(emptyList()))}
    var errors by remember {mutableStateOf(listOf<String>())}
    var refreshedAt by remember {mutableStateOf(0L)}
    var foreground by remember {mutableStateOf(true)}
    var reload by remember {mutableStateOf(0)}
    var loading by remember {mutableStateOf(true)}
    var variable by remember {mutableStateOf("total_precipitation_1hr")}
    var hour by remember {mutableStateOf(0)}
    var statistic by remember {mutableStateOf("p50")}
    var showForecast by remember {mutableStateOf(true)}
    var showHazards by remember {mutableStateOf(true)}
    var showMissions by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<JsonObject?>(null)}
    var showList by remember {mutableStateOf(false)}
    var assistant by remember {mutableStateOf(false)}
    var controls by remember {mutableStateOf(false)}
    LifecycleResumeEffect(Unit) {foreground=true;onPauseOrDispose{foreground=false;forecast=null;hazards=JsonArray(emptyList());missions=JsonArray(emptyList());selected=null;refreshedAt=0}}
    LaunchedEffect(org,region,reload,foreground) {
        while(foreground&&region.isNotBlank()) {
            loading=true
            val failures=mutableListOf<String>()
            suspend fun read(url:String,params:Map<String,String> = emptyMap()):JsonElement? = try {api.execute(ApiEndpoint(HttpMethod.Get,url,params){it})}catch(c:CancellationException){throw c}catch(e:Exception){failures.add(workspaceError(e));null}
            val base="/api/organizations/$org/sentinel/regions/$region"
            forecast=read("$base/forecasts/latest") as? JsonObject
            hazards=(read("$base/hazards") as? JsonObject)?.get("observations") as? JsonArray ?: JsonArray(emptyList())
            missions=(read("/api/organizations/$org/regions/$region/operations/records",mapOf("kind" to "MISSION")) as? JsonObject)?.get("items") as? JsonArray ?: JsonArray(emptyList())
            errors=failures.distinct();refreshedAt=Clock.System.now().toEpochMilliseconds();loading=false
            delay(30000)
        }
    }
    val cells=(forecast?.get("cells") as? JsonArray).orEmpty()
    val times=cells.map{it.jsonObject.text("validTime")}.distinct().sorted()
    val index=hour.coerceIn(0,(times.size-1).coerceAtLeast(0))
    val validTime=times.getOrNull(index).orEmpty()
    val unit=when(variable){"temperature_2m"->"°C";"wind_speed_10m"->"m/s";else->"mm"}
    val points=buildJsonArray {
        if(showForecast)cells.filter{it.jsonObject.text("validTime")==validTime}.take(800).forEachIndexed { i,item ->
            val c=item.jsonObject;val value=c["values"]?.jsonObject?.get(variable)?.jsonObject?.get(statistic)?.jsonPrimitive?.doubleOrNull
            if(value!=null)add(buildJsonObject{put("id","forecast-$i");put("kind","forecast");put("longitude",c["longitude"]!!);put("latitude",c["latitude"]!!);put("label","$value $unit · ${displayTime(validTime)} · $statistic")})
        }
        if(showHazards)hazards.take(500).forEach { item -> val h=item.jsonObject
            add(buildJsonObject{put("id",h.text("id"));put("kind","hazard");put("longitude",h["longitude"]!!);put("latitude",h["latitude"]!!);put("label",(h.text("title")+" · "+displayTime(h.text("observed_at"))).take(300))})
        }
        if(showMissions)missions.take(100).forEach { item -> val m=item.jsonObject;val p=m["payload"]!!.jsonObject
            if(p.text("status") in listOf("STAGED","ACTIVE","PAUSED")&&p["latitude"]!=null&&p["longitude"]!=null)add(buildJsonObject{put("id",m.text("id"));put("kind","mission");put("longitude",p["longitude"]!!);put("latitude",p["latitude"]!!);put("label",(p.text("title")+" · "+p.text("status")).take(300))})
        }
    }
    val currentSelection=points.firstOrNull{it.jsonObject.text("id")==selected?.text("id")}?.jsonObject
    val mapData=buildJsonObject {
        put("type","resq.evidence");put("expires_at",refreshedAt+45000);put("points",points);put("scope",region)
        currentSelection?.let {put("selected_id",it.text("id"))}
    }.toString()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide=maxWidth>=900.dp
        Column(Modifier.fillMaxSize().padding(if(wide)24.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {Text("Sentinel",style=MaterialTheme.typography.headlineLarge);Text("Regional evidence & forecast radar",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                OutlinedButton(onClick={controls=true}){Text("Sources & policy")}
                Button(onClick={assistant=true}){Text("Ask Sentinel")}
            }
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(errors.isNotEmpty())Text("Some evidence could not refresh. ${errors.first()}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                WorkspacePicker("Variable",listOf("total_precipitation_1hr" to "Hourly rain","temperature_2m" to "Temperature","wind_speed_10m" to "Wind"),variable,true,{variable=it;selected=null})
                WorkspacePicker("Statistic",listOf("p50" to "Median · P50","p90" to "Upper range · P90"),statistic,true,{statistic=it;selected=null})
                FilterChip(showForecast,{showForecast=!showForecast;selected=null},label={Text("Forecast")})
                FilterChip(showHazards,{showHazards=!showHazards;selected=null},label={Text("Reported hazards")})
                FilterChip(showMissions,{showMissions=!showMissions;selected=null},label={Text("Missions")})
                if(!wide)FilterChip(showList,{showList=!showList},label={Text(if(showList)"Show map" else "Evidence list")})
            }
            val evidence: @Composable ()->Unit = {
                Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),modifier=Modifier.fillMaxSize()) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("Evidence",style=MaterialTheme.typography.titleLarge)
                        Text("${points.size} mapped observations",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        currentSelection?.let { point ->
                            Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.medium) {
                                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(point.text("kind").replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.labelMedium)
                                    Text(point.text("label"),style=MaterialTheme.typography.bodySmall)
                                    Text("${point.text("latitude")}, ${point.text("longitude")}",style=MaterialTheme.typography.bodySmall)
                                    TextButton(onClick={selected=null}){Text("Clear selection")}
                                }
                            }
                        }
                        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            if(points.isEmpty())item{Text("No current observations for these layers.",style=MaterialTheme.typography.bodySmall)}
                            items(points.take(100)){item->val point=item.jsonObject
                                Surface(onClick={selected=point},shape=MaterialTheme.shapes.small,color=if(currentSelection?.text("id")==point.text("id"))MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                    Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                        Text(point.text("kind").uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(point.text("label"),style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if(points.size>100)item{Text("First 100 observations. Narrow the layer or forecast time.",style=MaterialTheme.typography.bodySmall)}
                        }
                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                        StatusPill(forecast?.text("status")?:"UNAVAILABLE")
                        forecast?.get("run")?.let { if(it is JsonObject){Text(it.text("model"),style=MaterialTheme.typography.labelMedium);Text("Issued ${displayTime(it.text("init_time"))}",style=MaterialTheme.typography.bodySmall)} }
                        Text("Forecast indicators are unvalidated. Official authorities issue warnings.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick={reload++},enabled=!loading){Text("Refresh evidence")}
                    }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                if(wide||!showList)Surface(Modifier.weight(1f).fillMaxHeight(),shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {EvidenceMap(mapData,Modifier.fillMaxSize()){id->selected=points.firstOrNull{it.jsonObject.text("id")==id}?.jsonObject}}
                if(wide)Box(Modifier.width(320.dp).fillMaxHeight()){evidence()} else if(showList)evidence()
            }
            if(times.isNotEmpty())Surface(shape=MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Valid time",style=MaterialTheme.typography.labelMedium)
                    if(times.size>1)Slider(value=index.toFloat(),onValueChange={hour=it.toInt();selected=null},valueRange=0f..(times.size-1).toFloat(),steps=(times.size-2).coerceAtLeast(0),modifier=Modifier.weight(1f))
                    Text(displayTime(validTime),style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if(assistant)SentinelAssistant(api,org,region,{assistant=false})
    if(controls)androidx.compose.ui.window.Dialog(onDismissRequest={controls=false}) {
        Surface(Modifier.widthIn(max=760.dp).fillMaxWidth().heightIn(max=780.dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                TextButton(onClick={controls=false}){Text("Back to radar")}
                SentinelControls(api,org,region)
            }
        }
    }
}

@Composable private fun SentinelAssistant(api:LiveApiTransport,org:String,region:String,onClose:()->Unit) {
    var question by remember {mutableStateOf("")};var response by remember{mutableStateOf<JsonObject?>(null)}
    var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    androidx.compose.ui.window.Dialog(onDismissRequest=onClose) {
        Surface(Modifier.widthIn(max=720.dp).fillMaxWidth().heightIn(max=760.dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("Ask Sentinel",style=MaterialTheme.typography.headlineSmall)
                Text("Evidence-based planning support. Requires Pro access.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(question,{question=it.take(2000)},label={Text("What do you need to understand?")},minLines=3,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(enabled=!busy&&question.trim().length>=3,onClick={busy=true;error="";scope.launch{try{response=api.execute(ApiEndpoint(HttpMethod.Post,"/api/organizations/$org/sentinel/regions/$region/agent/chat"){it.jsonObject},buildJsonObject{put("question",question)})}catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}){Text(if(busy)"Reviewing evidence…" else "Ask")}
                    TextButton(onClick=onClose){Text("Close")}
                }
                if(error.isNotBlank())PortalNotice("Assistant unavailable",error)
                response?.let { result ->
                    Text(result.text("answer"))
                    (result["sources"] as? JsonArray)?.takeIf{it.isNotEmpty()}?.let { sources ->
                        HorizontalDivider();Text("Sources",style=MaterialTheme.typography.titleSmall)
                        sources.forEach{source->val s=source.jsonObject;Text("${s.text("model")} · ${displayTime(s.text("init_time"))}",style=MaterialTheme.typography.bodySmall)}
                    }
                    Text("No operational actions were executed.",style=MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
