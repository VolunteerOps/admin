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

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow

/**
 * Gameplay timing. Gravity starts at [BASE_TICK_MS] per row and speeds up by
 * [SPEED_STEP_PER_LEVEL_MS] each level, floored at [MIN_TICK_MS]. Held D-pad input
 * auto-repeats arcade-style: DAS (Delayed Auto Shift) waits [DAS_DELAY_MS] after the
 * first move, then ARR (Auto Repeat Rate) fires every [ARR_INTERVAL_MS].
 */
private object BrickConfig {
    const val BASE_TICK_MS = 800L
    const val SPEED_STEP_PER_LEVEL_MS = 60L
    const val MIN_TICK_MS = 100L
    const val DAS_DELAY_MS = 200L
    const val ARR_INTERVAL_MS = 50L
}

/** Shared by every physical control: stiff enough to settle within the key travel, no visible bounce. */
private val PressSpring = spring<Float>(stiffness = Spring.StiffnessHigh)

// The seven tetrominoes as 0/1 row-major bitmaps.
private val I_PIECE = listOf(listOf(1, 1, 1, 1))
private val J_PIECE = listOf(listOf(1, 0, 0), listOf(1, 1, 1))
private val L_PIECE = listOf(listOf(0, 0, 1), listOf(1, 1, 1))
private val O_PIECE = listOf(listOf(1, 1), listOf(1, 1))
private val S_PIECE = listOf(listOf(0, 1, 1), listOf(1, 1, 0))
private val T_PIECE = listOf(listOf(0, 1, 0), listOf(1, 1, 1))
private val Z_PIECE = listOf(listOf(1, 1, 0), listOf(0, 1, 1))
private val SHAPES = listOf(I_PIECE, J_PIECE, L_PIECE, O_PIECE, S_PIECE, T_PIECE, Z_PIECE)

/**
 * A falling tetromino: a [matrix] bitmap positioned at board column [x] / row [y].
 * Immutable on purpose — moves and rotations produce copies, so every step is a
 * distinct snapshot value the draw phase can observe.
 */
@Immutable
private data class BrickPiece(val matrix: List<List<Int>>, val x: Int = 3, val y: Int = 0) {
    /** 90° clockwise rotation: `new(i)(j) = old(rows - 1 - j)(i)`. O is rotation-symmetric, skip the copy. */
    fun rotated(): BrickPiece {
        if (matrix == O_PIECE) return this
        val r = matrix.size
        val c = matrix[0].size
        val newMatrix = List(c) { i -> List(r) { j -> matrix[r - 1 - j][i] } }
        return copy(matrix = newMatrix)
    }
}

/**
 * Classic falling-blocks rules on a 10×20 board. Rows 0–1 are the hidden spawn zone —
 * only rows 2..19 are rendered, so new pieces slide into view instead of popping in.
 * Scoring uses the classic 40/100/300/1200 × level table and the level rises every
 * 10 cleared lines.
 *
 * Every mutable property is snapshot state: the LCD reads [board] and [currentPiece]
 * in the draw phase, so a gravity tick repaints pixels without recomposing anything.
 */
@Stable
private class BrickGame {
    var board by mutableStateOf(List(20) { List(10) { 0 } })
    var currentPiece by mutableStateOf(BrickPiece(SHAPES.random()))
    var nextPiece by mutableStateOf(BrickPiece(SHAPES.random()))
    var score by mutableIntStateOf(0)
    var level by mutableIntStateOf(1)
    var lines by mutableIntStateOf(0)
    var isGameOver by mutableStateOf(false)

    fun moveLeft() {
        if (isValid(currentPiece.matrix, currentPiece.x - 1, currentPiece.y)) {
            currentPiece = currentPiece.copy(x = currentPiece.x - 1)
        }
    }

    fun moveRight() {
        if (isValid(currentPiece.matrix, currentPiece.x + 1, currentPiece.y)) {
            currentPiece = currentPiece.copy(x = currentPiece.x + 1)
        }
    }

    fun rotate() {
        val p = currentPiece.rotated()
        if (isValid(p.matrix, p.x, p.y)) currentPiece = p
    }

    /** Teleports the piece to the lowest valid row and locks it in one move. */
    fun hardDrop() {
        var dropY = currentPiece.y
        while (isValid(currentPiece.matrix, currentPiece.x, dropY + 1)) dropY++
        currentPiece = currentPiece.copy(y = dropY)
        lockAndSpawn()
    }

    /** One gravity step: fall one row, or lock the piece once it rests on something. */
    fun tick() {
        if (isValid(currentPiece.matrix, currentPiece.x, currentPiece.y + 1)) {
            currentPiece = currentPiece.copy(y = currentPiece.y + 1)
        } else {
            lockAndSpawn()
        }
    }

    fun reset() {
        board = List(20) { List(10) { 0 } }
        score = 0
        level = 1
        lines = 0
        isGameOver = false
        currentPiece = BrickPiece(SHAPES.random())
        nextPiece = BrickPiece(SHAPES.random())
    }

    /** True if [matrix] placed at ([cx], [cy]) stays on the board and overlaps no settled block. */
    private fun isValid(matrix: List<List<Int>>, cx: Int, cy: Int): Boolean {
        for (r in matrix.indices) {
            for (c in matrix[r].indices) {
                if (matrix[r][c] == 1) {
                    val nx = cx + c
                    val ny = cy + r
                    if (nx !in 0..9 || ny !in 0..19 || board[ny][nx] == 1) return false
                }
            }
        }
        return true
    }

