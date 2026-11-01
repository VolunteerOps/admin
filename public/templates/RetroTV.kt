import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ──────────────────────────────────────────────────────────────────────────────
// Color Palette
//
// Every surface is lit from the top-left corner toward the bottom-right.
// Colors are grouped by the physical part they belong to so the lighting
// model stays consistent when you tweak any single material.
// ──────────────────────────────────────────────────────────────────────────────

// Wall gradient behind the TV
private val WallTop = Color(0xFFDB9BD7)
private val WallBottom = Color(0xFFA877B6)

// Outer plastic shell (highlight → deep shadow follows light direction)
private val TvShellHighlight = Color(0xFF8D94E8)
private val TvShellMidLight = Color(0xFF646AA8)
private val TvShellMidDark = Color(0xFF434778)
private val TvShellDeepShadow = Color(0xFF1E203A)
private val TvBlueDeepExtrusionColor = Color(0xFF181A30)

// Neon tube that frames the inner panel
private val NeonPipeEdge = Color(0xFFFF5CE2)
private val NeonPipeCenter = Color(0xFFFFAEEB)
private val NeonGlowColor = Color(0x30FF5CE2)

// Recessed holes and deep cavities
private val HoleHoleDarkColor = Color(0xFF0F1122)
private val HoleDeepColor = Color(0xFF070812)

// Pink push-buttons and speaker grille
private val PinkControlLightReal = Color(0xFFFF82E8)
private val PinkControlMainReal = Color(0xFFE25BCA)
private val PinkControlDarkReal = Color(0xFFAD2794)

// Power LED jewel
private val LedHousingRing = Color(0xFF111324)
private val LedBaseShadow = Color(0x90000000)
private val RubyDarkRoot = Color(0xFF5A001E)
private val RubyMidGlow = Color(0xFFD61868)
private val RubyHotTip = Color(0xFFFF48A5)
private val SpecularHardWhite = Color(0xFFFFFFFF)
private val SpecularSoftWhite = Color(0x60FFFFFF)
private val CausticPinkRim = Color(0xFFFF8CE0)

// Machined channel-selector knob and silkscreen icons
private val KnobSpunSpec = Color(0xFFFFFFFF)
private val KnobSpunMid = Color(0xFFE862D0)
private val KnobSpunDark = Color(0xFF7A0D63)
private val SilkscreenInk = Color(0xFF1E203D)

// Telescoping chrome antenna rods
private val ChromeDark = Color(0xFF2B2F3D)
private val ChromeMid = Color(0xFF8E96A8)
private val ChromeSpec = Color(0xFFFFFFFF)

// Brushed-metal stand feet
private val MetalFootDark = Color(0xFF202330)
private val MetalFootMid = Color(0xFF6E7588)
private val MetalFootLight = Color(0xFFC5CCDE)
private val RubberPadDark = Color(0xFF0A0B10)

// CRT screen bezel and dead-screen fill
private val BezelInnerReal = Color(0xFF232647)
private val ScreenDarkFill = Color(0xFF24272E)

// Baseline color for the floor glow when no video is playing
private val FloorBaselineColor = Color(0xFFCCD5FF)

// Pre-computed static noise grain levels.
// Avoids re-blending alpha inside the 64x48 draw loop (~3 072 cells/frame).
private val StaticBright = Color(0xFFF2F2F2).copy(alpha = 0.94f)
private val StaticMid = Color(0xFF9E9E9E).copy(alpha = 0.82f)
private val StaticDim = Color(0xFF3B3B3B).copy(alpha = 0.88f)
private val StaticDark = Color(0xFF0D0D0D).copy(alpha = 0.96f)

// ──────────────────────────────────────────────────────────────────────────────
// TV Power States
// ──────────────────────────────────────────────────────────────────────────────

/** Screen is off. Tap the CRT to advance to [TV_COLOR_BARS]. */
private const val TV_OFF = 0

/** SMPTE color bars are displayed. Tap again to advance to [TV_STATIC_OR_VIDEO]. */
private const val TV_COLOR_BARS = 1

/** RF static plays until the video stream buffers, then video takes over. */
private const val TV_STATIC_OR_VIDEO = 2

/** Total number of states the power cycle wraps through. */
private const val TV_STATE_COUNT = 3

// ──────────────────────────────────────────────────────────────────────────────
// SMPTE Color Bars
// ──────────────────────────────────────────────────────────────────────────────

/** Standard SMPTE/EIA color bar order: white, yellow, cyan, green, magenta, red, blue. */
private val SmpteBarColors = listOf(
    Color(0xFFBFBFBF), Color(0xFFBFBF00), Color(0xFF00BFBF), Color(0xFF00BF00),
    Color(0xFFBF00BF), Color(0xFFBF0000), Color(0xFF0000BF),
)

// ──────────────────────────────────────────────────────────────────────────────
// Geometry Helpers
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Builds a barrel-distorted rounded rectangle [Path].
 *
 * Each straight edge is replaced by a quadratic curve that bows outward by [bow] pixels,
 * producing the convex CRT glass look. Corners remain circular arcs of radius [cornerRadius].
 *
 * @param offsetX Horizontal origin of the rectangle.
 * @param offsetY Vertical origin of the rectangle.
 */
