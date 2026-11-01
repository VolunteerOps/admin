@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalAnimationApi::class)

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// DATA MODEL & MOCK DATA
// ============================================================================

/** A vinyl record with its sleeve and label appearance. [discColor] controls the vinyl variant (clear, gold, colored, or classic black). */
@Immutable
data class VinylAlbum(
    val id: Int,
    val title: String,
    val artist: String,
    val sleeveColor: Color,
    val labelColorTop: Color,
    val labelColorBottom: Color,
    val discColor: Color = Color(0xFF111111),
    val tracks: List<String>
)

val libraryData = listOf(
    VinylAlbum(
        id = 1,
        title = "Random Access Memories",
        artist = "Daft Punk",
        sleeveColor = Color(0xFF1E1E1E),
        labelColorTop = Color(0xFFC0C0C0),
        labelColorBottom = Color(0xFFDAA520),
        discColor = Color(0xFF111111),
        tracks = listOf(
            "Give Life Back to Music",
            "The Game of Love",
            "Giorgio by Moroder",
            "Within",
            "Instant Crush"
        )
    ),
    VinylAlbum(
        id = 2,
        title = "The Dark Side of the Moon",
        artist = "Pink Floyd",
        sleeveColor = Color(0xFF0F0F0F),
        labelColorTop = Color(0xFF000000),
        labelColorBottom = Color(0xFF333333),
        discColor = Color(0xFF111111),
        tracks = listOf(
            "Speak to Me",
            "Breathe (In the Air)",
            "On the Run",
            "Time",
            "The Great Gig in the Sky"
        )
    ),
    VinylAlbum(
        id = 3,
        title = "Kind of Blue",
        artist = "Miles Davis",
        sleeveColor = Color(0xFF2B3A42),
        labelColorTop = Color(0xFF0000CD),
        labelColorBottom = Color(0xFF00008B),
        discColor = Color(0xFF111111),
        tracks = listOf("So What", "Freddie Freeloader", "Blue in Green", "All Blues", "Flamenco Sketches")
    ),
    VinylAlbum(
        id = 4,
        title = "Abbey Road",
        artist = "The Beatles",
        sleeveColor = Color(0xFFE5E2D9),
        labelColorTop = Color(0xFF228B22),
        labelColorBottom = Color(0xFF006400),
        discColor = Color(0xFF111111),
        tracks = listOf(
            "Come Together",
            "Something",
            "Maxwell's Silver Hammer",
            "Oh! Darling",
            "Octopus's Garden"
        )
    ),
    VinylAlbum(
        id = 5,
        title = "Rumours",
        artist = "Fleetwood Mac",
        sleeveColor = Color(0xFFD6CFC4),
        labelColorTop = Color(0xFFE8E5D5),
        labelColorBottom = Color(0xFFC2C5B5),
        discColor = Color(0x33EEEEEE),
        tracks = listOf(
            "Second Hand News",
            "Dreams",
            "Never Going Back Again",
            "Don't Stop",
            "Go Your Own Way"
        )
    ),
    VinylAlbum(
        id = 6,
        title = "Currents",
        artist = "Tame Impala",
        sleeveColor = Color(0xFF2A1B38),
        labelColorTop = Color(0xFF2A2A2A),
        labelColorBottom = Color(0xFF0A0A0A),
        discColor = Color(0xFFFF5500),
        tracks = listOf("Let It Happen", "Nangs", "The Moment", "Yes I'm Changing", "Eventually")
    ),
    VinylAlbum(
        id = 7,
        title = "Thriller",
        artist = "Michael Jackson",
        sleeveColor = Color(0xFFEAEAEA),
        labelColorTop = Color(0xFF00B4DB),
        labelColorBottom = Color(0xFF0083B0),
        discColor = Color(0xFF111111),
        tracks = listOf(
            "Wanna Be Startin' Somethin'",
            "Baby Be Mine",
            "The Girl Is Mine",
            "Thriller",
            "Beat It"
        )
    ),
    VinylAlbum(
        id = 8,
        title = "Purple Rain",
        artist = "Prince",
        sleeveColor = Color(0xFF3B205E),
        labelColorTop = Color(0xFFFDE8CD),
        labelColorBottom = Color(0xFFD4AF37),
        discColor = Color(0xFF6A0DAD),
        tracks = listOf(
            "Let's Go Crazy",
            "Take Me With U",
            "The Beautiful Ones",
            "Computer Blue",
            "Darling Nikki"
        )
    ),
    VinylAlbum(
        id = 9,
        title = "Back to Black",
        artist = "Amy Winehouse",
        sleeveColor = Color(0xFF111111),
        labelColorTop = Color(0xFFFF416C),
        labelColorBottom = Color(0xFFFF4B2B),
        discColor = Color(0xFF111111),
        tracks = listOf("Rehab", "You Know I'm No Good", "Me & Mr Jones", "Just Friends", "Back to Black")
    ),
    VinylAlbum(
        id = 10,
        title = "A Love Supreme",
        artist = "John Coltrane",
        sleeveColor = Color(0xFFF0EAD6),
        labelColorTop = Color(0xFFFF6A00),
        labelColorBottom = Color(0xFFD32F2F),
        discColor = Color(0xFFD4AF37),
        tracks = listOf(
            "Part I - Acknowledgement",
            "Part II - Resolution",
            "Part III - Pursuance",
            "Part IV - Psalm"
        )
    )
)

/** Turntable physics and geometry constants. Tonearm angles are in degrees: rest → start (over first groove) → end (inner groove). */
object TurntableDesign {
    const val RPM_33 = 33.333f
    const val TonearmRestAngle = -16f
    const val TonearmStartAngle = -6f
    const val TonearmEndAngle = 18f
    const val TrackDurationMs = 180_000
}

private val ActiveTrackTextStyle = TextStyle(
    shadow = Shadow(color = Color.White, offset = Offset(0f, 2f), blurRadius = 0f)
)
private val InactiveTrackTextStyle = TextStyle()

private val KnurlingColorStops = Array(41) { i ->
    i / 40f to if (i % 2 == 0) Color(0xFF1A1A1A) else Color(0xFF666666)
}

// Pre-allocated Paint objects — cached at top level to avoid heap allocations in per-frame draw calls.
private val TonearmRestShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(140, 0, 0, 0)
    maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
}

