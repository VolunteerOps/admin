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

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// ------------------------------------------------------------------
// DATA MODELS & CONSTANTS
// ------------------------------------------------------------------

/**
 * Transport control icons for the skeuomorphic button panel.
 */
enum class RetroIcon { REC, REW, PLAY, FF, STOP }

/**
 * Playback states that drive the tape animation loop.
 * Each state maps to a speed multiplier in the frame loop.
 */
enum class TapeState { STOPPED, PLAYING, FF, REW }

/**
 * ## Why top-level color constants matter
 *
 * Every `Color(0xFF...)` call inside a composable or draw lambda
 * allocates a new Color object per frame or recomposition.
 * Top-level vals are created once and reused across all frames,
 * eliminating thousands of allocations per second during animation.
 *
 * This is especially critical inside `drawBehind`/`drawWithCache`
 * blocks that execute every frame at 60-120fps.
 */
private val VintageInkColor = Color(0xFFA19385)
private val ActiveLyricColor = Color(0xFF3E2723)
private val DarkTextColor = Color(0xFF221F1C)

private val BgCenterColor = Color(0xFFFAF6F0)
private val BgEdgeColor = Color(0xFFDFD6C2)
private val BgVignetteColor = Color(0x1A4A3B2A)

private val TapeBrownColor = Color(0xFF523321)
private val TapeDarkColor = Color(0xFF22140D)
private val WindowBgColor = Color(0xFF0D0D0D)

private val ControlPanelBgColor = Color(0xFF151515)
private val MarkColor = Color(0xFF7A7366)

/**
 * ## Lambda-based state deferral: the zero-cost constant
 *
 * When a composable accepts `() -> Float` instead of `Float`,
 * the caller decides WHEN the value is read. This top-level lambda
 * always returns 0f without any state read, so it never triggers
 * invalidation in the draw phase. Used by [PagerScreen] where
 * reel rotation is always static.
 *
 * Compare with PlayerScreen where the lambda reads from a
 * [MutableFloatState], deferring the state read to draw time.
 */
private val ZERO_ROTATION: () -> Float = { 0f }

/**
 * Darkens this color by reducing RGB channels proportionally.
 * Avoids creating intermediate color objects by working directly
 * with the float components.
 */
fun Color.darken(fraction: Float = 0.2f): Color = Color(
    red = (red * (1f - fraction)).coerceIn(0f, 1f),
    green = (green * (1f - fraction)).coerceIn(0f, 1f),
    blue = (blue * (1f - fraction)).coerceIn(0f, 1f),
    alpha = alpha
)

/**
 * ## drawWithCache for static backgrounds
 *
 * [drawWithCache] separates expensive setup (gradient creation) from
 * per-frame drawing. The [Brush] objects here are only recreated when
 * the composable's size changes, not on every recomposition.
 *
 * If this were `drawBehind`, every recomposition of the parent would
 * recreate both radial gradients. With `drawWithCache`, they survive
 * recomposition as long as size remains stable.
 */
fun Modifier.vintageBackground() = this.drawWithCache {
    val bgGradient = Brush.radialGradient(
        colors = listOf(BgCenterColor, BgEdgeColor),
        center = Offset(size.width * 0.5f, size.height * 0.3f),
        radius = size.maxDimension * 0.85f
    )
    val bgCenter = Offset(size.width / 2f, size.height / 2f)
    val vignetteGradient = Brush.radialGradient(
        colors = listOf(Color.Transparent, BgVignetteColor),
        center = bgCenter,
        radius = size.maxDimension * 0.8f
    )
    onDrawBehind {
        drawRect(brush = bgGradient)
        drawRect(brush = vignetteGradient)
    }
}

data class CassetteTape(
    val id: String,
    val artist: String,
    val title: String,
    val year: String,
    val bodyColor: Color,
    val labelColor: Color,
    val stripeColor: Color,
    val lyrics: List<String>,
    val easterEgg: String? = null,
)

val TapeLibrary = listOf(
    CassetteTape(
        "1", "John Denver", "Take Me Home, Country Roads", "1971",
        Color(0xFFE6DBC4), Color(0xFFF4EFE6), Color(0xFFD94532),
        listOf(
            "Almost heaven, West Virginia",
            "Blue Ridge Mountains, Shenandoah River",
            "Life is old there, older than the trees",
            "Younger than the mountains,",
            "growin' like a breeze",
            "Country roads, take me home",
            "To the place I belong",
            "West Virginia, mountain mama",
            "Take me home, country roads",
        ),
    ),
    CassetteTape(
        "2", "Fleetwood Mac", "Dreams", "1977",
        Color(0xFF2A2A2A), Color(0xFFDCD5C6), Color(0xFFD6A033),
        listOf(
            "Now here you go again, you say",
            "You want your freedom",
            "Well, who am I to keep you down?",
            "It's only right that you should",
            "Play the way you feel it",
            "But listen carefully to the sound",
            "Of your loneliness",
            "Like a heartbeat drives you mad",
            "In the stillness of remembering what you had",
        ),
    ),
    CassetteTape(
        "3", "The Beatles", "Here Comes The Sun", "1969",
        Color(0xFFE86658), Color(0xFFF4EFE6), Color(0xFF1E3A5F),
        listOf(
            "Here comes the sun, doo-doo-doo-doo",
            "Here comes the sun, and I say",
            "It's alright",
            "Little darling, it's been a long cold lonely winter",
            "Little darling, it feels like years since it's been here",
            "Here comes the sun",
            "Here comes the sun, and I say",
            "It's alright",
            "Sun, sun, sun, here it comes",
        ),
    ),
    CassetteTape(
        "4", "David Bowie", "Space Oddity", "1969",
        Color(0xFF1E3A5F), Color(0xFFE0E0E0), Color(0xFFE25A3B),
        listOf(
            "Ground Control to Major Tom",
            "Ground Control to Major Tom",
            "Take your protein pills and put your helmet on",
            "Ground Control to Major Tom",
            "Commencing countdown, engines on",
            "Check ignition and may God's love be with you",
            "This is Ground Control to Major Tom",
            "You've really made the grade",
            "And the papers want to know whose shirts you wear",
        ),
    ),
    CassetteTape(
        "5", "Eagles", "Hotel California", "1976",
        Color(0xFF2E4035), Color(0xFFE8E3D5), Color(0xFFD6A033),
        listOf(
            "On a dark desert highway, cool wind in my hair",
            "Warm smell of colitas, rising up through the air",
            "Up ahead in the distance, I saw a shimmering light",
            "My head grew heavy and my sight grew dim",
            "I had to stop for the night",
            "There she stood in the doorway",
            "I heard the mission bell",
            "And I was thinking to myself",
            "This could be Heaven or this could be Hell",
        ),
        easterEgg = "Follow for more",
    ),
)

// ------------------------------------------------------------------
// APP NAVIGATION (SHARED TRANSITION ARCHITECTURE)
// ------------------------------------------------------------------

