/*
 * Copyright 2026 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Sampled off the reference, per region and per half, rather than eyeballed.
private val yellowGround = Color(0xFFE4E168)
private val strokeColor = Color(0xFF0E100F)
private val sclera = Color(0xFFEFE48C)
private val smallEyeOuter = Color(0xFFE9C23A)
private val smallEyeInner = Color(0xFFE08A18)
private val irisRed = Color(0xFFC0392B)
private val irisBlue = Color(0xFF3B3F8F)

// The reference is 1080x2160; every coordinate below is a pixel position in it.
private const val ART_W = 1080f
private const val ART_H = 2160f

private val PI_F = PI.toFloat()

/** A facet outline as a flat list of x,y pairs; the same array backs both the
 *  drawn path and the texture containment test. */
private fun poly(vararg xy: Float): FloatArray = xy

private fun buildPath(pts: FloatArray): Path = Path().apply {
    moveTo(pts[0], pts[1])
    for (i in 2 until pts.size step 2) lineTo(pts[i], pts[i + 1])
    close()
}

private fun inside(poly: FloatArray, x: Float, y: Float): Boolean {
    var c = false
    val n = poly.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = poly[2 * i];
        val yi = poly[2 * i + 1]
        val xj = poly[2 * j];
        val yj = poly[2 * j + 1]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
        j = i
    }
    return c
}

private fun edgeAngleAt(poly: FloatArray, x: Float, y: Float): Float {
    var best = Float.MAX_VALUE
    var ang = 0f
    val n = poly.size / 2
    for (i in 0 until n) {
        val j = (i + 1) % n
        val x0 = poly[2 * i];
        val y0 = poly[2 * i + 1]
        val x1 = poly[2 * j];
        val y1 = poly[2 * j + 1]
        val dx = x1 - x0;
        val dy = y1 - y0
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0f) 0f else (((x - x0) * dx + (y - y0) * dy) / l2).coerceIn(0f, 1f)
        val px = x0 + dx * t;
        val py = y0 + dy * t
        val d = (x - px) * (x - px) + (y - py) * (y - py)
        if (d < best) {
            best = d
            ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }
    }
    return ang
}

/** Broad soft patches of shifted tone: a painted plane is never one flat value. */
private fun blotches(poly: FloatArray, seed: Int, count: Int): Path {
    val rnd = Random(seed)
    val xs = (poly.indices step 2).map { poly[it] }
    val ys = (1 until poly.size step 2).map { poly[it] }
    val x0 = xs.min();
    val y0 = ys.min()
    val w = xs.max() - x0;
    val h = ys.max() - y0
    val base = min(w, h)
    val path = Path()
    var kept = 0
    var tries = 0
    while (kept < count && tries < count * 40) {
        tries++
        val cx = x0 + rnd.nextFloat() * w
        val cy = y0 + rnd.nextFloat() * h
        val r = base * (0.09f + rnd.nextFloat() * 0.20f)
        val verts = 9
        val pts = FloatArray(verts * 2)
        var ok = true
        for (k in 0 until verts) {
            val a = k * 2f * PI_F / verts
            val rr = r * (0.60f + rnd.nextFloat() * 0.65f)
            val px = cx + cos(a) * rr
            val py = cy + sin(a) * rr
            pts[2 * k] = px; pts[2 * k + 1] = py
            if (!inside(poly, px, py)) {
                ok = false; break
            }
        }
        if (!ok) continue
        path.moveTo(pts[0], pts[1])
        for (k in 1 until verts) path.lineTo(pts[2 * k], pts[2 * k + 1])
        path.close()
        kept++
    }
    return path
}

/** Brush marks that follow the nearest edge and pool around a few centres, the
 *  way a brush worked in passes does. */
