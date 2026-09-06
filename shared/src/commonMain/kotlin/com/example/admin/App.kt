package com.example.admin
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.admin.core.data.AdminRepository
import com.example.admin.core.splash.AdminSplashScreenCanvas
import com.example.admin.core.theme.*
import com.example.admin.features.auth.AdminAuthScreen
import com.example.admin.features.navigation.AdminNavigationContainer
@Composable fun App() {
    val repository = remember { AdminRepository() }
    var stage by rememberSaveable { mutableStateOf("splash") }
    MaterialTheme(colorScheme = lightColorScheme(primary = AdminColors.VolunteerBlue, onPrimary = AdminColors.CleanWhite, primaryContainer = Color(0xFFEAF1FF), onPrimaryContainer = AdminColors.VolunteerBlue, secondaryContainer = Color(0xFFEAF1FF), onSecondaryContainer = AdminColors.VolunteerBlue, secondary = AdminColors.HealingTeal, background = AdminColors.CanvasBg, surface = AdminColors.SurfaceWhite, onSurface = AdminColors.Navy, onBackground = AdminColors.Navy, outline = AdminColors.BorderLight, error = AdminColors.CompassionCoral), typography = getAdminTypography()) {
        Crossfade(stage, animationSpec = tween(300)) { current ->
            when(current) {
                "splash" -> AdminSplashScreenCanvas(onSplashFinished = { stage = "auth" }, modifier = Modifier.background(AdminColors.Navy).safeDrawingPadding())
                "auth" -> AdminAuthScreen { stage = "main" }
                else -> AdminNavigationContainer(repository) { stage = "auth" }
            }
        }
    }
}

