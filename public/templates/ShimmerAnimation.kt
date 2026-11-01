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

import android.graphics.Matrix
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Immutable configuration for a shimmer effect.
 *
 * [colors] must contain at least two stops. Arrays derived from the color list are eagerly
 * precomputed so the draw phase does not allocate on every frame.
 */
@Immutable
data class ShimmerTheme(
    val colors: List<Color>,
    val durationMillis: Int = 2_000,
    val angle: Float = 20f,
) {
    internal val argbColors: IntArray = colors.map(Color::toArgb).toIntArray()
    internal val colorPositions: FloatArray = FloatArray(colors.size) { index ->
        index / (colors.size - 1).toFloat()
    }

    init {
        require(colors.size >= 2) { "ShimmerTheme requires at least 2 colors." }
    }
}

/** Common shimmer presets for demos and quick usage. */
object ShimmerThemes {
    val DefaultLight = ShimmerTheme(
        colors = listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9), Color(0xFFE2E8F0)),
        durationMillis = 1_500,
    )

    val Cyberpunk = ShimmerTheme(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF06B6D4),
            Color(0xFFD946EF),
            Color(0xFF0F172A)
        ),
        durationMillis = 1_800,
        angle = 20f,
    )

    val DataSpace = ShimmerTheme(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF3B82F6),
            Color(0xFF2DD4BF),
            Color(0xFF0F172A),
        ),
        durationMillis = 1_800,
        angle = 30f,
    )

    val SpatialSilver = ShimmerTheme(
        colors = listOf(
            Color(0xFFD1D5DB),
            Color(0xFFE2E8F0),
            Color(0xFFFFFFFF),
            Color(0xFFD1D5DB),
        ),
        durationMillis = 1_800,
        angle = 10f,
    )

    val NeoGlass = ShimmerTheme(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFE0F2FE),
            Color(0xFFFDE68A),
            Color(0xFFE9D5FF),
            Color(0xFFE2E8F0),
        ),
        durationMillis = 1_800,
        angle = 18f,
    )
}

/**
 * Shared animation state for all shimmer nodes under a [ShimmerProvider].
 *
 * A single clock is intentionally shared across the subtree so multiple placeholders animate in
 * sync without starting one coroutine per composable.
 */
@Stable
class ShimmerHostState(
    val theme: ShimmerTheme,
    val waveSize: Float,
) {
    var activeNodeCount by mutableIntStateOf(0)
    var progress by mutableFloatStateOf(0f)
}

private val LocalShimmerHostState = compositionLocalOf<ShimmerHostState?> { null }

/**
 * Provides a shared shimmer clock to all descendant [Modifier.shimmer] calls.
 *
 * The animation only runs while at least one attached shimmer node is visible and loading.
 * Descendants can still provide a [themeOverride] per modifier while reusing the same clock.
 */
@Composable
fun ShimmerProvider(
    theme: ShimmerTheme = ShimmerThemes.DefaultLight,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val waveSize = remember(screenWidthPx, screenHeightPx) {
        hypot(screenWidthPx, screenHeightPx) * 2f
    }

    val hostState = remember(theme, waveSize) {
        ShimmerHostState(theme = theme, waveSize = waveSize)
    }

    val isRunning = hostState.activeNodeCount > 0

    LaunchedEffect(isRunning, theme.durationMillis) {
        if (!isRunning) return@LaunchedEffect

        val durationNanos = theme.durationMillis * 1_000_000L
        var playTimeNanos = (hostState.progress * durationNanos).toLong()
        var lastFrameTimeNanos = withFrameNanos { it }

        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val deltaNanos = frameTimeNanos - lastFrameTimeNanos
                lastFrameTimeNanos = frameTimeNanos
                playTimeNanos += deltaNanos
                hostState.progress = (playTimeNanos % durationNanos) / durationNanos.toFloat()
            }
        }
    }

    CompositionLocalProvider(
        LocalShimmerHostState provides hostState,
        content = content,
    )
}

/**
 * Draws a shimmer placeholder on top of the node while [visible] is true.
 *
 * When used inside [ShimmerProvider], the modifier participates in the shared animation clock.
 * When used outside of a provider, the placeholder still renders with a static first frame.
 */
fun Modifier.shimmer(
    visible: Boolean,
    shape: Shape,
    themeOverride: ShimmerTheme? = null,
): Modifier = this then ShimmerElement(
    visible = visible,
    shape = shape,
    themeOverride = themeOverride,
)

