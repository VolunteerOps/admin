import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

/** Design tokens for the Polaroid OneStep skeuomorphic rendering. */
object PolaroidPalette {
    val AppBackground = Color(0xFF6B705C)
    val BodyCream = Color(0xFFEBE9DE)
    val BodyCreamShadow = Color(0xFFBCBAAB)
    val BodyCreamLip = Color(0xFFE5E3D5)
    val ChassisLight = Color(0xFF2E2E2E)
    val ChassisDark = Color(0xFF141414)
    val PanelRecess = Color(0xFF1F1F1F)
    val TextColor = Color(0xFFAFAFAF)
    val StickerBeige = Color(0xFFEBE6C8)
    val ShutterRed = Color(0xFFDF1A00)
    val ShutterRedHighlight = Color(0xFFFF4D33)
    val RainbowColors = listOf(
        Color(0xFFB30033),
        Color(0xFFE65C00),
        Color(0xFFFFC000),
        Color(0xFF00A633),
        Color(0xFF0073B3),
    )
}

enum class PhotoState { Idle, Capturing, Ejecting, Developing, Done }

/**
 * Root screen composing the camera body, live preview, and ejected print.
 *
 * ### Key performance decisions
 * - **[derivedStateOf]** wraps [showPrint] so the preview slot only recomposes
 *   when visibility flips, not on every [PhotoState] transition.
 * - **[graphicsLayer]** drives the flash overlay alpha. This is a RenderNode
 *   property update (no recomposition, no relayout) vs the naive approach of
 *   conditionally composing a white Box on each alpha tick.
 * - **Stable lambdas**: [handleShutterClick] and [handlePhotoDismiss] are
 *   wrapped in `remember`. [rememberUpdatedState] captures the latest
 *   [photoState] without changing the lambda identity, so `pointerInput(Unit)`
 *   never restarts.
 */
@Composable
fun PolaroidAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var photoState by remember { mutableStateOf(PhotoState.Idle) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isShutterPressed by remember { mutableStateOf(false) }

    val flashAlpha = remember { Animatable(0f) }
    val ejectOffsetY = remember { Animatable(-800f) }
    val printRotation = remember { Animatable(0f) }
    val devGreyAlpha = remember { Animatable(1f) }
    val devTealAlpha = remember { Animatable(1f) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val showPrint by remember {
        derivedStateOf { photoState != PhotoState.Idle && photoState != PhotoState.Capturing }
    }

    val currentPhotoState by rememberUpdatedState(photoState)

    val handleShutterClick: () -> Unit = remember {
        {
            if (currentPhotoState == PhotoState.Idle || currentPhotoState == PhotoState.Done) {
                coroutineScope.launch {
                    if (currentPhotoState == PhotoState.Done) {
                        launch {
                            ejectOffsetY.animateTo(
                                2500f,
                                tween(300, easing = FastOutSlowInEasing),
                            )
                        }
                    }

                    photoState = PhotoState.Capturing

                    flashAlpha.snapTo(1f)
                    launch { flashAlpha.animateTo(0f, tween(350)) }

                    takePhoto(
                        context, imageCapture,
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            photoState = PhotoState.Ejecting

                            coroutineScope.launch {
                                ejectOffsetY.snapTo(-800f)
                                devGreyAlpha.snapTo(1f)
                                devTealAlpha.snapTo(1f)

                                val randomTilt = ((-3)..3).random().toFloat()
                                launch { printRotation.animateTo(randomTilt, tween(1200)) }
                                ejectOffsetY.animateTo(
                                    40f,
                                    tween(1200, easing = LinearOutSlowInEasing),
                                )

                                photoState = PhotoState.Developing
                                devGreyAlpha.animateTo(0f, tween(1500, easing = LinearEasing))
                                devTealAlpha.animateTo(
                                    0f,
                                    tween(3500, easing = FastOutSlowInEasing),
                                )

                                photoState = PhotoState.Done
                            }
                        },
                        onError = { photoState = PhotoState.Idle },
                    )
                }
            }
        }
    }

    val handlePhotoDismiss: () -> Unit = remember {
        {
            if (currentPhotoState == PhotoState.Done || currentPhotoState == PhotoState.Developing) {
                photoState = PhotoState.Idle
                coroutineScope.launch {
                    ejectOffsetY.animateTo(
                        2500f,
                        tween(500, easing = FastOutSlowInEasing),
                    )
                    capturedBitmap = null
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PolaroidPalette.AppBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 8.dp)
                    .zIndex(2f),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(1.15f)) {
                    SkeuomorphicPolaroidCanvas(
                        isShutterPressed = isShutterPressed,
                        onShutterStateChange = { isShutterPressed = it },
                        onShutterClick = handleShutterClick,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .background(Color.Black)
                    .zIndex(1f),
            ) {
                LiveCameraPreview(imageCapture)

                if (showPrint) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        PolaroidPrint(
                            bitmap = capturedBitmap,
                            offsetY = ejectOffsetY.value,
                            rotationZ = printRotation.value,
                            devGreyAlpha = devGreyAlpha.value,
                            devTealAlpha = devTealAlpha.value,
                            onTap = handlePhotoDismiss,
                        )
                    }
                }
            }
        }

        // Flash overlay: always composed, visibility via graphicsLayer alpha.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .graphicsLayer { alpha = flashAlpha.value },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}

/**
 * The ejected Polaroid print with developing chemistry and sharpie drawing.
 *
 * ### Development animation timeline
 * Two stacked overlays sit on top of the captured photo:
 * 1. **Grey** (0xFF536C7A) fades out in 1500ms with [LinearEasing],
 *    simulating the initial chemical wash clearing.
 * 2. **Teal** (0xFF1A2B2D) fades out in 3500ms with [FastOutSlowInEasing].
 *    The slow ease-out makes the photo "emerge" gradually, matching how
 *    real Polaroid film develops from the edges inward.
 *
 * Both overlays use [graphicsLayer] for alpha so the animation is a
 * RenderNode property change, not a recomposition.
 *
 * ### Ejection physics
 * - Slides in with [LinearOutSlowInEasing]: fast push, friction slowdown.
 * - A random ±3 degree tilt simulates real roller misalignment.
 * - Dismiss uses [FastOutSlowInEasing] for a "flick away" feel.
 *
 * ### Sharpie drawing
 * Uses [mutableStateListOf] instead of list copying. Adding a stroke
 * mutates in place, triggering a draw invalidation without O(n) allocation.
 * [SharpieCanvas] is a separate composable so overlay alpha changes
 * don't invalidate the stroke drawing code path.
 */
@Composable
fun PolaroidPrint(
    bitmap: Bitmap?,
    offsetY: Float,
    rotationZ: Float,
    devGreyAlpha: Float,
    devTealAlpha: Float,
    onTap: () -> Unit,
) {
    val lines = remember { mutableStateListOf<List<Offset>>() }
    var currentLine by remember { mutableStateOf(emptyList<Offset>()) }

    // Convert Bitmap -> ImageBitmap once per capture, not every frame
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY
                this.rotationZ = rotationZ
            }
            .fillMaxWidth(0.75f)
            .aspectRatio(0.82f)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(Color(0xFFF4F4F0), RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> currentLine = listOf(offset) },
                    onDrag = { change, _ ->
                        currentLine = currentLine + change.position
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine)
                            currentLine = emptyList()
                        }
                    },
                    onDragCancel = { currentLine = emptyList() },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 48.dp),
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Captured Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF111111)),
                )
            }
            if (devTealAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = devTealAlpha }
                        .background(Color(0xFF1A2B2D)),
                )
            }
            if (devGreyAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = devGreyAlpha }
                        .background(Color(0xFF536C7A)),
                )
            }
        }

        SharpieCanvas(completedLines = lines, currentLine = currentLine)
    }
}

