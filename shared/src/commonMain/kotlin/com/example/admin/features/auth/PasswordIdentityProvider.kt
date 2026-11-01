package com.example.admin.features.auth
import com.example.admin.core.network.BackendConnection
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
/** Firebase REST adapter for password flows on every target. Session is memory-only.
 * No custom app tokens and no unencrypted refresh token persisted to disk. */
class PasswordIdentityProvider(private val client:HttpClient=HttpClient {followRedirects=false;install(HttpTimeout){requestTimeoutMillis=15000}}):FirebaseIdentityProvider {
 private var idToken:String?=null
 private var refreshToken:String?=null
 private val mutex=Mutex()
 private suspend fun request(operation:String,body:JsonObject):JsonObject {
  val config=client.get(BackendConnection.apiOrigin+"/api/auth/config?app=ops")
  val publicConfig=Json.parseToJsonElement(config.bodyAsText()).jsonObject
  val key=publicConfig["api_key"]?.jsonPrimitive?.contentOrNull?:error("IDENTITY_NOT_CONFIGURED")
  require(Regex("^[A-Za-z0-9_-]{20,200}$").matches(key))
  val response=client.post("https://identitytoolkit.googleapis.com/v1/accounts:"+operation+"?key="+key){header("Content-Type","application/json");setBody(body.toString())}
  val result=Json.parseToJsonElement(response.bodyAsText()).jsonObject
  if(response.status.value !in 200..299 || result.containsKey("mfaPendingCredential")) error("IDENTITY_REQUIRES_CONFIGURED_PROVIDER_OR_STEP_UP")
  result["idToken"]?.jsonPrimitive?.contentOrNull?.let{idToken=it}
  result["refreshToken"]?.jsonPrimitive?.contentOrNull?.let{refreshToken=it}
  return result
 }
 private suspend fun identity():IdentityResult {
  val token=idToken?:return IdentityResult.Error("Sign in to continue.")
  val account=request("lookup",buildJsonObject{put("idToken",token)})["users"]!!.jsonArray.first().jsonObject
  return IdentityResult.SignedIn(FirebaseIdentity(account["localId"]!!.jsonPrimitive.content,account["email"]!!.jsonPrimitive.content,account["emailVerified"]?.jsonPrimitive?.booleanOrNull==true,idToken!!))
 }
 private suspend fun attempt(block:suspend()->IdentityResult):IdentityResult=mutex.withLock {
  try{block()}catch(e:CancellationException){throw e}catch(_:Exception){IdentityResult.Error("Sign-in could not be completed. Check configuration, credentials, or use your institution's approved sign-in and MFA method.")}
 }
 override suspend fun signIn(email:String,password:String)=attempt{idToken=null;refreshToken=null;request("signInWithPassword",buildJsonObject{put("email",email);put("password",password);put("returnSecureToken",true)});identity()}
 override suspend fun signUp(name:String,email:String,password:String)=attempt{request("signUp",buildJsonObject{put("email",email);put("password",password);put("returnSecureToken",true)});request("update",buildJsonObject{put("idToken",idToken);put("displayName",name)});identity()}
 override suspend fun signInWith(provider:SocialProvider):IdentityResult=IdentityResult.Error("This provider requires a configured platform adapter. Password access cannot bypass workspace SSO policy.")
 override suspend fun sendPasswordReset(email:String)=attempt{request("sendOobCode",buildJsonObject{put("requestType","PASSWORD_RESET");put("email",email)});IdentityResult.Completed}
 override suspend fun confirmPasswordReset(actionCode:String,password:String)=attempt{request("resetPassword",buildJsonObject{put("oobCode",actionCode);put("newPassword",password)});IdentityResult.Completed}
 override suspend fun sendEmailVerification()=attempt{request("sendOobCode",buildJsonObject{put("requestType","VERIFY_EMAIL");put("idToken",idToken?:error("SIGN_IN_REQUIRED"))});IdentityResult.Completed}
 override suspend fun refreshIdentity()=attempt{
  val refresh=refreshToken?:return@attempt IdentityResult.Error("Sign in to continue.")
  val config=Json.parseToJsonElement(client.get(BackendConnection.apiOrigin+"/api/auth/config?app=ops").bodyAsText()).jsonObject
  val key=config["api_key"]?.jsonPrimitive?.contentOrNull?:error("IDENTITY_NOT_CONFIGURED")
  require(Regex("^[A-Za-z0-9_-]{20,200}$").matches(key))
  val response=client.post("https://securetoken.googleapis.com/v1/token?key="+key){header("Content-Type","application/json");setBody(buildJsonObject{put("grant_type","refresh_token");put("refresh_token",refresh)}.toString())}
  if(response.status.value !in 200..299){idToken=null;refreshToken=null;error("SESSION_EXPIRED")}
  val data=Json.parseToJsonElement(response.bodyAsText()).jsonObject;idToken=data["id_token"]!!.jsonPrimitive.content;refreshToken=data["refresh_token"]!!.jsonPrimitive.content;identity()
 }
 override suspend fun signOut(){mutex.withLock{idToken=null;refreshToken=null}}
}
