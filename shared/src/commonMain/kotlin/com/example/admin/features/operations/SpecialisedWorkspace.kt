@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

private data class Capability(val key:String,val label:String,val path:String,val model:String,val needsMission:Boolean=false,val needsAssessment:Boolean=false,val question:String)
private val CAPABILITIES=listOf(
 Capability("FLOOD","Flood synthesis","flood","Wonder",needsAssessment=true,question="What flood preparation should we prioritise?"),
 Capability("WATCH","Rising-water watch","watch","Wonder",question="Prepare a rising-water watch brief for this region."),
 Capability("HEAT","Heat-health preparation","heat","Spark",needsAssessment=true,question="What heat preparation should we prioritise?"),
 Capability("WIND","Wind & access","wind","Spark",needsMission=true,question="What access assumptions must we re-check?"),
 Capability("MULTI","Multi-region comparison","multi-region","Wonder",question="Where is preparation attention most needed?"),
 Capability("OUTCOME","Forecast vs outcome","outcome","Wonder",needsAssessment=true,question="How did the forecast compare with observations?"),
 Capability("FORESIGHT","Credential foresight","foresight","Spark",question="Which roles will become unstaffable?"),
 Capability("WHATIF","What-if scenario","what-if","Spark",needsMission=true,question="How does this scenario change the plan?"),
 Capability("SHORTAGE","Shortage options","shortage","Spark",needsMission=true,question="How can we cover these shortages?"),
 Capability("BRIEFING","Audience briefing","briefings","Meta",question=""),
 Capability("AFTER","After-action draft","after-action","Spark",needsMission=true,question="Draft an after-action summary for this mission."),
)