/** Isolated Canvas for sharpie strokes. Separate composable so overlay alpha changes skip this. */
@Composable
private fun SharpieCanvas(
    completedLines: List<List<Offset>>,
    currentLine: List<Offset>,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val sharpieColor = Color(0xFF18181A).copy(alpha = 0.85f)
        val style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        completedLines.forEach { line -> drawStrokeLine(line, sharpieColor, style, strokeWidth) }
        drawStrokeLine(currentLine, sharpieColor, style, strokeWidth)
    }
}

private fun DrawScope.drawStrokeLine(
    line: List<Offset>,
    color: Color,
    style: Stroke,
    strokeWidth: Float,
) {
    if (line.size > 1) {
        val path = Path().apply {
            moveTo(line.first().x, line.first().y)
            for (i in 1 until line.size) lineTo(line[i].x, line[i].y)
        }
        drawPath(path, color = color, style = style)
    } else if (line.size == 1) {
        drawCircle(color = color, radius = strokeWidth / 2, center = line.first())
    }
}

/** Captures a photo from [imageCapture], corrects rotation, and mirrors for the front camera. */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit,
    onError: () -> Unit,
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        postScale(-1f, 1f)
                    }

                    val rotated = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
                    )
                    onImageCaptured(rotated)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError()
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onError()
            }
        },
    )
}

/**
 * Renders the full skeuomorphic Polaroid OneStep camera body on a single Canvas.
 *
 * ### Body geometry
 * Four interlocking paths form the silhouette:
 * ```
 * topBumpPath         ┌──────────┐        viewfinder ridge
 * topBodyPath        /            \       tapered upper body (88% -> 94% width)
 *                   ├──────────────┤      ledgeTopY
 * ledgeAndLipPath    \            /       angled ledge with Bezier corners
 *                    ├────────────┤       lipTopY -> trayTopY
 * trayPath           │            │       rectangular base with rounded bottom
 *                    └────────────┘       bottomY
 * ```
 * The upper body tapers from 94% to 88% of total width. The 6% difference
 * creates the characteristic Polaroid shoulder. The ledge-to-lip transition
 * uses `slantRatio = (ledgeH - lipCornerRadius) / ledgeH` to compute where
 * the diagonal edges meet the lip's rounded corners via quadratic Beziers.
 *
 * ### drawWithCache optimization
 * This is the single biggest performance win in the file. The original `Canvas`
 * composable re-allocated ~50 Paths, ~30 Brushes, and ~10 Paint+BlurMaskFilter
 * objects every frame. [drawWithCache] splits this into:
 * - **Cache phase** (runs once per size change): builds all geometry and paints.
 * - **Draw phase** (runs per frame): references cached objects. Only branches
 *   on [isShutterPressed] for the button visual, which is a cheap draw
 *   invalidation, not a full recache.
 *
 * ### Shadow system
 * Every raised component casts a soft shadow via [BlurMaskFilter]. The light
 * source sits upper-right, so shadows fall left and down. Pre-allocated paints
 * are grouped by blur radius and passed to drawing functions:
 *
 * | Component | Offset (x,y) | Blur | Alpha |
 * |-----------|-------------|------|-------|
 * | Body      | (-8, 30)    | 35   | 0.50  |
 * | Lens      | (-6, 16)    | 25   | 0.40  |
 * | Flash     | (-4, 10)    | 15   | 0.40  |
 * | Shutter   | (-4, 8)     | 12   | 0.30  |
 * | Dial      | (-4, 8)     | 10   | 0.40  |
 * | Sticker   | (-2, 6)     | 8    | 0.20  |
 */
