package com.example.admin.core.theme
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
enum class GlassVariant { PRIMARY, WARNING, CRITICAL, INFO, DARK }
private fun glassColor(v: GlassVariant) = when(v) { GlassVariant.PRIMARY -> AdminColors.HealingTeal; GlassVariant.WARNING -> AdminColors.WarningAmber; GlassVariant.CRITICAL -> AdminColors.CompassionCoral; GlassVariant.INFO -> AdminColors.VolunteerBlue; GlassVariant.DARK -> AdminColors.Slate }
fun Modifier.liquidGlass(shape: Shape = RoundedCornerShape(50), tintColor: Color = AdminColors.VolunteerBlue, borderWidth: Dp = 1.dp): Modifier = clip(shape).background(tintColor.copy(alpha = .08f)).drawWithCache {
    val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha=.75f), Color.White.copy(alpha=.08f)))
    onDrawBehind { drawRect(sheen) }
}.border(borderWidth, tintColor.copy(alpha=.22f), shape)
fun Modifier.liquidGlass(tint: Color, shape: Shape = RoundedCornerShape(50), borderWidth: Dp = 1.dp): Modifier = liquidGlass(shape,tint,borderWidth)
@Composable fun LiquidGlassBadge(text: String, variant: GlassVariant = GlassVariant.INFO, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null) {
    Row(modifier.liquidGlass(tintColor = glassColor(variant)).padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { leadingIcon?.invoke(); Text(displayStatus(text), style = MaterialTheme.typography.labelMedium, color = glassColor(variant)) }
}
@Composable fun LiquidGlassBadge(text: String, statusColor: Color, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null) { Row(modifier.liquidGlass(tintColor=statusColor).padding(10.dp,5.dp)) { leadingIcon?.invoke(); Text(text, style=MaterialTheme.typography.labelMedium, color=statusColor) } }
@Composable fun LiquidGlassTag(text: String, modifier: Modifier = Modifier) { LiquidGlassBadge(text, GlassVariant.DARK, modifier) }
@Composable fun LiquidGlassAlert(title: String, message: String, variant: GlassVariant = GlassVariant.INFO, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp), glassColor(variant)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = glassColor(variant))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AdminColors.Navy)
    }
}
@Composable fun StatusBadge(status: String) { LiquidGlassBadge(status, when(status.lowercase()) { "critical", "unavailable" -> GlassVariant.CRITICAL; "high", "standby", "pending", "contained", "deployed" -> GlassVariant.WARNING; "active", "available", "completed", "resolved" -> GlassVariant.PRIMARY; else -> GlassVariant.INFO }) }