@Composable internal fun SpecialisedWorkspace(api:LiveApiTransport,org:String,region:String,onLock:(Boolean)->Unit,modifier:Modifier=Modifier) {
 val base="/api/organizations/$org/sentinel/regions/$region/specialised"
 val opsBase="/api/organizations/$org/regions/$region/operations"
 val planningBase="/api/organizations/$org/sentinel/regions/$region/planning"
 val scope=rememberCoroutineScope()
 var mode by remember{mutableStateOf("Analyses")}
 var capability by remember{mutableStateOf(CAPABILITIES[0])}
 var question by remember{mutableStateOf(CAPABILITIES[0].question)}
 var mission by remember{mutableStateOf(emptySet<String>())}
 var assessmentId by remember{mutableStateOf("")}
 var assessments by remember{mutableStateOf(JsonArray(emptyList()))}
 var proposals by remember{mutableStateOf(JsonArray(emptyList()))}
 var analyses by remember{mutableStateOf(JsonArray(emptyList()))}
 var records by remember{mutableStateOf(JsonArray(emptyList()))}
 var selected by remember{mutableStateOf<JsonObject?>(null)}
 var busy by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var horizon by remember{mutableStateOf("60")}
 var capacityOverride by remember{mutableStateOf("")}
 var skillsOverride by remember{mutableStateOf("")}
 var stockAdjustments by remember{mutableStateOf("")}
 var shortageResource by remember{mutableStateOf(emptySet<String>())}
 var shortageQuantity by remember{mutableStateOf("10")}
 var sourceKind by remember{mutableStateOf("ASSESSMENT")}
 var audience by remember{mutableStateOf("COORDINATOR")}
 var language by remember{mutableStateOf("en")}
 var reason by remember{mutableStateOf("")}
 DisposableEffect(Unit){onDispose{onLock(false)}}
 LaunchedEffect(busy){onLock(busy)}
 fun loadHistory(){scope.launch{try{
   records=api.execute(ApiEndpoint(HttpMethod.Get,"$base") {it.jsonObject})["items"]!!.jsonArray
   assessments=api.execute(ApiEndpoint(HttpMethod.Get,"$planningBase/weather") {it.jsonObject})["items"]!!.jsonArray
   proposals=api.execute(ApiEndpoint(HttpMethod.Get,"$planningBase/resources") {it.jsonObject})["items"]!!.jsonArray
   analyses=records
 }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}}}
 LaunchedEffect(base){loadHistory()}
 fun post(path:String,body:JsonObject,done:()->Unit={}){if(busy)return;busy=true;error="";scope.launch{try{
   selected=api.execute(ApiEndpoint(HttpMethod.Post,path,headers=mapOf("Idempotency-Key" to Uuid.random().toString())){it.jsonObject},body);done();loadHistory()
 }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}
 Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Specialised planning",style=MaterialTheme.typography.headlineLarge)
  Text("Meta-powered flood, hazard, scenario and briefing tools. Every result is a draft until a coordinator acts on it.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   listOf("Analyses","Evidence & tasks").forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m)})}
  }
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(error.isNotBlank())PortalNotice("Request not completed",error)
  if(mode=="Analyses") {
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    CAPABILITIES.forEach{c->FilterChip(selected=capability.key==c.key,onClick={capability=c;question=c.question},label={Text(c.label)})}
   }
   Text("${capability.label} · ${capability.model}",style=MaterialTheme.typography.titleMedium)
   if(capability.needsMission)ScopedRecordPicker(api,opsBase,"MISSION",mission,1,onChange={mission=it})
   if(capability.needsAssessment||capability.key=="BRIEFING") {
    Text(if(capability.key=="BRIEFING")"Source assessment" else "Weather assessment (latest if unselected)",style=MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     assessments.forEach{item->val r=item.jsonObject;val p=r["payload"]!!.jsonObject
      FilterChip(selected=assessmentId==r.text("id"),onClick={assessmentId=if(assessmentId==r.text("id"))"" else r.text("id")},label={Text(p.text("title").ifBlank{"Assessment"})})}
    }
   }
   when(capability.key){
    "FORESIGHT"->OutlinedTextField(horizon,{horizon=it.filter(Char::isDigit)},label={Text("Horizon days")},modifier=Modifier.fillMaxWidth())
    "WHATIF"->{
     OutlinedTextField(capacityOverride,{capacityOverride=it.filter(Char::isDigit)},label={Text("Capacity override (optional)")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(skillsOverride,{skillsOverride=it},label={Text("Required skills override, comma separated (optional)")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(stockAdjustments,{stockAdjustments=it},label={Text("Stock adjustments: resource-id:+5, other-id:-2 (optional)")},modifier=Modifier.fillMaxWidth())
     Text("Base proposal for diff (optional)",style=MaterialTheme.typography.titleSmall)
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      proposals.forEach{item->val r=item.jsonObject;FilterChip(selected=assessmentId==r.text("id"),onClick={assessmentId=r.text("id")},label={Text("Proposal")})}
     }
    }
    "SHORTAGE"->{
     ScopedRecordPicker(api,opsBase,"INVENTORY",shortageResource,1,onChange={shortageResource=it})
     OutlinedTextField(shortageQuantity,{shortageQuantity=it.filter(Char::isDigit)},label={Text("Quantity needed")},modifier=Modifier.fillMaxWidth())
    }
    "BRIEFING"->{
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("ASSESSMENT","PROPOSAL","ANALYSIS").forEach{k->FilterChip(selected=sourceKind==k,onClick={sourceKind=k},label={Text(k.lowercase().replaceFirstChar(Char::uppercase))})}}
     if(sourceKind!="ASSESSMENT")Text("Select the source from the assessment/proposal chips above is not used; enter via history selection below.",style=MaterialTheme.typography.bodySmall)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("COORDINATOR","VOLUNTEER","PUBLIC").forEach{a->FilterChip(selected=audience==a,onClick={audience=a},label={Text(a.lowercase().replaceFirstChar(Char::uppercase))})}}
     OutlinedTextField(language,{language=it.take(12)},label={Text("Language code (en, sw, fr…)")},modifier=Modifier.fillMaxWidth())
    }
   }
   if(capability.key!="BRIEFING")OutlinedTextField(question,{question=it.take(2000)},label={Text("Question / brief")},minLines=2,modifier=Modifier.fillMaxWidth())
   val canRun=!busy&&when{
    capability.needsMission->mission.isNotEmpty()
    capability.key=="SHORTAGE"->mission.isNotEmpty()&&shortageResource.isNotEmpty()&&(shortageQuantity.toIntOrNull()?:0)>0
    capability.key=="OUTCOME"->assessmentId.isNotBlank()
    capability.key=="BRIEFING"->assessmentId.isNotBlank()||selected!=null
    else->question.trim().length>=3||capability.key=="WATCH"
   }
   Button(enabled=canRun,onClick={
    val body=buildJsonObject{
     when(capability.key){
      "FLOOD","HEAT"->{put("question",question);if(assessmentId.isNotBlank())put("assessment_id",assessmentId)}
      "WATCH"->put("question",question.ifBlank{"Prepare a rising-water watch brief for this region."})
      "WIND","AFTER"->{put("mission_id",mission.first());put("question",question)}
      "OUTCOME"->{put("assessment_id",assessmentId);put("question",question)}
      "FORESIGHT"->{put("horizon_days",horizon.toIntOrNull()?:60);put("question",question)}
      "WHATIF"->{put("mission_id",mission.first());put("question",question)
       capacityOverride.toIntOrNull()?.let{put("capacity_override",it)}
       if(skillsOverride.isNotBlank())put("required_skills_override",JsonArray(skillsOverride.split(",").map{JsonPrimitive(it.trim())}.filter{it.content.isNotBlank()}))
       val adjustments=stockAdjustments.split(",").mapNotNull{part->val bits=part.trim().split(":");if(bits.size==2)bits[0].trim() to (bits[1].trim().toIntOrNull()?:return@mapNotNull null) else null}
       put("stock_adjustments",JsonArray(adjustments.map{(id,delta)->buildJsonObject{put("resource_id",id);put("delta",delta)}}))
       if(assessmentId.isNotBlank())put("base_proposal_id",assessmentId)}
      "SHORTAGE"->{put("mission_id",mission.first());put("question",question);put("requirements",JsonArray(listOf(buildJsonObject{put("resource_id",shortageResource.first());put("quantity",shortageQuantity.toIntOrNull()?:1)})))}
      "BRIEFING"->{put("source_kind",sourceKind);put("source_id",if(sourceKind=="ASSESSMENT")assessmentId else selected?.text("id").orEmpty());put("audience",audience);put("language",language.ifBlank{"en"})}
      else->put("question",question)
     }
    }
    post("$base/${capability.path}",body)
   }){Text("Run ${capability.label}")}
   if(capability.key=="WATCH")OutlinedButton(enabled=!busy,onClick={post("$base/watch/schedule",JsonObject(emptyMap()))}){Text("Schedule hourly watch brief")}
   selected?.let{record->val p=record["payload"]!!.jsonObject
    Surface(border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shape=MaterialTheme.shapes.medium){
     Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      Row{StatusPill(p.text("status"));Spacer(Modifier.weight(1f));Text(p.text("analysis_type"),style=MaterialTheme.typography.labelMedium)}
      Text(p.text("title").ifBlank{capability.label},style=MaterialTheme.typography.titleMedium)
      val result=p["result"] as? JsonObject;val briefing=p["briefing"] as? JsonObject;val recommendation=p["recommendation"] as? JsonObject
      Text(briefing?.text("body")?:result?.text("summary")?:recommendation?.text("summary")?:p.text("message"),style=MaterialTheme.typography.bodyMedium)
      HarnessView(p["harness"] as? JsonObject)
      SpecNotes("Findings",result?.get("findings"));SpecNotes("Recommendations",result?.get("recommendations"));SpecNotes("Missing inputs",result?.get("missing_inputs"))
      SpecNotes("Key points",briefing?.get("key_points"));SpecNotes("Limitations",briefing?.get("limitations"));SpecNotes("Shortages",recommendation?.get("shortages"));SpecNotes("Decisions",recommendation?.get("decisions"))
      if(p.text("flood_impact_estimate")=="null"||p.containsKey("flood_impact_estimate"))Text("No local flood-impact estimate is produced from this evidence.",style=MaterialTheme.typography.labelMedium)
      (p["diff"] as? JsonObject)?.let{d->Text("Scenario diff · Added ${d["added_user_ids"]} · Removed ${d["removed_user_ids"]} · Quantity changes ${d["quantity_changes"]}",style=MaterialTheme.typography.bodySmall)}
      if(p.text("status")=="DRAFT"&&p.text("analysis_type") in listOf("BRIEFING","AFTER_ACTION")){
       OutlinedTextField(reason,{reason=it.take(1500)},label={Text("Approval reason")},modifier=Modifier.fillMaxWidth())
       Button(enabled=!busy&&reason.trim().length>=10,onClick={post("$base/${record.text("id")}/approve",buildJsonObject{put("expected_version",record["version"]!!);put("reason",reason)})}){Text("Approve for use")}
      }
      if(p.text("status")=="APPROVED"&&p.text("analysis_type")=="BRIEFING"){
       OutlinedTextField(language,{language=it.take(12)},label={Text("Translate to language code")},modifier=Modifier.fillMaxWidth())
       OutlinedButton(enabled=!busy,onClick={post("$base/briefings/${record.text("id")}/translate",buildJsonObject{put("language",language.ifBlank{"sw"})})}){Text("Create translation draft")}
      }
     }
    }
   }
   Text("History",style=MaterialTheme.typography.titleLarge)
   records.forEach{item->val r=item.jsonObject;val p=r["payload"]!!.jsonObject
    Surface(onClick={selected=r},enabled=!busy,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
     Row(Modifier.fillMaxWidth().padding(16.dp)){Column(Modifier.weight(1f)){Text(p.text("title").ifBlank{p.text("analysis_type")},style=MaterialTheme.typography.titleSmall);Text(p.text("analysis_type")+" · "+displayTime(r.text("createdAt")),style=MaterialTheme.typography.bodySmall)};StatusPill(p.text("status"))}
    }
   }
  } else {
   SpecialisedEvidence(api,base,opsBase,assessments)
  }
 }
}