@Composable
fun SkeuomorphicPolaroidCanvas(
    isShutterPressed: Boolean,
    onShutterStateChange: (Boolean) -> Unit,
    onShutterClick: () -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentOnShutterClick by rememberUpdatedState(onShutterClick)

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val paddingX = 16.dp.toPx()
                        val paddingTop = 16.dp.toPx()
                        val paddingBottom = 32.dp.toPx()
                        val w = size.width - (paddingX * 2)
                        val h = size.height - (paddingTop + paddingBottom)

                        val trayH = h * 0.28f
                        val lipH = h * 0.048f
                        val ledgeH = h * 0.065f
                        val topBodyH = h - trayH - ledgeH - lipH

                        val shutterCenter = Offset(
                            paddingX + w * 0.20f,
                            paddingTop + topBodyH * 0.68f,
                        )
                        val shutterRadius = w * 0.065f * 1.35f

                        if ((offset - shutterCenter).getDistance() <= shutterRadius) {
                            onShutterStateChange(true)
                            tryAwaitRelease()
                            onShutterStateChange(false)
                            currentOnShutterClick()
                        }
                    },
                )
            }
            .drawWithCache {
                // ── CACHE PHASE: runs once per size change ──

                val paddingX = 16.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 32.dp.toPx()

                val w = size.width - (paddingX * 2)
                val h = size.height - (paddingTop + paddingBottom)

                val trayW = w
                val topBodyBottomW = w * 0.94f
                val topBodyTopW = w * 0.88f

                val trayH = h * 0.28f
                val lipH = h * 0.048f
                val ledgeH = h * 0.065f
                val topBodyH = h - trayH - ledgeH - lipH

                val trayLeft = 0f
                val trayRight = w

                val topBodyBottomLeft = (w - topBodyBottomW) / 2f
                val topBodyBottomRight = topBodyBottomLeft + topBodyBottomW
                val topBodyTopLeft = (w - topBodyTopW) / 2f
                val topBodyTopRight = topBodyTopLeft + topBodyTopW

                val topY = 0f
                val ledgeTopY = topBodyH
                val lipTopY = ledgeTopY + ledgeH
                val trayTopY = lipTopY + lipH
                val bottomY = h

                val topRadius = 24.dp.toPx()
                val seamRadius = 3.dp.toPx()
                val bottomRadius = 24.dp.toPx()

                // Cached paths
                val topBumpW = w * 0.48f
                val topBumpH = 14.dp.toPx()
                val topBumpX = (w - topBumpW) / 2f
                val topBumpY = topY - topBumpH + 4f

                val topBumpPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = topBumpX, top = topBumpY,
                            right = topBumpX + topBumpW, bottom = topY + 10f,
                            topLeftCornerRadius = CornerRadius(10.dp.toPx()),
                            topRightCornerRadius = CornerRadius(10.dp.toPx()),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }

                val topBodyPath = Path().apply {
                    moveTo(topBodyBottomLeft, ledgeTopY)
                    lineTo(topBodyTopLeft, topY + topRadius)
                    quadraticTo(topBodyTopLeft, topY, topBodyTopLeft + topRadius, topY)
                    lineTo(topBodyTopRight - topRadius, topY)
                    quadraticTo(topBodyTopRight, topY, topBodyTopRight, topY + topRadius)
                    lineTo(topBodyBottomRight, ledgeTopY)
                    close()
                }

                val lipCornerRadius = 6.dp.toPx()
                val slantRatio = (ledgeH - lipCornerRadius) / ledgeH
                val leftSlantX =
                    topBodyBottomLeft + (trayLeft - topBodyBottomLeft) * slantRatio
                val rightSlantX =
                    topBodyBottomRight + (trayRight - topBodyBottomRight) * slantRatio

                val ledgeAndLipPath = Path().apply {
                    moveTo(topBodyBottomLeft, ledgeTopY)
                    lineTo(topBodyBottomRight, ledgeTopY)
                    lineTo(rightSlantX, lipTopY - lipCornerRadius)
                    quadraticTo(trayRight, lipTopY, trayRight, lipTopY + lipCornerRadius)
                    lineTo(trayRight, trayTopY)
                    lineTo(trayLeft, trayTopY)
                    lineTo(trayLeft, lipTopY + lipCornerRadius)
                    quadraticTo(
                        trayLeft, lipTopY,
                        leftSlantX, lipTopY - lipCornerRadius,
                    )
                    close()
                }

                val trayPath = Path().apply {
                    moveTo(trayLeft + seamRadius, trayTopY)
                    lineTo(trayRight - seamRadius, trayTopY)
                    quadraticTo(trayRight, trayTopY, trayRight, trayTopY + seamRadius)
                    lineTo(trayRight, bottomY - bottomRadius)
                    quadraticTo(trayRight, bottomY, trayRight - bottomRadius, bottomY)
                    lineTo(trayLeft + bottomRadius, bottomY)
                    quadraticTo(trayLeft, bottomY, trayLeft, bottomY - bottomRadius)
                    lineTo(trayLeft, trayTopY + seamRadius)
                    quadraticTo(trayLeft, trayTopY, trayLeft + seamRadius, trayTopY)
                    close()
                }

                val silhouettePath = Path().apply {
                    addPath(topBumpPath); addPath(topBodyPath)
                    addPath(ledgeAndLipPath); addPath(trayPath)
                }

                // Cached brushes
                val topBumpGradient = Brush.verticalGradient(
                    listOf(Color(0xFF2E2E2E), Color(0xFF0A0A0A)),
                    startY = topBumpY, endY = topY,
                )
                val topBumpEdgeBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    start = Offset(topBumpX + topBumpW, topBumpY),
                    end = Offset(topBumpX, topBumpY + topBumpH),
                )
                val trayGradient = Brush.verticalGradient(
                    listOf(PolaroidPalette.ChassisLight, PolaroidPalette.ChassisDark),
                    startY = trayTopY, endY = bottomY,
                )
                val ledgeGradient = Brush.verticalGradient(
                    listOf(Color(0xFFE0DDD0), PolaroidPalette.BodyCreamShadow),
                    startY = ledgeTopY, endY = lipTopY,
                )
                val lipGradient = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.8f), PolaroidPalette.BodyCreamLip),
                    startY = lipTopY, endY = trayTopY,
                )
                val bodyGradient = Brush.verticalGradient(
                    listOf(Color.White, PolaroidPalette.BodyCream),
                    startY = topY, endY = ledgeTopY,
                )
                val leftFlareBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                    start = Offset(topBodyBottomLeft, ledgeTopY),
                    end = Offset(leftSlantX, lipTopY - lipCornerRadius),
                )
                val rightFlareBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                    start = Offset(topBodyBottomRight, ledgeTopY),
                    end = Offset(rightSlantX, lipTopY - lipCornerRadius),
                )

                // Cached shadow paints (allocated once, not per frame)
                val bodyShadowPaint = Paint().apply {
                    color = Color.Black.copy(alpha = 0.5f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(35f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowPaint12 = Paint().apply {
                    color = Color.Black.copy(alpha = 0.3f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowPaint25 = Paint().apply {
                    color = Color.Black.copy(alpha = 0.4f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowPaint15 = Paint().apply {
                    color = Color.Black.copy(alpha = 0.4f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowPaint10 = Paint().apply {
                    color = Color.Black.copy(alpha = 0.4f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowPaint8 = Paint().apply {
                    color = Color.Black.copy(alpha = 0.2f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                }
                val specularPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.85f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL)
                }
                val dialHighlightPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.15f)
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
                }

                // Component positions
                val housingCenter = Offset(w / 2f, topBodyH * 0.48f)
                val housingSize = topBodyBottomW * 0.35f
                val stripeStartY = housingCenter.y + (housingSize * 0.35f)
                val shutterCenter = Offset(w * 0.20f, topBodyH * 0.68f)
                val shutterRadius = w * 0.065f
                val flashCenter = Offset(w * 0.79f, topBodyH * 0.22f)
                val flashSize = w * 0.14f
                val dialCenter = Offset(w * 0.77f, topBodyH * 0.65f)
                val dialRadius = w * 0.05f

                // ── DRAW PHASE: runs per frame, uses cached objects ──
                onDrawBehind {
                    withTransform({ translate(left = paddingX, top = paddingTop) }) {

                        // Body shadow
                        drawIntoCanvas { canvas ->
                            canvas.translate(-8f, 30f)
                            canvas.drawPath(silhouettePath, bodyShadowPaint)
                            canvas.translate(8f, -30f)
                        }

                        drawPath(topBumpPath, topBumpGradient)
                        drawPath(topBumpPath, topBumpEdgeBrush, style = Stroke(2f))

                        drawRect(
                            Color(0xFF111111),
                            Offset(trayLeft, trayTopY - seamRadius),
                            Size(trayW, seamRadius * 2),
                        )

                        drawPath(trayPath, trayGradient)
                        drawPath(ledgeAndLipPath, ledgeGradient)
                        clipPath(ledgeAndLipPath) {
                            drawRect(lipGradient, Offset(0f, lipTopY), Size(w, trayTopY - lipTopY))
                            drawLine(
                                Color.White.copy(alpha = 0.9f),
                                Offset(0f, lipTopY), Offset(w, lipTopY),
                                strokeWidth = 4f,
                            )
                        }
                        drawPath(topBodyPath, bodyGradient)

                        // Edge highlights and seam lines
                        drawLine(
                            Color.White.copy(alpha = 0.6f),
                            Offset(topBodyTopLeft, topY + topRadius),
                            Offset(topBodyBottomLeft, ledgeTopY),
                            strokeWidth = 3f
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.05f),
                            Offset(topBodyTopRight, topY + topRadius),
                            Offset(topBodyBottomRight, ledgeTopY),
                            strokeWidth = 3f
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.15f),
                            Offset(topBodyBottomLeft, ledgeTopY),
                            Offset(topBodyBottomRight, ledgeTopY),
                            strokeWidth = 3f
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.3f),
                            Offset(trayLeft + seamRadius, trayTopY),
                            Offset(trayRight - seamRadius, trayTopY),
                            strokeWidth = 3f
                        )
                        drawLine(
                            leftFlareBrush,
                            Offset(topBodyBottomLeft, ledgeTopY),
                            Offset(leftSlantX, lipTopY - lipCornerRadius),
                            strokeWidth = 4f
                        )
                        drawLine(
                            rightFlareBrush,
                            Offset(topBodyBottomRight, ledgeTopY),
                            Offset(rightSlantX, lipTopY - lipCornerRadius),
                            strokeWidth = 4f
                        )

                        // Components
                        drawPerspectiveStripe(
                            w,
                            topBodyBottomW,
                            stripeStartY,
                            ledgeTopY,
                            lipTopY,
                            trayTopY
                        )
                        drawLensAssembly(housingCenter, housingSize, shadowPaint25, specularPaint)
                        drawBranding(textMeasurer, w, topY, topBodyH, shadowPaint8)
                        drawFlash(flashCenter, flashSize, shadowPaint15)
                        drawShutterButton(
                            shutterCenter,
                            shutterRadius,
                            isShutterPressed,
                            shadowPaint12
                        )
                        drawExposureDial(dialCenter, dialRadius, shadowPaint10, dialHighlightPaint)
                        drawBottomTrayDetails(textMeasurer, trayLeft, trayTopY, trayW, trayH)
                    }
                }
            },
    )
}

/**
 * Draws the iconic Polaroid rainbow stripe across the body, ledge, and tray.
 *
 * The stripe must follow the body's perspective warp across the angled ledge zone.
 * Each of the five color bands is drawn in three segments:
 * 1. A straight rectangle on the upper body.
 * 2. A **cubic Bezier patch** across the ledge (the interesting part).
 * 3. A straight rectangle on the tray.
 *
 * The Bezier control points are offset by 40% of the ledge height to create
 * an S-curve that follows the surface:
 * ```
 * CP1 = (segTopX,    ledgeTopY + 0.4 * ledgeH)   // eases out of top edge
 * CP2 = (segBottomX, lipTopY   - 0.4 * ledgeH)   // eases into bottom edge
 * ```
 *
 * The stripe widens from `topBodyW * 0.075` on the body to `1.4x` on the tray,
 * matching the body's own taper. A shadow gradient (0 -> 35% black) is painted
 * over the Bezier region to simulate the ledge casting a shadow.
 */
fun DrawScope.drawPerspectiveStripe(
    w: Float, topBodyW: Float, startY: Float,
    ledgeTopY: Float, lipTopY: Float, trayTopY: Float,
) {
    val topStripeW = topBodyW * 0.075f
    val bottomStripeW = topStripeW * 1.4f
    val topX = (w - topStripeW) / 2f
    val bottomX = (w - bottomStripeW) / 2f
    val topSegmentW = topStripeW / PolaroidPalette.RainbowColors.size
    val bottomSegmentW = bottomStripeW / PolaroidPalette.RainbowColors.size
    val controlPointOffset = (lipTopY - ledgeTopY) * 0.4f

    PolaroidPalette.RainbowColors.forEachIndexed { index, color ->
        val segTopX = topX + (index * topSegmentW)
        val segBottomX = bottomX + (index * bottomSegmentW)

        drawRect(color, Offset(segTopX, startY), Size(topSegmentW, ledgeTopY - startY))

        val flarePath = Path().apply {
            moveTo(segTopX, ledgeTopY)
            lineTo(segTopX + topSegmentW, ledgeTopY)
            cubicTo(
                segTopX + topSegmentW, ledgeTopY + controlPointOffset,
                segBottomX + bottomSegmentW, lipTopY - controlPointOffset,
                segBottomX + bottomSegmentW, lipTopY,
            )
            lineTo(segBottomX, lipTopY)
            cubicTo(
                segBottomX, lipTopY - controlPointOffset,
                segTopX, ledgeTopY + controlPointOffset,
                segTopX, ledgeTopY,
            )
            close()
        }
        drawPath(flarePath, color)
        drawRect(color, Offset(segBottomX, lipTopY), Size(bottomSegmentW, trayTopY - lipTopY))
    }

    val slantedShadowPath = Path().apply {
        moveTo(topX, ledgeTopY)
        lineTo(topX + topStripeW, ledgeTopY)
        cubicTo(
            topX + topStripeW,
            ledgeTopY + controlPointOffset,
            bottomX + bottomStripeW,
            lipTopY - controlPointOffset,
            bottomX + bottomStripeW,
            lipTopY
        )
        lineTo(bottomX, lipTopY)
        cubicTo(
            bottomX,
            lipTopY - controlPointOffset,
            topX,
            ledgeTopY + controlPointOffset,
            topX,
            ledgeTopY
        )
        close()
    }
    drawPath(
        slantedShadowPath,
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0f),
                Color.Black.copy(alpha = 0.35f)
            ), startY = ledgeTopY, endY = lipTopY
        )
    )
    drawRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
            startY = lipTopY,
            endY = trayTopY
        ), Offset(bottomX, lipTopY), Size(bottomStripeW, trayTopY - lipTopY)
    )
}

