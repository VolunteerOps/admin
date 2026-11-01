package com.example.admin.features.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.admin.core.icons.*
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

@Composable internal fun CoordinationOverview(
    api: LiveApiTransport, org: String, region: String,
    navigate: (String, String?) -> Unit, modifier: Modifier = Modifier,
) {
    var data by remember(org,region) { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var lane by remember { mutableStateOf("") }
    LaunchedEffect(org,region,reload) {
        loading=true; error=""
        try { data=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/regions/$region/operations/overview"){it.jsonObject}) }
        catch(c:CancellationException){throw c}
        catch(e:Exception){data=null;error=workspaceError(e)}
        finally {loading=false}
    }
    BoxWithConstraints(modifier) {
        val wide=maxWidth>=900.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if(wide)32.dp else 20.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Overview",style=MaterialTheme.typography.headlineLarge)
                    Text(data?.text("as_of")?.let { "Updated ${displayTime(it)}" }?:"Regional coordination",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick={reload++},enabled=!loading){Text("Refresh")}
            }
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank())PortalNotice("Overview unavailable",error)
            data?.let { result ->
                val counts=result["counts"]?.jsonObject?:buildJsonObject{}
                val paused=(result["dispatch"] as? JsonObject)?.text("paused")=="true"
                if(paused)Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("New dispatch is paused",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                        TextButton(onClick={navigate("Security",null)}){Text("Review controls")}
                    }
                }
                Surface(color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                    FlowRow(Modifier.fillMaxWidth().padding(24.dp),maxItemsInEachRow=if(wide)4 else 2,horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        listOf("active_missions" to "Active missions","checked_in" to "Volunteers checked in","awaiting_review" to "Awaiting a decision","staged_missions" to "Ready to activate").forEach { (key,label) ->
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text(counts.text(key).ifBlank{"0"},style=MaterialTheme.typography.headlineMedium)
                                Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                val queue: @Composable ()->Unit = {
                    val lanes=(result["lanes"] as? JsonArray).orEmpty().map{it.jsonObject}
                    val visible=lanes.filter{lane.isBlank()||it.text("key")==lane}
                    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text("Needs attention",style=MaterialTheme.typography.headlineSmall)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            FilterChip(lane.isBlank(),{lane=""},label={Text("All")})
                            lanes.filter{(it["count"]?.jsonPrimitive?.intOrNull?:0)>0}.forEach { item ->
                                FilterChip(lane==item.text("key"),{lane=item.text("key")},label={Text("${pageLabel(item.text("destination"))} ${item.text("count")}")})
                            }
                        }
                        Surface(shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                            Column {
                                val hasItems=visible.any{(it["items"] as? JsonArray)?.isNotEmpty()==true}
                                if(!hasItems)Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    OpsIcon(OpsSymbol.Check,Modifier.size(24.dp))
                                    Text("No decisions waiting",style=MaterialTheme.typography.titleMedium)
                                    Text("New applications and review requests will appear here.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                visible.forEach { section ->
                                    (section["items"] as? JsonArray).orEmpty().forEach { item ->
                                        val record=item.jsonObject
                                        Surface(onClick={navigate(section.text("destination"),record.text("id"))}) {
                                            Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                                    Text(section.text("action"),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(recordTitle(record),style=MaterialTheme.typography.titleMedium,maxLines=2)
                                                    StatusPill(record["payload"]!!.jsonObject.text("status"))
                                                }
                                                OpsIcon(OpsSymbol.ArrowRight,Modifier.size(20.dp))
                                            }
                                        }
                                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    if((section["count"]?.jsonPrimitive?.intOrNull?:0)>(section["items"] as? JsonArray).orEmpty().size)
                                        TextButton(onClick={navigate(section.text("destination"),null)},modifier=Modifier.padding(8.dp)){Text("View all ${section.text("count")} ${section.text("destination").lowercase()}")}
                                }
                            }
                        }
                    }
                }
                val missions: @Composable ()->Unit = {
                    val items=(result["missions"] as? JsonArray).orEmpty()
                    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("In the field",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);TextButton(onClick={navigate("Missions",null)}){Text("All missions")}}
                        if(items.isEmpty())Text("No staged or active missions.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        items.forEach { item ->
                            val record=item.jsonObject;val p=record["payload"]!!.jsonObject
                            Surface(onClick={navigate("Missions",record.text("id"))},shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){StatusPill(p.text("status"));Spacer(Modifier.weight(1f));OpsIcon(OpsSymbol.ArrowRight,Modifier.size(18.dp))}
                                    Text(recordTitle(record),style=MaterialTheme.typography.titleMedium)
                                    Text(p.text("meeting_point"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    val assigned=p["assigned_count"]?.jsonPrimitive?.intOrNull?:0;val capacity=p["capacity"]?.jsonPrimitive?.intOrNull?:0
                                    Row(Modifier.fillMaxWidth()){Text("$assigned / $capacity assigned",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);Text(displayTime(p.text("starts_at")),style=MaterialTheme.typography.bodySmall)}
                                    LinearProgressIndicator(progress={if(capacity>0)(assigned.toFloat()/capacity).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth(),trackColor=MaterialTheme.colorScheme.secondaryContainer)
                                }
                            }
                        }
                    }
                }
                if(wide)Row(horizontalArrangement=Arrangement.spacedBy(28.dp)) {Box(Modifier.weight(1.3f)){queue()};Box(Modifier.weight(1f)){missions()}}
                else {queue();missions()}
            }
        }
    }
}

internal fun displayTime(value:String):String = value.replace('T',' ').take(16).let{if(it.isBlank())"" else "$it UTC"}

@Composable internal fun PortalNotice(title:String,description:String,modifier:Modifier=Modifier) {
    Surface(modifier,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onErrorContainer)
            Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable internal fun StatusPill(status:String,modifier:Modifier=Modifier) {
    val attention=status in listOf("ACTION_REQUIRED","PENDING","AWAITING_REVIEW","WAITLISTED","PAUSED")
    Surface(modifier,shape=MaterialTheme.shapes.small,color=if(attention)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
        Text(status.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()},Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,color=if(attention)MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