private fun createBarrelPath(
    width: Float,
    height: Float,
    cornerRadius: Float,
    bow: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
): Path {
    val right = offsetX + width
    val bottom = offsetY + height
    return Path().apply {
        moveTo(x = offsetX + cornerRadius, y = offsetY)
        quadraticTo(
            x1 = offsetX + width / 2f,
            y1 = offsetY - bow,
            x2 = right - cornerRadius,
            y2 = offsetY
        )
        arcTo(
            rect = Rect(
                left = right - 2 * cornerRadius,
                top = offsetY,
                right = right,
                bottom = offsetY + 2 * cornerRadius
            ), startAngleDegrees = 270f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        quadraticTo(
            x1 = right + bow,
            y1 = offsetY + height / 2f,
            x2 = right,
            y2 = bottom - cornerRadius
        )
        arcTo(
            rect = Rect(
                left = right - 2 * cornerRadius,
                top = bottom - 2 * cornerRadius,
                right = right,
                bottom = bottom
            ), startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        quadraticTo(
            x1 = offsetX + width / 2f,
            y1 = bottom + bow,
            x2 = offsetX + cornerRadius,
            y2 = bottom
        )
        arcTo(
            rect = Rect(
                left = offsetX,
                top = bottom - 2 * cornerRadius,
                right = offsetX + 2 * cornerRadius,
                bottom = bottom
            ), startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        quadraticTo(
            x1 = offsetX - bow,
            y1 = offsetY + height / 2f,
            x2 = offsetX,
            y2 = offsetY + cornerRadius
        )
        arcTo(
            rect = Rect(
                left = offsetX,
                top = offsetY,
                right = offsetX + 2 * cornerRadius,
                bottom = offsetY + 2 * cornerRadius
            ), startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false
        )
        close()
    }
}

/**
 * Compose [Shape] backed by [createBarrelPath].
 * Used to clip the [AndroidView] video layer to match the CRT glass curvature.
 */
private val GlassBarrelShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { 14.dp.toPx() }
        val bow = with(density) { 3.2.dp.toPx() }
        val path = createBarrelPath(
            width = size.width,
            height = size.height,
            cornerRadius = r,
            bow = bow,
        )
        return Outline.Generic(path)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CRT Warm-up Curves
//
// A real CRT warms up in two visible phases:
//   1. Horizontal deflection locks first  → thin bright slit across the middle.
//   2. Vertical deflection ramps up       → slit expands to fill the screen.
// These three functions map the 0..1 power animation progress to scale and
// brightness values that reproduce that sequence.
// ──────────────────────────────────────────────────────────────────────────────

/** Maps power [progress] to the vertical CRT scale (thin slit → full height). */
private fun crtScaleY(progress: Float): Float = when {
    progress > 0.35f -> ((progress - 0.35f) / 0.65f).coerceAtLeast(0.008f)
    progress > 0.08f -> 0.008f
    else -> (progress / 0.08f) * 0.008f
}

/** Maps power [progress] to the horizontal CRT scale (hairline → full width). */
private fun crtScaleX(progress: Float): Float = when {
    progress > 0.35f -> 1f
    progress > 0.08f -> ((progress - 0.08f) / 0.27f).coerceAtLeast(0.015f)
    else -> (progress / 0.08f) * 0.015f
}

/** Maps power [progress] to the phosphor burn-in flash alpha (bright → fades out). */
private fun crtBurnAlpha(progress: Float): Float = when {
    progress > 0.35f -> (1f - ((progress - 0.35f) / 0.65f)) * 0.95f
    progress > 0.08f -> 0.95f
    else -> (progress / 0.08f) * 0.95f
}

// ──────────────────────────────────────────────────────────────────────────────
// Main Composable
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A skeuomorphic retro CRT television built entirely in Compose Canvas.
 *
 * The TV has three tap-cycled states:
 * 1. **Off** — dark CRT glass, no glow.
 * 2. **Color Bars** — SMPTE test pattern with CRT warm-up animation.
 * 3. **Static / Video** — RF static noise until the remote MP4 buffers,
 *    then live video plays inside the barrel-distorted glass.
 *
 * Visual features:
 * - Chrome telescoping antennas with per-pixel specular gradients.
 * - Neon tube frame with bloom glow.
 * - Machined channel-selector knob with knurled teeth and sweep gradient.
 * - Perforated speaker grille with per-hole 3D lighting.
 * - Reactive floor glow that samples the video's dominant color.
 * - Pinch-to-zoom + pan camera with double-tap reset.
 *
 * Performance notes:
 * - Barrel [Path] objects and static [Brush] instances are cached via
 *   [Modifier.drawWithCache] and only recomputed on size change.
 * - CRT scale/burn calculations run in the draw phase, not composition.
 * - Video floor-color sampling reuses a single pre-allocated 1x1 [Bitmap].
 * - Primitive states use [mutableIntStateOf] / [mutableFloatStateOf]
 *   to avoid autoboxing.
 */
@Composable
fun RetroTvScreenUltra3DRealism() {
    var cameraScale by remember { mutableFloatStateOf(1f) }
    var cameraOffset by remember { mutableStateOf(Offset.Zero) }

    var tvState by remember { mutableIntStateOf(TV_OFF) }
    var isVideoBuffered by remember { mutableStateOf(false) }
    var tunerLog by remember { mutableStateOf("► STANDBY") }

    val powerProgress by animateFloatAsState(
        targetValue = if (tvState > TV_OFF) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = LinearEasing),
        label = "flyback_transformer",
    )

    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoFloorColor by remember { mutableStateOf(FloorBaselineColor) }

    val samplingBitmap = remember { createBitmap(1, 1) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activePlayer?.stop()
                activePlayer?.release()
            } catch (_: Exception) {
                // MediaPlayer may already be released
            }
        }
    }

    // Play/pause the video stream based on power state
    LaunchedEffect(key1 = tvState, key2 = isVideoBuffered) {
        if (tvState == TV_STATIC_OR_VIDEO && isVideoBuffered) {
            try {
                activePlayer?.start()
            } catch (_: Exception) {
            }
        } else {
            try {
                activePlayer?.pause()
            } catch (_: Exception) {
            }
        }
    }

    // Sample the video's center pixel every ~60 ms to drive the reactive floor glow
    LaunchedEffect(key1 = tvState, key2 = isVideoBuffered) {
        if (tvState == TV_STATIC_OR_VIDEO && isVideoBuffered) {
            while (true) {
                textureViewRef?.getBitmap(samplingBitmap)?.let { bmp ->
                    val rawColor = Color(bmp[0, 0])
                    videoFloorColor =
                        lerp(start = FloorBaselineColor, stop = rawColor, fraction = 0.85f)
                }
                delay(timeMillis = 60)
            }
        } else {
            videoFloorColor = FloorBaselineColor
        }
    }

    // ── Infinite Animation Driver ───────────────────────────────────────
    // Runs continuously; only consumed inside Canvas draw lambdas that
    // guard reads behind tvState checks, so no wasted draws when TV is off.
    val infiniteTransition = rememberInfiniteTransition(label = "tv_static_master")
    val nativeTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rf_demodulator",
    )

    // ── Root Layout ─────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(WallTop, WallBottom)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        cameraScale = 1f
                        cameraOffset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = cameraScale
                    val newScale =
                        (oldScale * zoom).coerceIn(minimumValue = 0.6f, maximumValue = 4.5f)
                    val factor = newScale / oldScale
                    val center = Offset(x = size.width / 2f, y = size.height / 2f)
                    cameraOffset =
                        (cameraOffset * factor) + pan + (centroid - center) * (1f - factor)
                    cameraScale = newScale
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = cameraScale
                    scaleY = cameraScale
                    translationX = cameraOffset.x
                    translationY = cameraOffset.y
                },
        ) {
            Column(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .offset(y = (-30).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ════════════════════════════════════════════════════════
                // 1. ANTENNA MOUNT & CHROME RODS
                // ════════════════════════════════════════════════════════

                Box(
                    modifier = Modifier
                        .width(width = 220.dp)
                        .height(height = 110.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawAntennaAssembly()
                    }
                }

                // ════════════════════════════════════════════════════════
                // 2. MAIN TV CHASSIS
                // ════════════════════════════════════════════════════════

                Box(
                    modifier = Modifier
                        .width(width = 370.dp)
                        .height(height = 270.dp)
                ) {

                    // ── Floor Glow ───────────────────────────────────────
                    Canvas(
                        modifier = Modifier
                            .align(alignment = Alignment.BottomCenter)
                            .offset(x = (-6).dp, y = 92.dp)
                            .size(width = 440.dp, height = 100.dp),
                    ) {
                        val rfTick = (nativeTime * 0.045f).toInt()
                        val rng = Random(seed = rfTick)
                        val masterAlpha = (0.22f + (rng.nextFloat() * 0.14f)) * powerProgress

                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    videoFloorColor.copy(alpha = masterAlpha * 0.5f),
                                    videoFloorColor.copy(alpha = masterAlpha * 0.15f),
                                    Color.Transparent,
                                ),
                                center = Offset(x = size.width * 0.5f, y = 15.dp.toPx()),
                                radius = size.width * 0.56f,
                            ),
                            topLeft = Offset(x = 10.dp.toPx(), y = 10.dp.toPx()),
                            size = Size(width = size.width - 20.dp.toPx(), height = 88.dp.toPx()),
                        )

                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF2F5FF).copy(alpha = masterAlpha),
                                    videoFloorColor.copy(alpha = masterAlpha * 0.45f),
                                    Color.Transparent,
                                ),
                                center = Offset(x = size.width * 0.5f, y = 20.dp.toPx()),
                                radius = size.width * 0.32f,
                            ),
                            topLeft = Offset(x = size.width * 0.18f, y = 16.dp.toPx()),
                            size = Size(width = size.width * 0.64f, height = 56.dp.toPx()),
                        )
                    }

                    // ── Shell Extrusion Shadow ──────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 8.dp, y = 12.dp)
                            .drawBehind {
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            TvBlueDeepExtrusionColor,
                                            Color(0xFF090A14)
                                        ),
                                        start = Offset(x = size.width, y = 0f),
                                        end = Offset(x = 0f, y = size.height),
                                    ),
                                    cornerRadius = CornerRadius(x = 36.dp.toPx()),
                                )
                            },
                    )

                    // ── Shell Surface + Chamfer ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val r = 36.dp.toPx()
                                val cr = CornerRadius(x = r)
                                val w = size.width
                                val h = size.height

                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.0f to TvShellHighlight,
                                            0.25f to TvShellMidLight,
                                            0.65f to TvShellMidDark,
                                            1.0f to TvShellDeepShadow,
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(x = w, y = h),
                                    ),
                                    cornerRadius = cr,
                                )

                                val chamferW = 2.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.White.copy(alpha = 0.85f),
                                            0.35f to Color.White.copy(alpha = 0.15f),
                                            0.6f to Color.Transparent,
                                            0.85f to Color.Black.copy(alpha = 0.6f),
                                            1.0f to Color.Black.copy(alpha = 0.9f),
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(x = w, y = h),
                                    ),
                                    topLeft = Offset(x = chamferW / 2f, y = chamferW / 2f),
                                    size = Size(width = w - chamferW, height = h - chamferW),
                                    cornerRadius = CornerRadius(x = r - chamferW / 2f),
                                    style = Stroke(width = chamferW),
                                )
                            }
                            .padding(all = 14.dp),
                    ) {
                        // ── Inner Face + Neon Tube ──────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawInnerFaceAndNeon()
                                }
                                .padding(all = 16.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {

                                // ── LEFT: SCREEN ASSEMBLY ───────────
                                Box(
                                    modifier = Modifier
                                        .weight(weight = 1f)
                                        .fillMaxHeight()
                                        .padding(top = 2.dp, bottom = 4.dp, start = 2.dp),
                                ) {
                                    // SUB-LAYER A: Native Video Player
                                    Box(
                                        modifier = Modifier
                                            .padding(all = 12.dp)
                                            .fillMaxSize()
                                            .clip(shape = GlassBarrelShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { ctx ->
                                                TextureView(ctx).apply {
                                                    surfaceTextureListener = object :
                                                        TextureView.SurfaceTextureListener {
                                                        override fun onSurfaceTextureAvailable(
                                                            st: SurfaceTexture,
                                                            w: Int,
                                                            h: Int
                                                        ) {
                                                            val surface = Surface(st)
                                                            try {
                                                                val mp = MediaPlayer().apply {
                                                                    setSurface(surface)
                                                                    setDataSource("https://www.w3schools.com/html/mov_bbb.mp4")
                                                                    setOnErrorListener { _, what, _ ->
                                                                        tunerLog =
                                                                            "► ERR: BLOCK [$what]"
                                                                        true
                                                                    }
                                                                    setOnPreparedListener { player ->
                                                                        player.isLooping = true
                                                                        isVideoBuffered = true
                                                                        tunerLog = "► SIGNAL LOCKED"
                                                                        if (tvState > TV_OFF) player.start()
                                                                    }
                                                                    prepareAsync()
                                                                }
                                                                activePlayer = mp
                                                            } catch (_: Exception) {
                                                                tunerLog = "► ERR: INIT FAIL"
                                                            }
                                                        }

                                                        override fun onSurfaceTextureSizeChanged(
                                                            st: SurfaceTexture,
                                                            w: Int,
                                                            h: Int
                                                        ) {
                                                        }

                                                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean =
                                                            true

                                                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                                    }
                                                    textureViewRef = this
                                                }
                                            },
                                            update = { tv ->
                                                tv.visibility =
                                                    if (tvState == TV_STATIC_OR_VIDEO) View.VISIBLE else View.INVISIBLE
                                            },
                                        )
                                    }

                                    // SUB-LAYER B: Skia Master Compositor
                                    //
                                    // Barrel paths and static brushes live in the drawWithCache scope
                                    // and only recompute on size change. Animated state (powerProgress,
                                    // tvState, nativeTime) is read exclusively inside onDrawBehind so
                                    // changes stay in the draw phase and never trigger cache rebuilds.
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawWithCache {
                                                val w = size.width
                                                val h = size.height
                                                val pad = 12.dp.toPx()
                                                val glassW = w - pad * 2
                                                val glassH = h - pad * 2

                                                // ── Cached geometry ─────────────────────────
                                                val housingPath = createBarrelPath(
                                                    width = w,
                                                    height = h,
                                                    cornerRadius = 20.dp.toPx(),
                                                    bow = 4.5.dp.toPx(),
                                                )
                                                val housingPathOffset = createBarrelPath(
                                                    width = w,
                                                    height = h,
                                                    cornerRadius = 20.dp.toPx(),
                                                    bow = 4.5.dp.toPx(),
                                                    offsetX = 2.dp.toPx(),
                                                    offsetY = 2.dp.toPx(),
                                                )
                                                val glassPath = createBarrelPath(
                                                    width = glassW,
                                                    height = glassH,
                                                    cornerRadius = 14.dp.toPx(),
                                                    bow = 3.2.dp.toPx(),
                                                    offsetX = pad,
                                                    offsetY = pad,
                                                )

                                                // ── Cached brushes & strokes ────────────────
                                                val bezelHighlightBrush = Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to Color.White.copy(alpha = 0.35f),
                                                        0.4f to Color.Transparent,
                                                    ),
                                                    start = Offset.Zero,
                                                    end = Offset(x = w * 0.5f, y = h * 0.5f),
                                                )
                                                val bezelStroke = Stroke(width = 1.dp.toPx())
                                                val cornerShadowBrush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.75f),
                                                        Color.Transparent
                                                    ),
                                                    start = Offset(x = pad, y = pad),
                                                    end = Offset(
                                                        x = pad + glassW * 0.45f,
                                                        y = pad + glassH * 0.45f
                                                    ),
                                                )
                                                val glareStripeBrush = Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to Color.White.copy(alpha = 0.18f),
                                                        0.16f to Color.White.copy(alpha = 0.14f),
                                                        0.165f to Color.Transparent,
                                                        0.2f to Color.White.copy(alpha = 0.05f),
                                                        0.28f to Color.Transparent,
                                                    ),
                                                    start = Offset(x = pad, y = pad),
                                                    end = Offset(
                                                        x = pad + glassW,
                                                        y = pad + glassH
                                                    ),
                                                )
                                                val rimLightBrush = Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to Color.Transparent,
                                                        0.65f to Color.Transparent,
                                                        1.0f to Color.White.copy(alpha = 0.3f),
                                                    ),
                                                    start = Offset(x = pad, y = pad),
                                                    end = Offset(
                                                        x = pad + glassW,
                                                        y = pad + glassH
                                                    ),
                                                )
                                                val rimStroke = Stroke(width = 1.5.dp.toPx())
                                                val glassPivot = Offset(
                                                    x = pad + glassW / 2f,
                                                    y = pad + glassH / 2f
                                                )

                                                onDrawBehind {
                                                    // ── Bezel assembly ──
                                                    clipPath(
                                                        path = glassPath,
                                                        clipOp = ClipOp.Difference
                                                    ) {
                                                        drawPath(
                                                            path = housingPathOffset,
                                                            color = Color.Black.copy(alpha = 0.85f)
                                                        )
                                                    }
                                                    clipPath(
                                                        path = glassPath,
                                                        clipOp = ClipOp.Difference
                                                    ) {
                                                        drawPath(
                                                            path = housingPath,
                                                            color = BezelInnerReal
                                                        )
                                                        drawPath(
                                                            path = housingPath,
                                                            brush = bezelHighlightBrush,
                                                            style = bezelStroke
                                                        )
                                                    }

                                                    // ── CRT Screen Content ──
                                                    // Read snapshot states directly here (not derived
                                                    // vals from composition) so draw invalidation
                                                    // fires reliably from the snapshot system.
                                                    val isOn = tvState > TV_OFF
                                                    val isWarmupFinished = powerProgress >= 0.999f
                                                    val shouldDrawOverlay =
                                                        tvState != TV_STATIC_OR_VIDEO || !isVideoBuffered || !isWarmupFinished

                                                    if (shouldDrawOverlay) {
                                                        drawPath(
                                                            path = glassPath,
                                                            color = ScreenDarkFill
                                                        )

                                                        val scX =
                                                            crtScaleX(progress = powerProgress)
                                                        val scY =
                                                            crtScaleY(progress = powerProgress)

                                                        withTransform(transformBlock = {
                                                            scale(
                                                                scaleX = scX,
                                                                scaleY = scY,
                                                                pivot = glassPivot
                                                            )
                                                        }) {
                                                            if (isOn && powerProgress > 0.01f) {
                                                                clipPath(path = glassPath) {
                                                                    if (tvState == TV_COLOR_BARS) {
                                                                        drawSmpteBars(
                                                                            pad = pad,
                                                                            glassW = glassW,
                                                                            glassH = glassH
                                                                        )
                                                                    } else {
                                                                        drawStaticNoise(
                                                                            pad = pad,
                                                                            glassW = glassW,
                                                                            glassH = glassH,
                                                                            nativeTime = nativeTime
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            val burnAlpha =
                                                                crtBurnAlpha(progress = powerProgress)
                                                            if (burnAlpha > 0.01f) {
                                                                drawPath(
                                                                    path = glassPath,
                                                                    color = Color.White.copy(alpha = burnAlpha)
                                                                )
                                                            }
                                                        }

                                                        drawPath(
                                                            path = glassPath,
                                                            brush = cornerShadowBrush
                                                        )
                                                        drawPath(
                                                            path = glassPath,
                                                            brush = glareStripeBrush
                                                        )
                                                    }

                                                    // ── Glass rim highlight (always visible) ──
                                                    drawPath(
                                                        path = glassPath,
                                                        brush = rimLightBrush,
                                                        style = rimStroke
                                                    )
                                                }
                                            },
                                    )

                                    // SUB-LAYER C: VCR OSD
                                    if (tvState == TV_STATIC_OR_VIDEO && !isVideoBuffered) {
                                        BasicText(
                                            text = tunerLog,
                                            style = TextStyle(
                                                color = Color(0xFF00FF66),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                            ),
                                            modifier = Modifier
                                                .align(alignment = Alignment.TopStart)
                                                .padding(start = 22.dp, top = 22.dp),
                                        )
                                    }

                                    // SUB-LAYER D: Touch Interceptor
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = {
                                                        tvState = (tvState + 1) % TV_STATE_COUNT
                                                        if (tvState == TV_STATIC_OR_VIDEO) tunerLog =
                                                            "► TUNING CH-3..."
                                                    },
                                                )
                                            },
                                    )
                                }

                                Spacer(modifier = Modifier.width(width = 16.dp))

                                // ── RIGHT: PHYSICAL CONTROLS ────────
                                Column(
                                    modifier = Modifier
                                        .width(width = 48.dp)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Spacer(modifier = Modifier.height(height = 2.dp))

                                    Canvas(modifier = Modifier.size(size = 16.dp)) {
                                        drawLedIndicator()
                                    }

                                    Spacer(modifier = Modifier.height(height = 8.dp))

                                    Canvas(modifier = Modifier.size(size = 44.dp)) {
                                        drawMachinedDial()
                                    }

                                    Spacer(modifier = Modifier.height(height = 6.dp))

                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(height = 42.dp),
                                    ) {
                                        drawButtonPair()
                                    }

                                    Spacer(modifier = Modifier.height(height = 10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(height = 124.dp)
                                            .clip(shape = RoundedCornerShape(size = 6.dp))
                                            .background(color = TvShellDeepShadow),
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawSpeakerGrille()
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(weight = 1f))
                                }
                            }
                        }
                    }

                    // ════════════════════════════════════════════════════
                    // 3. STAND
                    // ════════════════════════════════════════════════════

                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.BottomCenter)
                            .offset(y = 44.dp)
                            .width(width = 300.dp)
                            .height(height = 56.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawStandAssembly()
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Antenna
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws the V-shaped antenna assembly: chrome mount bar, two socket inserts,
 * two tapered chrome rods, and a neon accent strip along the mount.
 */
private fun DrawScope.drawAntennaAssembly() {
    val mountBaseWidth = size.width * 0.28f
    val mountBaseHeight = 18.dp.toPx()
    val mountTopLeft = Offset(
        x = (size.width - mountBaseWidth) / 2f,
        y = size.height - mountBaseHeight,
    )

    val leftSocket = Offset(
        x = mountTopLeft.x + mountBaseWidth * 0.22f,
        y = size.height - mountBaseHeight * 0.35f,
    )
    val rightSocket = Offset(
        x = mountTopLeft.x + mountBaseWidth * 0.78f,
        y = size.height - mountBaseHeight * 0.35f,
    )

    val antennaTopLeft = Offset(x = 12.dp.toPx(), y = 8.dp.toPx())
    val antennaTopRight = Offset(x = size.width - 12.dp.toPx(), y = 8.dp.toPx())

    val baseR = 4.2.dp.toPx()
    val tipR = 1.6.dp.toPx()

    drawRoundRect(
        color = TvBlueDeepExtrusionColor,
        topLeft = mountTopLeft.copy(y = mountTopLeft.y + 3.dp.toPx()),
        size = Size(width = mountBaseWidth, height = mountBaseHeight),
        cornerRadius = CornerRadius(x = 6.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(TvShellHighlight, TvShellMidLight, TvShellDeepShadow),
        ),
        topLeft = mountTopLeft,
        size = Size(width = mountBaseWidth, height = mountBaseHeight),
        cornerRadius = CornerRadius(x = 6.dp.toPx()),
    )

    listOf(leftSocket, rightSocket).forEach { socketCenter ->
        drawOval(
            color = HoleDeepColor,
            topLeft = Offset(x = socketCenter.x - baseR * 1.5f, y = mountTopLeft.y + 1.dp.toPx()),
            size = Size(width = baseR * 3f, height = baseR * 1.8f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ChromeSpec, ChromeMid, ChromeDark),
                center = Offset(x = socketCenter.x - 1.dp.toPx(), y = mountTopLeft.y + 3.dp.toPx()),
                radius = baseR * 1.3f,
            ),
            radius = baseR * 1.15f,
            center = Offset(x = socketCenter.x, y = mountTopLeft.y + 4.5.dp.toPx()),
        )
        drawArc(
            color = Color.Black.copy(alpha = 0.65f),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(
                x = socketCenter.x - baseR * 1.15f,
                y = mountTopLeft.y + 4.5.dp.toPx() - baseR * 1.15f,
            ),
            size = Size(width = baseR * 2.3f, height = baseR * 2.3f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    drawChromeRod(
        start = leftSocket.copy(y = leftSocket.y - 1.dp.toPx()),
        end = antennaTopLeft,
        baseR = baseR,
        tipR = tipR
    )
    drawChromeRod(
        start = rightSocket.copy(y = rightSocket.y - 1.dp.toPx()),
        end = antennaTopRight,
        baseR = baseR,
        tipR = tipR
    )

    drawRoundRect(
        color = NeonPipeEdge,
        topLeft = Offset(x = mountTopLeft.x + 6.dp.toPx(), y = mountTopLeft.y - 2.dp.toPx()),
        size = Size(width = mountBaseWidth - 12.dp.toPx(), height = 4.dp.toPx()),
        cornerRadius = CornerRadius(x = 2.dp.toPx()),
    )
}

/**
 * Draws a single tapered chrome rod from [start] (socket) to [end] (tip).
 *
 * The rod is a trapezoid filled with a perpendicular linear gradient that
 * simulates a cylindrical chrome reflection. A shadow oval sits at the base
 * and a specular sphere caps the tip.
 */
private fun DrawScope.drawChromeRod(
    start: Offset,
    end: Offset,
    baseR: Float,
    tipR: Float,
) {
    val angle = atan2(y = end.y - start.y, x = end.x - start.x)
    val perpAngle = angle + (PI / 2).toFloat()
    val cosP = cos(perpAngle)
    val sinP = sin(perpAngle)

    val p1 = Offset(x = start.x + cosP * baseR, y = start.y + sinP * baseR)
    val p2 = Offset(x = start.x - cosP * baseR, y = start.y - sinP * baseR)
    val p3 = Offset(x = end.x - cosP * tipR, y = end.y - sinP * tipR)
    val p4 = Offset(x = end.x + cosP * tipR, y = end.y + sinP * tipR)

    val rodPath = Path().apply {
        moveTo(x = p1.x, y = p1.y)
        lineTo(x = p2.x, y = p2.y)
        lineTo(x = p3.x, y = p3.y)
        lineTo(x = p4.x, y = p4.y)
        close()
    }

    drawPath(
        path = rodPath,
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to ChromeDark, 0.35f to ChromeMid,
                0.47f to ChromeSpec, 0.53f to ChromeSpec,
                0.65f to ChromeMid, 1.0f to ChromeDark,
            ),
            start = p1,
            end = p2,
        ),
    )
    drawOval(
        color = Color(0xAA000000),
        topLeft = Offset(x = start.x - baseR * 1.1f, y = start.y - baseR * 0.4f),
        size = Size(width = baseR * 2.2f, height = baseR * 0.8f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ChromeSpec, ChromeMid, ChromeDark),
            center = Offset(x = end.x - tipR * 0.4f, y = end.y - tipR * 0.4f),
            radius = tipR * 2.2f,
        ),
        radius = tipR * 1.8f,
        center = end,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Inner Panel
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws the recessed inner face panel and the neon tube that runs around it.
 *
 * Layers from back to front:
 * 1. Beveled trench ring (inset shadow illusion).
 * 2. Flat dark face with corner shadow gradient.
 * 3. Soft neon glow fill.
 * 4. Bright neon tube stroke with round caps.
 */
private fun DrawScope.drawInnerFaceAndNeon() {
    val w = size.width
    val h = size.height
    val outerCorner = 24.dp.toPx()
    val innerCorner = 18.dp.toPx()
    val trenchW = 7.dp.toPx()

    drawRoundRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF0C0D1A),
                0.35f to TvShellDeepShadow,
                0.7f to TvShellMidLight,
                1.0f to Color.White.copy(alpha = 0.45f),
            ),
            start = Offset.Zero,
            end = Offset(x = w, y = h),
        ),
        topLeft = Offset(x = trenchW / 2f, y = trenchW / 2f),
        size = Size(width = w - trenchW, height = h - trenchW),
        cornerRadius = CornerRadius(x = outerCorner - trenchW / 2f),
        style = Stroke(width = trenchW),
    )

    val faceLeft = trenchW
    val faceTop = trenchW
    val faceW = w - trenchW * 2
    val faceH = h - trenchW * 2

    drawRoundRect(
        color = TvShellMidDark,
        topLeft = Offset(x = faceLeft, y = faceTop),
        size = Size(width = faceW, height = faceH),
        cornerRadius = CornerRadius(x = innerCorner),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
            start = Offset(x = faceLeft, y = faceTop),
            end = Offset(x = faceLeft + 20.dp.toPx(), y = faceTop + 20.dp.toPx()),
        ),
        topLeft = Offset(x = faceLeft, y = faceTop),
        size = Size(width = faceW, height = faceH),
        cornerRadius = CornerRadius(x = innerCorner),
    )

    // Neon tube
    val tubeW = 2.5.dp.toPx()
    val neonInset = trenchW / 2f

    drawRoundRect(
        color = NeonGlowColor,
        topLeft = Offset(x = neonInset - 2.dp.toPx(), y = neonInset - 2.dp.toPx()),
        size = Size(
            width = w - (neonInset - 2.dp.toPx()) * 2,
            height = h - (neonInset - 2.dp.toPx()) * 2,
        ),
        cornerRadius = CornerRadius(x = outerCorner),
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(NeonPipeCenter, NeonPipeEdge),
            center = Offset(x = w, y = 0f),
            radius = w,
        ),
        topLeft = Offset(x = neonInset, y = neonInset),
        size = Size(width = w - neonInset * 2, height = h - neonInset * 2),
        cornerRadius = CornerRadius(x = outerCorner - neonInset),
        style = Stroke(width = tubeW, cap = StrokeCap.Round),
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Screen Content
// ──────────────────────────────────────────────────────────────────────────────

