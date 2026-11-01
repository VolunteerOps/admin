package com.example.admin.core.theme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import admin.shared.generated.resources.*

@Composable fun getOswaldFontFamily() = FontFamily(Font(Res.font.oswald_regular), Font(Res.font.oswald_medium, FontWeight.Medium), Font(Res.font.oswald_semibold, FontWeight.SemiBold), Font(Res.font.oswald_bold, FontWeight.Bold))
@Composable fun getOverlockFontFamily() = FontFamily(Font(Res.font.overlock_regular), Font(Res.font.overlock_bold, FontWeight.Bold), Font(Res.font.overlock_italic, FontWeight.Normal, FontStyle.Italic), Font(Res.font.overlock_black, FontWeight.Black))
@Composable fun getAdminTypography(compact: Boolean = false): Typography {
    val title = getOswaldFontFamily()
    val body = getOverlockFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = title, fontWeight = FontWeight.Bold, fontSize = (if(compact)40 else 56).sp, lineHeight = (if(compact)48 else 64).sp),
        headlineLarge = TextStyle(fontFamily = title, fontWeight = FontWeight.Bold, fontSize = (if(compact)32 else 40).sp, lineHeight = (if(compact)40 else 48).sp),
        headlineMedium = TextStyle(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = (if(compact)24 else 32).sp, lineHeight = (if(compact)32 else 40).sp),
        headlineSmall = TextStyle(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = (if(compact)20 else 24).sp, lineHeight = (if(compact)28 else 32).sp),
        titleLarge = TextStyle(fontFamily = title, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = body, fontSize = (if(compact)16 else 18).sp, lineHeight = 26.sp),
        bodyMedium = TextStyle(fontFamily = body, fontSize = (if(compact)14 else 16).sp, lineHeight = 24.sp),
        bodySmall = TextStyle(fontFamily = body, fontSize = (if(compact)13 else 14).sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp)
    )
}


