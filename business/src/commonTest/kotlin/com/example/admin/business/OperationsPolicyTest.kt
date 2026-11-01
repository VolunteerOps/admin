package com.example.admin.business
import kotlin.test.*
class OperationsPolicyTest {
 @Test fun admissionNeverComesFromALegacyRole(){
   assertFalse(OperationsPolicy.canCoordinate("SUPER_ADMIN",true,true,"org"))
   assertFalse(OperationsPolicy.canCoordinate("ORG_ADMIN",true,false,"org"))
   assertFalse(OperationsPolicy.canCoordinate("ORG_ADMIN",false,true,"org"))
   assertTrue(OperationsPolicy.canCoordinate("COORDINATOR",true,true,"org"))
 }
 @Test fun terminalMissionsCannotBeReopened(){
   assertTrue(OperationsPolicy.missionTransitions("COMPLETED").isEmpty())
   assertTrue(OperationsPolicy.missionTransitions("CANCELLED").isEmpty())
   assertEquals(listOf("STAGED","CANCELLED"),OperationsPolicy.missionTransitions("DRAFT"))
 }
 @Test fun capacityAndInventoryStayBounded(){
   assertEquals(0,OperationsPolicy.availableStock(4,9))
   assertEquals(100,OperationsPolicy.coverage(30,20))
   assertEquals(0,OperationsPolicy.coverage(1,0))
 }
 @Test fun invalidMissionTimesAndCoordinatesAreRejected(){
   assertEquals("",OperationsPolicy.missionError("Mission","A valid mission brief",10,1.0,3600001.0,-1.28,36.82))
   assertTrue(OperationsPolicy.missionError("Mission","A valid mission brief",10,Double.NaN,3600001.0,-1.28,36.82).isNotEmpty())
   assertTrue(OperationsPolicy.missionError("Mission","A valid mission brief",10,1.0,3600001.0,91.0,36.82).isNotEmpty())
 }
}
