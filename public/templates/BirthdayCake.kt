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
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val pinkBase = Color(0xFFD48BA6)
private val brownBase = Color(0xFF3B1E1C)
private val pinkFilling = Color(0xFFB06482)
private val crumbColor = Color(0xFF160807)

private val pageBackground = Color(0xFFFFF3E0)

// Sorted by x so each colour bucket below is one contiguous run. Reordering
// changes nothing visually: the same points get drawn.
private val crumbData =
    List(80) { Offset(Random.nextFloat(), Random.nextFloat()) }.sortedBy { it.x }
private val crustCrumbData =
    List(130) { Offset(Random.nextFloat(), Random.nextFloat()) }.sortedBy { it.x }

private const val CRUMB_RADIUS = 1.5f

// One drawCircle per crumb meant ~3,120 draw calls a frame. They go into a single
// Path instead, split into this many buckets where the tint varies along a
// surface. Six keeps the shading error under 1/255.
private const val CRUMB_BUCKETS = 6

private fun Color.scaleRgb(factor: Float) = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun getShade(normalAngle: Float, baseColor: Color): Color {
    val lightAngle = -45f
    val diff = abs((normalAngle - lightAngle + 180) % 360 - 180)
    val intensity = (180f - diff) / 180f
    return baseColor.scaleRgb(0.5f + (intensity * 0.6f))
}

// One source of truth, so the cut walls, the crust and the bitten interior can
// never drift apart.
private class CakeLayer(val top: Float, val bottom: Float, val color: Color, val isSponge: Boolean)

private val cakeLayers = listOf(
    CakeLayer(0.00f, 0.10f, pinkBase, false),
    CakeLayer(0.10f, 0.29f, brownBase, true),
    CakeLayer(0.29f, 0.33f, pinkFilling, false),
    CakeLayer(0.33f, 0.52f, brownBase, true),
    CakeLayer(0.52f, 0.56f, pinkFilling, false),
    CakeLayer(0.56f, 0.75f, brownBase, true),
    CakeLayer(0.75f, 0.79f, pinkFilling, false),
    CakeLayer(0.79f, 1.00f, brownBase, true),
)

// Drawing is single-threaded and each of these is filled and used inside one
// call, so they can be shared instead of allocating hundreds of Paths a frame.
// Anything that nests needs its own.
private val wallScratch = Path()
private val crustScratch = Path()
private val craterScratch = Path()
private val crumbScratch = Path()
private val footprintScratch = Path()
private val topFaceScratch = Path()
private val biteScratch = Path()
private val volumeScratch = Path()
private val cherryScratch = Path()
private val flameScratch = Path()

private fun Path.addCrumb(x: Float, y: Float) =
    addOval(Rect(x - CRUMB_RADIUS, y - CRUMB_RADIUS, x + CRUMB_RADIUS, y + CRUMB_RADIUS))

private fun bucketOf(u: Float) = (u * CRUMB_BUCKETS).toInt().coerceIn(0, CRUMB_BUCKETS - 1)

/**
 * A crumb's position *relative to its slice* depends only on the slice's angle and
 * the cake radius, not on where the slice currently sits. So the hover never
 * invalidates it, and a frame costs one addPath per bucket instead of 3,120.
 */
private class CrumbCache {
    // Fields are open and the lookups inline, so the builder lambdas get inlined
    // rather than allocating ~100 closures a frame.
    val paths = HashMap<Long, Path>()
    val brushes = HashMap<Long, Brush>()
    var keyRx = 0f

    fun ensure(rx: Float) {
        // A moving slice mints a new key every frame, so cap the growth.
        if (rx != keyRx || paths.size > 900 || brushes.size > 900) {
            paths.clear()
            brushes.clear()
            keyRx = rx
        }
    }

    inline fun path(
        rx: Float,
        angle: Float,
        wall: Int,
        layer: Int,
        bucket: Int,
        build: (Path) -> Unit
    ): Path {
        ensure(rx)
        val k = ((angle.toRawBits().toLong() and 0xFFFFFFFFL) shl 16) or
                (wall.toLong() shl 10) or (layer.toLong() shl 5) or bucket.toLong()
        return paths.getOrPut(k) { Path().also(build) }
    }

    /**
     * Every Brush instance compiles a fresh native shader on use, and the crust was
     * making 48 a frame. The hover only moves things vertically, so a horizontal
     * gradient's coordinates never change: same shader every frame.
     */
    inline fun crustBrush(
        rx: Float,
        angle: Float,
        cx: Float,
        layer: Int,
        make: () -> Brush
    ): Brush {
        ensure(rx)
        val k = ((angle.toRawBits().toLong() and 0xFFFFFFFFL) shl 24) or
                ((cx.toInt().toLong() and 0xFFFFFL) shl 4) or layer.toLong()
        return brushes.getOrPut(k, make)
    }
}

private val crumbCache = CrumbCache()

/** [CrumbCache] again, for the bitten-out interior: everything about a crater is
 *  fixed by its radius once the bite's spring settles. */
private class CraterCache {
    val paths = HashMap<Long, Path>()
    val brushes = HashMap<Long, Brush>()

    fun key(r: Float, a: Int, b: Int) =
        ((r.toRawBits().toLong() and 0xFFFFFFFFL) shl 16) or (a.toLong() shl 8) or b.toLong()

    fun trim() {
        if (paths.size > 600) paths.clear()
        if (brushes.size > 600) brushes.clear()
    }

    inline fun path(r: Float, slot: Int, bucket: Int, build: (Path) -> Unit): Path {
        trim()
        return paths.getOrPut(key(r, slot, bucket)) { Path().also(build) }
    }

    /** [bx] is in the key because the gradient bakes it in. It happens to be
     *  constant today; omitting it would fail silently the day it is not. */
    inline fun brush(r: Float, slot: Int, bx: Float, make: () -> Brush): Brush {
        trim()
        val k = ((r.toRawBits().toLong() and 0xFFFFFFFFL) shl 24) or
                ((bx.toInt().toLong() and 0xFFFFL) shl 8) or slot.toLong()
        return brushes.getOrPut(k, make)
    }
}

// Kept apart so the packed cache keys cannot collide
private const val CRATER_SLOT_BAND = 0        // ..7, one per layer
private const val CRATER_SLOT_LIP = 32
private const val CRATER_SLOT_CRUMBS = 64     // ..121
private const val CRATER_SLOT_SILHOUETTE = 200

private val craterCache = CraterCache()

private const val WALL_LEFT_CUT = 0
private const val WALL_RIGHT_CUT = 1
private const val WALL_CRUST = 2