private val TonearmBaseShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(150, 0, 0, 0)
    maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
}

private val ControlsTowerShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(150, 0, 0, 0)
    maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
}

private val SleeveShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(120, 0, 0, 0)
    maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
}

private val PlatterShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(160, 0, 0, 0)
    maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
}

private val FaderKnobShadowPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.argb(160, 0, 0, 0)
    maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
}

// ============================================================================
// MAIN NAVIGATION
// ============================================================================

/** Root composable. Two-screen nav: album grid → turntable player with shared element transitions. */
@Preview(showBackground = true)
@Composable
fun VinylArtApp() {
    MaterialTheme {
        val navController = rememberNavController()

        SharedTransitionLayout {
            NavHost(navController = navController, startDestination = "grid") {
                composable("grid") {
                    VinylGridScreen(
                        albums = libraryData,
                        animatedVisibilityScope = this@composable,
                        onAlbumClick = { albumId -> navController.navigate("player/$albumId") }
                    )
                }
                composable(
                    "player/{albumId}",
                    arguments = listOf(navArgument("albumId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getInt("albumId") ?: 1
                    val album = libraryData.first { it.id == albumId }
                    PlayerScreen(
                        album = album,
                        animatedVisibilityScope = this@composable,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 1: THE EDITORIAL GALLERY
// ============================================================================

/** Gallery grid showing album sleeves with vinyl discs peeking out. Each item is a shared element transition source. */
@Composable
fun SharedTransitionScope.VinylGridScreen(
    albums: List<VinylAlbum>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAlbumClick: (Int) -> Unit
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    Scaffold(
        containerColor = Color(0xFFF5F5F7),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = systemBars.calculateTopPadding() + 24.dp,
                bottom = systemBars.calculateBottomPadding() + 48.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MuseumHeader()
            }

            items(albums, key = { it.id }) { album ->
                VinylGridItem(
                    album = album,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}

@Composable
fun MuseumHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = "HIGH FIDELITY",
            color = Color.Black.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "MASTER COLLECTION",
            style = TextStyle(
                color = Color(0xFF1A1A1A),
                shadow = Shadow(color = Color.White, offset = Offset(0f, 3f), blurRadius = 0f)
            ),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun SharedTransitionScope.VinylGridItem(
    album: VinylAlbum,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAlbumClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onAlbumClick(album.id) }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp)
                    .rotate(-2f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val corner = 4.dp.toPx()
                    drawIntoCanvas { canvas ->
                        val paint = SleeveShadowPaint
                        canvas.nativeCanvas.drawRoundRect(
                            4.dp.toPx(),
                            8.dp.toPx(),
                            size.width,
                            size.height + 4.dp.toPx(),
                            corner,
                            corner,
                            paint
                        )
                    }
                    val isLightSleeve = album.sleeveColor.luminance() > 0.5f
                    val sleeveBrush = Brush.linearGradient(
                        listOf(
                            album.sleeveColor,
                            album.sleeveColor.copy(alpha = 0.6f)
                        )
                    )
                    drawRoundRect(
                        brush = sleeveBrush,
                        size = size,
                        cornerRadius = CornerRadius(corner)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = if (isLightSleeve) 0.5f else 0.1f),
                        start = Offset(0f, corner),
                        end = Offset(0f, size.height - corner),
                        strokeWidth = 2.dp.toPx()
                    )

                    val ringR = size.width * 0.44f
                    drawIntoCanvas { canvas ->
                        val darkScuffPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                if (isLightSleeve) 15 else 40,
                                0,
                                0,
                                0
                            ); style = android.graphics.Paint.Style.STROKE; strokeWidth =
                            6.dp.toPx(); maskFilter =
                            BlurMaskFilter(6.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawCircle(
                            size.width / 2,
                            size.height / 2,
                            ringR,
                            darkScuffPaint
                        )
                        val lightScuffPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                if (isLightSleeve) 40 else 15,
                                255,
                                255,
                                255
                            ); style = android.graphics.Paint.Style.STROKE; strokeWidth =
                            3.dp.toPx(); maskFilter =
                            BlurMaskFilter(3.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawCircle(
                            size.width / 2,
                            size.height / 2,
                            ringR - 2.dp.toPx(),
                            lightScuffPaint
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterEnd)
            ) {
                val radius = size.minDimension / 2f
                val shadowCenter =
                    Offset((size.width / 2f) - 6.dp.toPx(), (size.height / 2f) + 6.dp.toPx())
                val shadowAlpha = if (album.discColor.alpha < 1f) 0.3f else 0.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color.Black.copy(alpha = shadowAlpha),
                            Color.Transparent
                        ), center = shadowCenter, radius = radius * 1.15f
                    ), radius = radius * 1.15f, center = shadowCenter
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterEnd)
                    .sharedElement(
                        rememberSharedContentState("vinyl_${album.id}"),
                        animatedVisibilityScope,
                        boundsTransform = { _, _ -> spring(dampingRatio = 0.85f, stiffness = 100f) }
                    )
            ) {
                VinylRecord(
                    modifier = Modifier.fillMaxSize(),
                    surfaceRotationDegrees = { 0f },
                    platterCenterOffset = Offset(0.5f, 0.5f),
                    album = album
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = album.title,
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            letterSpacing = 0.2.sp,
            modifier = Modifier.sharedElement(
                rememberSharedContentState("title_${album.id}"),
                animatedVisibilityScope
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.artist,
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.sharedElement(
                rememberSharedContentState("artist_${album.id}"),
                animatedVisibilityScope
            )
        )
    }
}

// ============================================================================
// SCREEN 2: THE TURNTABLE PLAYER
// ============================================================================

/**
 * Full turntable player screen. Manages playback state, tonearm animation, vinyl rotation, and fader control.
 *
 * Interactions: drag the tonearm to seek, drag the fader to pitch-bend, tap the start/stop button on the chassis.
 * The tonearm angle drives track selection via [derivedStateOf] to avoid recomposing the tracklist every frame.
 */
@Composable
fun SharedTransitionScope.PlayerScreen(
    album: VinylAlbum,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var isPlaying by remember { mutableStateOf(false) }
    var isDraggingTonearm by remember { mutableStateOf(false) }
    var isDraggingFader by remember { mutableStateOf(false) }
    var faderPosition by remember { mutableFloatStateOf(0.5f) }
    var isStartButtonPressed by remember { mutableStateOf(false) }

    val startButtonTravel by animateFloatAsState(
        targetValue = if (isStartButtonPressed) 1f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "buttonTravel"
    )
    val tonearmAngle = remember { Animatable(TurntableDesign.TonearmRestAngle) }
    val motorRpm = remember { Animatable(0f) }
    var vinylRotation by remember { mutableFloatStateOf(0f) }

    val activeTrackIndex by remember {
        derivedStateOf {
            val angle = tonearmAngle.value
            if (angle < TurntableDesign.TonearmStartAngle) -1
            else {
                val progress = ((angle - TurntableDesign.TonearmStartAngle) /
                        (TurntableDesign.TonearmEndAngle - TurntableDesign.TonearmStartAngle)).coerceIn(
                    0f,
                    0.999f
                )
                ((progress * album.tracks.size).toInt()).coerceAtMost(album.tracks.size - 1)
            }
        }
    }

    val flipRotation by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.85f, stiffness = 100f) },
        label = "flipRotation"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> -360f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 360f
        }
    }

    LaunchedEffect(Unit) {
        delay(600)
        tonearmAngle.animateTo(
            TurntableDesign.TonearmRestAngle - 4f,
            tween(300, easing = LinearOutSlowInEasing)
        )
        tonearmAngle.animateTo(
            TurntableDesign.TonearmStartAngle,
            tween(800, easing = FastOutSlowInEasing)
        )
        coroutineScope.launch {
            tonearmAngle.animateTo(
                TurntableDesign.TonearmStartAngle,
                tween(300, easing = LinearOutSlowInEasing)
            )
        }
        isPlaying = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && tonearmAngle.value >= TurntableDesign.TonearmStartAngle) {
            val progress =
                (tonearmAngle.value - TurntableDesign.TonearmStartAngle) / (TurntableDesign.TonearmEndAngle - TurntableDesign.TonearmStartAngle)
            val timeRemaining =
                (TurntableDesign.TrackDurationMs * (1f - progress.coerceIn(0f, 1f))).toLong()

            tonearmAngle.animateTo(
                targetValue = TurntableDesign.TonearmEndAngle,
                animationSpec = tween(durationMillis = timeRemaining.toInt(), easing = LinearEasing)
            )

            if (tonearmAngle.value >= TurntableDesign.TonearmEndAngle - 0.1f) {
                isPlaying = false
                tonearmAngle.animateTo(
                    TurntableDesign.TonearmRestAngle,
                    tween(1000, easing = FastOutSlowInEasing)
                )
            }
        } else if (!isPlaying) {
            tonearmAngle.stop()
        }
    }

    val skipTrack = { forward: Boolean ->
        if (tonearmAngle.value >= TurntableDesign.TonearmStartAngle) {
            coroutineScope.launch {
                val wasPlaying = isPlaying
                isPlaying = false

                val degreePerTrack =
                    (TurntableDesign.TonearmEndAngle - TurntableDesign.TonearmStartAngle) / album.tracks.size
                val delta = if (forward) degreePerTrack else -degreePerTrack
                val targetAngle = (tonearmAngle.value + delta).coerceIn(
                    TurntableDesign.TonearmStartAngle,
                    TurntableDesign.TonearmEndAngle
                )

                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                tonearmAngle.animateTo(targetAngle, spring(dampingRatio = 0.8f, stiffness = 200f))

                if (wasPlaying && targetAngle < TurntableDesign.TonearmEndAngle) {
                    isPlaying = true
                } else if (targetAngle >= TurntableDesign.TonearmEndAngle) {
                    tonearmAngle.animateTo(
                        TurntableDesign.TonearmRestAngle,
                        tween(1000, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        motorRpm.animateTo(
            if (isPlaying) TurntableDesign.RPM_33 else 0f,
            tween(if (isPlaying) 800 else 1200, easing = LinearOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
            lastFrameTime = frameTime
            if (motorRpm.value > 0.05f) {
                val pitchMultiplier = 1f + ((faderPosition - 0.5f) * 0.16f)
                val degreesPerSec = (motorRpm.value * pitchMultiplier) * 6f
                vinylRotation = (vinylRotation + (degreesPerSec * (deltaMs / 1000f))) % 360f
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F7), // Main gallery backdrop
        topBar = {
            PlayerHeader(
                album = album,
                animatedVisibilityScope = animatedVisibilityScope,
                onBack = onBack
            )
        },
        bottomBar = {
            MinimalControlPanel(
                isPlaying = isPlaying,
                onPlayPause = { isPlaying = !isPlaying },
                onSkipForward = { skipTrack(true) },
                onSkipBackward = { skipTrack(false) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp)
                    .clipToBounds(), // Prevents scrolling text from bleeding onto the turntable
                contentAlignment = Alignment.TopStart
            ) {
                EditorialTracklist(album = album, activeTrackIndex = activeTrackIndex)
            }

            TurntableChassis(
                modifier = Modifier
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                    .aspectRatio(0.85f)
                    .fillMaxWidth()
                    .shadow(
                        36.dp,
                        RoundedCornerShape(8.dp),
                        spotColor = Color(0x99000000),
                        ambientColor = Color(0x66000000)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawChassisDetails(size, faderPosition)
                        val center = Offset(size.width * 0.46f, size.height * 0.46f)
                        val platterRadius = size.minDimension * 0.44f
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawCircle(
                                center.x + 6.dp.toPx(),
                                center.y + 10.dp.toPx(),
                                platterRadius + 1.dp.toPx(),
                                PlatterShadowPaint
                            )
                        }
                        drawStrobePlatterBand(size, center, platterRadius)
                    }

                    VinylRecord(
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "vinyl_${album.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = 0.85f,
                                        stiffness = 100f
                                    )
                                }
                            )
                            .graphicsLayer {
                                rotationY = flipRotation
                                cameraDistance = 12f * density
                            },
                        surfaceRotationDegrees = { vinylRotation },
                        platterCenterOffset = Offset(0.46f, 0.46f),
                        album = album
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val btnWidth = size.width * 0.13f
                                        val btnHeight = size.height * 0.075f
                                        val btnTopLeft =
                                            Offset(size.width * 0.07f, size.height * 0.90f)
                                        if (offset.x in btnTopLeft.x..(btnTopLeft.x + btnWidth) && offset.y in btnTopLeft.y..(btnTopLeft.y + btnHeight)) {
                                            isStartButtonPressed = true
                                            if (tryAwaitRelease()) {
                                                isStartButtonPressed = false
                                                if (isPlaying) {
                                                    isPlaying = false
                                                } else {
                                                    isPlaying = true
                                                    if (tonearmAngle.value < TurntableDesign.TonearmStartAngle) coroutineScope.launch {
                                                        tonearmAngle.animateTo(
                                                            TurntableDesign.TonearmStartAngle,
                                                            tween(1000)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (offset.x > size.width * 0.75f && offset.y > size.height * 0.55f) {
                                            isDraggingFader = true
                                        } else if (offset.x > size.width * 0.45f && offset.y < size.height * 0.85f) {
                                            isDraggingTonearm = true; isPlaying =
                                                false; coroutineScope.launch { tonearmAngle.stop() }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (isDraggingFader) {
                                            val newPos =
                                                (faderPosition + (dragAmount.y / (size.height * 0.28f))).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            if (abs(newPos - 0.5f) < 0.04f) {
                                                if (faderPosition != 0.5f) hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                ); faderPosition = 0.5f
                                            } else faderPosition = newPos
                                        } else if (isDraggingTonearm) {
                                            val pivot =
                                                Offset(size.width * 0.84f, size.height * 0.22f)
                                            val prevAngle = atan2(
                                                change.previousPosition.y - pivot.y,
                                                change.previousPosition.x - pivot.x
                                            )
                                            val newAngle = atan2(
                                                change.position.y - pivot.y,
                                                change.position.x - pivot.x
                                            )
                                            var deltaDeg =
                                                Math.toDegrees((newAngle - prevAngle).toDouble())
                                                    .toFloat()
                                            if (deltaDeg > 180) deltaDeg -= 360
                                            if (deltaDeg < -180) deltaDeg += 360
                                            coroutineScope.launch {
                                                tonearmAngle.snapTo(
                                                    (tonearmAngle.value + deltaDeg).coerceIn(
                                                        TurntableDesign.TonearmRestAngle - 4f,
                                                        TurntableDesign.TonearmEndAngle + 2f
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        isDraggingFader = false
                                        if (isDraggingTonearm) {
                                            isDraggingTonearm =
                                                false; if (tonearmAngle.value >= TurntableDesign.TonearmStartAngle) isPlaying =
                                                true else coroutineScope.launch {
                                                tonearmAngle.animateTo(
                                                    TurntableDesign.TonearmRestAngle,
                                                    tween(500)
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingFader = false; isDraggingTonearm = false
                                    }
                                )
                            }
                    ) {
                        val center = Offset(size.width * 0.46f, size.height * 0.46f)
                        drawTonearmRest(size)
                        drawAnimatedTonearm(size, tonearmAngle.value, center)
                        drawSkeuomorphicControls(size, center, isPlaying, startButtonTravel)
                    }
                }
            }
        }
    }
}

// ============================================================================
// NEW PLAYER UI COMPONENTS
// ============================================================================

/** Scrollable track listing that auto-scrolls to and highlights the active track. Each [TrackItem] is extracted for skip optimization. */
@Composable
fun EditorialTracklist(album: VinylAlbum, activeTrackIndex: Int) {
    val scrollState = rememberScrollState()

    LaunchedEffect(activeTrackIndex) {
        if (activeTrackIndex >= 0) {
            scrollState.animateScrollTo(activeTrackIndex * 100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "SIDE A",
            color = Color.Black.copy(alpha = 0.3f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        album.tracks.forEachIndexed { index, track ->
            TrackItem(
                index = index,
                track = track,
                isActive = index == activeTrackIndex
            )
        }
    }
}

@Composable
private fun TrackItem(index: Int, track: String, isActive: Boolean) {
    val animatedOffsetX by animateDpAsState(
        if (isActive) 12.dp else 0.dp,
        spring(stiffness = Spring.StiffnessLow),
        label = "offset"
    )
    val textColor by animateColorAsState(
        if (isActive) Color.Black else Color(0xFFB3B3B3),
        tween(300),
        label = "textColor"
    )
    val fontSize by animateFloatAsState(
        if (isActive) 22f else 16f,
        spring(stiffness = Spring.StiffnessLow),
        label = "fontSize"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(x = animatedOffsetX)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "0${index + 1}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color(0xFFFF5500) else Color(0xFFCCCCCC)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = track,
            style = if (isActive) ActiveTrackTextStyle else InactiveTrackTextStyle,
            fontSize = fontSize.sp,
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium,
            color = textColor,
            letterSpacing = (-0.5).sp,
            maxLines = 1
        )
    }
}

@Composable
fun SharedTransitionScope.PlayerHeader(
    album: VinylAlbum,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = systemBars.calculateTopPadding() + 8.dp,
                start = 8.dp,
                end = 16.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                letterSpacing = 0.2.sp,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState("title_${album.id}"),
                    animatedVisibilityScope
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = album.artist,
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState("artist_${album.id}"),
                    animatedVisibilityScope
                )
            )
        }
    }
}

/** Circular brushed-metal button with an engraved icon. Used for playback controls. */
@Composable
fun MetallicControlButton(
    icon: ImageVector,
    size: Dp,
    iconTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color(0x99000000),
                ambientColor = Color(0x44000000)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFBDBDBD)))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Rim Highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color.White, Color.Transparent)),
                    shape = CircleShape
                )
        )

        // Inner well / Machined center
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFE0E0E0), Color(0xFFAAAAAA)))
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF999999),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Engraved Icon Drop Shadow
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(size * 0.45f)
                    .offset(y = 1.dp)
            )
            // Actual Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

