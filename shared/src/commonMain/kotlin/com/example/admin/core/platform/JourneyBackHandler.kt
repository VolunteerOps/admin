package com.example.admin.core.platform
import androidx.compose.runtime.Composable
@Composable expect fun JourneyBackHandler(enabled: Boolean, onBack: () -> Unit)