/** Fills the CRT glass area with seven vertical SMPTE/EIA color bars. */
private fun DrawScope.drawSmpteBars(pad: Float, glassW: Float, glassH: Float) {
    val barWidth = glassW / SmpteBarColors.size
    SmpteBarColors.forEachIndexed { i, color ->
        drawRect(
            color = color,
            topLeft = Offset(x = pad + i * barWidth, y = pad),
            size = Size(width = barWidth + 1f, height = glassH),
        )
    }
}

/**
 * Fills the CRT glass area with animated RF static noise.
 *
 * A seeded [Random] is re-created each frame from a time-derived tick so the
 * noise pattern is deterministic per frame (no flicker on draw-phase replays).
 * Grain colors use pre-computed [StaticBright]/[StaticMid]/[StaticDim]/[StaticDark]
 * to avoid redundant alpha blending inside the 64x48 loop.
 */
private fun DrawScope.drawStaticNoise(
    pad: Float,
    glassW: Float,
    glassH: Float,
    nativeTime: Float,
) {
    val rfTick = (nativeTime * 0.045f).toInt()
    val rng = Random(seed = rfTick)
    val cols = 64
    val rows = 48
    val cellW = glassW / cols
    val cellH = glassH / rows

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val n = rng.nextFloat()
            val grain = when {
                n > 0.80f -> StaticBright
                n > 0.48f -> StaticMid
                n > 0.20f -> StaticDim
                else -> StaticDark
            }
            drawRect(
                color = grain,
                topLeft = Offset(x = pad + c * cellW, y = pad + r * cellH),
                size = Size(width = cellW * 1.35f, height = cellH + 0.6f),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Physical Controls
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws the power LED jewel: housing ring, ruby-red glass dome,
 * two specular highlight ovals, and a caustic pink rim arc.
 */
private fun DrawScope.drawLedIndicator() {
    val c = center
    val r = size.minDimension / 2f
    val glassR = r * 0.82f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(LedBaseShadow, Color.Transparent),
            center = c.copy(x = c.x - 1f, y = c.y + 2f),
            radius = r,
        ),
        radius = r,
    )
    drawCircle(color = LedHousingRing, radius = r * 0.95f)
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(RubyHotTip, RubyMidGlow, RubyDarkRoot),
            start = Offset.Zero,
            end = Offset(x = size.width, y = size.height),
        ),
        radius = glassR,
    )

    drawOval(
        color = SpecularHardWhite,
        topLeft = Offset(x = c.x - glassR * 0.6f, y = c.y - glassR * 0.65f),
        size = Size(width = glassR * 0.75f, height = glassR * 0.4f),
    )
    drawOval(
        color = SpecularSoftWhite,
        topLeft = Offset(x = c.x - glassR * 0.45f, y = c.y - glassR * 0.15f),
        size = Size(width = glassR * 0.45f, height = glassR * 0.2f),
    )
    drawArc(
        color = CausticPinkRim,
        startAngle = 15f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(x = c.x - glassR * 0.88f, y = c.y - glassR * 0.88f),
        size = Size(width = glassR * 1.76f, height = glassR * 1.76f),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
    )
}

