package com.example.admin.core.onboarding
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.ExperimentalResourceApi
import admin.shared.generated.resources.Res
import kotlin.math.min
private data class PaintedRegion(val color: Color, val path: Path)
// Accessed on the composition dispatcher only; paths are immutable after publication.
private val artworkCache = mutableMapOf<Int,List<PaintedRegion>>()
@OptIn(ExperimentalResourceApi::class)
@Composable fun OnboardingArtwork(scene: Int, modifier: Modifier = Modifier, reducedMotion: Boolean = false) {
    var regions by remember(scene) { mutableStateOf(artworkCache[scene]) }
    var failed by remember(scene) { mutableStateOf(false) }
    var attempt by remember(scene) { mutableStateOf(0) }
    val reveal = remember(scene) { Animatable(if(reducedMotion) 1f else 0f) }
    LaunchedEffect(scene,attempt) {
        try {
            failed=false
            if(regions == null) {
                val prepared=withContext(Dispatchers.Default) {
                    Res.readBytes("files/onboarding_${scene.coerceIn(0,2)}.tsv").decodeToString().lineSequence().filter { it.isNotBlank() }.map {
                        val parts=it.split('\t',limit=2)
                        PaintedRegion(Color((0xFF000000L or parts[0].toLong(16)).toInt()),PathParser().parsePathString(parts[1]).toPath().apply { fillType=PathFillType.EvenOdd })
                    }.toList()
                }
                artworkCache[scene]=prepared;regions=prepared
            }
            if(reducedMotion) reveal.snapTo(1f) else reveal.animateTo(1f,tween(650, easing=FastOutSlowInEasing))
        } catch(c:CancellationException) { throw c } catch(_:Exception) { failed=true }
    }
    Box(modifier.background(Color.White),contentAlignment=Alignment.Center) {
        val art=regions
        if(art != null) Spacer(Modifier.fillMaxSize().semantics { contentDescription=DESCRIPTIONS[scene.coerceIn(0,2)] }.graphicsLayer {
            alpha=if(reducedMotion) 1f else reveal.value
            translationY=if(reducedMotion) 0f else (1f-reveal.value)*12.dp.toPx()
        }.drawWithCache {
            val factor=min(size.width/1000f,size.height/800f)
            val dx=(size.width-1000f*factor)/2f;val dy=(size.height-800f*factor)/2f
            val seam=Stroke(.3f)
            onDrawBehind { withTransform({ translate(dx,dy);scale(factor,factor,Offset.Zero) }) {
                art.forEach { drawPath(it.path,it.color);drawPath(it.path,it.color,style=seam) }
            } }
        }) else if(failed) TextButton({ attempt++ }) { Text("Reload illustration") }
        else CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
    }
}
private val DESCRIPTIONS = listOf("Two coordinators review a regional map with a laptop and radio.","A coordinator briefs a response team beside a planning board and equipment.","A radio operator and field leader exchange a handover at a coordination desk.")
