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

import android.graphics.Bitmap
import android.graphics.RectF
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath

/*
 * Van Gogh, June 1889 — the cypress half of the canvas.
 *
 * Nothing here is a traced bitmap. The painting is rebuilt every launch out of
 * some five thousand brush strokes laid along a flow field, and that field is the
 * reason it reads as his hand rather than as swirls: it was measured off the
 * reference with a structure tensor, so every stroke runs the way his ran — the
 * vortex circulates, the halos orbit their stars, the cypress climbs, the hills
 * lie flat.
 *
 * What is borrowed from the reference is a study, not an image: a 20x36 grid of
 * brush directions, a 14x25 grid holding the dark and light end of each patch's
 * tonal range, one silhouette for the cypress, and eight star positions. Some
 * three hundred numbers in all. Everything else — every stroke's position,
 * length, width, colour and motion — is generated from them at launch.
 *
 * And it moves. Not a still with sparks drifting over it: the finished canvas is
 * drawn through a mesh whose every vertex slides along the brush direction at
 * that point, so the sky breathes, the great spiral turns, the halos orbit their
 * stars and the cypress gutters like the flame it is. The same field carries the
 * strokes that travel over the top, so nothing slides against anything else.
 *
 * Drag and the paint moves under your finger; tap and a ripple crosses the
 * canvas while the night blooms.
 */

// The reference is 800x1422; every coordinate in this file is a pixel in it.
private const val ART_W = 800f
private const val ART_H = 1422f

private val PI_F = PI.toFloat()
private const val TAU = 2.0 * PI

// ---------------------------------------------------------------------------
// The study
// ---------------------------------------------------------------------------

/**
 * Brush direction, one value per 40x40 patch of the canvas, in 256ths of a turn.
 *
 * Taken from the structure tensor of the reference: the minor eigenvector of the
 * smoothed gradient covariance is the direction along which the paint does not
 * change, which is the direction the bristles went. The tensor only yields an
 * orientation — a line, not an arrow — so the 180-degree ambiguity was resolved
 * globally by spin relaxation over the grid, leaving a field that actually
 * circulates instead of one that flips sign every other cell.
 */
private const val FLOW_W = 20
private const val FLOW_H = 36
private val FLOW = intArrayOf(
    20, 30, 254, 254, 230, 34, 8, 7, 17, 228, 21, 28, 18, 8, 4, 242, 240, 237, 235, 239,
    62, 60, 255, 27, 24, 8, 7, 4, 19, 47, 252, 54, 15, 4, 245, 242, 239, 231, 236, 239,
    223, 221, 248, 246, 4, 0, 4, 2, 20, 18, 255, 249, 25, 252, 242, 243, 244, 239, 236, 237,
    235, 251, 249, 204, 255, 4, 4, 1, 5, 5, 254, 2, 13, 254, 245, 248, 243, 244, 243, 244,
    251, 3, 8, 200, 246, 7, 20, 5, 255, 255, 254, 252, 1, 4, 254, 252, 253, 248, 250, 255,
    8, 7, 237, 202, 233, 14, 39, 6, 251, 246, 240, 243, 252, 3, 255, 2, 6, 9, 18, 6,
    17, 5, 249, 192, 47, 255, 193, 209, 240, 234, 235, 233, 243, 250, 255, 4, 8, 16, 14, 14,
    8, 0, 202, 54, 15, 250, 230, 229, 227, 230, 235, 239, 245, 246, 4, 5, 9, 14, 17, 19,
    13, 1, 24, 54, 254, 246, 240, 233, 229, 233, 240, 244, 244, 249, 1, 6, 19, 20, 27, 31,
    10, 4, 249, 51, 245, 243, 234, 230, 229, 229, 233, 234, 236, 244, 6, 16, 25, 32, 36, 40,
    3, 251, 244, 62, 51, 234, 228, 227, 231, 249, 228, 226, 228, 233, 0, 24, 35, 41, 48, 52,
    3, 65, 80, 71, 60, 237, 229, 228, 208, 200, 195, 217, 202, 219, 96, 45, 52, 57, 59, 58,
    2, 60, 63, 64, 65, 234, 229, 227, 200, 236, 214, 207, 198, 71, 71, 51, 53, 55, 81, 70,
    112, 58, 64, 63, 64, 234, 224, 228, 231, 233, 223, 194, 58, 61, 57, 50, 41, 37, 7, 89,
    249, 66, 62, 59, 62, 227, 230, 229, 227, 229, 223, 56, 47, 48, 44, 42, 37, 28, 14, 5,
    251, 50, 61, 61, 59, 218, 231, 229, 228, 233, 237, 48, 47, 43, 43, 37, 28, 23, 15, 4,
    252, 52, 61, 61, 62, 70, 232, 227, 229, 244, 3, 20, 31, 36, 26, 17, 13, 14, 5, 248,
    57, 61, 59, 62, 61, 81, 232, 218, 218, 231, 2, 23, 34, 39, 22, 21, 14, 11, 3, 245,
    5, 79, 60, 63, 59, 69, 238, 209, 204, 205, 255, 24, 51, 40, 11, 10, 9, 3, 249, 245,
    9, 70, 58, 60, 58, 61, 114, 219, 187, 45, 38, 196, 192, 207, 8, 7, 7, 252, 242, 237,
    20, 59, 51, 55, 60, 61, 111, 110, 43, 26, 255, 231, 219, 226, 2, 252, 255, 254, 254, 235,
    29, 61, 60, 64, 67, 64, 94, 100, 42, 14, 254, 239, 236, 244, 247, 246, 249, 249, 250, 236,
    30, 57, 68, 66, 68, 71, 67, 75, 24, 251, 250, 241, 241, 241, 243, 246, 243, 238, 242, 237,
    7, 55, 58, 65, 66, 68, 65, 64, 41, 3, 242, 242, 239, 247, 245, 251, 250, 238, 239, 236,
    6, 48, 58, 63, 62, 62, 63, 55, 44, 30, 247, 243, 237, 240, 251, 255, 251, 241, 242, 239,
    2, 47, 64, 64, 59, 51, 58, 49, 48, 38, 5, 243, 236, 239, 0, 15, 6, 244, 239, 233,
    123, 63, 67, 60, 52, 53, 54, 48, 47, 46, 33, 246, 245, 4, 10, 9, 252, 248, 244, 240,
    117, 82, 67, 54, 50, 32, 58, 54, 50, 54, 48, 44, 4, 12, 20, 34, 225, 237, 240, 242,
    98, 82, 81, 48, 53, 43, 54, 57, 52, 56, 63, 59, 7, 250, 252, 1, 0, 252, 241, 238,
    73, 75, 61, 42, 57, 70, 52, 52, 65, 58, 67, 72, 239, 251, 2, 251, 6, 245, 43, 35,
    62, 63, 53, 51, 64, 71, 62, 59, 75, 63, 77, 80, 55, 255, 250, 225, 33, 75, 60, 51,
    55, 51, 45, 52, 68, 82, 80, 80, 84, 84, 88, 64, 49, 54, 224, 253, 31, 69, 52, 52,
    48, 36, 25, 41, 73, 81, 220, 201, 206, 82, 71, 54, 50, 36, 35, 231, 236, 230, 254, 2,
    247, 32, 26, 36, 62, 205, 207, 192, 194, 198, 59, 30, 31, 42, 48, 246, 251, 255, 252, 255,
    221, 27, 34, 43, 61, 192, 194, 184, 179, 182, 43, 37, 42, 54, 61, 74, 255, 1, 1, 1,
    66, 36, 69, 56, 61, 177, 179, 184, 181, 183, 57, 52, 58, 78, 74, 0, 2, 1, 0, 3,
)

private const val TONE_W = 14
private const val TONE_H = 25

/**
 * The tonal range of each 57x57 patch, as RGB565: the mean of the darkest sixth
 * of its pixels and of the brightest sixth, measured off the reference with the
 * village walls masked out — left in, the brightest thing in the bottom third
 * drags a quarter of the grid grey and the sky around the houses paints as fog.
 *
 * Two registers rather than one average, because a stroke is never the mean. The
 * grid carries only the broad tone at this size; all of the contrast comes from
 * which end of its cell's range a given stroke draws from.
 */