/**
 * Draws the machined channel-selector dial: knurled teeth ring, swept-gradient
 * face, and a notch position indicator at 12 o'clock.
 */
private fun DrawScope.drawMachinedDial() {
    val c = center
    val dialR = 17.dp.toPx()

    drawCircle(
        color = Color(0x60000000),
        radius = dialR + 3.dp.toPx(),
        center = c + Offset(x = -2f, y = 4f),
    )
    drawCircle(color = HoleDeepColor, radius = dialR + 1.5.dp.toPx())

    val teethCount = 36
    val toothW = 1.8.dp.toPx()
    val toothH = 3.6.dp.toPx()
    val toothCorner = CornerRadius(x = 0.8.dp.toPx())

    for (i in 0 until teethCount) {
        withTransform(transformBlock = { rotate(degrees = (360f / teethCount) * i, pivot = c) }) {
            drawRoundRect(
                brush = Brush.verticalGradient(colors = listOf(KnobSpunSpec, KnobSpunDark)),
                topLeft = Offset(x = c.x - toothW / 2f, y = c.y - dialR),
                size = Size(width = toothW, height = toothH),
                cornerRadius = toothCorner,
            )
        }
    }

    // Swept-gradient face
    val faceR = dialR - 1.8.dp.toPx()
    drawCircle(
        brush = Brush.sweepGradient(
            colorStops = arrayOf(
                0.0f to KnobSpunMid, 0.25f to KnobSpunSpec,
                0.5f to KnobSpunDark, 0.75f to KnobSpunSpec,
                1.0f to KnobSpunMid,
            ),
        ),
        radius = faceR,
    )
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.4f)
            ),
            start = Offset(x = c.x - faceR, y = c.y - faceR),
            end = Offset(x = c.x + faceR, y = c.y + faceR),
        ),
        radius = faceR,
    )

    // Notch indicator
    val notchW = 2.dp.toPx()
    val notchH = 6.dp.toPx()
    val notchTop = c.y - faceR + 3.dp.toPx()
    drawRoundRect(
        color = HoleDeepColor,
        topLeft = Offset(x = c.x - notchW / 2f, y = notchTop),
        size = Size(width = notchW, height = notchH),
        cornerRadius = CornerRadius(x = 1.dp.toPx()),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.7f),
        start = Offset(x = c.x - notchW / 2f, y = notchTop + notchH),
        end = Offset(x = c.x + notchW / 2f, y = notchTop + notchH),
        strokeWidth = 1.dp.toPx(),
    )
}

