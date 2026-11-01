package com.example.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        com.example.admin.core.platform.JourneyStateStorage.initialize(this)

        com.example.admin.features.subscriptions.RevenueCatSubscriptions.install(BuildConfig.REVENUECAT_API_KEY, BuildConfig.DEBUG)
        if (com.google.firebase.FirebaseApp.initializeApp(this) != null) {
            com.example.admin.features.auth.IdentityServices.firebase = com.example.admin.auth.AndroidFirebaseIdentityProvider(this)
        }
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
