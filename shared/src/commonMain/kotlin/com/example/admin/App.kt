package com.example.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.splash.AdminSplashScreenCanvas
import com.example.admin.core.theme.getAdminTypography
import com.example.admin.core.theme.PortalTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.admin.features.auth.EntryJourney
import com.example.admin.features.operations.LiveWorkspace

/** All targets use the same authenticated workspace; there is no local operations mode. */
@Composable fun App() {
    var dark by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    PortalTheme(dark, maxWidth < 600.dp, { dark = !dark }) {
        var showSplash by remember { mutableStateOf(true) }
        var authenticated by remember { mutableStateOf(false) }
        if(showSplash) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            AdminSplashScreenCanvas(onSplashFinished={showSplash=false},modifier=Modifier.widthIn(max=640.dp).fillMaxWidth().fillMaxHeight())
        } else if(!authenticated) EntryJourney(onAuthenticated={authenticated=true})
        else LiveWorkspace(onLogout={authenticated=false})
    }
    }
}