private data class ShimmerElement(
    val visible: Boolean,
    val shape: Shape,
    val themeOverride: ShimmerTheme?,
) : ModifierNodeElement<ShimmerNode>() {

    override fun create(): ShimmerNode = ShimmerNode(
        visible = visible,
        shape = shape,
        themeOverride = themeOverride,
    )

    override fun update(node: ShimmerNode) {
        node.update(
            visible = visible,
            shape = shape,
            themeOverride = themeOverride,
        )
    }

    override fun androidx.compose.ui.platform.InspectorInfo.inspectableProperties() {
        name = "shimmer"
        properties["visible"] = visible
        properties["shape"] = shape
        properties["themeOverride"] = themeOverride
    }
}

/**
 * Draw node for the shimmer placeholder.
 *
 * The node caches shader-related objects and shape outlines to avoid per-frame allocations.
 * Global position is tracked so the gradient appears to travel across the full window instead of
 * resetting for each individual placeholder.
 */
private class ShimmerNode(
    var visible: Boolean,
    var shape: Shape,
    var themeOverride: ShimmerTheme?,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    ObserverModifierNode {

    private val shaderMatrix = Matrix()
    private val paint = Paint()

    private var globalX = 0f
    private var globalY = 0f

    private var cachedOutline: Outline? = null
    private var cachedSize: Size = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedShape: Shape? = null
    private var cachedTheme: ShimmerTheme? = null

    private var attachedHost: ShimmerHostState? = null

    override fun onAttach() {
        updateHostRegistration()
    }

    override fun onDetach() {
        attachedHost?.let { it.activeNodeCount -= 1 }
        attachedHost = null
    }

    override fun onObservedReadsChanged() {
        updateHostRegistration()
    }

    fun update(
        visible: Boolean,
        shape: Shape,
        themeOverride: ShimmerTheme?,
    ) {
        val visibilityChanged = this.visible != visible

        this.visible = visible
        this.shape = shape
        this.themeOverride = themeOverride

        if (visibilityChanged) {
            updateHostRegistration()
        }

        invalidateDraw()
    }

    private fun updateHostRegistration() {
        observeReads {
            val newHost = currentValueOf(LocalShimmerHostState)

            if (!visible) {
                attachedHost?.let { it.activeNodeCount -= 1 }
                attachedHost = null
                return@observeReads
            }

            if (newHost != attachedHost) {
                attachedHost?.let { it.activeNodeCount -= 1 }
                attachedHost = newHost
                attachedHost?.let { it.activeNodeCount += 1 }
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow()
        globalX = position.x
        globalY = position.y
    }

    override fun ContentDrawScope.draw() {
        if (!visible) {
            drawContent()
            return
        }

        val host = attachedHost
        val activeTheme = themeOverride ?: host?.theme

        if (activeTheme == null) {
            drawContent()
            return
        }

        val waveSize = host?.waveSize ?: hypot(size.width, size.height) * 2f
        val progress = host?.progress ?: 0f

        if (cachedSize != size || cachedTheme != activeTheme) {
            createShader(theme = activeTheme, waveSize = waveSize)
            cachedSize = size
            cachedTheme = activeTheme
        }

        if (
            cachedOutline == null ||
            cachedSize != size ||
            cachedLayoutDirection != layoutDirection ||
            cachedShape != shape
        ) {
            cachedOutline = shape.createOutline(size, layoutDirection, this)
            cachedLayoutDirection = layoutDirection
            cachedShape = shape
        }

        val translation = (progress * waveSize) - (waveSize / 2f)

        shaderMatrix.reset()
        shaderMatrix.setRotate(activeTheme.angle)
        shaderMatrix.postTranslate(
            translation - globalX,
            -globalY,
        )

        // Re-applying the shader after its local matrix changes avoids stale internal caches on
        // some older Android versions.
        (paint.shader as? android.graphics.Shader)?.let { shader ->
            shader.setLocalMatrix(shaderMatrix)
            paint.shader = shader
        }

        drawIntoCanvas { canvas ->
            canvas.drawOutline(cachedOutline!!, paint)
        }
    }

    private fun createShader(
        theme: ShimmerTheme,
        waveSize: Float,
    ) {
        val waveWidth = waveSize / 2f
        paint.shader = android.graphics.LinearGradient(
            0f,
            0f,
            waveWidth,
            0f,
            theme.argbColors,
            theme.colorPositions,
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
}

/**
 * Animated dark holographic background used by showcase cards after loading completes.
 */
fun Modifier.animatedDarkHoloBackground(
    enabled: Boolean,
    shape: Shape,
): Modifier = composed {
    if (!enabled) {
        return@composed this.background(Color(0xFF1E293B), shape)
    }

    val transition = rememberInfiniteTransition(label = "dark_holo_background")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dark_holo_angle",
    )

    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = hypot(size.width, size.height)

        onDrawBehind {
            val angleRadians = Math.toRadians(angle.value.toDouble())
            val brush = Brush.linearGradient(
                colors = listOf(Color(0xFFD946EF), Color(0xFF06B6D4), Color(0xFF3B82F6)),
                start = Offset.Zero,
                end = Offset(
                    x = (cos(angleRadians) * radius).toFloat(),
                    y = (sin(angleRadians) * radius).toFloat(),
                ),
            )
            drawOutline(outline = outline, brush = brush)
        }
    }
}

/**
 * Animated light holographic background used by the commerce hero once content is loaded.
 *
 * Two animations are combined intentionally:
 * - rotation keeps the background moving across large surfaces,
 * - alpha shifting adds a subtle premium "glass" feel without introducing another gradient pass.
 */
fun Modifier.animatedNeoGlassBackground(
    enabled: Boolean,
    shape: Shape,
): Modifier = composed {
    if (!enabled) {
        return@composed this.background(Color(0xFFF8FAFC), shape)
    }

    val transition = rememberInfiniteTransition(label = "neo_glass_background")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "neo_glass_angle",
    )
    val highlightShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "neo_glass_highlight_shift",
    )

    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = hypot(size.width, size.height)

        onDrawBehind {
            val angleRadians = Math.toRadians(angle.value.toDouble())
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE0F2FE).copy(alpha = 0.9f),
                    Color(0xFFFDE68A).copy(alpha = 0.7f + (highlightShift.value * 0.2f)),
                    Color(0xFFE9D5FF).copy(alpha = 0.8f),
                    Color(0xFFF8FAFC),
                ),
                start = Offset.Zero,
                end = Offset(
                    x = (cos(angleRadians) * radius).toFloat(),
                    y = (sin(angleRadians) * radius).toFloat(),
                ),
            )
            drawOutline(outline = outline, brush = brush)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerShowcaseScreen() {
    var isLoading by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(pageCount = { 4 })

    val backgroundColors = listOf(
        Color(0xFF0B0F19),
        Color(0xFF050B14),
        Color(0xFFF8FAFC),
        Color(0xFFFAFAFA),
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColors[pagerState.currentPage],
        animationSpec = tween(600),
        label = "showcase_background",
    )

    ShimmerProvider(theme = ShimmerThemes.Cyberpunk) {
        Scaffold(
            containerColor = animatedBackgroundColor,
            bottomBar = { ShowcasePagerIndicator(pagerState = pagerState, pageCount = 4) },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> AnalyticsDashboardScreen(
                        isLoading = isLoading,
                        theme = ShimmerThemes.Cyberpunk
                    )

                    1 -> DiagramScreen(isLoading = isLoading, theme = ShimmerThemes.DataSpace)
                    2 -> SpatialSmartHomeScreen(
                        isLoading = isLoading,
                        theme = ShimmerThemes.SpatialSilver
                    )

                    3 -> MinimalCommerceScreen(
                        isLoading = isLoading,
                        theme = ShimmerThemes.NeoGlass
                    )
                }
            }
        }
    }
}

