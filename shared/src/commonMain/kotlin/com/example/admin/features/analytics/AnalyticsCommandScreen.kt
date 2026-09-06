package com.example.admin.features.analytics

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


@Composable fun AnalyticsCommandScreen(repository: AdminRepository) {
    val people by repository.volunteers.collectAsState(); val missions by repository.missions.collectAsState()
    val trends by repository.deploymentTrends.collectAsState(); val messages by repository.analyticsMessages.collectAsState()
    var period by rememberSaveable { mutableStateOf("5 months") }; var measure by rememberSaveable { mutableStateOf("Volunteers") }
    var query by rememberSaveable { mutableStateOf("") }; var result by remember { mutableStateOf<MutationResult?>(null) }
    var chat by rememberSaveable { mutableStateOf(false) }
    val active=missions.filter {it.status=="active"}
    OpsPage {
        item { PageTitle("Understand your response","Current workspace totals, sample trends and useful questions.","INSIGHTS") }
        item { Metrics(listOf(Metric(people.size.toString(),"Registered people","Current local records",OpsSymbol.People),Metric(active.size.toString(),"Active missions","${missions.count {it.status=="completed"}} completed",OpsSymbol.Mission),Metric(active.sumOf {it.requiredVolunteers-it.volunteersAssigned}.toString(),"Open places","Across active missions",OpsSymbol.Profile),Metric(people.count {it.status=="available"}.toString(),"Available now","Local availability",OpsSymbol.Check))) }
        item { OpsCard {
            SectionTitle("Deployment trends","Illustrative monthly history. It is not connected to live reporting.")
            Choices(listOf("3 months","5 months"),period) {period=it}; Choices(listOf("Volunteers","Missions"),measure) {measure=it}
            val shown=if(period=="3 months") trends.takeLast(3) else trends
            val ceiling=(shown.maxOfOrNull {if(measure=="Volunteers") it.volunteers else it.missions} ?: 1).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(200.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom) {
                shown.forEach { item ->
                    val value=if(measure=="Volunteers") item.volunteers else item.missions
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(value.toString(),style=MaterialTheme.typography.labelMedium)
                        Box(Modifier.fillMaxWidth(.65f).height((125f*value/ceiling).dp).background(AdminColors.VolunteerBlue,RoundedCornerShape(topStart=7.dp,topEnd=7.dp)))
                        Text(item.month,style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } }
        item { SectionTitle("Coverage by location","Current assignments and available capacity") }
        items(missions.groupBy {it.location}.entries.toList(),key={it.key}) { region -> OpsCard {
            SectionTitle(region.key); Capacity(region.value.sumOf {it.volunteersAssigned},region.value.sumOf {it.requiredVolunteers}); Text("${region.value.size} missions in this workspace",style=MaterialTheme.typography.bodyMedium,color=AdminColors.Slate)
        } }
        item { OpsCard {
            OpsIcon(OpsSymbol.Chart,tint=AdminColors.VolunteerBlue); SectionTitle("Ask your workspace","Clear calculations from the records you can inspect.")
            Text("This assistant calculates staffing, incident and stock totals locally. It does not use a generative AI service or make predictions.")
            OpsButton("Ask a question",{chat=true},Modifier.fillMaxWidth(),symbol=OpsSymbol.Messages)
        } }
    }
    if(chat) DetailSheet("Workspace questions",{chat=false}) {
        Choices(listOf("Staffing","Missions","Supplies","Incidents"),"") {query=it}
        messages.takeLast(8).forEach { message -> OpsCard(color=if(message.isFromUser) AdminColors.SoftSky else Color.White) { Text(if(message.isFromUser) "You" else "Workspace insight",style=MaterialTheme.typography.labelMedium,color=AdminColors.VolunteerBlue); Text(message.text) } }
        OpsField("Your question",query,{query=it}); Feedback(result?.takeIf {!it.success})
        OpsButton("Calculate insight",{result=repository.sendAnalyticsQuery(query); if(result?.success==true) query=""},Modifier.fillMaxWidth(),enabled=query.isNotBlank(),symbol=OpsSymbol.Send)
    }
}
