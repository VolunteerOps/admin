package com.example.admin.core.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import admin.shared.generated.resources.Res
import kotlin.math.min
import kotlin.math.sin

private data class ReliefLayer(val tagline: Boolean, val color: Color, val path: Path)
private const val ArtWidth = 941f
private const val ArtHeight = 1672f

/**
 * The supplied splash composition compiled to editable vector contours and its
 * measured palette. No bitmap is used at runtime. Paths are parsed once, then
 * rendered with a cached transform and a draw-phase-only bristle reveal.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun AdminSplashScreenCanvas(onSplashFinished: () -> Unit = {}, modifier: Modifier = Modifier) {
    val finished by rememberUpdatedState(onSplashFinished)
    val paint = remember { Animatable(0f) }
    var layers by remember { mutableStateOf<List<ReliefLayer>?>(null) }
    var alreadyFinished by remember { mutableStateOf(false) }
    fun complete() { if (!alreadyFinished) { alreadyFinished = true; finished() } }

    LaunchedEffect(Unit) {
        layers = withContext(Dispatchers.Default) {
            runCatching {
                Res.readBytes("files/relief_vectors.tsv").decodeToString().lineSequence()
                    .filter { it.isNotBlank() }.map { line ->
                        val parts = line.split('\t', limit = 3)
                        ReliefLayer(parts[0] == "tagline", Color((0xff000000L or parts[1].toLong(16)).toInt()),
                            PathParser().parsePathString(parts[2]).toPath().apply { fillType = PathFillType.EvenOdd })
                    }.toList()
            }.getOrDefault(emptyList())
        }
        paint.animateTo(1f, tween(1450, easing = LinearEasing))
        delay(220)
        complete()
    }

    Box(modifier.fillMaxSize().background(Color.White)) {
        val artwork = layers
        if (artwork != null) {
            ReliefArtwork(artwork, { paint.value }, Modifier.fillMaxSize())
        }
        // A readable fallback also covers resource loading and accessible text.
        if (artwork.isNullOrEmpty()) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VolunteerOps", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text("Track, Coordinate, Save Lives")
            }
        }
        TextButton(onClick = { complete() }, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp)) {
            Text("Skip", color = Color(0xFF475569))
        }
    }
}

@Composable
private fun ReliefArtwork(layers: List<ReliefLayer>, progress: () -> Float, modifier: Modifier) {
    Spacer(modifier.semantics {
        contentDescription = "VolunteerOps. Track, Coordinate, Save Lives. Responders supporting a community at a medical relief camp."
    }.drawWithCache {
        // Preserve reference proportions on tablets and landscape. Tall phone
        // space is distributed above and below; no stretching of human figures.
        val factor = min(size.width / ArtWidth, size.height / ArtHeight)
        val dx = (size.width - ArtWidth * factor) / 2f
        val dy = (size.height - ArtHeight * factor) / 2f
        val scene = layers.filterNot { it.tagline }
        val tagline = layers.filter { it.tagline }
        val edge = Path()
        val fineStroke = Stroke(width = .45f)
        val caringHands = PathParser().parsePathString("M135,104 L129,106 L121,113 L117,121 L116,132 L119,141 L126,149 L132,152 L142,153 L151,150 L156,145 L160,139 L162,133 L162,124 L158,114 L152,108 L142,104ZM221,67 L218,67 L215,70 L213,76 L213,128 L207,142 L197,153 L190,157 L187,156 L189,151 L195,143 L199,135 L201,129 L201,125 L202,124 L200,116 L199,115 L195,115 L192,117 L188,123 L184,132 L176,144 L157,164 L149,178 L147,184 L146,192 L145,193 L145,201 L144,202 L145,203 L145,216 L146,217 L146,221 L148,226 L150,228 L153,227 L169,215 L175,212 L195,197 L212,180 L218,172 L224,160 L226,158 L226,156 L229,151 L231,145 L231,142 L233,137 L233,133 L234,132 L235,110 L234,109 L234,101 L233,100 L233,95 L232,94 L231,85 L225,70ZM58,67 L54,70 L50,77 L47,86 L47,89 L46,90 L45,101 L44,102 L44,110 L43,111 L44,132 L45,133 L45,137 L46,138 L46,141 L50,153 L57,167 L63,176 L76,191 L94,206 L108,215 L125,228 L127,228 L129,227 L130,225 L131,218 L132,217 L132,211 L133,210 L132,191 L131,190 L131,185 L127,176 L127,174 L117,159 L102,144 L96,133 L94,131 L88,118 L84,115 L80,115 L78,117 L77,120 L77,127 L80,136 L85,146 L91,155 L89,157 L81,153 L71,142 L68,136 L66,130 L66,125 L65,124 L65,90 L66,89 L65,73 L62,68Z").toPath()
        val glow = Brush.radialGradient(listOf(Color(0x182563EB), Color.Transparent), Offset.Zero, 12f)
        onDrawBehind {
            withTransform({ translate(dx, dy); scale(factor, factor, Offset.Zero) }) {
                scene.forEach { layer ->
                    drawPath(layer.path, layer.color)
                    drawPath(layer.path, layer.color, style = fineStroke)
                }
                if (true) {
                    drawRoundRect(Color(0xFF0F172A), Offset(330f,269f), Size(280f,282f), CornerRadius(74f))
                    translate(330f,269f) { drawPath(caringHands, Color.White) }
                }
                val p = progress().coerceIn(0f,1f)
                val x = 171f + 600f * p
                edge.rewind()
                edge.moveTo(160f,688f)
                edge.lineTo(x,688f)
                // A dozen unequal bristles paint the letter outlines; ink
                // remains inside the real tagline paths, not on a glowing bar.
                for (i in 0..24) {
                    val y = 688f + i * 4f
                    edge.lineTo(x + sin(i * 2.43f) * 5f + sin(i*.81f)*2f, y)
                }
                edge.lineTo(160f,790f)
                edge.close()
                clipPath(edge) { tagline.forEach { drawPath(it.path,it.color) } }
                if (p > .01f && p < .99f) {
                    translate(x,734f) { drawCircle(glow,12f,Offset.Zero) }
                }
            }
        }
    })
}