/**
 * Uniting the slice's four faces costs three Skia boolean ops. The union is
 * translation-invariant, so it is cut once in local space and stamped into place
 * rather than re-cut every time the hover moves the slice.
 */
private class VolumeCache {
    private var keyAngle = Float.NaN
    private var keyRx = 0f
    private var keyH = 0f
    private val local = Path()

    fun localVolume(startAngle: Float, sweep: Float, rx: Float, ry: Float, h: Float): Path {
        // Exact key on purpose: once the rotation settles the angle is
        // bit-identical every frame, so the cache holds. Quantising would risk a
        // slightly-wrong clip bound mid-rotation.
        if (startAngle != keyAngle || rx != keyRx || h != keyH) {
            keyAngle = startAngle
            keyRx = rx
            keyH = h
            rebuild(startAngle, sweep, rx, ry, h)
        }
        return local
    }

    private fun rebuild(startAngle: Float, sweep: Float, rx: Float, ry: Float, h: Float) {
        val pushBackDist = 1.5f
        val midRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
        val startRad = Math.toRadians(startAngle.toDouble())
        val endRad = Math.toRadians((startAngle + sweep).toDouble())

        val topCenter = Offset(
            -cos(midRad).toFloat() * pushBackDist,
            -sin(midRad).toFloat() * (ry / rx) * pushBackDist
        )
        val topStart = Offset(cos(startRad).toFloat() * rx, sin(startRad).toFloat() * ry)
        val botStart = Offset(topStart.x, topStart.y + h)
        val topEnd = Offset(cos(endRad).toFloat() * rx, sin(endRad).toFloat() * ry)
        val botEnd = Offset(topEnd.x, topEnd.y + h)
        val rectTop = Rect(-rx, -ry, rx, ry)
        val rectBot = Rect(-rx, h - ry, rx, h + ry)

        val topFace = Path().apply {
            moveTo(topCenter.x, topCenter.y); arcTo(
            rectTop,
            startAngle,
            sweep,
            false
        ); close()
        }
        val leftFace = Path().apply {
            moveTo(topCenter.x, topCenter.y); lineTo(topEnd.x, topEnd.y); lineTo(
            botEnd.x,
            botEnd.y
        ); lineTo(topCenter.x, topCenter.y + h); close()
        }
        val rightFace = Path().apply {
            moveTo(topCenter.x, topCenter.y); lineTo(topStart.x, topStart.y); lineTo(
            botStart.x,
            botStart.y
        ); lineTo(topCenter.x, topCenter.y + h); close()
        }
        val crustFace = Path().apply {
            moveTo(topStart.x, topStart.y); arcTo(
            rectTop,
            startAngle,
            sweep,
            false
        ); lineTo(botEnd.x, botEnd.y); arcTo(rectBot, startAngle + sweep, -sweep, false); close()
        }

        val t1 =
            Path().apply { op(topFace, leftFace, androidx.compose.ui.graphics.PathOperation.Union) }
        val t2 =
            Path().apply { op(t1, rightFace, androidx.compose.ui.graphics.PathOperation.Union) }
        local.op(t2, crustFace, androidx.compose.ui.graphics.PathOperation.Union)
    }
}

private val PI_F = PI.toFloat()
private val TWO_PI = PI_F * 2f

private const val CRATER_RINGS = 12
private const val CRATER_STEPS = 22

// Baked once so the rim is crumbly rather than a perfect circle, and holds still.
private val biteRimHarmonics: List<List<Triple<Float, Float, Float>>> = List(4) { i ->
    val rnd = Random(9176 + i * 37)
    listOf(
        Triple(0.055f, 3f, rnd.nextFloat() * TWO_PI),
        Triple(0.032f, 6f, rnd.nextFloat() * TWO_PI),
        Triple(0.016f, 11f, rnd.nextFloat() * TWO_PI)
    )
}

private val rimCrumbData = List(4) { List(24) { Offset(Random.nextFloat(), Random.nextFloat()) } }

private fun rimWobble(biteIndex: Int, angle: Float): Float {
    var w = 1f
    biteRimHarmonics[biteIndex % biteRimHarmonics.size].forEach { (amp, freq, phase) ->
        w += amp * sin(angle * freq + phase)
    }
    return w
}

private fun craterDepth(r: Float, h: Float) = (r * 1.75f).coerceAtMost(h * 0.94f)

/**
 * A bite is a hemispherical scoop, not a drilled cylinder: full width at the rim,
 * closing to a flat floor that stops short of the base so the slice is never
 * see-through.
 */
private fun craterRadiusAt(f: Float, r: Float, h: Float): Float {
    val d = craterDepth(r, h) / h
    if (d <= 0f || f >= d) return 0f
    val t = f / d
    return r * sqrt((1f - t * t).coerceAtLeast(0f))
}

