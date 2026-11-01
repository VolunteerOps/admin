package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*

@Composable internal fun ResourceCreation(enabled:Boolean,send:(JsonObject)->Unit){
 var title by remember{mutableStateOf("")};var unit by remember{mutableStateOf("")};var place by remember{mutableStateOf("")};var quantity by remember{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Add a resource",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(title,{title=it},label={Text("Resource name")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(unit,{unit=it},label={Text("Unit, e.g. pack")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(place,{place=it},label={Text("Storage location")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(quantity,{quantity=it},label={Text("Opening quantity")},modifier=Modifier.fillMaxWidth())
  Button(enabled=enabled&&title.length>=3&&unit.isNotBlank()&&place.length>=3&&(quantity.toIntOrNull()?:-1)>=0,onClick={send(buildJsonObject{put("action","CREATE_RESOURCE");put("title",title);put("unit",unit);put("location",place);put("quantity",quantity.toInt())})}){Text("Save opening stock")}
 }
}
@Composable internal fun StockAdjustment(record:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var delta by remember(record["id"]){mutableStateOf("")};var reason by remember(record["id"]){mutableStateOf("")}
 OutlinedTextField(delta,{delta=it},label={Text("Quantity change (+ received / - removed)")},modifier=Modifier.fillMaxWidth())
 OutlinedTextField(reason,{reason=it},label={Text("Reason for this correction")},modifier=Modifier.fillMaxWidth())
 Button(enabled=enabled&&(delta.toIntOrNull()?:0)!=0&&reason.length>=10,onClick={send(buildJsonObject{put("action","ADJUST_STOCK");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("delta",delta.toInt());put("reason",reason)})}){Text("Record stock adjustment")}
}

@Composable internal fun StockTransfer(record:JsonObject,resources:JsonArray,enabled:Boolean,send:(JsonObject)->Unit){
 val source=record["payload"]!!.jsonObject
 var destination by remember(record["id"],record["version"]){mutableStateOf<String?>(null)}
 var quantity by remember(record["id"],record["version"]){mutableStateOf("")}
 var reason by remember(record["id"],record["version"]){mutableStateOf("")}
 val targets=resources.map{it.jsonObject}.filter{it["id"]!=record["id"]&&it["kind"]?.jsonPrimitive?.content=="INVENTORY"&&it["payload"]!!.jsonObject["title"]==source["title"]&&it["payload"]!!.jsonObject["unit"]==source["unit"]}
 val target=targets.firstOrNull{it["id"]!!.jsonPrimitive.content==destination}
 Text("Transfer available stock",style=MaterialTheme.typography.titleMedium)
 Text("Choose a matching resource at another storage location. Load more resources above if the destination is missing.")
 targets.forEach{item->val payload=item["payload"]!!.jsonObject
  FilterChip(selected=item["id"]!!.jsonPrimitive.content==destination,onClick={destination=item["id"]!!.jsonPrimitive.content},enabled=enabled,label={Text(payload["location"]!!.jsonPrimitive.content)})
 }
 if(targets.isEmpty())Text("Add a destination with the same resource name and unit first.")
 OutlinedTextField(quantity,{quantity=it},label={Text("Quantity to transfer")},modifier=Modifier.fillMaxWidth())
 OutlinedTextField(reason,{reason=it},label={Text("Transfer reason")},modifier=Modifier.fillMaxWidth())
 val available=(source["quantity"]?.jsonPrimitive?.intOrNull?:0)-(source["reserved"]?.jsonPrimitive?.intOrNull?:0)
 Button(enabled=enabled&&target!=null&&(quantity.toIntOrNull()?:0) in 1..available&&reason.length in 10..1500,onClick={
  send(buildJsonObject{put("action","TRANSFER_STOCK");put("id",record["id"]!!);put("expected_version",record["version"]!!);put("target_id",target!!["id"]!!);put("target_version",target["version"]!!);put("quantity",quantity.toInt());put("reason",reason)})
 }){Text("Confirm stock transfer")}
}