private fun grain(poly: FloatArray, seed: Int, count: Int, lenScale: Float): Path {
    val rnd = Random(seed)
    val xs = (poly.indices step 2).map { poly[it] }
    val ys = (1 until poly.size step 2).map { poly[it] }
    val x0 = xs.min();
    val y0 = ys.min()
    val w = xs.max() - x0;
    val h = ys.max() - y0
    val clusters = List(6) {
        Offset(
            x0 + rnd.nextFloat() * w,
            y0 + rnd.nextFloat() * h
        ) to (0.10f + rnd.nextFloat() * 0.22f)
    }
    val path = Path()
    repeat(count) {
        val (c, spreadF) = clusters[rnd.nextInt(clusters.size)]
        val spread = min(w, h) * spreadF
        val px = c.x + (rnd.nextFloat() - 0.5f) * 2f * spread
        val py = c.y + (rnd.nextFloat() - 0.5f) * 2f * spread
        if (!inside(poly, px, py)) return@repeat
        val a =
            Math.toRadians((edgeAngleAt(poly, px, py) + (rnd.nextFloat() - 0.5f) * 34f).toDouble())
        val len = (16f + rnd.nextFloat() * 54f) * lenScale
        val ex = px + cos(a).toFloat() * len
        val ey = py + sin(a).toFloat() * len
        if (inside(poly, ex, ey)) {
            path.moveTo(px, py)
            path.lineTo(ex, ey)
        }
    }
    return path
}

/**
 * A flat colour plane. [depth] drives the parallax: planes that read as nearer the
 * front travel further under your finger, which is what makes the facets sit at
 * different distances instead of looking like a flat print.
 *
 * Everything below is derived once. All of it is per-frame draw input, and a Brush,
 * a Stroke or a resolved Color rebuilt inside the draw lambda costs an allocation
 * or a native shader compile on every frame that anything moves. The paint texture
 * is the exception: it is far too expensive to build here, so it arrives later
 * ([Texture]) and is rasterised into a layer rather than replayed per frame.
 */
private class Plane(
    val top: Color,
    val bottom: Color,
    val depth: Float,
    val strokeWidth: Float,
    val grainN: Int,
    val blotchN: Int,
    val grainAlpha: Float,
    val blotchAlpha: Float,
    val lenScale: Float,
    val pts: FloatArray
) {
    val path = buildPath(pts)
    private val bounds = path.getBounds()
    val pivot = bounds.center
    val entryTravel = entryTravel(pivot)
    val brush = Brush.verticalGradient(
        listOf(top, bottom), startY = bounds.top, endY = bounds.bottom
    )
    val edgeStroke =
        Stroke(strokeWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter, miter = 6f)
    val blotchDarkColor = Color.Black.copy(alpha = blotchAlpha)
    val blotchLightColor = Color.White.copy(alpha = blotchAlpha * 0.8f)
    val lightGrainColor = Color.White.copy(alpha = grainAlpha * 0.8f)
    val darkGrainColor = Color.Black.copy(alpha = grainAlpha)

    // The texture layer's footprint. Padded because a grain stroke is centred on a
    // point inside the polygon, so half its width can fall outside the bounds.
    val texLeft = bounds.left - TEX_PAD
    val texTop = bounds.top - TEX_PAD
    val texWidth = bounds.width + TEX_PAD * 2f
    val texHeight = bounds.height + TEX_PAD * 2f

    fun buildTexture() = Texture(
        blotches(pts, pts.size * 71 + 5, blotchN),
        blotches(pts, pts.size * 97 + 13, blotchN - 1),
        grain(pts, pts.size * 17 + 3, grainN, lenScale),
        grain(pts, pts.size * 53 + 11, grainN, lenScale)
    )
}

private const val TEX_PAD = 4f