private fun Path.addRimRing(cx: Float, cy: Float, rx: Float, ry: Float, biteIndex: Int) {
    for (i in 0..CRATER_STEPS) {
        val a = TWO_PI * (i.toFloat() / CRATER_STEPS)
        val w = rimWobble(biteIndex, a)
        val x = cx + cos(a) * rx * w
        val y = cy + sin(a) * ry * w
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Every ring winds the same way, so non-zero fill unions them cleanly. */
private fun Path.addCrater(
    bx: Float,
    by: Float,
    r: Float,
    h: Float,
    squash: Float,
    biteIndex: Int
) {
    if (r <= 0.75f) return
    val dFrac = craterDepth(r, h) / h
    for (ring in 0..CRATER_RINGS) {
        // sqrt spacing clusters rings toward the floor, where the radius collapses fastest
        val f = sqrt(ring.toFloat() / CRATER_RINGS) * dFrac
        val rr = craterRadiusAt(f, r, h)
        if (rr <= 0.75f) continue
        addRimRing(bx, by + h * f, rr, rr * squash, biteIndex)
    }
}

/** The far wall of the scoop between two depths, which is all the camera can see
 *  into. Angles run 180..360 degrees. */
private fun buildCraterBand(
    path: Path, h: Float, squash: Float, biteIndex: Int,
    f1: Float, r1: Float, f2: Float, r2: Float
) {
    val y1 = h * f1
    val y2 = h * f2
    for (i in 0..CRATER_STEPS) {
        val a = PI_F + PI_F * (i.toFloat() / CRATER_STEPS)
        val w = rimWobble(biteIndex, a)
        val x = cos(a) * r1 * w
        val y = y1 + sin(a) * r1 * squash * w
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (i in CRATER_STEPS downTo 0) {
        val a = PI_F + PI_F * (i.toFloat() / CRATER_STEPS)
        val w = rimWobble(biteIndex, a)
        path.lineTo(cos(a) * r2 * w, y2 + sin(a) * r2 * squash * w)
    }
    path.close()
}

private fun craterBandPath(
    bx: Float, by: Float, h: Float, squash: Float, biteIndex: Int, slot: Int,
    r: Float, f1: Float, r1: Float, f2: Float, r2: Float
): Path {
    val cached = craterCache.path(r, slot, biteIndex) { p ->
        buildCraterBand(p, h, squash, biteIndex, f1, r1, f2, r2)
    }
    craterScratch.reset()
    craterScratch.addPath(cached, Offset(bx, by))
    return craterScratch
}

private fun DrawScope.drawCraterInterior(
    bx: Float, by: Float, r: Float, h: Float, squash: Float, biteIndex: Int
) {
    if (r <= 0.75f) return
    val dFrac = (craterDepth(r, h) / h).coerceAtLeast(0.0001f)

    cakeLayers.forEachIndexed { index, layer ->
        val f1 = layer.top.coerceAtMost(dFrac)
        val f2 = layer.bottom.coerceAtMost(dFrac)
        if (f2 - f1 < 0.001f) return@forEachIndexed

        val r1 = craterRadiusAt(f1, r, h)
        val r2 = craterRadiusAt(f2, r, h)
        if (r1 <= 0.75f) return@forEachIndexed

        // Gentle: getShade already darkens to 0.5x, and the two compound into
        // near-black on sponge this dark.
        val ao = 1f - 0.26f * (((f1 + f2) * 0.5f) / dFrac).coerceIn(0f, 1f)
        val occluded = layer.color.scaleRgb(ao * 1.22f)

        // A concave wall's normal sweeps 180 degrees across the visible arc, so the
        // gradient samples it at the inward normal: screen angle + 180. Using the
        // outward normal shades a hole like a bump, and it reads flat.
        drawPath(
            craterBandPath(bx, by, h, squash, biteIndex, index, r, f1, r1, f2, r2),
            craterCache.brush(r, CRATER_SLOT_BAND + index * 2 + biteIndex, bx) {
                Brush.horizontalGradient(
                    0.000f to getShade(0f, occluded),
                    0.146f to getShade(45f, occluded),
                    0.500f to getShade(90f, occluded),
                    0.854f to getShade(135f, occluded),
                    1.000f to getShade(180f, occluded),
                    startX = bx - r1, endX = bx + r1
                )
            }
        )

        if (layer.isSponge) {
            val base = crumbColor.scaleRgb(ao)
            for (bucket in 0 until CRUMB_BUCKETS) {
                val cached = craterCache.path(
                    r,
                    CRATER_SLOT_CRUMBS + index * 2 + biteIndex,
                    bucket
                ) { path ->
                    crumbData.forEach { crumb ->
                        if (bucketOf(crumb.x) != bucket) return@forEach
                        val a = PI_F + PI_F * crumb.x
                        val w = rimWobble(biteIndex, a)
                        val fx = f1 + crumb.y * (f2 - f1)
                        val rf = craterRadiusAt(fx, r, h)
                        path.addCrumb(cos(a) * rf * w, h * fx + sin(a) * rf * squash * w)
                    }
                }
                if (cached.isEmpty) continue
                crumbScratch.reset()
                crumbScratch.addPath(cached, Offset(bx, by))
                drawPath(
                    crumbScratch,
                    getShade(360f + 180f * (bucket + 0.5f) / CRUMB_BUCKETS, base)
                )
            }
        }
    }

    // Shadow the rim casts into the scoop. Any stronger and it reads as a drawn
    // outline rather than as blocked light.
    val lipEnd = 0.14f.coerceAtMost(dFrac)
    if (lipEnd > 0.001f) {
        drawPath(
            craterBandPath(
                bx,
                by,
                h,
                squash,
                biteIndex,
                CRATER_SLOT_LIP,
                r,
                0f,
                r,
                lipEnd,
                craterRadiusAt(lipEnd, r, h)
            ),
            Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                startY = by - r * squash, endY = by + h * lipEnd
            )
        )
    }
}

/** Crumbs knocked onto the top face, so they sit outside the bite clip. Left
 *  unbatched: they are translucent, and one path would lose the overlap darkening. */
private fun DrawScope.drawBiteRim(bx: Float, by: Float, r: Float, squash: Float, biteIndex: Int) {
    if (r <= 4f) return

    rimCrumbData[biteIndex % rimCrumbData.size].forEach { c ->
        val a = c.x * TWO_PI
        val rad = r * (1.02f + c.y * 0.30f) * rimWobble(biteIndex, a)
        drawCircle(
            color = brownBase.copy(alpha = 0.75f),
            radius = 1.6f + c.y * 1.8f,
            center = Offset(bx + cos(a) * rad, by + sin(a) * rad * squash)
        )
    }
}

private class BiteSpot(val index: Int, val x: Float, val y: Float, val r: Float)

private fun biteSpots(
    cx: Float, cy: Float, rx: Float, ry: Float, startAngle: Float, sweep: Float,
    tip: Float, flank: Float
): List<BiteSpot> {
    val radLeft = Math.toRadians(startAngle.toDouble())
    return listOf(
        BiteSpot(0, cx, cy, tip),
        BiteSpot(
            1,
            cx + cos(radLeft).toFloat() * rx * 0.5f,
            cy + sin(radLeft).toFloat() * ry * 0.5f,
            flank
        )
    ).filter { it.r > 0.75f }
}

// Every constant here was tuned against a 1080px canvas, so rather than density
// conversions everywhere the whole scene draws inside one scale(). Height is in
// the unit too, so the cake still fits short or wide screens.
private const val DESIGN_WIDTH = 1080f

private fun designScale(width: Float, height: Float) =
    min(width, height * 0.5f) / DESIGN_WIDTH

private class CakeGeom(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val h: Float) {
    val squash get() = ry / rx
}

private fun cakeGeom(designW: Float, designH: Float, centerProgress: Float, bob: Float): CakeGeom {
    val rx = designW * 0.45f
    return CakeGeom(
        cx = designW / 2f,
        cy = designH * centerProgress + bob,
        rx = rx, ry = rx * 0.45f, h = 240f
    )
}

/** Shortest-arc rotation that swings the kept slice's midpoint round to 225 degrees. */
private fun animatedStartAngle(
    index: Int,
    sliceAngle: Float,
    keptIndex: Int?,
    focus: Float
): Float {
    val original = index * sliceAngle
    if (keptIndex != index || focus <= 0f) return original
    var diff = (225f - (original + sliceAngle / 2f)) % 360f
    if (diff > 180f) diff -= 360f
    if (diff <= -180f) diff += 360f
    return original + diff * focus
}

/**
 * The body is the top-face wedge swept straight down, so a point is inside when
 * something within h above it lands on that wedge. Testing the ellipse's near edge
 * instead reads a tap below the wedge as a top-face hit and resolves the wrong
 * angle, which made every bite tap miss.
 */
private fun pointInSlice(
    p: Offset, sx: Float, sy: Float, g: CakeGeom, startAngle: Float, sweep: Float
): Boolean {
    val nx = (p.x - sx) / g.rx
    if (abs(nx) > 1f) return false

    val steps = 48
    for (i in 0..steps) {
        val ny = (p.y - sy - g.h * i / steps) / g.ry
        if (nx * nx + ny * ny > 1f) continue
        var a = Math.toDegrees(atan2(ny.toDouble(), nx.toDouble())).toFloat()
        if (a < 0f) a += 360f
        if ((((a - startAngle) % 360f) + 360f) % 360f <= sweep) return true
    }
    return false
}

private fun hitSlice(
    p: Offset, g: CakeGeom, slices: Int, sliceAngle: Float, pulls: FloatArray
): Int? {
    var best: Int? = null
    var bestDepth = Float.NEGATIVE_INFINITY
    for (i in 0 until slices) {
        val start = i * sliceAngle
        val mid = Math.toRadians((start + sliceAngle / 2f).toDouble())
        val sx = g.cx + cos(mid).toFloat() * pulls[i]
        val sy = g.cy + sin(mid).toFloat() * pulls[i] * g.squash
        if (!pointInSlice(p, sx, sy, g, start, sliceAngle)) continue
        // Nearest to the camera wins, matching the painter's-algorithm draw order
        val depth = sin(mid).toFloat()
        if (depth > bestDepth) {
            bestDepth = depth
            best = i
        }
    }
    return best
}

private class FocusXf(val pivot: Offset, val scale: Float, val trans: Offset)

/** Shared so the bite hit test can invert exactly what the renderer applied. */
private fun focusTransform(
    sx: Float, sy: Float, midRad: Double, g: CakeGeom,
    focus: Float, designW: Float, designH: Float, bob: Float
): FocusXf {
    val pivotX = sx + cos(midRad).toFloat() * (g.rx * 0.65f)
    val pivotY = sy + sin(midRad).toFloat() * (g.ry * 0.65f) + g.h * 0.5f
    return FocusXf(
        pivot = Offset(pivotX, pivotY),
        scale = 1f + focus * 0.55f,
        trans = Offset(
            (designW / 2f - pivotX) * focus,
            (designH / 2f + bob - pivotY) * focus
        )
    )
}

private fun FocusXf.invert(p: Offset) = Offset(
    (p.x - trans.x - pivot.x) / scale + pivot.x,
    (p.y - trans.y - pivot.y) / scale + pivot.y
)

@Composable
fun BirthdayCake(modifier: Modifier = Modifier) {
    MatchStatusBarToPage(pageBackground)

    val slices = 6
    val sliceAngle = 360f / slices

    val explodedStates = remember { mutableStateListOf(*Array(slices) { false }) }
    var keptSliceIndex by remember { mutableStateOf<Int?>(null) }
    var isCenteringPiece by remember { mutableStateOf(false) }

    // Tip, then flank, and that second one is the terminal state of the scene
    val maxBites = 2
    var bitesTaken by remember { mutableIntStateOf(0) }

    // Let the slice clear the cake before it starts flying to centre
    LaunchedEffect(keptSliceIndex) {
        if (keptSliceIndex != null) {
            delay(450)
            isCenteringPiece = true
        }
    }

    val scratchPath = remember { Path() }
    var scratchTrigger by remember { mutableIntStateOf(0) }
    val layerPaint = remember { androidx.compose.ui.graphics.Paint() }

    // A fresh Paint and BlurMaskFilter per shadow per frame was the scene's most
    // expensive allocation.
    val shadowPaint = remember { androidx.compose.ui.graphics.Paint() }
    val blurCache = remember { mutableMapOf<Int, android.graphics.BlurMaskFilter>() }
    val volumeCache = remember { VolumeCache() }

    // derivedStateOf so hundreds of drag ticks only recompose on the one frame the
    // threshold is crossed
    val isRevealed by remember { derivedStateOf { scratchTrigger > 120 } }

    val cakeCenterProgress by animateFloatAsState(
        targetValue = if (isRevealed) 0.50f else 0.75f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "cake_center"
    )

    val layerAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 1f,
        animationSpec = tween(800),
        label = "layer_alpha"
    )

    val globalShadowAlpha by animateFloatAsState(
        targetValue = if (keptSliceIndex != null) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "global_shadow"
    )

    val keptShadowAlpha by animateFloatAsState(
        targetValue = when {
            keptSliceIndex == null -> 1f
            !isCenteringPiece -> 0f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = if (isCenteringPiece) 1000 else 400,
            delayMillis = if (isCenteringPiece) 200 else 0,
            easing = EaseInOutCubic
        ),
        label = "kept_shadow_alpha"
    )

    val focusProgress by animateFloatAsState(
        targetValue = if (isCenteringPiece) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "focus_progress"
    )

    val pullAnimations = List(slices) { index ->
        val shouldScatter = keptSliceIndex != null && keptSliceIndex != index
        val isKept = keptSliceIndex == index

        animateFloatAsState(
            targetValue = when {
                shouldScatter -> 2500f
                isKept && isCenteringPiece -> 0f
                explodedStates[index] -> 55f
                else -> 0f
            },
            animationSpec = if (shouldScatter) tween(
                1200,
                easing = FastOutSlowInEasing
            ) else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
            label = "pull_anim_$index"
        )
    }
    val currentPulls by rememberUpdatedState(pullAnimations)

    val biteSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 700f)
    val tipBite by animateFloatAsState(
        if (bitesTaken >= 1) 110f else 0f,
        biteSpring,
        label = "bite_tip"
    )
    val flankBite by animateFloatAsState(
        if (bitesTaken >= 2) 135f else 0f,
        biteSpring,
        label = "bite_flank"
    )

    val biteRecoil = remember { Animatable(0f) }
    LaunchedEffect(bitesTaken) {
        if (bitesTaken == 0) {
            biteRecoil.snapTo(0f)
            return@LaunchedEffect
        }
        biteRecoil.snapTo(1f)
        biteRecoil.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 1200f))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "environment")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cake_hover"
    )

    val flickerFast by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(130, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "fire_fast"
    )
    val flickerSlow by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(211, easing = LinearEasing), RepeatMode.Reverse),
        label = "fire_slow"
    )
    val fireSway by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(350, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fire_sway"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                fun unit() = designScale(size.width.toFloat(), size.height.toFloat())

                fun geomFor(u: Float) =
                    cakeGeom(size.width / u, size.height / u, cakeCenterProgress, floatAnim)

                fun sliceAt(tap: Offset): Int? {
                    val u = unit()
                    return hitSlice(
                        Offset(tap.x / u, tap.y / u), geomFor(u), slices, sliceAngle,
                        FloatArray(slices) { currentPulls[it].value }
                    )
                }

                fun tappedKeptSlice(tap: Offset): Boolean {
                    val kept = keptSliceIndex ?: return false
                    val u = unit()
                    val g = geomFor(u)
                    val designW = size.width / u
                    val designH = size.height / u
                    val start = animatedStartAngle(kept, sliceAngle, kept, focusProgress)
                    val midRad = Math.toRadians((start + sliceAngle / 2f).toDouble())
                    val pull = currentPulls[kept].value
                    val sx = g.cx + cos(midRad).toFloat() * pull
                    val sy = g.cy + sin(midRad).toFloat() * pull * g.squash
                    val xf = focusTransform(
                        sx,
                        sy,
                        midRad,
                        g,
                        focusProgress,
                        designW,
                        designH,
                        floatAnim
                    )
                    return pointInSlice(
                        xf.invert(Offset(tap.x / u, tap.y / u)), sx, sy, g, start, sliceAngle
                    )
                }

                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (isRevealed && keptSliceIndex == null) {
                            sliceAt(tapOffset)?.let { if (explodedStates[it]) keptSliceIndex = it }
                        }
                    },
                    onTap = { tapOffset ->
                        when {
                            keptSliceIndex == null ->
                                sliceAt(tapOffset)?.let { explodedStates[it] = !explodedStates[it] }

                            isCenteringPiece && bitesTaken < maxBites && tappedKeptSlice(tapOffset) ->
                                bitesTaken++
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!isRevealed) {
                            val u = designScale(size.width.toFloat(), size.height.toFloat())
                            scratchPath.moveTo(offset.x / u, offset.y / u)
                            scratchTrigger++
                        }
                    },
                    onDrag = { change, _ ->
                        if (!isRevealed) {
                            change.consume()
                            val u = designScale(size.width.toFloat(), size.height.toFloat())
                            scratchPath.lineTo(change.position.x / u, change.position.y / u)
                            scratchTrigger++
                        }
                    }
                )
            }
    ) {
        drawRect(color = pageBackground, size = size)

        val u = designScale(size.width, size.height)
        val designW = size.width / u
        val designH = size.height / u

        withTransform({ scale(u, u, pivot = Offset.Zero) }) {
            val g = cakeGeom(designW, designH, cakeCenterProgress, floatAnim)
            val cx = g.cx
            val cy = g.cy
            val rx = g.rx
            val ry = g.ry
            val h = g.h

            val shadowProjX = -35f
            val shadowProjY = 20f

            val candleY = designH * 0.28f
            val organicFlicker = (flickerFast + flickerSlow) / 2f

            val animatedStartAngles = FloatArray(slices) {
                animatedStartAngle(it, sliceAngle, keptSliceIndex, focusProgress)
            }

            if (layerAlpha > 0.01f) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FFB300).copy(alpha = 0.2f * layerAlpha),
                            Color(0x11FF9800).copy(alpha = 0.07f * layerAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx, candleY),
                        radius = designW * 1.2f * organicFlicker
                    ),
                    topLeft = Offset(cx - designW * 1.2f, candleY - designW * 1.2f),
                    size = Size(designW * 2.4f, designW * 2.4f)
                )
            }

            if (globalShadowAlpha > 0.01f) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x77000000).copy(alpha = 0.46f * globalShadowAlpha),
                            Color(0x22000000).copy(alpha = 0.13f * globalShadowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx + rx * 0.1f, cy + h + ry * 0.2f),
                        radius = rx * 1.5f
                    ),
                    topLeft = Offset(
                        cx - rx * 1.2f + shadowProjX,
                        cy + h - ry * 0.8f + shadowProjY
                    ),
                    size = Size(rx * 2.6f, ry * 2.2f)
                )
            }

            for (index in 0 until slices) {
                val pullDist = currentPulls[index].value
                val isKept = keptSliceIndex == index

                if (pullDist > 0.5f || isKept) {
                    val currentStartAngle = animatedStartAngles[index]
                    val rad = Math.toRadians((currentStartAngle + sliceAngle / 2f).toDouble())

                    val sliceCx = cx + cos(rad).toFloat() * pullDist
                    val sliceCy = cy + sin(rad).toFloat() * pullDist * g.squash

                    val currentScale = if (isKept) 1f + (focusProgress * 0.55f) else 1f

                    val currentProjX =
                        if (isKept) shadowProjX * (1f - focusProgress) else shadowProjX * (pullDist / 55f).coerceIn(
                            0f,
                            1f
                        )
                    val currentProjY =
                        if (isKept) shadowProjY * (1f - focusProgress) + (120f * focusProgress) else shadowProjY * (pullDist / 55f).coerceIn(
                            0f,
                            1f
                        )

                    val blurRadius = if (isKept) {
                        (15f + (focusProgress * 80f)).coerceAtLeast(1f)
                    } else {
                        (15f + (pullDist.coerceIn(0f, 150f) * 0.1f)).coerceAtLeast(1f)
                    }

                    val shadowFade =
                        if (isKept) keptShadowAlpha else (1f - (pullDist / 1500f)).coerceIn(0f, 1f)

                    if (shadowFade > 0.01f) {
                        val alphaStrength = if (isKept) 0.35f - (focusProgress * 0.17f) else 0.35f
                        val xf = focusTransform(
                            sliceCx,
                            sliceCy,
                            rad,
                            g,
                            focusProgress,
                            designW,
                            designH,
                            floatAnim
                        )

                        withTransform({
                            if (isKept && focusProgress > 0f) {
                                translate(xf.trans.x, xf.trans.y)
                                scale(currentScale, currentScale, pivot = xf.pivot)
                            }

                            translate(currentProjX, currentProjY)

                            if (isKept && focusProgress > 0f) {
                                val shadowCenterY = sliceCy + h + sin(rad).toFloat() * (ry * 0.65f)
                                val shadowScale = 1f - (focusProgress * 0.25f)
                                scale(
                                    shadowScale,
                                    shadowScale,
                                    pivot = Offset(xf.pivot.x, shadowCenterY)
                                )
                            }
                        }) {
                            footprintScratch.reset()
                            footprintScratch.moveTo(sliceCx, sliceCy + h)
                            footprintScratch.arcTo(
                                Rect(
                                    sliceCx - rx,
                                    sliceCy + h - ry,
                                    sliceCx + rx,
                                    sliceCy + h + ry
                                ),
                                currentStartAngle, sliceAngle, forceMoveTo = false
                            )
                            footprintScratch.close()

                            shadowPaint.color = Color.Black.copy(alpha = alphaStrength * shadowFade)
                            val blurKey = blurRadius.roundToInt().coerceAtLeast(1)
                            shadowPaint.asFrameworkPaint().maskFilter =
                                blurCache.getOrPut(blurKey) {
                                    android.graphics.BlurMaskFilter(
                                        blurKey.toFloat(),
                                        android.graphics.BlurMaskFilter.Blur.NORMAL
                                    )
                                }
                            drawContext.canvas.drawPath(footprintScratch, shadowPaint)
                        }
                    }
                }
            }

            val sortedSlices = (0 until slices).sortedBy { index ->
                sin(Math.toRadians((animatedStartAngles[index] + sliceAngle / 2f).toDouble()))
            }

            for (index in sortedSlices) {
                val pullDist = currentPulls[index].value
                val leftPullDist = currentPulls[(index - 1 + slices) % slices].value
                val rightPullDist = currentPulls[(index + 1) % slices].value
                val isKept = keptSliceIndex == index

                val currentStartAngle = animatedStartAngles[index]
                val currentMidDeg = currentStartAngle + sliceAngle / 2f
                val rad = Math.toRadians(currentMidDeg.toDouble())

                val currentScale = if (isKept) 1f + (focusProgress * 0.55f) else 1f

                if (pullDist < 2000f || isKept) {
                    val sliceCx = cx + cos(rad).toFloat() * pullDist
                    val sliceCy = cy + sin(rad).toFloat() * pullDist * g.squash

                    val xf = focusTransform(
                        sliceCx,
                        sliceCy,
                        rad,
                        g,
                        focusProgress,
                        designW,
                        designH,
                        floatAnim
                    )

                    withTransform({
                        if (isKept && focusProgress > 0f) {
                            translate(xf.trans.x, xf.trans.y)
                            scale(currentScale, currentScale, pivot = xf.pivot)
                        }

                        val recoil = biteRecoil.value
                        if (isKept && recoil > 0.001f) {
                            translate(0f, recoil * 14f)
                            scale(
                                1f + recoil * 0.03f, 1f - recoil * 0.05f,
                                pivot = Offset(xf.pivot.x, xf.pivot.y + h * 0.5f)
                            )
                        }
                    }) {

                        val squash = g.squash
                        val bites = if (isKept) {
                            biteSpots(
                                sliceCx, sliceCy, rx, ry, currentStartAngle, sliceAngle,
                                tipBite, flankBite
                            )
                        } else emptyList()

                        val bitePath = biteScratch.apply {
                            reset()
                            bites.forEach { spot ->
                                addPath(
                                    craterCache.path(
                                        spot.r,
                                        CRATER_SLOT_SILHOUETTE,
                                        spot.index
                                    ) { p ->
                                        p.addCrater(0f, 0f, spot.r, h, squash, spot.index)
                                    },
                                    Offset(spot.x, spot.y)
                                )
                            }
                        }

                        val cakeVolumePath = volumeScratch.apply {
                            reset()
                            if (bites.isNotEmpty()) {
                                addPath(
                                    volumeCache.localVolume(
                                        currentStartAngle,
                                        sliceAngle,
                                        rx,
                                        ry,
                                        h
                                    ),
                                    Offset(sliceCx, sliceCy)
                                )
                            }
                        }

                        clipPath(
                            bitePath,
                            clipOp = androidx.compose.ui.graphics.ClipOp.Difference
                        ) {
                            draw3DSlice(
                                cx = sliceCx,
                                cy = sliceCy,
                                rx = rx,
                                ry = ry,
                                h = h,
                                startAngle = currentStartAngle,
                                sweepAngle = sliceAngle,
                                pullDist = pullDist,
                                leftPullDist = leftPullDist,
                                rightPullDist = rightPullDist
                            )
                        }

                        if (bites.isNotEmpty()) {
                            clipPath(
                                cakeVolumePath,
                                clipOp = androidx.compose.ui.graphics.ClipOp.Intersect
                            ) {
                                // Backing so no sliver of page can show through a scoop
                                drawPath(bitePath, getShade(135f, brownBase))

                                clipPath(
                                    bitePath,
                                    clipOp = androidx.compose.ui.graphics.ClipOp.Intersect
                                ) {
                                    // Back to front, so overlapping mouthfuls keep their depth order
                                    bites.sortedBy { it.y }.forEach {
                                        drawCraterInterior(it.x, it.y, it.r, h, squash, it.index)
                                    }
                                }

                                bites.forEach { drawBiteRim(it.x, it.y, it.r, squash, it.index) }
                            }
                        }

                        drawCherry(sliceCx, sliceCy, rx, ry, currentMidDeg)
                    }
                }
            }

            if (layerAlpha > 0.01f) {
                layerPaint.alpha = layerAlpha
                drawContext.canvas.saveLayer(Rect(0f, 0f, designW, designH), layerPaint)

                val candleSpacing = 220f
                val massiveScale = 3.4f

                drawWaxCandle3(
                    Offset(cx - candleSpacing, candleY),
                    organicFlicker,
                    fireSway,
                    massiveScale
                )
                drawWaxCandle7(
                    Offset(cx + candleSpacing, candleY),
                    organicFlicker,
                    fireSway,
                    massiveScale
                )

                if (scratchTrigger > 0) {
                    drawPath(
                        path = scratchPath,
                        color = Color.Black,
                        style = Stroke(
                            width = 150f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = BlendMode.Clear
                    )
                }

                drawContext.canvas.restore()
            }
        }
    }
}