private val TONE_LOW = intArrayOf(
    0x00AB, 0x0000, 0x0008, 0x0009, 0x0005, 0x000C, 0x0151, 0x1ACE, 0x0030, 0x002D,
    0x000B, 0x000C, 0x000B, 0x000D, 0x0AD1, 0x0011, 0x0003, 0x008B, 0x0008, 0x000B,
    0x01ED, 0x11E9, 0x008F, 0x1169, 0x000E, 0x000C, 0x000B, 0x0032, 0x0013, 0x0055,
    0x0000, 0x0052, 0x0031, 0x000D, 0x0054, 0x0032, 0x0012, 0x0034, 0x0093, 0x000D,
    0x000B, 0x000F, 0x00B6, 0x006F, 0x0000, 0x00CD, 0x0031, 0x0074, 0x00D2, 0x00B2,
    0x00F1, 0x0130, 0x006D, 0x000E, 0x0010, 0x0011, 0x0154, 0x002E, 0x0000, 0x7B80,
    0x018F, 0x00B4, 0x0094, 0x0195, 0x02D5, 0x12D4, 0x0232, 0x1AD3, 0x01F1, 0x0250,
    0x00B3, 0x004B, 0x0000, 0x0135, 0x0074, 0x0152, 0x02B4, 0x12F1, 0x2B0F, 0x01F2,
    0x0110, 0x00ED, 0x0174, 0x0196, 0x000B, 0x0092, 0x0000, 0x0194, 0x0B75, 0x0336,
    0x0334, 0x0B11, 0x0007, 0x006C, 0x0152, 0x0194, 0x000F, 0x0255, 0x0B14, 0x01EC,
    0x0000, 0x0128, 0x1310, 0x02D2, 0x1B4F, 0x000D, 0x0004, 0x01F3, 0x01D3, 0x00F3,
    0x01F3, 0x0172, 0x12F4, 0x0800, 0x0000, 0x0127, 0x0292, 0x0006, 0x2269, 0x00F1,
    0x0152, 0x01B0, 0x0254, 0x0173, 0x0113, 0x01B5, 0x3C58, 0x0000, 0x0000, 0x0003,
    0x0006, 0x0007, 0x0008, 0x0110, 0x0234, 0x0AD4, 0x0152, 0x0174, 0x0154, 0x0296,
    0x0315, 0x0000, 0x0000, 0x0001, 0x0000, 0x0003, 0x00EE, 0x0216, 0x02D4, 0x0234,
    0x0232, 0x008E, 0x006E, 0x0173, 0x5C28, 0x0000, 0x0000, 0x0000, 0x0004, 0x0234,
    0x0255, 0x02F2, 0x0315, 0x02F4, 0x02F3, 0x01F2, 0x02F5, 0x01D3, 0x4C2D, 0x0000,
    0x0000, 0x0000, 0x00AA, 0x02AF, 0x7CD0, 0x84EF, 0x6493, 0x0213, 0x002C, 0x01D4,
    0x0151, 0x00D3, 0x0215, 0x0000, 0x0000, 0x0000, 0x00EC, 0x122D, 0x8CCF, 0xAC80,
    0x6490, 0x022F, 0x0151, 0x002A, 0x0173, 0x01D3, 0x01F6, 0x0000, 0x0000, 0x0000,
    0x0004, 0x0025, 0x6451, 0x7CD3, 0x2B72, 0x0253, 0x0275, 0x02D1, 0x02F2, 0x0272,
    0x031B, 0x0000, 0x0000, 0x0000, 0x0000, 0x0066, 0x0213, 0x02B5, 0x1331, 0x4C0E,
    0x234F, 0x0230, 0x0A6E, 0x126E, 0x0399, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
    0x2BF3, 0x4453, 0x0373, 0x01F2, 0x01F2, 0x02B3, 0x01F3, 0x000C, 0x0236, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0068, 0x012E, 0x00EE, 0x0006, 0x000B,
    0x004D, 0x000B, 0x0072, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
    0x0003, 0x0004, 0x000B, 0x000A, 0x004D, 0x0111, 0x0000, 0x0000, 0x0000, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0003, 0x0027, 0x0023, 0x0025,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
    0x004C, 0x00AB, 0x0028, 0x0002, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0001, 0x0000, 0x0000, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0001,
    0x0001, 0x0000, 0x0002, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0046, 0x0023, 0x0000, 0x0000,
    0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
)


private val TONE_HIGH = intArrayOf(
    0xC7B9, 0x7DBE, 0x3B37, 0xA73D, 0x43BA, 0x5CFF, 0xB754, 0xB716, 0x9EBC, 0x5D5E,
    0xAEDD, 0xEFFC, 0x969F, 0x5D3E, 0xC758, 0x6D3D, 0x43B8, 0x7DFC, 0x5D9F, 0x6D9E,
    0xC73A, 0xBF17, 0xCF7B, 0xD72F, 0x96DD, 0x7E1F, 0x4CFF, 0x541C, 0xBF5F, 0x547C,
    0x4A6D, 0x239D, 0x345F, 0x559F, 0x557F, 0x33BE, 0x5D3C, 0x8DF3, 0x3BFC, 0x55DF,
    0x551F, 0x7D5D, 0x445C, 0x443C, 0x7CB0, 0xD731, 0xBE97, 0x3C3E, 0x2C3E, 0x4C9B,
    0x44DE, 0x347E, 0x759D, 0xA63D, 0x6CFA, 0x4C9C, 0x4D1E, 0x3C7C, 0xCF56, 0xEF0A,
    0xEFB8, 0x44DE, 0x553C, 0x5D7D, 0x7DFB, 0xDF36, 0xD75D, 0xD75F, 0xC71F, 0xB71F,
    0x24FF, 0x241E, 0x3D1F, 0xA678, 0xB73A, 0x867F, 0x963A, 0x7E1B, 0xB6B9, 0x9E78,
    0xAEBB, 0x9E9C, 0xCF5E, 0xA6BD, 0x8E9E, 0x763D, 0x961C, 0xB71E, 0x9E9F, 0xC6FA,
    0x9EDF, 0xA6BE, 0x96DE, 0x357E, 0x4DBE, 0x7E9F, 0xAF3E, 0xC73E, 0xB6DC, 0xA6DF,
    0x74B4, 0xB71F, 0xAEBE, 0xB71B, 0xD77B, 0xC79B, 0x6E5F, 0xA6DD, 0xCF3C, 0xA71F,
    0x9EFF, 0xB73F, 0xAEFF, 0x9F1F, 0x4AEA, 0xA6FF, 0xCF1F, 0xAF7E, 0xDFB7, 0xBF9A,
    0x9EBF, 0xAF1F, 0xC71D, 0xC73D, 0x9EFF, 0xCF5E, 0xA6BD, 0xA6FF, 0x52EA, 0x8EDE,
    0x6E7F, 0x45DE, 0x871F, 0x563F, 0xBF1E, 0x969F, 0xA69C, 0xAF1D, 0x9EDE, 0xB6FE,
    0x9EBB, 0x6CF5, 0x4AA9, 0x555A, 0x5E1F, 0x45DF, 0x259F, 0x4DFD, 0xAEDF, 0xB71D,
    0xA6BC, 0x96DF, 0x8E9F, 0x8E1E, 0xCF77, 0x9D4F, 0x5268, 0x4475, 0x5E9F, 0x767E,
    0xF7BE, 0xE7DF, 0xB71D, 0xB6DF, 0x9EBD, 0x7E9E, 0x969C, 0xB6FC, 0xE775, 0x9570,
    0x5AA9, 0x4C54, 0x5E9F, 0xD7BE, 0xEF9B, 0xE77A, 0xE79B, 0xBF3F, 0x7EBF, 0xBF5F,
    0x869D, 0x969D, 0x4DBF, 0x4C34, 0x52AA, 0x5455, 0x4E5F, 0xDF9E, 0xEFBB, 0xE779,
    0xEF9A, 0xD7FF, 0x9EFF, 0x9F1F, 0x6E9F, 0x561E, 0x561E, 0x638F, 0x4A68, 0x538E,
    0x4DFE, 0xB79F, 0xEF9D, 0xEFBE, 0xEFBD, 0xDF99, 0xBF1B, 0x9EBA, 0xAEFA, 0x96DB,
    0x8E78, 0x5BCF, 0x62C9, 0x634B, 0x65B9, 0x65DC, 0xC71B, 0xCF18, 0xDF74, 0xD715,
    0xD739, 0xE779, 0xDF56, 0xCF7C, 0xD6B6, 0x6C10, 0x4269, 0x6B4B, 0x43D0, 0x6E1C,
    0xA69A, 0xA699, 0xCEF9, 0xB6B8, 0x8E39, 0xA698, 0xAEFA, 0xC7DD, 0xBEF9, 0x74B3,
    0x4A68, 0x736B, 0x5B8E, 0x53F2, 0x8490, 0x95F8, 0x6E3C, 0x5DB9, 0x5CD9, 0x96FE,
    0x767D, 0x865D, 0x8E9C, 0x5CB5, 0x3A28, 0x5289, 0x4AA9, 0x632B, 0x42AA, 0x4207,
    0x13B8, 0x22F6, 0x43F7, 0x549A, 0x54BB, 0x5D1B, 0x3B51, 0x5B0B, 0x4ACA, 0x4A47,
    0x4A48, 0x52CA, 0x52CA, 0x52EA, 0x4B8E, 0x230D, 0x4CB4, 0x865C, 0x55BB, 0x457D,
    0x5B0B, 0x530B, 0x5349, 0x632A, 0x6BAC, 0x5ACA, 0x52CA, 0x5289, 0x534C, 0x5D19,
    0x5538, 0x5CB5, 0x5C2F, 0x45FE, 0x73AD, 0x4A68, 0x3A68, 0x5B0B, 0x4A27, 0x52C9,
    0x5AEA, 0x4227, 0x4A89, 0x6BD0, 0x430E, 0x330D, 0x5430, 0x6430, 0x7CEC, 0x5309,
    0x3A27, 0x52A9, 0x4207, 0x5B4B, 0x2144, 0x6AE9, 0x42EB, 0x19E6, 0x3B0A, 0x33EE,
    0x6C8F, 0x8D94, 0x6C89, 0x4286, 0x3227, 0x4288, 0x2A26, 0x3267, 0x29A6, 0x49A4,
    0x2A27, 0x42A8, 0x4B0B, 0x6C70, 0x6453, 0x74F7, 0x53AB, 0x6405, 0x2A26, 0x5B8C,
    0x4B0A, 0x3AC9, 0x2165, 0x4AC9, 0x52EA, 0x4983, 0x5B8D, 0x8DBA, 0x95D9, 0x8D57,
)

private const val FLOW_CW = ART_W / FLOW_W
private const val FLOW_CH = ART_H / FLOW_H

/**
 * The same grid as unit vectors, expanded once.
 *
 * Vectors, not angles. Bilinear interpolation of an angle tears wherever the
 * angle wraps, and the great vortex is nothing but wrap — interpolating 350
 * degrees against 10 sweeps the long way round and puts a seam straight through
 * the middle of the sky.
 */
private val flowVec = FloatArray(FLOW.size * 2).also { v ->
    for (i in FLOW.indices) {
        val a = FLOW[i] * (2f * PI_F / 256f)
        v[2 * i] = cos(a)
        v[2 * i + 1] = sin(a)
    }
}

/** Writes the unit flow direction at (x,y) into [out]. Hot: this runs a few
 *  hundred thousand times while the painting is being laid out. */
