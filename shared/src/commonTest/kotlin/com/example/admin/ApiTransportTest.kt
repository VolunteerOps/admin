package com.example.admin
import com.example.admin.core.network.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
class ApiTransportTest {
 @Test fun csvExportPreservesTextButStillChecksAuthorization() = runTest {
   var denied=false
   val client=HttpClient(MockEngine {
     if(denied)respond("{\"error\":\"CAPABILITY_REQUIRED\"}",HttpStatusCode.Forbidden)
     else respond("metric,value\r\nmission_count,2\r\n",HttpStatusCode.OK,headersOf("Content-Type","text/csv"))
   })
   val api=LiveApiTransport(FirebaseTokenProvider{"fresh-token"},"https://stage.example.org",client)
   val endpoint=ApiEndpoint(HttpMethod.Get,"/api/organizations/test/regions/test/operations/metrics/export",rawText=true){it.jsonPrimitive.content}
   try {
     assertEquals("metric,value\r\nmission_count,2\r\n",api.execute(endpoint))
     denied=true
     assertEquals("CAPABILITY_REQUIRED",assertFailsWith<ApiFailure>{api.execute(endpoint)}.code)
   } finally {api.close()}
 }
 @Test fun liveFailureNeverBecomesPracticeData() = runTest {
   val client = HttpClient(MockEngine { request ->
     assertEquals("Bearer fresh-token", request.headers["Authorization"])
     assertEquals("VolunteerOps", request.headers["X-Requested-With"])
     respond("{\"error\":\"STORAGE_UNAVAILABLE\"}", HttpStatusCode.ServiceUnavailable, headersOf("X-Request-ID", "request-123"))
   })
   val api = LiveApiTransport(FirebaseTokenProvider { "fresh-token" }, "https://stage.example.org", client)
   try {
     val error = assertFailsWith<ApiFailure> { LiveFoundationRepository(api).health() }
     assertEquals(503,error.status); assertEquals("STORAGE_UNAVAILABLE",error.code)
     assertEquals("request-123",error.requestId)
   } finally { api.close() }
 }
 @Test fun repositoryModeIsExplicit() = runTest {
   assertEquals(RepositoryMode.TEST, TestFoundationRepository(true).mode)
   assertTrue(TestFoundationRepository(true).health())
 }
 @Test fun transportRejectsCredentialAndPathOrigins() {
   for (origin in listOf("https://user:pass@example.org", "https://example.org/path", "http://example.org")) {
     val client = HttpClient(MockEngine { error("Unexpected network") })
     try { assertFailsWith<IllegalArgumentException> { LiveApiTransport(FirebaseTokenProvider { null },origin,client) } } finally { client.close() }
   }
 }
}
