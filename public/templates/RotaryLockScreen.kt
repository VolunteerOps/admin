/*
 * Copyright 2026 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ──────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────

/** Digits arranged clockwise around the dial, from "1" through "0". */
private val Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

/** Angular spacing between adjacent digit holes (degrees). */
private const val DIGIT_SPACING_DEG = 29f

/** Base angle of the first digit ("1") on the dial (degrees). */
private const val FIRST_DIGIT_DEG = 345f

/** Angle of the stationary finger stop pin (degrees). */
private const val STOP_PIN_DEG = 42f

/** Sweep arc covering all ten digit holes (degrees). */
private const val WHEEL_SWEEP_DEG = 261f

/** Rotation interval between haptic ticks (degrees). Smaller values
 *  produce a denser click-clack feel during the sweep. */
private const val HAPTIC_TICK_DEG = 6f

/** Pre-computed PI / 180 as Float for degree-to-radian conversion. */
private const val DEG_TO_RAD = 0.017453292f

// ──────────────────────────────────────────────────────────
// Main composable
// ──────────────────────────────────────────────────────────

/**
 * A rotary phone-style passcode lock screen.
 *
 * The user drags a digit hole clockwise to the finger stop to enter that digit.
 * After release the dial springs back. When every digit is entered the passcode
 * is verified: a match fades the dial out and shows a success message, a mismatch
 * flashes the dots red and resets the input.
 *
 * Three mutable [Path] objects used for the rotating wheel are pre-allocated
 * via [remember] and reset each frame to reduce allocation pressure during
 * the spring-back animation.
 *
 * @param modifier Modifier applied to the root container.
 * @param passcode Target passcode. Its length determines the number of pin dots.
 */
