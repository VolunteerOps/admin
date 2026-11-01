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

@Composable internal fun TeamSponsorship(api:LiveApiTransport,org:String){
 val scope=rememberCoroutineScope();var members by remember{mutableStateOf(JsonArray(emptyList()))};var seats by remember{mutableStateOf(JsonArray(emptyList()))};var busy by remember{mutableStateOf(false)};var notice by remember{mutableStateOf("")}
 suspend fun load(){members=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/memberships"){it.jsonArray});seats=api.execute(ApiEndpoint(HttpMethod.Get,"/api/organizations/$org/pro/sponsorships"){it.jsonArray})}
 LaunchedEffect(org){if(org.isNotBlank()){busy=true;try{load()}catch(e:Exception){notice="Coordinator access and recent MFA are required."}finally{busy=false}}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Bring your team further",style=MaterialTheme.typography.headlineMedium)
  Text("Your active ResQOps Pro plan can sponsor up to three volunteers across your workspaces. Sponsorship adds Pro tools without changing anyone's role.")
  if(notice.isNotBlank())Text(notice)
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  members.filter{it.jsonObject.text("role")=="VOLUNTEER"&&it.jsonObject.text("status")=="ACTIVE"}.forEach{item->val member=item.jsonObject;val seat=seats.firstOrNull{it.jsonObject["userId"]==member["userId"]}?.jsonObject;val active=seat?.get("active")?.jsonPrimitive?.booleanOrNull==true
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){
    Text((member["user"] as? JsonObject)?.text("fullName")?:"Volunteer")
    Text(if(active)"Sponsored Pro access" else "Standard access")
    Button(enabled=!busy,onClick={busy=true;scope.launch{try{api.execute(ApiEndpoint(HttpMethod.Put,"/api/organizations/$org/pro/sponsorships"){it},buildJsonObject{put("user_id",member["userId"]!!);put("active",!active);put("expected_version",seat?.get("version")?.jsonPrimitive?.int?:0)});load();notice="Sponsorship updated."}catch(e:Exception){notice=if(e is ApiFailure)e.code else "Could not update sponsorship."}finally{busy=false}}}){Text(if(active)"Release seat" else "Sponsor this volunteer")}
   }}
  }
 }
}
