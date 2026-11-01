package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

@Composable internal fun SentinelControls(api:LiveApiTransport,org:String,region:String) {
    val base="/api/organizations/$org/sentinel/regions/$region"
    val scope=rememberCoroutineScope()
    var current by remember{mutableStateOf<JsonObject?>(null)}
    var readiness by remember{mutableStateOf<JsonObject?>(null)}
    var busy by remember{mutableStateOf(false)}
    var notice by remember{mutableStateOf("")}
    var date by remember{mutableStateOf("")};var time by remember{mutableStateOf("00:00")}
    var from by remember{mutableStateOf("1")};var to by remember{mutableStateOf("24")}
    var job by remember{mutableStateOf<JsonObject?>(null)}
    var reason by remember{mutableStateOf("")}
    var editing by remember{mutableStateOf(false)}
    var values by remember{mutableStateOf(mapOf<String,String>())}
    var confirm by remember{mutableStateOf(false)}
    suspend fun load() {
        current=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/sentinel/regions"){it.jsonArray}).firstOrNull{it.jsonObject.text("id")==region}?.jsonObject
        val policy=current?.get("forecastPolicy") as? JsonObject
        val bounds=current?.get("forecastBounds") as? JsonObject
        values=buildMap {put("name",current?.text("name").orEmpty());listOf("heat_c","wind_mps","hourly_rain_mm","maximum_run_age_hours").forEach{put(it,policy?.text(it).orEmpty())};listOf("west","south","east","north").forEach{put(it,bounds?.text(it).orEmpty())}}
        readiness=api.execute(ApiEndpoint(HttpMethod.Get,"$base/readiness"){it.jsonObject})
    }
    fun run(action:suspend()->Unit){if(busy)return;busy=true;notice="";scope.launch{try{action()}catch(c:CancellationException){throw c}catch(e:Exception){notice=workspaceError(e)}finally{busy=false}}}
    LaunchedEffect(org,region){run{load()}}
    LaunchedEffect(job?.text("job_id")) {
        val id=job?.text("job_id").orEmpty()
        while(id.isNotBlank()&&job?.text("state") in listOf("PENDING","RUNNING")) {
            delay(3000)
            try {val result=api.execute(ApiEndpoint(HttpMethod.Get,"$base/jobs/$id"){it.jsonObject});job=JsonObject(result+ ("job_id" to JsonPrimitive(id)))}catch(c:CancellationException){throw c}catch(e:Exception){notice=workspaceError(e);break}
        }
    }
    Text("Sources & regional policy",style=MaterialTheme.typography.headlineSmall)
    Text("WeatherNext 3 forecasts and reported USGS earthquakes. Thresholds flag conditions for review; they do not certify disaster predictions.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(notice.isNotBlank())Text(notice,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    readiness?.let { state ->
        val forecast=state["forecast"]?.jsonObject
        val agents=state["agents"]?.jsonObject
        val source=state["earthquake_source"]?.jsonObject
        Text("Source status",style=MaterialTheme.typography.titleMedium)
        Text(if(forecast?.text("access_enabled")=="true")"WeatherNext access enabled" else "WeatherNext access is disabled",style=MaterialTheme.typography.bodyMedium)
        Text("Query ceiling: ${forecast?.text("maximum_bytes_per_query").orEmpty()} bytes",style=MaterialTheme.typography.bodySmall)
        Text(if(agents?.text("configuration_present")=="true")"Assistant configuration present" else "Assistant configuration missing",style=MaterialTheme.typography.bodySmall)
        Text(source?.text("last_observation_received_at").orEmpty().takeUnless{it.isBlank()||it=="null"}?.let{"Latest USGS observation received ${displayTime(it)}"}?:"No USGS observation has been received",style=MaterialTheme.typography.bodySmall)
        (forecast?.get("counts") as? JsonArray).orEmpty().forEach { count->val c=count.jsonObject;Text("${c.text("count")} forecast jobs · ${c.text("state").lowercase()}",style=MaterialTheme.typography.bodySmall)}
        (forecast?.get("jobs") as? JsonArray).orEmpty().filter{it.jsonObject.text("state") in listOf("DEAD","EXPIRED","PENDING","RUNNING")}.forEach { item->val entry=item.jsonObject
            TextButton(enabled=!busy,onClick={job=JsonObject(entry+("job_id" to entry["id"]!!))}){Text("${entry.text("state").lowercase()} · ${displayTime(entry.text("createdAt"))}")}
        }
        Text("Coverage still needed",style=MaterialTheme.typography.titleSmall)
        (state["coverage_gaps"] as? JsonArray).orEmpty().forEach{Text(it.jsonPrimitive.content,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        HorizontalDivider()
    }
    Text("Request a forecast run",style=MaterialTheme.typography.titleMedium)
    Text("Choose an issued UTC cycle from the last 48 hours. Query windows are limited to 48 forecast hours.",style=MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(date,{date=it},label={Text("Date · YYYY-MM-DD")},modifier=Modifier.weight(1f),singleLine=true)
        OutlinedTextField(time,{time=it},label={Text("Cycle · HH:00 UTC")},modifier=Modifier.weight(1f),singleLine=true)
    }
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(from,{from=it},label={Text("First forecast hour")},modifier=Modifier.weight(1f),singleLine=true)
        OutlinedTextField(to,{to=it},label={Text("Last forecast hour")},modifier=Modifier.weight(1f),singleLine=true)
    }
    val init=runCatching{Instant.parse("${date}T${time}:00Z")}.getOrNull()
    val valid=init!=null&&(from.toIntOrNull()?:0)>=1&&(to.toIntOrNull()?:0) in (from.toIntOrNull()?:1)..((from.toIntOrNull()?:1)+47)
    Button(enabled=!busy&&valid,onClick={run{job=api.execute(ApiEndpoint(HttpMethod.Post,"$base/forecasts/refresh"){it.jsonObject},buildJsonObject{put("init_time",init.toString());put("from_hour",from.toInt());put("to_hour",to.toInt())})}}){Text("Request forecast")}
    job?.let { value ->
        StatusPill(value.text("state"))
        if(value.text("state") in listOf("DEAD","EXPIRED")) {
            OutlinedTextField(reason,{reason=it.take(500)},label={Text("Reason for retry")},modifier=Modifier.fillMaxWidth())
            Button(enabled=!busy&&reason.trim().length>=10,onClick={run{job=api.execute(ApiEndpoint(HttpMethod.Post,"$base/jobs/${value.text("job_id")}/retry"){it.jsonObject},buildJsonObject{put("reason",reason)})}}){Text("Retry failed job")}
        }
        if(value.text("state")=="COMPLETED")Text("Forecast run completed. Return to the radar and refresh evidence.",style=MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider()
    Row(Modifier.fillMaxWidth()) {Text("Region policy",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);TextButton(onClick={editing=!editing},enabled=current!=null&&!busy){Text(if(editing)"Cancel edit" else "Edit policy")}}
    current?.let { regionData ->
        val policy=regionData["forecastPolicy"] as? JsonObject
        Text("${regionData.text("name")} · revision ${policy?.text("version")?:"0"}",style=MaterialTheme.typography.bodySmall)
        if(!editing) {
            Text("Heat ${policy?.text("heat_c").orEmpty()} °C · Wind ${policy?.text("wind_mps").orEmpty()} m/s · Rain ${policy?.text("hourly_rain_mm").orEmpty()} mm/h",style=MaterialTheme.typography.bodySmall)
        } else {
            val fields=listOf("name" to "Region name","west" to "West longitude","south" to "South latitude","east" to "East longitude","north" to "North latitude","heat_c" to "Heat threshold · °C","wind_mps" to "Wind threshold · m/s","hourly_rain_mm" to "Rain threshold · mm/h","maximum_run_age_hours" to "Maximum run age · hours")
            fields.forEach{(key,label)->OutlinedTextField(values[key].orEmpty(),{values=values+(key to it)},label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true)}
            OutlinedTextField(reason,{reason=it.take(500)},label={Text("Reason for policy change")},modifier=Modifier.fillMaxWidth())
            val numbers=fields.drop(1).all{values[it.first]?.toDoubleOrNull()?.isFinite()==true}
            Button(enabled=!busy&&numbers&&values["maximum_run_age_hours"]?.toIntOrNull()!=null&&reason.trim().length>=10,onClick={confirm=true}){Text("Review policy change")}
        }
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Update regional thresholds?")},text={Text("New assessments will use revision ${((current?.get("forecastPolicy") as? JsonObject)?.get("version")?.jsonPrimitive?.intOrNull?:0)+1}. This does not activate automatic warnings or deployment.")},confirmButton={Button(onClick={confirm=false;run{
        val policy=current?.get("forecastPolicy") as? JsonObject
        api.execute(ApiEndpoint(HttpMethod.Put,base){it},buildJsonObject{put("name",values["name"]!!);put("reason",reason);put("bounds",buildJsonObject{listOf("west","south","east","north").forEach{put(it,values[it]!!.toDouble())}});put("policy",buildJsonObject{put("version",(policy?.get("version")?.jsonPrimitive?.intOrNull?:0)+1);listOf("heat_c","wind_mps","hourly_rain_mm").forEach{put(it,values[it]!!.toDouble())};put("maximum_run_age_hours",values["maximum_run_age_hours"]!!.toInt())})});editing=false;load()
    }}){Text("Save policy")}},dismissButton={TextButton(onClick={confirm=false}){Text("Go back")}})
}
