package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*

@Composable internal fun SupportCase(record:JsonObject,coordinator:Boolean,enabled:Boolean,send:(JsonObject)->Unit){
 val payload=record["payload"]!!.jsonObject
 var reply by remember(record["id"],record["version"]){mutableStateOf("")}
 var reason by remember(record["id"],record["version"]){mutableStateOf("")}
 Text("Case status: "+payload["status"]!!.jsonPrimitive.content)
 payload["resolution_note"]?.jsonPrimitive?.contentOrNull?.let{Text(it)}
 (payload["replies"] as? JsonArray)?.forEach{entry->val item=entry.jsonObject
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
   Text(if(item["coordinator"]?.jsonPrimitive?.booleanOrNull==true)"Coordinator" else "Case owner",style=MaterialTheme.typography.labelLarge)
   Text(item["body"]!!.jsonPrimitive.content);Text(item["created_at"]!!.jsonPrimitive.content,style=MaterialTheme.typography.bodySmall)
  }}
 }
 if(payload["status"]!!.jsonPrimitive.content!="RESOLVED"){
  OutlinedTextField(reply,{reply=it},label={Text("Reply to this case")},modifier=Modifier.fillMaxWidth())
  Button(enabled=enabled&&reply.isNotBlank()&&reply.length<=4000,onClick={send(buildJsonObject{put("action","REPLY_SUPPORT");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("body",reply)})}){Text("Send reply")}
 }
 if(coordinator){
  OutlinedTextField(reason,{reason=it},label={Text("Review or resolution note")},modifier=Modifier.fillMaxWidth())
  listOf("OPEN" to "Reopen","IN_PROGRESS" to "Start review","RESOLVED" to "Resolve").filter{it.first!=payload["status"]!!.jsonPrimitive.content}.forEach{(status,label)->
   TextButton(enabled=enabled&&reason.length in 10..1500,onClick={send(buildJsonObject{put("action","TRIAGE_SUPPORT");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("status",status);put("reason",reason)})}){Text(label)}
  }
 }
}