private fun flowAt(x: Float, y: Float, out: FloatArray) {
    val fx = (x / FLOW_CW - 0.5f).coerceIn(0f, FLOW_W - 1.001f)
    val fy = (y / FLOW_CH - 0.5f).coerceIn(0f, FLOW_H - 1.001f)
    val ix = fx.toInt()
    val iy = fy.toInt()
    val tx = fx - ix
    val ty = fy - iy
    val i00 = (iy * FLOW_W + ix) * 2
    val i01 = i00 + FLOW_W * 2
    val w00 = (1f - tx) * (1f - ty)
    val w10 = tx * (1f - ty)
    val w01 = (1f - tx) * ty
    val w11 = tx * ty
    val vx = flowVec[i00] * w00 + flowVec[i00 + 2] * w10 +
            flowVec[i01] * w01 + flowVec[i01 + 2] * w11
    val vy = flowVec[i00 + 1] * w00 + flowVec[i00 + 3] * w10 +
            flowVec[i01 + 1] * w01 + flowVec[i01 + 3] * w11
    val n = hypot(vx, vy)
    if (n > 1e-5f) {
        out[0] = vx / n
        out[1] = vy / n
    } else {
        out[0] = 1f
        out[1] = 0f
    }
}

private const val TONE_CW = ART_W / TONE_W
private const val TONE_CH = ART_H / TONE_H

private fun unpack565(v: Int): Int =
    (((v ushr 11) and 31) shl 19) or (((v ushr 5) and 63) shl 10) or ((v and 31) shl 3)

private val toneLow = IntArray(TONE_LOW.size) { unpack565(TONE_LOW[it]) }
private val toneHigh = IntArray(TONE_HIGH.size) { unpack565(TONE_HIGH[it]) }

private fun bilerpTone(grid: IntArray, x: Float, y: Float): Int {
    val fx = (x / TONE_CW - 0.5f).coerceIn(0f, TONE_W - 1.001f)
    val fy = (y / TONE_CH - 0.5f).coerceIn(0f, TONE_H - 1.001f)
    val ix = fx.toInt()
    val iy = fy.toInt()
    val tx = fx - ix
    val ty = fy - iy
    val i00 = iy * TONE_W + ix
    val i01 = i00 + TONE_W
    val w00 = (1f - tx) * (1f - ty)
    val w10 = tx * (1f - ty)
    val w01 = (1f - tx) * ty
    val w11 = tx * ty
    val a = grid[i00]
    val b = grid[i00 + 1]
    val c = grid[i01]
    val d = grid[i01 + 1]
    val r = (a ushr 16 and 255) * w00 + (b ushr 16 and 255) * w10 +
            (c ushr 16 and 255) * w01 + (d ushr 16 and 255) * w11
    val g = (a ushr 8 and 255) * w00 + (b ushr 8 and 255) * w10 +
            (c ushr 8 and 255) * w01 + (d ushr 8 and 255) * w11
    val bl = (a and 255) * w00 + (b and 255) * w10 +
            (c and 255) * w01 + (d and 255) * w11
    return rgb(r, g, bl)
}

private fun rgb(r: Float, g: Float, b: Float): Int =
    (r.toInt().coerceIn(0, 255) shl 16) or
            (g.toInt().coerceIn(0, 255) shl 8) or
            b.toInt().coerceIn(0, 255)

private fun mix(a: Int, b: Int, t: Float): Int {
    val u = t.coerceIn(0f, 1f)
    return rgb(
        (a ushr 16 and 255) + ((b ushr 16 and 255) - (a ushr 16 and 255)) * u,
        (a ushr 8 and 255) + ((b ushr 8 and 255) - (a ushr 8 and 255)) * u,
        (a and 255) + ((b and 255) - (a and 255)) * u
    )
}

private fun luminance(c: Int): Float =
    0.2126f * (c ushr 16 and 255) + 0.7152f * (c ushr 8 and 255) + 0.0722f * (c and 255)

/** Deterministic 0..1 noise. A launch of this screen always paints the same
 *  canvas, and none of this allocates. */
