package com.example.admin.features.auth
import com.example.admin.core.network.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.*
class BackendIdentityGateway : DatabaseAccessGateway {
 private suspend fun bootstrap(token:String):JsonObject {
  val api=LiveApiTransport(FirebaseTokenProvider{token})
  try {return api.execute(ApiEndpoint(HttpMethod.Post,"/api/auth/firebase/bootstrap"){it.jsonObject},buildJsonObject {put("full_name","ResQ Volunteer")})}
  finally {api.close()}
 }
 override suspend fun membership(idToken:String):Membership? {
  val data=bootstrap(idToken); val user=data["user"]!!.jsonObject
  val member=data["memberships"]?.jsonArray?.firstOrNull{it.jsonObject["access"]?.jsonPrimitive?.content=="ALLOWED"}?.jsonObject
  val api=LiveApiTransport(FirebaseTokenProvider{idToken})
  val complete=try { api.execute(ApiEndpoint(HttpMethod.Get,"/api/me/home"){it.jsonObject})["profile"] is JsonObject } finally {api.close()}
  return Membership(user["id"]!!.jsonPrimitive.content,member?.get("role")?.jsonPrimitive?.content?:"VOLUNTEER",member?.get("organization_id")?.jsonPrimitive?.content,true,member!=null,complete)
 }
 override suspend fun saveSetup(idToken:String,setup:SetupDraft):Membership? {
  val access=membership(idToken)?:return null
  val api=LiveApiTransport(FirebaseTokenProvider{idToken})
  try {
   val home=api.execute(ApiEndpoint(HttpMethod.Get,"/api/me/home"){it.jsonObject})
   val profile=home["profile"] as? JsonObject
   api.execute(ApiEndpoint(HttpMethod.Put,"/api/me/profile"){it},buildJsonObject {
    put("expected_version",profile?.get("version")?.jsonPrimitive?.int?:0)
    put("display_name",setup.displayName.take(120).ifBlank { profile?.get("displayName")?.jsonPrimitive?.content?:"ResQ member" })
    put("region",setup.location.take(80));put("skills",JsonArray(setup.choices.take(30).map{JsonPrimitive(it.take(60))}))
    put("availability",when(setup.availability){"Weekends"->"WEEKENDS";"Weekdays"->"WEEKDAYS";else->"FLEXIBLE"})
    put("private_details",profile?.get("privateDetails")?:buildJsonObject{})
   })
   api.execute(ApiEndpoint(HttpMethod.Post,"/api/me/consents"){it},buildJsonObject{put("purpose","UPDATES");put("policy_version","2026-10-01");put("granted",setup.updates)})
  } finally {api.close()}
  return access
 }
}