    /** Stamps the piece into the board, clears and scores full lines, then spawns the next piece. */
    private fun lockAndSpawn() {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        var gameOver = false
        for (r in currentPiece.matrix.indices) {
            for (c in currentPiece.matrix[r].indices) {
                if (currentPiece.matrix[r][c] == 1) {
                    val ny = currentPiece.y + r
                    if (ny < 0) gameOver = true
                    else newBoard[ny][currentPiece.x + c] = 1
                }
            }
        }
        if (gameOver) {
            isGameOver = true
            return
        }

        val fullLines = newBoard.indices.filter { r -> newBoard[r].all { it == 1 } }
        fullLines.reversed().forEach { newBoard.removeAt(it) }
        repeat(fullLines.size) { newBoard.add(0, MutableList(10) { 0 }) }

        board = newBoard
        lines += fullLines.size
        level = 1 + (lines / 10)
        score += when (fullLines.size) {
            1 -> 40 * level
            2 -> 100 * level
            3 -> 300 * level
            4 -> 1200 * level
            else -> 0
        }

        currentPiece = nextPiece.copy(x = 3, y = 0)
        nextPiece = BrickPiece(SHAPES.random())
        if (!isValid(currentPiece.matrix, currentPiece.x, currentPiece.y)) isGameOver = true
    }
}

// Palette sampled from photo reference of the classic 1989 handheld. Casing greys come as a
// highlight/base/shadow triplet so every surface can fake the same top-left
// key light; the LCD pair (base/dark tint) reproduces the olive "pea soup"
// screen with its faintly visible off pixels.
private val CasingBase = Color(0xFFCFCFCB)
private val CasingHighlight = Color(0xFFEBEBEB)
private val CasingShadow = Color(0xFFA0A09B)
private val BezelBase = Color(0xFF5A5B64)
private val BezelDark = Color(0xFF323339)
private val ScreenBase = Color(0xFF7D9221)
private val ScreenDark = Color(0xFF6B8018)
private val PixelDark = Color(0xFF1E3814)
private val ButtonMagentaBase = Color(0xFF9D1E5C)
private val ButtonMagentaHighlight = Color(0xFFCE307E)
private val ButtonMagentaShadow = Color(0xFF5A0C30)
private val DPadBase = Color(0xFF191919)
private val LogoBlue = Color(0xFF1B2264)

/**
 * One star in the background parallax field. [xOffset]/[yOffset] are normalized 0..1
 * positions; [speed] multiplies the global drift phase, so faster (nearer) stars streak
 * past slower (farther) ones — cheap parallax from a single animated value.
 */
@Immutable
private data class RetroStar(
    val xOffset: Float,
    val yOffset: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float
)

/**
 * Arcade-grade press handling. [onPress] fires on the raw pointer-down — no tap slop,
 * no waiting for up like `clickable` — which is what makes the controls feel zero-latency.
 * With [repeating], held input replays DAS/ARR style (one move, a beat, then a rapid
 * stream), matching how the original console treats a held D-pad.
 *
 * [isPressed] is only written here, never read, so consumers decide the observation
 * phase — this file reads it exclusively inside draw lambdas and snapshotFlows,
 * keeping button mashing recomposition-free.
 */
private fun Modifier.arcadeButton(
    isPressed: MutableState<Boolean>? = null,
    repeating: Boolean = false,
    onPress: () -> Unit
): Modifier = pointerInput(Unit) {
    coroutineScope {
        awaitPointerEventScope {
            while (true) {
                awaitFirstDown(requireUnconsumed = false)
                isPressed?.value = true
                onPress()

                val repeatJob = if (repeating) {
                    launch {
                        delay(BrickConfig.DAS_DELAY_MS)
                        while (true) {
                            delay(BrickConfig.ARR_INTERVAL_MS)
                            onPress()
                        }
                    }
                } else null

                waitForUpOrCancellation()
                isPressed?.value = false
                repeatJob?.cancel()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun BrickGameScreenPreview() {
    BrickGameScreen()
}

/**
 * The Jetpack Compose BRICK GAME — a playable retro handheld rendered entirely with
 * Compose draw primitives — no bitmaps. The hardware art is authored on a fixed
 * 350×630dp canvas and uniformly scaled to the viewport, so every hand-tuned
 * proportion survives any screen size.
 */
@Composable
fun BrickGameScreen() {
    val game = remember { BrickGame() }
    BrickGravity(game)

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RetroAnimatedBackground()
        val scaleX = maxWidth / 350.dp
        val scaleY = maxHeight / 630.dp
        val dynamicScale = min(scaleX, scaleY) * 0.92f

        Box(modifier = Modifier.scale(dynamicScale)) {
            ConsoleRealistic(game)
        }
    }
}

/** Gravity tick loop. snapshotFlow restarts it on level/game-over changes without recomposing. */
@Composable
private fun BrickGravity(game: BrickGame) {
    LaunchedEffect(Unit) {
        snapshotFlow { game.isGameOver to game.level }
            .collectLatest { (isGameOver, level) ->
                if (isGameOver) return@collectLatest
                val tickMs =
                    (BrickConfig.BASE_TICK_MS - (level - 1) * BrickConfig.SPEED_STEP_PER_LEVEL_MS)
                        .coerceAtLeast(BrickConfig.MIN_TICK_MS)
                while (true) {
                    delay(tickMs)
                    game.tick()
                }
            }
    }
}

/**
 * Synthwave backdrop in four layers: gradient sky, parallax starfield, a scan-lined sun,
 * and a perspective floor grid. Perspective is faked with two tricks — the sun's dark
 * bands sit at `horizonY - i² · step` (quadratic spacing compresses toward the horizon,
 * like foreshortening) and the floor's horizontal lines at `1.4^i` (exponential spacing
 * expands toward the viewer). Animating the exponent/index by one full unit per cycle
 * makes both loops seamless. All brushes and px constants are cached; the three phase
 * values are read only inside the draw pass, so 60fps costs zero recompositions.
 */
@Composable
private fun RetroAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "arcade_bg")
    val gridPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "grid"
    )
    val sunPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "sun"
    )
    val starPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "stars"
    )
    val stars = remember {
        val random = kotlin.random.Random(42)
        List(200) {
            RetroStar(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat() * 2.5f + 0.5f,
                random.nextFloat() * 0.7f + 0.1f,
                random.nextFloat() * 3f + 1f
            )
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val horizonY = size.height * 0.65f
                val skySize = Size(size.width, horizonY)
                val skyBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070212),
                        Color(0xFF240B45),
                        Color(0xFF7D145F),
                        Color(0xFFF75C54)
                    ),
                    startY = 0f, endY = horizonY
                )
                val starFieldHeight = horizonY * 0.8f
                val sunRadius = min(size.width, size.height) * 0.55f
                val sunCenter = Offset(size.width / 2, horizonY)
                val sunBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE53B), Color(0xFFFF2525)),
                    startY = sunCenter.y - sunRadius,
                    endY = sunCenter.y
                )
                val scanColor = Color(0xFFF75C54)
                val scanStepPx = 1.8f.dp.toPx()
                val scanUnitPx = 1.2f.dp.toPx()
                val horizonBrush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF00FFFF), Color.Transparent)
                )
                val horizonStrokePx = 6.dp.toPx()
                val groundBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF130624), Color(0xFF05020B)),
                    startY = horizonY,
                    endY = size.height
                )
                val groundTopLeft = Offset(0f, horizonY)
                val groundSize = Size(size.width, size.height - horizonY)
                val gridColor = Color(0xFF00FFFF)
                val gridInitialStep = 6.dp.toPx()
                val gridStrokePx = 1.5f.dp.toPx()
                val vpX = size.width / 2
                val vpY = horizonY - 60.dp.toPx()
                val vLineSpacingPx = 80.dp.toPx()

                onDrawBehind {
                    drawRect(brush = skyBrush, size = skySize)

                    stars.forEach { star ->
                        val x = ((star.xOffset + starPhase * star.speed) % 1f) * size.width
                        val y = star.yOffset * starFieldHeight
                        drawCircle(
                            color = Color.White.copy(alpha = star.alpha),
                            radius = star.radius,
                            center = Offset(x, y)
                        )
                    }

                    drawCircle(brush = sunBrush, radius = sunRadius, center = sunCenter)

                    for (i in -2..25) {
                        val scanIndex = i - sunPhase
                        val lineY = horizonY - (scanIndex * scanIndex * scanStepPx)
                        if (lineY < sunCenter.y - sunRadius || lineY > horizonY) continue
                        val thickness = (20f - scanIndex).coerceAtLeast(0f) * scanUnitPx
                        drawLine(
                            color = scanColor,
                            start = Offset(0f, lineY),
                            end = Offset(size.width, lineY),
                            strokeWidth = thickness
                        )
                    }

                    drawLine(
                        brush = horizonBrush,
                        start = Offset(0f, horizonY),
                        end = Offset(size.width, horizonY),
                        strokeWidth = horizonStrokePx
                    )

                    drawRect(brush = groundBrush, topLeft = groundTopLeft, size = groundSize)

                    clipRect(left = 0f, top = horizonY, right = size.width, bottom = size.height) {
                        for (i in -5..25) {
                            val powY = 1.4f.pow(i + gridPhase)
                            val lineY = horizonY + gridInitialStep * powY
                            if (lineY > horizonY && lineY < size.height) {
                                val lineAlpha =
                                    ((lineY - horizonY) / (size.height - horizonY)).coerceIn(0.1f, 1f)
                                drawLine(
                                    gridColor.copy(alpha = lineAlpha * 0.6f),
                                    Offset(0f, lineY),
                                    Offset(size.width, lineY),
                                    gridStrokePx
                                )
                            }
                        }
                        for (i in -40..40) {
                            val endX = vpX + (i * vLineSpacingPx)
                            drawLine(
                                gridColor.copy(alpha = 0.4f),
                                Offset(vpX, vpY),
                                Offset(endX, size.height),
                                gridStrokePx
                            )
                        }
                    }
                }
            }
    )
}