private fun noise(a: Int, b: Int): Float {
    var h = a * 374761393 + b * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

// ---------------------------------------------------------------------------
// Fixtures measured off the reference
// ---------------------------------------------------------------------------

private fun poly(vararg xy: Float): FloatArray = xy

private fun inside(poly: FloatArray, x: Float, y: Float): Boolean {
    var c = false
    val n = poly.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val yi = poly[2 * i + 1]
        val yj = poly[2 * j + 1]
        if ((yi > y) != (yj > y)) {
            val xi = poly[2 * i]
            val xj = poly[2 * j]
            if (x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
        }
        j = i
    }
    return c
}

/**
 * The cypress, traced off a blurred luminance cut of the reference and then
 * walked row by row.
 *
 * It is one closed outline rather than a trunk plus branches: the tree is a
 * single flame in this painting, and the licks that fan off it — the thin spire
 * at (57,443), the tongue at (300,880), the flames that fan out at its base — are
 * spikes in the same silhouette. Its right edge below the hills is the one run
 * that had to be re-walked by hand: a blurred luminance cut merges the tree with
 * the bank of dark scrub in front of the village and hands back a silhouette a
 * hundred pixels too wide, which swallows the houses whole. The edge is deliberately not smoothed. Van Gogh's
 * cypress has no contour; it ends where its strokes run out, and a polygon that
 * merely decides which palette a stroke draws from keeps that.
 */
private val CYPRESS = poly(
    135f, 55f, 126f, 130f, 120f, 250f, 122f, 350f, 130f, 420f, 120f, 470f,
    108f, 530f, 96f, 556f, 86f, 516f, 66f, 470f, 57f, 443f, 50f, 510f,
    48f, 566f, 62f, 600f, 74f, 612f, 80f, 642f, 86f, 672f, 91f, 702f,
    85f, 732f, 72f, 762f, 62f, 792f, 61f, 822f, 58f, 852f, 59f, 882f,
    61f, 912f, 66f, 942f, 69f, 972f, 77f, 1002f, 81f, 1032f, 73f, 1062f,
    30f, 1090f, 30f, 1120f, 18f, 1150f, 5f, 1180f, 0f, 1210f, 0f, 1422f,
    300f, 1422f, 386f, 1396f, 452f, 1360f, 500f, 1322f, 524f, 1284f,
    518f, 1246f, 506f, 1208f, 494f, 1176f, 478f, 1150f,
    469f, 1120f, 471f, 1090f, 448f, 1060f, 419f, 1032f,
    390f, 1002f, 364f, 972f, 329f, 942f, 300f, 880f, 292f, 930f, 283f, 946f,
    233f, 912f, 227f, 882f, 225f, 852f, 217f, 822f, 212f, 792f, 210f, 762f,
    208f, 732f, 209f, 702f, 205f, 672f, 196f, 642f, 183f, 612f, 171f, 580f,
    169f, 550f, 170f, 520f, 174f, 490f, 174f, 460f, 168f, 430f, 152f, 400f,
    148f, 300f, 146f, 180f
)

/**
 * Centre, core radius and halo radius of each star, found by blob detection on
 * the warm channel rather than placed by hand. Three of them run off the edge of
 * this crop, which is why their centres sit at or past x = 0.
 */
private class Star(val cx: Float, val cy: Float, val coreR: Float, val haloR: Float) {
    /**
     * Only the twinkle. The halo itself is painted into the canvas — the sky's
     * own strokes are dragged towards [GLOW_HIGH] for a radius around each star —
     * so this sits on top of a glow that is already there, and a strong one
     * washes the whole upper sky out of its ultramarine.
     *
     * Built once: a radial gradient is a native shader compile, and rebuilding
     * one inside the draw lambda pays that on every frame the sky moves.
     */
    val glow: Brush = Brush.radialGradient(
        0.0f to Color(0x33FFF0B4),
        0.35f to Color(0x1AFFE694),
        1.0f to Color.Transparent,
        center = Offset(cx, cy),
        radius = haloR * 1.5f
    )
    val glowRadius = haloR * 1.5f
    val centre = Offset(cx, cy)
}

private val STARS = arrayOf(
    Star(204f, 45f, 15f, 44f),
    Star(409f, 54f, 18f, 52f),
    Star(5f, 60f, 16f, 46f),
    Star(532f, 91f, 26f, 74f),
    Star(211f, 250f, 46f, 96f),
    Star(376f, 467f, 20f, 60f),
    Star(24f, 685f, 36f, 90f),
    Star(426f, 758f, 38f, 124f)
)

// The star itself: a hot centre, the yellow that rings it, and the pale halo the
// sky is dragged towards for a radius or so around it.
private const val STAR_CORE = 0xFACA10
private const val STAR_HOT = 0xFCE66E
private const val STAR_RIM = 0xECE89C
private const val GLOW_HIGH = 0xD4E8C8
private const val GLOW_LOW = 0x5B8FA0

// Clustered out of the reference inside the silhouette above, rather than read
// off its luminance deciles — a decile mean averages the greens against the
// browns and hands back grey. These are the five cluster centres with their
// chroma pushed back out and the ends pulled apart, because a centre is an
// average too. The tree is far darker than it looks from across a room: the
// darkest two clusters are 60% of its area.
private val CYPRESS_RAMP = intArrayOf(0x000302, 0x0A1A10, 0x1E3324, 0x3A5A42, 0x6C8874)
private const val VEIN_DARK = 0x2E1A08
private const val VEIN_LIGHT = 0x6B4420

/**
 * The tree is not ribboned the way the sky is.
 *
 * [ribbonTone] is bimodal by design — that is what alternates the sky's light and
 * dark bands — but the reference's clusters inside the cypress come back as a
 * smooth run, and feeding a bimodal tone straight into a ramp leaves a hole in
 * the middle of it: a tree that is either black or lit, with none of its body in
 * between. Half ribbon and half a spatial draw fills that middle without
 * flattening the flames into one value, and the bands are uneven to land the
 * strokes in the proportions the clusters actually came back with.
 */
private fun cypressColour(x: Float, y: Float, tone: Float): Int {
    val t = tone * 0.45f + noise(x.toInt() shr 1, y.toInt() shr 2) * 0.55f
    // The trunk's brown is not a band of the ramp — it is the few strokes in
    // every dozen where the paint went on thin. Sampled on cells twice as tall
    // as they are wide, so a vein runs up the tree instead of dotting it.
    if (noise(x.toInt() shr 2, y.toInt() shr 3) > 0.88f) return mix(VEIN_DARK, VEIN_LIGHT, t)
    return when {
        t < 0.42f -> mix(CYPRESS_RAMP[0], CYPRESS_RAMP[1], t / 0.42f)
        t < 0.68f -> mix(CYPRESS_RAMP[1], CYPRESS_RAMP[2], (t - 0.42f) / 0.26f)
        t < 0.86f -> mix(CYPRESS_RAMP[2], CYPRESS_RAMP[3], (t - 0.68f) / 0.18f)
        else -> mix(CYPRESS_RAMP[3], CYPRESS_RAMP[4], (t - 0.86f) / 0.14f)
    }
}

/** Index of the star whose halo covers this point, or -1. The halos do not
 *  overlap, so the first hit is the only hit. */
private fun starAt(x: Float, y: Float): Int {
    for (i in STARS.indices) {
        val s = STARS[i]
        if (hypot(x - s.cx, y - s.cy) < s.haloR) return i
    }
    return -1
}

/**
 * The colour of one brush stroke: the local tonal range from the study, drawn
 * from at whichever end this stroke's ribbon puts it.
 *
 * The grid only carries the broad tone — 57 pixels to a cell. All of the contrast
 * that makes the sky read as painted rather than airbrushed comes from [tone],
 * which alternates across neighbouring streamlines, so a light ribbon and the
 * dark one beside it draw the two ends of the same cell's range.
 */
private fun paintColour(x: Float, y: Float, tone: Float): Int {
    if (inside(CYPRESS, x, y)) return cypressColour(x, y, tone)
    var lo = bilerpTone(toneLow, x, y)
    var hi = bilerpTone(toneHigh, x, y)
    val si = starAt(x, y)
    if (si >= 0) {
        // Both ends of the range are dragged towards the halo, so the ribbons
        // survive inside it: a star sits in swirling paint, not in a wash.
        val s = STARS[si]
        val d = hypot(x - s.cx, y - s.cy)
        val k = min(1f, (1f - d / s.haloR) * 1.45f)
        lo = mix(lo, GLOW_LOW, k)
        hi = mix(hi, GLOW_HIGH, k)
        val core = s.coreR * 1.6f
        if (d < core) {
            val c = min(1f, (core - d) / (s.coreR * 1.2f))
            lo = mix(lo, STAR_RIM, c)
            hi = mix(hi, STAR_HOT, c)
        }
    }
    return mix(lo, hi, tone)
}

// ---------------------------------------------------------------------------
// Laying the strokes down
// ---------------------------------------------------------------------------

/** Growable primitive buffers. The layout allocates around a hundred thousand
 *  coordinates; boxing any of them would dominate the build. */
private class Floats(initial: Int = 4096) {
    var a = FloatArray(initial)
    var n = 0
    fun add(v: Float) {
        if (n == a.size) a = a.copyOf(n * 2)
        a[n++] = v
    }
}

private class Ints(initial: Int = 1024) {
    var a = IntArray(initial)
    var n = 0
    fun add(v: Int) {
        if (n == a.size) a = a.copyOf(n * 2)
        a[n++] = v
    }
}

private const val D_SEP = 5.4f              // gap between neighbouring streamlines
private const val D_STEP = 3.6f             // integration step along one
private const val D_TEST = D_SEP * 0.80f    // how close a line may come to its neighbour
private const val MAX_STEPS = 70

private val GRID_COLS = (ART_W / D_SEP).toInt() + 2
private val GRID_ROWS = (ART_H / D_SEP).toInt() + 2

/**
 * Occupancy for the streamline placement, as buckets of a linked list over three
 * flat arrays. Every candidate seed and every integration step asks it whether a
 * point is clear, so it is the hottest structure in the build; a map of lists
 * would spend the whole layout in the allocator.
 */
private class SeedGrid(capacity: Int) {
    private val head = IntArray(GRID_COLS * GRID_ROWS) { -1 }
    private val next = IntArray(capacity)
    private val px = FloatArray(capacity)
    private val py = FloatArray(capacity)
    private var n = 0

    fun add(x: Float, y: Float) {
        if (n == next.size) return
        val b = (y / D_SEP).toInt() * GRID_COLS + (x / D_SEP).toInt()
        px[n] = x
        py[n] = y
        next[n] = head[b]
        head[b] = n
        n++
    }

    fun free(x: Float, y: Float, dmin: Float): Boolean {
        if (x < 0f || x >= ART_W || y < 0f || y >= ART_H) return false
        val cx = (x / D_SEP).toInt()
        val cy = (y / D_SEP).toInt()
        val d2 = dmin * dmin
        var j = max(cy - 1, 0)
        val jEnd = min(cy + 1, GRID_ROWS - 1)
        while (j <= jEnd) {
            var i = max(cx - 1, 0)
            val iEnd = min(cx + 1, GRID_COLS - 1)
            while (i <= iEnd) {
                var k = head[j * GRID_COLS + i]
                while (k >= 0) {
                    val dx = px[k] - x
                    val dy = py[k] - y
                    if (dx * dx + dy * dy < d2) return false
                    k = next[k]
                }
                i++
            }
            j++
        }
        return true
    }
}

private class Streamlines(
    val pts: FloatArray,
    val first: IntArray,
    val count: IntArray,
    val ribbon: IntArray,
    val size: Int
)

private fun trace(grid: SeedGrid, sx: Float, sy: Float, dir: Float, out: Floats, v: FloatArray) {
    var x = sx
    var y = sy
    repeat(MAX_STEPS) {
        if (!grid.free(x, y, D_TEST)) return
        out.add(x)
        out.add(y)
        flowAt(x, y, v)
        x += v[0] * D_STEP * dir
        y += v[1] * D_STEP * dir
        if (x < 0f || x >= ART_W || y < 0f || y >= ART_H) return
    }
}

/**
 * Evenly spaced streamlines, after Jobard and Lefer: trace one line, then offer
 * new seeds a fixed distance out along its normal, and keep only the seeds that
 * are still clear.
 *
 * Two things fall out of this that a scatter of random seeds does not give.
 * Coverage is even — no clumps, no bald patches, which is what a canvas worked
 * over in passes looks like. And each line inherits a ribbon index from the line
 * that seeded it, one step further out; that index is what the tone alternates
 * on, so light and dark bands run *along* the flow the way Van Gogh's do instead
 * of speckling across it.
 */
private suspend fun placeStreamlines(): Streamlines {
    val grid = SeedGrid(140_000)
    val pts = Floats(140_000)
    val first = Ints(2048)
    val count = Ints(2048)
    val ribbon = Ints(2048)

    val qx = Floats(65_536)
    val qy = Floats(65_536)
    val qr = Ints(65_536)
    qx.add(ART_W * 0.5f); qy.add(ART_H * 0.5f); qr.add(0)

    val v = FloatArray(2)
    val fwd = Floats(256)
    val bwd = Floats(256)
    var cursor = 0

    while (cursor < qr.n) {
        // Neither loop suspends, so without this the layout runs to completion
        // on a background thread after the screen it was for has gone.
        if ((cursor and 255) == 0) coroutineContext.ensureActive()
        val sx = qx.a[cursor]
        val sy = qy.a[cursor]
        val rib = qr.a[cursor]
        cursor++
        if (!grid.free(sx, sy, D_SEP)) continue

        fwd.n = 0
        bwd.n = 0
        trace(grid, sx, sy, 1f, fwd, v)
        trace(grid, sx, sy, -1f, bwd, v)
        val nf = fwd.n / 2
        val nb = bwd.n / 2
        if (nf + nb - 1 < 3) continue

        val start = pts.n / 2
        for (i in nb - 1 downTo 1) {
            pts.add(bwd.a[2 * i]); pts.add(bwd.a[2 * i + 1])
        }
        for (i in 0 until nf) {
            pts.add(fwd.a[2 * i]); pts.add(fwd.a[2 * i + 1])
        }
        val n = pts.n / 2 - start
        for (i in 0 until n) grid.add(pts.a[2 * (start + i)], pts.a[2 * (start + i) + 1])
        first.add(start); count.add(n); ribbon.add(rib)

        var k = 0
        while (k < n) {
            val x = pts.a[2 * (start + k)]
            val y = pts.a[2 * (start + k) + 1]
            flowAt(x, y, v)
            qx.add(x - v[1] * D_SEP); qy.add(y + v[0] * D_SEP); qr.add(rib + 1)
            qx.add(x + v[1] * D_SEP); qy.add(y - v[0] * D_SEP); qr.add(rib - 1)
            k += 3
        }
    }
    return Streamlines(pts.a, first.a, count.a, ribbon.a, first.n)
}

/**
 * Where this stroke sits between the dark and the light end of its cell.
 *
 * The sine over the ribbon index is pushed towards a square wave, because the
 * reference alternates rather than ramps: a pale band sits hard against a deep
 * ultramarine one with nothing in between. The two noise terms stop the bands
 * from reading as wallpaper.
 */
private fun ribbonTone(ribbon: Int, seg: Int): Float {
    val s = sin(ribbon * 2.399f)
    val squared = 0.5f + 0.5f * sign(s) * abs(s).pow(0.55f)
    // Both noise terms are added rather than centred, so the three coefficients
    // sum to exactly 1 and the result spans the full range without ever needing
    // the clamp. An earlier version centred them, overshot both ends, and leant
    // on coerceIn — which piled something like a fifteenth of every region onto
    // exactly its palest colour, a speckle of identical bright strokes that
    // reads as noise rather than as light. Centring it inside the range instead
    // fixed the pile-up but lifted the darks off the bottom of every cell.
    return (squared * 0.79f + 0.14f * noise(ribbon, seg) +
            0.07f * noise(seg, ribbon)).coerceIn(0f, 1f)
}

// One Stroke per width step, resolved once. A Stroke built inside a draw lambda
// is an allocation on every frame that anything moves.
private const val WIDTH_STEPS = 8
private const val WIDTH_MIN = 2.2f
private const val WIDTH_MAX = 8.0f
private val strokeWidths = FloatArray(WIDTH_STEPS) {
    WIDTH_MIN + (WIDTH_MAX - WIDTH_MIN) * it / (WIDTH_STEPS - 1f)
}

private val liveStyles = Array(WIDTH_STEPS) {
    Stroke(strokeWidths[it], cap = StrokeCap.Round, join = StrokeJoin.Bevel)
}

private fun widthBucket(w: Float): Int =
    (((w - WIDTH_MIN) / (WIDTH_MAX - WIDTH_MIN)) * (WIDTH_STEPS - 1) + 0.5f)
        .toInt().coerceIn(0, WIDTH_STEPS - 1)

/** A run of brush strokes as five parallel arrays: points, where each stroke
 *  starts and ends in them, its colour and its width step. */
private class StrokeSet(
    val pts: FloatArray,
    val first: IntArray,
    val count: IntArray,
    val colour: IntArray,
    val width: IntArray,
    val size: Int
)

private class StrokeBuilder(capacity: Int = 8192) {
    private val pts = Floats(capacity * 12)
    private val first = Ints(capacity)
    private val count = Ints(capacity)
    private val colour = Ints(capacity)
    private val width = Ints(capacity)

    fun begin(): Int = pts.n / 2

    fun point(x: Float, y: Float) {
        pts.add(x); pts.add(y)
    }

    fun end(start: Int, argb: Int, w: Float) {
        val n = pts.n / 2 - start
        if (n < 2) {
            pts.n = start * 2
            return
        }
        first.add(start); count.add(n); colour.add(argb); width.add(widthBucket(w))
    }

    fun copyFrom(src: FloatArray, from: Int, n: Int, argb: Int, w: Float) {
        val start = begin()
        for (i in 0 until n) point(src[2 * (from + i)], src[2 * (from + i) + 1])
        end(start, argb, w)
    }

    fun build() = StrokeSet(pts.a, first.a, count.a, colour.a, width.a, first.n)
}

private fun opaque(rgb: Int) = 0xFF000000.toInt() or rgb

// ---------------------------------------------------------------------------
// Building the painting
// ---------------------------------------------------------------------------

private const val LIVE_BUCKETS = 12
private const val LIVE_SPAN = 5
private const val LIVE_STRIDE = 3
private const val LIVE_MIN_POINTS = 20
private const val LIVE_SHARE = 0.30f

/**
 * The strokes that move.
 *
 * Each one is a short window onto a streamline the static painting also used, so
 * a moving stroke has the same shape as the paint it slides over and the surface
 * churns instead of sprouting fireflies. Its colour and width are close but not
 * identical: the colour is biased towards the light end of its cell and then
 * quantised to [LIVE_BUCKETS] representatives, and the width is its bucket's
 * average. Both are what let a frame walk each bucket contiguously and issue one
 * draw for the lot.
 */
private class LiveSet(
    val pts: FloatArray,
    val first: IntArray,
    val count: IntArray,
    val phase: FloatArray,
    val speed: FloatArray,
    val bucketStart: IntArray,
    val bucketColour: IntArray,
    val bucketWidth: IntArray
)

/**
 * The finished canvas as a bitmap, plus the strokes that move over it.
 *
 * A bitmap rather than a GraphicsLayer, for three reasons that all matter. It can
 * be filled on a background thread, so the five thousand paths are stroked before
 * the screen is ever asked for a frame instead of in the one that navigates here.
 * Its size is fixed, so a landscape window cannot ask for 44MB and blow past the
 * texture limit the way a cover-scaled layer does. And it can be drawn through a
 * mesh, which is the whole of how this painting moves.
 */
private class Painting(val canvas: Bitmap, val live: LiveSet, val warp: Warp)

private const val WASH_W = TONE_W * 4
private const val WASH_H = TONE_H * 4

/** The underpainting: the tonal study itself, smoothed. It only has to fill the
 *  hairline gaps the brush leaves, so a 56x100 bitmap stretched over the canvas
 *  is both enough and one draw call. */
private fun buildWash(): Bitmap {
    val px = IntArray(WASH_W * WASH_H)
    for (j in 0 until WASH_H) {
        val y = (j + 0.5f) / WASH_H * ART_H
        for (i in 0 until WASH_W) {
            val x = (i + 0.5f) / WASH_W * ART_W
            px[j * WASH_W + i] =
                opaque(mix(bilerpTone(toneLow, x, y), bilerpTone(toneHigh, x, y), 0.42f))
        }
    }
    return Bitmap.createBitmap(px, WASH_W, WASH_H, Bitmap.Config.ARGB_8888)
}

/**
 * The scale the canvas is rasterised at, fixed rather than taken from the window.
 *
 * Fixing it is what lets the whole thing happen off the main thread — the window
 * has not been measured yet when the build starts. 1600x2844 is a shade sharper
 * than a 1080p portrait screen needs and a sixth soft on a 1440p one, which the
 * mesh's own filtering would cost anyway.
 */
private const val RASTER = 2.0f
private val RASTER_W = (ART_W * RASTER).toInt()
private val RASTER_H = (ART_H * RASTER).toInt()

/** Strokes one run of paint into a software canvas, reusing a single Path and
 *  Paint the way the display-list version did. */
private fun strokeInto(c: AndroidCanvas, paint: AndroidPaint, path: AndroidPath, set: StrokeSet) {
    var i = 0
    while (i < set.size) {
        val f = set.first[i] * 2
        val n = set.count[i]
        path.rewind()
        path.moveTo(set.pts[f], set.pts[f + 1])
        var k = 1
        while (k < n) {
            path.lineTo(set.pts[f + 2 * k], set.pts[f + 2 * k + 1])
            k++
        }
        paint.color = set.colour[i]
        paint.strokeWidth = strokeWidths[set.width[i]]
        c.drawPath(path, paint)
        i++
    }
}

private suspend fun rasterise(wash: Bitmap, sky: StrokeSet, stars: StrokeSet): Bitmap {
    val bmp = Bitmap.createBitmap(RASTER_W, RASTER_H, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.scale(RASTER, RASTER)
    c.drawBitmap(
        wash,
        null,
        RectF(0f, 0f, ART_W, ART_H),
        AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG)
    )
    coroutineContext.ensureActive()
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }
    val path = AndroidPath()
    strokeInto(c, paint, path, sky)
    coroutineContext.ensureActive()
    strokeInto(c, paint, path, stars)
    // The wash covers the whole rect from an opaque source, so nothing here is
    // translucent — saying so lets HWUI skip blending once the entry fade is
    // done. (prepareToDraw looks like it belongs here too, to get the 17MB
    // texture upload off the first frame; measured from a background thread it
    // moves nothing, so it is not here.)
    bmp.setHasAlpha(false)
    return bmp
}