/** Bottom bar with skip/play/pause buttons. Fades in from the chassis background. */
@Composable
fun MinimalControlPanel(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val fadeBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF5F5F7).copy(alpha = 0f),
                Color(0xFFE8E8EB),
                Color(0xFFD0D0D6)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = fadeBrush)
            .padding(bottom = systemBars.calculateBottomPadding() + 32.dp, top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            MetallicControlButton(
                icon = Icons.Default.FastRewind,
                size = 56.dp,
                iconTint = Color(0xFF888888),
                onClick = onSkipBackward
            )

            MetallicControlButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                size = 72.dp,
                iconTint = if (isPlaying) Color(0xFF111111) else Color(0xFF888888), // High-contrast graphite when active
                onClick = onPlayPause
            )

            MetallicControlButton(
                icon = Icons.Default.FastForward,
                size = 56.dp,
                iconTint = Color(0xFF888888),
                onClick = onSkipForward
            )
        }
    }
}

// ============================================================================
// SHARED MASTER COMPONENTS
// ============================================================================

/** Brushed-metal chassis container. Provides the silver gradient background and light/shadow overlay. */
@Composable
fun TurntableChassis(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val chassisBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEBEBEB), Color(0xFFDCDCDC), Color(0xFFC4C4C4),
                Color(0xFFA5A5A5), Color(0xFF888888)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    }
    val overlayBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0x66FFFFFF),
                Color.Transparent,
                Color.Transparent,
                Color(0x66000000)
            ),
            start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chassisBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )
        content()
    }
}