/**
 * The console shell. Depth comes from a two-shadow setup borrowed from product
 * photography: a wide, faint ambient blur (45dp, 31% black) plus a tight, dark
 * contact blur (15dp, 63% black) — one blur alone reads flat. The body gradient
 * runs highlight→shadow along the same top-left key light as every control, and
 * the asymmetric 64dp corner is the retro handheld's signature bottom-right curve.
 */
@Composable
private fun ConsoleRealistic(game: BrickGame) {
    Box(
        modifier = Modifier
            .width(350.dp)
            .height(630.dp)
            .drawWithCache {
                val ambientPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(80, 0, 0, 0)
                    maskFilter = BlurMaskFilter(45.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
                val contactPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(160, 0, 0, 0)
                    maskFilter = BlurMaskFilter(15.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
                val cornerRad = 64.dp.toPx()
                val ambientOffsetX = 15.dp.toPx()
                val ambientOffsetY = 25.dp.toPx()
                val contactOffsetX = 5.dp.toPx()
                val contactOffsetY = 10.dp.toPx()

                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            ambientOffsetX,
                            ambientOffsetY,
                            size.width + ambientOffsetX,
                            size.height + ambientOffsetY,
                            cornerRad,
                            cornerRad,
                            ambientPaint
                        )
                        canvas.nativeCanvas.drawRoundRect(
                            contactOffsetX,
                            contactOffsetY,
                            size.width + contactOffsetX,
                            size.height + contactOffsetY,
                            cornerRad,
                            cornerRad,
                            contactPaint
                        )
                    }
                }
            }
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = 64.dp,
                    bottomStart = 16.dp
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CasingHighlight,
                        CasingBase,
                        CasingShadow
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGrooves()
            Spacer(modifier = Modifier.height(16.dp))
            ScreenBezel(modifier = Modifier.padding(horizontal = 24.dp), game = game)
            LogoSection()
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPadRealistic(game)
                ActionButtonsRealistic(game)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 28.dp, end = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 62.dp, y = (-25).dp)
                ) {
                    SystemButtonsRealistic(game)
                }
                Box(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 18.dp)) {
                    SpeakerGrillRealistic()
                }
            }
        }
    }
}

/**
 * The grooves cut across the top edge and the ON/OFF power switch. A groove is two
 * parallel lines — [CasingShadow] on the near wall, white on the far lip — because
 * under a top-left light a recess darkens where it faces the light and catches a
 * highlight on the opposite edge. A third faint dark line down the middle plays the
 * cut itself. The switch pill inverts the trick (shadow up-left, highlight down-right)
 * so it reads as raised.
 */