// Outlines measured off the reference. Every boundary is a ruled run between
// corners: the painter's strokes wander inside their own width but do not curve,
// and tracing that wander is what made earlier versions read as wobbly noodles.
//
// Neighbours SHARE the vertices of a common boundary — re-fitting a facet on its
// own gives the two sides different lines and the ground shows through the seam.
private val planes = listOf(
    // the dark red ground above the head, cut by the sweep of the hair
    Plane(
        Color(0xFF3A160E), Color(0xFF2A100B), 0.06f, 26f, 90, 5, 0.017f, 0.015f, 1.4f,
        poly(
            1080f, 0f, 1080f, 431f, 921f, 352f, 600f, 98f, 297f, 0f
        )
    ),
    // the hair: one crescent from the top-left, over the crown and down the right
    Plane(
        Color(0xFF2E3220), Color(0xFF23261A), 0.12f, 30f, 300, 11, 0.03f, 0.028f, 1.9f,
        poly(
            297f, 0f, 600f, 98f, 921f, 352f, 1080f, 431f, 1080f, 1601f, 1045f, 1440f, 995f, 1436f,
            1006f, 1067f, 865f, 591f, 675f, 343f, 589f, 266f, 325f, 95f, 244f, 65f, 217f, 56f,
            224f, 0f
        )
    ),
    Plane(
        Color(0xFF3E3E1E), Color(0xFF33331A), 0.08f, 24f, 70, 4, 0.028f, 0.022f, 1.1f,
        poly(
            1080f, 2048f, 852f, 1972f, 1080f, 1737f
        )
    ),
    Plane(
        Color(0xFF43190F), Color(0xFF33130B), 0.06f, 22f, 60, 4, 0.03f, 0.02f, 1.2f,
        poly(
            852f, 1972f, 1080f, 2048f, 1080f, 2081f, 733f, 2064f, 723f, 2003f, 851f, 1821f
        )
    ),
    // brick red wedge under the right jaw
    Plane(
        Color(0xFFA03320), Color(0xFF8A2B18), 0.14f, 30f, 130, 7, 0.03f, 0.025f, 0.8f,
        poly(
            1080f, 1737f, 852f, 1972f, 851f, 1821f, 976f, 1575f, 995f, 1436f, 1045f, 1440f,
            1080f, 1601f
        )
    ),
    // the painting's bottom edge
    Plane(
        Color(0xFF1E2114), Color(0xFF14170E), 0.04f, 22f, 90, 4, 0.03f, 0.02f, 1.4f,
        poly(
            723f, 2003f, 733f, 2064f, 1080f, 2081f, 1080f, 2160f, 1050f, 2160f, 1055f, 2104f,
            107f, 2105f, 84f, 2160f, 0f, 2160f, 0f, 2115f, 126f, 2004f, 668f, 2065f, 676f, 1979f
        )
    ),
    Plane(
        Color(0xFF293B34), Color(0xFF37563B), 0.05f, 20f, 60, 3, 0.028f, 0.02f, 1.1f,
        poly(
            84f, 2160f, 107f, 2105f, 1055f, 2104f, 1050f, 2160f
        )
    ),
    // the whole left cheek: the reference runs olive at the brow to green at the
    // jaw with no contour between, so it is one facet with a gradient, not two
    Plane(
        Color(0xFF626034), Color(0xFF767C3C), 0.22f, 32f, 220, 12, 0.018f, 0.025f, 1.4f,
        poly(
            295f, 514f, 388f, 766f, 123f, 1173f, 60f, 1242f, 0f, 1264f, 0f, 580f, 217f, 56f,
            244f, 65f
        )
    ),
    // forehead wedge. Its upper edge keeps its bend: that run really does bow ~20px.
    Plane(
        Color(0xFFE87A21), Color(0xFFDD6C1B), 0.3f, 34f, 110, 5, 0.025f, 0.018f, 1.1f,
        poly(
            367f, 509f, 295f, 514f, 244f, 65f, 325f, 95f, 589f, 266f
        )
    ),
    // teal, unbroken from the brow past the eye to the mouth
    Plane(
        Color(0xFF04828C), Color(0xFF026E77), 0.34f, 34f, 260, 9, 0.03f, 0.02f, 1.7f,
        poly(
            675f, 343f, 672f, 1962f, 662f, 1951f, 621f, 1687f, 615f, 1656f, 557f, 1440f,
            658f, 1407f, 503f, 1298f, 388f, 766f, 295f, 514f, 367f, 509f, 589f, 266f
        )
    ),
    // the big orange down the right of the face, unbroken behind the eye
    Plane(
        Color(0xFFE97D20), Color(0xFFE0721A), 0.22f, 34f, 330, 10, 0.03f, 0.025f, 2.1f,
        poly(
            865f, 591f, 1006f, 1067f, 995f, 1436f, 976f, 1575f, 851f, 1821f, 723f, 2003f,
            676f, 1979f, 672f, 1962f, 675f, 343f
        )
    ),
    // the nose
    Plane(
        Color(0xFF394874), Color(0xFF3B4C7B), 0.42f, 32f, 100, 5, 0.019f, 0.025f, 0.9f,
        poly(
            503f, 1298f, 288f, 1403f, 199f, 1423f, 180f, 1137f, 123f, 1173f, 388f, 766f
        )
    ),
    // shadow under the nose. Its right tip stops short of the teal/orange edge on
    // purpose — a polygon that actually touched would pinch the teal facet in half.
    Plane(
        Color(0xFF1C5C72), Color(0xFF17506A), 0.4f, 28f, 80, 5, 0.03f, 0.025f, 1.0f,
        poly(
            658f, 1407f, 557f, 1440f, 495f, 1441f, 288f, 1403f, 503f, 1298f
        )
    ),
    // upper lip
    Plane(
        Color(0xFFA7628D), Color(0xFFB6608F), 0.46f, 30f, 70, 5, 0.025f, 0.025f, 0.7f,
        poly(
            615f, 1656f, 429f, 1648f, 302f, 1584f, 256f, 1602f, 236f, 1526f, 495f, 1441f,
            557f, 1440f
        )
    ),
    // the shadow between the lips
    Plane(
        Color(0xFF035B77), Color(0xFF025977), 0.44f, 28f, 60, 4, 0.03f, 0.022f, 0.8f,
        poly(
            621f, 1687f, 456f, 1732f, 265f, 1639f, 256f, 1602f, 302f, 1584f, 429f, 1648f,
            615f, 1656f
        )
    ),
    // lower lip
    Plane(
        Color(0xFFA89A1D), Color(0xFF968C16), 0.44f, 30f, 90, 5, 0.03f, 0.02f, 0.8f,
        poly(
            456f, 1732f, 621f, 1687f, 662f, 1951f, 481f, 1830f, 290f, 1824f, 265f, 1639f
        )
    ),
    // the chin: reads as a dome, measures as three straight runs
    Plane(
        Color(0xFF056480), Color(0xFF036E78), 0.34f, 32f, 150, 6, 0.03f, 0.025f, 1.3f,
        poly(
            672f, 1962f, 676f, 1979f, 668f, 2065f, 126f, 2004f, 290f, 1824f, 481f, 1830f,
            662f, 1951f
        )
    ),
)

