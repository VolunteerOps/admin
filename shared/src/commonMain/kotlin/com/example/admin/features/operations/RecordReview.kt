package com.example.admin.features.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import com.example.admin.features.auth.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours


@Composable internal fun RecordReview(api:LiveApiTransport, path:String, record:JsonObject, records:JsonArray, admin:Boolean, userId:String, busy:Boolean, send:(JsonObject)->Unit) {
 if(record.text("kind")=="MISSION") {
  MissionDossier(api,path,record,admin,userId,busy,send,Modifier.fillMaxWidth())
  return
 }
 val payload=record["payload"]!!.jsonObject
 var body by remember(record.text("id")){mutableStateOf("")}
 var title by remember(record.text("id")){mutableStateOf("")}
 var hours by remember(record.text("id")){mutableStateOf("")}
 fun path()=path
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text(recordTitle(record),style=MaterialTheme.typography.headlineSmall)
  Text("Version "+record.text("version")+" / "+payload.text("status"))
  if(payload.text("brief").isNotBlank())Text(payload.text("brief"))
  if(payload.text("body").isNotBlank())Text(payload.text("body"))
      if(record.text("kind")=="COURSE"){
       Text("Skill: "+payload.text("skill"))
       if(admin&&payload.text("status")=="DRAFT")Button(enabled=!busy,onClick={send(buildJsonObject{put("action","PUBLISH_COURSE");put("id",record.text("id"));put("expected_version",record["version"]!!)})}){Text("Publish course")}
       if(payload.text("status")=="PUBLISHED")Button(enabled=!busy,onClick={send(buildJsonObject{put("action","ENROLL_COURSE");put("id",record.text("id"))})}){Text("Enroll")}
      }
      if(record.text("kind")=="ENROLLMENT")Text(payload.text("course_title")+" / Lessons passed: "+payload.text("next_lesson"))
      if(record.text("kind")=="CREDENTIAL")Text(payload.text("skill")+" / Valid until: "+payload.text("valid_until"))
      if(record.text("kind")=="INVENTORY")Text("${payload.text("quantity")} ${payload.text("unit")} at ${payload.text("location")} / ${payload.text("reserved")} reserved")
      if(record.text("kind")=="STOCK_LEDGER")Text("${payload.text("action")}: ${payload.text("delta")} / ${payload.text("reason")}")
      if(record.text("kind")=="MISSION"){
       Text(payload.text("meeting_point")+" / "+payload.text("starts_at"))
       if(!admin)Button(enabled=!busy,onClick={send(buildJsonObject{put("action","APPLY");put("id",record.text("id"))})}){Text("Apply to join")}
       if(!admin)TextButton(enabled=!busy,onClick={send(buildJsonObject{put("action","SAVE_MISSION");put("id",record.text("id"));put("saved",true)})}){Text("Save mission")}
       if(admin)Row(Modifier.horizontalScroll(rememberScrollState())){(when(payload.text("status")){"DRAFT"->listOf("STAGED","CANCELLED");"STAGED"->listOf("ACTIVE","CANCELLED");"ACTIVE"->listOf("PAUSED","COMPLETED","CANCELLED");"PAUSED"->listOf("ACTIVE","CANCELLED");else->emptyList()}).forEach{status->TextButton(enabled=!busy,onClick={send(buildJsonObject{put("action","TRANSITION_MISSION");put("id",record.text("id"));put("expected_version",record["version"]!!);put("status",status)})}){Text(when(status){"STAGED"->"Stage mission";"ACTIVE"->"Activate";"PAUSED"->"Pause";"COMPLETED"->"Complete";else->"Cancel mission"})}}}
      }
      if(record.text("kind")=="APPLICATION"&&!admin){
       if(payload.text("saved")=="true")TextButton(enabled=!busy,onClick={send(buildJsonObject{put("action","SAVE_MISSION");put("id",record.text("parentId"));put("saved",false)})}){Text("Remove saved mission")}
       if(payload.text("status") in listOf("APPLIED","WAITLISTED"))TextButton(enabled=!busy,onClick={send(buildJsonObject{put("action","WITHDRAW_APPLICATION");put("id",record.text("id"));put("expected_version",record["version"]!!)})}){Text("Withdraw application")}
      }
      
      if(record.text("kind")=="ASSIGNMENT"&&record.text("ownerId")==userId&&payload.text("status")=="ASSIGNED")Button(enabled=!busy,onClick={send(buildJsonObject{put("action","CHECK_IN");put("id",record.text("id"));put("expected_version",record["version"]!!)})}){Text("Check in")}
      if(record.text("kind")=="HOURS")Text("Submitted: ${payload.text("submitted_hours")} hours / Approved: ${payload.text("approved_hours").ifBlank{"awaiting review"}}")
      if(record.text("kind")=="ENROLLMENT"){
       if(admin&&payload.text("status")=="AWAITING_REVIEW")TrainingReview(record,!busy){send(it)}
       else if(record.text("ownerId")==userId&&payload.text("status")=="IN_PROGRESS")CourseStudy(api,path(),record,!busy){send(it)}
      }
      if(record.text("kind")=="BULLETIN") {
       Text("Audience: members of this region / Priority: "+payload.text("priority"))
       if(admin&&payload.text("status")=="PUBLISHED") {
        OutlinedTextField(body,{body=it},label={Text("Reason for withdrawal")},modifier=Modifier.fillMaxWidth())
        Button(enabled=!busy&&body.trim().length in 10..1000,onClick={send(buildJsonObject{put("action","WITHDRAW_BULLETIN");put("id",record.text("id"));put("expected_version",record["version"]!!);put("reason",body)})}){Text("Withdraw bulletin")}
       }
      }
      if(record.text("kind")=="EVENT"){Text("Change: "+payload.text("action"));Text("Record: "+payload.text("record_kind")+" / "+payload.text("record_id"))}
      if(record.text("kind")=="REPORT"&&admin)ReportReview(api,path(),record,!busy){send(it)}
      if(record.text("kind")=="MESSAGE")MessageActions(record,admin,!busy){send(it)}
      if(record.text("kind")=="SUPPORT")SupportCase(record,admin,!busy){send(it)}
      if(record.text("kind")=="CREDENTIAL"&&admin)CredentialRevocation(record,!busy){send(it)}
      if(record.text("kind")=="INVENTORY"&&admin){StockAdjustment(record,!busy){send(it)};StockTransfer(record,records,!busy){send(it)}}
      if(record.text("kind")=="APPLICATION"&&admin&&payload.text("status") in listOf("APPLIED","WAITLISTED"))AssignmentReview(api,path(),record,!busy){send(it)}
      if(record.text("kind")=="MISSION"&&payload.text("status")!="DRAFT"){
       OutlinedTextField(body,{body=it},label={Text("Message or field report")},modifier=Modifier.fillMaxWidth())
       OutlinedTextField(title,{title=it},label={Text("Report title")},modifier=Modifier.fillMaxWidth())
       Row{Button(enabled=!busy&&body.isNotBlank(),onClick={send(buildJsonObject{put("action","MESSAGE");put("id",record.text("id"));put("body",body)})}){Text("Send message")};TextButton(enabled=!busy&&body.isNotBlank()&&title.isNotBlank(),onClick={send(buildJsonObject{put("action","REPORT");put("id",record.text("id"));put("title",title);put("body",body)})}){Text("Submit report")}}
      }
      if((record.text("kind")=="ASSIGNMENT"&&record.text("ownerId")==userId&&payload.text("status")=="CHECKED_IN")||(record.text("kind")=="HOURS"&&admin)){
       OutlinedTextField(hours,{hours=it},label={Text("Actual hours contributed")})
       OutlinedTextField(body,{body=it},label={Text("Contribution / approval notes")})
       Button(enabled=!busy&&hours.toDoubleOrNull()?.let{it.isFinite()&&it in 0.0..24.0}==true&&body.length>=10,onClick={send(buildJsonObject{put("action",if(record.text("kind")=="HOURS")"APPROVE_HOURS" else "COMPLETE_ASSIGNMENT");put("id",record.text("id"));put("expected_version",record["version"]!!);put("hours",hours.toDouble());put(if(record.text("kind")=="HOURS")"reason" else "note",body)})}){Text(if(record.text("kind")=="HOURS")"Approve hours" else "Complete assignment")}
      }

 }
}
@Composable internal fun MissionCreation(busy:Boolean,send:(JsonObject)->Unit) {
 var title by remember {mutableStateOf("")};var body by remember {mutableStateOf("")}
 var starts by remember {mutableStateOf("")};var ends by remember {mutableStateOf("")}
 var capacity by remember {mutableStateOf("10")};var latitude by remember {mutableStateOf("")};var longitude by remember {mutableStateOf("")}
 var meeting by remember {mutableStateOf("")}
 var skills by remember {mutableStateOf("")}
 val validDates=runCatching{val a=Instant.parse(starts);val b=Instant.parse(ends);b>a&&b-a<=24.hours}.getOrDefault(false)
 val valid=title.trim().length in 3..160&&body.trim().length in 10..5000&&meeting.trim().length in 3..500&&(capacity.toIntOrNull()?:0) in 1..1000&&latitude.toDoubleOrNull()?.let{it.isFinite()&&it in -90.0..90.0}==true&&longitude.toDoubleOrNull()?.let{it.isFinite()&&it in -180.0..180.0}==true&&validDates
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text("Plan a mission",style=MaterialTheme.typography.headlineSmall)
     Text("Use UTC timestamps. The mission must last no more than 24 hours. Capacity: 1-1000.")
     listOf(Triple("Mission title",title,{v:String->title=v}),Triple("Brief",body,{v:String->body=v}),Triple("Meeting point",meeting,{v:String->meeting=v}),Triple("Starts (UTC, e.g. 2026-10-02T08:00:00Z)",starts,{v:String->starts=v}),Triple("Ends (UTC)",ends,{v:String->ends=v}),Triple("Capacity",capacity,{v:String->capacity=v}),Triple("Latitude",latitude,{v:String->latitude=v}),Triple("Longitude",longitude,{v:String->longitude=v})).forEach{(label,value,set)->OutlinedTextField(value,set,label={Text(label)},modifier=Modifier.fillMaxWidth())}
     OutlinedTextField(skills,{skills=it},label={Text("Required verified skills, separated by commas")},modifier=Modifier.fillMaxWidth())
     Button(enabled=!busy&&valid,onClick={send(buildJsonObject{put("action","CREATE_MISSION");put("mission",buildJsonObject{put("title",title);put("brief",body);put("meeting_point",meeting);put("starts_at",starts);put("ends_at",ends);put("capacity",capacity.toInt());put("latitude",latitude.toDouble());put("longitude",longitude.toDouble());put("required_skills",JsonArray(skills.split(',').map{it.trim()}.filter{it.isNotEmpty()}.distinct().map{JsonPrimitive(it)}))})})}){Text("Save mission draft")}

 }
}
