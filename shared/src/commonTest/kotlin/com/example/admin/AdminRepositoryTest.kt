package com.example.admin

import com.example.admin.core.data.AdminRepository
import kotlin.test.*

class AdminRepositoryTest {
    private fun repo() = AdminRepository({ null }, {})

    @Test fun assignmentsValidateAvailabilityCapacityAndReferences() {
        val r = repo()
        assertFalse(r.deployVolunteer("unknown", "MIS-901", "Intake", "Briefed").success)
        assertFalse(r.deployVolunteer("VOL-101", "MIS-903", "Intake", "Briefed").success)
        assertFalse(r.deployVolunteer("VOL-104", "MIS-901", "Intake", "Briefed").success)
        assertTrue(r.deployVolunteer("VOL-101", "MIS-901", "Intake", "North gate briefing").success)
        assertFalse(r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Again").success)
        assertEquals(2, r.missions.value.first { it.id == "MIS-901" }.volunteersAssigned)
        assertEquals("North gate briefing", r.assignments.value.last().notes)
        assertEquals("deployed", r.volunteers.value.first { it.id == "VOL-101" }.status)
    }

    @Test fun completingMissionReleasesEveryAssignmentAndPreventsReopening() {
        val r = repo()
        r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Briefed")
        assertTrue(r.updateMissionStatus("MIS-901", "completed").success)
        assertFalse(r.assignments.value.any { it.missionId == "MIS-901" && it.active })
        assertEquals(0, r.missions.value.first { it.id == "MIS-901" }.volunteersAssigned)
        assertEquals("available", r.volunteers.value.first { it.id == "VOL-107" }.status)
        assertFalse(r.updateMissionStatus("MIS-901", "active").success)
    }

    @Test fun pausedMissionRetainsStaffAndRejectsNewAssignments() {
        val r = repo()
        assertTrue(r.updateMissionStatus("MIS-901", "paused").success)
        assertEquals(1, r.missions.value.first { it.id == "MIS-901" }.volunteersAssigned)
        assertFalse(r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Briefed").success)
        assertTrue(r.updateMissionStatus("MIS-901", "active").success)
        assertTrue(r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Briefed").success)
    }

    @Test fun capacityCannotShrinkBelowAssignedPeople() {
        val r = repo()
        r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Briefed")
        val m = r.missions.value.first { it.id == "MIS-901" }
        assertFalse(r.saveMission(m.id, m.title, m.description, m.priority, m.location, 1, m.estimatedDuration, m.skillsRequired, m.coordinator, m.startTime).success)
        assertFalse(r.saveMission(null, "New mission", "Briefing", "high", "Camp", -1, "4 hours", listOf("Logistics"), "Lead", "Tomorrow").success)
        assertTrue(r.saveMission(null, "New mission", "Briefing", "high", "Camp", 3, "4 hours", listOf("Logistics"), "Lead", "Tomorrow").success)
        assertEquals("pending", r.missions.value.first().status)
    }

    @Test fun supplyAllocationsCannotExceedStock() {
        val r = repo()
        assertFalse(r.dispatchResource("RES-1", "INC-081", -3).success)
        assertFalse(r.dispatchResource("RES-1", "INC-081", 43).success)
        assertTrue(r.dispatchResource("RES-1", "INC-081", 10).success)
        assertEquals(32, r.resources.value.first().available)
        assertFalse(r.restockResource("RES-1", 29).success)
        assertTrue(r.restockResource("RES-1", 10).success)
        assertEquals(42, r.resources.value.first().available)
    }

    @Test fun dispatchPauseBlocksAssignmentsButAllowsTeamRelease() {
        val r = repo()
        r.triggerLockdown(true)
        assertFalse(r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Briefed").success)
        assertFalse(r.dispatchResource("RES-1", "INC-081", 1).success)
        assertFalse(r.assignUnit("UNIT-3", "INC-081").success)
        assertTrue(r.assignUnit("UNIT-1", "").success)
        assertEquals(0, r.incidents.value.first().teamsDeployed)
        assertTrue(r.releaseVolunteer("VOL-107").success)
    }

    @Test fun incidentsRequireAssignedTeamsToBeReleasedBeforeResolution() {
        val r = repo()
        assertFalse(r.updateIncident("INC-081", "resolved", 0).success)
        r.assignUnit("UNIT-1", "")
        assertTrue(r.updateIncident("INC-081", "resolved", 0).success)
        assertFalse(r.assignUnit("UNIT-3", "INC-081").success)
    }

    @Test fun skillRenamePreservesPeopleAndMissionReferences() {
        val r = repo()
        assertTrue(r.saveSkill("SKL-1", "Clinical support", "Medical", "high", "Qualified care team").success)
        assertTrue("Clinical support" in r.volunteers.value.first().skills)
        assertTrue("Clinical support" in r.missions.value.first().skillsRequired)
        assertFalse(r.saveSkill(null, "Clinical support", "Medical", "high", "Duplicate").success)
    }

    @Test fun broadcastsFreezeTheReviewedAudienceAndNeverClaimDelivery() {
        val r = repo()
        assertFalse(r.sendEmergencyAlert("Update", "Meet at the north gate.", "urgent", "Emergency alerts", "All volunteers").success)
        assertTrue(r.sendEmergencyAlert("Update", "Meet at the north gate.", "high", "Emergency alerts", "Medical volunteers").success)
        val draft = r.alerts.value.first()
        assertEquals(setOf("VOL-101", "VOL-107"), draft.recipientIds.toSet())
        assertEquals("Saved locally", draft.deliveryStatus)
        assertEquals(0, draft.responsesCount)
    }

    @Test fun completeSnapshotRoundTripsUnicodeNotesAndPausedMission() {
        var saved: String? = null
        val r = AdminRepository({ null }, { saved = it })
        r.deployVolunteer("VOL-101", "MIS-901", "Intake", "Équipe | north:\nMeet Amina ✓")
        r.updateMissionStatus("MIS-901", "paused")
        r.updateSettings(r.settings.value.copy(language = "French", smsAlerts = true))
        val restored = AdminRepository({ saved }, {})
        assertEquals(r.volunteers.value, restored.volunteers.value)
        assertEquals(r.missions.value, restored.missions.value)
        assertEquals(r.assignments.value, restored.assignments.value)
        assertEquals(r.settings.value, restored.settings.value)
        assertNull(restored.storageWarning.value)
    }

    @Test fun corruptSnapshotFallsBackAtomically() {
        val r = repo()
        r.addVolunteer("New Person", "new@example.org", "+123456789", "Nairobi", listOf("Logistics"))
        val restored = AdminRepository({ r.snapshot().dropLast(9) }, {})
        assertEquals(7, restored.volunteers.value.size)
        assertNotNull(restored.storageWarning.value)
    }

    @Test fun auditIdsStayUniqueAfterRetentionLimitAndStorageFailureIsVisible() {
        val r = repo()
        repeat(215) { r.triggerLockdown(it % 2 == 0) }
        assertEquals(200, r.securityEvents.value.size)
        assertEquals(200, r.securityEvents.value.map { it.id }.distinct().size)
        val unavailable = AdminRepository({ null }, { error("No disk") })
        unavailable.triggerLockdown(true)
        assertNotNull(unavailable.storageWarning.value)
    }
}