/**
 * Draws two raised push-buttons (power + resize) with concentric dot texture
 * on the power button and silkscreen icons below each.
 */
private fun DrawScope.drawButtonPair() {
    val baseBtnR = 6.dp.toPx()
    val raisedFaceR = baseBtnR * 1.08f
    val b1X = size.width * 0.25f
    val b2X = size.width * 0.75f
    val bY = 11.dp.toPx()
    val iconY = bY + baseBtnR + 11.dp.toPx()
    val silkStroke = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)

    drawCircle(
        color = Color(0x75000000),
        radius = raisedFaceR * 1.15f,
        center = Offset(x = b1X - 2.5.dp.toPx(), y = bY + 3.5.dp.toPx()),
    )
    drawCircle(
        color = HoleDeepColor,
        radius = baseBtnR + 1.2.dp.toPx(),
        center = Offset(x = b1X, y = bY),
    )
    drawCircle(
        color = PinkControlDarkReal,
        radius = raisedFaceR,
        center = Offset(x = b1X - 0.5.dp.toPx(), y = bY + 1.dp.toPx()),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PinkControlLightReal, PinkControlMainReal, PinkControlDarkReal),
            center = Offset(x = b1X - 1.5.dp.toPx(), y = bY - 1.5.dp.toPx()),
            radius = raisedFaceR * 1.4f,
        ),
        radius = raisedFaceR,
        center = Offset(x = b1X, y = bY),
    )

    val dotRadius = 0.35.dp.toPx()
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = dotRadius,
        center = Offset(x = b1X, y = bY)
    )

    for (ring in 1..3) {
        val ringR = (raisedFaceR * 0.23f) * ring
        val dots = ring * 6
        for (d in 0 until dots) {
            val theta = (2 * PI / dots) * d
            val cx = b1X + (ringR * cos(theta)).toFloat()
            val cy = bY + (ringR * sin(theta)).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = dotRadius,
                center = Offset(x = cx + 0.3f, y = cy - 0.3f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = dotRadius,
                center = Offset(x = cx - 0.3f, y = cy + 0.3f)
            )
        }
    }

    drawArc(
        color = SilkscreenInk,
        startAngle = -65f,
        sweepAngle = 310f,
        useCenter = false,
        topLeft = Offset(x = b1X - 3.5.dp.toPx(), y = iconY - 3.5.dp.toPx()),
        size = Size(width = 7.dp.toPx(), height = 7.dp.toPx()),
        style = silkStroke,
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b1X, y = iconY - 4.dp.toPx()),
        end = Offset(x = b1X, y = iconY - 1.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )

    drawCircle(
        color = HoleDeepColor,
        radius = baseBtnR + 1.5.dp.toPx(),
        center = Offset(x = b2X, y = bY),
    )
    drawArc(
        color = Color(0x35FFFFFF),
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(x = b2X - baseBtnR - 1.5.dp.toPx(), y = bY - baseBtnR - 1.5.dp.toPx()),
        size = Size(
            width = (baseBtnR + 1.5.dp.toPx()) * 2,
            height = (baseBtnR + 1.5.dp.toPx()) * 2
        ),
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PinkControlLightReal, PinkControlMainReal, PinkControlDarkReal),
            center = Offset(x = b2X + 2.dp.toPx(), y = bY - 2.dp.toPx()),
            radius = baseBtnR * 1.3f,
        ),
        radius = baseBtnR,
        center = Offset(x = b2X, y = bY),
    )

    val sW = 3.5.dp.toPx()
    val sH = 2.5.dp.toPx()
    val tip = 1.8.dp.toPx()
    val sw = 1.2.dp.toPx()

    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X - sW, y = iconY - sH),
        end = Offset(x = b2X + sW, y = iconY + sH),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X - sW, y = iconY + sH),
        end = Offset(x = b2X + sW, y = iconY - sH),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X + sW, y = iconY + sH),
        end = Offset(x = b2X + sW - tip, y = iconY + sH),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X + sW, y = iconY + sH),
        end = Offset(x = b2X + sW, y = iconY + sH - tip),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X + sW, y = iconY - sH),
        end = Offset(x = b2X + sW - tip, y = iconY - sH),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SilkscreenInk,
        start = Offset(x = b2X + sW, y = iconY - sH),
        end = Offset(x = b2X + sW, y = iconY - sH + tip),
        strokeWidth = sw,
        cap = StrokeCap.Round
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Speaker Grille
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws the perforated speaker grille: chamfered housing ring, pink face plate,
 * top shadow gradient, and a 6x18 grid of rectangular holes with per-hole
 * top-shadow and bottom-highlight edges.
 */
