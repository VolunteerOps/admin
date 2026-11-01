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

package com.zengrip

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * # PhysicsBubbleScreen
 *
 * A physics-driven soap bubble demo built with Jetpack Compose and AGSL.
 *
 * ## What it does
 *
 * Renders an interactive soap bubble that the user can drag vertically across the screen.
 * The bubble refracts the content behind it, displays thin-film interference colors
 * (the rainbow swirls you see on real soap bubbles), and deforms based on movement velocity.
 * Tapping anywhere pops the bubble with a fade-out animation.
 *
 * ## Physics involved
 *
 * ### Thin-film interference (AGSL shader)
 *
 * Real soap bubbles get their colors from light bouncing between the two surfaces
 * of a thin water film. When a light ray hits the bubble:
 *
 * 1. Part of it reflects off the outer surface.
 * 2. Part enters the film, travels through it, and reflects off the inner surface.
 * 3. These two reflected rays combine. Depending on the film thickness and the
 *    wavelength of light, they either reinforce each other (constructive interference,
 *    bright color) or cancel out (destructive interference, dark band).
 *
 * The math: the optical path difference is `Δ = 2 * n * d * cos(θt)`, where `n` is
 * the refractive index (1.33 for soapy water), `d` is the film thickness, and `θt`
 * is the refracted angle inside the film (from Snell's law). We evaluate this for
 * three wavelengths (R=650nm, G=532nm, B=450nm) and compute a cosine oscillation
 * for each channel. The result is the characteristic color bands.
 *
 * ### Fresnel reflectance (AGSL shader)
 *
 * How much light reflects vs passes through depends on the viewing angle. At normal
 * incidence (looking straight at the bubble), reflectance is low (~2%). At grazing
 * angles (the edge/rim), reflectance approaches 100% — that's why bubble edges
 * look brighter and whiter. We approximate this with the Schlick formula:
 * `R = R0 + (1 - R0) * (1 - cos(θ))^5`.
 *
 * ### Kinematic spring deformation (Compose frame loop)
 *
 * When the bubble moves, it squashes along the direction of motion and stretches
 * perpendicular to it — like a real bubble resisting air drag. This uses a
 * volume-preserving transform: if you stretch by factor `s` along one axis,
 * you squash by `1/√s` on the other to keep the area constant.
 *
 * The deformation amount is driven by a manually-stepped Euler spring that tracks
 * the drag velocity. This avoids creating a new coroutine every frame and gives
 * smooth, jelly-like oscillation.
 *
 * ### Chromatic aberration (AGSL shader)
 *
 * Light bends differently depending on wavelength. We sample the background at
 * three slightly different offsets for R, G, and B channels, creating a subtle
 * color fringe around refracted content — just like a real lens or bubble.
 *
 * ## API level
 *
 * The AGSL shader requires API 33+ (Tiramisu). On older devices, a simple radial
 * gradient fallback is drawn instead.
 */

/**
 * Layout and animation constants for the bubble.
 *
 * Ratios are relative to screen height. The bubble lives on a vertical
 * track between [BOTTOM_ORB_RATIO] (rest position) and [TOP_ORB_RATIO] (pulled up).
 */
private object BubbleConfig {
    /** Y position of the bubble at rest, as a fraction of screen height. */
    const val BOTTOM_ORB_RATIO = 0.92f

    /** Y position of the bubble when fully dragged up. */
    const val TOP_ORB_RATIO = 0.28f

    /** Maximum bubble radius (at rest / bottom position). */
    val MAX_ORB_RADIUS = 250.dp

    /** Minimum bubble radius (when fully pulled to the top). */
    val MIN_ORB_RADIUS = 85.dp

    /** Y position of the detail text block at the bottom state. */
    const val TEXT_Y_BOTTOM_RATIO = 0.48f

    /** Y position of the detail text block at the top state. */
    const val TEXT_Y_TOP_RATIO = 0.42f

    /**
     * Distance threshold (px) from the top position at which the bubble
     * "unlocks" and allows free XY dragging instead of vertical-only.
     */
    const val SNAP_UNLOCK_THRESHOLD = 10f

    /** How far below the rest position the bubble can be dragged (overshoot). */
    const val DRAG_OVERSHOOT_RATIO = 0.05f

    /** Multiplier converting drag velocity into deformation magnitude. */
    const val DEFORMATION_FACTOR = 0.015f

    /** Maximum deformation clamp to prevent extreme stretching. */
    const val DEFORMATION_CLAMP = 0.6f

    /** Low-pass filter factor for smoothing velocity between frames. */
    const val VELOCITY_SMOOTHING = 0.15f

    /** Duration (ms) of the circular-reveal theme transition. */
    const val THEME_REVEAL_DURATION = 1100

    /** Duration (ms) of the bubble pop fade-out. */
    const val POP_DURATION = 150

    /** Delay (ms) before the bubble respawns after popping. */
    const val POP_DELAY = 2000L

    /** Duration (ms) of the text color crossfade on theme change. */
    const val TEXT_ANIM_DURATION = 700
}

/** Color palette for both light and dark themes. */
private object BubbleColors {
    val LIGHT_CENTER = Color(0xFFFFFFFF)
    val LIGHT_MID1 = Color(0xFFFBF8F6)
    val LIGHT_MID2 = Color(0xFFF5EFEE)
    val LIGHT_EDGE = Color(0xFFEEEAE8)

    val DARK_CENTER = Color(0xFF2A2D34)
    val DARK_MID = Color(0xFF16171B)
    val DARK_EDGE = Color(0xFF0A0B0D)

    val LIGHT_MAIN_TEXT = Color(0xFF4A403A)
    val DARK_MAIN_TEXT = Color(0xFFE5E5EA)
    val LIGHT_TITLE = Color(0xFF1F1A17)
    val DARK_TITLE = Color(0xFFF5F5F7)
    val LIGHT_SUBTITLE = Color(0xFF8A807A)
    val DARK_SUBTITLE = Color(0xFFA1A1A6)
}

/** Spring config for snapping back to rest or top position (locked vertical drag). */
private val SnapBackSpring = spring<Offset>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessLow
)

