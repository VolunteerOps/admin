package com.example.admin.features.auth
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.admin.core.icons.*
import com.example.admin.core.theme.*
import org.jetbrains.compose.resources.painterResource
import admin.shared.generated.resources.*
@Composable fun AdminAuthScreen(onLoginSuccess: () -> Unit) {
    var showAccess by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(AdminColors.Navy).safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Image(painterResource(Res.drawable.app_logo_monochrome), "VolunteerOps", Modifier.size(64.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("VOLUNTEEROPS / COORDINATE", style = MaterialTheme.typography.labelMedium, color = Color(0xFF93C5FD))
                Text("Good coordination.\nGreater impact.", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text("Bring the right people, resources and information together when your community needs them.", color = Color(0xFFCBD5E1))
            }
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SectionTitle("Your response workspace", "Plan missions, support your volunteers and keep every team informed.")
                    OpsButton("Explore the practice workspace", onLoginSuccess, Modifier.fillMaxWidth(), symbol = OpsSymbol.ArrowRight)
                    Text("Sample records · Changes saved on this device", style = MaterialTheme.typography.bodySmall, color = AdminColors.Slate)
                    HorizontalDivider(color = AdminColors.BorderLight)
                    OpsButton("Organisation sign-in", { showAccess = true }, Modifier.fillMaxWidth(), secondary = true, symbol = OpsSymbol.Shield)
                }
            }
            Text("Track, Coordinate, Save Lives", style = MaterialTheme.typography.titleMedium, color = Color(0xFFBFDBFE))
        }
    }
    if (showAccess) DetailSheet("Organisation access", { showAccess = false }) {
        SectionTitle("Connect your organisation", "A verified identity provider and operational backend have not been configured for this build.")
        Text("Ask your organisation's administrator for the managed app configuration. The practice workspace is available now with sample records; it does not send alerts or authenticate an operational account.")
        OpsButton("Explore practice workspace", { showAccess = false; onLoginSuccess() }, Modifier.fillMaxWidth())
    }
}

