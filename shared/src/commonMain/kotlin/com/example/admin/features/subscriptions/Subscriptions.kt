package com.example.admin.features.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

const val RESQ_PRO_ENTITLEMENT = "resqops_pro"
const val REVENUECAT_TEST_API_KEY = "test_SjUmggQCSfuDEEcusFoLhwfHqjm"

enum class ProFeature { AgenticAi, Sentinel, ForecastMap, HourlyForecasts, Preparedness, Coordination }
enum class ProPlan(val packageId: String) {
    Lifetime("\$rc_lifetime"), Yearly("\$rc_annual"), Monthly("\$rc_monthly")
}
data class SubscriptionProduct(val plan: ProPlan, val title: String, val price: String)
data class SubscriptionState(
    val available: Boolean = false,
    val userId: String? = null,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val customerInfoLoaded: Boolean = false,
    val entitlementActive: Boolean = false,
    val expirationMillis: Long? = null,
    val willRenew: Boolean = false,
    val billingIssue: Boolean = false,
    val managementUrl: String? = null,
    val products: List<SubscriptionProduct> = emptyList(),
    val message: String? = null,
    val testStore: Boolean = false,
) {
    fun hasAccessAt(nowMillis: Long): Boolean = available && userId != null &&
        customerInfoLoaded && entitlementActive &&
        (expirationMillis == null || nowMillis < expirationMillis)
    val isPro: Boolean get() = hasAccessAt(Clock.System.now().toEpochMilliseconds())
    fun canAccess(feature: ProFeature): Boolean = when (feature) {
        ProFeature.AgenticAi, ProFeature.Sentinel, ProFeature.ForecastMap,
        ProFeature.HourlyForecasts, ProFeature.Preparedness, ProFeature.Coordination -> isPro
    }
}

/** Client presentation only. The API must independently check billing AND role/organization scope. */
interface SubscriptionController {
    val state: StateFlow<SubscriptionState>
    fun identify(userId: String?)
    fun refresh()
    fun restore()
    fun purchase(plan: ProPlan)
    @Composable fun Paywall(onDismiss: () -> Unit)
    @Composable fun CustomerCenter(onDismiss: () -> Unit)
}
object SubscriptionServices {
    var controller: SubscriptionController = UnavailableSubscriptions()
}
class UnavailableSubscriptions(message: String = "In-app purchases are available in the Android and iOS apps.") : SubscriptionController {
    override val state = MutableStateFlow(SubscriptionState(
        message = message
    ))
    override fun identify(userId: String?) = Unit
    override fun refresh() = Unit
    override fun restore() = Unit
    override fun purchase(plan: ProPlan) = Unit
    @Composable override fun Paywall(onDismiss: () -> Unit) { Text(state.value.message.orEmpty()) }
    @Composable override fun CustomerCenter(onDismiss: () -> Unit) { Text(state.value.message.orEmpty()) }
}