/**
 * ## Root composable with SharedTransitionLayout
 *
 * [SharedTransitionLayout] + [AnimatedContent] enables shared element
 * transitions between the pager and player screens. The cassette card
 * morphs smoothly between its rotated pager position and the player.
 *
 * ### State management note
 * [lastActiveCassette] persists the selection during the exit animation.
 * Without it, the shared element would lose its identity when
 * [selectedCassette] becomes null, causing a visual pop.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CassetteTapeDemo() {
    var selectedCassette by remember { mutableStateOf<CassetteTape?>(null) }
    var lastActiveCassette by remember { mutableStateOf<CassetteTape?>(null) }

    val pagerState = rememberPagerState(pageCount = { TapeLibrary.size })

    if (selectedCassette != null) {
        lastActiveCassette = selectedCassette
    }

    val activeCassetteId = (selectedCassette ?: lastActiveCassette)?.id

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedCassette,
            label = "app_nav",
            transitionSpec = {
                fadeIn(spring(dampingRatio = 0.8f, stiffness = 250f)) togetherWith
                        fadeOut(spring(dampingRatio = 0.8f, stiffness = 250f))
            },
        ) { target ->
            if (target == null) {
                PagerScreen(
                    pagerState = pagerState,
                    onCassetteSelected = { selectedCassette = it },
                    transitioningCassetteId = activeCassetteId,
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            } else {
                PlayerScreen(
                    cassette = target,
                    onBack = { selectedCassette = null },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// 1. THE LIBRARY HORIZONTAL PAGER SCREEN
// ------------------------------------------------------------------

/**
 * Horizontal pager displaying the tape library with perspective scaling.
 *
 * ### Shared element key convention
 * Each cassette uses `"cassette-$id"` as its shared content state key.
 * Both [PagerScreen] and [PlayerScreen] must use the exact same key
 * for the transition to connect the two visual representations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PagerScreen(
    pagerState: PagerState,
    onCassetteSelected: (CassetteTape) -> Unit,
    transitioningCassetteId: String?,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .vintageBackground(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Text(
                text = "MIXTAPES",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = DarkTextColor,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier
                    .padding(top = 48.dp, start = 32.dp, bottom = 16.dp)
                    .fillMaxWidth(),
            )

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val cassette = TapeLibrary[page]
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val scale = lerp(1f, 0.85f, absOffset)
                val alpha = lerp(1f, 0.5f, absOffset)

                val isActivelyTransitioning = cassette.id == transitioningCassetteId

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val animatedRotation by animatedVisibilityScope.transition.animateFloat(
                        transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 250f) },
                        label = "rot",
                    ) { state ->
                        if (state == EnterExitState.Visible) -90f else 0f
                    }

                    val finalRotationZ = if (isActivelyTransitioning) animatedRotation else -90f

                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "cassette-${cassette.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        spring(dampingRatio = 0.8f, stiffness = 250f)
                                    },
                                    zIndexInOverlay = 1f,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onCassetteSelected(cassette) }
                                .graphicsLayer { rotationZ = finalRotationZ }
                                .requiredWidth(440.dp)
                                .aspectRatio(1.58f)
                                .shadow(24.dp, RoundedCornerShape(12.dp), clip = false),
                        ) {
                            CassetteTapeRenderer(
                                cassette = cassette,
                                rightReelRotation = ZERO_ROTATION,
                                leftReelRotation = ZERO_ROTATION,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                val currentCassette = TapeLibrary[pagerState.currentPage]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentCassette.artist,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = VintageInkColor,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentCassette.title,
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = DarkTextColor,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 2. THE TAPE PLAYER SCREEN
// ------------------------------------------------------------------

/**
 * ## The main performance-critical screen
 *
 * This screen drives per-frame animation for reel rotation and lyric
 * scrolling. Two key optimizations prevent full recomposition at 60fps:
 *
 * ### 1. mutableFloatStateOf instead of mutableStateOf(Float)
 * [mutableFloatStateOf] stores the float as a primitive, avoiding
 * autoboxing on every write. At 60fps that is 60 box allocations/sec
 * per state, times 3 animated floats = 180 unnecessary objects/sec.
 *
 * ### 2. Lambda-based state deferral with `() -> Float`
 * Instead of passing `rightReelRotation.floatValue` (a composition-phase
 * read that triggers recomposition), we pass `{ rightReelRotation.floatValue }`
 * (a lambda that defers the read to the draw phase).
 *
 * **Why this matters:** [CassetteTapeRenderer] uses [drawWithCache], which
 * caches expensive work like [Path] creation and text measurement. A
 * composition-phase state read would recompose the renderer every frame,
 * invalidating that cache and re-measuring text 60 times per second.
 * With lambda deferral, the cache survives and only the lightweight
 * `onDrawBehind` block re-executes.
 *
 * The same pattern applies to [VintageReelsLyrics] and its
 * [currentLyricProgress] parameter.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    cassette: CassetteTape,
    onBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    BackHandler { onBack() }

    val rightReelRotation = remember { mutableFloatStateOf(0f) }
    val leftReelRotation = remember { mutableFloatStateOf(0f) }
    val currentLyricProgress = remember { mutableFloatStateOf(0f) }

    var tapeState by remember { mutableStateOf(TapeState.STOPPED) }
    var volume by remember { mutableFloatStateOf(0.7f) }

    /**
     * ## Frame loop keyed on tapeState
     *
     * [LaunchedEffect] restarts when [tapeState] changes, resetting
     * the speed multiplier to 0. This gives a natural acceleration
     * ramp on each state transition.
     *
     * State writes inside [withFrameNanos] do NOT trigger recomposition
     * of PlayerScreen because no composable in the tree reads these
     * values during composition. They are consumed only via lambdas
     * in the draw phase.
     */
    LaunchedEffect(tapeState) {
        var lastFrameNanos = 0L
        val rightSpeedNominal = 120f
        val leftSpeedNominal = rightSpeedNominal * (0.12f / 0.23f)
        var currentSpeedMultiplier = 0f

        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
                val deltaTime = (frameTimeNanos - lastFrameNanos) / 1e9f
                lastFrameNanos = frameTimeNanos

                val targetSpeed = when (tapeState) {
                    TapeState.PLAYING -> 1f
                    TapeState.FF -> 6f
                    TapeState.REW -> -6f
                    TapeState.STOPPED -> 0f
                }

                val acceleration = 4f
                currentSpeedMultiplier +=
                    (targetSpeed - currentSpeedMultiplier) * (deltaTime * acceleration)

                val motorDrift = if (tapeState == TapeState.PLAYING) {
                    (1.0 + 0.03 * sin(frameTimeNanos * 1e-9 * (2.0 * PI / 4.0))).toFloat()
                } else {
                    1f
                }

                if (abs(currentSpeedMultiplier) > 0.01f) {
                    val frameDelta = currentSpeedMultiplier * motorDrift * deltaTime
                    rightReelRotation.floatValue += rightSpeedNominal * frameDelta
                    leftReelRotation.floatValue += leftSpeedNominal * frameDelta

                    val lyricSpeed = currentSpeedMultiplier * 0.25f
                    val newProgress =
                        currentLyricProgress.floatValue + (lyricSpeed * deltaTime)
                    val maxProgress =
                        (cassette.lyrics.size - 1).coerceAtLeast(0).toFloat()

                    when {
                        newProgress >= maxProgress &&
                                (tapeState == TapeState.PLAYING || tapeState == TapeState.FF) -> {
                            currentLyricProgress.floatValue = maxProgress
                            tapeState = TapeState.STOPPED
                        }

                        newProgress <= 0f && tapeState == TapeState.REW -> {
                            currentLyricProgress.floatValue = 0f
                            tapeState = TapeState.STOPPED
                        }

                        else -> {
                            currentLyricProgress.floatValue =
                                newProgress.coerceIn(0f, maxProgress)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .vintageBackground(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                VintageEjectButton(
                    onClick = onBack,
                    modifier = Modifier
                        .height(44.dp)
                        .width(110.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            with(animatedVisibilityScope) {
                VintageTitleHeader(
                    cassette = cassette,
                    tapeState = tapeState,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(40.dp)
                        .animateEnterExit(
                            enter = slideInVertically(
                                initialOffsetY = { -100 },
                                animationSpec = spring(0.75f, 200f),
                            ) + fadeIn(tween(400)),
                            exit = fadeOut(),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== SHARED CASSETTE ELEMENT =====
            val rotation by animatedVisibilityScope.transition.animateFloat(
                transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 250f) },
                label = "rot",
            ) { state ->
                if (state == EnterExitState.Visible) 0f else -90f
            }

            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = "cassette-${cassette.id}",
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = 250f)
                            },
                            zIndexInOverlay = 1f,
                        )
                        .graphicsLayer { rotationZ = rotation }
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1.58f)
                        .shadow(24.dp, RoundedCornerShape(24.dp), clip = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val cornerRadius = CornerRadius(size.height * 0.06f)
                                drawRoundRect(
                                    color = Color(0xFF0A0A0A),
                                    size = size,
                                    cornerRadius = cornerRadius,
                                )
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xDD000000), Color.Transparent),
                                        startY = 0f,
                                        endY = size.height * 0.4f,
                                    ),
                                    size = size,
                                    cornerRadius = cornerRadius,
                                )
                            },
                    )

                    CassetteTapeRenderer(
                        cassette = cassette,
                        rightReelRotation = { rightReelRotation.floatValue },
                        leftReelRotation = { leftReelRotation.floatValue },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            with(animatedVisibilityScope) {
                VintageReelsLyrics(
                    lyrics = cassette.lyrics,
                    currentProgress = { currentLyricProgress.floatValue },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.88f)
                        .animateEnterExit(
                            enter = slideInVertically(
                                initialOffsetY = { 200 },
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f),
                            ) + fadeIn(tween(600, 100)),
                            exit = fadeOut(),
                        ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // CONTROL PANEL
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .animateEnterExit(
                            enter = slideInVertically(
                                initialOffsetY = { 1500 },
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 150f),
                            ) + fadeIn(tween(500, 250)),
                            exit = slideOutVertically(
                                targetOffsetY = { 1500 },
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 150f),
                            ) + fadeOut(),
                        )
                        .shadow(2.dp, RoundedCornerShape(8.dp), clip = false)
                        .drawBehind {
                            val trackRadius = CornerRadius(8.dp.toPx())
                            drawRoundRect(
                                color = ControlPanelBgColor,
                                size = size,
                                cornerRadius = trackRadius,
                            )
                            val separatorY = 8.dp.toPx()
                            drawLine(
                                color = Color(0x33000000),
                                start = Offset(6.dp.toPx(), separatorY),
                                end = Offset(size.width - 6.dp.toPx(), separatorY),
                                strokeWidth = 2.dp.toPx(),
                            )
                            drawLine(
                                color = Color(0x1AFFFFFF),
                                start = Offset(6.dp.toPx(), separatorY + 1.dp.toPx()),
                                end = Offset(
                                    size.width - 6.dp.toPx(),
                                    separatorY + 1.dp.toPx(),
                                ),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        .padding(top = 24.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VintageVolumeTuner(
                            volume = volume,
                            onVolumeChange = { volume = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        val textMeasurer = rememberTextMeasurer()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                        ) {
                            val titleStyle = TextStyle(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MarkColor.copy(alpha = 0.8f),
                                letterSpacing = 2.sp,
                            )
                            val titleText = textMeasurer.measure(
                                text = "COMPONENT PANEL",
                                style = titleStyle,
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawText(
                                    textLayoutResult = titleText,
                                    topLeft = Offset(
                                        (size.width - titleText.size.width) / 2,
                                        0f,
                                    ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val btnModifier = Modifier.weight(1f)
                            AnimatedSkeuoButton(
                                icon = RetroIcon.REC,
                                isRed = true,
                                isToggled = tapeState == TapeState.PLAYING,
                                modifier = btnModifier,
                            ) { tapeState = TapeState.PLAYING }
                            AnimatedSkeuoButton(
                                icon = RetroIcon.REW,
                                isToggled = tapeState == TapeState.REW,
                                modifier = btnModifier,
                            ) { tapeState = TapeState.REW }
                            AnimatedSkeuoButton(
                                icon = RetroIcon.PLAY,
                                isToggled = tapeState == TapeState.PLAYING,
                                modifier = btnModifier,
                            ) { tapeState = TapeState.PLAYING }
                            AnimatedSkeuoButton(
                                icon = RetroIcon.FF,
                                isToggled = tapeState == TapeState.FF,
                                modifier = btnModifier,
                            ) { tapeState = TapeState.FF }
                            AnimatedSkeuoButton(
                                icon = RetroIcon.STOP,
                                isToggled = false,
                                modifier = btnModifier,
                            ) { tapeState = TapeState.STOPPED }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// SKEUOMORPHIC EJECT BUTTON COMPONENT
// ------------------------------------------------------------------

/**
 * Physical eject button with spring-animated press depth.
 *
 * ### graphicsLayer lambda for deferred reads
 * The content scaling is computed inside the [graphicsLayer] lambda
 * rather than in the composition body. The [graphicsLayer] lambda
 * executes during the layout/draw phase, so reading [pressDepth]
 * there avoids triggering recomposition. The [animateFloatAsState]
 * still drives the animation, but the composable body does not
 * re-execute on each animation frame.
 */
@Composable
fun VintageEjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val pressDepth by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "pressDepth",
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        tryAwaitRelease()
                        isPressed = false
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                        onClick()
                    },
                )
            }
            .drawBehind {
                val w = size.width
                val h = size.height
                val corner = 4.dp.toPx()
                val currentScale = 1f - (pressDepth * 0.035f)

                withTransform({
                    scale(currentScale, currentScale, Offset(w / 2, h / 2))
                }) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply { color = Color(0xFF070707) }
                        val fwPaint = paint.asFrameworkPaint()
                        val shadowRadius =
                            (1f - pressDepth) * 8.dp.toPx() + (pressDepth * 2.dp.toPx())
                        val shadowY =
                            (1f - pressDepth) * 6.dp.toPx() + (pressDepth * 1.dp.toPx())
                        fwPaint.setShadowLayer(
                            shadowRadius, 0f, shadowY,
                            android.graphics.Color.argb(
                                (180 + (pressDepth * 70)).toInt(), 0, 0, 0,
                            ),
                        )
                        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, paint)
                    }

                    val base0 = lerp(Color(0xFF383430), Color(0xFF2A2724), pressDepth)
                    val base1 = lerp(Color(0xFF24211E), Color(0xFF1A1816), pressDepth)

                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(base0, base1)),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(corner),
                    )

                    val ribsCount = 5
                    val ribSpacing = 5.dp.toPx()
                    val startX = w - (ribsCount * ribSpacing) - 8.dp.toPx()
                    val ribStartY = h * 0.2f
                    val ribEndY = h * 0.8f
                    for (i in 0 until ribsCount) {
                        val x = startX + (i * ribSpacing)
                        drawLine(
                            color = Color(0x22FFFFFF),
                            start = Offset(x, ribStartY),
                            end = Offset(x, ribEndY),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLine(
                            color = Color(0x66000000),
                            start = Offset(x + 1.dp.toPx(), ribStartY),
                            end = Offset(x + 1.dp.toPx(), ribEndY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    if (pressDepth < 0.99f) {
                        val lightAlpha = (1f - (pressDepth * 2f)).coerceIn(0f, 1f)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33FFFFFF).copy(alpha = lightAlpha * 0.3f),
                                    Color.Transparent,
                                    Color(0x88000000).copy(alpha = lightAlpha * 0.8f),
                                ),
                            ),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(corner),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .padding(start = 14.dp)
                .graphicsLayer {
                    val contentScale = 1f - (pressDepth * 0.035f)
                    scaleX = contentScale
                    scaleY = contentScale
                },
        ) {
            Text(
                text = "\u23CF",
                fontSize = 18.sp,
                color = Color(0xFFD6A033),
                modifier = Modifier.padding(end = 6.dp, bottom = 2.dp),
            )
            Text(
                text = "EJECT",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color(0xFFDCD5C6),
                    letterSpacing = 2.sp,
                ),
            )
        }
    }
}