/**
 * Canvas-drawn vinyl disc with grooves, label, and spindle hole.
 *
 * Uses [drawWithCache] so all Brushes, Strokes, and geometry are computed once per size change.
 * Only the rotation (read inside [onDrawBehind]) changes per frame — no recomposition needed.
 *
 * @param surfaceRotationDegrees lambda to defer the rotation state read to the draw phase, avoiding recomposition.
 * @param platterCenterOffset normalized (0..1) position of the disc center within the composable bounds.
 */
@Composable
fun VinylRecord(
    modifier: Modifier = Modifier,
    surfaceRotationDegrees: () -> Float,
    platterCenterOffset: Offset,
    album: VinylAlbum
) {
    val isClear = album.discColor.alpha < 1f
    val isGold = album.discColor == Color(0xFFD4AF37)
    val isBlack = album.discColor == Color(0xFF111111)
    val isColoredOpaque = !isClear && !isGold && !isBlack

    Spacer(modifier = modifier.drawWithCache {
        val center = Offset(size.width * platterCenterOffset.x, size.height * platterCenterOffset.y)
        val vinylRadius = size.minDimension * 0.42f
        val innerLabelRadius = vinylRadius * 0.32f

        val baseBrush = Brush.radialGradient(
            colors = listOf(
                album.discColor,
                album.discColor.copy(alpha = album.discColor.alpha * 0.8f)
            ),
            center = center, radius = vinylRadius
        )
        val sweepColors = when {
            isClear -> listOf(
                Color.White.copy(0.7f), Color.Transparent, Color.Black.copy(0.1f),
                Color.Transparent, Color.White.copy(0.7f), Color.Transparent,
                Color.Black.copy(0.1f), Color.Transparent, Color.White.copy(0.7f)
            )

            isGold -> listOf(
                Color.White.copy(0.8f), Color(0xFF5C3A00).copy(0.6f), Color.Transparent,
                Color(0xFF5C3A00).copy(0.6f), Color.White.copy(0.8f), Color(0xFF5C3A00).copy(0.6f),
                Color.Transparent, Color(0xFF5C3A00).copy(0.6f), Color.White.copy(0.8f)
            )

            isColoredOpaque -> listOf(
                Color.White.copy(0.4f), Color.Transparent, Color.Black.copy(0.3f),
                Color.Transparent, Color.White.copy(0.4f), Color.Transparent,
                Color.Black.copy(0.3f), Color.Transparent, Color.White.copy(0.4f)
            )

            else -> listOf(
                Color.White.copy(0.35f), Color.Transparent, Color.Black.copy(0.8f),
                Color.Transparent, Color.White.copy(0.35f), Color.Transparent,
                Color.Black.copy(0.8f), Color.Transparent, Color.White.copy(0.35f)
            )
        }
        val sweepBrush = Brush.sweepGradient(sweepColors, center = center)
        val edgeRefraction = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(if (isClear || isColoredOpaque) 0.4f else 0.15f),
                Color.Black.copy(0.5f)
            ), center = center, radius = vinylRadius
        )
        val borderColor = Color.Black.copy(alpha = 0.5f)
        val borderStroke = Stroke(width = 1.dp.toPx())

        val grooveColor = if (isGold) Color(0xFF5C3A00) else Color.Black
        val grooveDarkColor =
            grooveColor.copy(alpha = if (isClear) 0.08f else if (isColoredOpaque) 0.2f else 0.35f)
        val grooveLightColor =
            Color.White.copy(alpha = if (isClear || isColoredOpaque) 0.15f else 0.08f)
        val grooveDarkStroke = Stroke(width = 0.5.dp.toPx())
        val grooveLightStroke = Stroke(width = 0.2.dp.toPx())
        val halfDpPx = 0.5.dp.toPx()

        val trackGapColor =
            grooveColor.copy(alpha = if (isClear) 0.05f else if (isColoredOpaque) 0.15f else 0.3f)
        val trackGapStroke3 = Stroke(width = 3.dp.toPx())
        val trackGapStroke4 = Stroke(width = 4.dp.toPx())
        val trackGapStroke2 = Stroke(width = 2.dp.toPx())

        val oneDpPx = 1.dp.toPx()
        val twoDpPx = 2.dp.toPx()
        val labelShadowCenter = center.copy(x = center.x + oneDpPx, y = center.y + twoDpPx)
        val labelBrush = Brush.linearGradient(listOf(album.labelColorTop, album.labelColorBottom))
        val labelArcTopLeft =
            Offset(center.x - innerLabelRadius * 0.8f, center.y - innerLabelRadius * 0.8f)
        val labelArcSize = Size(innerLabelRadius * 1.6f, innerLabelRadius * 1.6f)
        val labelRectTopLeft =
            Offset(center.x + innerLabelRadius * 0.3f, center.y - innerLabelRadius * 0.1f)
        val labelRectSize = Size(innerLabelRadius * 0.4f, innerLabelRadius * 0.2f)
        val labelRectCorner = CornerRadius(twoDpPx)

        val spindleShadowCenter = center.copy(x = center.x + 1.5.dp.toPx(), y = center.y + twoDpPx)

        onDrawBehind {
            drawCircle(brush = baseBrush, radius = vinylRadius, center = center)
            drawCircle(brush = sweepBrush, radius = vinylRadius, center = center)
            drawCircle(brush = edgeRefraction, radius = vinylRadius, center = center)
            drawCircle(
                color = borderColor,
                radius = vinylRadius,
                center = center,
                style = borderStroke
            )

            rotate(surfaceRotationDegrees(), pivot = center) {
                for (i in 0..80) {
                    val r = vinylRadius * (0.98f - (i * 0.008f))
                    if (r > innerLabelRadius) {
                        drawCircle(
                            color = grooveDarkColor,
                            radius = r,
                            center = center,
                            style = grooveDarkStroke
                        )
                        drawCircle(
                            color = grooveLightColor,
                            radius = r - halfDpPx,
                            center = center,
                            style = grooveLightStroke
                        )
                    }
                }

                drawCircle(
                    color = trackGapColor,
                    radius = vinylRadius * 0.80f,
                    center = center,
                    style = trackGapStroke3
                )
                drawCircle(
                    color = trackGapColor,
                    radius = vinylRadius * 0.65f,
                    center = center,
                    style = trackGapStroke4
                )
                drawCircle(
                    color = trackGapColor,
                    radius = vinylRadius * 0.45f,
                    center = center,
                    style = trackGapStroke2
                )

                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = innerLabelRadius,
                    center = labelShadowCenter
                )
                drawCircle(brush = labelBrush, radius = innerLabelRadius, center = center)
                drawArc(
                    color = Color(0xFFA53B3B),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = labelArcTopLeft,
                    size = labelArcSize,
                    style = trackGapStroke2
                )
                drawRoundRect(
                    color = Color(0xFF222222), topLeft = labelRectTopLeft,
                    size = labelRectSize, cornerRadius = labelRectCorner
                )
            }

            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = innerLabelRadius * 0.12f,
                center = spindleShadowCenter
            )
            drawCircle(
                color = Color(0xFF111111),
                radius = innerLabelRadius * 0.10f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = innerLabelRadius * 0.10f,
                center = center,
                style = grooveDarkStroke
            )
        }
    })
}