@Composable
private fun TopGrooves() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(26.dp)) {
        val pillOffsetX = 44.dp
        val pillWidth = 56.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            val y = size.height - stroke * 1.5f
            val leftX = 24.dp.toPx()
            val rightX = size.width - 24.dp.toPx()

            drawLine(
                color = CasingShadow,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(0f, y + stroke),
                end = Offset(size.width, y + stroke),
                strokeWidth = stroke
            )
            drawLine(
                color = CasingShadow,
                start = Offset(leftX, 0f),
                end = Offset(leftX, y - stroke / 2),
                strokeWidth = stroke
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(leftX + stroke, 0f),
                end = Offset(leftX + stroke, y + stroke / 2),
                strokeWidth = stroke
            )
            drawLine(
                color = CasingShadow,
                start = Offset(rightX, 0f),
                end = Offset(rightX, y - stroke / 2),
                strokeWidth = stroke
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(rightX + stroke, 0f),
                end = Offset(rightX + stroke, y + stroke / 2),
                strokeWidth = stroke
            )

            val cutStroke = stroke * 0.4f
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(0f, y + stroke * 0.3f),
                end = Offset(size.width, y + stroke * 0.3f),
                strokeWidth = cutStroke
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(leftX + stroke * 0.3f, 0f),
                end = Offset(leftX + stroke * 0.3f, y),
                strokeWidth = cutStroke
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(rightX + stroke * 0.3f, 0f),
                end = Offset(rightX + stroke * 0.3f, y),
                strokeWidth = cutStroke
            )

            val switchCenterX = pillOffsetX.toPx() + (pillWidth.toPx() * 0.30f)
            val ridgeTop = 0f
            val ridgeBottom = 4.5f.dp.toPx()
            for (i in -1..1) {
                val lineX = switchCenterX + (i * 4.5f.dp.toPx())
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(lineX - 1.dp.toPx(), ridgeTop),
                    end = Offset(lineX - 1.dp.toPx(), ridgeBottom),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = CasingBase,
                    start = Offset(lineX, ridgeTop),
                    end = Offset(lineX, ridgeBottom),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = CasingShadow.copy(alpha = 0.95f),
                    start = Offset(lineX + 1.dp.toPx(), ridgeTop),
                    end = Offset(lineX + 1.dp.toPx(), ridgeBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = pillOffsetX, y = 6.dp)
                .width(pillWidth)
                .height(13.dp)
                .drawWithCache {
                    val corner = CornerRadius(size.height / 2)
                    val edgeBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.8f)
                        ), start = Offset(0f, 0f), end = Offset(size.width, size.height)
                    )
                    val edgeStroke = Stroke(width = 1.dp.toPx())
                    val bevelPx = 1.dp.toPx()

                    onDrawBehind {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(bevelPx, bevelPx),
                            size = size,
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            color = CasingShadow.copy(alpha = 0.8f),
                            topLeft = Offset(-bevelPx, -bevelPx),
                            size = size,
                            cornerRadius = corner
                        )
                        drawRoundRect(color = CasingBase, size = size, cornerRadius = corner)
                        drawRoundRect(
                            brush = edgeBrush,
                            size = size,
                            cornerRadius = corner,
                            style = edgeStroke
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val text = "◀ OFF • ON ▶"
            // Molded lettering: dark copy nudged down-right, light copy up-left,
            // face color on top — three stacked Texts fake an embossed stamp.
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = CasingShadow.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.offset(x = 0.5.dp, y = 0.5.dp)
                )
                Text(
                    text = text,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.offset(x = (-0.5).dp, y = (-0.5).dp)
                )
                Text(
                    text = text,
                    color = Color(0xFFE2E2DF),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

/**
 * The charcoal bezel around the LCD: radial gradient centered off the top-left so the
 * panel catches light unevenly, a 4dp inner border stroke standing in for the glass
 * inset, plus the printed trim lines and the "DOT MATRIX WITH ZERO RECOMPOSITION"
 * legend — the original's marketing line swapped for this file's engineering claim.
 */
@Composable
private fun ScreenBezel(modifier: Modifier = Modifier, game: BrickGame) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(
                RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomEnd = 50.dp,
                    bottomStart = 12.dp
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(BezelBase, BezelDark),
                    center = Offset(400f, 400f),
                    radius = 800f
                )
            )
            .drawBehind {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5f.dp)
                            .background(ButtonMagentaBase)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5f.dp)
                        .background(LogoBlue))
                }
                Text(
                    "DOT MATRIX WITH ZERO RECOMPOSITION",
                    color = Color(0xFFAAAABB),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5f.dp)
                            .background(ButtonMagentaBase)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5f.dp)
                        .background(LogoBlue))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxSize()) {
                BatteryIndicator()
                LCDScreen(game)
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

/**
 * The battery LED, built as four layers of a lit glass dome: a soft red halo bleeding
 * onto the bezel, a dark socket ring, a radial core whose hot spot sits off-center
 * toward the light, and a small rotated specular streak on top.
 */
@Composable
private fun BatteryIndicator() {
    Column(
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Canvas(modifier = Modifier.size(20.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = 5.dp.toPx()
            val innerRadius = 4.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF2222).copy(alpha = 0.5f),
                        Color.Transparent
                    ), center = center, radius = outerRadius * 2.5f
                ), radius = outerRadius * 2.5f, center = center
            )
            drawCircle(color = Color(0xFF111111), radius = outerRadius, center = center)
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF9999),
                        Color(0xFFDD0000),
                        Color(0xFF550000)
                    ),
                    center = center.copy(
                        x = center.x - innerRadius * 0.2f,
                        y = center.y - innerRadius * 0.2f
                    ),
                    radius = innerRadius * 1.2f
                ), radius = innerRadius, center = center
            )
            rotate(
                degrees = -45f,
                pivot = center.copy(
                    x = center.x - innerRadius * 0.3f,
                    y = center.y - innerRadius * 0.3f
                )
            ) {
                drawOval(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(center.x - innerRadius * 0.6f, center.y - innerRadius * 0.75f),
                    size = Size(innerRadius * 0.8f, innerRadius * 0.35f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("BATTERY", color = Color(0xFFAAAABB), fontSize = 6.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The dot-matrix LCD, split into two draw layers. The static parent draws the visible
 * pixel grid, the dark side panel and an inset vignette (dark top/left, light bottom/right —
 * the screen sits *behind* the bezel, so the shading is the inverse of a raised surface).
 * The cached child layer bakes the brick-pattern walls into two Paths once per size and
 * repaints only the falling blocks, so a gravity tick costs one draw pass and zero
 * recompositions. Board rows 0–1 are the hidden spawn zone and are simply not drawn.
 */
@Composable
private fun RowScope.LCDScreen(game: BrickGame) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(ScreenBase)
            .drawBehind {
                val pad = 10.dp.toPx()
                val innerHeight = size.height - (pad * 2)
                val blockSize = innerHeight / 19f
                val rightPanelX = pad + (blockSize * 12f)

                val gridSpacing = 4.dp.toPx()
                for (x in 0..rightPanelX.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = ScreenDark,
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = ScreenDark,
                        start = Offset(0f, y.toFloat()),
                        end = Offset(rightPanelX, y.toFloat()),
                        strokeWidth = 1f
                    )
                }

                drawRect(
                    color = PixelDark,
                    topLeft = Offset(rightPanelX, 0f),
                    size = Size(size.width - rightPanelX, size.height)
                )

                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 6.dp.toPx())
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(6.dp.toPx(), size.height)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(0f, size.height - 4f),
                    size = Size(size.width, 4f)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(size.width - 4f, 0f),
                    size = Size(4f, size.height)
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .drawWithCache {
                    val blockSize = size.height / 19f
                    val playFieldWidth = blockSize * 10f
                    val playFieldHeight = blockSize * 18f
                    val playFieldX = blockSize

                    val wallStroke = Stroke(width = 2.dp.toPx())
                    val brickStroke = Stroke(width = 1.5f.dp.toPx())
                    val blockStroke = Stroke(width = 1.5f.dp.toPx())
                    val blockPad = 1.dp.toPx()
                    val blockInnerPad = 3.dp.toPx()
                    val blockSide = blockSize - blockPad * 2
                    val blockOuterSize = Size(blockSide, blockSide)
                    val blockInnerSize =
                        Size(blockSide - blockInnerPad * 2, blockSide - blockInnerPad * 2)

                    // Walls and brick pattern are static per size — bake them into paths once.
                    val wallOutline = Path()
                    val brickPattern = Path()

                    fun addWall(x: Float, y: Float, w: Float, h: Float) {
                        wallOutline.addRect(Rect(x, y, x + w, y + h))
                        var currY = y + blockSize / 2f
                        var isShifted = false
                        while (currY < y + h) {
                            brickPattern.moveTo(x, currY)
                            brickPattern.lineTo(x + w, currY)
                            var currX = x + (if (isShifted) blockSize / 2f else 0f)
                            while (currX < x + w) {
                                brickPattern.moveTo(currX, currY - blockSize / 2f)
                                brickPattern.lineTo(currX, currY)
                                currX += blockSize
                            }
                            currY += blockSize / 2f
                            isShifted = !isShifted
                        }
                    }

                    addWall(0f, 0f, blockSize, playFieldHeight)
                    addWall(playFieldX + playFieldWidth, 0f, blockSize, playFieldHeight)
                    addWall(0f, playFieldHeight, playFieldWidth + blockSize * 2, blockSize)
                    wallOutline.addRect(
                        Rect(playFieldX, 0f, playFieldX + playFieldWidth, playFieldHeight)
                    )

                    onDrawBehind {
                        drawPath(wallOutline, PixelDark, style = wallStroke)
                        drawPath(brickPattern, PixelDark, style = brickStroke)

                        fun drawBrick(col: Int, row: Int) {
                            val topX = playFieldX + col * blockSize + blockPad
                            val topY = row * blockSize + blockPad
                            drawRect(
                                color = PixelDark,
                                topLeft = Offset(topX, topY),
                                size = blockOuterSize,
                                style = blockStroke
                            )
                            drawRect(
                                color = PixelDark,
                                topLeft = Offset(topX + blockInnerPad, topY + blockInnerPad),
                                size = blockInnerSize
                            )
                        }

                        for (r in 2..19) {
                            for (c in 0..9) {
                                if (game.board[r][c] == 1) drawBrick(c, r - 2)
                            }
                        }

                        if (!game.isGameOver) {
                            val piece = game.currentPiece
                            for (r in piece.matrix.indices) {
                                for (c in piece.matrix[r].indices) {
                                    if (piece.matrix[r][c] == 1) {
                                        val boardY = piece.y + r
                                        if (boardY >= 2) drawBrick(piece.x + c, boardY - 2)
                                    }
                                }
                            }
                        }
                    }
                }
        )

        BrickStats(
            game = game,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        )

        GameOverBadge(game)
    }
}