// ------------------------------------------------------------------
// J-CARD SPINE HEADER
// ------------------------------------------------------------------

/**
 * Marquee-scrolling title bar styled as a cassette J-card spine.
 *
 * ### Text measurement caching
 * [rememberTextMeasurer] returns a measurer that is stable across
 * recompositions. The measured [TextLayoutResult] objects are cached
 * by Compose's snapshot system. Since [displayText] changes only when
 * [tapeState] changes (not every frame), the text is re-measured
 * only on user interaction, not during the marquee scroll animation.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun VintageTitleHeader(
    cassette: CassetteTape,
    tapeState: TapeState,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val displayText = when (tapeState) {
        TapeState.STOPPED -> "STOPPED"
        TapeState.FF -> "FAST FORWARD >>"
        TapeState.REW -> "<< REWINDING"
        TapeState.PLAYING ->
            "${cassette.artist} - ${cassette.title} (${cassette.year})        ".uppercase()
    }

    val textStyle = TextStyle(
        fontSize = 15.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        color = DarkTextColor,
        letterSpacing = 0.5.sp,
    )
    val textLayout = textMeasurer.measure(displayText, style = textStyle)
    val textWidth = textLayout.size.width.toFloat()

    val sideStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    val sideLayout = textMeasurer.measure("A", style = sideStyle)

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -textWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scrollOffset",
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(2.dp))
            .drawBehind {
                val w = size.width
                val h = size.height
                val corner = CornerRadius(2.dp.toPx())
                drawRoundRect(color = Color(0xFFF4EFE6), size = size, cornerRadius = corner)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0x1A000000), Color.Transparent,
                            Color.Transparent, Color(0x1A000000),
                        ),
                        startX = 0f,
                        endX = w,
                    ),
                    size = size,
                    cornerRadius = corner,
                )
                val stripeWidth = 8.dp.toPx()
                drawPath(
                    Path().apply {
                        addRoundRect(
                            RoundRect(
                                0f, 0f, stripeWidth, h,
                                topLeftCornerRadius = corner,
                                bottomLeftCornerRadius = corner,
                            ),
                        )
                    },
                    color = cassette.stripeColor,
                )
                val boxSize = h * 0.55f
                val boxX = stripeWidth + 8.dp.toPx()
                val boxY = (h - boxSize) / 2f
                drawRoundRect(
                    color = DarkTextColor,
                    topLeft = Offset(boxX, boxY),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                drawText(
                    textLayoutResult = sideLayout,
                    topLeft = Offset(
                        boxX + (boxSize - sideLayout.size.width) / 2f,
                        boxY + (boxSize - sideLayout.size.height) / 2f,
                    ),
                )
                val textStartX = boxX + boxSize + 12.dp.toPx()
                clipPath(
                    Path().apply {
                        addRect(
                            androidx.compose.ui.geometry.Rect(textStartX, 0f, w, h),
                        )
                    },
                ) {
                    val textY = (h - textLayout.size.height) / 2f
                    if (tapeState == TapeState.PLAYING) {
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(textStartX + scrollOffset, textY),
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                textStartX + scrollOffset + textWidth,
                                textY,
                            ),
                        )
                    } else {
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(textStartX, textY),
                        )
                    }
                }
            },
    )
}

// ------------------------------------------------------------------
// VINTAGE REELS LYRICS
// ------------------------------------------------------------------

/**
 * ## Deferred state read in drawWithCache: the critical pattern
 *
 * This composable renders scrolling lyrics where [currentProgress]
 * changes every frame during playback. The expensive work (text
 * measurement via [rememberTextMeasurer]) is done in the outer
 * [drawWithCache] block and cached until the composable's size changes.
 *
 * **The key insight:** [currentProgress] is a `() -> Float` lambda,
 * NOT a `Float`. The lambda is invoked inside `onDrawWithContent`,
 * which means:
 * - The [State] backing this lambda is read during the DRAW phase
 * - Compose marks only the draw as needing re-execution
 * - The composable does NOT recompose
 * - The cached text measurements survive indefinitely
 *
 * If [currentProgress] were a plain `Float`, every frame would:
 * 1. Recompose this composable (new Float value)
 * 2. Invalidate the [drawWithCache] cache
 * 3. Re-measure all lyrics text (expensive)
 * 4. Rebuild the [Path] objects
 * 5. Then finally draw
 *
 * With the lambda pattern, only step 5 happens each frame.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun VintageReelsLyrics(
    lyrics: List<String>,
    currentProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                val maxScale = 1.1f
                val measuredLines = lyrics.map {
                    textMeasurer.measure(
                        it,
                        style = textStyle,
                        constraints = Constraints(
                            maxWidth = (size.width / maxScale).toInt().coerceAtLeast(0),
                        ),
                    )
                }
                val lineHeight = 50.dp.toPx()
                val fadeBrush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent, Color.Black,
                        Color.Black, Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height,
                )

                onDrawWithContent {
                    drawContent()
                    val progress = currentProgress()
                    val centerY = size.height / 2f
                    measuredLines.forEachIndexed { index, textLayout ->
                        val distance = abs(index - progress)
                        val lineScale = lerp(maxScale, 0.85f, (distance / 2f).coerceIn(0f, 1f))
                        val lineAlpha = lerp(1f, 0.1f, (distance / 3f).coerceIn(0f, 1f))
                        if (lineAlpha <= 0.05f) return@forEachIndexed
                        val lineColor = lerp(
                            ActiveLyricColor,
                            VintageInkColor,
                            (distance * 1.5f).coerceIn(0f, 1f),
                        )
                        val yOffset =
                            centerY + ((index - progress) * lineHeight) -
                                    (textLayout.size.height / 2f)
                        withTransform({
                            translate(
                                left = (size.width - textLayout.size.width) / 2f,
                                top = yOffset,
                            )
                            scale(
                                lineScale, lineScale,
                                Offset(
                                    textLayout.size.width / 2f,
                                    textLayout.size.height / 2f,
                                ),
                            )
                        }) {
                            if (distance < 1.2f) {
                                drawIntoCanvas { canvas ->
                                    val p = Paint()
                                    p.asFrameworkPaint().apply {
                                        color = android.graphics.Color.TRANSPARENT
                                        setShadowLayer(
                                            16f, 0f, 0f,
                                            android.graphics.Color.argb(
                                                (lineAlpha * 140).toInt(),
                                                240, 225, 195,
                                            ),
                                        )
                                    }
                                    canvas.drawRect(
                                        0f, 0f,
                                        textLayout.size.width.toFloat(),
                                        textLayout.size.height.toFloat(),
                                        p,
                                    )
                                }
                            }
                            drawText(
                                textLayoutResult = textLayout,
                                color = lineColor.copy(alpha = lineAlpha),
                            )
                        }
                    }
                    drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                }
            },
    )
}

// ------------------------------------------------------------------
// CLASSIC HIGH-DETAIL CASSETTE TAPE RENDERER
// ------------------------------------------------------------------

/**
 * ## The most complex Canvas composable: drawWithCache in action
 *
 * This renderer draws the entire cassette tape using a single
 * [drawWithCache] modifier. The cache block does heavy setup work:
 * - Creates 8+ [Path] objects (shell, label, window, mechanism, screws)
 * - Measures text for easter eggs and side markings
 * - Pre-computes layout coordinates
 *
 * All of this survives across frames because reel rotation is consumed
 * via `() -> Float` lambdas, read only inside `onDrawBehind`. The
 * composable never recomposes during playback animation.
 *
 * ### Why not just use drawBehind?
 * [drawBehind] re-executes its ENTIRE lambda (including Path creation
 * and text measurement) on every draw call. [drawWithCache] splits
 * this into "setup once" (outer block) and "draw every frame"
 * (onDrawBehind). For a renderer this complex, that is the difference
 * between smooth 60fps and visible jank.
 *
 * @param rightReelRotation Lambda providing the right reel angle in degrees.
 *   Read only during the draw phase to avoid recomposition.
 * @param leftReelRotation Lambda providing the left reel angle in degrees.
 *   Read only during the draw phase to avoid recomposition.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun CassetteTapeRenderer(
    cassette: CassetteTape,
    rightReelRotation: () -> Float,
    leftReelRotation: () -> Float,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val shellCornerRadius = CornerRadius(h * 0.06f)

            val tapeBodyColor = cassette.bodyColor
            val labelBgColor = cassette.labelColor
            val stripeColor = cassette.stripeColor

            val leftHubCenter = Offset(w * 0.32f, h * 0.46f)
            val rightHubCenter = Offset(w * 0.68f, h * 0.46f)
            val tapeFullRadius = h * 0.23f
            val tapeEmptyRadius = h * 0.12f
            val hubRadius = h * 0.09f
            val spindleHoleRadius = h * 0.035f

            val labelLeft = w * 0.08f
            val labelTop = h * 0.13f
            val labelWidth = w * 0.84f
            val labelHeight = h * 0.56f
            val labelPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = labelLeft,
                        top = labelTop,
                        right = labelLeft + labelWidth,
                        bottom = labelTop + labelHeight,
                        cornerRadius = CornerRadius(h * 0.02f),
                    ),
                )
            }

            val windowWidth = w * 0.56f
            val windowHeight = h * 0.28f
            val windowLeft = w * 0.22f
            val windowTop = h * 0.32f
            val windowPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = windowLeft,
                        top = windowTop,
                        right = windowLeft + windowWidth,
                        bottom = windowTop + windowHeight,
                        cornerRadius = CornerRadius(h * 0.03f),
                    ),
                )
            }

            val trapBaseLeft = w * 0.15f
            val trapTopLeft = w * 0.24f
            val trapWidth = w * 0.52f
            val trapHeight = h * 0.20f
            val trapBottom = h
            val trapTop = trapBottom - trapHeight
            val mechanismPath = Path().apply {
                moveTo(trapBaseLeft, trapBottom)
                lineTo(trapTopLeft, trapTop)
                lineTo(trapTopLeft + trapWidth, trapTop)
                lineTo(trapBaseLeft + trapWidth + (trapTopLeft - trapBaseLeft), trapBottom)
                close()
            }

            val screwSize = h * 0.035f
            val screwXOffset = screwSize * 0.3f
            val screwPath = Path().apply {
                moveTo(-screwXOffset, -screwXOffset)
                lineTo(screwXOffset, screwXOffset)
                moveTo(screwXOffset, -screwXOffset)
                lineTo(-screwXOffset, screwXOffset)
            }

            val aBoxWidth = h * 0.08f
            val aBoxLeft = labelLeft + w * 0.03f
            val aBoxTop = labelTop + labelHeight - aBoxWidth - h * 0.02f
            val sideAPath = Path().apply {
                moveTo(aBoxLeft + aBoxWidth * 0.25f, aBoxTop + aBoxWidth * 0.75f)
                lineTo(aBoxLeft + aBoxWidth * 0.50f, aBoxTop + aBoxWidth * 0.20f)
                lineTo(aBoxLeft + aBoxWidth * 0.75f, aBoxTop + aBoxWidth * 0.75f)
                moveTo(aBoxLeft + aBoxWidth * 0.33f, aBoxTop + aBoxWidth * 0.60f)
                lineTo(aBoxLeft + aBoxWidth * 0.67f, aBoxTop + aBoxWidth * 0.60f)
            }

            // Pre-measure easter egg text in cache block (not per frame)
            val easterEggLayout = cassette.easterEgg?.let { egg ->
                textMeasurer.measure(
                    text = egg,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color(0x99221F1C),
                        letterSpacing = 1.sp,
                    ),
                )
            }

            val stripeTop = labelTop + h * 0.04f
            val stripeHeight = h * 0.09f

            // Pre-allocate reflection path (static geometry, no per-frame allocation)
            val reflectionPath = Path().apply {
                moveTo(windowLeft, windowTop)
                lineTo(windowLeft + windowWidth * 0.8f, windowTop)
                lineTo(windowLeft + windowWidth * 0.3f, windowTop + windowHeight)
                lineTo(windowLeft, windowTop + windowHeight)
                close()
            }

            // Pre-compute screw positions
            val screwMarginX = w * 0.035f
            val screwMarginY = h * 0.055f
            val screwPositions = floatArrayOf(
                screwMarginX, screwMarginY,
                w - screwMarginX, screwMarginY,
                screwMarginX, h - screwMarginY,
                w - screwMarginX, h - screwMarginY,
                w * 0.5f, screwMarginY,
            )

            onDrawBehind {
                // State reads deferred to draw phase
                val rotRight = rightReelRotation()
                val rotLeft = leftReelRotation()

                // 1. PLASTIC SHELL
                drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(
                            tapeBodyColor.copy(alpha = 0.9f),
                            tapeBodyColor.darken(0.3f),
                        ),
                        center = Offset(w * 0.5f, h * 0.3f),
                        radius = w * 0.8f,
                    ),
                    size = size,
                    cornerRadius = shellCornerRadius,
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33FFFFFF), Color.Transparent, Color(0x66000000),
                        ),
                        startY = 0f,
                        endY = h,
                    ),
                    size = size,
                    cornerRadius = shellCornerRadius,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0x44000000),
                    topLeft = Offset(labelLeft, labelTop + h * 0.005f),
                    size = Size(labelWidth, labelHeight),
                    cornerRadius = CornerRadius(h * 0.02f),
                )

                // 2. PAPER LABEL
                drawPath(
                    path = labelPath,
                    brush = Brush.radialGradient(
                        colors = listOf(labelBgColor, labelBgColor.darken(0.15f)),
                        center = Offset(w * 0.5f, h * 0.4f),
                        radius = w * 0.5f,
                    ),
                )

                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            stripeColor.darken(0.1f),
                            stripeColor,
                            stripeColor.darken(0.1f),
                        ),
                    ),
                    topLeft = Offset(labelLeft, stripeTop),
                    size = Size(labelWidth, stripeHeight),
                )

                easterEggLayout?.let { layout ->
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            labelLeft + labelWidth - layout.size.width - (w * 0.04f),
                            stripeTop + (stripeHeight - layout.size.height) / 2f,
                        ),
                    )
                }

                for (i in 0 until 3) {
                    val lineY = labelTop + h * 0.17f + (i * h * 0.06f)
                    drawLine(
                        color = Color(0x1A000000),
                        start = Offset(w * 0.12f, lineY),
                        end = Offset(w * 0.88f, lineY),
                        strokeWidth = h * 0.003f,
                    )
                }

                drawRoundRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(aBoxLeft, aBoxTop),
                    size = Size(aBoxWidth, aBoxWidth),
                    cornerRadius = CornerRadius(h * 0.01f),
                    style = Stroke(width = h * 0.005f),
                )
                drawPath(
                    path = sideAPath,
                    color = Color(0xFF1A1A1A),
                    style = Stroke(width = h * 0.008f, cap = StrokeCap.Round),
                )

                val text90Left = labelLeft + labelWidth - w * 0.1f
                val text90Top = aBoxTop + h * 0.02f
                val stroke90 = h * 0.008f
                drawOval(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(text90Left, text90Top),
                    size = Size(w * 0.025f, w * 0.025f),
                    style = Stroke(width = stroke90),
                )
                drawLine(
                    color = Color(0xFF1A1A1A),
                    start = Offset(text90Left + w * 0.025f, text90Top + w * 0.012f),
                    end = Offset(text90Left + w * 0.025f, text90Top + h * 0.06f),
                    strokeWidth = stroke90,
                    cap = StrokeCap.Round,
                )
                drawOval(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(text90Left + w * 0.035f, text90Top),
                    size = Size(w * 0.025f, h * 0.06f),
                    style = Stroke(width = stroke90),
                )

                // 3. RECESSED WINDOW AND TAPE SPOOLS
                clipPath(windowPath) {
                    drawRect(
                        color = WindowBgColor,
                        topLeft = Offset(windowLeft, windowTop),
                        size = Size(windowWidth, windowHeight),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x99000000), Color.Transparent),
                            startY = windowTop,
                            endY = windowTop + h * 0.1f,
                        ),
                        topLeft = Offset(windowLeft, windowTop),
                        size = Size(windowWidth, windowHeight),
                    )
                    drawLine(
                        color = TapeDarkColor,
                        start = Offset(leftHubCenter.x - tapeFullRadius, leftHubCenter.y),
                        end = Offset(leftHubCenter.x - tapeFullRadius, windowTop + windowHeight),
                        strokeWidth = h * 0.015f,
                    )
                    drawLine(
                        color = TapeDarkColor,
                        start = Offset(rightHubCenter.x + tapeEmptyRadius, rightHubCenter.y),
                        end = Offset(
                            rightHubCenter.x + tapeEmptyRadius,
                            windowTop + windowHeight,
                        ),
                        strokeWidth = h * 0.015f,
                    )

                    drawTapeSpool(leftHubCenter, tapeFullRadius, h)
                    drawTapeSpool(rightHubCenter, tapeEmptyRadius, h)

                    drawHub(leftHubCenter, rotLeft, hubRadius, spindleHoleRadius, h)
                    drawHub(rightHubCenter, rotRight, hubRadius, spindleHoleRadius, h)

                    val gaugeWidth = w * 0.18f
                    val gaugeHeight = h * 0.02f
                    val gaugeLeft = (w - gaugeWidth) / 2f
                    val gaugeTop = windowTop + windowHeight * 0.45f
                    drawRoundRect(
                        color = Color(0x1AFFFFFF),
                        topLeft = Offset(gaugeLeft, gaugeTop),
                        size = Size(gaugeWidth, gaugeHeight),
                        cornerRadius = CornerRadius(h * 0.01f),
                    )
                    for (i in 0..4) {
                        val tickX = gaugeLeft + (i * (gaugeWidth / 4f))
                        drawLine(
                            color = Color(0x66FFFFFF),
                            start = Offset(tickX, gaugeTop + h * 0.005f),
                            end = Offset(tickX, gaugeTop + gaugeHeight - h * 0.005f),
                            strokeWidth = h * 0.003f,
                        )
                    }
                }

                // 4. WINDOW GLASS REFLECTION
                drawPath(
                    path = windowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000), Color.Transparent, Color(0x44FFFFFF),
                        ),
                        startY = windowTop,
                        endY = windowTop + windowHeight,
                    ),
                    style = Stroke(width = h * 0.008f),
                )
                clipPath(windowPath) {
                    drawPath(
                        path = reflectionPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x22FFFFFF), Color.Transparent),
                            start = Offset(windowLeft, windowTop),
                            end = Offset(
                                windowLeft + windowWidth * 0.5f,
                                windowTop + windowHeight,
                            ),
                        ),
                    )
                }

                // 5. TAPE EXPOSED AREA
                drawPath(
                    path = mechanismPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF222222), Color(0xFF111111)),
                    ),
                )
                drawPath(
                    path = mechanismPath,
                    color = Color(0x22FFFFFF),
                    style = Stroke(width = h * 0.004f),
                )
                val exposedTapeY = trapTop + h * 0.05f
                drawLine(
                    color = TapeDarkColor,
                    start = Offset(w * 0.25f, exposedTapeY),
                    end = Offset(w * 0.75f, exposedTapeY),
                    strokeWidth = h * 0.018f,
                )
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(w * 0.25f, exposedTapeY - h * 0.005f),
                    end = Offset(w * 0.75f, exposedTapeY - h * 0.005f),
                    strokeWidth = h * 0.002f,
                )
                val holesY = trapTop + h * 0.10f
                drawBridgeHole(w * 0.28f, holesY, h * 0.035f)
                drawBridgeHole(w * 0.72f, holesY, h * 0.035f)
                drawBridgeHole(w * 0.50f, holesY, h * 0.045f)
                drawBridgeHole(w * 0.18f, holesY, h * 0.025f)
                drawBridgeHole(w * 0.82f, holesY, h * 0.025f)

                // 6. SCREWS
                for (i in screwPositions.indices step 2) {
                    drawScrewHead(
                        screwPositions[i], screwPositions[i + 1],
                        h, screwPath,
                    )
                }
            }
        },
    )
}

/**
 * Extracted draw helpers reduce code duplication inside `onDrawBehind`.
 * Since these are extension functions on [DrawScope], they have access
 * to the same drawing context without creating closures or lambdas.
 */
