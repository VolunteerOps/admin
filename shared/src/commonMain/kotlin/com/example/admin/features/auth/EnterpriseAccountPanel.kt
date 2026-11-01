package com.example.admin.features.auth
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
private fun JsonObject.text(key:String)=get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
@Composable fun EnterpriseAccountPanel(organization:String,platform:Boolean,call:suspend(HttpMethod,String,JsonElement?)->JsonElement){
 val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)};var notice by remember{mutableStateOf("")}
 var reason by remember{mutableStateOf("")};var title by remember{mutableStateOf("")};var slug by remember{mutableStateOf("")}
 var domain by remember{mutableStateOf("")};var evidence by remember{mutableStateOf("")};var userId by remember{mutableStateOf("")}
 var provider by remember{mutableStateOf("password")};var mfa by remember{mutableStateOf(true)}
 var organizations by remember{mutableStateOf<JsonArray>(JsonArray(emptyList()))};var applications by remember{mutableStateOf<JsonArray>(JsonArray(emptyList()))}
 var requests by remember(organization){mutableStateOf<JsonArray>(JsonArray(emptyList()))};var invites by remember(organization){mutableStateOf<JsonArray>(JsonArray(emptyList()))}
 var selected by remember{mutableStateOf<JsonObject?>(null)};var proof by remember(organization){mutableStateOf<JsonObject?>(null)}
 var support by remember{mutableStateOf<JsonObject?>(null)};var overview by remember{mutableStateOf<JsonObject?>(null)}
 fun perform(action:suspend()->Unit){if(!busy){busy=true;notice="";scope.launch{try{action()}catch(e:CancellationException){throw e}catch(_:Exception){notice="Action not confirmed. Check current access, MFA, required fields and record version."}finally{busy=false}}}}
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text("Workspace and partnership controls",style=MaterialTheme.typography.titleLarge)
 if(notice.isNotBlank())Text(notice)
 if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 OutlinedTextField(reason,{reason=it},label={Text("Reason for this action (10 characters minimum)")})
 OutlinedTextField(title,{title=it},label={Text("Institution or unit name")})
 OutlinedTextField(domain,{domain=it},label={Text("Exact institution domain")})
 Button(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/me/partnerships",buildJsonObject{put("name",title);put("domain",domain);put("message",reason)});notice="Partnership submitted for owner review."}}){Text("Submit partnership application")}
 if(organization.isNotBlank()){
  TextButton(enabled=!busy,onClick={perform{requests=call(HttpMethod.Get,"/api/organizations/"+organization+"/join-requests",null).jsonArray;invites=call(HttpMethod.Get,"/api/organizations/"+organization+"/invitations",null).jsonArray}}){Text("Review recruitment")}
  requests.forEach{element->val request=element.jsonObject;Card{Column(Modifier.padding(10.dp)){Text(request.text("message"));Text(request.text("status"));if(request.text("status")=="PENDING")Row{
   listOf("INVITE","DECLINE").forEach{decision->TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/organizations/"+organization+"/join-requests/"+request.text("id")+"/review",buildJsonObject{put("decision",decision);put("expected_version",request["version"]!!.jsonPrimitive.int);put("reason",reason)});notice="Review recorded. Refresh recruitment."}}){Text(decision)}}
  }}}}
  invites.forEach{element->val invite=element.jsonObject;Card{Column(Modifier.padding(10.dp)){Text(invite.text("recipientEmail"));Text("Delivery: "+invite.text("deliveryStatus"));TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/organizations/"+organization+"/invitations/"+invite.text("id")+"/resend",buildJsonObject{put("reason",reason)});notice="Replacement invitation created; previous invitation revoked."}}){Text("Resend")}}}}
  TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/organizations/"+organization+"/units",buildJsonObject{put("name",title)});notice="Unit created."}}){Text("Create workspace unit")}
  TextButton(enabled=!busy,onClick={perform{proof=call(HttpMethod.Post,"/api/organizations/"+organization+"/domains",buildJsonObject{put("domain",domain);put("reason",reason)}).jsonObject}}){Text("Start domain verification")}
  proof?.let{p->Text("DNS TXT: "+p.text("record"));OutlinedTextField(p.text("value"),{},readOnly=true);TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/organizations/"+organization+"/domains/"+p.text("id")+"/verify",buildJsonObject{});notice="DNS proof verified. Institution admission remains a separate review."}}){Text("Verify DNS proof")}}
  listOf(true,false).forEach{open->TextButton(enabled=!busy,onClick={perform{val current=call(HttpMethod.Get,"/api/organizations/"+organization,null).jsonObject;call(HttpMethod.Put,"/api/organizations/"+organization+"/recruitment",buildJsonObject{put("open",open);put("expected_version",current["version"]!!.jsonPrimitive.int);put("reason",reason)});notice="Recruitment visibility updated."}}){Text(if(open)"Open public join requests" else "Close public join requests")}}
 }
 if(platform){
  Text("Platform administration",style=MaterialTheme.typography.titleLarge)
  Button(enabled=!busy,onClick={perform{overview=call(HttpMethod.Get,"/api/platform/overview",null).jsonObject;organizations=call(HttpMethod.Get,"/api/platform/organizations",null).jsonArray;applications=call(HttpMethod.Get,"/api/platform/partnerships",null).jsonArray}}){Text("Refresh platform overview")}
  overview?.let{data->Text("Pending partnerships: "+data.text("pending_partnerships"));val readiness=data["provider_readiness"]?.jsonObject;Text("Identity: "+readiness?.text("identity"));Text("Email: "+readiness?.text("email"));Text("Commercial entitlements become available in Stage 4.")}
  OutlinedTextField(slug,{slug=it},label={Text("Workspace slug")})
  OutlinedTextField(provider,{provider=it},label={Text("Approved Firebase provider ID")})
  Row{Checkbox(mfa,{mfa=it});Text("Require MFA")}
  Button(enabled=!busy,onClick={perform{selected=call(HttpMethod.Post,"/api/platform/organizations",buildJsonObject{put("slug",slug);put("display_name",title);put("allowed_providers",JsonArray(listOf(JsonPrimitive(provider))));put("require_mfa",mfa);put("reason",reason)}).jsonObject;notice="Workspace provisioned. Activation and verification are separate steps."}}){Text("Provision institution")}
  organizations.forEach{element->val org=element.jsonObject;TextButton(onClick={selected=org}){Text(org.text("displayName")+" / "+org.text("status"))}}
  selected?.let{org->
   Text("Selected institution: "+org.text("displayName"))
   listOf("activate","suspend","offboard").forEach{action->TextButton(enabled=!busy,onClick={perform{selected=call(HttpMethod.Post,"/api/platform/organizations/"+org.text("id")+"/"+action,buildJsonObject{put("expected_version",org["version"]!!.jsonPrimitive.int);put("reason",reason)}).jsonObject}}){Text(action)}}
   OutlinedTextField(evidence,{evidence=it},label={Text("Reviewed verification evidence reference")})
   TextButton(enabled=!busy,onClick={perform{selected=call(HttpMethod.Post,"/api/platform/organizations/"+org.text("id")+"/verification",buildJsonObject{put("expected_version",org["version"]!!.jsonPrimitive.int);put("reason",reason);put("evidence_reference",evidence)}).jsonObject}}){Text("Record institutional verification")}
   TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Put,"/api/platform/organizations/"+org.text("id")+"/policy",buildJsonObject{put("expected_version",org["version"]!!.jsonPrimitive.int);put("reason",reason);put("allowed_providers",JsonArray(listOf(JsonPrimitive(provider))));put("require_mfa",mfa)});selected=null;notice="Policy updated. Refresh before another change."}}){Text("Update authentication policy")}
   applications.filter{it.jsonObject.text("status")=="PENDING"}.forEach{item->val application=item.jsonObject;Text(application.text("name"));listOf("APPROVED","DECLINED").forEach{decision->TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Post,"/api/platform/partnerships/"+application.text("id")+"/review",buildJsonObject{put("status",decision);put("reason",reason);put("organization_id",org.text("id"))});notice="Partnership review recorded."}}){Text(decision)}}}
   OutlinedTextField(userId,{userId=it},label={Text("Reviewed internal user ID")})
   TextButton(enabled=!busy,onClick={perform{support=call(HttpMethod.Post,"/api/platform/organizations/"+org.text("id")+"/support-grants",buildJsonObject{put("user_id",userId.toInt());put("expires_in_minutes",30);put("reason",reason)}).jsonObject}}){Text("Grant 30-minute configuration support")}
  }
  support?.let{grant->Text("Support expires: "+grant.text("expiresAt"));TextButton(enabled=!busy,onClick={perform{call(HttpMethod.Delete,"/api/platform/support-grants/"+grant.text("id"),buildJsonObject{put("reason",reason)});support=null}}){Text("Revoke support grant")}}
 }
 }
}