/** Isolates score/level/lines text reads so per-lock recompositions stay off [LCDScreen]. */
@Composable
private fun BrickStats(game: BrickGame, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        val labelWidth = 56.dp
        StatLabel("SCORE", "${game.score}", labelWidth)
        Spacer(modifier = Modifier.height(14.dp))
        StatLabel("LEVEL", "${game.level}", labelWidth)
        Spacer(modifier = Modifier.height(14.dp))
        StatLabel("LINES", "${game.lines}", labelWidth)
        Spacer(modifier = Modifier.height(14.dp))
        NextPieceBox(game, labelWidth)
    }
}

@Composable
private fun BoxScope.GameOverBadge(game: BrickGame) {
    if (game.isGameOver) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(PixelDark)
                .border(2.dp, ScreenBase)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GAME OVER",
                color = ScreenBase,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun StatLabel(title: String, value: String, width: Dp) {
    Column(
        modifier = Modifier
            .width(width)
            .background(ScreenBase)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = PixelDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = value,
            color = PixelDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/** Preview of the upcoming piece, centered in its panel; reads [BrickGame.nextPiece] in the draw phase only. */
@Composable
private fun NextPieceBox(game: BrickGame, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(1.25f)
            .background(ScreenBase)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val blockSize = size.width / 4.5f
                    val blockPad = 1.dp.toPx()
                    val blockInnerPad = 2.dp.toPx()
                    val blockSide = blockSize - blockPad * 2
                    val blockOuterSize = Size(blockSide, blockSide)
                    val blockInnerSize =
                        Size(blockSide - blockInnerPad * 2, blockSide - blockInnerPad * 2)
                    val blockStroke = Stroke(width = 1.5f.dp.toPx())

                    onDrawBehind {
                        val matrix = game.nextPiece.matrix
                        val rCount = matrix.size
                        val cCount = matrix[0].size
                        val startX = (size.width - (cCount * blockSize)) / 2f
                        val startY = (size.height - (rCount * blockSize)) / 2f

                        for (r in 0 until rCount) {
                            for (c in 0 until cCount) {
                                if (matrix[r][c] == 1) {
                                    val topX = startX + c * blockSize + blockPad
                                    val topY = startY + r * blockSize + blockPad
                                    drawRect(
                                        color = PixelDark,
                                        topLeft = Offset(topX, topY),
                                        size = blockOuterSize,
                                        style = blockStroke
                                    )
                                    drawRect(
                                        color = PixelDark,
                                        topLeft = Offset(
                                            topX + blockInnerPad,
                                            topY + blockInnerPad
                                        ),
                                        size = blockInnerSize
                                    )
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun LogoSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            "Jetpack Compose",
            color = LogoBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "BRICK GAME",
            color = LogoBlue,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 19.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * The D-pad, modeled as a real rocker: the cross pivots on a hollow center dome, so a
 * press tilts the whole part ±10° around the opposite axis via `rotationX`/`rotationY`.
 * `cameraDistance = 8f` is deliberately tiny (default is ~1280px) — a close camera
 * exaggerates the perspective so even 10° reads clearly at 120dp. A radial shadow pool
 * under the pressed arm sells the contact point.
 *
 * Up = hard drop, down = soft drop, left/right = shift (with DAS auto-repeat).
 */
@Composable
private fun DPadRealistic(game: BrickGame) {
    val upPressed = remember { mutableStateOf(false) }
    val downPressed = remember { mutableStateOf(false) }
    val leftPressed = remember { mutableStateOf(false) }
    val rightPressed = remember { mutableStateOf(false) }

    // Animatable + snapshotFlow keeps press handling out of composition entirely:
    // targets retarget mid-flight exactly like animateFloatAsState, but nothing recomposes.
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            snapshotFlow {
                if (upPressed.value) 10f else if (downPressed.value) -10f else 0f
            }.collectLatest { rotX.animateTo(it, PressSpring) }
        }
        launch {
            snapshotFlow {
                if (leftPressed.value) -10f else if (rightPressed.value) 10f else 0f
            }.collectLatest { rotY.animateTo(it, PressSpring) }
        }
    }

    Box(modifier = Modifier.size(120.dp)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val recessRadius = size.width / 2 - 2.dp.toPx()
            drawCircle(
                color = CasingHighlight,
                radius = recessRadius,
                center = center.copy(x = center.x + 1.5f.dp.toPx(), y = center.y + 1.5f.dp.toPx())
            )
            drawCircle(
                color = CasingShadow,
                radius = recessRadius,
                center = center.copy(x = center.x - 1f.dp.toPx(), y = center.y - 1f.dp.toPx())
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CasingShadow.copy(alpha = 0.6f),
                        CasingBase,
                        CasingHighlight.copy(alpha = 0.4f)
                    ), start = Offset(0f, 0f), end = Offset(size.width, size.height)
                ), radius = recessRadius, center = center
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = rotX.value
                    rotationY = rotY.value
                    cameraDistance = 8f
                }
                .drawWithCache {
                    val center = Offset(size.width / 2, size.height / 2)
                    val recessRadius = size.width / 2 - 2.dp.toPx()
                    val crossLength = recessRadius * 1.45f
                    val crossWidth = crossLength * 0.35f
                    val startXY = (size.width - crossLength) / 2
                    val crossEnd = startXY + crossLength
                    val armStart = startXY + (crossLength - crossWidth) / 2
                    val armEnd = armStart + crossWidth

                    val dPadPath = Path().apply {
                        moveTo(armStart, startXY); lineTo(armEnd, startXY)
                        lineTo(armEnd, armStart); lineTo(crossEnd, armStart)
                        lineTo(crossEnd, armEnd); lineTo(armEnd, armEnd)
                        lineTo(armEnd, crossEnd); lineTo(armStart, crossEnd)
                        lineTo(armStart, armEnd); lineTo(startXY, armEnd)
                        lineTo(startXY, armStart); lineTo(armStart, armStart)
                        close()
                    }
                    val crossBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF333333), DPadBase, Color(0xFF0A0A0A)),
                        start = Offset(startXY, startXY),
                        end = Offset(crossEnd, crossEnd)
                    )

                    val edgeHighlightPath = Path().apply {
                        moveTo(armStart, startXY); lineTo(armEnd, startXY)
                        moveTo(startXY, armStart); lineTo(armStart, armStart)
                        lineTo(armStart, startXY)
                        moveTo(startXY, armStart); lineTo(startXY, armEnd)
                    }
                    val edgeStroke = Stroke(width = 2.dp.toPx())

                    val arrowHalfWidth = 4.dp.toPx()
                    val arrowRise = 4.dp.toPx()
                    val arrowDrop = 3.dp.toPx()
                    val arrowDist = recessRadius * 0.86f
                    val depthPx = 1.dp.toPx()

                    fun arrowPathAt(tip: Offset) = Path().apply {
                        moveTo(tip.x, tip.y - arrowRise)
                        lineTo(tip.x + arrowHalfWidth, tip.y + arrowDrop)
                        lineTo(tip.x - arrowHalfWidth, tip.y + arrowDrop)
                        close()
                    }

                    val arrows = listOf(
                        Offset(center.x, center.y - arrowDist) to 0f,
                        Offset(center.x, center.y + arrowDist) to 180f,
                        Offset(center.x - arrowDist, center.y) to -90f,
                        Offset(center.x + arrowDist, center.y) to 90f
                    ).map { (pivot, degrees) -> Triple(arrowPathAt(pivot), pivot, degrees) }

                    val hollowRadius = crossWidth * 0.45f
                    val hollowShadowBrush = Brush.linearGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        start = Offset(center.x - hollowRadius, center.y - hollowRadius),
                        end = Offset(center.x + hollowRadius, center.y + hollowRadius)
                    )
                    val hollowLightBrush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.25f)),
                        start = Offset(center.x - hollowRadius, center.y - hollowRadius),
                        end = Offset(center.x + hollowRadius, center.y + hollowRadius)
                    )

                    val gripWidth = crossWidth * 0.55f
                    val gripStartY = center.y - (crossLength / 2f) + 5.dp.toPx()
                    val gripSpacing = 4.dp.toPx()
                    val gripStrokePx = 1.5f.dp.toPx()
                    val gripRotations = floatArrayOf(0f, 180f, -90f, 90f)

                    val pressRadius = 18.dp.toPx()
                    val pressOffset = crossLength * 0.35f
                    val pressIndicators = listOf(
                        upPressed to center.copy(y = center.y - pressOffset),
                        downPressed to center.copy(y = center.y + pressOffset),
                        leftPressed to center.copy(x = center.x - pressOffset),
                        rightPressed to center.copy(x = center.x + pressOffset)
                    ).map { (state, pos) ->
                        Triple(
                            state, pos,
                            Brush.radialGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                                center = pos, radius = pressRadius
                            )
                        )
                    }

                    onDrawBehind {
                        drawPath(path = dPadPath, brush = crossBrush)
                        drawPath(
                            path = edgeHighlightPath,
                            color = Color(0xFF7A7A7A),
                            style = edgeStroke
                        )

                        // Carved arrows: dark pass up-left, light pass down-right — the
                        // inverse of the raised switch pill, so they read as engraved.
                        arrows.forEach { (path, pivot, degrees) ->
                            translate(left = -depthPx, top = -depthPx) {
                                rotate(degrees = degrees, pivot = pivot) {
                                    drawPath(path, color = Color.Black.copy(alpha = 0.5f))
                                }
                            }
                            translate(left = depthPx, top = depthPx) {
                                rotate(degrees = degrees, pivot = pivot) {
                                    drawPath(path, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            rotate(degrees = degrees, pivot = pivot) {
                                drawPath(path, color = CasingShadow)
                            }
                        }

                        drawCircle(color = DPadBase, radius = hollowRadius, center = center)
                        drawCircle(
                            brush = hollowShadowBrush,
                            radius = hollowRadius,
                            center = center
                        )
                        drawCircle(brush = hollowLightBrush, radius = hollowRadius, center = center)

                        gripRotations.forEach { degrees ->
                            rotate(degrees = degrees, pivot = center) {
                                for (i in 0..2) {
                                    val y = gripStartY + (i * gripSpacing)
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.9f),
                                        start = Offset(center.x - gripWidth / 2f, y),
                                        end = Offset(center.x + gripWidth / 2f, y),
                                        strokeWidth = gripStrokePx
                                    )
                                    drawLine(
                                        color = Color(0xFF555555),
                                        start = Offset(center.x - gripWidth / 2f, y + gripStrokePx),
                                        end = Offset(center.x + gripWidth / 2f, y + gripStrokePx),
                                        strokeWidth = gripStrokePx
                                    )
                                }
                            }
                        }

                        pressIndicators.forEach { (state, pos, brush) ->
                            if (state.value) drawCircle(
                                brush = brush,
                                radius = pressRadius,
                                center = pos
                            )
                        }
                    }
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(40.dp)
                .arcadeButton(upPressed, repeating = false) { game.hardDrop() })
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(40.dp)
                .arcadeButton(downPressed, repeating = true) { game.tick() })
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .arcadeButton(leftPressed, repeating = true) { game.moveLeft() })
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .arcadeButton(rightPressed, repeating = true) { game.moveRight() })
    }
}

