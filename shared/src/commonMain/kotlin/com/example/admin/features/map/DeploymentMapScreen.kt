package com.example.admin.features.map

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


import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable fun DeploymentMapScreen(repository: AdminRepository) {
    val units by repository.tacticalUnits.collectAsState(); val incidents by repository.incidents.collectAsState()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }; var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }; var style by rememberSaveable { mutableStateOf("Terrain") }
    OpsPage {
        item { PageTitle("Every team in context","Select a team to review its people and incident assignment.","DEPLOYMENT MAP") }
        item { LiquidGlassAlert("Practice area","This is an interactive response diagram using sample positions, not a geographic navigation map or live GPS feed.") }
        item { Choices(listOf("Terrain","High contrast"),style) { style = it } }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(24.dp)).background(if (style == "Terrain") Color(0xFFE8F1EC) else Color(0xFFEDF2F8))) {
                val areaWidth = maxWidth; val areaHeight = maxHeight
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, shift, scale, _ -> zoom = (zoom * scale).coerceIn(1f,3f); pan = if (zoom == 1f) Offset.Zero else Offset((pan.x+shift.x).coerceIn(-size.width.toFloat(),size.width.toFloat()),(pan.y+shift.y).coerceIn(-size.height.toFloat(),size.height.toFloat())) } }) {
                    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }) {
                        Spacer(Modifier.fillMaxSize().drawWithCache {
                            val river = Path().apply { moveTo(size.width*.58f,0f); cubicTo(size.width*.9f,size.height*.3f,size.width*.15f,size.height*.4f,size.width*.46f,size.height); }
                            val road = Path().apply { moveTo(0f,size.height*.7f); cubicTo(size.width*.32f,size.height*.42f,size.width*.58f,size.height*.86f,size.width,size.height*.31f) }
                            onDrawBehind {
                                drawPath(river,Color(0xFFB5D8EB),style=Stroke(24.dp.toPx(),cap=StrokeCap.Round))
                                drawPath(road,Color.White,style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
                                for (i in 1..7) { val x=size.width*i/8; drawLine(Color(0x14475669),Offset(x,0f),Offset(x,size.height),1f) }
                                for (i in 1..5) { val y=size.height*i/6; drawLine(Color(0x14475669),Offset(0f,y),Offset(size.width,y),1f) }
                            }
                        })
                        Text("N ↑",Modifier.align(Alignment.TopStart).padding(16.dp),color=AdminColors.Slate,style=MaterialTheme.typography.labelMedium)
                        units.forEachIndexed { index, unit ->
                            Surface(onClick = { selected = unit.id }, modifier = Modifier.offset(x=areaWidth*unit.lngNormalized-24.dp,y=areaHeight*unit.latNormalized-24.dp).size(48.dp),shape=CircleShape,color=if(unit.status=="deployed") AdminColors.VolunteerBlue else AdminColors.Navy,border=BorderStroke(3.dp,Color.White),shadowElevation=3.dp) {
                                Box(contentAlignment=Alignment.Center) { Text((index+1).toString(),color=Color.White,style=MaterialTheme.typography.titleLarge) }
                            }
                        }
                    }
                }
                Text("Sample positions · pinch or drag",Modifier.align(Alignment.BottomStart).background(Color.White.copy(alpha=.9f)).padding(10.dp),style=MaterialTheme.typography.bodySmall,color=AdminColors.Slate)
            }
        }
        item { Actions { OpsButton("Zoom in",{ zoom=(zoom+.25f).coerceAtMost(3f) },secondary=true,symbol=OpsSymbol.Add); OpsButton("Reset view",{ zoom=1f; pan=Offset.Zero },secondary=true,symbol=OpsSymbol.Map) } }
        item { SectionTitle("Response teams","The list is also available without map gestures.") }
        items(units,key={it.id}) { unit -> OpsCard {
            Actions { StatusBadge(unit.status); LiquidGlassTag(unit.unitType) }
            SectionTitle(unit.name,"${unit.personnelCount} people")
            KeyValue("Assigned incident",incidents.firstOrNull { it.id==unit.assignedIncident }?.title ?: "Ready for an assignment")
            OpsButton("Manage team",{ selected=unit.id },Modifier.fillMaxWidth(),true,symbol=OpsSymbol.People)
        } }
    }
    units.firstOrNull { it.id==selected }?.let { unit ->
        var target by remember(unit.id) { mutableStateOf(unit.assignedIncident) }; var result by remember { mutableStateOf<MutationResult?>(null) }
        DetailSheet("Response team",{selected=null}) {
            SectionTitle(unit.name,"${unit.unitType} · ${unit.personnelCount} people"); StatusBadge(unit.status)
            Text("Choose an incident for this local coordination exercise.")
            incidents.filter { it.status!="resolved" }.forEach { incident -> Row(Modifier.fillMaxWidth().clickable {target=incident.id},verticalAlignment=Alignment.CenterVertically) { RadioButton(target==incident.id,{target=incident.id}); Text(incident.title,Modifier.weight(1f)) } }
            Feedback(result)
            OpsButton("Record team assignment",{result=repository.assignUnit(unit.id,target)},Modifier.fillMaxWidth(),enabled=target.isNotBlank())
            if(unit.assignedIncident.isNotBlank()) OpsButton("Release team to standby",{result=repository.assignUnit(unit.id,""); target=""},Modifier.fillMaxWidth(),true)
        }
    }
}