/** Draws the pitch fader: slot, knob with shadow, center-lock LED indicator. */
fun DrawScope.drawChassisDetails(size: Size, faderPosition: Float) {
    val faderWidth = size.width * 0.05f
    val faderTopLeft = Offset(size.width * 0.89f, size.height * 0.65f)
    val faderHeight = size.height * 0.28f

    drawRoundRect(
        color = Color(0xFF444444),
        topLeft = faderTopLeft,
        size = Size(faderWidth, faderHeight),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = faderTopLeft,
        size = Size(faderWidth, faderHeight),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    val slotWidth = 3.dp.toPx()
    drawRoundRect(
        color = Color(0xFF000000),
        topLeft = Offset(
            faderTopLeft.x + (faderWidth / 2) - (slotWidth / 2),
            faderTopLeft.y + 8.dp.toPx()
        ),
        size = Size(slotWidth, faderHeight - 16.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx())
    )
    drawLine(
        color = Color(0x88FFFFFF),
        start = Offset(faderTopLeft.x - 2.dp.toPx(), faderTopLeft.y + faderHeight / 2),
        end = Offset(faderTopLeft.x + faderWidth + 2.dp.toPx(), faderTopLeft.y + faderHeight / 2),
        strokeWidth = 1.dp.toPx()
    )

    val isLocked = abs(faderPosition - 0.5f) < 0.01f
    val ledCenter = Offset(faderTopLeft.x - 10.dp.toPx(), faderTopLeft.y + faderHeight / 2)
    drawCircle(
        color = if (isLocked) Color(0xFF00FF00) else Color(0xFF003300),
        radius = 2.dp.toPx(),
        center = ledCenter
    )
    if (isLocked) drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0x9900FF00),
                Color.Transparent
            ), center = ledCenter, radius = 8.dp.toPx()
        ), radius = 8.dp.toPx(), center = ledCenter
    )

    val knobWidth = faderWidth + 16.dp.toPx()
    val knobHeight = 24.dp.toPx()
    val knobTopLeft = Offset(
        faderTopLeft.x - 8.dp.toPx(),
        faderTopLeft.y + ((faderHeight - knobHeight) * faderPosition)
    )

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            knobTopLeft.x,
            knobTopLeft.y + 4.dp.toPx(),
            knobTopLeft.x + knobWidth,
            knobTopLeft.y + knobHeight + 6.dp.toPx(),
            2.dp.toPx(),
            2.dp.toPx(),
            FaderKnobShadowPaint
        )
    }
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFBDBDBD),
                Color(0xFF777777)
            )
        ),
        topLeft = knobTopLeft,
        size = Size(knobWidth, knobHeight),
        cornerRadius = CornerRadius(1.dp.toPx())
    )
    drawLine(
        color = Color(0xFFAA0000),
        start = Offset(faderTopLeft.x - 4.dp.toPx(), knobTopLeft.y + knobHeight / 2),
        end = Offset(faderTopLeft.x + faderWidth + 4.dp.toPx(), knobTopLeft.y + knobHeight / 2),
        strokeWidth = 2.dp.toPx()
    )
}