private fun DrawScope.drawTapeSpool(center: Offset, radius: Float, h: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(TapeBrownColor, TapeDarkColor),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    val numRings = (radius / (h * 0.012f)).toInt()
    for (i in 1..numRings) {
        drawCircle(
            color = Color(0x0AFFFFFF),
            radius = radius * (i.toFloat() / numRings),
            center = center,
            style = Stroke(width = h * 0.002f),
        )
    }
    drawCircle(
        color = Color(0x1AFFFFFF),
        radius = radius * 0.98f,
        center = center,
        style = Stroke(width = h * 0.003f),
    )
}

private fun DrawScope.drawHub(
    center: Offset,
    rotationAngle: Float,
    hubRadius: Float,
    spindleHoleRadius: Float,
    h: Float,
) {
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF2F2F2), Color(0xFFCCCCCC)),
            start = Offset(center.x - hubRadius, center.y - hubRadius),
            end = Offset(center.x + hubRadius, center.y + hubRadius),
        ),
        radius = hubRadius,
        center = center,
    )
    drawCircle(
        color = Color(0x66000000),
        radius = hubRadius,
        center = center,
        style = Stroke(width = h * 0.005f),
    )
    drawCircle(
        color = Color(0xFF050505),
        radius = spindleHoleRadius,
        center = center,
    )
    withTransform({
        translate(center.x, center.y)
        rotate(rotationAngle, Offset.Zero)
    }) {
        for (i in 0 until 6) {
            withTransform({ rotate(i * 60f, Offset.Zero) }) {
                drawLine(
                    color = Color(0xFF050505),
                    start = Offset(0f, -spindleHoleRadius),
                    end = Offset(0f, -hubRadius * 0.85f),
                    strokeWidth = h * 0.018f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(h * 0.01f, -spindleHoleRadius),
                    end = Offset(h * 0.01f, -hubRadius * 0.8f),
                    strokeWidth = h * 0.003f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawBridgeHole(x: Float, y: Float, r: Float) {
    drawCircle(color = Color(0x66000000), radius = r + 2f, center = Offset(x, y + 2f))
    drawCircle(color = Color(0xFF050505), radius = r, center = Offset(x, y))
}

private fun DrawScope.drawScrewHead(x: Float, y: Float, h: Float, screwPath: Path) {
    val screwCenter = Offset(x, y)
    drawCircle(color = Color(0xFF111111), radius = h * 0.045f, center = screwCenter)
    drawCircle(
        brush = Brush.radialGradient(
            0.7f to Color(0xFF888888),
            1.0f to Color(0xFF333333),
            center = screwCenter,
            radius = h * 0.035f,
        ),
        radius = h * 0.035f,
        center = screwCenter,
    )
    withTransform({
        translate(screwCenter.x, screwCenter.y)
        rotate(45f, Offset.Zero)
    }) {
        drawPath(
            path = screwPath,
            color = Color(0xFF1A1A1A),
            style = Stroke(width = h * 0.008f, cap = StrokeCap.Round),
        )
    }
}

// ------------------------------------------------------------------
// VOLUME TUNER COMPONENT
// ------------------------------------------------------------------

/**
 * Horizontal slider styled as a vintage tuner knob.
 *
 * ### rememberUpdatedState for callback stability
 * [rememberUpdatedState] wraps [volume] and [onVolumeChange] so the
 * [pointerInput] coroutine always sees the latest values without
 * restarting. Without this, the `pointerInput(Unit)` block would
 * capture stale references and either report wrong values or require
 * a key parameter that restarts gesture detection mid-drag.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun VintageVolumeTuner(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()
    var isDragging by remember { mutableStateOf(false) }

    val capScale by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "capScale",
    )

    val currentVolume by rememberUpdatedState(volume)
    val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        val trackWidth = size.width - 48.dp.toPx()
                        val newVolume =
                            (currentVolume + dragAmount / trackWidth).coerceIn(0f, 1f)
                        val oldTick = (currentVolume * 40).roundToInt()
                        val newTick = (newVolume * 40).roundToInt()
                        if (oldTick != newTick) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        currentOnVolumeChange(newVolume)
                        change.consume()
                    },
                )
            }
            .drawBehind {
                val w = size.width
                val h = size.height
                val trackX = 24.dp.toPx()
                val trackY = h * 0.35f
                val trackW = w - (trackX * 2)
                val trackH = 4.dp.toPx()
                val trackRadius = trackH / 2

                drawRoundRect(
                    color = Color(0xFF050505),
                    topLeft = Offset(trackX, trackY),
                    size = Size(trackW, trackH),
                    cornerRadius = CornerRadius(trackRadius),
                )
                drawRoundRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(trackX, trackY + trackH),
                    size = Size(trackW, 1.dp.toPx()),
                    cornerRadius = CornerRadius(trackRadius),
                )

                val marksY = h * 0.65f
                val marksCount = 10
                val textStyle = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MarkColor,
                )

                for (i in 0..marksCount) {
                    val markX = trackX + (i * (trackW / marksCount))
                    drawLine(
                        color = MarkColor.copy(alpha = 0.6f),
                        start = Offset(markX, marksY),
                        end = Offset(markX, marksY + 6.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                    if (i % 2 == 0 && i != 0 && i != 10) {
                        val textLayoutResult =
                            textMeasurer.measure(text = i.toString(), style = textStyle)
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                markX - (textLayoutResult.size.width / 2),
                                marksY + 8.dp.toPx(),
                            ),
                        )
                    }
                }

                val labelStyle = textStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium)
                val minText = textMeasurer.measure(text = "MIN", style = labelStyle)
                val maxText = textMeasurer.measure(text = "MAX", style = labelStyle)
                drawText(
                    textLayoutResult = minText,
                    topLeft = Offset(trackX - (minText.size.width / 2), marksY + 8.dp.toPx()),
                )
                drawText(
                    textLayoutResult = maxText,
                    topLeft = Offset(
                        trackX + trackW - (maxText.size.width / 2),
                        marksY + 8.dp.toPx(),
                    ),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val trackX = 24.dp.toPx()
                    val trackY = h * 0.35f
                    val trackW = w - (trackX * 2)

                    val capW = 16.dp.toPx()
                    val capH = 26.dp.toPx()
                    val capY = trackY - (capH / 2) + 2.dp.toPx()
                    val capCorner = 2.dp.toPx()
                    val capCenterX = trackX + (volume * trackW)

                    withTransform({
                        translate(top = capY)
                        scale(capScale, capScale, Offset(capCenterX, capH / 2))
                    }) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint()
                            val fwPaint = paint.asFrameworkPaint()
                            fwPaint.setShadowLayer(
                                (if (isDragging) 4.dp else 8.dp).toPx(),
                                0f,
                                (if (isDragging) 2.dp else 4.dp).toPx(),
                                android.graphics.Color.argb(
                                    if (isDragging) 255 else 160, 0, 0, 0,
                                ),
                            )
                            paint.color = Color(0xFFF7F1E6)
                            canvas.drawRoundRect(
                                capCenterX - capW / 2, 0f,
                                capCenterX + capW / 2, capH,
                                capCorner, capCorner, paint,
                            )
                        }

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFF7F1E6), Color(0xFFDCD5C6)),
                                start = Offset(capCenterX - capW / 2, 0f),
                                end = Offset(capCenterX - capW / 2, capH),
                            ),
                            topLeft = Offset(capCenterX - capW / 2, 0f),
                            size = Size(capW, capH),
                            cornerRadius = CornerRadius(capCorner),
                        )

                        val groovesCount = 8
                        val grooveMargin = 4.dp.toPx()
                        val groovesW = capW - (grooveMargin * 2)
                        for (i in 0 until groovesCount) {
                            val gY = (capH / groovesCount) * i
                            drawLine(
                                color = Color(0x33000000),
                                start = Offset(capCenterX - groovesW / 2, gY),
                                end = Offset(capCenterX + groovesW / 2, gY),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawLine(
                                color = Color(0x88FFFFFF),
                                start = Offset(capCenterX - groovesW / 2, gY + 1.dp.toPx()),
                                end = Offset(capCenterX + groovesW / 2, gY + 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color.Transparent,
                                    Color(0xFFB5AFA1).copy(alpha = 0.8f),
                                ),
                                start = Offset(capCenterX - capW / 2, 0f),
                                end = Offset(capCenterX + capW / 2, capH),
                            ),
                            topLeft = Offset(capCenterX - capW / 2, 0f),
                            size = Size(capW, capH),
                            cornerRadius = CornerRadius(capCorner),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )

                        drawLine(
                            color = Color(0xFFC75549),
                            start = Offset(capCenterX, 0f),
                            end = Offset(capCenterX, capH),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
        )
    }
}

// ------------------------------------------------------------------
// PIANO KEYS COMPONENT WITH REALISTIC DEPTH PHYSICS
// ------------------------------------------------------------------

/**
 * Skeuomorphic button with spring-animated press depth and dimple lighting.
 *
 * ### Composition vs draw phase state reads
 * [pressDepth] is an [animateFloatAsState] value. Even though it
 * animates continuously during press/release, it is only read inside
 * the [drawBehind] lambda (draw phase), never in the composition body.
 *
 * The Kotlin `by` delegate compiles to a getter that calls `state.value`.
 * When this getter is invoked inside a draw lambda, Compose's snapshot
 * system registers the read in the draw observation scope, not the
 * composition scope. This means the button body never recomposes
 * during the press animation. Only the canvas redraws.
 */
@Composable
fun AnimatedSkeuoButton(
    icon: RetroIcon,
    isToggled: Boolean = false,
    isRed: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var isManuallyPressed by remember { mutableStateOf(false) }
    val visualPress = isManuallyPressed || isToggled
    val view = LocalView.current

    val pressDepth by animateFloatAsState(
        targetValue = if (visualPress) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "pressDepth",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .drawBehind {
                    val s = size.height * 0.35f
                    translate(left = size.width / 2, top = size.height / 2) {
                        drawWalkmanIcon(icon, MarkColor, s)
                    }
                },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isManuallyPressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            tryAwaitRelease()
                            isManuallyPressed = false
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE)
                            onClick()
                        },
                    )
                }
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val corner = 6.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF070707),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(corner),
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black, Color(0x88000000), Color(0x22FFFFFF),
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(width = 1.dp.toPx()),
                    )

                    val baseUnpressed0 =
                        if (isRed) Color(0xFFE86658) else Color(0xFFF7F1E6)
                    val baseUnpressed1 =
                        if (isRed) Color(0xFFC75549) else Color(0xFFE3DCCF)
                    val basePressed0 =
                        if (isRed) Color(0xFFC75549) else Color(0xFFDCD5C6)
                    val basePressed1 =
                        if (isRed) Color(0xFF9E3A30) else Color(0xFFC4BCAB)

                    val base0 = lerp(baseUnpressed0, basePressed0, pressDepth)
                    val base1 = lerp(baseUnpressed1, basePressed1, pressDepth)

                    val currentScale = 1f - (pressDepth * 0.035f)

                    withTransform({
                        scale(currentScale, currentScale, Offset(w / 2, h / 2))
                    }) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply { color = base0 }
                            val fwPaint = paint.asFrameworkPaint()
                            val shadowRadius =
                                (1f - pressDepth) * 12.dp.toPx() +
                                        (pressDepth * 1.5f.dp.toPx())
                            val shadowYOffset =
                                (1f - pressDepth) * 8.dp.toPx() +
                                        (pressDepth * 0.5f.dp.toPx())
                            val shadowAlpha =
                                ((1f - pressDepth) * 130 + (pressDepth * 240)).toInt()
                            fwPaint.setShadowLayer(
                                shadowRadius, 0f, shadowYOffset,
                                android.graphics.Color.argb(shadowAlpha, 0, 0, 0),
                            )
                            canvas.drawRoundRect(0f, 0f, w, h, corner, corner, paint)
                        }

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(base0, base1),
                                start = Offset.Zero,
                                end = Offset(0f, h),
                            ),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(corner),
                        )

                        val dimpleRadius = w * 0.35f
                        val dimpleDark = lerp(
                            if (isRed) Color(0xFF9E382E) else Color(0xFFCAC2AE),
                            if (isRed) Color(0xFF7A2720) else Color(0xFFA8A08E),
                            pressDepth,
                        )
                        val dimpleLight = lerp(
                            if (isRed) Color(0xFFF0796C) else Color(0xFFFFFFFF),
                            if (isRed) Color(0xFFD95D50) else Color(0xFFDED6C4),
                            pressDepth,
                        )

                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(dimpleDark, dimpleLight),
                                start = Offset(w / 2 - dimpleRadius, h / 2 - dimpleRadius),
                                end = Offset(w / 2 + dimpleRadius, h / 2 + dimpleRadius),
                            ),
                            radius = dimpleRadius,
                            center = Offset(w / 2, h / 2),
                        )

                        drawCircle(
                            color = Color(0x15000000),
                            radius = dimpleRadius,
                            center = Offset(w / 2, h / 2),
                            style = Stroke(width = 1.dp.toPx()),
                        )

                        if (pressDepth < 0.99f) {
                            val lightAlpha = (1f - (pressDepth * 2f)).coerceIn(0f, 1f)
                            val lightBevel =
                                if (isRed) Color(0xFFF2897E).copy(alpha = lightAlpha)
                                else Color.White.copy(alpha = lightAlpha)
                            val darkBevel =
                                if (isRed) Color(0xFFA63B30).copy(alpha = lightAlpha)
                                else Color(0xFFB5AFA1).copy(alpha = lightAlpha)

                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        lightBevel, Color.Transparent, darkBevel,
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(w, h),
                                ),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(corner),
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                    }

                    if (pressDepth > 0.01f) {
                        clipPath(
                            Path().apply {
                                addRoundRect(
                                    RoundRect(0f, 0f, w, h, CornerRadius(corner)),
                                )
                            },
                        ) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF000000),
                                        Color(0x88000000),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = 14.dp.toPx(),
                                ),
                                alpha = pressDepth,
                            )
                            drawRoundRect(
                                color = Color.Black.copy(alpha = pressDepth * 0.8f),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(corner),
                                style = Stroke(width = 4.dp.toPx() * pressDepth),
                            )
                        }
                    }
                },
        )
    }
}