private val groundRect = floatArrayOf(0f, 0f, ART_W, 0f, ART_W, ART_H, 0f, ART_H)

/**
 * The paint texture for one facet: broad tonal blotches and the brush marks over
 * them. Generating this is the one genuinely expensive thing on this screen —
 * rejection sampling with a point-in-polygon test per vertex, an edge search per
 * stroke, and some five thousand native path segments all told — so it is built off
 * the main thread. Left on it, all of it landed in the static initialiser and
 * stalled the frame that navigated here.
 */
private class Texture(
    val blotchDark: Path,
    val blotchLight: Path,
    val lightGrain: Path,
    val darkGrain: Path
)

private class Textures(
    val facets: List<Texture>,
    val groundBlotch: Path,
    val groundGrain: Path
)

/** One facet per core, in parallel; the ground alongside them. */
private suspend fun buildTextures(): Textures = withContext(Dispatchers.Default) {
    val facets = planes.map { async { it.buildTexture() } }
    val blotch = async { blotches(groundRect, 883, 14) }
    val marks = async { grain(groundRect, 991, 420, 1.3f) }
    Textures(facets.awaitAll(), blotch.await(), marks.await())
}

/**
 * One offscreen buffer per facet, holding that facet's texture rasterised once.
 *
 * The texture is thousands of low-alpha stroked segments; the transform that moves
 * it is four numbers. Replaying the segments every frame made a drag cost as much
 * as the whole painting, so each facet's texture is recorded into a layer that HWUI
 * rasterises once and then re-blits under the new matrix. Only the texture goes in:
 * the gradient fill and the mitred contour stay vectors, so the edges the outlines
 * were measured for are never resampled. The texture itself is at most 3% opaque,
 * where a half-pixel of filtering cannot be seen.
 *
 * Costs about 24MB of GPU texture at 1080p, for roughly nine tenths of the per-frame
 * geometry. The ground's own texture is deliberately NOT in a layer: it would be the
 * single largest buffer of the lot, screen-sized, and it holds under a tenth as much
 * geometry as the facets do.
 *
 * Deliberately outside the snapshot system — none of this should ever be able to
 * trigger a recomposition.
 */
private class TextureLayers(private val context: GraphicsContext) {
    val facets = List(planes.size) { newLayer() }

    var recorded: Textures? = null
    var recordedAt = Size.Unspecified

    private fun newLayer() = context.createGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }

    fun release() = facets.forEach { context.releaseGraphicsLayer(it) }
}

/** Records [tex] into [cache] if it is not already there, and reports whether the
 *  layers are usable this frame. Recording only builds display lists; the one-off
 *  rasterisation happens on the render thread at the first blit. */
