package com.example.admin.features.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
class PasswordIdentityProviderTest {
 @Test fun verifiedPasswordIdentityRefreshAndLogout()=runTest {
  val paths=mutableListOf<String>()
  val client=HttpClient(MockEngine{request->
   paths.add(request.url.encodedPath)
   val payload=when(request.url.encodedPath){
    "/api/auth/config"->"{\"api_key\":\"public_test_api_key_123456789\"}"
    "/v1/accounts:signInWithPassword"->"{\"idToken\":\"first-token\",\"refreshToken\":\"refresh-secret\"}"
    "/v1/accounts:lookup"->"{\"users\":[{\"localId\":\"immutable-uid\",\"email\":\"person@example.org\",\"emailVerified\":true}]}"
    "/v1/token"->"{\"id_token\":\"refreshed-token\",\"refresh_token\":\"rotated-secret\"}"
    else->error("Unexpected identity request")
   }
   respond(payload,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
  })
  val provider=PasswordIdentityProvider(client)
  val signed=provider.signIn("person@example.org","private-password") as IdentityResult.SignedIn
  assertEquals("immutable-uid",signed.identity.uid);assertTrue(signed.identity.emailVerified)
  val refreshed=provider.refreshIdentity() as IdentityResult.SignedIn
  assertEquals("refreshed-token",refreshed.identity.idToken)
  provider.signOut();val before=paths.size
  assertTrue(provider.refreshIdentity() is IdentityResult.Error);assertEquals(before,paths.size)
  assertFalse(Membership("1","VOLUNTEER",null,true,true).canEnter(true))
  assertTrue(Membership("1","VOLUNTEER",null,true,true).canEnter(false))
  assertTrue(Membership("1","ORG_ADMIN","workspace",true,true).canEnter(true))
  client.close()
 }
}
