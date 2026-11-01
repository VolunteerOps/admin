package com.example.admin.features.subscriptions

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.*
import com.revenuecat.purchases.kmp.models.*
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall as RevenueCatPaywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.CustomerCenter as RevenueCatCustomerCenter
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** One process-lifetime instance. All entry points are invoked on the UI thread. */
class RevenueCatSubscriptions private constructor(private val sdk: Purchases, testStore: Boolean) : SubscriptionController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(SubscriptionState(available = true, testStore = testStore))
    override val state = mutableState.asStateFlow()
    private var generation = 0L
    private var offering: Offering? = null
    private var expiryJob: Job? = null
    private var lastCustomerInfo: CustomerInfo? = null

    init {
        sdk.delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                // A delegate payload has no current UID. Fetch for the current session instead,
                // preventing an old account's callback from granting a new account access.
                scope.launch { if (!state.value.busy && !state.value.loading) refresh() }
            }
            override fun onPurchasePromoProduct(product: StoreProduct,
                startPurchase: ((PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit) -> Unit) {
                // Store promotions are deferred; purchases start from the explicit in-app paywall.
            }
        }
    }

    override fun identify(userId: String?) {
        val uid = userId?.takeIf { it.isNotBlank() }
        if (state.value.userId == uid && uid != null && state.value.customerInfoLoaded) return
        val ticket = ++generation
        expiryJob?.cancel()
        lastCustomerInfo = null
        offering = null
        mutableState.value = SubscriptionState(available = true, userId = uid,
            loading = true, testStore = state.value.testStore)
        scope.launch {
            mutex.withLock {
                if (ticket != generation) return@withLock
                try {
                    if (uid == null) {
                        if (!sdk.isAnonymous) sdk.awaitLogOut()
                    } else {
                        val info = if (sdk.appUserID == uid) sdk.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
                            else sdk.awaitLogIn(uid).customerInfo
                        accept(info, ticket)
                        loadOffering(ticket)
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { failure(e, ticket) }
                finally { if (ticket == generation) mutableState.value = state.value.copy(loading = false) }
            }
        }
    }

    override fun refresh() {
        if (state.value.userId == null || state.value.loading || state.value.busy) return
        if (!state.value.customerInfoLoaded || sdk.appUserID != state.value.userId) {
            identify(state.value.userId)
            return
        }
        execute {
            accept(sdk.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT), it)
            loadOffering(it)
        }
    }

    override fun restore() = execute { ticket ->
        val info = sdk.awaitRestore()
        accept(info, ticket)
        if (ticket == generation) mutableState.value = state.value.copy(message =
            if (state.value.isPro) "Purchases restored. ResQOps Pro is active."
            else "No active ResQOps Pro purchase was found for this store account.")
    }

    /** Optional custom purchase UI; RevenueCat Paywall handles its own purchases. */
    override fun purchase(plan: ProPlan) = execute { ticket ->
        if (state.value.isPro) {
            mutableState.value = state.value.copy(message = "Use Manage subscription to change your existing plan.")
            return@execute
        }
        val rcPackage = offering?.availablePackages?.firstOrNull { it.identifier == plan.packageId }
        if (rcPackage == null) {
            mutableState.value = state.value.copy(message = "This plan is unavailable. Refresh plans and try again.")
            return@execute
        }
        purchaseCompleted(sdk.awaitPurchase(packageToPurchase = rcPackage).customerInfo, ticket)
    }

    private fun execute(block: suspend (Long) -> Unit) {
        if (state.value.userId == null || state.value.busy || state.value.loading) return
        val ticket = generation
        mutableState.value = state.value.copy(busy = true, message = null)
        scope.launch {
            mutex.withLock {
                if (ticket != generation) return@withLock
                try {
                    if (sdk.appUserID != state.value.userId) {
                        mutableState.value = state.value.copy(customerInfoLoaded = false,
                            entitlementActive = false, message = "Sign in again to reconnect purchases.")
                    } else block(ticket)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { failure(e, ticket) }
                finally { if (ticket == generation) mutableState.value = state.value.copy(busy = false) }
            }
        }
    }

    private suspend fun loadOffering(ticket: Long) {
        // Explicit role-specific offering; never fall back to another product tier.
        val current = sdk.awaitOfferings().all["default"]
        if (ticket != generation) return
        offering = current
        val products = ProPlan.entries.mapNotNull { plan ->
            current?.availablePackages?.firstOrNull { it.identifier == plan.packageId }?.let {
                SubscriptionProduct(plan, it.storeProduct.title, it.storeProduct.price.formatted)
            }
        }
        mutableState.value = state.value.copy(products = products, message =
            if (products.isEmpty()) "Plans are not available yet. Please try again later." else state.value.message)
    }

    private fun purchaseCompleted(info: CustomerInfo, ticket: Long) {
        accept(info, ticket)
        if (ticket == generation && !state.value.isPro) mutableState.value = state.value.copy(
            message = "The purchase completed, but Pro access is not active yet. Refresh status or contact support; do not purchase again.")
    }

    private fun accept(info: CustomerInfo, ticket: Long) {
        if (ticket != generation || state.value.userId == null || sdk.appUserID != state.value.userId) return
        if (lastCustomerInfo?.requestDateMillis?.let { it > info.requestDateMillis } == true) return
        lastCustomerInfo = info
        val entitlement = info.entitlements[RESQ_PRO_ENTITLEMENT]
        mutableState.value = state.value.copy(customerInfoLoaded = true,
            entitlementActive = entitlement?.isActive == true,
            expirationMillis = entitlement?.expirationDateMillis,
            willRenew = entitlement?.willRenew == true,
            billingIssue = entitlement?.billingIssueDetectedAtMillis != null,
            managementUrl = info.managementUrlString, message = null)
        expiryJob?.cancel()
        entitlement?.expirationDateMillis?.let { expires ->
            val remaining = expires - Clock.System.now().toEpochMilliseconds()
            if (remaining > 0 && entitlement.isActive) expiryJob = scope.launch {
                delay(remaining)
                if (ticket == generation) {
                    mutableState.value = state.value.copy(entitlementActive = false)
                    refresh()
                }
            }
        }
    }

    private fun failure(error: Exception, ticket: Long) {
        if (ticket != generation) return
        if (error is PurchasesTransactionException && error.userCancelled) {
            mutableState.value = state.value.copy(message = null)
            return
        }
        val message = (error as? PurchasesException)?.error?.let(::messageFor)
            ?: "Could not connect to subscriptions. Please try again."
        mutableState.value = state.value.copy(message = message)
    }

    @Composable override fun Paywall(onDismiss: () -> Unit) {
        val currentState by state.collectAsState()
        val ticket = generation
        val currentOffering = offering
        if (currentState.userId == null || !currentState.customerInfoLoaded ||
            sdk.appUserID != currentState.userId || currentOffering == null) {
            Text("Sign in and refresh your plans before purchasing.")
            return
        }
        if (currentState.isPro) {
            LaunchedEffect(Unit) { onDismiss() }
            return
        }
        val options = remember(ticket, currentOffering, onDismiss) {
            PaywallOptions(dismissRequest = onDismiss) {
                offering = currentOffering
                shouldDisplayDismissButton = true
                listener = object : PaywallListener {
                    override fun onPurchaseStarted(rcPackage: com.revenuecat.purchases.kmp.models.Package) { setBusy(true) }
                    override fun onRestoreStarted() { setBusy(true) }
                    override fun onPurchaseCancelled() { setBusy(false) }
                    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                        purchaseCompleted(customerInfo, ticket)
                        setBusy(false)
                    }
                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        accept(customerInfo, ticket)
                        setBusy(false)
                        if (ticket == generation && !state.value.isPro) mutableState.value = state.value.copy(
                            message = "No active ResQOps Pro purchase was found for this store account.")
                    }
                    override fun onPurchaseError(error: PurchasesError) { report(error) }
                    override fun onRestoreError(error: PurchasesError) { report(error) }
                    private fun setBusy(busy: Boolean) {
                        if (ticket == generation) mutableState.value = state.value.copy(busy = busy)
                    }
                    private fun report(error: PurchasesError) {
                        if (ticket == generation) mutableState.value = state.value.copy(busy = false, message = messageFor(error))
                    }
                }
            }
        }
        RevenueCatPaywall(options)
    }

    @Composable override fun CustomerCenter(onDismiss: () -> Unit) {
        val currentState by state.collectAsState()
        if (currentState.userId == null || !currentState.customerInfoLoaded || sdk.appUserID != currentState.userId) {
            Text("Sign in and refresh subscription status to manage purchases.")
            return
        }
        if (currentState.testStore) { Text("Subscription management is available with App Store and Google Play purchases."); return }
        RevenueCatCustomerCenter(onDismiss = { refresh(); onDismiss() })
    }

    companion object {
        /** Public client key only. A test key can never configure a production binary. */
        fun install(apiKey: String, debug: Boolean) {
            if (SubscriptionServices.controller is RevenueCatSubscriptions) return
            if (!Regex("(test|goog|appl)_[A-Za-z0-9]+").matches(apiKey) || (!debug && apiKey.startsWith("test_"))) {
                SubscriptionServices.controller = UnavailableSubscriptions("Subscriptions are not configured for this build.")
                return
            }
            try {
                Purchases.logLevel = if (debug) LogLevel.DEBUG else LogLevel.ERROR
                val sdk = if (Purchases.isConfigured) Purchases.sharedInstance else Purchases.configure(apiKey)
                SubscriptionServices.controller = RevenueCatSubscriptions(sdk, apiKey.startsWith("test_"))
            } catch (e: Exception) {
                SubscriptionServices.controller = UnavailableSubscriptions("Subscriptions could not start. Please restart the app.")
            }
        }
    }
}

private fun messageFor(error: PurchasesError): String = when (error.code) {
    PurchasesErrorCode.PurchaseCancelledError -> "Purchase cancelled."
    PurchasesErrorCode.PaymentPendingError -> "Payment is pending approval. Pro unlocks after payment is confirmed."
    PurchasesErrorCode.NetworkError -> "Could not refresh subscriptions. Check your connection and try again."
    PurchasesErrorCode.ProductAlreadyPurchasedError -> "This product is already owned. Try Restore purchases."
    PurchasesErrorCode.ReceiptAlreadyInUseError, PurchasesErrorCode.ReceiptInUseByOtherSubscriberError ->
        "This purchase belongs to another ResQ account. Sign in to the original account."
    PurchasesErrorCode.ConfigurationError, PurchasesErrorCode.ProductNotAvailableForPurchaseError ->
        "This plan is currently unavailable. Please try again later."
    PurchasesErrorCode.PurchaseNotAllowedError -> "Purchases are not allowed for this store account or device."
    else -> "The store could not complete the request. Please try again."
}
