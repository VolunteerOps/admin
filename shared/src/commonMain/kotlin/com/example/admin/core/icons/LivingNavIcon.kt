package com.example.admin.core.icons
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.*

private data class Part(val path: Path, val width: Float)
private data class Drawing(val origin: Float, val parts: List<Part>)
/** One short story per selection; animation reads are confined to the draw phase. */
@Composable fun LivingNavIcon(symbol: OpsSymbol, active: Boolean, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, reducedMotion: Boolean = false) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active, reducedMotion) {
        progress.snapTo(0f)
        if (active && !reducedMotion) progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    val drawing = remember(symbol) { when(symbol) { OpsSymbol.Home -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M25.02 18.8399L19.63 23.0399C18.73 23.7399 18 25.2299 18 26.3599V33.7699C18 36.0899 19.89 37.9899 22.21 37.9899H33.79C36.11 37.9899 38 36.0899 38 33.7799V26.4999C38 25.2899 37.19 23.7399 36.2 23.0499L30.02 18.7199C28.62 17.7399 26.37 17.7899 25.02 18.8399Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M28 33.99V30.99").toPath(), 1.5f)))
OpsSymbol.Search -> Drawing(-16.5f, listOf(Part(PathParser().parsePathString("M27.5 37C32.7467 37 37 32.7467 37 27.5C37 22.2533 32.7467 18 27.5 18C22.2533 18 18 22.2533 18 27.5C18 32.7467 22.2533 37 27.5 37Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M38 38L36 36").toPath(), 1.5f)))
OpsSymbol.Mission -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M21.1499 18V38").toPath(), 1.5f),
Part(PathParser().parsePathString("M21.1499 20H32.3499C35.0499 20 35.6499 21.5 33.7499 23.4L32.5499 24.6C31.7499 25.4 31.7499 26.7 32.5499 27.4L33.7499 28.6C35.6499 30.5 34.9499 32 32.3499 32H21.1499").toPath(), 1.5f)))
OpsSymbol.Messages -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M33.98 26.79V30.79C33.98 31.05 33.97 31.3 33.94 31.54C33.71 34.24 32.12 35.58 29.19 35.58H28.79C28.54 35.58 28.3 35.7 28.15 35.9L26.95 37.5C26.42 38.21 25.56 38.21 25.03 37.5L23.83 35.9C23.7 35.73 23.41 35.58 23.19 35.58H22.79C19.6 35.58 18 34.79 18 30.79V26.79C18 23.86 19.35 22.27 22.04 22.04C22.28 22.01 22.53 22 22.79 22H29.19C32.38 22 33.98 23.6 33.98 26.79Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M37.98 22.79V26.79C37.98 29.73 36.63 31.31 33.94 31.54C33.97 31.3 33.98 31.05 33.98 30.79V26.79C33.98 23.6 32.38 22 29.19 22H22.79C22.53 22 22.28 22.01 22.04 22.04C22.27 19.35 23.86 18 26.79 18H33.19C36.38 18 37.98 19.6 37.98 22.79Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M29.4955 29.25H29.5045").toPath(), 2f),
Part(PathParser().parsePathString("M25.9955 29.25H26.0045").toPath(), 2f),
Part(PathParser().parsePathString("M22.4955 29.25H22.5045").toPath(), 2f)))
OpsSymbol.Profile -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M28 28C30.7614 28 33 25.7614 33 23C33 20.2386 30.7614 18 28 18C25.2386 18 23 20.2386 23 23C23 25.7614 25.2386 28 28 28Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M36.5901 38C36.5901 34.13 32.7402 31 28.0002 31C23.2602 31 19.4102 34.13 19.4102 38").toPath(), 1.5f)))
OpsSymbol.People -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M34.0001 23.16C33.9401 23.15 33.8701 23.15 33.8101 23.16C32.4301 23.11 31.3301 21.98 31.3301 20.58C31.3301 19.15 32.4801 18 33.9101 18C35.3401 18 36.4901 19.16 36.4901 20.58C36.4801 21.98 35.3801 23.11 34.0001 23.16Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M32.9704 30.4402C34.3404 30.6702 35.8504 30.4302 36.9104 29.7202C38.3204 28.7802 38.3204 27.2402 36.9104 26.3002C35.8404 25.5902 34.3104 25.3502 32.9404 25.5902").toPath(), 1.5f),
Part(PathParser().parsePathString("M21.9705 23.16C22.0305 23.15 22.1005 23.15 22.1605 23.16C23.5405 23.11 24.6405 21.98 24.6405 20.58C24.6405 19.15 23.4905 18 22.0605 18C20.6305 18 19.4805 19.16 19.4805 20.58C19.4905 21.98 20.5905 23.11 21.9705 23.16Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M23.0004 30.4402C21.6304 30.6702 20.1204 30.4302 19.0604 29.7202C17.6504 28.7802 17.6504 27.2402 19.0604 26.3002C20.1304 25.5902 21.6604 25.3502 23.0304 25.5902").toPath(), 1.5f),
Part(PathParser().parsePathString("M28.0001 30.6302C27.9401 30.6202 27.8701 30.6202 27.8101 30.6302C26.4301 30.5802 25.3301 29.4502 25.3301 28.0502C25.3301 26.6202 26.4801 25.4702 27.9101 25.4702C29.3401 25.4702 30.4901 26.6302 30.4901 28.0502C30.4801 29.4502 29.3801 30.5902 28.0001 30.6302Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M25.0897 33.7804C23.6797 34.7204 23.6797 36.2603 25.0897 37.2003C26.6897 38.2703 29.3097 38.2703 30.9097 37.2003C32.3197 36.2603 32.3197 34.7204 30.9097 33.7804C29.3197 32.7204 26.6897 32.7204 25.0897 33.7804Z").toPath(), 1.5f)))
OpsSymbol.More -> Drawing(-16.0f, listOf(Part(PathParser().parsePathString("M21 26C19.9 26 19 26.9 19 28C19 29.1 19.9 30 21 30C22.1 30 23 29.1 23 28C23 26.9 22.1 26 21 26Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M35 26C33.9 26 33 26.9 33 28C33 29.1 33.9 30 35 30C36.1 30 37 29.1 37 28C37 26.9 36.1 26 35 26Z").toPath(), 1.5f),
Part(PathParser().parsePathString("M28 26C26.9 26 26 26.9 26 28C26 29.1 26.9 30 28 30C29.1 30 30 29.1 30 28C30 26.9 29.1 26 28 26Z").toPath(), 1.5f)))
        else -> null
    } }
    if (drawing == null) { OpsIcon(symbol, modifier, tint); return }
    val flag = remember { Path() }
    Canvas(modifier.size(24.dp)) {
        val p = progress.value
        val envelope = if (active && !reducedMotion && p > 0f && p < 1f) sin(p * PI).toFloat() else 0f
        val wave = sin(p * PI * 6).toFloat() * envelope
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            translate(drawing.origin, -16f) {
                drawing.parts.forEachIndexed { index, part ->
                    val stroke = Stroke(part.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    when {
                        symbol == OpsSymbol.Mission && index == 1 && envelope > 0f -> {
                            // The pole stays planted while the fabric catches two gusts.
                            flag.reset(); flag.moveTo(21.15f,20f)
                            flag.cubicTo(26f,20f+wave,30f,20f-wave,33.8f,20f+wave)
                            flag.lineTo(31.8f,26f+wave); flag.lineTo(33.8f,32f+wave)
                            flag.cubicTo(29f,32f-wave,25f,32f+wave,21.15f,32f)
                            drawPath(flag,tint,style=stroke)
                        }
                        symbol == OpsSymbol.Messages && index >= 2 -> {
                            val lift = max(0f,sin(p*PI.toFloat()*8f-index*.9f))*envelope
                            translate(top=-lift*2f) { drawPath(part.path,tint,style=Stroke(part.width+lift*.4f,cap=StrokeCap.Round)) }
                        }
                        symbol == OpsSymbol.Profile && index == 0 -> translate(left=wave*.7f,top=abs(wave)*.7f) { drawPath(part.path,tint,style=stroke) }
                        symbol == OpsSymbol.People -> translate(top=if(index%2==0) -max(0f,sin(p*PI.toFloat()*6f-index))*envelope else 0f) { drawPath(part.path,tint,style=stroke) }
                        else -> drawPath(part.path,tint,style=stroke)
                    }
                }
                if (envelope > 0f) when(symbol) {
                    OpsSymbol.Search -> { // A lens sweep reveals a found point.
                        val y=21f+13f*p
                        drawLine(tint.copy(alpha=envelope*.65f),Offset(23f,y),Offset(32f,y),1f,StrokeCap.Round)
                        drawCircle(tint.copy(alpha=envelope),1.2f,Offset(28f,27f))
                    }
                    OpsSymbol.Home -> { // The doorway lights up as somebody arrives home.
                        drawRoundRect(tint.copy(alpha=envelope*.35f),Offset(25.5f,29f),androidx.compose.ui.geometry.Size(5f,7f),androidx.compose.ui.geometry.CornerRadius(1f))
                        drawLine(tint.copy(alpha=envelope),Offset(28f,31f),Offset(28f+2f*envelope,34f),1.2f,StrokeCap.Round)
                    }
                    OpsSymbol.More -> { // Three tools unfold from the menu and tuck away.
                        for(i in 0..2) drawCircle(tint.copy(alpha=envelope*.65f),.8f,Offset(21f+i*7f,28f-5f*envelope))
                    }
                    else -> Unit
                }
            }
        }
    }
}
