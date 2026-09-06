package com.example.admin.features.help

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


@Composable fun HelpSupportScreen(repository: AdminRepository) {
    val articles by repository.helpArticles.collectAsState()
    var query by rememberSaveable {mutableStateOf("")}; var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var category by rememberSaveable {mutableStateOf("All")}
    val shown=articles.filter {(category=="All" || it.category==category) && (it.title.contains(query,true) || it.summary.contains(query,true))}
    OpsPage {
        item {PageTitle("Support for the next step","Clear workflow guides for your coordination team.","HELP & SUPPORT")}
        item {LiquidGlassAlert("Need urgent help?","Use your organisation’s confirmed response lead or the local emergency number. This practice workspace does not contact emergency services.",GlassVariant.WARNING)}
        item {SearchBox(query,{query=it},"Search workflow guides"); Choices(listOf("All")+articles.map {it.category}.distinct(),category) {category=it}}
        if(shown.isEmpty()) item {EmptyState()}
        items(shown,key={it.id}) {article -> OpsCard {
            LiquidGlassTag(article.category); SectionTitle(article.title,article.summary); Text("${article.format} · ${article.readTime}",style=MaterialTheme.typography.bodySmall,color=AdminColors.Slate)
            OpsButton("Read guide",{selected=article.id},Modifier.fillMaxWidth(),true,symbol=OpsSymbol.Book)
        }}
        item {OpsCard {SectionTitle("Your escalation directory"); KeyValue("Mission questions","Use the named coordinator in each mission briefing.",OpsSymbol.People); KeyValue("Volunteer contact","Open People, then the volunteer’s dossier for recorded phone and email.",OpsSymbol.Phone); KeyValue("Urgent hazards","Use your organisation’s verified emergency contact and local procedures.",OpsSymbol.Warning)}}
    }
    articles.firstOrNull {it.id==selected}?.let {article -> DetailSheet("Workflow guide",{selected=null}) {
        SectionTitle(article.title,article.summary); LiquidGlassTag(article.category)
        article.steps.forEachIndexed {index,step -> KeyValue("Step ${index+1}",step)}
    }}
}