/** Cuts the streamlines into brush strokes: a run of paint, the brush lifted, the
 *  next run. Left uncut they read as contour lines — the single thing that most
 *  gives away a flow field pretending to be a painting. */
private suspend fun paintSky(lines: Streamlines, rnd: Random): StrokeSet {
    val b = StrokeBuilder(7000)
    for (li in 0 until lines.size) {
        if ((li and 127) == 0) coroutineContext.ensureActive()
        val f = lines.first[li]
        val n = lines.count[li]
        val rib = lines.ribbon[li]
        var i = 0
        var seg = 0
        while (i < n - 2) {
            // The tree is not painted the way the sky is. Its strokes are long
            // sinuous flames laid edge to edge with no ground showing through,
            // where the sky is mostly dabs with one sweep in six and a visible
            // lift between them. Cutting both the same length is what made the
            // cypress read as scattered dashes on a dark field.
            val hx = lines.pts[2 * (f + i)]
            val hy = lines.pts[2 * (f + i) + 1]
            val tree = inside(CYPRESS, hx, hy)
            // A stroke takes its colour from its own midpoint, so a long one that
            // starts in a halo drags the star's yellow out across the sky behind
            // it. Inside a halo the brush stays short and the glow stays round.
            val run = when {
                tree -> 12 + rnd.nextInt(18)
                starAt(hx, hy) >= 0 -> 4 + rnd.nextInt(6)
                rnd.nextInt(6) == 0 -> 15 + rnd.nextInt(11)
                else -> 5 + rnd.nextInt(10)
            }
            val j = min(i + run, n)
            if (j - i >= 3) {
                val mid = f + (i + j) / 2
                val tone = ribbonTone(rib, seg)
                val colour = paintColour(lines.pts[2 * mid], lines.pts[2 * mid + 1], tone)
                val w = D_SEP * (0.80f + 0.40f * noise(seg * 7 + rib, rib * 3))
                b.copyFrom(lines.pts, f + i, j - i, opaque(colour), w)
            }
            i = j + if (tree) rnd.nextInt(2) else 1 + rnd.nextInt(3)
            seg++
        }
    }
    return b.build()
}

/**
 * The star centres, painted over the sky once it is down.
 *
 * Van Gogh does not stamp a disc — he winds the paint round — so each centre is
 * a set of short arcs on jittered rings, not a filled circle. They go on last
 * because that is the order they were painted in: the sky's strokes run through
 * where the star is, and its centre is laid on top of them.
 */
private fun paintStarCores(): StrokeSet {
    val b = StrokeBuilder(2400)
    for (s in STARS) {
        val rings = max(3, (s.coreR / 6.5f).toInt())
        for (ri in 0 until rings) {
            val rr = s.coreR * (0.20f + 0.86f * ri / rings) *
                    (1f + 0.10f * (noise(ri, s.cx.toInt()) - 0.5f))
            val arcs = max(5, (2f * PI_F * rr / 7f).toInt())
            val base = mix(STAR_CORE, STAR_RIM, (ri / max(1, rings - 1).toFloat()).pow(0.8f))
            for (si in 0 until arcs) {
                val a0 = 2f * PI_F * (si + noise(ri * 31 + si, s.cx.toInt()) * 0.8f) / arcs
                val sweep = 2f * PI_F / arcs * 1.5f
                val rj = rr * (1f + 0.09f * (noise(si * 13, ri * 7) - 0.5f))
                val start = b.begin()
                for (u in 0..6) {
                    val a = a0 + sweep * u / 6f
                    b.point(s.cx + cos(a) * rj, s.cy + sin(a) * rj)
                }
                val tint = if (noise(si, ri) > 0.62f) 0xFFFFFF else 0xBE840A
                b.end(
                    start, opaque(mix(base, tint, abs(noise(si * 7, ri * 3) - 0.5f) * 0.8f)),
                    max(2.4f, s.coreR * 0.20f)
                )
            }
        }
    }

    return b.build()
}