@Composable
fun ShowcasePagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
) {
    val activeColors = listOf(
        Color(0xFF06B6D4),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "indicator_width_$index",
            )

            val inactiveColor = if (pagerState.currentPage >= 2) {
                Color(0xFFCBD5E1)
            } else {
                Color(0xFF334155)
            }

            val color by animateColorAsState(
                targetValue = if (isSelected) activeColors[index] else inactiveColor,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "indicator_color_$index",
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .height(8.dp)
                    .width(width)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
fun AnalyticsDashboardScreen(
    isLoading: Boolean,
    theme: ShimmerTheme,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                        .background(Color(0xFF1E293B), CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(18.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(Color(0xFF1E293B)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(Color(0xFF1E293B)),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                    .background(Color(0xFF1E293B), CircleShape),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(24.dp),
                    themeOverride = theme
                )
                .animatedDarkHoloBackground(
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp)
                ),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(16.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .width(140.dp)
                .height(20.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(4.dp),
                    themeOverride = theme
                )
                .background(Color(0xFF1E293B)),
        )

        Spacer(modifier = Modifier.height(24.dp))

        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(12.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(Color(0xFF1E293B)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(Color(0xFF1E293B)),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(4.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B)),
                )
            }
        }
    }
}

@Composable
fun DiagramScreen(
    isLoading: Boolean,
    theme: ShimmerTheme,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(24.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(6.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(4.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B)),
                )
            }
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
                    .shimmer(
                        visible = isLoading,
                        shape = RoundedCornerShape(20.dp),
                        themeOverride = theme
                    )
                    .background(Color(0xFF1E293B), RoundedCornerShape(20.dp)),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                    .background(Color(0xFF334155), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E293B), CircleShape),
            )
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(20.dp)
                    .shimmer(
                        visible = isLoading,
                        shape = RoundedCornerShape(4.dp),
                        themeOverride = theme
                    )
                    .background(Color(0xFF475569)),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 1.0f, 0.3f, 0.8f)
                heights.forEach { heightFraction ->
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight(heightFraction)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                themeOverride = theme,
                            )
                            .background(
                                Color(0xFF334155),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(4.dp),
                    themeOverride = theme
                )
                .background(Color(0xFF1E293B)),
        )

        Spacer(modifier = Modifier.height(24.dp))

        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                        .background(Color(0xFF1E293B), CircleShape),
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(4.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B)),
                )

                Spacer(modifier = Modifier.width(24.dp))

                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(4.dp),
                            themeOverride = theme
                        )
                        .background(Color(0xFF1E293B)),
                )
            }
        }
    }
}