private fun DrawScope.drawWaxCandle3(
    center: Offset,
    flickerScale: Float,
    sway: Float,
    globalScale: Float
) {
    withTransform({
        translate(center.x, center.y)
        scale(scaleX = globalScale, scaleY = globalScale, pivot = Offset.Zero)
    }) {
        drawCandleDropShadow(path3)
        drawWaxBody(path3)
        drawWickAndFire(Offset(8f, -75f), flickerScale, sway)
    }
}

private fun DrawScope.drawWaxCandle7(
    center: Offset,
    flickerScale: Float,
    sway: Float,
    globalScale: Float
) {
    withTransform({
        translate(center.x, center.y)
        scale(scaleX = globalScale, scaleY = globalScale, pivot = Offset.Zero)
    }) {
        drawCandleDropShadow(path7)
        drawWaxBody(path7)
        drawWickAndFire(Offset(10f, -75f), flickerScale, sway)
    }
}

private val path3 = Path().apply {
    moveTo(-25f, -65f)
    cubicTo(45f, -75f, 55f, -10f, 0f, 0f)
    cubicTo(65f, 15f, 50f, 85f, -30f, 75f)
}

private val path7 = Path().apply {
    moveTo(-35f, -65f)
    quadraticTo(5f, -80f, 40f, -65f)
    cubicTo(40f, -30f, 10f, 20f, -20f, 80f)
}