/** Draws the metallic platter rim with strobe dots and a red LED reflection. */
fun DrawScope.drawStrobePlatterBand(size: Size, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.sweepGradient(
            0.0f to Color(0xFFBDBDBD),
            0.25f to Color(0xFF666666),
            0.5f to Color(0xFFEEEEEE),
            0.75f to Color(0xFF555555),
            1.0f to Color(0xFFBDBDBD),
            center = center
        ), radius = radius, center = center
    )
    drawCircle(
        color = Color(0xFF151515),
        radius = radius - 4.dp.toPx(),
        center = center,
        style = Stroke(width = 6.dp.toPx())
    )
    val strobeDash = PathEffect.dashPathEffect(
        floatArrayOf(
            (2 * PI * radius / 200).toFloat() * 0.5f,
            (2 * PI * radius / 200).toFloat() * 0.5f
        ), 0f
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFAAAAAA)),
            center = center
        ),
        radius = radius - 4.dp.toPx(),
        center = center,
        style = Stroke(width = 6.dp.toPx(), pathEffect = strobeDash, cap = StrokeCap.Butt)
    )
    drawCircle(
        color = Color(0x66000000),
        radius = radius - 7.dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFFFF1111).copy(alpha = 0.8f),
            0.5f to Color(0xFFFF1111).copy(alpha = 0.0f),
            center = Offset(
                center.x + cos(135f * PI / 180f).toFloat() * radius,
                center.y + sin(135f * PI / 180f).toFloat() * radius
            ),
            radius = radius * 0.25f
        ),
        radius = radius - 2.dp.toPx(),
        center = center,
        style = Stroke(width = 8.dp.toPx()),
        blendMode = BlendMode.Screen
    )
}

