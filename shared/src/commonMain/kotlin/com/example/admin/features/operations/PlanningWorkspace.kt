@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

private data class PlanningRequest(val path:String,val key:String,val body:JsonObject)

@Composable internal fun PlanningWorkspace(api:LiveApiTransport,org:String,region:String,weather:Boolean,navigate:(String,String?)->Unit,onLock:(Boolean)->Unit,modifier:Modifier=Modifier,initialAssessmentId:String?=null) {
    val base="/api/organizations/$org/sentinel/regions/$region/planning/${if(weather)"weather" else "resources"}"
    val scope=rememberCoroutineScope()
    var records by remember {mutableStateOf(JsonArray(emptyList()))}
    var selected by remember {mutableStateOf<JsonObject?>(null)}
    var creating by remember {mutableStateOf(!weather && initialAssessmentId!=null)}
    var assessmentId by remember {mutableStateOf(initialAssessmentId)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var pending by remember {mutableStateOf<PlanningRequest?>(null)}
    var next by remember{mutableStateOf<Int?>(null)}
    var reason by remember{mutableStateOf("")}
    var routeReviewed by remember{mutableStateOf(false)}
    var conditionsReviewed by remember{mutableStateOf(false)}
    var confirmation by remember{mutableStateOf(false)}
    var editing by remember{mutableStateOf(false)}
    var composerMission by remember{mutableStateOf("")};var composerQuestion by remember{mutableStateOf("")}
    var members by remember{mutableStateOf(JsonArray(emptyList()))}
    fun memberName(id:String)=members.firstOrNull{it.jsonObject.text("userId")==id}?.jsonObject?.get("user")?.jsonObject?.text("fullName").orEmpty().ifBlank{"Member $id"}
    val locked=busy||pending!=null
    DisposableEffect(Unit){onDispose{onLock(false)}}
    LaunchedEffect(locked){onLock(locked)}
    suspend fun load(append:Boolean=false) {
        val page=api.execute(ApiEndpoint(HttpMethod.Get,base,if(append)mapOf("after" to (next?:0).toString())else emptyMap()){it.jsonObject})
        records=if(append)JsonArray((records+page["items"]!!.jsonArray).distinctBy{it.jsonObject.text("id")})else page["items"]!!.jsonArray
        next=page["next_cursor"]?.jsonPrimitive?.intOrNull
    }
    fun refresh(append:Boolean=false){if(busy)return;busy=true;error="";scope.launch{try{load(append)}catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}
    LaunchedEffect(base){refresh()}
    LaunchedEffect(org,weather){if(!weather)try{members=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/memberships"){it.jsonArray})}catch(c:CancellationException){throw c}catch(_:Exception){}}
    fun send(path:String,body:JsonObject) {
        if(busy)return
        val request=pending?:PlanningRequest(path,Uuid.random().toString(),body)
        pending=request;busy=true;error=""
        scope.launch {
            try {
                selected=api.execute(ApiEndpoint(HttpMethod.Post,request.path,headers=mapOf("Idempotency-Key" to request.key)){it.jsonObject},request.body)
                pending=null;creating=false;editing=false;confirmation=false;reason="";routeReviewed=false;conditionsReviewed=false;load()
            }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e);if(e is ApiFailure&&e.status in 400..499&&e.status!=429)pending=null}
            finally{busy=false}
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)){Text(if(weather)"Weather analysis" else "Resource planning",style=MaterialTheme.typography.headlineLarge);Text(if(weather)"Forecast evidence & preparedness" else "Staffing, equipment & allocation decisions",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Button(enabled=!locked,onClick={composerMission="";composerQuestion="";assessmentId=null;creating=true;selected=null;editing=false}){Text(if(weather)"New assessment" else "New proposal")}
        }
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotBlank())PortalNotice("Request not completed",error)
        pending?.let { request ->
            Text("The result has not been confirmed. Retry with the same request key or refresh its history.",style=MaterialTheme.typography.bodySmall)
            Row {TextButton(enabled=!busy,onClick={send(request.path,request.body)}){Text("Retry request")};TextButton(enabled=!busy,onClick={pending=null;refresh()}){Text("Stop retrying & refresh")}}
        }
        if(creating)Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!weather&&assessmentId!=null) {
                    Text("Weather assessment linked",style=MaterialTheme.typography.titleSmall)
                    Text("Its source, regional policy and age will be checked before planning and approval.",style=MaterialTheme.typography.bodySmall)
                    TextButton(enabled=!locked,onClick={assessmentId=null}){Text("Remove weather context")}
                }
                PlanningComposer(api,org,region,weather,locked,{creating=false},{input->
                    send(base,if(!weather&&assessmentId!=null)JsonObject(input+("source_assessment_id" to JsonPrimitive(assessmentId)))else input)
                },Modifier.fillMaxWidth(),composerMission,composerQuestion)
            }
        }
        selected?.let { record ->
            val p=record["payload"]!!.jsonObject
            Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth()){StatusPill(p.text("status"));Spacer(Modifier.weight(1f));TextButton(enabled=!locked,onClick={selected=null;confirmation=false}){Text("Close review")}}
                    Text(p.text("title").ifBlank{if(weather)"Weather assessment" else "Resource proposal"},style=MaterialTheme.typography.headlineSmall)
                    Text(p.text("model"),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    HarnessView(p["harness"] as? JsonObject)
                    if(p.text("status")=="RUNNING")Text("Analysis is processing. Refresh history to check its result.")
                    if(p.text("status")=="FAILED")Text(p.text("message").ifBlank{"Analysis did not complete. Check provider configuration before submitting a new request."})
                    if(weather) {
                        val narrative=p["narrative"] as? JsonObject
                        val source=p["source"] as? JsonObject
                        if(narrative!=null){Text(narrative.text("summary"));PlanningNotes("Preparation",narrative["preparedness"]);PlanningNotes("Assumptions",narrative["assumptions"]);PlanningNotes("Missing evidence",narrative["missing_inputs"])}
                        if(source!=null){HorizontalDivider();Text("Forecast source",style=MaterialTheme.typography.titleSmall);Text("${source.text("model")} · ${displayTime(source.text("init_time"))}",style=MaterialTheme.typography.bodySmall);Text("Query ${source.text("source_id")}",style=MaterialTheme.typography.bodySmall)}
                        val station=p["station_detail"] as? JsonObject
                        if(station!=null) {
                            Text("Station temperature · 0.05°",style=MaterialTheme.typography.titleMedium)
                            Text("Query ${station.text("source_id")} · ${displayTime(station.text("init_time"))}",style=MaterialTheme.typography.bodySmall)
                            Text(station.text("limitation"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            (station["cells"] as? JsonArray).orEmpty().take(8).forEach { cell -> val point=cell.jsonObject
                                val values=point["values"]?.jsonObject
                                val temp=(values?.get("station_head_temperature_2m") as? JsonObject)?.text("p50")
                                val dew=(values?.get("station_head_dewpoint_temperature_2m") as? JsonObject)?.text("p50")
                                Text("${point.text("latitude")}, ${point.text("longitude")} · ${displayTime(point.text("valid_time"))} · ${temp ?: "—"} °C · Dew point ${dew ?: "—"} °C",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        val evidence=p["evidence"] as? JsonObject
                        if(evidence!=null) {
                            Text("Calculated indicators",style=MaterialTheme.typography.titleMedium)
                            Text(evidence.text("method"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            (evidence["thresholds"] as? JsonObject)?.let { limits ->
                                Text("Regional thresholds · Heat ≥ ${limits.text("heat_c")} °C · Wind ≥ ${limits.text("wind_mps")} m/s · Rain ≥ ${limits.text("hourly_rain_mm")} mm/h",style=MaterialTheme.typography.bodySmall)
                            }
                            (evidence["points"] as? JsonArray).orEmpty().take(12).forEach { item->val point=item.jsonObject
                                Text("${point.text("latitude")}, ${point.text("longitude")} · Rain ${point.text("expected_rainfall_mm").takeUnless{it=="null"}.orEmpty().ifBlank{"incomplete"}} mm · Heavy rain ${point.text("hours_with_mean_rain_at_or_above_threshold")} h · Heat ${point.text("longest_median_heat_exceedance_hours")} h · Wind ${point.text("longest_median_wind_exceedance_hours")} h",style=MaterialTheme.typography.bodySmall)
                            }
                            Text("Weather indicators do not establish local flood impacts.",style=MaterialTheme.typography.labelMedium)
                            OutlinedButton(enabled=!locked&&(evidence["points"] as? JsonArray)?.isNotEmpty()==true,onClick={navigate("Resource planning",record.text("id"))}){Text("Review staffing & resources")}
                        }
                    } else {
                        val plan=p["recommendation"] as? JsonObject
                        if(plan!=null) {
                            if(p.text("edited_by").isNotBlank())Text("Revised by ${memberName(p.text("edited_by"))} · ${p.text("edit_reason")}",style=MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(p.text("status")=="DRAFT")OutlinedButton(enabled=!locked,onClick={editing=!editing}){Text(if(editing)"Close revision" else "Edit allocations")}
                                TextButton(enabled=!locked,onClick={composerMission=p.text("mission_id");composerQuestion=(p["request"] as? JsonObject)?.text("question").orEmpty();assessmentId=(p["weather_evidence"] as? JsonObject)?.text("assessment_id");selected=null;creating=true;editing=false}){Text("Recompute proposal")}
                            }
                            if(editing) ResourceProposalEditor(
                                plan = plan,
                                stock = (p["snapshot"]?.jsonObject?.get("resources") as? JsonArray) ?: JsonArray(emptyList()),
                                busy = locked,
                                name = ::memberName,
                                onCancel = { editing = false },
                                onSave = { revised, why ->
                                    send("$base/${record.text("id")}/edit", buildJsonObject {
                                        put("expected_version", record["version"]!!)
                                        put("reason", why)
                                        put("recommendation", revised)
                                    })
                                },
                            )
                            if(!editing) {
                            (p["weather_evidence"] as? JsonObject)?.let { context ->
                                Text("Weather basis",style=MaterialTheme.typography.titleMedium)
                                Text(context.text("summary"),style=MaterialTheme.typography.bodyMedium)
                                Text("Forecast ${displayTime((context["source"] as? JsonObject)?.text("init_time").orEmpty())} · Policy ${context.text("policy_version")}",style=MaterialTheme.typography.bodySmall)
                                (context["indicators"] as? JsonObject)?.let { peaks ->
                                    Text("Peak rain ${peaks.text("maximum_expected_rainfall_mm")} mm · Heavy rain ${peaks.text("most_hours_with_mean_rain_at_or_above_threshold")} h · Heat ${peaks.text("longest_median_heat_exceedance_hours")} h · Wind ${peaks.text("longest_median_wind_exceedance_hours")} h",style=MaterialTheme.typography.bodySmall)
                                }
                                PlanningNotes("Missing weather evidence",context["missing_inputs"])
                            }
                            Text(plan.text("summary"))
                            (plan["allocations"] as? JsonArray).orEmpty().forEach { item ->val allocation=item.jsonObject
                                Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Text(memberName(allocation.text("user_id")),style=MaterialTheme.typography.titleSmall)
                                        Text(allocation.text("reason"),style=MaterialTheme.typography.bodySmall)
                                        (allocation["supplies"] as? JsonArray).orEmpty().forEach { supply -> val stock=supply.jsonObject
                                            val inventory=(p["snapshot"]?.jsonObject?.get("resources") as? JsonArray).orEmpty().firstOrNull{it.jsonObject.text("id")==stock.text("resource_id")}?.jsonObject
                                            Text("${stock.text("quantity")} ${inventory?.text("unit").orEmpty()} ${inventory?.text("title").orEmpty()} · ${inventory?.text("location").orEmpty()}",style=MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            PlanningNotes("Shortages",plan["shortages"]);PlanningNotes("Decisions to review",plan["decisions"])
                            PlanningNotes("Planning limits",p["snapshot"]?.jsonObject?.get("limitations"))
                            if(p.text("status")=="DRAFT"&&!editing) {
                                Text("Valid until ${displayTime(p.text("expires_at"))}",style=MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(routeReviewed,{routeReviewed=it;confirmation=false},enabled=!locked,modifier=Modifier.semantics { contentDescription = "Access and rendezvous reviewed" });Text("I have reviewed access and the rendezvous.",style=MaterialTheme.typography.bodySmall)}
                                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(conditionsReviewed,{conditionsReviewed=it;confirmation=false},enabled=!locked,modifier=Modifier.semantics { contentDescription = "Hazards and readiness reviewed" });Text("I have reviewed current hazards and team readiness.",style=MaterialTheme.typography.bodySmall)}
                                OutlinedTextField(reason,{reason=it.take(1500);confirmation=false},label={Text("Decision reason")},enabled=!locked,modifier=Modifier.fillMaxWidth().semantics { contentDescription = "Decision reason" })
                                if(confirmation)Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                        Text("Approve these assignments?",style=MaterialTheme.typography.titleMedium)
                                        Text("Capacity, qualifications, commitments and stock will be checked again. All assignments commit together. Recent MFA is required.",style=MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                            Button(enabled=!locked&&routeReviewed&&conditionsReviewed&&reason.trim().length>=10,onClick={confirmation=false;send("$base/${record.text("id")}/approve",buildJsonObject{put("expected_version",record["version"]!!);put("reason",reason);put("route_reviewed",true);put("conditions_reviewed",true)})}){Text("Approve allocations")}
                                            TextButton(enabled=!locked,onClick={confirmation=false}){Text("Go back")}
                                        }
                                    }
                                }else {
                                    Button(enabled=!locked&&routeReviewed&&conditionsReviewed&&reason.trim().length>=10&&(plan["allocations"] as? JsonArray)?.isNotEmpty()==true,onClick={confirmation=true}){Text("Review allocation approval")}
                                    TextButton(enabled=!locked&&reason.trim().length>=10,onClick={send("$base/${record.text("id")}/reject",buildJsonObject{put("expected_version",record["version"]!!);put("reason",reason)})}){Text("Reject proposal")}
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(weather)"Assessment history" else "Proposal history",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);TextButton(onClick={refresh()},enabled=!locked){Text("Refresh history")}}
        if(records.isEmpty()&&!busy&&error.isBlank())Text(if(weather)"No assessments in this region. Start with an issued forecast cycle." else "No resource proposals. Stage a mission and collect volunteer applications first.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        records.forEach { item ->val record=item.jsonObject;val p=record["payload"]!!.jsonObject
            Surface(onClick={selected=record;creating=false;editing=false;confirmation=false;routeReviewed=false;conditionsReviewed=false;reason=""},enabled=!locked,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.text("title").ifBlank{if(weather)"Weather assessment" else "Resource proposal"},style=MaterialTheme.typography.titleSmall);Text(displayTime(record.text("createdAt")),style=MaterialTheme.typography.bodySmall)};StatusPill(p.text("status"))}
            }
        }
        if(next!=null)TextButton(onClick={refresh(true)},enabled=!locked){Text("Load more history")}
    }
}

@Composable internal fun HarnessView(harness:JsonObject?) {
    if(harness==null)return
    val plan=(harness["plan"] as? JsonArray).orEmpty()
    val notes=(harness["notes"] as? JsonArray).orEmpty()
    val steps=(harness["steps"] as? JsonArray).orEmpty()
    if(plan.isEmpty()&&notes.isEmpty()&&steps.isEmpty())return
    if(plan.isNotEmpty()){Text("Agent plan",style=MaterialTheme.typography.titleSmall);plan.forEach{val step=it.jsonObject
        Text((when(step.text("status")){"done"->"✓ ";"in_progress"->"▶ ";else->"○ "})+step.text("title"),style=MaterialTheme.typography.bodySmall)}}
    if(notes.isNotEmpty()){Text("Working notes",style=MaterialTheme.typography.titleSmall);notes.forEach{Text("• ${it.jsonPrimitive.contentOrNull.orEmpty()}",style=MaterialTheme.typography.bodySmall)}}
    if(steps.isNotEmpty()){Text("Tool steps · ${harness.text("rounds")} rounds",style=MaterialTheme.typography.titleSmall);steps.forEach{val step=it.jsonObject
        Text("• ${step.text("tool")}${if(step.text("ok")=="false")" — failed: ${step.text("error")}" else ""} · ${step.text("duration_ms")} ms",style=MaterialTheme.typography.bodySmall)}}
}

@Composable private fun PlanningNotes(title:String,value:JsonElement?) {
    val lines=(value as? JsonArray).orEmpty()
    if(lines.isNotEmpty()){Text(title,style=MaterialTheme.typography.titleSmall);lines.forEach{Text("• ${it.jsonPrimitive.content}",style=MaterialTheme.typography.bodySmall)}}
}

@Composable private fun PlanningComposer(api:LiveApiTransport,org:String,region:String,weather:Boolean,busy:Boolean,onClose:()->Unit,send:(JsonObject)->Unit,modifier:Modifier=Modifier,initialMission:String="",initialQuestion:String="") {
    var question by rememberSaveable{mutableStateOf(initialQuestion)};var mission by remember{mutableStateOf(if(initialMission.isBlank())emptySet<String>() else setOf(initialMission))}
    // Server-synced draft: the same key the web portal uses, so work started
    // on one device can be resumed in this app.
    val draftKey="planning:"+(if(weather)"weather" else "resources")+":" + org + ":" + region
    val draftScope=rememberCoroutineScope()
    LaunchedEffect(draftKey){
        runCatching{
            val saved=api.execute(ApiEndpoint(HttpMethod.Get,"/api/me/drafts/$draftKey"){it.jsonObject})
            val savedQuestion=((saved["payload"] as? JsonObject)?.get("values") as? JsonObject)?.text("question").orEmpty()
            if(savedQuestion.isNotBlank()&&question.isBlank())question=savedQuestion
        }
    }
    LaunchedEffect(question){
        if(question.isBlank())return@LaunchedEffect
        delay(900)
        runCatching{api.execute(ApiEndpoint(HttpMethod.Put,"/api/me/drafts/$draftKey"){it.jsonObject},buildJsonObject{put("payload",buildJsonObject{put("values",buildJsonObject{put("question",question)});put("saved_at","")});put("base_version",0)})}
    }
    var mode by remember{mutableStateOf("EXECUTE")}
    var date by remember{mutableStateOf("")};var cycle by remember{mutableStateOf("00:00")}
    var first by remember{mutableStateOf("1")};var last by remember{mutableStateOf("24")}
    var stationDetail by remember{mutableStateOf(false)}
    val init=runCatching{Instant.parse("${date}T${cycle}:00Z")}.getOrNull()
    Column(modifier,verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(if(weather)"Analyze a forecast window" else "Prepare a mission allocation",style=MaterialTheme.typography.titleLarge)
        if(weather){
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(date,{date=it},label={Text("Cycle date · YYYY-MM-DD")},modifier=Modifier.weight(1f));OutlinedTextField(cycle,{cycle=it},label={Text("Cycle time · HH:00 UTC")},modifier=Modifier.weight(1f))}
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(first,{first=it},label={Text("First forecast hour")},modifier=Modifier.weight(1f));OutlinedTextField(last,{last=it},label={Text("Last forecast hour")},modifier=Modifier.weight(1f))}
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(stationDetail,{stationDetail=it},enabled=!busy);Column{Text("Include local temperature detail",style=MaterialTheme.typography.bodyMedium);Text("Separate 0.05° temperature and dew-point query",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        }else ScopedRecordPicker(api,"/api/organizations/$org/regions/$region/operations","MISSION",mission,1,onChange={mission=it})
        OutlinedTextField(question,{question=it.take(2000)},label={Text(if(weather)"Preparedness question" else "Allocation requirements")},minLines=3,modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("EXECUTE" to "Execute","PLAN_FIRST" to "Plan first").forEach{(value,label)->FilterChip(selected=mode==value,onClick={mode=value},label={Text(label)})}}
        if(mode=="PLAN_FIRST")Text("Plan-first: the agent records its plan before evidence tools, and allocations from this run cannot be approved.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if(weather)"BigQuery calculates the indicators. Wonder explains their basis and gaps." else "Spark checks applicants, skills, stock and commitments. This creates a proposal only.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        val validWindow=init!=null&&(first.toIntOrNull()?:0)>=1&&(last.toIntOrNull()?:0)>=(first.toIntOrNull()?:1)&&(last.toIntOrNull()?:0)-(first.toIntOrNull()?:0)<48
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(enabled=!busy&&question.trim().length>=3&&(if(weather)validWindow else mission.isNotEmpty()),onClick={send(buildJsonObject{put("question",question);put("mode",mode);if(weather){put("include_station_detail",stationDetail);put("init_time",init.toString());put("from",Instant.fromEpochMilliseconds(init!!.toEpochMilliseconds()+(first.toLong()-1)*3600000).toString());put("to",Instant.fromEpochMilliseconds(init.toEpochMilliseconds()+last.toLong()*3600000).toString())}else put("mission_id",mission.first())})}){Text(if(weather)"Run assessment" else "Generate proposal")}
            TextButton(enabled=!busy,onClick=onClose){Text("Cancel")}
        }
    }
}