private fun DrawScope.drawWaxBody(path: Path) {
    drawPath(
        path = path,
        color = Color(0xFFC62828),
        style = Stroke(width = 42f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFFFFFFF)),
            start = Offset(-40f, 0f), end = Offset(40f, 0f)
        ),
        style = Stroke(width = 32f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCandleDropShadow(path: Path) {
    withTransform({ translate(-8f, 15f) }) {
        drawPath(
            path = path,
            color = Color(0x1A000000),
            style = Stroke(width = 42f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawWickAndFire(base: Offset, scale: Float, sway: Float) {
    flameScratch.reset()
    flameScratch.moveTo(base.x, base.y)
    flameScratch.quadraticTo(base.x - 5f, base.y - 10f, base.x + 2f, base.y - 18f)
    drawPath(
        path = flameScratch,
        color = Color(0xFF212121),
        style = Stroke(width = 4f, cap = StrokeCap.Round)
    )

    val fireBase = Offset(base.x + 2f, base.y - 16f)

    fun drawTeardrop(height: Float, width: Float, color: Color) {
        flameScratch.reset()
        flameScratch.moveTo(fireBase.x, fireBase.y)
        flameScratch.quadraticTo(
            fireBase.x + width,
            fireBase.y - height * 0.4f,
            fireBase.x + sway,
            fireBase.y - height
        )
        flameScratch.quadraticTo(
            fireBase.x - width,
            fireBase.y - height * 0.4f,
            fireBase.x,
            fireBase.y
        )
        flameScratch.close()
        drawPath(path = flameScratch, color = color)
    }

    drawTeardrop(height = 55f * scale, width = 20f * scale, color = Color(0x66FF5722))
    drawTeardrop(height = 45f * scale, width = 14f * scale, color = Color(0xFFFFC107))
    drawTeardrop(height = 25f * scale, width = 8f * scale, color = Color(0xFFFFF9C4))
    drawTeardrop(height = 10f, width = 6f, color = Color(0x9929B6F6))
}

private fun DrawScope.drawCherry(
    cx: Float, cy: Float, rx: Float, ry: Float, midAngle: Float
) {
    val anchorX = cx + cos(Math.toRadians(midAngle.toDouble())).toFloat() * rx * 0.72f
    val anchorY = cy + sin(Math.toRadians(midAngle.toDouble())).toFloat() * ry * 0.72f
    val chCy = anchorY - 55f

    withTransform({
        translate(anchorX, anchorY)
        rotate(150f)
    }) {
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0x887D161A), Color(0x227D161A), Color.Transparent),
                startX = 0f, endX = 60f
            ),
            topLeft = Offset(0f, -8f),
            size = Size(60f, 16f)
        )
    }

    cherryScratch.reset()
    cherryScratch.moveTo(anchorX - 32f, chCy - 54f)
    cherryScratch.quadraticTo(anchorX - 75f, chCy - 77f, anchorX - 90f, chCy - 37f)
    cherryScratch.quadraticTo(anchorX - 60f, chCy - 17f, anchorX - 32f, chCy - 54f)
    cherryScratch.close()
    drawPath(
        path = cherryScratch,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFAED581), Color(0xFF33691E)),
            start = Offset(anchorX - 90f, chCy - 77f), end = Offset(anchorX - 32f, chCy - 17f)
        )
    )

    cherryScratch.reset()
    cherryScratch.moveTo(anchorX, chCy - 15f)
    cherryScratch.quadraticTo(anchorX - 10f, chCy - 62f, anchorX - 40f, chCy - 82f)
    drawPath(
        path = cherryScratch,
        color = Color(0xFF689F38), style = Stroke(width = 4f, cap = StrokeCap.Round)
    )

    cherryScratch.reset()
    cherryScratch.moveTo(anchorX, chCy - 15f)
    cherryScratch.cubicTo(
        anchorX - 15f, chCy - 32f,
        anchorX - 45f, chCy - 17f, anchorX - 42f, chCy + 18f
    )
    cherryScratch.cubicTo(anchorX - 42f, chCy + 48f, anchorX - 20f, chCy + 60f, anchorX, chCy + 60f)
    cherryScratch.cubicTo(
        anchorX + 20f, chCy + 60f,
        anchorX + 42f, chCy + 48f, anchorX + 42f, chCy + 18f
    )
    cherryScratch.cubicTo(anchorX + 45f, chCy - 17f, anchorX + 15f, chCy - 32f, anchorX, chCy - 15f)
    cherryScratch.close()
    drawPath(
        path = cherryScratch,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF5252), Color(0xFFB70014), Color(0xFF4A0005)),
            center = Offset(anchorX - 15f, chCy - 2f), radius = 45f
        )
    )

    withTransform({ rotate(15f, Offset(anchorX + 13f, chCy - 9f)) }) {
        drawOval(
            color = Color(0xAAFFFFFF),
            topLeft = Offset(anchorX + 13f, chCy - 9f),
            size = Size(10f, 16f)
        )
    }
}

