package com.example.admin.features.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException

@Composable internal fun AssignmentReview(api:LiveApiTransport,path:String,application:JsonObject,enabled:Boolean,send:(JsonObject)->Unit){
 var mission by remember(path,application["id"]){mutableStateOf<JsonObject?>(null)}
 var resources by remember(path){mutableStateOf(JsonArray(emptyList()))}
 var amounts by remember(path,application["id"]){mutableStateOf(mapOf<String,String>())}
 var failure by remember(path){mutableStateOf("")}
 var reload by remember{mutableStateOf(0)}
 LaunchedEffect(path,application["id"],reload){try{
  mission=api.execute(ApiEndpoint(HttpMethod.Get,path+"/records/"+application["parentId"]!!.jsonPrimitive.content){it.jsonObject})
  resources=api.execute(ApiEndpoint(HttpMethod.Get,path+"/records",mapOf("kind" to "INVENTORY")){it.jsonObject["items"]!!.jsonArray})
  failure=""
 }catch(c:CancellationException){throw c}catch(_:Exception){failure="Could not load the mission and available supplies. Refresh to try again."}}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Confirm the volunteer and supplies",style=MaterialTheme.typography.titleMedium)
  mission?.let{Text(it["payload"]!!.jsonObject["title"]!!.jsonPrimitive.content)}
  Text("Supplies are optional. Stock and responder availability are checked together when you confirm.")
  if(failure.isNotBlank())Text(failure,color=MaterialTheme.colorScheme.error)
  resources.forEach{item->val stock=item.jsonObject;val id=stock["id"]!!.jsonPrimitive.content;val payload=stock["payload"]!!.jsonObject
   OutlinedTextField(amounts[id].orEmpty(),{amounts=amounts+(id to it)},label={Text(payload["title"]!!.jsonPrimitive.content+" / "+payload["location"]!!.jsonPrimitive.content)},supportingText={Text("Available: "+((payload["quantity"]!!.jsonPrimitive.int)-(payload["reserved"]!!.jsonPrimitive.int)).toString())},modifier=Modifier.fillMaxWidth())
  }
  TextButton(onClick={reload++},enabled=enabled){Text("Refresh availability")}
  Button(enabled=enabled&&mission!=null&&failure.isBlank()&&amounts.values.all{it.isBlank()||(it.toIntOrNull()?:-1)>=0},onClick={
   val current=mission!!
   send(buildJsonObject{put("action","ASSIGN");put("id",current["id"]!!);put("expected_version",current["version"]!!);put("user_id",application["ownerId"]!!);put("supplies",buildJsonArray{amounts.forEach{(id,amount)->if((amount.toIntOrNull()?:0)>0)add(buildJsonObject{put("resource_id",id);put("quantity",amount.toInt())})}})})
  }){Text("Confirm assignment")}
 }
}