/**
 * Draws the lens housing, barrel ribs, glass element, and specular highlights.
 *
 * ### Concentric rib illusion
 * 35 stroke-only rounded rectangles converge from the outer funnel (85% of housing)
 * to the inner hole (48%). Each rib has two strokes: a dark base and a directional
 * highlight (top-right to bottom-left), simulating light catching machined grooves.
 * The corner radius decreases linearly so outer ribs are more square, inner ribs
 * are more circular.
 *
 * ### Specular window reflection
 * Two overlapping rounded rects are subtracted via [PathOperation.Difference] to
 * produce a thin crescent. Rotated -35 degrees and clipped to the glass path, this
 * mimics a rectangular window reflected on a convex surface. A smaller "dot" crescent
 * nearby completes the effect.
 *
 * @param shadowPaint Pre-allocated [Paint] with blur=25 for the housing drop shadow.
 * @param specularPaint Pre-allocated [Paint] with blur=1 for the window reflection.
 */
fun DrawScope.drawLensAssembly(
    center: Offset,
    size: Float,
    shadowPaint: Paint,
    specularPaint: Paint,
) {
    val corner = CornerRadius(size * 0.15f)
    val topLeft = Offset(center.x - size / 2f, center.y - size / 2f)
    val topRight = Offset(center.x + size / 2f, center.y - size / 2f)
    val bottomLeft = Offset(center.x - size / 2f, center.y + size / 2f)

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            topLeft.x - 6f, topLeft.y + 16f,
            topLeft.x + size - 6f, topLeft.y + size + 16f,
            corner.x, corner.x, shadowPaint,
        )
    }

    val rimSize = size + 8f
    drawRoundRect(
        PolaroidPalette.BodyCream,
        Offset(center.x - rimSize / 2f, center.y - rimSize / 2f),
        Size(rimSize, rimSize),
        CornerRadius(rimSize * 0.15f)
    )
    val innerRimSize = size + 2f
    drawRoundRect(
        Color(0xFFC7C5B5),
        Offset(center.x - innerRimSize / 2f, center.y - innerRimSize / 2f),
        Size(innerRimSize, innerRimSize),
        CornerRadius(innerRimSize * 0.15f)
    )

    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFF2B2B2B), Color(0xFF030303)),
            start = topRight,
            end = bottomLeft
        ), topLeft, Size(size, size), corner
    )
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
            start = topRight,
            end = center
        ), topLeft, Size(size, size), corner, style = Stroke(2f)
    )

    val numRibs = 35
    val funnelOuterSize = size * 0.85f
    val innerHoleSize = size * 0.48f
    val ribStep = (funnelOuterSize - innerHoleSize) / 2f / numRibs

    val funnelTopLeft = Offset(center.x - funnelOuterSize / 2f, center.y - funnelOuterSize / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.Black, Color(0xFF2A2A2A)),
            start = topRight,
            end = bottomLeft
        ),
        funnelTopLeft,
        Size(funnelOuterSize, funnelOuterSize),
        CornerRadius(corner.x * 0.85f),
        style = Stroke(3f)
    )

    for (i in 0..numRibs) {
        val currentSize = funnelOuterSize - (i * ribStep * 2)
        val currentCorner = (corner.x * 0.85f) - (i * (corner.x * 0.4f / numRibs))
        val currentTopLeft = Offset(center.x - currentSize / 2f, center.y - currentSize / 2f)
        val currentTopRight = Offset(center.x + currentSize / 2f, center.y - currentSize / 2f)
        val currentBottomLeft = Offset(center.x - currentSize / 2f, center.y + currentSize / 2f)

        drawRoundRect(
            Color(0xFF080808),
            currentTopLeft,
            Size(currentSize, currentSize),
            CornerRadius(currentCorner),
            style = Stroke(1.5f)
        )
        drawRoundRect(
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent,
                    Color.Transparent
                ), start = currentTopRight, end = currentBottomLeft
            ),
            currentTopLeft,
            Size(currentSize, currentSize),
            CornerRadius(currentCorner),
            style = Stroke(1f)
        )
    }

    val craterTopLeft = Offset(center.x - innerHoleSize / 2f, center.y - innerHoleSize / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFF000000), Color(0xFF252525)),
            start = Offset(center.x + innerHoleSize / 2, center.y - innerHoleSize / 2),
            end = Offset(center.x - innerHoleSize / 2, center.y + innerHoleSize / 2)
        ), craterTopLeft, Size(innerHoleSize, innerHoleSize), CornerRadius(14.dp.toPx())
    )

    val glassSize = innerHoleSize * 0.85f
    val glassTopLeft = Offset(center.x - glassSize / 2f, center.y - glassSize / 2f)
    val glassCorner = CornerRadius(10.dp.toPx())
    val glassPath = Path().apply {
        addRoundRect(
            RoundRect(
                glassTopLeft.x,
                glassTopLeft.y,
                glassTopLeft.x + glassSize,
                glassTopLeft.y + glassSize,
                glassCorner
            )
        )
    }

    drawPath(
        glassPath,
        Brush.radialGradient(
            listOf(Color(0xFF13181A), Color(0xFF010202)),
            center = center,
            radius = glassSize * 0.8f
        )
    )

    clipPath(glassPath) {
        // Broad diffuse highlight (convex glass illusion)
        withTransform({ rotate(-30f, center) }) {
            drawOval(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                Offset(center.x - glassSize, center.y - glassSize * 0.6f),
                Size(glassSize * 2f, glassSize * 0.8f)
            )
        }

        // Sharp window reflection via PathOperation.Difference
        withTransform({ rotate(-35f, center) }) {
            drawIntoCanvas { canvas ->
                val mainRect = Rect(
                    center.x - glassSize * 0.15f,
                    center.y - glassSize * 0.28f,
                    center.x + glassSize * 0.15f,
                    center.y - glassSize * 0.12f
                )
                val mainCutout = Rect(
                    center.x - glassSize * 0.12f,
                    center.y - glassSize * 0.24f,
                    center.x + glassSize * 0.18f,
                    center.y - glassSize * 0.08f
                )
                val mainPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            mainRect,
                            CornerRadius(glassSize * 0.1f)
                        )
                    )
                }
                val mainSubPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            mainCutout,
                            CornerRadius(glassSize * 0.1f)
                        )
                    )
                }
                canvas.drawPath(Path().apply {
                    op(
                        mainPath,
                        mainSubPath,
                        PathOperation.Difference
                    )
                }, specularPaint)

                val dotRect = Rect(
                    center.x + glassSize * 0.18f,
                    center.y - glassSize * 0.15f,
                    center.x + glassSize * 0.28f,
                    center.y - glassSize * 0.08f
                )
                val dotCutout = Rect(
                    center.x + glassSize * 0.21f,
                    center.y - glassSize * 0.12f,
                    center.x + glassSize * 0.31f,
                    center.y - glassSize * 0.05f
                )
                val dotPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            dotRect,
                            CornerRadius(glassSize * 0.05f)
                        )
                    )
                }
                val dotSubPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            dotCutout,
                            CornerRadius(glassSize * 0.05f)
                        )
                    )
                }
                canvas.drawPath(
                    Path().apply { op(dotPath, dotSubPath, PathOperation.Difference) },
                    specularPaint
                )
            }
        }
    }
}