/** Picks the streamlines that will carry motion and quantises their colours down
 *  to [LIVE_BUCKETS], so a frame costs a dozen draws rather than seven hundred. */
private fun buildLive(lines: Streamlines): LiveSet {
    val pick = Ints(1024)
    for (li in 0 until lines.size) {
        if (noise(li, 3) < LIVE_SHARE && lines.count[li] >= LIVE_MIN_POINTS) pick.add(li)
    }
    val n = pick.n
    // Not a state the field can produce, but the palette below indexes into the
    // picks and should not be the thing that finds out.
    if (n == 0) return LiveSet(
        lines.pts, IntArray(0), IntArray(0), FloatArray(0), FloatArray(0),
        IntArray(LIVE_BUCKETS + 1), IntArray(LIVE_BUCKETS), IntArray(LIVE_BUCKETS)
    )
    val colour = IntArray(n)
    val width = IntArray(n)
    val phase = FloatArray(n)
    val speed = FloatArray(n)
    val first = IntArray(n)
    val count = IntArray(n)
    for (k in 0 until n) {
        val li = pick.a[k]
        val f = lines.first[li]
        val c = lines.count[li]
        first[k] = f
        count[k] = c
        val mid = f + c / 2
        // Biased to the light end: the moving paint is the paint that catches
        // the eye, and a dark stroke sliding over dark paint is invisible work.
        val tone = 0.45f + 0.55f * ribbonTone(lines.ribbon[li], k)
        colour[k] = paintColour(lines.pts[2 * mid], lines.pts[2 * mid + 1], tone)
        width[k] = widthBucket(D_SEP * (0.85f + 0.35f * noise(li, 21)))
        phase[k] = noise(li, 9)
        speed[k] = 0.020f + 0.026f * noise(li, 27)
    }

    // Quantise on luminance: the palette runs deep ultramarine to cream almost
    // monotonically, so ordering by brightness gives representatives that are
    // close in hue as well. Twelve is measured, not guessed — issuing a path
    // costs far more than the strokes in it, so every bucket past this is a draw
    // call bought for a shade nobody can pick out of moving paint.
    val order = (0 until n).sortedBy { luminance(colour[it]) }
    val palette = IntArray(LIVE_BUCKETS) {
        colour[order[(it * n / LIVE_BUCKETS).coerceIn(0, max(0, n - 1))]]
    }
    val bucket = IntArray(n)
    for (k in 0 until n) {
        var best = 0
        var bestD = Int.MAX_VALUE
        for (p in 0 until LIVE_BUCKETS) {
            val d = abs((colour[k] ushr 16 and 255) - (palette[p] ushr 16 and 255)) +
                    abs((colour[k] ushr 8 and 255) - (palette[p] ushr 8 and 255)) +
                    abs((colour[k] and 255) - (palette[p] and 255))
            if (d < bestD) {
                bestD = d; best = p
            }
        }
        bucket[k] = best
    }

    // Counting sort into bucket order.
    val start = IntArray(LIVE_BUCKETS + 1)
    for (k in 0 until n) start[bucket[k] + 1]++
    for (p in 0 until LIVE_BUCKETS) start[p + 1] += start[p]
    val cursor = start.copyOf()
    val sFirst = IntArray(n);
    val sCount = IntArray(n)
    val sPhase = FloatArray(n);
    val sSpeed = FloatArray(n)
    val widthSum = IntArray(LIVE_BUCKETS);
    val widthN = IntArray(LIVE_BUCKETS)
    for (k in 0 until n) {
        val p = bucket[k]
        val at = cursor[p]++
        sFirst[at] = first[k]; sCount[at] = count[k]
        sPhase[at] = phase[k]; sSpeed[at] = speed[k]
        widthSum[p] += width[k]; widthN[p]++
    }
    return LiveSet(
        lines.pts, sFirst, sCount, sPhase, sSpeed, start,
        IntArray(LIVE_BUCKETS) { opaque(palette[it]) },
        IntArray(LIVE_BUCKETS) { if (widthN[it] == 0) 4 else widthSum[it] / widthN[it] }
    )
}

/**
 * The whole painting, off the main thread.
 *
 * Placing the streamlines is a few hundred thousand point-in-neighbourhood tests
 * and the colouring is a point-in-polygon test per stroke; on the main thread all
 * of it lands in the frame that navigates here and the screen arrives late.
 */
private suspend fun buildPainting(): Painting = withContext(Dispatchers.Default) {
    val rnd = Random(11)
    val lines = placeStreamlines()
    val canvas = rasterise(buildWash(), paintSky(lines, rnd), paintStarCores())
    // Off the main thread with the rest of it: the mesh costs a flow lookup and
    // nine point-in-polygon tests against a 75-vertex outline per vertex, which
    // is a slow frame if it is built during composition.
    Painting(canvas, buildLive(lines), Warp())
}

// ---------------------------------------------------------------------------
// Drawing it
// ---------------------------------------------------------------------------

/**
 * One offscreen buffer holding the finished canvas.
 *
 * The static painting is some five thousand stroked polylines. Replaying that
 * every frame costs more than the whole screen's budget, and none of it changes:
 * so it is recorded once, HWUI rasterises it into a texture, and a frame is that
 * texture under a matrix. Roughly 14MB at 1080p, which buys back everything the
 * moving paint needs.
 *
 * Kept outside the snapshot system on purpose — nothing in here should ever be
 * able to trigger a recomposition.
 */
// ---------------------------------------------------------------------------
// The warp
// ---------------------------------------------------------------------------

private const val MESH_W = 32
private const val MESH_H = 56
private const val MESH_VX = MESH_W + 1
private const val MESH_VY = MESH_H + 1
private const val MESH_N = MESH_VX * MESH_VY
private const val MESH_CW = ART_W / MESH_W
private const val MESH_CH = ART_H / MESH_H

private const val FLOW_AMP = 6.4f      // how far paint slides along its own grain
private const val CROSS_AMP = 0.45f    // and across it, which is what makes it swirl
private const val SWAY_AMP = 9.5f      // the cypress, at the tip
private const val FLAME_AMP = 0.5f     // and how much of the flow motion it keeps
private const val TWIST_AMP = 0.155f   // radians the eddies rock through
private const val EDGE_FADE = 80f      // ambient travel tapers out over this
private const val PUSH_FADE = 130f     // the push, being far stronger, over this

/**
 * Where the paint turns rather than merely sliding.
 *
 * A plane wave running through the great spiral makes it pulse, not rotate — the
 * whole thing brightens and slackens together. These rock their neighbourhood
 * about a centre instead, which is the difference between a swirl that breathes
 * and a swirl that turns. The amplitude is small and it reverses, because an
 * unbounded twist would eventually smear the halo's arcs into plain rings.
 *
 * Centres are the great vortex and the three stars with halos wide enough to
 * carry a rotation; [group] splits them across two of the existing time phases so
 * they do not all rock in step.
 */
private class Eddy(val cx: Float, val cy: Float, val radius: Float, val group: Int)

private val EDDIES = arrayOf(
    Eddy(566f, 470f, 310f, 0),
    Eddy(426f, 758f, 200f, 1),
    Eddy(211f, 250f, 150f, 1),
    Eddy(532f, 91f, 120f, 0)
)

private const val PUSH_RADIUS = 260f
private const val PUSH_MAX = 52f       // art px of shove a drag can pile up
private const val RIPPLE_SPEED = 620f  // art px per second
private const val RIPPLE_WIDTH = 190f
private const val RIPPLE_AMP = 26f
private const val RIPPLE_LIFE = 1.9f   // seconds before the ring is spent

/** Where your finger is and how hard it is pushing, in art units. Deliberately
 *  not snapshot state: the frame loop already invalidates the draw every frame,
 *  and routing a pointer stream through the snapshot system to say so again
 *  would cost a recomposition per move event. */
private class Touch {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var tapX = 0f
    var tapY = 0f
    var ripple = -1f          // seconds since the tap, negative when spent
}

/**
 * The canvas is drawn through a mesh, and this is the mesh.
 *
 * Everything that made the earlier version read as a still painting with sparks
 * on it comes down to one fact: the base was a blit, and a blit cannot move.
 * Here the bitmap is mapped onto a 32x56 grid whose every vertex slides along the
 * brush direction at that point — so the vortex turns, the halos orbit, the sky
 * breathes, and the strokes stretch and gather the way wet paint does.
 *
 * The direction, the amplitude and the three spatial phases are all resolved once
 * at build time, because none of them depend on the clock. A frame is three sines
 * and a handful of multiplies per vertex, about nineteen hundred of them.
 */
private class Warp {
    val base = FloatArray(MESH_N * 2)
    private val alongX = FloatArray(MESH_N)
    private val alongY = FloatArray(MESH_N)
    private val crossX = FloatArray(MESH_N)
    private val crossY = FloatArray(MESH_N)
    private val p1 = FloatArray(MESH_N)
    private val p2 = FloatArray(MESH_N)
    private val p3 = FloatArray(MESH_N)
    private val twistAX = FloatArray(MESH_N)
    private val twistAY = FloatArray(MESH_N)
    private val twistBX = FloatArray(MESH_N)
    private val twistBY = FloatArray(MESH_N)

    /**
     * The boundary taper, kept per vertex. It is baked into the ambient terms
     * below, but the push and the ripple are added at draw time and have to be
     * multiplied by it there, or a finger near an edge peels the canvas off it.
     *
     * The push gets its own, gentler ramp. It is several times stronger than the
     * ambient motion, and a taper steep enough for one is a cliff for the other:
     * at [EDGE_FADE] the amplitude climbs from nothing to full over three rows of
     * mesh, which at full shove is more than a cell's width of shear per cell —
     * enough to turn a triangle inside out and crease the paint.
     */
    private val edgeW = FloatArray(MESH_N)
    private val pushW = FloatArray(MESH_N)