/** The A/B cluster: both buttons sit on a shared recessed pill, tilted -22° like the original. */
@Composable
private fun ActionButtonsRealistic(game: BrickGame) {
    Box(
        modifier = Modifier
            .width(135.dp)
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(116.dp)
                .height(54.dp)
                .rotate(-22f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val corner = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(
                    color = CasingHighlight,
                    topLeft = Offset(1.5f.dp.toPx(), 1.5f.dp.toPx()),
                    size = size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = CasingShadow,
                    topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                    size = size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CasingShadow.copy(alpha = 0.6f),
                            CasingBase,
                            CasingHighlight.copy(alpha = 0.4f)
                        ), start = Offset(0f, 0f), end = Offset(size.width, size.height)
                    ), size = size, cornerRadius = corner
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealisticButton(text = "B", onClick = { game.rotate() })
                RealisticButton(text = "A", onClick = { game.rotate() })
            }
        }
    }
}

/**
 * A domed A/B button. The cap is shaded by a radial gradient whose center is pulled
 * up-left toward the light, layered with a rotated gloss oval for the specular streak.
 * Pressing translates the cap diagonally toward its *fixed* drop shadow — the shrinking
 * gap between cap and shadow is what reads as travel depth. The label is silk-screened
 * on the casing below the button and rides the same spring.
 */