@Composable
fun RotaryLockScreen(
    modifier: Modifier = Modifier,
    passcode: String = "1234"
) {
    val rotationAngle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val haptic = LocalHapticFeedback.current

    var enteredCode by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    // Pre-allocated mutable paths — reset each frame to avoid
    // per-frame allocation during the spring-back animation.
    val wedgePath = remember { Path() }
    val capsPath = remember { Path() }
    val holesPath = remember { Path() }

    val dialAlpha by animateFloatAsState(
        targetValue = if (isUnlocked) 0f else 1f,
        animationSpec = tween(800),
        label = "dialAlpha"
    )

    // Verify passcode once the entered code reaches the required length.
    LaunchedEffect(enteredCode) {
        if (enteredCode.length == passcode.length) {
            if (enteredCode == passcode) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isUnlocked = true
            } else {
                isError = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(600)
                enteredCode = ""
                isError = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        // Success overlay
        AnimatedVisibility(
            visible = isUnlocked,
            enter = fadeIn(tween(1000, delayMillis = 400)) + scaleIn(initialScale = 0.95f)
        ) {
            Text(
                text = "SYSTEM UNLOCKED",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    letterSpacing = 4.sp
                )
            )
        }

        // Dial layer — fades out on unlock
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(dialAlpha)
        ) {

            // Header and pin dots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 90.dp, start = 24.dp, end = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "ENTER\nPASSCODE",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 36.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF1A1A1A)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(passcode.length) { index ->
                            PinDot(
                                isFilled = index < enteredCode.length,
                                isError = isError
                            )
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isUnlocked, isError) {
                        if (isUnlocked || isError) return@pointerInput

                        // Pre-compute touch geometry once per pointer-input
                        // session. Radii are intentionally larger than the
                        // visual radii for comfortable touch targets.
                        val center = Offset(size.width / 2f, size.height * 0.62f)
                        val touchBaseRadius = size.width / 2f * 0.88f
                        val touchPitchRadius = touchBaseRadius * 0.68f
                        val touchHitSlop = touchBaseRadius * 0.14f * 1.8f

                        var isDraggingHole = false
                        var activeMaxRotation = 0f
                        var previousTouchAngle = 0f
                        var virtualRotation = 0f
                        var activeDigit = ""
                        var lastHapticTick = 0

                        detectDragGestures(
                            onDragStart = { offset ->
                                // Reset so a stale flag from the previous gesture
                                // cannot leak into a tap on empty space.
                                isDraggingHole = false

                                val currentRot = rotationAngle.value
                                for (i in Digits.indices) {
                                    val angle =
                                        (FIRST_DIGIT_DEG - i * DIGIT_SPACING_DEG + currentRot) * DEG_TO_RAD
                                    val cx = center.x + cos(angle) * touchPitchRadius
                                    val cy = center.y + sin(angle) * touchPitchRadius
                                    if (hypot(offset.x - cx, offset.y - cy) <= touchHitSlop) {
                                        isDraggingHole = true
                                        activeDigit = Digits[i]
                                        activeMaxRotation =
                                            (47.0f - 18f) + i * DIGIT_SPACING_DEG
                                        virtualRotation = currentRot
                                        lastHapticTick = 0
                                        previousTouchAngle = Math.toDegrees(
                                            atan2(
                                                (offset.y - center.y).toDouble(),
                                                (offset.x - center.x).toDouble()
                                            )
                                        ).toFloat()
                                        scope.launch { rotationAngle.stop() }
                                        break
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isDraggingHole) {
                                    if (rotationAngle.value >= activeMaxRotation - 20f &&
                                        enteredCode.length < passcode.length
                                    ) {
                                        enteredCode += activeDigit
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    scope.launch {
                                        rotationAngle.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = 0.60f,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isDraggingHole) {
                                    scope.launch { rotationAngle.animateTo(0f) }
                                }
                            }
                        ) { change, _ ->
                            if (!isDraggingHole) return@detectDragGestures

                            val currentTouchAngle = Math.toDegrees(
                                atan2(
                                    (change.position.y - center.y).toDouble(),
                                    (change.position.x - center.x).toDouble()
                                )
                            ).toFloat()

                            var delta = currentTouchAngle - previousTouchAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            virtualRotation = (virtualRotation + delta).coerceAtLeast(0f)
                            val nextRot = virtualRotation.coerceAtMost(activeMaxRotation)

                            val currentTick = (nextRot / HAPTIC_TICK_DEG).toInt()
                            if (currentTick != lastHapticTick && nextRot < activeMaxRotation) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticTick = currentTick
                            }
                            if (virtualRotation >= activeMaxRotation &&
                                rotationAngle.value < activeMaxRotation
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            scope.launch { rotationAngle.snapTo(nextRot) }
                            previousTouchAngle = currentTouchAngle
                        }
                    }
            ) {
                val centerOffset = Offset(size.width / 2f, size.height * 0.62f)
                val dialRadius = size.width / 2f * 0.83f
                val centerCutoutRadius = dialRadius * 0.47f
                val holePitchRadius = dialRadius * 0.735f
                val capRadius = dialRadius * 0.265f
                val holeRadius = dialRadius * 0.16f
                val thickStrokeWidth = 4.dp.toPx()

                val currentRot = rotationAngle.value

                // LAYER 1: Black donut track
                val outerRect = Rect(
                    centerOffset - Offset(dialRadius, dialRadius),
                    Size(dialRadius * 2, dialRadius * 2)
                )
                val innerRect = Rect(
                    centerOffset - Offset(centerCutoutRadius, centerCutoutRadius),
                    Size(centerCutoutRadius * 2, centerCutoutRadius * 2)
                )
                val outerCircle = Path().apply { addOval(outerRect) }
                val innerCircle = Path().apply { addOval(innerRect) }
                val baseDonutPath = Path.combine(
                    PathOperation.Difference, outerCircle, innerCircle
                )

                drawPath(baseDonutPath, color = Color(0xFF1A1A1A))

                // LAYER 2: White digit labels (stationary)
                // TextMeasurer caches results internally so repeated calls
                // with identical parameters return instantly.
                for (i in Digits.indices) {
                    val staticAngle =
                        (FIRST_DIGIT_DEG - i * DIGIT_SPACING_DEG) * DEG_TO_RAD
                    val scx = centerOffset.x + cos(staticAngle) * holePitchRadius
                    val scy = centerOffset.y + sin(staticAngle) * holePitchRadius

                    val textLayout = textMeasurer.measure(
                        text = Digits[i],
                        style = TextStyle(
                            fontSize = (holeRadius * 1.05f).toSp(),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        color = Color(0xFFFFFFFF),
                        topLeft = Offset(
                            scx - textLayout.size.width / 2f,
                            scy - textLayout.size.height / 2f
                        )
                    )
                }

                // LAYER 3: White rotating wheel with punched holes
                val angleOfDigit1 = FIRST_DIGIT_DEG + currentRot
                val angleOfDigit0 =
                    (FIRST_DIGIT_DEG - 9 * DIGIT_SPACING_DEG) + currentRot

                wedgePath.reset()
                wedgePath.moveTo(centerOffset.x, centerOffset.y)
                wedgePath.arcTo(
                    rect = Rect(
                        centerOffset - Offset(dialRadius * 2f, dialRadius * 2f),
                        Size(dialRadius * 4f, dialRadius * 4f)
                    ),
                    startAngleDegrees = angleOfDigit0,
                    sweepAngleDegrees = WHEEL_SWEEP_DEG,
                    forceMoveTo = false
                )
                wedgePath.close()

                val boundedWedge = Path.combine(
                    PathOperation.Intersect, baseDonutPath, wedgePath
                )

                capsPath.reset()
                val cx0 =
                    centerOffset.x + cos(angleOfDigit0 * (PI / 180f)).toFloat() * holePitchRadius
                val cy0 =
                    centerOffset.y + sin(angleOfDigit0 * (PI / 180f)).toFloat() * holePitchRadius
                capsPath.addOval(
                    Rect(
                        Offset(cx0, cy0) - Offset(capRadius, capRadius),
                        Size(capRadius * 2, capRadius * 2)
                    )
                )

                val cx1 =
                    centerOffset.x + cos(angleOfDigit1 * (PI / 180f)).toFloat() * holePitchRadius
                val cy1 =
                    centerOffset.y + sin(angleOfDigit1 * (PI / 180f)).toFloat() * holePitchRadius
                capsPath.addOval(
                    Rect(
                        Offset(cx1, cy1) - Offset(capRadius, capRadius),
                        Size(capRadius * 2, capRadius * 2)
                    )
                )

                val solidWhitePiece = Path.combine(
                    PathOperation.Union, boundedWedge, capsPath
                )

                holesPath.reset()
                for (i in Digits.indices) {
                    val angle =
                        (FIRST_DIGIT_DEG - i * DIGIT_SPACING_DEG + currentRot) * DEG_TO_RAD
                    val cx = centerOffset.x + cos(angle) * holePitchRadius
                    val cy = centerOffset.y + sin(angle) * holePitchRadius
                    holesPath.addOval(
                        Rect(
                            Offset(cx, cy) - Offset(holeRadius, holeRadius),
                            Size(holeRadius * 2, holeRadius * 2)
                        )
                    )
                }

                val finalWhitePieceWithHoles = Path.combine(
                    PathOperation.Difference, solidWhitePiece, holesPath
                )

                drawPath(finalWhitePieceWithHoles, color = Color(0xFFFFFFFF))
                drawPath(
                    finalWhitePieceWithHoles,
                    color = Color(0xFF1A1A1A),
                    style = Stroke(width = thickStrokeWidth, join = StrokeJoin.Round)
                )

                // LAYER 4: White finger stop pin (stationary)
                val stopAccentAngle = STOP_PIN_DEG * DEG_TO_RAD
                val stopCx = centerOffset.x + cos(stopAccentAngle) * holePitchRadius
                val stopCy = centerOffset.y + sin(stopAccentAngle) * holePitchRadius

                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = holeRadius * 0.80f,
                    center = Offset(stopCx, stopCy)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Pin dot indicator
// ──────────────────────────────────────────────────────────

/**
 * Animated circular dot for the passcode display row.
 *
 * Transitions between hollow (unfilled), coral (filled), and red (error)
 * with a 150 ms color tween.
 *
 * @param isFilled Whether this dot represents an already-entered digit.
 * @param isError Whether the passcode entry is in error state.
 */
@Composable
fun PinDot(isFilled: Boolean, isError: Boolean) {
    val dotColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFE53935)
            isFilled -> Color(0xFFE95A5A)
            else -> Color(0xFFFFFFFF)
        },
        animationSpec = tween(150),
        label = "dotColor"
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(dotColor)
            .border(4.dp, Color(0xFF1A1A1A), CircleShape)
    )
}

// ──────────────────────────────────────────────────────────
// Preview
// ──────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun RotaryLockScreenPreview() {
    RotaryLockScreen(passcode = "1234")
}