/** Spring config for snapping when unlocked (free XY drag). More bouncy. */
private val UnlockedSnapSpring = spring<Offset>(
    dampingRatio = 0.45f,
    stiffness = Spring.StiffnessLow
)

/**
 * Holds the mutable animation state for the bubble.
 *
 * Marked [Stable] so Compose can skip recomposition when the reference hasn't changed.
 * All animation values are [Animatable], which means they're updated outside of
 * recomposition — the shader and draw calls read them directly each frame.
 *
 * @param screenHeightPx Total screen height in pixels.
 * @param orbRadiusMaxPx Maximum bubble radius in pixels.
 * @param orbRadiusMinPx Minimum bubble radius in pixels.
 * @param centerX Horizontal center of the screen in pixels.
 */
@Stable
class PhysicsBubbleState(
    private val screenHeightPx: Float,
    orbRadiusMaxPx: Float,
    orbRadiusMinPx: Float,
    val centerX: Float,
) {
    /** Y center of the bubble at rest (bottom). */
    val bottomOrbCenterY = screenHeightPx * BubbleConfig.BOTTOM_ORB_RATIO

    /** Y center of the bubble when fully pulled up (top). */
    val topOrbCenterY = screenHeightPx * BubbleConfig.TOP_ORB_RATIO

    /** Midpoint between top and bottom — used to decide snap direction on release. */
    val midPoint = (bottomOrbCenterY + topOrbCenterY) / 2f

    /** Maximum Y the bubble can be dragged to (slightly below rest for overshoot). */
    val maxDragY = bottomOrbCenterY + (screenHeightPx * BubbleConfig.DRAG_OVERSHOOT_RATIO)

    private val orbRadiusMax = orbRadiusMaxPx
    private val orbRadiusMin = orbRadiusMinPx

    private val orbRange = bottomOrbCenterY - topOrbCenterY
    private val textYBottom = screenHeightPx * BubbleConfig.TEXT_Y_BOTTOM_RATIO
    private val textYTop = screenHeightPx * BubbleConfig.TEXT_Y_TOP_RATIO

    /** Current bubble position. Animated via spring physics on drag release. */
    val bubblePos = Animatable(Offset(centerX, bottomOrbCenterY), Offset.VectorConverter)

    /**
     * Current squash/stretch deformation.
     *
     * X and Y represent how much the bubble is deformed along each axis.
     * Driven by a manual Euler spring in [DeformationFrameLoop].
     */
    val deformationAnim = Animatable(Offset.Zero, Offset.VectorConverter)

    /** Pop animation progress: 0 = normal, 1 = fully popped/invisible. */
    val popAnim = Animatable(0f)

    /** Circular reveal progress for the theme transition: 0 = start, 1 = complete. */
    val themeRevealProgress = Animatable(1f)

    /** Elapsed time in seconds, fed to the AGSL shader for animated noise. */
    val shaderTime = floatArrayOf(0f)

    /**
     * Normalized drag progress from bottom (0) to top (1).
     * Used to interpolate bubble radius, text position, and opacity.
     */
    val progress: Float
        get() = ((bottomOrbCenterY - bubblePos.value.y) / orbRange).coerceIn(0f, 1f)

    /** Bubble radius interpolated between max (bottom) and min (top). */
    val currentOrbRadius: Float
        get() = androidx.compose.ui.util.lerp(orbRadiusMax, orbRadiusMin, progress)

    /** Y offset of the detail text block, interpolated with drag progress. */
    val textYOffsetPx: Float
        get() = androidx.compose.ui.util.lerp(textYBottom, textYTop, progress)

    /** Returns true if the bubble is at (or very near) the top snap position. */
    fun isAtTop(): Boolean =
        bubblePos.value.y <= topOrbCenterY + BubbleConfig.SNAP_UNLOCK_THRESHOLD
}

/**
 * Creates and remembers a [PhysicsBubbleState] scoped to the current screen dimensions.
 * Re-creates the state if screen size changes (e.g. rotation).
 */