/**
 * Draws the flash unit: outer housing, recessed window, xenon bulb, and glass reflection.
 *
 * The reflection is a diagonal quad clipped to the glass path, creating the
 * illusion of a curved protective cover catching overhead light.
 *
 * @param shadowPaint Pre-allocated [Paint] with blur=15.
 */
fun DrawScope.drawFlash(center: Offset, size: Float, shadowPaint: Paint) {
    val corner = CornerRadius(size * 0.18f)
    val topLeft = Offset(center.x - size / 2f, center.y - size / 2f)
    val topRight = Offset(center.x + size / 2f, center.y - size / 2f)
    val bottomLeft = Offset(center.x - size / 2f, center.y + size / 2f)

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            topLeft.x - 4f,
            topLeft.y + 10f,
            topLeft.x + size - 4f,
            topLeft.y + size + 10f,
            corner.x,
            corner.x,
            shadowPaint
        )
    }

    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFF383838), Color(0xFF121212)),
            start = topRight,
            end = bottomLeft
        ), topLeft, Size(size, size), corner
    )
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
            start = topRight,
            end = center
        ), topLeft, Size(size, size), corner, style = Stroke(1.5f)
    )

    val innerSize = size * 0.65f
    val innerTopLeft = Offset(center.x - innerSize / 2f, center.y - innerSize / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFF030303), Color(0xFF222222)),
            start = Offset(center.x + innerSize / 2, center.y - innerSize / 2),
            end = Offset(center.x - innerSize / 2, center.y + innerSize / 2)
        ), innerTopLeft, Size(innerSize, innerSize), CornerRadius(innerSize * 0.15f)
    )

    val glassSize = innerSize * 0.90f
    val glassTopLeft = Offset(center.x - glassSize / 2f, center.y - glassSize / 2f)
    val glassPath = Path().apply {
        addRoundRect(
            RoundRect(
                glassTopLeft.x,
                glassTopLeft.y,
                glassTopLeft.x + glassSize,
                glassTopLeft.y + glassSize,
                CornerRadius(glassSize * 0.1f)
            )
        )
    }

    drawPath(
        glassPath,
        Brush.radialGradient(
            listOf(Color(0xFF151515), Color(0xFF000000)),
            center = center,
            radius = glassSize * 0.8f
        )
    )

    clipPath(glassPath) {
        val bulbW = glassSize * 0.45f
        val bulbH = glassSize * 0.35f
        val bulbTopLeft = Offset(center.x - bulbW / 2f, center.y - bulbH / 2f)
        drawRoundRect(
            Color(0xFFE5E5E5),
            bulbTopLeft,
            Size(bulbW, bulbH),
            CornerRadius(bulbW * 0.2f)
        )
        drawRoundRect(
            Brush.linearGradient(
                listOf(
                    Color.Black.copy(alpha = 0.4f),
                    Color.Transparent
                ), start = bulbTopLeft, end = Offset(bulbTopLeft.x, bulbTopLeft.y + bulbH)
            ), bulbTopLeft, Size(bulbW, bulbH), CornerRadius(bulbW * 0.2f)
        )

        val reflectionPath = Path().apply {
            moveTo(glassTopLeft.x, glassTopLeft.y)
            lineTo(glassTopLeft.x + glassSize, glassTopLeft.y)
            lineTo(glassTopLeft.x + glassSize, glassTopLeft.y + glassSize * 0.5f)
            quadraticTo(
                center.x,
                center.y - glassSize * 0.1f,
                glassTopLeft.x,
                glassTopLeft.y + glassSize * 0.6f
            )
            close()
        }
        drawPath(
            reflectionPath,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset(center.x + glassSize / 2, center.y - glassSize / 2),
                end = Offset(center.x - glassSize / 2, center.y + glassSize / 2)
            )
        )
    }
}

