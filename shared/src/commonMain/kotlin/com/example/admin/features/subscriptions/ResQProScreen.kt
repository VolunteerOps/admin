package com.example.admin.features.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.admin.core.platform.JourneyBackHandler

@Composable
fun ResQProScreen(controller: SubscriptionController = SubscriptionServices.controller) {
    val state by controller.state.collectAsState()
    var kes by remember { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf("overview") }
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(controller) {
        controller.refresh()
        onPauseOrDispose { }
    }
    // Close store UI immediately when the identity changes.
    LaunchedEffect(state.userId) { destination = "overview" }
    fun dismiss() { destination = "overview"; controller.refresh() }
    JourneyBackHandler(destination != "overview", ::dismiss)
    if (destination != "overview") {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = ::dismiss) { Text("Back to ResQOps Pro") }
            Box(Modifier.weight(1f)) {
                if (destination == "paywall") controller.Paywall(::dismiss)
                else controller.CustomerCenter(::dismiss)
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ResQOps Pro", style = MaterialTheme.typography.headlineMedium)
        com.example.admin.core.onboarding.OnboardingArtwork(1, Modifier.fillMaxWidth().heightIn(max=260.dp).aspectRatio(1.6f), reducedMotion=true)
        Row { FilterChip(selected=!kes,onClick={kes=false},label={Text("USD")}); Spacer(Modifier.width(8.dp)); FilterChip(selected=kes,onClick={kes=true},label={Text("KSh")}) }
        val catalog = listOf("Monthly" to if(kes)"KSh 2,600" else "US$19.99", "Yearly" to if(kes)"KSh 20,800" else "US$159.99", "Lifetime" to if(kes)"KSh 78,000" else "US$599.99")
        catalog.forEach { (period,price) -> OutlinedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(period);Text(price)} } }
        Text("Planned catalog prices. The store shows the final price, taxes and billing currency before you pay. Lifetime is a one-time purchase; hosted AI and forecasts have usage limits.",style=MaterialTheme.typography.bodySmall)
        state.products.forEach { product -> Text(product.title + " ? " + product.price) }
        Row { TextButton(onClick={uriHandler.openUri("https://resq-web-staging.up.railway.app/terms")}){Text("Terms")}; TextButton(onClick={uriHandler.openUri("https://resq-web-staging.up.railway.app/privacy")}){Text("Privacy")} }

        Text(if (state.isPro) "Your ResQOps Pro access is active" else "Prepare and coordinate with ResQOps Pro",
            style = MaterialTheme.typography.titleLarge)
        Text("For volunteers, coordinators and administrators, within your organization's permissions.")
        Text("- Agentic AI assistance grounded in operational evidence\n" +
            "- Sentinel hazard updates and preparedness recommendations\n" +
            "- Global forecast map and hourly forecast views where data is available\n" +
            "- Volunteer coordination and response planning")
        Text("Forecasts show source coverage, freshness and uncertainty. Official authorities issue warnings; deployments require administrator approval. Provider access and pilot availability vary.")
        if (state.testStore) Text("Test Store: purchases are simulated", color = MaterialTheme.colorScheme.primary)
        if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it) }
        linkError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.available && state.userId == null) Text("Sign in to your ResQ account to buy or restore Pro.")
        if (state.isPro) {
            Text(when {
                state.expirationMillis == null -> "Lifetime access: no renewal"
                state.willRenew -> "Subscription renews automatically until cancelled."
                else -> "Renewal is off. Access continues until the paid period ends."
            })
            if (state.billingIssue) Text("There is a billing issue. Review your payment method in subscription management.")
        }
        val enabled = state.available && state.userId != null && state.customerInfoLoaded && !state.loading && !state.busy
        if (!state.isPro) Button(onClick = { destination = "paywall" }, enabled = enabled && state.products.isNotEmpty()) {
            Text("View plans")
        }
        OutlinedButton(onClick = controller::restore, enabled = enabled) { Text("Restore purchases") }
        OutlinedButton(onClick = { destination = "center" }, enabled = enabled && !state.testStore) {
            Text("Manage subscription")
        }
        state.managementUrl?.let { url ->
            TextButton(onClick = {
                linkError = runCatching { uriHandler.openUri(url) }.exceptionOrNull()
                    ?.let { "Could not open the store. Try Manage subscription." }
            }) { Text("Open store subscription settings") }
        }
        TextButton(onClick = controller::refresh, enabled = state.available && !state.busy && !state.loading) {
            Text("Refresh subscription status")
        }
    }
}

@Composable
fun ResQProGate(feature: ProFeature, content: @Composable () -> Unit) {
    val controller = SubscriptionServices.controller
    val state by controller.state.collectAsState()
    LifecycleResumeEffect(controller) {
        controller.refresh()
        onPauseOrDispose { }
    }
    if (state.canAccess(feature)) content() else ResQProScreen(controller)
}