@Composable
private fun rememberBubbleState(
    screenWidthPx: Float,
    screenHeightPx: Float,
): PhysicsBubbleState {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { BubbleConfig.MAX_ORB_RADIUS.toPx() }
    val minRadiusPx = with(density) { BubbleConfig.MIN_ORB_RADIUS.toPx() }

    return remember(screenWidthPx, screenHeightPx) {
        PhysicsBubbleState(
            screenHeightPx = screenHeightPx,
            orbRadiusMaxPx = maxRadiusPx,
            orbRadiusMinPx = minRadiusPx,
            centerX = screenWidthPx / 2f,
        )
    }
}

/**
 * Entry point. Measures the screen and sets up the bubble state.
 *
 * Uses [BoxWithConstraints] to get pixel dimensions before creating
 * position-dependent state like snap points and text offsets.
 */
@Composable
fun PhysicsBubbleScreen() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val state = rememberBubbleState(screenWidthPx, screenHeightPx)

        PhysicsBubbleContent(state, screenWidthPx, screenHeightPx)
    }
}

/**
 * Main content layout.
 *
 * Manages the theme state, background drawing, text layers, and composes the
 * shader layer that renders the bubble. The background uses a circular-reveal
 * clip animation that expands from the toggle button position.
 */
@Composable
private fun PhysicsBubbleContent(
    state: PhysicsBubbleState,
    screenWidthPx: Float,
    screenHeightPx: Float,
) {
    val scope = rememberCoroutineScope()

    var isDarkTheme by remember { mutableStateOf(false) }
    var previousIsDark by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !isDarkTheme
        }
    }

    val lightBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx, screenHeightPx,
            BubbleColors.LIGHT_CENTER, BubbleColors.LIGHT_MID1,
            BubbleColors.LIGHT_MID2, BubbleColors.LIGHT_EDGE
        )
    }
    val darkBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx, screenHeightPx,
            BubbleColors.DARK_CENTER, BubbleColors.DARK_MID,
            BubbleColors.DARK_MID, BubbleColors.DARK_EDGE
        )
    }

    val textTween = remember {
        tween<Color>(BubbleConfig.TEXT_ANIM_DURATION, easing = FastOutLinearInEasing)
    }
    val mainTextColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_MAIN_TEXT else BubbleColors.LIGHT_MAIN_TEXT,
        animationSpec = textTween, label = "mainText"
    )
    val titleColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_TITLE else BubbleColors.LIGHT_TITLE,
        animationSpec = textTween, label = "title"
    )
    val subtitleColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_SUBTITLE else BubbleColors.LIGHT_SUBTITLE,
        animationSpec = textTween, label = "subtitle"
    )

    DeformationFrameLoop(state)

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(KINEMATIC_LENS_SHADER)
        } else null
    }

    val revealClipPath = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bubbleDragInput(state, scope)
            .bubbleTapInput(state, scope)
            .bubbleShaderLayer(state, shader)
            .drawBehind {
                drawThemeBackground(
                    isDarkTheme = isDarkTheme,
                    previousIsDark = previousIsDark,
                    revealProgress = state.themeRevealProgress.value,
                    lightBrush = lightBrush,
                    darkBrush = darkBrush,
                    reusablePath = revealClipPath,
                )
            }
    ) {
        ThemeToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = {
                if (state.themeRevealProgress.isRunning) return@ThemeToggleButton
                previousIsDark = isDarkTheme
                isDarkTheme = !isDarkTheme
                scope.launch {
                    state.themeRevealProgress.snapTo(0f)
                    state.themeRevealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BubbleConfig.THEME_REVEAL_DURATION,
                            easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f)
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
        )

        Text(
            text = "Pixels are now\nphysical.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp,
            color = mainTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .graphicsLayer { alpha = 1f - (state.progress * 4).coerceIn(0f, 1f) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = state.textYOffsetPx.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { alpha = (state.progress * 3).coerceIn(0f, 1f) }
            ) {
                Text(
                    text = "AGSL Pipelines",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = titleColor
                )
                Text(
                    text = "Real-time thin-film interference\ndriven by kinematic springs.",
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    color = subtitleColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            BubbleFallback(state)
        }
    }
}

/**
 * Sun/moon toggle for switching between light and dark themes.
 *
 * Animates between a sun (with rays) and a crescent moon using path operations.
 * The crescent is created by subtracting a second circle from the main one
 * via [PathOperation.Difference]. Paths are allocated once and reused to
 * avoid GC pressure during drawing.
 *
 * @param isDarkTheme Current theme state.
 * @param onToggle Called when the button is tapped.
 * @param modifier Modifier for positioning.
 */
@Composable
private fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isDarkTheme) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "theme_morph"
    )

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isDarkTheme) {
        scaleAnim.snapTo(0.85f)
        scaleAnim.animateTo(
            1f,
            spring(dampingRatio = 0.6f, stiffness = 400f)
        )
    }

    val mainPath = remember { Path() }
    val cutoutPath = remember { Path() }
    val finalPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .padding(6.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f

        val sunColor = Color(0xFFFDB813)
        val moonColor = Color(0xFFE5E5EA)
        val currentColor = androidx.compose.ui.graphics.lerp(sunColor, moonColor, progress)

        rotate(
            degrees = progress * -90f,
            pivot = center
        ) {
            val rayAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
            if (rayAlpha > 0f) {
                val rayLength = maxRadius * 0.25f
                val rayOffset = maxRadius * 0.6f
                for (i in 0 until 8) {
                    rotate(
                        degrees = i * 45f,
                        pivot = center
                    ) {
                        drawLine(
                            color = currentColor.copy(alpha = rayAlpha),
                            start = center.copy(y = center.y - rayOffset),
                            end = center.copy(y = center.y - rayOffset - rayLength),
                            strokeWidth = maxRadius * 0.15f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            val sunRadius = maxRadius * 0.45f
            val moonRadius = maxRadius * 0.85f
            val currentRadius = sunRadius + (moonRadius - sunRadius) * progress

            mainPath.reset()
            mainPath.addOval(
                Rect(
                    left = center.x - currentRadius,
                    top = center.y - currentRadius,
                    right = center.x + currentRadius,
                    bottom = center.y + currentRadius
                )
            )

            val cutoutStartOffset = Offset(center.x + maxRadius * 2f, center.y - maxRadius * 2f)
            val cutoutEndOffset =
                Offset(center.x + currentRadius * 0.3f, center.y - currentRadius * 0.3f)

            val cutoutX = cutoutStartOffset.x + (cutoutEndOffset.x - cutoutStartOffset.x) * progress
            val cutoutY = cutoutStartOffset.y + (cutoutEndOffset.y - cutoutStartOffset.y) * progress
            val cutoutRadius = currentRadius * 0.95f

            cutoutPath.reset()
            cutoutPath.addOval(
                Rect(
                    left = cutoutX - cutoutRadius,
                    top = cutoutY - cutoutRadius,
                    right = cutoutX + cutoutRadius,
                    bottom = cutoutY + cutoutRadius
                )
            )

            finalPath.reset()
            finalPath.op(mainPath, cutoutPath, PathOperation.Difference)

            drawPath(path = finalPath, color = currentColor)
        }
    }
}

/**
 * Simple gradient circle fallback for devices below API 33 where AGSL is unavailable.
 */
@Composable
private fun BubbleFallback(state: PhysicsBubbleState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = state.bubblePos.value,
                radius = state.currentOrbRadius
            )
        )
    }
}