/** Draws the tonearm rest cradle (the small post the arm sits on when not playing). */
fun DrawScope.drawTonearmRest(size: Size) {
    val restCenter = Offset(size.width * 0.84f, size.height * 0.45f)
    val baseRadius = size.minDimension * 0.035f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawCircle(
            restCenter.x + 4.dp.toPx(),
            restCenter.y + 4.dp.toPx(),
            baseRadius,
            TonearmRestShadowPaint
        )
    }
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(
                Color(0xFF222222),
                Color(0xFF555555),
                Color(0xFF111111),
                Color(0xFF555555),
                Color(0xFF222222)
            ), center = restCenter
        ), radius = baseRadius, center = restCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFE0E0E0), Color(0xFF666666)),
            center = restCenter
        ), radius = baseRadius * 0.6f, center = restCenter, style = Stroke(width = 3.dp.toPx())
    )

    val arcTopLeft = Offset(restCenter.x - baseRadius * 0.5f, restCenter.y - baseRadius * 0.8f)
    val arcSize = Size(baseRadius, baseRadius * 1.5f)
    drawArc(
        color = Color(0x88000000),
        startAngle = -20f,
        sweepAngle = -140f,
        useCenter = false,
        topLeft = Offset(arcTopLeft.x + 2.dp.toPx(), arcTopLeft.y + 3.dp.toPx()),
        size = arcSize,
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF111111),
        startAngle = -20f,
        sweepAngle = -140f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        brush = Brush.linearGradient(listOf(Color(0xFF777777), Color.Transparent)),
        startAngle = -20f,
        sweepAngle = -140f,
        useCenter = false,
        topLeft = Offset(arcTopLeft.x - 0.5.dp.toPx(), arcTopLeft.y - 0.5.dp.toPx()),
        size = arcSize,
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
    )
}

/** Draws the tonearm: pivot base, curved arm tube, and headshell. Rotated by [trackingAngle] around the pivot. */
fun DrawScope.drawAnimatedTonearm(size: Size, trackingAngle: Float, platterCenter: Offset) {
    val mainPivot = Offset(size.width * 0.84f, size.height * 0.22f)
    val vinylRadius = size.minDimension * 0.42f
    val baseRadius = size.minDimension * 0.09f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawCircle(
            mainPivot.x + 4.dp.toPx(),
            mainPivot.y + 6.dp.toPx(),
            baseRadius + 2.dp.toPx(),
            TonearmBaseShadowPaint
        )
    }
    drawCircle(
        brush = Brush.sweepGradient(
            0.0f to Color(0xFF222222),
            0.25f to Color(0xFF555555),
            0.5f to Color(0xFF1A1A1A),
            0.75f to Color(0xFF444444),
            1.0f to Color(0xFF222222),
            center = mainPivot
        ), radius = baseRadius, center = mainPivot
    )
    rotate(trackingAngle, pivot = mainPivot) {
        val armPath = Path().apply {
            moveTo(
                mainPivot.x,
                mainPivot.y
            ); cubicTo(
            mainPivot.x + vinylRadius * 0.35f,
            mainPivot.y + vinylRadius * 0.88f * 0.3f,
            mainPivot.x + vinylRadius * 0.05f,
            mainPivot.y + vinylRadius * 0.88f * 0.65f,
            mainPivot.x - vinylRadius * 0.15f,
            mainPivot.y + vinylRadius * 0.88f
        )
        }
        drawPath(
            path = armPath,
            color = Color(0xFFB3B3B3),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )
        rotate(
            28f,
            pivot = Offset(mainPivot.x - vinylRadius * 0.15f, mainPivot.y + vinylRadius * 0.88f)
        ) {
            val hsWidth = vinylRadius * 0.14f
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF262626),
                        Color(0xFF0A0A0A)
                    )
                ),
                topLeft = Offset(
                    (mainPivot.x - vinylRadius * 0.15f) - hsWidth / 2f,
                    (mainPivot.y + vinylRadius * 0.88f) + 8.dp.toPx()
                ),
                size = Size(hsWidth, vinylRadius * 0.28f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFE0E0E0), Color(0xFF444444))),
        radius = baseRadius * 0.45f,
        center = mainPivot
    )
}

