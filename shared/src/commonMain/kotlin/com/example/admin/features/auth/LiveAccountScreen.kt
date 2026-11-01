package com.example.admin.features.auth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
private fun JsonObject.value(name:String)=get(name)?.jsonPrimitive?.contentOrNull.orEmpty()
@Composable fun LiveAccountScreen(onSessionEnded:()->Unit = {}) {
 var section by remember {mutableStateOf("Profile")}
 val scope=rememberCoroutineScope()
 var signedIn by remember {mutableStateOf(false)}
 var busy by remember {mutableStateOf(false)}
 var notice by remember {mutableStateOf("")}
 var email by remember {mutableStateOf("")};var password by remember {mutableStateOf("")};var name by remember {mutableStateOf("")}
 var region by remember {mutableStateOf("")};var biography by remember {mutableStateOf("")};var skills by remember {mutableStateOf("")}
 var home by remember {mutableStateOf<JsonObject?>(null)}
 var contexts by remember {mutableStateOf<JsonObject?>(null)}
 var invitations by remember {mutableStateOf<JsonArray>(JsonArray(emptyList()))}
 var workspaces by remember {mutableStateOf<JsonArray>(JsonArray(emptyList()))}
 var directoryCursor by remember {mutableStateOf<String?>(null)}
 var directoryRegion by remember {mutableStateOf("")}
 var directorySkill by remember {mutableStateOf("")}
 var privatePhone by remember {mutableStateOf("")}
 var privateContact by remember {mutableStateOf("")}
 var directory by remember {mutableStateOf<JsonArray>(JsonArray(emptyList()))}
 var organization by remember {mutableStateOf("")}
 var recipient by remember {mutableStateOf("")}
 var reason by remember {mutableStateOf("")}
 var exportContent by remember {mutableStateOf("")}
 var previewContent by remember {mutableStateOf("")}
 var sharePreviewCode by remember {mutableStateOf("")}
 var shareCode by remember {mutableStateOf("")}
 var shareId by remember {mutableStateOf("")}
 var institutionProvider by remember {mutableStateOf("")}
 var otp by remember {mutableStateOf("")}
 var enrollmentSecret by remember {mutableStateOf("")}
 var recovery by remember {mutableStateOf("")}
 var discoverable by remember {mutableStateOf(false)};var publicPreview by remember {mutableStateOf(false)};var showAvailability by remember {mutableStateOf(false)}
 val api=remember {LiveApiTransport(FirebaseTokenProvider {
  when(val result=IdentityServices.firebase.refreshIdentity()){is IdentityResult.SignedIn->result.identity.idToken;else->null}
 })}
 DisposableEffect(api){onDispose{api.close()}}
 suspend fun call(method:HttpMethod,path:String,body:JsonElement?=null,query:Map<String,String> = emptyMap())=api.execute(ApiEndpoint(method,path,query){it},body)
 fun clearAccount(){signedIn=false;home=null;contexts=null;invitations=JsonArray(emptyList());workspaces=JsonArray(emptyList());directory=JsonArray(emptyList());organization="";shareCode="";shareId="";privatePhone="";privateContact="";directoryCursor=null;exportContent="";previewContent="";sharePreviewCode="";recipient="";reason="";otp="";enrollmentSecret="";password="";name="";biography="";skills="";region="";onSessionEnded()}
 suspend fun refresh(){
  val incoming=call(HttpMethod.Get,"/api/auth/me").jsonObject
  if(contexts?.get("user")?.jsonObject?.value("id")!=incoming["user"]?.jsonObject?.value("id")){
   home=null;invitations=JsonArray(emptyList());workspaces=JsonArray(emptyList());directory=JsonArray(emptyList());organization="";shareCode="";shareId="";exportContent="";previewContent=""
  }
  contexts=incoming
  home=call(HttpMethod.Get,"/api/me/home").jsonObject
  invitations=call(HttpMethod.Get,"/api/me/invitations").jsonArray
  workspaces=call(HttpMethod.Get,"/api/me/workspaces").jsonObject["items"]!!.jsonArray
  val profile=home?.get("profile") as? JsonObject
  name=profile?.value("displayName")?:contexts?.get("user")?.jsonObject?.value("fullName").orEmpty()
  region=profile?.value("region").orEmpty();biography=profile?.value("biography").orEmpty()
  privatePhone=(profile?.get("privateDetails") as? JsonObject)?.value("phone").orEmpty();privateContact=(profile?.get("privateDetails") as? JsonObject)?.value("emergency_contact").orEmpty()
  skills=profile?.get("skills")?.jsonArray?.joinToString(", "){it.jsonPrimitive.content}.orEmpty()
  val preferences=home?.get("directory") as? JsonObject
  discoverable=preferences?.get("discoverable")?.jsonPrimitive?.booleanOrNull==true
  publicPreview=preferences?.get("publicPreview")?.jsonPrimitive?.booleanOrNull==true
  showAvailability=preferences?.get("showAvailability")?.jsonPrimitive?.booleanOrNull==true
  if(contexts?.get("memberships")?.jsonArray?.none{it.jsonObject.value("organization_id")==organization && it.jsonObject.value("access")=="ALLOWED"}!=false){organization="";directory=JsonArray(emptyList())}
 }
 fun runAction(action:suspend()->Unit){if(!busy){busy=true;notice="";scope.launch {try{action()}catch(e:CancellationException){throw e}catch(e:ApiFailure){if(e.status==401){clearAccount()};notice=when(e.code){"SSO_REQUIRED"->"Use your institution's approved sign-in method.";"MFA_REQUIRED","RECENT_AUTH_REQUIRED"->"Sign in again with your institution's required MFA before continuing.";"STALE_VERSION"->"This record changed. Refresh before trying again.";else->"The request could not be completed ("+e.code+")."}}catch(_:Exception){notice="Unable to connect. Your changes have not been confirmed."}finally{busy=false}}}}
 suspend fun resolve(result:IdentityResult){when(result){is IdentityResult.MfaEnrollmentReady->{enrollmentSecret=result.secretKey;notice="Add this setup key to your authenticator app, then enter its current six-digit code."};is IdentityResult.Error->notice=result.message;IdentityResult.Completed->notice="Request completed. Check your email if applicable.";is IdentityResult.SignedIn->{password="";if(!result.identity.emailVerified){notice="Verify your email, then select Continue after verification."}else{
  call(HttpMethod.Post,"/api/auth/firebase/bootstrap",buildJsonObject{put("full_name",name.trim().ifBlank{"ResQ Volunteer"})});signedIn=true;refresh()
 }}}}
 LaunchedEffect(Unit){runAction{resolve(IdentityServices.firebase.refreshIdentity())}}
 Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("My account",style=MaterialTheme.typography.headlineLarge)
 Text("Workspace administration",style=MaterialTheme.typography.titleLarge)
 if(notice.isNotBlank()) Text(notice)
 if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
 if(!signedIn){
  OutlinedTextField(name,{name=it},label={Text("Chosen display name")})
  OutlinedTextField(email,{email=it},label={Text("Email")})
  OutlinedTextField(password,{password=it},label={Text("Password")},visualTransformation=PasswordVisualTransformation())
  Button(enabled=!busy,onClick={runAction{val credential=password;clearAccount();IdentityServices.firebase.signOut();resolve(IdentityServices.firebase.signIn(email,credential))}}){Text("Sign in")}
  Text("An approved membership is required for workspace administration. Personal signup never grants coordinator access.")
  Button(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.refreshIdentity())}}){Text("Continue after verification")}
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.sendEmailVerification())}}){Text("Resend verification email")}
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.sendPasswordReset(email))}}){Text("Recover password")}
  OutlinedTextField(recovery,{recovery=it},label={Text("Recovery code from email")})
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.confirmPasswordReset(recoveryCode(recovery),password));recovery="";password=""}}){Text("Set new password")}
  
  OutlinedTextField(institutionProvider,{institutionProvider=it},label={Text("Institution provider ID supplied by your administrator")})
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.enterpriseSignIn(institutionProvider))}}){Text("Institution sign-in")}
  OutlinedTextField(otp,{otp=it},label={Text("Authenticator code")})
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.completeMfa(otp));otp=""}}){Text("Complete MFA sign-in")}
  OutlinedTextField(sharePreviewCode,{sharePreviewCode=it},label={Text("Profile share code")})
  TextButton(enabled=!busy,onClick={runAction{val preview=call(HttpMethod.Post,"/api/profile-shares/preview",buildJsonObject{put("token",sharePreviewCode)}).jsonObject;previewContent=preview.value("display_name")+"\n"+preview.value("biography")+"\n"+preview.value("region")}}){Text("Preview shared profile")}
  if(previewContent.isNotBlank())Text(previewContent)
 }else{
  Row {TextButton(enabled=!busy,onClick={runAction{refresh()}}){Text("Refresh")};TextButton(enabled=!busy,onClick={runAction{IdentityServices.firebase.signOut();clearAccount()}}){Text("Sign out / switch account")}}
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Profile","Privacy","Workspaces","Administration","Account security").forEach{label->FilterChip(selected=section==label,onClick={section=label},label={Text(label)})}}
  if(section=="Profile") {
  Text("Personal profile",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(name,{name=it},label={Text("Chosen display name")})
  OutlinedTextField(biography,{biography=it},label={Text("Biography")})
  OutlinedTextField(region,{region=it},label={Text("Broad region (no precise address)")})
  OutlinedTextField(skills,{skills=it},label={Text("Self-declared skills, separated by commas")})
  OutlinedTextField(privatePhone,{privatePhone=it},label={Text("Private phone - visible only to you")})
  OutlinedTextField(privateContact,{privateContact=it},label={Text("Private emergency contact - visible only to you")})
  Button(enabled=!busy,onClick={runAction{
   val profile=home?.get("profile") as? JsonObject
   call(HttpMethod.Put,"/api/me/profile",buildJsonObject{put("expected_version",profile?.get("version")?.jsonPrimitive?.int?:0);put("display_name",name);put("biography",biography);put("region",region);put("skills",JsonArray(skills.split(',').map{it.trim()}.filter{it.isNotEmpty()}.map{JsonPrimitive(it)}));put("private_details",buildJsonObject{(profile?.get("privateDetails") as? JsonObject)?.forEach{(k,v)->put(k,v)};put("phone",privatePhone);put("emergency_contact",privateContact)})});refresh()
  }}){Text("Save profile")}
  }
  if(section=="Privacy") {
  Text("Discoverability and sharing",style=MaterialTheme.typography.titleLarge)
  Row{Checkbox(discoverable,{discoverable=it});Text("Allow approved coordinators to discover my profile")}
  Row{Checkbox(publicPreview,{publicPreview=it});Text("Allow public preview through a share code")}
  Row{Checkbox(showAvailability,{showAvailability=it});Text("Include my availability preference")}
  Button(enabled=!busy,onClick={runAction{val prefs=home?.get("directory") as? JsonObject?:error("PROFILE_REQUIRED");call(HttpMethod.Put,"/api/me/directory",buildJsonObject{put("expected_version",prefs["version"]!!.jsonPrimitive.int);put("discoverable",discoverable);put("public_preview",publicPreview);put("show_availability",showAvailability)});refresh()}}){Text("Save privacy preferences")}
  TextButton(enabled=!busy,onClick={runAction{val shared=call(HttpMethod.Post,"/api/me/profile-shares",buildJsonObject{}).jsonObject;shareCode=shared.value("token");shareId=shared.value("id")}}){Text("Create seven-day profile share code")}
  if(shareCode.isNotBlank()){OutlinedTextField(shareCode,{},readOnly=true,label={Text("Share code ? permitted profile fields only")});TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Delete,"/api/me/profile-shares/"+shareId);shareCode="";shareId=""}}){Text("Revoke this share")}}
  }
  if(section=="Workspaces") {
  Text("Invitation inbox",style=MaterialTheme.typography.titleLarge)
  if(invitations.isEmpty()) Text("No invitations. You can use your personal account independently.")
  invitations.forEach{element->val invite=element.jsonObject;Card{Column(Modifier.padding(12.dp)){
   Text((invite["organization"] as? JsonObject)?.value("displayName")?:"Inviting workspace");Text("Role: "+invite.value("role"))
   val pending=invite["acceptedAt"]==JsonNull&&invite["revokedAt"]==JsonNull&&invite["declinedAt"]==JsonNull
   if(pending){Row{Button(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/me/invitations/"+invite.value("id")+"/accept",buildJsonObject{});refresh()}}){Text("Accept")};TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/me/invitations/"+invite.value("id")+"/decline",buildJsonObject{});refresh()}}){Text("Decline")}}
   TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/me/recruitment-blocks",buildJsonObject{put("organization_id",invite.value("organizationId"))});refresh()}}){Text("Block invitations from this workspace")}}
   else Text(if(invite["acceptedAt"]!=JsonNull)"Accepted" else if(invite["declinedAt"]!=JsonNull)"Declined" else "Revoked")
  }}}
  Text("Workspaces",style=MaterialTheme.typography.titleLarge)
  contexts?.get("memberships")?.jsonArray?.forEach{element->val member=element.jsonObject
   TextButton(enabled=!busy&&member.value("access")=="ALLOWED",onClick={organization=member.value("organization_id");directory=JsonArray(emptyList())}){Text(member.value("organization_name")+" ? "+member.value("role")+" / "+member.value("status"))}
  }
  OutlinedTextField(reason,{reason=it},label={Text("Reason or introduction (at least 10 characters)")})
  workspaces.forEach{element->val workspace=element.jsonObject;TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/me/join-requests",buildJsonObject{put("organization_id",workspace.value("id"));put("message",reason)});notice="Join request sent for review. This does not grant membership."}}){Text("Request to join "+workspace.value("displayName"))}}
  if(organization.isNotBlank()){
   Text("Selected workspace: "+organization)
   OutlinedTextField(directoryRegion,{directoryRegion=it},label={Text("Filter by broad region")})
   OutlinedTextField(directorySkill,{directorySkill=it},label={Text("Filter by self-declared skill")})
   TextButton(enabled=!busy,onClick={runAction{val result=call(HttpMethod.Get,"/api/organizations/"+organization+"/directory",query=buildMap{if(directoryRegion.isNotBlank())put("region",directoryRegion);if(directorySkill.isNotBlank())put("skill",directorySkill)}).jsonObject;directory=result["items"]!!.jsonArray;directoryCursor=result["next_cursor"]?.jsonPrimitive?.contentOrNull}}){Text("Discover volunteers")}
   if(directoryCursor!=null)TextButton(enabled=!busy,onClick={runAction{val result=call(HttpMethod.Get,"/api/organizations/"+organization+"/directory",query=buildMap{put("after",directoryCursor!!);if(directoryRegion.isNotBlank())put("region",directoryRegion);if(directorySkill.isNotBlank())put("skill",directorySkill)}).jsonObject;directory=JsonArray(directory+result["items"]!!.jsonArray);directoryCursor=result["next_cursor"]?.jsonPrimitive?.contentOrNull}}){Text("Load more volunteers")}
   directory.forEach{element->val person=element.jsonObject;Card{Column(Modifier.padding(12.dp)){Text(person.value("display_name"));Text(person.value("biography"));Text(person.value("region"));TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/organizations/"+organization+"/directory/"+person.value("id")+"/invite",buildJsonObject{put("reason",reason)});notice="Invitation created. Check delivery status in the workspace inbox."}}){Text("Invite volunteer")}}}}
   OutlinedTextField(recipient,{recipient=it},label={Text("Invitation recipient email")})
   Button(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/organizations/"+organization+"/invitations",buildJsonObject{put("recipient_email",recipient);put("role","VOLUNTEER");put("reason",reason)});notice="Invitation created. Membership requires recipient acceptance."}}){Text("Invite by email")}
   TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/organizations/"+organization+"/leave",buildJsonObject{});organization="";directory=JsonArray(emptyList());refresh()}}){Text("Leave selected workspace")}
  }
  }
  if(section=="Administration") EnterpriseAccountPanel(organization,contexts?.value("platform_access")=="ALLOWED",{ method,path,body->call(method,path,body) })
  if(section=="Account security") {
  Text("Account controls",style=MaterialTheme.typography.titleLarge)
  Text("Sign in with your email and password. Workspace access is managed separately.")
  OutlinedTextField(email,{email=it},label={Text("New email address")})
  TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.changeEmail(email))}}){Text("Verify new email address")}
  listOf(SocialProvider.Google).forEach{provider->TextButton(enabled=!busy,onClick={runAction{resolve(IdentityServices.firebase.linkProvider(provider))}}){Text("Link "+provider.name+" to this account")}}

  TextButton(enabled=!busy,onClick={runAction{exportContent="";exportContent=collectPersonalExport{cursor->call(HttpMethod.Get,"/api/me/export",query=cursor?.let{mapOf("after" to it)}?:emptyMap()).jsonObject}.toString()}}){Text("Export personal profile and consents")}
  if(exportContent.isNotBlank())OutlinedTextField(exportContent,{},readOnly=true,label={Text("Private export - copy and save securely")})
  Text("Deleting your account is separate from leaving a workspace or cancelling a store subscription.")
  TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/auth/sessions/revoke",buildJsonObject{});IdentityServices.firebase.signOut();clearAccount();notice="Sessions revoked locally; provider revocation is queued."}}){Text("Revoke all sessions")}
  TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Post,"/api/me/deletion-request",buildJsonObject{});refresh();notice="Deletion requested. Privacy review and retained-record rules apply."}}){Text("Request account deletion")}
  Text("Deletion status: "+((home?.get("deletion") as? JsonObject)?.value("status")?:"No request"))
  TextButton(enabled=!busy,onClick={runAction{call(HttpMethod.Delete,"/api/me/deletion-request");refresh()}}){Text("Cancel deletion request")}
 }
 }
 }
}