/**
 * Per-frame loop that drives the bubble's squash/stretch deformation.
 *
 * ## How it works
 *
 * Each frame, we compute the bubble's velocity from its position delta.
 * That velocity is smoothed with a low-pass filter and fed as the target
 * into a damped spring. The spring is stepped manually using semi-implicit
 * Euler integration (not a Compose `animateTo` call) to avoid allocating
 * a new coroutine every frame.
 *
 * The spring parameters (stiffness=1500, damping=34.8) are derived from
 * the equivalent Compose spring with `dampingRatio=0.45` and `StiffnessMedium`.
 * The relationship is: `damping = 2 * dampingRatio * sqrt(stiffness)`.
 *
 * Delta time is capped at 32ms to prevent the spring from exploding
 * after a lag spike (e.g. GC pause or app backgrounding).
 *
 * This loop also advances [PhysicsBubbleState.shaderTime] so the AGSL shader
 * can animate its noise-based film thickness variation over time.
 */
@Composable
private fun DeformationFrameLoop(state: PhysicsBubbleState) {
    LaunchedEffect(state) {
        var previousActualPos = state.bubblePos.value
        val startTime = withFrameNanos { it }
        var lastFrameTime = startTime
        var smoothedVelocity = Offset.Zero
        var defVelocity = Offset.Zero

        val stiffness = 1500f
        val damping = 34.8f

        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.032f)
            lastFrameTime = frameTime

            state.shaderTime[0] = (frameTime - startTime) / 1_000_000_000f

            val currentActualPos = state.bubblePos.value
            val rawVelocity = currentActualPos - previousActualPos

            smoothedVelocity = Offset(
                x = smoothedVelocity.x + (rawVelocity.x - smoothedVelocity.x) * BubbleConfig.VELOCITY_SMOOTHING,
                y = smoothedVelocity.y + (rawVelocity.y - smoothedVelocity.y) * BubbleConfig.VELOCITY_SMOOTHING
            )

            if (state.popAnim.value == 0f) {
                val targetDeformation = Offset(
                    x = (smoothedVelocity.x * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP,
                        BubbleConfig.DEFORMATION_CLAMP
                    ),
                    y = (smoothedVelocity.y * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP,
                        BubbleConfig.DEFORMATION_CLAMP
                    )
                )

                val currentDef = state.deformationAnim.value
                val forceX =
                    (targetDeformation.x - currentDef.x) * stiffness - defVelocity.x * damping
                val forceY =
                    (targetDeformation.y - currentDef.y) * stiffness - defVelocity.y * damping

                defVelocity = Offset(defVelocity.x + forceX * dt, defVelocity.y + forceY * dt)
                val nextDef =
                    Offset(currentDef.x + defVelocity.x * dt, currentDef.y + defVelocity.y * dt)

                state.deformationAnim.snapTo(nextDef)
            } else {
                state.deformationAnim.snapTo(Offset.Zero)
                defVelocity = Offset.Zero
            }
            previousActualPos = currentActualPos
        }
    }
}