private fun DrawScope.drawSpeakerGrille() {
    val w = size.width
    val h = size.height
    val chamfer = 3.dp.toPx()
    val pT = chamfer + 3.dp.toPx()
    val pL = chamfer + 2.dp.toPx()
    val pB = chamfer + 1.dp.toPx()
    val pR = chamfer + 1.dp.toPx()
    val innerW = w - (pL + pR)
    val innerH = h - (pT + pB)

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(HoleDeepColor, TvShellHighlight.copy(alpha = 0.3f)),
            start = Offset.Zero,
            end = Offset(x = w, y = h),
        ),
        size = size,
        cornerRadius = CornerRadius(x = 6.dp.toPx()),
        style = Stroke(width = chamfer * 1.5f),
    )

    drawRoundRect(
        color = PinkControlMainReal,
        topLeft = Offset(x = pL, y = pT),
        size = Size(width = innerW, height = innerH),
        cornerRadius = CornerRadius(x = 3.dp.toPx()),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent),
            startY = pT,
            endY = pT + 16.dp.toPx(),
        ),
        topLeft = Offset(x = pL, y = pT),
        size = Size(width = innerW, height = 16.dp.toPx()),
    )

    // Perforated hole grid
    val cols = 6
    val rows = 18
    val sX = innerW / cols
    val sY = innerH / rows
    val hW = sX * 0.64f
    val hH = sY * 0.64f
    val highlightStroke = 0.6.dp.toPx()
    val shadowStroke = 1.dp.toPx()

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = pL + (c * sX) + (sX - hW) / 2f
            val y = pT + (r * sY) + (sY - hH) / 2f

            drawRect(
                color = HoleDeepColor,
                topLeft = Offset(x = x, y = y),
                size = Size(width = hW, height = hH)
            )
            drawLine(
                color = HoleHoleDarkColor,
                start = Offset(x = x, y = y),
                end = Offset(x = x + hW, y = y),
                strokeWidth = shadowStroke
            )
            drawLine(
                color = HoleHoleDarkColor,
                start = Offset(x = x + hW, y = y),
                end = Offset(x = x + hW, y = y + hH),
                strokeWidth = shadowStroke
            )
            drawLine(
                color = PinkControlLightReal,
                start = Offset(x = x, y = y + hH),
                end = Offset(x = x + hW, y = y + hH),
                strokeWidth = highlightStroke
            )
            drawLine(
                color = PinkControlLightReal,
                start = Offset(x = x, y = y),
                end = Offset(x = x, y = y + hH),
                strokeWidth = highlightStroke
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// DrawScope Extensions — Stand
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Draws the stand: two brushed-metal feet with rubber pads and chrome caps,
 * a horizontal platform bar with extrusion shadow and top-edge specular,
 * and a recessed logo slot in the center.
 */
private fun DrawScope.drawStandAssembly() {
    val platW = 260.dp.toPx()
    val platH = 14.dp.toPx()
    val platLeft = (size.width - platW) / 2f
    val platTop = 18.dp.toPx()
    val fRX = 14.dp.toPx()
    val fRY = 6.dp.toPx()
    val fH = 18.dp.toPx()
    val cR = 6.dp.toPx()

    listOf(platLeft + 36.dp.toPx(), platLeft + platW - 36.dp.toPx()).forEach { fx ->
        val tY = platTop + platH - 4.dp.toPx()
        val bY = tY + fH

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xD90A0512), Color(0x550A0512), Color.Transparent),
                center = Offset(x = fx + 3.dp.toPx(), y = bY + 1.dp.toPx()),
                radius = fRX * 1.5f,
            ),
            topLeft = Offset(x = fx - fRX * 1.3f + 3.dp.toPx(), y = bY - fRY * 0.8f),
            size = Size(width = fRX * 2.6f, height = fRY * 2.2f),
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to MetalFootDark, 0.25f to MetalFootMid,
                    0.45f to MetalFootLight, 0.55f to MetalFootLight,
                    0.8f to MetalFootMid, 1.0f to MetalFootDark,
                ),
                startX = fx - fRX,
                endX = fx + fRX,
            ),
            topLeft = Offset(x = fx - fRX, y = tY),
            size = Size(width = fRX * 2, height = fH),
        )

        drawOval(
            color = RubberPadDark,
            topLeft = Offset(x = fx - fRX, y = bY - fRY),
            size = Size(width = fRX * 2, height = fRY * 2),
        )

        drawOval(
            brush = Brush.verticalGradient(colors = listOf(MetalFootLight, MetalFootDark)),
            topLeft = Offset(x = fx - fRX, y = tY - fRY),
            size = Size(width = fRX * 2, height = fRY * 2),
        )
    }

    // ── Platform bar ──
    drawRoundRect(
        color = TvBlueDeepExtrusionColor,
        topLeft = Offset(x = platLeft - 4.dp.toPx(), y = platTop + 5.dp.toPx()),
        size = Size(width = platW, height = platH),
        cornerRadius = CornerRadius(x = cR),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                TvShellHighlight,
                TvShellMidLight,
                TvShellDeepShadow
            )
        ),
        topLeft = Offset(x = platLeft, y = platTop),
        size = Size(width = platW, height = platH),
        cornerRadius = CornerRadius(x = cR),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(x = platLeft + cR, y = platTop + 1.dp.toPx()),
        end = Offset(x = platLeft + platW - cR, y = platTop + 1.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // ── Logo slot ──
    val slW = platW * 0.32f
    val slH = 5.dp.toPx()
    val slL = platLeft + (platW - slW) / 2f
    val slT = platTop + 4.dp.toPx()

    drawRoundRect(
        color = HoleDeepColor,
        topLeft = Offset(x = slL, y = slT),
        size = Size(width = slW, height = slH),
        cornerRadius = CornerRadius(x = slH / 2f),
    )
    drawRoundRect(
        color = TvShellHighlight.copy(alpha = 0.6f),
        topLeft = Offset(x = slL, y = slT + 1.dp.toPx()),
        size = Size(width = slW, height = slH),
        cornerRadius = CornerRadius(x = slH / 2f),
        style = Stroke(width = 1.dp.toPx()),
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.7f),
        start = Offset(x = slL + 2.dp.toPx(), y = slT),
        end = Offset(x = slL + slW - 2.dp.toPx(), y = slT),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
}