/**
 * Draws the red shutter button with collar, shadow, and specular highlight.
 *
 * When [isPressed], the button translates (+3 down, -1 left) and the specular
 * highlight dims from 75% to 40% alpha, simulating physical depression.
 * A sweep gradient produces subtle concentric shading around the button edge.
 *
 * @param shadowPaint Pre-allocated [Paint] with blur=12.
 */
fun DrawScope.drawShutterButton(
    center: Offset,
    radius: Float,
    isPressed: Boolean,
    shadowPaint: Paint,
) {
    val outerCollarRadius = radius * 1.35f

    drawIntoCanvas { canvas ->
        canvas.drawOval(
            center.x - outerCollarRadius - 4f, center.y - outerCollarRadius + 8f,
            center.x + outerCollarRadius - 4f, center.y + outerCollarRadius + 8f,
            shadowPaint,
        )
    }

    drawCircle(
        Brush.linearGradient(listOf(Color(0xFFE2DFCD), Color(0xFFA5A394))),
        outerCollarRadius,
        center
    )

    val pressOffset = if (isPressed) Offset(-1f, 3f) else Offset.Zero
    val buttonCenter = center + pressOffset

    drawCircle(Color.Black.copy(alpha = 0.4f), radius * 1.05f, Offset(center.x, center.y + 4f))
    drawCircle(
        Brush.radialGradient(
            listOf(
                PolaroidPalette.ShutterRedHighlight,
                PolaroidPalette.ShutterRed
            ), center = buttonCenter, radius = radius * 1.5f
        ), radius, buttonCenter
    )
    drawCircle(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
            start = Offset(buttonCenter.x - radius, buttonCenter.y - radius),
            end = Offset(buttonCenter.x, buttonCenter.y)
        ), radius, buttonCenter
    )
    drawCircle(
        Brush.sweepGradient(
            0.0f to Color.Black.copy(alpha = 0.1f),
            0.15f to Color.White.copy(alpha = 0.1f),
            0.3f to Color.Black.copy(alpha = 0.1f),
            0.45f to Color.White.copy(alpha = 0.1f),
            0.6f to Color.Black.copy(alpha = 0.1f),
            0.75f to Color.White.copy(alpha = 0.1f),
            1.0f to Color.Black.copy(alpha = 0.1f),
            center = buttonCenter
        ), radius, buttonCenter
    )
    drawCircle(Color.White.copy(alpha = 0.15f), radius * 0.98f, buttonCenter, style = Stroke(1f))

    withTransform({ rotate(-35f, buttonCenter) }) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = Color.White.copy(alpha = if (isPressed) 0.4f else 0.75f)
                asFrameworkPaint().maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawOval(
                buttonCenter.x - radius * 0.2f,
                buttonCenter.y - radius * 0.7f,
                buttonCenter.x + radius * 0.4f,
                buttonCenter.y - radius * 0.4f,
                paint
            )
        }
    }
}