private fun DrawScope.draw3DSlice(
    cx: Float, cy: Float, rx: Float, ry: Float, h: Float,
    startAngle: Float, sweepAngle: Float,
    pullDist: Float, leftPullDist: Float, rightPullDist: Float
) {
    val midAngle = startAngle + sweepAngle / 2f

    val showLeftWall = pullDist > 0.5f || leftPullDist > 0.5f
    val showRightWall = pullDist > 0.5f || rightPullDist > 0.5f

    val expandLeft = if (!showLeftWall) 0.8f else 0f
    val expandRight = if (!showRightWall) 0.8f else 0f
    val pushBackDist = if (!showLeftWall || !showRightWall) 1.5f else 0f

    val expandedTopCenter = Offset(
        cx - cos(Math.toRadians(midAngle.toDouble())).toFloat() * pushBackDist,
        cy - sin(Math.toRadians(midAngle.toDouble())).toFloat() * (ry / rx) * pushBackDist
    )
    val trueTopCenter = Offset(cx, cy)

    fun proj(angle: Float): Offset {
        val rad = Math.toRadians(angle.toDouble())
        return Offset(cx + cos(rad).toFloat() * rx, cy + sin(rad).toFloat() * ry)
    }

    val topStart = proj(startAngle)
    val topEnd = proj(startAngle + sweepAngle)

    val drawCutWall = { wall: Int, pTopEdge: Offset, normal: Float ->
        val edgeX = pTopEdge.x - cx
        val edgeY = pTopEdge.y - cy

        val drawLayer = { index: Int, f1: Float, f2: Float, color: Color, isSponge: Boolean ->
            wallScratch.reset()
            wallScratch.moveTo(cx, cy + h * f1)
            wallScratch.lineTo(pTopEdge.x, pTopEdge.y + h * f1)
            wallScratch.lineTo(pTopEdge.x, pTopEdge.y + h * f2)
            wallScratch.lineTo(cx, cy + h * f2)
            wallScratch.close()
            drawPath(path = wallScratch, color = getShade(normal, color))

            if (isSponge) {
                val cached = crumbCache.path(rx, startAngle, wall, index, 0) { path ->
                    crumbData.forEach { crumb ->
                        val x = crumb.x * edgeX
                        val yTop = crumb.x * edgeY + h * f1
                        val yBot = crumb.x * edgeY + h * f2
                        path.addCrumb(x, yTop + crumb.y * (yBot - yTop))
                    }
                }
                crumbScratch.reset()
                crumbScratch.addPath(cached, Offset(cx, cy))
                drawPath(crumbScratch, getShade(normal, crumbColor))
            }
        }

        cakeLayers.forEachIndexed { i, l -> drawLayer(i, l.top, l.bottom, l.color, l.isSponge) }
    }

    class Wall(val depth: Float, val drawCommand: () -> Unit)

    val walls = mutableListOf<Wall>()

    val leftWallDepth = (trueTopCenter.y + topStart.y) / 2f
    val rightWallDepth = (trueTopCenter.y + topEnd.y) / 2f
    val crustDepth = proj(midAngle).y

    if (showLeftWall) walls.add(Wall(leftWallDepth) {
        drawCutWall(
            WALL_LEFT_CUT,
            topStart,
            startAngle - 90f
        )
    })
    if (showRightWall) walls.add(Wall(rightWallDepth) {
        drawCutWall(
            WALL_RIGHT_CUT,
            topEnd,
            startAngle + sweepAngle + 90f
        )
    })

    walls.add(Wall(crustDepth) {
        val drawCrustLayer =
            { index: Int, f1: Float, f2: Float, baseColor: Color, isSponge: Boolean ->
                val rectTop = Rect(cx - rx, cy + h * f1 - ry, cx + rx, cy + h * f1 + ry)
                val rectBot = Rect(cx - rx, cy + h * f2 - ry, cx + rx, cy + h * f2 + ry)

                val crustStartAngle = startAngle - expandLeft
                val crustSweepAngle = sweepAngle + expandLeft + expandRight
                val crustEndAngle = crustStartAngle + crustSweepAngle

                crustScratch.reset()
                crustScratch.arcTo(rectTop, crustStartAngle, crustSweepAngle, forceMoveTo = true)
                crustScratch.lineTo(
                    cx + cos(Math.toRadians(crustEndAngle.toDouble())).toFloat() * rx,
                    cy + h * f2 + sin(Math.toRadians(crustEndAngle.toDouble())).toFloat() * ry
                )
                crustScratch.arcTo(rectBot, crustEndAngle, -crustSweepAngle, forceMoveTo = false)
                crustScratch.close()

                // Two-stop approximation of the normal sweeping round the curve
                drawPath(
                    path = crustScratch,
                    brush = crumbCache.crustBrush(rx, startAngle, cx, index) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                getShade(startAngle, baseColor),
                                getShade(startAngle + sweepAngle, baseColor)
                            ),
                            startX = topStart.x, endX = topEnd.x
                        )
                    }
                )

                if (isSponge) {
                    // Curved face, so the tint varies along it: one draw per bucket.
                    for (bucket in 0 until CRUMB_BUCKETS) {
                        val cached =
                            crumbCache.path(rx, startAngle, WALL_CRUST, index, bucket) { path ->
                                crustCrumbData.forEach { crumb ->
                                    if (bucketOf(crumb.x) != bucket) return@forEach
                                    val rad =
                                        Math.toRadians((startAngle + crumb.x * sweepAngle).toDouble())
                                    val x = cos(rad).toFloat() * rx
                                    val yTop = sin(rad).toFloat() * ry + h * f1
                                    val yBot = sin(rad).toFloat() * ry + h * f2
                                    path.addCrumb(x, yTop + crumb.y * (yBot - yTop))
                                }
                            }
                        if (cached.isEmpty) continue
                        crumbScratch.reset()
                        crumbScratch.addPath(cached, Offset(cx, cy))
                        drawPath(
                            crumbScratch,
                            getShade(
                                startAngle + sweepAngle * (bucket + 0.5f) / CRUMB_BUCKETS,
                                crumbColor
                            )
                        )
                    }
                }
            }

        cakeLayers.forEachIndexed { i, l ->
            drawCrustLayer(
                i,
                l.top,
                l.bottom,
                l.color,
                l.isSponge
            )
        }
    })

    walls.sortedBy { it.depth }.forEach { it.drawCommand() }

    topFaceScratch.reset()
    topFaceScratch.moveTo(expandedTopCenter.x, expandedTopCenter.y)
    topFaceScratch.arcTo(
        Rect(cx - rx, cy - ry, cx + rx, cy + ry),
        startAngle - expandLeft, sweepAngle + expandLeft + expandRight, forceMoveTo = false
    )
    topFaceScratch.close()

    drawPath(
        path = topFaceScratch,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF4B8D2), pinkBase),
            start = Offset(cx + rx, cy - ry), end = Offset(cx - rx, cy + ry)
        )
    )
}

@Composable
fun BirthdayCakeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BirthdayCake(modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Birthday Cake", showBackground = true)
@Composable
fun BirthdayCakePreview() {
    BirthdayCakeScreen()
}