    /** The displacement itself, kept so the strokes and glows drawn on top can
     *  be moved by the same field and stay welded to the paint under them. */
    val disp = FloatArray(MESH_N * 2)
    val verts = FloatArray(MESH_N * 2)

    init {
        val v = FloatArray(2)
        for (j in 0 until MESH_VY) {
            for (i in 0 until MESH_VX) {
                val k = j * MESH_VX + i
                val x = i * MESH_CW
                val y = j * MESH_CH
                base[2 * k] = x
                base[2 * k + 1] = y
                flowAt(x, y, v)

                // Nothing moves at the very edge, or the canvas would peel away
                // from its own boundary and show the ground behind it.
                val fade = min(
                    min(x, ART_W - x) / EDGE_FADE,
                    min(y, ART_H - y) / EDGE_FADE
                ).coerceIn(0f, 1f)
                val edge = fade * fade * (3f - 2f * fade)
                edgeW[k] = edge
                val soft = min(
                    min(x, ART_W - x) / PUSH_FADE,
                    min(y, ART_H - y) / PUSH_FADE
                ).coerceIn(0f, 1f)
                pushW[k] = soft * soft * (3f - 2f * soft)

                // A tree does not slide along its own grain; it sways about its
                // root. Softened across the silhouette so the boundary shears
                // instead of tearing.
                val tree = cypressWeight(x, y)
                val h = ((1360f - y) / 1300f).coerceIn(0f, 1f)
                val sway = SWAY_AMP * h * h * edge

                // The tree keeps half its flow motion on top of the sway. Its
                // grain runs up the flames, so that half reads as the fire
                // guttering — which is what a cypress does in this painting.
                val amp = FLOW_AMP * edge * (1f - tree * (1f - FLAME_AMP))
                alongX[k] = v[0] * amp + sway * tree
                alongY[k] = v[1] * amp
                crossX[k] = -v[1] * amp * CROSS_AMP
                crossY[k] = v[0] * amp * CROSS_AMP

                for (eddy in EDDIES) {
                    val ox = x - eddy.cx
                    val oy = y - eddy.cy
                    val d = hypot(ox, oy)
                    if (d >= eddy.radius) continue
                    // Rotation about the centre is a displacement perpendicular
                    // to the radius and proportional to it, so this is already
                    // the whole per-radian term; the frame only scales it.
                    val q = 1f - d / eddy.radius
                    val w = q * q * TWIST_AMP * edge
                    if (eddy.group == 0) {
                        twistAX[k] -= oy * w
                        twistAY[k] += ox * w
                    } else {
                        twistBX[k] -= oy * w
                        twistBY[k] += ox * w
                    }
                }

                // Three wavelengths, none a multiple of another, so the surface
                // never visibly repeats. Long ones: a wave crosses the canvas in
                // about ten seconds, which is a current, not a vibration.
                p1[k] = 0.0195f * x + 0.0131f * y
                p2[k] = -0.0112f * x + 0.0168f * y
                p3[k] = 0.0451f * x - 0.0377f * y
            }
        }
        smoothDirections()
        System.arraycopy(base, 0, verts, 0, base.size)
    }

    /**
     * One 1-2-1 pass over the displacement vectors.
     *
     * [flowAt] normalises *after* interpolating, so where two cells of the baked
     * FLOW grid point nearly opposite each other the unit direction flips inside
     * a single cell. At 25 art px of mesh that showed up as neighbouring vertices
     * with along-displacements of -6.4 and +4.2 — a 10.6 px swing across one
     * cell, enough that a hard drag on top of it could invert the triangle and
     * crease the paint. Smoothing the vectors costs nothing at build time and
     * cannot introduce a flip of its own, because an average of unit vectors is
     * never longer than the vectors it averages.
     */
    private fun smoothDirections() {
        for (a in arrayOf(alongX, alongY, crossX, crossY)) {
            val copy = a.copyOf()
            for (j in 0 until MESH_VY) {
                for (i in 0 until MESH_VX) {
                    val k = j * MESH_VX + i
                    val l = if (i > 0) copy[k - 1] else copy[k]
                    val r = if (i < MESH_VX - 1) copy[k + 1] else copy[k]
                    val u = if (j > 0) copy[k - MESH_VX] else copy[k]
                    val d = if (j < MESH_VY - 1) copy[k + MESH_VX] else copy[k]
                    a[k] = (4f * copy[k] + l + r + u + d) / 8f
                }
            }
        }
    }

    fun update(t: Double, touch: Touch, gust: Float) {
        // Constant for the whole frame: a slow wander in the wave speed, so the
        // current surges and eases instead of running at one rate forever.
        // Folded into 0..2pi in double before narrowing. Sine is periodic, so
        // this is exact — and it is the whole reason the clock is a Double: take
        // the product in float and the precision it was protecting is gone.
        val wob = 0.55 * sin(t * 0.107)
        val a1 = ((t * 0.62 + wob) % TAU).toFloat()
        val a2 = ((-t * 0.44) % TAU).toFloat()
        val a3 = ((t * 0.87) % TAU).toFloat()
        val swell = 1f + 0.55f * gust

        val push = touch.vx != 0f || touch.vy != 0f
        val ripple = touch.ripple >= 0f
        val rr = touch.ripple * RIPPLE_SPEED
        val rAmp =
            if (ripple) RIPPLE_AMP * (1f - touch.ripple / RIPPLE_LIFE).coerceIn(0f, 1f) else 0f

        for (k in 0 until MESH_N) {
            val s1 = sin(p1[k] + a1)
            val s2 = sin(p2[k] + a2)
            val s3 = sin(p3[k] + a3)
            val along = (0.50f * s1 + 0.32f * s2 + 0.18f * s3) * swell
            val cross = (0.55f * s2 - 0.45f * s3) * swell
            // The two twists ride s2 and s3 rather than sines of their own: the
            // phases are already there and unrelated, so the eddies rock out of
            // step for free.
            var dx = alongX[k] * along + crossX[k] * cross +
                    twistAX[k] * s2 * swell + twistBX[k] * s3 * swell
            var dy = alongY[k] * along + crossY[k] * cross +
                    twistAY[k] * s2 * swell + twistBY[k] * s3 * swell

            val x = base[2 * k]
            val y = base[2 * k + 1]

            if (push) {
                // Paint moves with your finger and settles back. The falloff is
                // squared rather than gaussian: no exp per vertex per frame, and
                // it reaches exactly zero at the radius instead of asymptotically.
                val ox = x - touch.x
                val oy = y - touch.y
                val q = 1f - (ox * ox + oy * oy) / (PUSH_RADIUS * PUSH_RADIUS)
                if (q > 0f) {
                    val f = q * q * pushW[k]
                    dx += touch.vx * f
                    dy += touch.vy * f
                }
            }

            if (ripple) {
                val ox = x - touch.tapX
                val oy = y - touch.tapY
                // sqrt, not hypot: hypot compiles to the overflow-safe libm
                // routine and this runs 1881 times a frame for the ripple's
                // whole life. Nothing here gets within range of overflowing.
                val d = sqrt(ox * ox + oy * oy)
                val g = (d - rr) / RIPPLE_WIDTH
                if (g > -1f && g < 1f && d > 1f) {
                    val f = (1f - g * g) * (1f - g * g) * pushW[k]
                    dx += ox / d * rAmp * f
                    dy += oy / d * rAmp * f
                }
            }

            disp[2 * k] = dx
            disp[2 * k + 1] = dy
            verts[2 * k] = x + dx
            verts[2 * k + 1] = y + dy
        }

    }

    /** The displacement at an arbitrary point, bilinear off the grid the mesh
     *  just computed. Anything drawn over the canvas rides this so it moves with
     *  the paint rather than sliding across it. */
    fun sample(x: Float, y: Float, out: FloatArray) {
        val fx = (x / MESH_CW).coerceIn(0f, MESH_W - 0.001f)
        val fy = (y / MESH_CH).coerceIn(0f, MESH_H - 0.001f)
        val ix = fx.toInt()
        val iy = fy.toInt()
        val tx = fx - ix
        val ty = fy - iy
        val i00 = (iy * MESH_VX + ix) * 2
        val i01 = i00 + MESH_VX * 2
        val w00 = (1f - tx) * (1f - ty)
        val w10 = tx * (1f - ty)
        val w01 = (1f - tx) * ty
        val w11 = tx * ty
        out[0] = disp[i00] * w00 + disp[i00 + 2] * w10 + disp[i01] * w01 + disp[i01 + 2] * w11
        out[1] = disp[i00 + 1] * w00 + disp[i00 + 3] * w10 +
                disp[i01 + 1] * w01 + disp[i01 + 3] * w11
    }
}


/** How much of this point belongs to the tree, sampled over the cell around it so
 *  the silhouette's edge becomes a gradient the mesh can shear across instead of
 *  a step it would tear at. */
private fun cypressWeight(x: Float, y: Float): Float {
    var hit = 0
    for (j in -1..1) {
        for (i in -1..1) {
            if (inside(CYPRESS, x + i * MESH_CW * 0.5f, y + j * MESH_CH * 0.5f)) hit++
        }
    }
    return hit / 9f
}

/**
 * The moving paint.
 *
 * A stroke is a window of [LIVE_SPAN] points sliding along a streamline it shares
 * with the static painting. Its length is tied to its own position — full in the
 * middle of the run, nothing at either end — so a stroke arrives and leaves by
 * growing and shrinking, and the wrap back to the start happens while it has no
 * length to show. That is why none of this needs a per-stroke alpha, which is
 * also why a dozen draw calls cover seven hundred strokes.
 */