/**
 * Handles vertical drag gestures to move the bubble.
 *
 * The bubble starts in a "locked" mode where it can only move vertically
 * along the center axis. Once dragged to the top snap position, it "unlocks"
 * and allows free XY movement. On release, it snaps to whichever endpoint
 * (top or bottom) is closer, using spring animations.
 */
private fun Modifier.bubbleDragInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope,
): Modifier = pointerInput(Unit) {
    var isUnlocked = false
    detectDragGestures(
        onDragStart = { isUnlocked = state.isAtTop() },
        onDragEnd = {
            scope.launch {
                if (isUnlocked) {
                    if (state.bubblePos.value.y < state.midPoint) {
                        state.bubblePos.animateTo(
                            Offset(state.centerX, state.topOrbCenterY),
                            UnlockedSnapSpring
                        )
                    } else {
                        state.bubblePos.animateTo(
                            Offset(state.centerX, state.bottomOrbCenterY),
                            SnapBackSpring
                        )
                    }
                } else {
                    val targetY =
                        if (state.bubblePos.value.y < state.midPoint) state.topOrbCenterY else state.bottomOrbCenterY
                    state.bubblePos.animateTo(Offset(state.centerX, targetY), SnapBackSpring)
                }
            }
        }
    ) { change, dragAmount ->
        if (state.popAnim.value > 0f) return@detectDragGestures
        change.consume()
        val proposedY = state.bubblePos.value.y + dragAmount.y
        if (!isUnlocked && proposedY <= state.topOrbCenterY) isUnlocked = true
        if (isUnlocked) {
            scope.launch {
                state.bubblePos.snapTo(Offset(state.bubblePos.value.x + dragAmount.x, proposedY))
            }
        } else {
            val clampedY = proposedY.coerceAtMost(state.maxDragY)
            scope.launch { state.bubblePos.snapTo(Offset(state.centerX, clampedY)) }
        }
    }
}

/**
 * Handles tap gestures to pop the bubble.
 *
 * On tap, the bubble fades out over [BubbleConfig.POP_DURATION] ms,
 * waits [BubbleConfig.POP_DELAY] ms, then respawns at the rest position.
 */
private fun Modifier.bubbleTapInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope,
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onTap = {
            if (state.popAnim.value == 0f) {
                scope.launch {
                    state.popAnim.animateTo(
                        1f,
                        tween(BubbleConfig.POP_DURATION, easing = FastOutLinearInEasing)
                    )
                    delay(BubbleConfig.POP_DELAY)
                    state.popAnim.snapTo(0f)
                    state.bubblePos.snapTo(Offset(state.centerX, state.bottomOrbCenterY))
                }
            }
        }
    )
}

/**
 * Applies the AGSL [RuntimeShader] as a [RenderEffect] on the graphics layer.
 *
 * Passes all bubble state (position, radius, deformation, pop progress, time)
 * as shader uniforms each frame. The shader receives the composable's rendered
 * content as `uniform shader composable` and applies the bubble optics on top.
 *
 * No-op on API < 33 where [RuntimeShader] is unavailable.
 */
private fun Modifier.bubbleShaderLayer(
    state: PhysicsBubbleState,
    shader: RuntimeShader?,
): Modifier = graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
        shader.setFloatUniform("touchCenter", state.bubblePos.value.x, state.bubblePos.value.y)
        shader.setFloatUniform("radius", state.currentOrbRadius)
        shader.setFloatUniform("progress", state.progress)
        shader.setFloatUniform(
            "deformation",
            state.deformationAnim.value.x,
            state.deformationAnim.value.y
        )
        shader.setFloatUniform("popProgress", state.popAnim.value)
        shader.setFloatUniform("sysTime", state.shaderTime[0])

        renderEffect = RenderEffect.createRuntimeShaderEffect(
            shader, "composable"
        ).asComposeRenderEffect()
    }
}

/**
 * Creates a radial gradient [Brush] centered at 40% screen height.
 * Used for both light and dark theme backgrounds.
 */
private fun createRadialBrush(
    screenWidthPx: Float,
    screenHeightPx: Float,
    center: Color,
    mid1: Color,
    mid2: Color,
    edge: Color,
): Brush = Brush.radialGradient(
    0.0f to center,
    0.3f to mid1,
    0.7f to mid2,
    1.0f to edge,
    center = Offset(screenWidthPx / 2f, screenHeightPx * 0.4f)
)