/**
 * Renders transport control icons as vector paths directly in [DrawScope].
 * Using raw path drawing instead of Icon composables avoids the overhead
 * of the Material icon pipeline (loading, intrinsic measurement, layout)
 * for these simple geometric shapes.
 */
fun DrawScope.drawWalkmanIcon(icon: RetroIcon, color: Color, s: Float) {
    when (icon) {
        RetroIcon.REC -> drawCircle(color = color, radius = s * 0.9f, center = Offset.Zero)
        RetroIcon.PLAY -> {
            val path = Path().apply {
                moveTo(-s * 0.7f, -s); lineTo(s * 0.9f, 0f); lineTo(-s * 0.7f, s); close()
            }
            drawPath(path, color)
        }

        RetroIcon.STOP -> drawRoundRect(
            color = color,
            topLeft = Offset(-s * 0.8f, -s * 0.8f),
            size = Size(s * 1.6f, s * 1.6f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )

        RetroIcon.REW -> {
            val path = Path().apply {
                moveTo(s * 0.9f, -s); lineTo(-s * 0.1f, 0f); lineTo(s * 0.9f, s); close()
                moveTo(-s * 0.1f, -s); lineTo(-s * 1.1f, 0f); lineTo(-s * 0.1f, s); close()
            }
            drawPath(path, color)
        }

        RetroIcon.FF -> {
            val path = Path().apply {
                moveTo(-s * 0.9f, -s); lineTo(s * 0.1f, 0f); lineTo(-s * 0.9f, s); close()
                moveTo(s * 0.1f, -s); lineTo(s * 1.1f, 0f); lineTo(s * 0.1f, s); close()
            }
            drawPath(path, color)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CassetteTapeDemoPreview() {
    CassetteTapeDemo()
}