@Composable private fun SpecNotes(title:String,value:JsonElement?) {
 val lines=(value as? JsonArray).orEmpty()
 if(lines.isNotEmpty()){Text(title,style=MaterialTheme.typography.titleSmall);lines.forEach{Text("• ${it.jsonPrimitive.contentOrNull.orEmpty()}",style=MaterialTheme.typography.bodySmall)}}
}

@Composable private fun SpecialisedEvidence(api:LiveApiTransport,base:String,opsBase:String,assessments:JsonArray) {
 val scope=rememberCoroutineScope()
 var kind by remember{mutableStateOf("GAUGE_READING")}
 var items by remember{mutableStateOf(JsonArray(emptyList()))}
 var tasks by remember{mutableStateOf(JsonArray(emptyList()))}
 var busy by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var name by remember{mutableStateOf("")}
 var gaugeId by remember{mutableStateOf("")}
 var river by remember{mutableStateOf("")}
 var level by remember{mutableStateOf("")}
 var gaugeLat by remember{mutableStateOf("")}
 var gaugeLon by remember{mutableStateOf("")}
 var watch by remember{mutableStateOf("")}
 var accessType by remember{mutableStateOf("ROAD")}
 var accessStatus by remember{mutableStateOf("OPEN")}
 var group by remember{mutableStateOf("")}
 var count by remember{mutableStateOf("")}
 var needs by remember{mutableStateOf("")}
 var variable by remember{mutableStateOf("RAINFALL_MM")}
 var value by remember{mutableStateOf("")}
 var unit by remember{mutableStateOf("mm")}
 var observedAt by remember{mutableStateOf(Clock.System.now().toString())}
 var resource by remember{mutableStateOf(emptySet<String>())}
 var substitute by remember{mutableStateOf(emptySet<String>())}
 var assessmentId by remember{mutableStateOf("")}
 var assignee by remember{mutableStateOf("")}
 fun load(){scope.launch{try{
   items=api.execute(ApiEndpoint(HttpMethod.Get,"$base/evidence"){it.jsonObject})["items"]!!.jsonArray
   tasks=api.execute(ApiEndpoint(HttpMethod.Get,"$base/gap-tasks"){it.jsonObject})["items"]!!.jsonArray
 }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}}}
 LaunchedEffect(base){load()}
 fun send(path:String,body:JsonObject){if(busy)return;busy=true;error="";scope.launch{try{
   api.execute(ApiEndpoint(HttpMethod.Post,path,headers=mapOf("Idempotency-Key" to Uuid.random().toString())){it.jsonObject},body);load()
 }catch(c:CancellationException){throw c}catch(e:Exception){error=workspaceError(e)}finally{busy=false}}}
 if(error.isNotBlank())PortalNotice("Request not completed",error)
 Text("Record local evidence",style=MaterialTheme.typography.titleLarge)
 Text("Gauges, access, vulnerability, observations and substitutes are coordinator-recorded inputs for the specialised tools.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  listOf("GAUGE_READING" to "Gauge","ROAD_STATUS" to "Access","VULNERABILITY_PROFILE" to "Vulnerability","FIELD_OBSERVATION" to "Observation","SUBSTITUTE_LINK" to "Substitute").forEach{(k,label)->
   FilterChip(selected=kind==k,onClick={kind=k},label={Text(label)})}
 }
 when(kind){
  "GAUGE_READING"->{
   OutlinedTextField(gaugeId,{gaugeId=it},label={Text("Gauge ID")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(name,{name=it},label={Text("Gauge name")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(river,{river=it},label={Text("River")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(level,{level=it},label={Text("Level (m)")},modifier=Modifier.weight(1f));OutlinedTextField(watch,{watch=it},label={Text("Watch level (m)")},modifier=Modifier.weight(1f))}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(gaugeLat,{gaugeLat=it},label={Text("Latitude (for radar pin)")},modifier=Modifier.weight(1f));OutlinedTextField(gaugeLon,{gaugeLon=it},label={Text("Longitude (for radar pin)")},modifier=Modifier.weight(1f))}
  }
  "ROAD_STATUS"->{
   OutlinedTextField(name,{name=it},label={Text("Road / bridge / ferry name")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("ROAD","BRIDGE","FERRY","OTHER").forEach{t->FilterChip(selected=accessType==t,onClick={accessType=t},label={Text(t.lowercase().replaceFirstChar(Char::uppercase))})}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("OPEN","RESTRICTED","CLOSED").forEach{s->FilterChip(selected=accessStatus==s,onClick={accessStatus=s},label={Text(s.lowercase().replaceFirstChar(Char::uppercase))})}}
  }
  "VULNERABILITY_PROFILE"->{
   OutlinedTextField(group,{group=it},label={Text("Group (e.g. older residents)")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(count,{count=it.filter(Char::isDigit)},label={Text("Count")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(needs,{needs=it},label={Text("Needs, comma separated")},modifier=Modifier.fillMaxWidth())
  }
  "FIELD_OBSERVATION"->{
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("RAINFALL_MM","TEMPERATURE_C","WIND_MPS","RIVER_LEVEL_M").forEach{v->FilterChip(selected=variable==v,onClick={variable=v;unit=when(v){"RAINFALL_MM"->"mm";"TEMPERATURE_C"->"°C";"WIND_MPS"->"m/s";else->"m"}},label={Text(v)})}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(value,{value=it},label={Text("Observed value")},modifier=Modifier.weight(1f));OutlinedTextField(unit,{unit=it},label={Text("Unit")},modifier=Modifier.weight(1f))}
  }
  else->{
   Text("Stock item",style=MaterialTheme.typography.titleSmall);ScopedRecordPicker(api,opsBase,"INVENTORY",resource,1,onChange={resource=it})
   Text("Recorded substitute",style=MaterialTheme.typography.titleSmall);ScopedRecordPicker(api,opsBase,"INVENTORY",substitute,1,onChange={substitute=it})
  }
 }
 OutlinedTextField(observedAt,{observedAt=it},label={Text("Observed at (ISO UTC)")},modifier=Modifier.fillMaxWidth())
 Button(enabled=!busy,onClick={
  val body=buildJsonObject{put("kind",kind);put("observed_at",observedAt)
   when(kind){
    "GAUGE_READING"->{put("gauge_id",gaugeId);put("name",name);put("river",river);put("level_m",level.toDoubleOrNull()?:0.0);put("watch_level_m",watch.toDoubleOrNull()?:0.0);gaugeLat.toDoubleOrNull()?.let{put("latitude",it)};gaugeLon.toDoubleOrNull()?.let{put("longitude",it)}}
    "ROAD_STATUS"->{put("name",name);put("access_type",accessType);put("status",accessStatus);put("note","")}
    "VULNERABILITY_PROFILE"->{put("group",group);put("count",count.toIntOrNull()?:0);put("needs",JsonArray(needs.split(",").map{JsonPrimitive(it.trim())}.filter{it.content.isNotBlank()}));put("note","")}
    "FIELD_OBSERVATION"->{put("variable",variable);put("value",value.toDoubleOrNull()?:0.0);put("unit",unit);put("note","")}
    else->{put("resource_id",resource.firstOrNull()?:"");put("substitute_id",substitute.firstOrNull()?:"");put("note","")}
   }}
  send("$base/evidence",body)
 }){Text("Record evidence")}
 Text("Recorded evidence",style=MaterialTheme.typography.titleMedium)
 items.take(30).forEach{item->val e=item.jsonObject
  Text("• ${e.text("evidence_kind")} · ${e.text("name").ifBlank{e.text("group").ifBlank{e.text("variable")}}} · ${displayTime(e.text("observed_at"))}",style=MaterialTheme.typography.bodySmall)}
 HorizontalDivider()
 Text("Evidence-gap worklist",style=MaterialTheme.typography.titleLarge)
 Text("Build tracked tasks from an assessment's missing inputs.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  assessments.forEach{item->val r=item.jsonObject;FilterChip(selected=assessmentId==r.text("id"),onClick={assessmentId=r.text("id")},label={Text("Assessment")})}
 }
 Button(enabled=!busy&&assessmentId.isNotBlank(),onClick={send("$base/gap-worklist",buildJsonObject{put("assessment_id",assessmentId)})}){Text("Build worklist")}
 OutlinedTextField(assignee,{assignee=it.filter(Char::isDigit)},label={Text("Assignee member ID (optional)")},modifier=Modifier.fillMaxWidth())
 tasks.forEach{item->val t=item.jsonObject;val p=t["payload"]!!.jsonObject
  Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text(p.text("title"),style=MaterialTheme.typography.bodyMedium);Text(p.text("status")+" · Assignee ${p.text("assignee_user_id").ifBlank{"—"}}",style=MaterialTheme.typography.bodySmall)}
   TextButton(enabled=!busy,onClick={send("$base/gap-tasks/${t.text("id")}",buildJsonObject{put("status",if(p.text("status")=="DONE")"OPEN" else "DONE");put("assignee_user_id",assignee.toIntOrNull()?.let{JsonPrimitive(it)}?:JsonNull)})}){Text(if(p.text("status")=="DONE")"Reopen" else "Mark done")}
  }
 }
}
