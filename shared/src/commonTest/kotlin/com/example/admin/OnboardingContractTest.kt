package com.example.admin
import com.example.admin.features.auth.*
import kotlin.test.*
class OnboardingContractTest {
 @Test fun rejectsUnapprovedInactiveAndVolunteerAdminAccess() {
  assertFalse(Membership("1","ADMIN","org",true,false).canEnter(true))
  assertFalse(Membership("1","ADMIN","org",false,true).canEnter(true))
  assertFalse(Membership("1","VOLUNTEER","org",true,true).canEnter(true))
  assertFalse(Membership("1","ADMIN",null,true,true).canEnter(true))
  assertTrue(Membership("1","COORDINATOR","org",true,true).canEnter(true))
 }
 @Test fun draftRoundTripAndMalformedInput() {
  val draft=SetupDraft(step=2,location="North | Valley",organization="50% response", choices=setOf("Medical","Logistics"))
  assertEquals(draft,SetupDraft.decode(draft.encode()))
  assertEquals(SetupDraft(),SetupDraft.decode("broken"))
 }
 @Test fun emailAndPasswordValidation() {
  assertTrue(validEmail("person@example.org")); assertFalse(validEmail("person @example.org")); assertFalse(validEmail("bad"))
  assertFalse(validNewPassword("short")); assertFalse(validNewPassword("        "));assertTrue(validNewPassword("long passphrase"))
 }
 @Test fun extractsRecoveryCodeWithoutPassingTheWholeLink() {
  assertEquals("abc-123", recoveryCode("https://example.org/action?mode=resetPassword&oobCode=abc%2D123&apiKey=public"))
  assertEquals("", recoveryCode("https://example.org/action?mode=resetPassword"))
  assertEquals("opaque-code", recoveryCode(" opaque-code "))
 }
}