/**
 * Draws the theme background with a circular-reveal transition.
 *
 * During a theme switch, the previous theme's background is drawn first as a full rect.
 * The new theme is then drawn inside a growing circular clip path that expands from
 * the top-right corner (near the toggle button). Once the reveal completes, only
 * the current theme's background is drawn.
 *
 * Uses [reusablePath] to avoid allocating a new [Path] every frame during the animation.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThemeBackground(
    isDarkTheme: Boolean,
    previousIsDark: Boolean,
    revealProgress: Float,
    lightBrush: Brush,
    darkBrush: Brush,
    reusablePath: Path,
) {
    val currentBrush = if (isDarkTheme) darkBrush else lightBrush
    val prevBrush = if (previousIsDark) darkBrush else lightBrush

    drawRect(brush = prevBrush)

    if (revealProgress < 1f) {
        val maxRadius = hypot(size.width, size.height)
        val currentRevealRadius = revealProgress * maxRadius
        val epicenter = Offset(size.width - 100f, 150f)

        reusablePath.reset()
        reusablePath.addOval(
            Rect(
                left = epicenter.x - currentRevealRadius,
                top = epicenter.y - currentRevealRadius,
                right = epicenter.x + currentRevealRadius,
                bottom = epicenter.y + currentRevealRadius
            )
        )

        clipPath(reusablePath) {
            drawRect(brush = currentBrush)
        }
    } else {
        drawRect(brush = currentBrush)
    }
}

/**
 * AGSL thin-film interference shader.
 *
 * This shader runs per-pixel on the GPU and produces the soap bubble effect.
 * It receives the composable's rendered content as `uniform shader composable`
 * and applies the following optical effects:
 *
 * ## Thin-film interference
 *
 * Models a thin film of soapy water (n=1.33) bounded by air (n=1.0).
 * For each pixel, we compute:
 * 1. The surface normal of the sphere from the UV coordinates.
 * 2. The refracted angle inside the film using Snell's law.
 * 3. The film thickness at that point (varies with gravity, noise, and time).
 * 4. The optical path difference: `OPD = 2 * n * d * cos(θt)`.
 * 5. The interference intensity for R (650nm), G (532nm), and B (450nm)
 *    using `0.5 + 0.5 * cos(2π * OPD / λ)`.
 *
 * ## Fresnel reflection
 *
 * Uses the Schlick approximation to compute angle-dependent reflectance.
 * At the rim (grazing angles), the bubble becomes more reflective and
 * the interference colors fade to white.
 *
 * ## Chromatic aberration
 *
 * Samples the background at three different offsets for R, G, B channels,
 * simulating wavelength-dependent refraction through a curved lens.
 *
 * ## Environment reflection
 *
 * Samples the background at normal-offset positions with a 5-tap blur
 * to simulate the distorted reflection visible on real bubbles.
 *
 * ## Volume-preserving deformation
 *
 * Stretches the bubble along the movement direction and squashes it
 * perpendicular to that direction. The transform preserves area:
 * `stretch = 1 + speed`, `squash = 1 / sqrt(stretch)`.
 *
 * ## Tuning knobs (defined at the top of the shader body)
 *
 * - `THICKNESS_BASE`: Center film thickness in nm. Controls dominant color.
 * - `THICKNESS_GRAVITY`: How much gravity thins the top vs thickens the bottom.
 * - `THICKNESS_SWIRL` / `THICKNESS_DETAIL`: Noise amplitudes for organic turbulence.
 * - `COLOR_INTENSITY`: Vividity multiplier for interference colors.
 * - `EDGE_FADE_END`: Where interference fades to white Fresnel at the rim.
 * - `ENV_REFLECTION_STRENGTH`: How much of the environment is reflected.
 * - `ENV_BLUR_RADIUS`: Blur radius for reflected environment samples.
 */