private fun DrawScope.syncLayers(cache: TextureLayers, tex: Textures?, u: Float): Boolean {
    if (tex == null || size.width < 1f || size.height < 1f) return false
    if (cache.recorded === tex && cache.recordedAt == size) return true

    planes.forEachIndexed { i, plane ->
        val t = tex.facets[i]
        cache.facets[i].record(
            this, layoutDirection,
            IntSize(ceil(plane.texWidth * u).toInt(), ceil(plane.texHeight * u).toInt())
        ) {
            withTransform({
                scale(u, u, pivot = Offset.Zero)
                translate(-plane.texLeft, -plane.texTop)
            }) {
                drawPath(t.blotchDark, plane.blotchDarkColor)
                drawPath(t.blotchLight, plane.blotchLightColor)
                drawPath(t.lightGrain, plane.lightGrainColor, style = grainStroke)
                drawPath(t.darkGrain, plane.darkGrainColor, style = grainStroke)
            }
        }
    }

    cache.recorded = tex
    cache.recordedAt = size
    return true
}

/** Age, not brushwork: pale hairlines across everything. */
private val craquelure = Path().apply {
    val rnd = Random(4271)
    repeat(70) {
        val x = rnd.nextFloat() * ART_W
        val y = rnd.nextFloat() * ART_H
        val a = Math.toRadians((rnd.nextFloat() * 360f).toDouble())
        val len = 60f + rnd.nextFloat() * 240f
        moveTo(x, y)
        lineTo(x + cos(a).toFloat() * len, y + sin(a).toFloat() * len)
    }
}

// The nostril slit: 21px wide in the reference, which is fineStroke's 22f.
private val nostril = Path().apply {
    moveTo(350f, 1064f)
    lineTo(352f, 1233f)
}

// The lid is a ~34px painted stroke, so the almond below is its CENTRE line and
// the facets around it were grown into the lid until they met.
//
// The eye is the one thing here that is genuinely curved: each lid fits a single
// cubic to rms 2.7px. Ruling it faceted the almond; tracing it put the wobble back.
private val smallEyeCentre = Offset(227f, 535f)
private const val SMALL_EYE_R = 108f
private val irisCentre = Offset(657f, 812f)
private val bigEyePath = Path().apply {
    moveTo(380f, 797f)                             // inner corner
    cubicTo(544f, 579f, 823f, 634f, 944f, 807f)    // upper lid, out to the outer corner
    cubicTo(750f, 1070f, 518f, 998f, 380f, 797f)   // lower lid, back
    close()
}

private val smallEyeTravel = entryTravel(smallEyeCentre)
private val bigEyeTravel = entryTravel(irisCentre)

