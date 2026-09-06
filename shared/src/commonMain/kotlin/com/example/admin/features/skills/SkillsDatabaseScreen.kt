package com.example.admin.features.skills

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.data.*
import com.example.admin.core.model.*
import com.example.admin.core.theme.*
import com.example.admin.core.icons.*


import com.example.admin.features.volunteers.PersonCard
import com.example.admin.features.volunteers.VolunteerDossier

@Composable fun SkillsDatabaseScreen(repository: AdminRepository) {
    val skills by repository.skills.collectAsState(); val people by repository.volunteers.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("All") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var editing by rememberSaveable { mutableStateOf(false) }; var personId by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = skills.filter { (category == "All" || it.category == category) && (it.name.contains(query,true) || it.description.contains(query,true)) }
    OpsPage {
        item { PageTitle("Know your team's strengths","A shared language for skills, demand and readiness.","SKILLS DIRECTORY") { GlassIconButton(OpsSymbol.Add, "Add skill", { selected = null; editing = true }) } }
        item { SearchBox(query,{ query = it },"Search skills"); Choices(listOf("All") + skills.map { it.category }.distinct(),category) { category = it } }
        if (shown.isEmpty()) item { EmptyState() }
        items(shown,key = { it.id }) { skill -> OpsCard {
            LiquidGlassTag(skill.category); SectionTitle(skill.name,skill.description)
            val matched = people.filter { p -> p.skills.any { it.equals(skill.name,true) } }
            Text("${matched.size} people · ${matched.count { it.status in listOf("available","standby") }} available or on standby")
            LiquidGlassBadge("${displayStatus(skill.demand)} demand",if (skill.demand == "high") GlassVariant.WARNING else GlassVariant.INFO)
            OpsButton("View skill & people",{ selected = skill.id },Modifier.fillMaxWidth(),true)
        } }
    }
    if (editing) SkillEditor(repository,skills.firstOrNull { it.id == selected }) { editing = false }
    else if (personId != null) VolunteerDossier(repository,personId!!,{ personId = null })
    else skills.firstOrNull { it.id == selected }?.let { skill -> DetailSheet("Skill overview",{ selected = null }) {
        SectionTitle(skill.name,skill.description); Tags(listOf(skill.category,"${skill.demand} demand")); OpsButton("Edit skill definition",{ editing = true },Modifier.fillMaxWidth(),true,symbol = OpsSymbol.Edit)
        SectionTitle("People with this skill")
        val matched = people.filter { p -> p.skills.any { it.equals(skill.name,true) } }
        if (matched.isEmpty()) EmptyState("No matching volunteers","Add this skill to the registry when qualifications have been confirmed.")
        matched.forEach { p -> PersonCard(p) { personId = p.id } }
    } }
}
@Composable private fun SkillEditor(repository: AdminRepository, skill: SkillItem?, close: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(skill?.name ?: "") }; var category by rememberSaveable { mutableStateOf(skill?.category ?: "Medical") }
    var demand by rememberSaveable { mutableStateOf(skill?.demand ?: "medium") }; var description by rememberSaveable { mutableStateOf(skill?.description ?: "") }; var result by remember { mutableStateOf<MutationResult?>(null) }
    DetailSheet(if (skill == null) "Add a skill" else "Edit skill definition",close) {
        OpsField("Skill name",name,{ name = it }); Choices(listOf("Medical","Search & Rescue","Logistics","Communications","Engineering"),category) { category = it }
        SectionTitle("Current demand"); Choices(listOf("high","medium","low"),demand) { demand = it }; OpsField("Scope and credential requirements",description,{ description = it },multiline = true)
        Feedback(result); OpsButton("Save definition",{ result = repository.saveSkill(skill?.id,name,category,demand,description); if (result?.success == true) close() },Modifier.fillMaxWidth())
    }
}
