package com.example.admin.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Semantic tokens transcribed from the supplied Design sys product shots. */
object ResQPalette {
    val Konjo = Color(0xFF043473)
    val Nude = Color(0xFF28518A)
    val Queen = Color(0xFF476998)
    val Grey = Color(0xFF8B9091)
    val Sage = Color(0xFFB2B39D)
    val Mesh = Color(0xFFD7D6AE)
    val Papaya = Color(0xFFE4D076)
}

internal data class PortalAppearance(val dark: Boolean, val toggle: () -> Unit)
internal val LocalPortalAppearance = staticCompositionLocalOf { PortalAppearance(false) {} }

@Composable
fun PortalTheme(dark: Boolean, compact: Boolean, toggle: () -> Unit, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
        primary = ResQPalette.Papaya, onPrimary = ResQPalette.Konjo,
        primaryContainer = Color(0xFF113E78), onPrimaryContainer = Color.White,
        secondary = ResQPalette.Mesh, onSecondary = ResQPalette.Konjo,
        secondaryContainer = Color(0xFF113E78), onSecondaryContainer = ResQPalette.Mesh,
        background = Color(0xFF021C40), onBackground = Color.White,
        surface = ResQPalette.Konjo, onSurface = Color.White,
        surfaceVariant = Color(0xFF113E78), onSurfaceVariant = ResQPalette.Mesh,
        outline = ResQPalette.Queen, outlineVariant = ResQPalette.Nude,
        surfaceContainer = ResQPalette.Konjo, surfaceContainerLow = Color(0xFF021C40),
        surfaceContainerHigh = Color(0xFF113E78), surfaceContainerHighest = Color(0xFF113E78),
    ) else lightColorScheme(
        primary = ResQPalette.Konjo, onPrimary = Color.White,
        primaryContainer = Color(0xFFEAF0F8), onPrimaryContainer = ResQPalette.Konjo,
        secondary = ResQPalette.Nude, onSecondary = Color.White,
        secondaryContainer = ResQPalette.Mesh.copy(alpha = .25f), onSecondaryContainer = ResQPalette.Konjo,
        background = Color(0xFFF8FAFC), onBackground = ResQPalette.Konjo,
        surface = Color.White, onSurface = ResQPalette.Konjo,
        surfaceVariant = Color(0xFFF5F5EB), onSurfaceVariant = ResQPalette.Nude,
        outline = ResQPalette.Queen, outlineVariant = ResQPalette.Grey.copy(alpha = .3f),
        surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF8FAFC),
        surfaceContainerHigh = Color(0xFFF5F5EB), surfaceContainerHighest = Color(0xFFEAF0F8),
    )
    CompositionLocalProvider(LocalPortalAppearance provides PortalAppearance(dark, toggle)) {
        MaterialTheme(colors, shapes = Shapes(
            small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(16.dp),
        ), typography = getAdminTypography(compact)) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}