private val grainStroke = Stroke(5f, cap = StrokeCap.Round)
private val fineStroke = Stroke(22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val crackStroke = Stroke(2.2f, cap = StrokeCap.Round)
private val eyeStroke = Stroke(34f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private val groundBlotchColor = Color.Black.copy(alpha = 0.018f)
private val groundGrainColor = Color.Black.copy(alpha = 0.022f)
private val craquelureColor = Color.White.copy(alpha = 0.04f)
private val eyeHighlight = sclera.copy(alpha = 0.85f)

// The eyes ride along as two more facets past the end of the plane list, so they
// enter, shatter and parallax on the same footing as everything else.
private val SMALL_EYE_INDEX = planes.size
private val BIG_EYE_INDEX = planes.size + 1

// The nose plane, the one facet that carries an extra mark.
private const val NOSTRIL_PLANE = 11

private const val PARALLAX = 70f
private const val SHATTER = 130f

/** Deterministic per-facet noise: a shatter is repeatable and allocates nothing. */
private fun jitter(index: Int, seed: Int, salt: Int): Float {
    var h = index * 374761393 + seed * 668265263 + salt * 1274126177
    h = (h xor (h ushr 13)) * 1274126177
    return (((h xor (h ushr 16)) and 0xFFFF) / 32768f) - 1f
}

@Composable
fun CubistPortraitCanvas(modifier: Modifier = Modifier) {
    MatchStatusBarToPage(yellowGround)

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Read in the draw lambda, so the facets simply draw flat until the texture
    // lands — which happens well before the first of them has flown in.
    val textures = remember { mutableStateOf<Textures?>(null) }
    LaunchedEffect(Unit) { textures.value = buildTextures() }

    val graphicsContext = LocalGraphicsContext.current
    val layers = remember(graphicsContext) { TextureLayers(graphicsContext) }
    DisposableEffect(layers) { onDispose { layers.release() } }

    // All three are read only inside the draw lambda, so animating them invalidates
    // the draw without ever recomposing.
    val assemble = remember { Animatable(0f) }
    LaunchedEffect(Unit) { assemble.animateTo(1f, tween(1900, easing = LinearEasing)) }

    val look = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val shatter = remember { Animatable(0f) }
    val seed = remember { intArrayOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        seed[0]++
                        shatter.snapTo(1f)
                        shatter.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 170f))
                    }
                }
            }
            .pointerInput(Unit) {
                // Normalised to -1..1 so the parallax is independent of screen size
                fun norm(p: Offset) = Offset(
                    (p.x / size.width - 0.5f) * 2f,
                    (p.y / size.height - 0.5f) * 2f
                )

                val followSpring = spring<Offset>(dampingRatio = 0.75f, stiffness = 190f)
                detectDragGestures(
                    onDragStart = { scope.launch { look.animateTo(norm(it), followSpring) } },
                    onDrag = { change, _ ->
                        change.consume()
                        scope.launch { look.animateTo(norm(change.position), followSpring) }
                    },
                    onDragEnd = { scope.launch { look.animateTo(Offset.Zero, followSpring) } },
                    onDragCancel = { scope.launch { look.animateTo(Offset.Zero, followSpring) } }
                )
            }
    ) {
        // Fill, not fit: the reference is itself a crop, so covering the screen beats
        // letterboxing it inside dark bands. Cover scaling also means the ground
        // reaches every edge, so there is no backdrop to paint under it.
        val u = max(size.width / ART_W, size.height / ART_H)
        val tex = textures.value
        val ready = syncLayers(layers, tex, u)

        withTransform({
            translate((size.width - ART_W * u) / 2f, (size.height - ART_H * u) / 2f)
            scale(u, u, pivot = Offset.Zero)
        }) {
            val l = look.value
            val s = shatter.value
            val asm = assemble.value

            val sd = seed[0]
            val entering = asm < 1f

            drawRect(yellowGround, size = Size(ART_W, ART_H))
            if (tex != null) {
                drawPath(tex.groundBlotch, groundBlotchColor)
                drawPath(tex.groundGrain, groundGrainColor, style = grainStroke)
            }

            planes.forEachIndexed { i, plane ->
                // Neither term survives the animation it belongs to, and once the
                // entry has landed the stagger curve is 0 for every facet — so in the
                // common case, a drag, this is one multiply per axis.
                var dx = l.x * plane.depth * PARALLAX
                var dy = l.y * plane.depth * PARALLAX
                val spin = if (entering) entryProgress(i, asm) else 0f
                if (spin > 0f) {
                    dx += plane.entryTravel.x * spin
                    dy += plane.entryTravel.y * spin
                }
                // != 0, not > 0: the shatter spring rings through negative values and
                // the facets have to swing back the other way with it.
                if (s != 0f) {
                    dx += jitter(i, sd, 0) * SHATTER * s
                    dy += jitter(i, sd, 1) * SHATTER * s
                }
                val rot = when {
                    spin > 0.001f -> jitter(i, 0, 2) * 26f * spin
                    s > 0.001f -> jitter(i, sd, 2) * 11f * s
                    else -> 0f
                }
                val layer = if (ready) layers.facets[i] else null
                // A facet that has landed while the rest are still arriving skips the
                // canvas save/restore entirely.
                if (dx == 0f && dy == 0f && rot == 0f) {
                    drawPlane(plane, i, layer, u)
                } else {
                    withTransform({
                        translate(dx, dy)
                        if (rot != 0f) rotate(rot, plane.pivot)
                    }) { drawPlane(plane, i, layer, u) }
                }
            }

            drawEyes(l, s, sd, asm)

            drawPath(craquelure, craquelureColor, style = crackStroke)
        }
    }
}

/** One facet: the flat colour, the paint texture worked over it, then its own
 *  contour. */