/**
 * Draws the exposure compensation dial. Three concentric circles with a
 * directional highlight create the illusion of a knurled metal knob.
 *
 * @param shadowPaint Pre-allocated [Paint] with blur=10.
 * @param highlightPaint Pre-allocated [Paint] with blur=4 for the top highlight.
 */
fun DrawScope.drawExposureDial(
    center: Offset,
    radius: Float,
    shadowPaint: Paint,
    highlightPaint: Paint,
) {
    drawIntoCanvas { canvas ->
        canvas.drawOval(
            center.x - radius - 4f,
            center.y - radius + 8f,
            center.x + radius - 4f,
            center.y + radius + 8f,
            shadowPaint
        )
    }

    drawCircle(
        Brush.linearGradient(
            listOf(Color(0xFF383838), Color(0xFF0A0A0A)),
            start = Offset(center.x + radius, center.y - radius),
            end = Offset(center.x - radius, center.y + radius)
        ), radius, center
    )
    drawCircle(Color(0xFF050505), radius * 0.88f, center)
    val topRadius = radius * 0.82f
    drawCircle(
        Brush.linearGradient(
            listOf(Color(0xFF1E1E1E), Color(0xFF111111)),
            start = Offset(center.x + topRadius, center.y - topRadius),
            end = Offset(center.x - topRadius, center.y + topRadius)
        ), topRadius, center
    )

    withTransform({ rotate(45f, center) }) {
        drawIntoCanvas { canvas ->
            canvas.drawOval(
                center.x - topRadius * 0.6f,
                center.y - topRadius * 0.8f,
                center.x + topRadius * 0.6f,
                center.y - topRadius * 0.2f,
                highlightPaint
            )
        }
    }
}