@Composable
private fun RealisticButton(text: String, onClick: () -> Unit) {
    val isPressed = remember { mutableStateOf(false) }
    val pushAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { if (isPressed.value) 2.5f else 0f }
            .collectLatest { pushAnim.animateTo(it, PressSpring) }
    }

    Box(modifier = Modifier
        .size(44.dp)
        .arcadeButton(isPressed) { onClick() }) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val center = Offset(size.width / 2, size.height / 2)
                    val buttonRadius = size.width / 2 - 1.dp.toPx()
                    val shadowCenter =
                        center.copy(x = center.x + 3.dp.toPx(), y = center.y + 4.dp.toPx())
                    val actualButtonRadius = buttonRadius - 1.dp.toPx()

                    val faceBrush = Brush.radialGradient(
                        colors = listOf(
                            ButtonMagentaHighlight,
                            ButtonMagentaBase,
                            ButtonMagentaShadow
                        ),
                        center = center.copy(
                            x = center.x - actualButtonRadius * 0.3f,
                            y = center.y - actualButtonRadius * 0.3f
                        ),
                        radius = actualButtonRadius * 1.2f
                    )
                    val glossPivot = center.copy(
                        x = center.x - actualButtonRadius * 0.5f,
                        y = center.y - actualButtonRadius * 0.5f
                    )
                    val glossBrush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                        start = Offset(
                            center.x - actualButtonRadius * 0.8f,
                            center.y - actualButtonRadius * 0.8f
                        ),
                        end = Offset(center.x, center.y)
                    )
                    val glossTopLeft = Offset(
                        center.x - actualButtonRadius * 0.65f,
                        center.y - actualButtonRadius * 0.75f
                    )
                    val glossSize = Size(actualButtonRadius * 0.9f, actualButtonRadius * 0.4f)

                    onDrawBehind {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = buttonRadius,
                            center = shadowCenter
                        )
                        // Shift the cap instead of rebuilding push-dependent brushes each frame.
                        val push = pushAnim.value.dp.toPx()
                        translate(left = push, top = push) {
                            drawCircle(
                                color = Color(0xFF222222),
                                radius = buttonRadius,
                                center = center
                            )
                            drawCircle(
                                brush = faceBrush,
                                radius = actualButtonRadius,
                                center = center
                            )
                            rotate(degrees = -35f, pivot = glossPivot) {
                                drawOval(
                                    brush = glossBrush,
                                    topLeft = glossTopLeft,
                                    size = glossSize
                                )
                            }
                        }
                    }
                }
        )

        Text(
            text = text,
            color = LogoBlue,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    val push = pushAnim.value
                    IntOffset((6 + push).dp.roundToPx(), (42 + push).dp.roundToPx())
                }
        )
    }
}

