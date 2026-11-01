package com.example.admin.core.platform
import androidx.compose.runtime.Composable
@Composable actual fun JourneyBackHandler(enabled: Boolean, onBack: () -> Unit) { androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack) }
