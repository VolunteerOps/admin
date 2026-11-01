package com.example.admin.features.auth
import kotlinx.serialization.json.*
/** Collect bounded API pages without combining exports from different signed-in identities. */
suspend fun collectPersonalExport(fetch:suspend(String?)->JsonObject):JsonObject {
 var cursor:String?=null
 var first:JsonObject?=null
 val records=mutableListOf<JsonElement>()
 val seen=mutableSetOf<String>()
 do {
  check(seen.size<100){"Export exceeds the interactive limit; use a reviewed server-side export."}
  val page=fetch(cursor)
  if(first==null)first=page else check(first["user_id"]==page["user_id"]){"Account changed during export."}
  check(page["user_id"]?.jsonPrimitive?.intOrNull!=null){"Missing export owner."}
  records.addAll(page["consents"]!!.jsonArray)
  cursor=page["consents_next_cursor"]?.jsonPrimitive?.contentOrNull
  if(cursor!=null)check(seen.add(cursor)){"Repeated export cursor."}
 } while(cursor!=null)
 return JsonObject(first!!.toMutableMap().apply {put("consents",JsonArray(records));put("consents_next_cursor",JsonNull)})
}
