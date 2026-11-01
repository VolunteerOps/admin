package com.example.admin.features.auth
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
class PersonalExportTest {
 private fun page(owner:Int,cursor:String?,record:Int)=buildJsonObject{put("user_id",owner);put("consents_next_cursor",cursor?.let(::JsonPrimitive)?:JsonNull);put("consents",JsonArray(listOf(JsonPrimitive(record))))}
 @Test fun collectsAllConsentPages()=runTest {val result=collectPersonalExport{cursor->if(cursor==null)page(1,"next",1)else page(1,null,2)};assertEquals(2,result["consents"]!!.jsonArray.size)}
 @Test fun refusesAccountSwitchDuringExport()=runTest {assertFailsWith<IllegalStateException>{collectPersonalExport{cursor->if(cursor==null)page(1,"next",1)else page(2,null,2)}}}
 @Test fun refusesRepeatedCursors()=runTest {assertFailsWith<IllegalStateException>{collectPersonalExport{page(1,"next",1)}}}
}