@Language("AGSL")
const val KINEMATIC_LENS_SHADER = """
uniform shader composable;
uniform float2 touchCenter;
uniform float radius;
uniform float progress; 
uniform float2 deformation; 
uniform float popProgress;
uniform float sysTime;

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

float smoothNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}

half4 main(float2 fragCoord) {

    // ================================================================
    // TUNING KNOBS — adjust these to taste
    // ================================================================

    // Film thickness center (nm). Controls the dominant color.
    //   200–300 = blues/violets,  300–400 = greens/yellows,
    //   400–550 = oranges/pinks,  550+ = higher-order pastels.
    float THICKNESS_BASE = 300.0;

    // How much gravity thins the top vs thickens the bottom (nm).
    // 0 = uniform thickness. Higher = more color variation top-to-bottom.
    float THICKNESS_GRAVITY = 120.0;

    // Noise amplitude — organic swirly turbulence (nm).
    // 0 = clean bands. Higher = more chaotic, soap-like flow.
    float THICKNESS_SWIRL = 100.0;
    float THICKNESS_DETAIL = 40.0;

    // Color intensity multiplier. Controls how vivid the interference is.
    // 1.0 = physically dim (barely visible). 2.0 = natural soap bubble.
    // 4.0+ = exaggerated/artistic.
    float COLOR_INTENSITY = 2.0;

    // Edge white fade — where interference fades to white Fresnel.
    // Lower = fade starts earlier (more white rim). Higher = colors
    // extend further to the edge. Range: 0.05 – 0.4
    float EDGE_FADE_END = 0.20;

    // Ambient environment reflection strength.
    // 0.0 = no environment reflection. 1.0 = full mirror.
    // 0.3–0.5 = subtle, realistic bubble environment pickup.
    float ENV_REFLECTION_STRENGTH = 0.4;

    // Environment blur radius (pixels). How blurry the reflected
    // environment appears. Real bubbles show very distorted reflections.
    // 30–80 = natural. Higher = more diffuse.
    float ENV_BLUR_RADIUS = 50.0;

    // ================================================================
    half4 rawBackground = composable.eval(fragCoord);
    if (popProgress >= 1.0) return rawBackground;

    float2 rawUv = fragCoord - touchCenter;
    
    float speed = length(deformation); 
    float2 moveDir = speed > 0.001 ? deformation / speed : float2(0.0, 1.0); 
    
    float parallelDist = dot(rawUv, moveDir);
    float2 perpVector = rawUv - moveDir * parallelDist;
    
    float stretch = 1.0 + speed; 
    float squash = 1.0 / sqrt(stretch); 
    
    float2 uv = (moveDir * (parallelDist / stretch)) + (perpVector / squash);
    float dist = length(uv);
    float activeRadius = radius * (1.0 + popProgress * 1.5);

    if (dist >= activeRadius) {
        return rawBackground;
    }

    // Surface geometry
    float2 nUv = uv / activeRadius;
    float distSq = dot(nUv, nUv);
    float z = sqrt(max(0.0, 1.0 - distSq));
    float3 normal = normalize(float3(nUv, z));
    float3 viewDir = float3(0.0, 0.0, 1.0); 
    float NdotV = max(0.0, dot(normal, viewDir));

    // Refraction with chromatic aberration
    float magnification = 0.45;
    float lensDeform = (1.0 - z) * magnification * (1.0 - popProgress);
    
    float2 refUvR = fragCoord - (nUv * activeRadius * (lensDeform * 0.88));
    float2 refUvG = fragCoord - (nUv * activeRadius * (lensDeform * 1.00));
    float2 refUvB = fragCoord - (nUv * activeRadius * (lensDeform * 1.12));

    half3 bgColor = half3(
        composable.eval(refUvR).r,
        composable.eval(refUvG).g,
        composable.eval(refUvB).b
    );

    // Lighting vectors
    float3 reflectionDir = reflect(-viewDir, normal);
    float3 lightDir1 = normalize(float3(0.6, 0.7, 0.8));
    float3 lightDir2 = normalize(float3(-0.5, -0.4, 0.6));
    
    float lightAlign1 = max(0.0, dot(reflectionDir, lightDir1));
    float lightAlign2 = max(0.0, dot(reflectionDir, lightDir2));

    // ================================================================
    // PHYSICALLY-BASED THIN-FILM INTERFERENCE
    //
    // Models a soap bubble: thin film of soapy water (n=1.33)
    // bounded by air (n=1.0) on both sides.
    //
    // Physics:
    //   1. Snell's law:  sin(θ_i) = n_film * sin(θ_t)
    //   2. Optical path difference: Δ = 2 * n_film * d * cos(θ_t)
    //   3. Phase shift of π at first surface (air→film, low-to-high n)
    //   4. Reflectance per λ: R(λ) = 2*R0*(1 - cos(2π·Δ/λ + π))
    //      which simplifies to: R(λ) = 2*R0*(1 + cos(2π·Δ/λ))
    //   5. Fresnel R0 at normal incidence = ((n-1)/(n+1))^2
    //   6. Full Fresnel via Schlick approximation for angle dependence.
    //
    // We sample the visible spectrum at three representative wavelengths:
    //   R ≈ 650nm,  G ≈ 532nm,  B ≈ 450nm
    // ================================================================

    // Film properties
    float n_film = 1.33;       // Refractive index of soapy water
    float n_air  = 1.0;

    // Fresnel reflectance at normal incidence: ((n1-n2)/(n1+n2))^2
    float R0 = pow((n_film - n_air) / (n_film + n_air), 2.0); // ≈ 0.02

    // Schlick Fresnel — angle-dependent reflectance
    float fresnel = R0 + (1.0 - R0) * pow(1.0 - NdotV, 5.0);

    // Snell's law: cos(θ_t) inside the film
    // sin(θ_i) = sqrt(1 - cos²(θ_i)) = sqrt(1 - NdotV²)
    float sinThetaI = sqrt(max(0.0, 1.0 - NdotV * NdotV));
    float sinThetaT = sinThetaI / n_film;
    float cosThetaT = sqrt(max(0.0, 1.0 - sinThetaT * sinThetaT));

    // Film thickness varies across the surface — thinner at top (gravity),
    // thicker at bottom, with fluid noise for organic turbulence.
    // Range ~100nm to ~800nm covers the full visible interference spectrum.
    float swirl = smoothNoise(nUv * 3.0 + sysTime * 0.12);
    float thicknessNoise = smoothNoise(nUv * 5.0 - sysTime * 0.08);

    // Base thickness: gravity thins the top, thickens the bottom.
    // nUv.y goes from -1 (top of sphere) to +1 (bottom).
    float baseThickness = THICKNESS_BASE + nUv.y * THICKNESS_GRAVITY;
    float thickness = baseThickness
        + swirl * THICKNESS_SWIRL
        + thicknessNoise * THICKNESS_DETAIL;

    // Clamp to physically meaningful range
    thickness = clamp(thickness, 80.0, 900.0);

    // Optical path difference (nm)
    float opd = 2.0 * n_film * thickness * cosThetaT;

    // Wavelengths in nm for RGB channels
    float lambda_R = 650.0;
    float lambda_G = 532.0;
    float lambda_B = 450.0;

    // Thin-film reflectance per channel.
    //
    // The interference oscillation gives vivid color across most of the
    // surface. Only at the very edge (grazing angles) do we fade toward
    // uniform white Fresnel — on a real bubble, the extreme rim goes
    // silvery because all wavelengths reflect almost equally there.
    //
    // The key balance: interference stays vivid across ~85% of the
    // surface. The white fade only kicks in at the outermost rim.

    float TWO_PI = 6.2831853;

    // Interference oscillation per channel: ranges [0, 1]
    float oscR = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_R);
    float oscG = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_G);
    float oscB = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_B);

    half3 interferenceColor = half3(oscR, oscG, oscB);

    // Only fade interference at the very edge — NdotV < 0.15 is the
    // outermost ~15% of the sphere where grazing-angle washout happens.
    float interferenceStrength = smoothstep(0.0, EDGE_FADE_END, NdotV);

    // Amplify the interference color — pure Fresnel * [0,1] is too dim
    // because R0 ≈ 0.02 at normal incidence. Real bubbles appear more
    // vivid than bare Fresnel predicts because:
    //   - Both surfaces of the film reflect (constructive doubling)
    //   - The eye adapts to the transparency and perceives the color
    //   - Environment light contributes from all angles
    //
    // We scale by 2.5 to approximate the double-surface amplification
    // and perceptual brightness, while keeping it energy-plausible.
    half3 filmReflection = interferenceColor * fresnel * COLOR_INTENSITY;

    // At the rim, blend toward clean white Fresnel (no color)
    half3 whiteReflection = half3(fresnel);

    half3 thinFilmColor = mix(whiteReflection, filmReflection, interferenceStrength);

    // ================================================================
    // SPECULAR HIGHLIGHTS — physically motivated
    //
    // Blinn-Phong model with two light sources.
    // The high exponent on light1 gives a tight sun-like glint.
    // Light2 is broader and dimmer — an environment fill.
    // ================================================================
    float spec1 = pow(lightAlign1, 250.0) * 2.5;
    float spec2 = pow(lightAlign2, 60.0) * 0.5;
    half3 highlights = half3(spec1 + spec2);

    // ================================================================
    // AMBIENT ENVIRONMENT REFLECTION
    //
    // Real bubbles reflect their surroundings — you can see distorted
    // trees, sky, objects in a soap bubble. We approximate this by
    // sampling the composable (screen content) at offset positions
    // based on the surface normal, creating a blurred pseudo-reflection.
    //
    // We take 5 samples in a cross pattern around the reflection point
    // to simulate the blurry, distorted reflection a curved film produces.
    // The reflection is modulated by Fresnel — stronger at edges
    // (where real reflections are most visible on bubbles).
    // ================================================================
    float2 reflectOffset = normal.xy * ENV_BLUR_RADIUS;
    float2 envCenter = fragCoord + reflectOffset;

    // 5-tap blur: center + 4 cardinal offsets for a soft box
    float blurStep = ENV_BLUR_RADIUS * 0.4;
    half3 envSample = composable.eval(envCenter).rgb * 0.4
        + composable.eval(envCenter + float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter - float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter + float2(0.0, blurStep)).rgb * 0.15
        + composable.eval(envCenter - float2(0.0, blurStep)).rgb * 0.15;

    // Modulate by Fresnel and user-controlled strength
    half3 envReflection = envSample * fresnel * ENV_REFLECTION_STRENGTH;

    // ================================================================
    // COMPOSITE
    // ================================================================

    // Rim darkening — simulates light absorption at grazing angles
    // where the optical path through the film is longest.
    float rimShadow = smoothstep(0.92, 1.0, sqrt(distSq));
    bgColor *= (1.0 - rimShadow * 0.25);

    // The refracted background shows through (1 - reflectance),
    // thin-film color and environment reflection are additive on top,
    // specular highlights go on last — energy-conserving blend.
    half3 finalColor = bgColor * (1.0 - half3(fresnel))
        + thinFilmColor
        + envReflection
        + highlights;

    float fadeOut = 1.0 - pow(popProgress, 0.5);
    return half4(mix(rawBackground.rgb, finalColor, fadeOut), rawBackground.a);
}
"""