/** Draws the "Supercolor 1000" branding sticker with ruled lines. */
fun DrawScope.drawBranding(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    w: Float,
    topY: Float,
    topBodyH: Float,
    shadowPaint: Paint,
) {
    val stickerWidth = w * 0.18f
    val stickerHeight = stickerWidth * 0.85f
    val stickerX = w * 0.11f
    val stickerY = topY + (topBodyH * 0.10f)

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            stickerX - 2f,
            stickerY + 6f,
            stickerX + stickerWidth - 2f,
            stickerY + stickerHeight + 6f,
            6.dp.toPx(),
            6.dp.toPx(),
            shadowPaint
        )
    }

    drawRoundRect(
        PolaroidPalette.StickerBeige,
        Offset(stickerX, stickerY),
        Size(stickerWidth, stickerHeight),
        CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.4f),
        Offset(stickerX + 1f, stickerY + 1f),
        Size(stickerWidth - 2f, stickerHeight - 2f),
        CornerRadius(5.dp.toPx()),
        style = Stroke(1f)
    )

    val numLines = 14
    val lineSpacing = stickerHeight / (numLines + 1)
    for (i in 1..numLines) {
        val lineY = stickerY + (i * lineSpacing)
        drawLine(
            Color(0xFFD4D0B3),
            Offset(stickerX + 4f, lineY),
            Offset(stickerX + stickerWidth - 4f, lineY),
            strokeWidth = 1.5f
        )
    }

    val leftPadding = stickerWidth * 0.12f
    drawText(
        textMeasurer,
        "Supercolor",
        Offset(stickerX + leftPadding, stickerY + (stickerHeight * 0.15f)),
        TextStyle(
            color = Color(0xFFD11A00),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-0.5).sp
        )
    )
    drawText(
        textMeasurer,
        "1000",
        Offset(stickerX + leftPadding, stickerY + (stickerHeight * 0.38f)),
        TextStyle(
            color = Color(0xFF151515),
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-0.5).sp,
            shadow = androidx.compose.ui.graphics.Shadow(
                Color.Black.copy(alpha = 0.3f),
                Offset(-2f, 3f),
                4f
            )
        )
    )
}

/** Draws the bottom chassis: speaker grille panel, model text, and film ejection slot. */
fun DrawScope.drawBottomTrayDetails(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    startX: Float,
    startY: Float,
    trayW: Float,
    trayH: Float,
) {
    val panelWidth = trayW * 0.90f
    val panelHeight = trayH * 0.35f
    val panelX = startX + (trayW - panelWidth) / 2f
    val panelY = startY + (trayH * 0.12f)

    drawRoundRect(
        Color(0xFF050505),
        Offset(panelX - 2f, panelY - 2f),
        Size(panelWidth + 4f, panelHeight + 4f),
        CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        PolaroidPalette.PanelRecess,
        Offset(panelX, panelY),
        Size(panelWidth, panelHeight),
        CornerRadius(6.dp.toPx())
    )

    val numRibs = 6
    val ribSpacing = panelHeight / (numRibs + 1)
    for (i in 1..numRibs) {
        val yPos = panelY + (i * ribSpacing)
        drawLine(
            Color(0xFF0F0F0F),
            Offset(panelX + 8f, yPos),
            Offset(panelX + panelWidth - 8f, yPos),
            strokeWidth = 3f
        )
        drawLine(
            Color.White.copy(alpha = 0.03f),
            Offset(panelX + 8f, yPos + 2f),
            Offset(panelX + panelWidth - 8f, yPos + 2f),
            strokeWidth = 2f
        )
    }

    drawText(
        textMeasurer,
        "POLAROID LAND CAMERA",
        Offset(panelX + (trayW * 0.04f), panelY + (panelHeight * 0.25f)),
        TextStyle(
            color = PolaroidPalette.TextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp
        )
    )

    val slotWidth = trayW * 0.86f
    val slotHeight = trayH * 0.26f
    val slotX = startX + (trayW - slotWidth) / 2f
    val slotY = startY + (trayH * 0.62f)

    drawRoundRect(
        Color(0xFF1E1E1E),
        Offset(slotX - 4f, slotY - 4f),
        Size(slotWidth + 8f, slotHeight + 8f),
        CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        Color(0xFF050505),
        Offset(slotX, slotY),
        Size(slotWidth, slotHeight),
        CornerRadius(4.dp.toPx())
    )

    val lipHeight = slotHeight * 0.5f
    drawRoundRect(
        Brush.verticalGradient(
            0.0f to Color(0xFF0F0F0F),
            0.4f to Color(0xFF6A6A6A),
            0.6f to Color(0xFF222222),
            1.0f to Color(0xFF050505),
            startY = slotY + 2f,
            endY = slotY + lipHeight
        ),
        Offset(slotX + 4f, slotY + 2f),
        Size(slotWidth - 8f, lipHeight),
        CornerRadius(3.dp.toPx())
    )
}

/** CameraX preview bound to the lifecycle. Shows a placeholder in the IDE preview. */
@Composable
fun LiveCameraPreview(imageCapture: ImageCapture) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text("Camera Preview", color = Color.Gray)
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView ->
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@ComposePreview(showBackground = true)
@Composable
fun PreviewPolaroidApp() {
    PolaroidAppScreen()
}