@Composable
fun SpatialSmartHomeScreen(
    isLoading: Boolean,
    theme: ShimmerTheme,
) {
    val loadedBaseColor = Color(0xFFE2E8F0)
    val loadedInnerColor = Color(0xFFCBD5E1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(28.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(8.dp),
                            themeOverride = theme
                        )
                        .background(loadedBaseColor),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(14.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(4.dp),
                            themeOverride = theme
                        )
                        .background(loadedBaseColor),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFF8FAFC), CircleShape)
                            .padding(2.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = CircleShape,
                                themeOverride = theme
                            )
                            .background(loadedBaseColor, CircleShape),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .shimmer(
                        visible = isLoading,
                        shape = RoundedCornerShape(32.dp),
                        themeOverride = theme
                    )
                    .background(loadedBaseColor, RoundedCornerShape(32.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(24.dp),
                            themeOverride = theme
                        )
                        .background(loadedBaseColor, RoundedCornerShape(24.dp)),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(24.dp),
                            themeOverride = theme
                        )
                        .background(loadedBaseColor, RoundedCornerShape(24.dp)),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(24.dp),
                    themeOverride = theme
                )
                .background(loadedBaseColor, RoundedCornerShape(24.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(12.dp),
                            themeOverride = theme
                        )
                        .background(loadedInnerColor, RoundedCornerShape(12.dp)),
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(loadedInnerColor),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(loadedInnerColor),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = CircleShape,
                                themeOverride = theme
                            )
                            .background(loadedInnerColor),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                        .background(loadedInnerColor, CircleShape),
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .width(160.dp)
                .height(20.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(6.dp),
                    themeOverride = theme
                )
                .background(loadedBaseColor),
        )

        Spacer(modifier = Modifier.height(24.dp))

        val placeholders = List(5) { it }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(placeholders) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(140.dp)
                        .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                        .background(loadedBaseColor, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(32.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = CircleShape,
                                themeOverride = theme
                            )
                            .background(loadedInnerColor, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalCommerceScreen(
    isLoading: Boolean,
    theme: ShimmerTheme,
) {
    val loadedBaseColor = Color(0xFFF8FAFC)
    val loadedInnerColor = Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                        .background(loadedBaseColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(loadedBaseColor),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .shimmer(
                                visible = isLoading,
                                shape = RoundedCornerShape(4.dp),
                                themeOverride = theme
                            )
                            .background(loadedBaseColor),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shimmer(visible = isLoading, shape = CircleShape, themeOverride = theme)
                    .background(loadedBaseColor, CircleShape),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(16.dp),
                    themeOverride = theme
                )
                .background(loadedBaseColor, RoundedCornerShape(16.dp)),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(24.dp),
                    themeOverride = theme
                )
                .animatedNeoGlassBackground(
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp)
                ),
        )

        Spacer(modifier = Modifier.height(32.dp))

        val categories = List(5) { it }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(36.dp)
                        .shimmer(
                            visible = isLoading,
                            shape = RoundedCornerShape(24.dp),
                            themeOverride = theme
                        )
                        .background(loadedBaseColor, RoundedCornerShape(24.dp)),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .width(140.dp)
                .height(20.dp)
                .shimmer(
                    visible = isLoading,
                    shape = RoundedCornerShape(6.dp),
                    themeOverride = theme
                )
                .background(loadedBaseColor),
        )

        Spacer(modifier = Modifier.height(24.dp))

        repeat(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(2) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .shimmer(
                                    visible = isLoading,
                                    shape = RoundedCornerShape(16.dp),
                                    themeOverride = theme
                                )
                                .background(loadedBaseColor, RoundedCornerShape(16.dp)),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .shimmer(
                                    visible = isLoading,
                                    shape = RoundedCornerShape(4.dp),
                                    themeOverride = theme
                                )
                                .background(loadedInnerColor),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(14.dp)
                                .shimmer(
                                    visible = isLoading,
                                    shape = RoundedCornerShape(4.dp),
                                    themeOverride = theme
                                )
                                .background(loadedInnerColor),
                        )
                    }
                }
            }
        }
    }
}
