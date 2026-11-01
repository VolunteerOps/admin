package com.example.admin.features.operations
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
@Composable internal fun ScopedRecordPicker(api:LiveApiTransport,path:String,kind:String,selected:Set<String>,maximum:Int,exclude:String?=null,onChange:(Set<String>)->Unit){
 var records by remember(path,kind){mutableStateOf(listOf<JsonObject>())}
 var next by remember(path,kind){mutableStateOf<Int?>(null)}
 var busy by remember(path,kind){mutableStateOf(false)}
 var error by remember(path,kind){mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 suspend fun load(append:Boolean){
  busy=true;error=false
  try{
   val response=api.execute(ApiEndpoint(HttpMethod.Get,path+"/records",mapOf("kind" to kind,"after" to if(append)(next?:0).toString() else "0")){it.jsonObject})
   val page=response["items"]!!.jsonArray.map{it.jsonObject}
   records=(if(append)records+page else page).distinctBy{it["id"]!!.jsonPrimitive.content}
   next=response["next_cursor"]?.jsonPrimitive?.intOrNull
  }catch(c:CancellationException){throw c}catch(_:Exception){error=true}finally{busy=false}
 }
 LaunchedEffect(path,kind){if(!path.contains("/regions//"))load(false)}
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
  val available=records.filter{it["id"]!!.jsonPrimitive.content!=exclude&&(kind!="COURSE"||it["payload"]!!.jsonObject["status"]?.jsonPrimitive?.content=="PUBLISHED")}
  available.forEach{record->
   val id=record["id"]!!.jsonPrimitive.content
   val title=record["payload"]!!.jsonObject["title"]?.jsonPrimitive?.content.orEmpty()
   FilterChip(selected=id in selected,enabled=!busy&&(id in selected||selected.size<maximum),onClick={onChange(if(id in selected)selected-id else selected+id)},label={Text(title)})
  }
  if(busy)LinearProgressIndicator()
  if(!busy&&available.isEmpty()&&!error)Text(if(kind=="COURSE")"No published courses available." else "No other reports available.")
  if(error)TextButton(onClick={scope.launch{load(false)}}){Text("Unable to load choices. Retry")}
  if(next!=null)TextButton(enabled=!busy,onClick={scope.launch{load(true)}}){Text("Load more choices")}
 }
}