private fun DrawScope.drawPlane(plane: Plane, index: Int, texture: GraphicsLayer?, u: Float) {
    drawPath(plane.path, plane.brush)
    // The layer holds device pixels, so the art scale is undone for the blit and one
    // texel lands on one pixel.
    if (texture != null) {
        withTransform({
            scale(1f / u, 1f / u, pivot = Offset.Zero)
            translate(plane.texLeft * u, plane.texTop * u)
        }) { drawLayer(texture) }
    }
    // Each facet strokes its own edge. Sharing one skeleton path for the internal
    // lines is cheaper, but those lines peel away the moment the facets move
    // independently. Mitred, not round: a 30px round join turns the small facets
    // into pills instead of the crisp quadrilaterals the reference has.
    drawPath(plane.path, strokeColor, style = plane.edgeStroke)
    if (index == NOSTRIL_PLANE) drawPath(nostril, strokeColor, style = fineStroke)
}

/** 0 once this facet has landed, 1 before it starts moving. Staggered so the
 *  painting builds up rather than snapping together at once — the stagger has to
 *  fit inside asm's 0..1 or the last facets never finish arriving. */
private fun entryProgress(index: Int, asm: Float): Float {
    val t = ((asm - index * 0.020f) / 0.55f).coerceIn(0f, 1f)
    val e = 1f - (1f - t) * (1f - t) * (1f - t)      // ease out cubic
    return 1f - e
}

/** How far off-screen a facet starts and in which direction: out along the ray from
 *  the canvas centre through its pivot, so they converge inward on entry rather than
 *  sliding in as a slab. Fixed per facet, so scaling it by the progress is all the
 *  per-frame work the entry costs. */
private fun entryTravel(pivot: Offset): Offset {
    val dx = pivot.x - ART_W / 2f
    val dy = pivot.y - ART_H / 2f
    val len = max(1f, hypot(dx, dy))
    return Offset(dx / len * 2100f, dy / len * 2100f)
}

private fun DrawScope.drawEyes(look: Offset, s: Float, seed: Int, asm: Float) {
    val entering = asm < 1f
    val eSmall =
        if (entering) smallEyeTravel * entryProgress(SMALL_EYE_INDEX, asm) else Offset.Zero
    withTransform({
        translate(
            look.x * 0.24f * PARALLAX + jitter(SMALL_EYE_INDEX, seed, 0) * SHATTER * s + eSmall.x,
            look.y * 0.24f * PARALLAX + jitter(SMALL_EYE_INDEX, seed, 1) * SHATTER * s + eSmall.y
        )
    }) {
        // rings at r = 108 / 78 / 58, pupil at 40
        drawCircle(strokeColor, SMALL_EYE_R, smallEyeCentre)
        drawCircle(smallEyeOuter, 78f, smallEyeCentre)
        drawCircle(smallEyeInner, 58f, smallEyeCentre)
        // The pupils lead the drag further than their plane does, which is what makes
        // the face read as watching you rather than just sliding about.
        drawCircle(strokeColor, 40f, smallEyeCentre + look * 14f)
    }

    val eBig = if (entering) bigEyeTravel * entryProgress(BIG_EYE_INDEX, asm) else Offset.Zero
    withTransform({
        translate(
            look.x * 0.38f * PARALLAX + jitter(BIG_EYE_INDEX, seed, 0) * SHATTER * s + eBig.x,
            look.y * 0.38f * PARALLAX + jitter(BIG_EYE_INDEX, seed, 1) * SHATTER * s + eBig.y
        )
    }) {
        drawPath(bigEyePath, sclera)

        // Clipped to the sclera, so the iris can chase your finger without sliding
        // out through the lid.
        clipPath(bigEyePath) {
            val iris = irisCentre + Offset(look.x * 52f, look.y * 22f)
            // Red out to 124, blue to 96, red AGAIN to 70, pupil to 54: the reference
            // really does put red on both sides of the blue.
            drawCircle(irisRed, 124f, iris)
            drawCircle(irisBlue, 96f, iris)
            drawCircle(irisRed, 70f, iris)
            drawCircle(strokeColor, 54f, iris)
            drawCircle(eyeHighlight, 14f, iris + Offset(-26f, -26f))
        }

        drawPath(bigEyePath, strokeColor, style = eyeStroke)
    }
}

/** The canvas sizes itself, so neither a centring Box nor a second fillMaxSize from
 *  here buys anything but an extra layout node. */
@Composable
fun CubistPortraitScreen() {
    CubistPortraitCanvas()
}

@Preview(showSystemUi = true)
@Composable
fun CubistPortraitPreview() {
    CubistPortraitCanvas()
}