/** Draws the start/stop tower (knurled knob with LED) and the mechanical push button. [buttonTravel] 0→1 animates the press depth. */
fun DrawScope.drawSkeuomorphicControls(
    size: Size,
    platterCenter: Offset,
    isPlaying: Boolean,
    buttonTravel: Float
) {
    val towerBaseCenter = Offset(size.width * 0.14f, size.height * 0.82f)
    val towerRadius = size.minDimension * 0.06f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawCircle(
            towerBaseCenter.x + 4.dp.toPx(),
            towerBaseCenter.y + 4.dp.toPx(),
            towerRadius,
            ControlsTowerShadowPaint
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFE0E0E0), Color(0xFF888888)),
            center = towerBaseCenter
        ), radius = towerRadius, center = towerBaseCenter
    )
    drawCircle(
        brush = Brush.sweepGradient(*KnurlingColorStops, center = towerBaseCenter),
        radius = towerRadius * 0.85f,
        center = towerBaseCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF333333), Color(0xFF111111)),
            center = towerBaseCenter
        ), radius = towerRadius * 0.75f, center = towerBaseCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFFFF2222).copy(alpha = if (isPlaying) 0.8f else 0.3f),
            1.0f to Color.Transparent,
            center = Offset(
                towerBaseCenter.x + towerRadius * 1.5f,
                towerBaseCenter.y - towerRadius * 1.5f
            ),
            radius = towerRadius * 2.5f
        ),
        radius = towerRadius * 2.5f,
        center = Offset(
            towerBaseCenter.x + towerRadius * 1.5f,
            towerBaseCenter.y - towerRadius * 1.5f
        )
    )

    rotate(-48f, pivot = towerBaseCenter) {
        val path = Path().apply {
            moveTo(
                towerBaseCenter.x - towerRadius * 0.1f,
                towerBaseCenter.y - towerRadius * 0.4f
            ); lineTo(
            towerBaseCenter.x + towerRadius * 0.8f,
            towerBaseCenter.y - towerRadius * 0.2f
        ); lineTo(
            towerBaseCenter.x + towerRadius * 0.8f,
            towerBaseCenter.y + towerRadius * 0.2f
        ); lineTo(
            towerBaseCenter.x - towerRadius * 0.1f,
            towerBaseCenter.y + towerRadius * 0.4f
        ); close()
        }
        drawPath(path = path, color = Color(0x99000000), style = Fill); drawPath(
        path = path,
        brush = Brush.linearGradient(
            listOf(Color(0xFF444444), Color(0xFF151515)),
            start = Offset(towerBaseCenter.x, towerBaseCenter.y - towerRadius * 0.4f),
            end = Offset(
                towerBaseCenter.x + towerRadius * 0.8f,
                towerBaseCenter.y + towerRadius * 0.2f
            )
        )
    )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(
                towerBaseCenter.x - towerRadius * 0.1f,
                towerBaseCenter.y - towerRadius * 0.4f
            ),
            end = Offset(
                towerBaseCenter.x + towerRadius * 0.8f,
                towerBaseCenter.y - towerRadius * 0.2f
            ),
            strokeWidth = 1.dp.toPx()
        )
        val ledX = towerBaseCenter.x + towerRadius * 0.8f;
        val ledY = towerBaseCenter.y; drawCircle(
        color = if (isPlaying) Color(0xFFFF3333) else Color(
            0xFFAA0000
        ), radius = 1.5.dp.toPx(), center = Offset(ledX, ledY)
    )
        if (isPlaying) {
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 0.5.dp.toPx(),
                center = Offset(ledX, ledY)
            ); drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFF3333).copy(alpha = 0.8f),
                        Color.Transparent
                    ), center = Offset(ledX, ledY), radius = 8.dp.toPx()
                ), radius = 8.dp.toPx(), center = Offset(ledX, ledY)
            )
        }
    }

    val btnWidth = size.width * 0.13f;
    val btnHeight = size.height * 0.075f;
    val btnTopLeft = Offset(size.width * 0.07f, size.height * 0.90f)
    drawRoundRect(
        color = Color(0x99000000),
        topLeft = btnTopLeft.copy(x = btnTopLeft.x - 1.dp.toPx(), y = btnTopLeft.y - 1.dp.toPx()),
        size = Size(btnWidth + 2.dp.toPx(), btnHeight + 2.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xCCFFFFFF),
        topLeft = btnTopLeft.copy(y = btnTopLeft.y + 1.dp.toPx()),
        size = Size(btnWidth, btnHeight),
        cornerRadius = CornerRadius(4.dp.toPx())
    ); drawRoundRect(
        color = Color(0xFF0A0A0A),
        topLeft = btnTopLeft,
        size = Size(btnWidth, btnHeight),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    clipPath(Path().apply {
        addRoundRect(
            RoundRect(
                Rect(btnTopLeft, Size(btnWidth, btnHeight)),
                CornerRadius(4.dp.toPx())
            )
        )
    }) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 0, 0, 0); maskFilter =
                BlurMaskFilter(6.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }; canvas.nativeCanvas.drawRoundRect(
            btnTopLeft.x - 4.dp.toPx(),
            btnTopLeft.y - 4.dp.toPx(),
            btnTopLeft.x + btnWidth,
            btnTopLeft.y + btnHeight,
            4.dp.toPx(),
            4.dp.toPx(),
            paint
        )
        }
    }

    val bW = btnWidth - (1.5.dp.toPx() * 2);
    val bH = btnHeight - (1.5.dp.toPx() * 2);
    val bTopLeft =
        btnTopLeft.copy(x = btnTopLeft.x + 1.5.dp.toPx(), y = btnTopLeft.y + 1.5.dp.toPx());
    val buttonCenter = Offset(bTopLeft.x + bW / 2f, bTopLeft.y + bH / 2f)

    scale(scale = 1f - (0.03f * buttonTravel), pivot = buttonCenter) {
        drawIntoCanvas { canvas ->
            val shadowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (200 * (1f - buttonTravel * 0.3f)).toInt(),
                    0,
                    0,
                    0
                ); maskFilter = BlurMaskFilter(
                4.dp.toPx() * (1f - buttonTravel) + 1.dp.toPx(),
                BlurMaskFilter.Blur.NORMAL
            )
            }; canvas.nativeCanvas.drawRoundRect(
            bTopLeft.x + 1.5.dp.toPx() * (1f - buttonTravel),
            bTopLeft.y + 3.dp.toPx() * (1f - buttonTravel),
            bTopLeft.x + bW + 1.5.dp.toPx() * (1f - buttonTravel),
            bTopLeft.y + bH + 3.dp.toPx() * (1f - buttonTravel),
            3.dp.toPx(),
            3.dp.toPx(),
            shadowPaint
        )
        }
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFAFAFA).copy(alpha = 1f - (buttonTravel * 0.12f)),
                    Color(0xFFBDBDBD).copy(alpha = 1f - (buttonTravel * 0.12f))
                ),
                start = Offset(bTopLeft.x, bTopLeft.y),
                end = Offset(bTopLeft.x + bW, bTopLeft.y + bH)
            ), topLeft = bTopLeft, size = Size(bW, bH), cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 1f - buttonTravel),
                    Color.Transparent,
                    Color(0x44000000)
                ),
                start = Offset(bTopLeft.x, bTopLeft.y),
                end = Offset(bTopLeft.x + bW, bTopLeft.y + bH)
            ),
            topLeft = bTopLeft,
            size = Size(bW, bH),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )
        drawCircle(
            brush = Brush.verticalGradient(
                0f to Color(0xFF777777),
                1f to Color(0xFFFFFFFF),
                startY = buttonCenter.y - bH * 0.38f,
                endY = buttonCenter.y + bH * 0.38f
            ), radius = bH * 0.38f, center = buttonCenter
        )
        for (i in 1..8) drawCircle(
            color = Color.Black.copy(0.03f),
            radius = bH * 0.38f * (i / 8f),
            center = buttonCenter,
            style = Stroke(0.5.dp.toPx())
        )
    }
}