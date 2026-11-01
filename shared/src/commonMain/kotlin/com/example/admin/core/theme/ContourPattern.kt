package com.example.admin.core.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/** Decorative contour study based on the supplied pattern sheet; no geographic meaning. */
@Composable internal fun ContourPattern(modifier:Modifier=Modifier,color:Color=ResQPalette.Mesh.copy(alpha=.18f)) {
    Box(modifier.drawWithCache {
        val radius=min(size.width,size.height)*.46f
        val paths=(1..24).map { ring ->
            Path().apply {
                for(step in 0..160){
                    val angle=step*2*PI/160
                    val fraction=ring/24f
                    val wave=1+.08*sin(3*angle+fraction*2)+.055*cos(5*angle-fraction)
                    val r=radius*fraction*wave
                    val x=(size.width*.5+r*cos(angle)).toFloat()
                    val y=(size.height*.5+r*sin(angle)*.85).toFloat()
                    if(step==0)moveTo(x,y)else lineTo(x,y)
                }
                close()
            }
        }
        onDrawBehind{paths.forEach{drawPath(it,color,style=Stroke(.6f))}}
    })
}
