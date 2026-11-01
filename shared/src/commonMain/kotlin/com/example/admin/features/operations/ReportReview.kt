package com.example.admin.features.operations
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*
import com.example.admin.core.network.*
@Composable internal fun ReportReview(api:LiveApiTransport,path:String,record:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var reason by remember(record["id"]){mutableStateOf("")}
 var related by remember(record["id"]){mutableStateOf(setOf<String>())}
 Text("Review field report",style=MaterialTheme.typography.titleMedium)
 OutlinedTextField(reason,{reason=it},label={Text("Review notes and next action")},modifier=Modifier.fillMaxWidth())
 Text("Related report (optional)")
 ScopedRecordPicker(api,path,"REPORT",related,1,record["id"]!!.jsonPrimitive.content){related=it}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){listOf("REVIEWING","ACTION_REQUIRED","RESOLVED").forEach{status->
  TextButton(enabled=enabled&&reason.trim().length>=10,onClick={send(buildJsonObject{put("action","TRIAGE_REPORT");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("status",status);put("reason",reason);related.firstOrNull()?.let{put("related_report_id",it)}})}){Text(status.lowercase().replace('_',' '))}
 }}
}
@Composable internal fun MessageActions(record:JsonObject,admin:Boolean,enabled:Boolean,send:(JsonObject)->Unit){
 var body by remember(record["id"]){mutableStateOf("")}
 var reason by remember(record["id"]){mutableStateOf("")}
 if(record["payload"]!!.jsonObject["status"]?.jsonPrimitive?.content=="REMOVED")return
 OutlinedTextField(body,{body=it},label={Text("Reply to this message")},modifier=Modifier.fillMaxWidth())
 Button(enabled=enabled&&body.isNotBlank(),onClick={send(buildJsonObject{put("action","MESSAGE");put("id",record["parentId"]!!);put("reply_to",record["id"]!!);put("body",body)})}){Text("Send reply")}
 if(admin){
  OutlinedTextField(reason,{reason=it},label={Text("Moderation reason (audit only)")},modifier=Modifier.fillMaxWidth())
  TextButton(enabled=enabled&&reason.trim().length>=10,onClick={send(buildJsonObject{put("action","MODERATE_MESSAGE");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("reason",reason)})}){Text("Remove message")}
 }
}