@Composable
private fun SystemButtonsRealistic(game: BrickGame) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.rotate(-22f)) {
            SystemButton(text = "SELECT", onClick = { })
        }
        Box(modifier = Modifier
            .offset(y = 16.dp)
            .rotate(-22f)) {
            SystemButton(text = "START", onClick = { game.reset() })
        }
    }
}

/**
 * START/SELECT: a rubber pill sunk into a recessed well. The well darkens toward the
 * top (its wall blocks the light), and the cap's drop shadow is skipped entirely while
 * pressed — a cap sitting flush with the well floor can't cast one, and removing it is
 * what makes the 1.5dp travel feel like real depth.
 */
@Composable
private fun SystemButton(text: String, onClick: () -> Unit) {
    val isPressed = remember { mutableStateOf(false) }
    val pushAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { if (isPressed.value) 1.5f else 0f }
            .collectLatest { pushAnim.animateTo(it, PressSpring) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.arcadeButton(isPressed) { onClick() }) {
        Spacer(
            modifier = Modifier
                .width(50.dp)
                .height(14.dp)
                .drawWithCache {
                    val cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                    val recessOverlayBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF111111), Color.Transparent),
                        startY = 0f, endY = size.height * 0.7f
                    )

                    val paddingX = 2.dp.toPx()
                    val paddingTop = 1.dp.toPx()
                    val paddingBottom = 3.dp.toPx()
                    val buttonWidth = size.width - (paddingX * 2)
                    val buttonHeight = size.height - paddingTop - paddingBottom
                    val buttonSize = Size(buttonWidth, buttonHeight)
                    val buttonCorner = CornerRadius(buttonHeight / 2, buttonHeight / 2)
                    val restTopLeft = Offset(paddingX, paddingTop)
                    val shadowTopLeft = Offset(paddingX + 1.dp.toPx(), paddingTop + 2.dp.toPx())
                    val faceBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD4D4D8),
                            Color(0xFF989AA0),
                            Color(0xFF6A6B70)
                        ), startY = paddingTop, endY = paddingTop + buttonHeight
                    )

                    onDrawBehind {
                        drawRoundRect(
                            color = Color(0xFF4A4B50),
                            size = size,
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            brush = recessOverlayBrush,
                            size = size,
                            cornerRadius = cornerRadius
                        )

                        if (!isPressed.value) {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.7f),
                                topLeft = shadowTopLeft,
                                size = buttonSize,
                                cornerRadius = buttonCorner
                            )
                        }

                        val push = pushAnim.value.dp.toPx()
                        translate(left = push, top = push) {
                            drawRoundRect(
                                brush = faceBrush,
                                topLeft = restTopLeft,
                                size = buttonSize,
                                cornerRadius = buttonCorner
                            )
                        }
                    }
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = LogoBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * The six angled speaker slots. Each slot stacks a shadow copy (up-left), a highlight
 * copy (down-right), the slot face, then two darkening gradients — from the left wall
 * and the top lip — so the openings read as drilled through the casing at the
 * original's signature -25° tilt.
 */
@Composable
private fun SpeakerGrillRealistic() {
    Canvas(modifier = Modifier.size(width = 110.dp, height = 80.dp)) {
        val slotWidth = 8.5f.dp.toPx()
        val slotHeight = 52.dp.toPx()
        val spacing = 16.dp.toPx()
        val angle = -25f
        rotate(angle, pivot = Offset(size.width / 2, size.height / 2)) {
            for (i in 0..5) {
                val xOffset = i * spacing + 12.dp.toPx()
                val yOffset = 15.dp.toPx()
                val cornerRadius = CornerRadius(slotWidth / 2, slotWidth / 2)
                drawRoundRect(
                    color = CasingShadow.copy(alpha = 0.7f),
                    topLeft = Offset(xOffset - 1.dp.toPx(), yOffset - 1.dp.toPx()),
                    size = Size(slotWidth, slotHeight),
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = CasingHighlight,
                    topLeft = Offset(xOffset + 1.dp.toPx(), yOffset + 1.dp.toPx()),
                    size = Size(slotWidth, slotHeight),
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = Color(0xFF8A8A86),
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(slotWidth, slotHeight),
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(
                                alpha = 0.35f
                            ), Color.Transparent
                        ), startX = xOffset, endX = xOffset + slotWidth * 0.8f
                    ),
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(slotWidth, slotHeight),
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        ), startY = yOffset, endY = yOffset + slotHeight * 0.5f
                    ),
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(slotWidth, slotHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