private fun DrawScope.drawLive(
    live: LiveSet, paths: Array<Path>, warp: Warp, off: FloatArray,
    t: Double, stretch: Float, alpha: Float
) {
    for (b in 0 until LIVE_BUCKETS) {
        val from = live.bucketStart[b]
        val to = live.bucketStart[b + 1]
        if (from == to) continue
        val path = paths[b]
        path.rewind()
        var drew = false
        var i = from
        while (i < to) {
            val n = live.count[i]
            // In double, then narrowed. The clock only counts up, and taking
            // this product in float puts back exactly the precision the double
            // accumulator was there to keep.
            var h = ((live.phase[i] + t * live.speed[i]) % 1.0).toFloat()
            if (h < 0f) h += 1f
            // Clamped to what this streamline actually has. A tap stretches the
            // window by nearly two, which is more than the shortest lines can
            // give — unclamped they failed the length test outright and blinked
            // out for the length of the bloom instead of stretching with it.
            val span = min(
                (LIVE_SPAN * (0.34f + 0.66f * sin(PI_F * h)) * stretch).toInt(),
                (n - 2) / LIVE_STRIDE
            )
            val reach = span * LIVE_STRIDE
            if (span >= 2) {
                val head = h * (n - reach - 1)
                val at = head.toInt()
                val frac = head - at
                val base = live.first[i] * 2 + at * 2
                var k = 0
                while (k < span) {
                    val p = base + k * (2 * LIVE_STRIDE)
                    val x = live.pts[p] + (live.pts[p + 2] - live.pts[p]) * frac
                    val y = live.pts[p + 1] + (live.pts[p + 3] - live.pts[p + 1]) * frac
                    // Displaced by the same field as the paint beneath, or a
                    // moving stroke would visibly slide over a moving surface.
                    warp.sample(x, y, off)
                    if (k == 0) path.moveTo(x + off[0], y + off[1])
                    else path.lineTo(x + off[0], y + off[1])
                    k++
                }
                drew = true
            }
            i++
        }
        if (drew) {
            drawPath(
                path, Color(live.bucketColour[b]), alpha = alpha,
                style = liveStyles[live.bucketWidth[b]]
            )
        }
    }
}

private val nightFill = Color(0xFF0B1436)

private const val PARALLAX = 18f
private const val OVERSCAN = 1.035f


@Composable
fun StarryNightCanvas(modifier: Modifier = Modifier) {
    MatchStatusBarToPage(nightFill)

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Read in the draw lambda, so the screen shows its ground colour and then the
    // painting rises out of it the moment the build finishes.
    val painting = remember { mutableStateOf<Painting?>(null) }
    LaunchedEffect(Unit) { painting.value = buildPainting() }

    val touch = remember { Touch() }
    val livePaths = remember { Array(LIVE_BUCKETS) { Path() } }
    val meshPaint = remember { AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG) }
    val offset = remember { FloatArray(2) }

    val assemble = remember { Animatable(0f) }
    val look = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val surge = remember { Animatable(0f) }
    
    val flowTime = remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val tick: (Long) -> Unit = { now ->
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            val entry = 1f - assemble.value
            flowTime.doubleValue += (dt * (1f + 3.2f * entry * entry +
                    1.1f * look.value.getDistance() + 2.4f * surge.value)).toDouble()
            val decay = 1f - min(1f, dt * 4.2f)
            touch.vx *= decay
            touch.vy *= decay
            if (!(abs(touch.vx) >= 0.05f) && !(abs(touch.vy) >= 0.05f)) {
                touch.vx = 0f
                touch.vy = 0f
            }
            if (touch.ripple >= 0f) {
                touch.ripple += dt
                if (touch.ripple > RIPPLE_LIFE) touch.ripple = -1f
            }
        }
        while (true) withFrameNanos(tick)
    }

    LaunchedEffect(painting.value) {
        if (painting.value != null) assemble.animateTo(1f, tween(1800, easing = LinearEasing))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // Tap before drag, and the order is load-bearing. Pointer events
            // reach a chain's inner node first on the main pass, and
            // detectTapGestures consumes the down it sees — put it inside the
            // drag detector and awaitTouchSlopOrCancellation finds the change
            // already consumed and cancels, so the drag silently never fires.
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val u = max(size.width / ART_W, size.height / ART_H) * OVERSCAN
                    touch.tapX = (p.x - (size.width - ART_W * u) / 2f) / u
                    touch.tapY = (p.y - (size.height - ART_H * u) / 2f) / u
                    touch.ripple = 0f
                    scope.launch {
                        surge.snapTo(1f)
                        surge.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 26f))
                    }
                }
            }
            .pointerInput(Unit) {
                // Art coordinates, so the mesh does not have to know about the
                // window. The cover transform below is the inverse of this.
                fun toArt(p: Offset): Offset {
                    val u = max(size.width / ART_W, size.height / ART_H) * OVERSCAN
                    return Offset(
                        (p.x - (size.width - ART_W * u) / 2f) / u,
                        (p.y - (size.height - ART_H * u) / 2f) / u
                    )
                }

                fun norm(p: Offset) = Offset(
                    (p.x / size.width - 0.5f) * 2f,
                    (p.y / size.height - 0.5f) * 2f
                )

                val follow = spring<Offset>(dampingRatio = 0.78f, stiffness = 130f)
                detectDragGestures(
                    onDragStart = {
                        val a = toArt(it)
                        touch.x = a.x
                        touch.y = a.y
                        scope.launch { look.animateTo(norm(it), follow) }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        val a = toArt(change.position)
                        touch.x = a.x
                        touch.y = a.y
                        // Accumulated, not assigned: a fast drag should pile up
                        // more push than a slow one covering the same ground.
                        val u = max(size.width / ART_W, size.height / ART_H) * OVERSCAN
                        touch.vx = (touch.vx + drag.x / u * 0.55f).coerceIn(-PUSH_MAX, PUSH_MAX)
                        touch.vy = (touch.vy + drag.y / u * 0.55f).coerceIn(-PUSH_MAX, PUSH_MAX)
                        scope.launch { look.animateTo(norm(change.position), follow) }
                    },
                    onDragEnd = { scope.launch { look.animateTo(Offset.Zero, follow) } },
                    onDragCancel = { scope.launch { look.animateTo(Offset.Zero, follow) } }
                )
            }
    ) {
        // Fill, not fit: the reference is itself a crop, so covering the screen
        // beats letterboxing it in dark bands. The overscan is there so that the
        // axis the cover-scale fits exactly still has slack for the parallax to
        // travel into; the travel is clamped to it below.
        val u = max(size.width / ART_W, size.height / ART_H) * OVERSCAN
        val art = painting.value

        withTransform({
            translate((size.width - ART_W * u) / 2f, (size.height - ART_H * u) / 2f)
            scale(u, u, pivot = Offset.Zero)
        }) {
            drawRect(nightFill, size = Size(ART_W, ART_H))
            if (art == null) return@withTransform

            val asm = assemble.value
            val flare = surge.value
            val t = flowTime.doubleValue

            // Clamped to the room the cover-scale actually left on each axis
            // rather than to a figure that happens to hold in portrait. Which
            // axis is tight flips with the window: a landscape one is
            // width-limited and leaves 13 art px, well under PARALLAX.
            val slackX = (ART_W * u - size.width) / (2f * u)
            val slackY = (ART_H * u - size.height) / (2f * u)
            val dx = (look.value.x * PARALLAX).coerceIn(-slackX, slackX)
            val dy = (look.value.y * PARALLAX).coerceIn(-slackY, slackY)

            val warp = art.warp
            warp.update(t, touch, flare)

            // The canvas arrives rather than appearing: it comes up out of the
            // ground colour a little oversized and settles, while the flow above
            // is still running fast.
            val entry = (asm / 0.55f).coerceIn(0f, 1f)
            val ease = entry * entry * (3f - 2f * entry)

            withTransform({
                translate(dx, dy)
                if (ease < 1f) {
                    val grow = 1f + 0.04f * (1f - ease)
                    scale(grow, grow, pivot = Offset(ART_W * 0.5f, ART_H * 0.5f))
                }
            }) {
                // Only while the entry is running: after that it is 255 forever
                // and writing it again just dirties a native Paint each frame.
                if (ease < 1f) meshPaint.alpha = (255f * ease).toInt()
                else if (meshPaint.alpha != 255) meshPaint.alpha = 255
                drawIntoCanvas {
                    it.nativeCanvas.drawBitmapMesh(
                        art.canvas, MESH_W, MESH_H, warp.verts, 0, null, 0, meshPaint
                    )
                }

                val live = ((asm - 0.22f) / 0.5f).coerceIn(0f, 1f)
                if (live > 0f) {
                    drawLive(art.live, livePaths, warp, offset, t, 1f + 0.9f * flare, live * 0.9f)
                }
            }

            // The glows lead the rest of the painting when you drag: light that
            // floats a little in front of the canvas is what stops the parallax
            // reading as one flat card sliding about.
            withTransform({
                translate(dx * 1.7f, dy * 1.7f)
                // The same settle as the canvas, or for the first half-second of
                // the entry a glow floats off the star it belongs to.
                if (ease < 1f) {
                    val grow = 1f + 0.04f * (1f - ease)
                    scale(grow, grow, pivot = Offset(ART_W * 0.5f, ART_H * 0.5f))
                }
            }) {
                for (i in STARS.indices) {
                    val star = STARS[i]
                    val born = ((asm - 0.24f - i * 0.05f) / 0.34f).coerceIn(0f, 1f)
                    if (born <= 0f) continue
                    // Two frequencies with no common period, so no star ever
                    // repeats itself and no two of them beat together.
                    val twinkle = 0.70f + 0.30f * (
                            0.62f * sin(t * (0.53 + 0.09 * i) + i * 2.3).toFloat() +
                                    0.38f * sin(t * (0.87 + 0.13 * i) + i * 5.1).toFloat()
                            )
                    val pulse = 1f + 0.06f * (twinkle - 0.74f) / 0.26f + 0.34f * flare
                    warp.sample(star.cx, star.cy, offset)
                    withTransform({
                        translate(offset[0], offset[1])
                        scale(pulse, pulse, pivot = star.centre)
                    }) {
                        drawCircle(
                            star.glow, star.glowRadius, star.centre,
                            alpha = (born * twinkle * (0.8f + 1.5f * flare)).coerceIn(0f, 1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun StarryNightPreview() {
    StarryNightCanvas()
}
