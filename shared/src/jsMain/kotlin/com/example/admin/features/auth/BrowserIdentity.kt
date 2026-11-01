package com.example.admin.features.auth

import com.example.admin.core.network.BackendConnection
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.js.Promise

actual fun browserIdentityProvider(): FirebaseIdentityProvider? = FirebaseWebIdentityProvider()

/** Firebase's modular npm SDK owns browser credentials and session refresh.
 * Public app configuration is fetched from the backend and is never generated from a client .env file. */
private class FirebaseWebIdentityProvider : FirebaseIdentityProvider {
    private val initialization = Mutex()
    private val actions = Mutex()
    private var activeAuth: dynamic = null
    private var pendingResolver: dynamic = null
    private var pendingSecret: dynamic = null

    private suspend fun auth(): dynamic = initialization.withLock {
        if (activeAuth != null) return@withLock activeAuth
        val options = js("({ credentials: 'omit', redirect: 'error' })")
        val response = try {
            (window.asDynamic().fetch(BackendConnection.apiOrigin + "/sentinel/config?app=ops", options) as Promise<dynamic>).await()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            error("IDENTITY_CONFIG_UNAVAILABLE")
        }
        if (response.ok != true) error("IDENTITY_CONFIG_UNAVAILABLE")
        val publicConfig = try {
            (response.json() as Promise<dynamic>).await()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            error("IDENTITY_NOT_CONFIGURED")
        }
        val firebase = publicConfig.firebase
        if (firebase == null || (firebase.apiKey as? String).isNullOrBlank() ||
            (firebase.projectId as? String).isNullOrBlank() || (firebase.appId as? String).isNullOrBlank()) {
            error("IDENTITY_NOT_CONFIGURED")
        }
        val configured = getAuth(initializeApp(firebase, "resqops"))
        setPersistence(configured, browserSessionPersistence).await()
        (configured.authStateReady() as Promise<dynamic>).await()
        activeAuth = configured
        configured
    }

    private suspend fun identity(): IdentityResult {
        val user = auth().currentUser ?: return IdentityResult.Error("Sign in to continue.")
        (user.reload() as Promise<dynamic>).await()
        val current = auth().currentUser ?: return IdentityResult.Error("Sign in to continue.")
        val email = current.email as? String ?: return IdentityResult.Error("A verified email address is required.")
        val token = (current.getIdToken(true) as Promise<String>).await()
        return IdentityResult.SignedIn(FirebaseIdentity(
            uid = current.uid as String,
            email = email,
            emailVerified = current.emailVerified as Boolean,
            idToken = token,
        ))
    }

    private suspend fun attempt(block: suspend () -> IdentityResult): IdentityResult = actions.withLock {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (error.asDynamic().code == "auth/multi-factor-auth-required") {
                pendingResolver = getMultiFactorResolver(auth(), error.asDynamic())
                IdentityResult.Error("Enter your authenticator code to finish signing in.", code = "MFA_REQUIRED")
            } else {
                IdentityResult.Error(identityFailureMessage(error.asDynamic().code as? String ?: error.message))
            }
        }
    }

    private fun provider(id: String): dynamic = when (id) {
        "google.com" -> GoogleAuthProvider()
        "apple.com", "microsoft.com" -> OAuthProvider(id)
        "facebook.com" -> FacebookAuthProvider()
        else -> {
            require(Regex("^(saml|oidc)\\.[A-Za-z0-9_-]{1,100}$").matches(id)) { "PROVIDER_NOT_APPROVED" }
            if (id.startsWith("saml.")) SAMLAuthProvider(id) else OAuthProvider(id)
        }
    }

    private fun providerId(provider: SocialProvider) = when (provider) {
        SocialProvider.Google -> "google.com"
        SocialProvider.Apple -> "apple.com"
        SocialProvider.Facebook -> "facebook.com"
        SocialProvider.Microsoft -> "microsoft.com"
    }

    override suspend fun signIn(email: String, password: String) = attempt {
        pendingResolver = null
        pendingSecret = null
        signInWithEmailAndPassword(auth(), email, password).await()
        identity()
    }

    override suspend fun signUp(name: String, email: String, password: String) = attempt {
        val credential = createUserWithEmailAndPassword(auth(), email, password).await()
        val profile = js("({})")
        profile.displayName = name
        updateProfile(credential.user, profile).await()
        identity()
    }

    override suspend fun signInWith(provider: SocialProvider) = attempt {
        signInWithPopup(auth(), provider(providerId(provider))).await()
        identity()
    }

    override suspend fun enterpriseSignIn(providerId: String) = attempt {
        signInWithPopup(auth(), provider(providerId)).await()
        identity()
    }

    override suspend fun completeMfa(code: String) = attempt {
        val resolver = pendingResolver ?: error("NO_PENDING_MFA")
        val hints = resolver.hints as Array<dynamic>
        val hint = hints.firstOrNull { it.factorId == TotpMultiFactorGenerator.FACTOR_ID } ?: error("TOTP_REQUIRED")
        val assertion = TotpMultiFactorGenerator.assertionForSignIn(hint.uid, code)
        (resolver.resolveSignIn(assertion) as Promise<dynamic>).await()
        pendingResolver = null
        identity()
    }

    override suspend fun enrollMfa(code: String?) = attempt {
        val user = auth().currentUser ?: error("SIGN_IN_REQUIRED")
        if (code == null) {
            pendingSecret = null
            val session = (multiFactor(user).getSession() as Promise<dynamic>).await()
            pendingSecret = (TotpMultiFactorGenerator.generateSecret(session) as Promise<dynamic>).await()
            IdentityResult.MfaEnrollmentReady(pendingSecret.secretKey as String)
        } else {
            val secret = pendingSecret ?: error("NO_PENDING_ENROLLMENT")
            val verificationCode = code.filterNot { it.isWhitespace() }
            if (!Regex("^[0-9]{6}$").matches(verificationCode)) error("INVALID_ENROLLMENT_CODE")
            val assertion = TotpMultiFactorGenerator.assertionForEnrollment(secret, verificationCode)
            (multiFactor(user).enroll(assertion, "ResQ authenticator") as Promise<dynamic>).await()
            pendingSecret = null
            signOut(auth()).await()
            IdentityResult.Completed
        }
    }

    override suspend fun linkProvider(provider: SocialProvider) = attempt {
        val user = auth().currentUser ?: error("SIGN_IN_REQUIRED")
        linkWithPopup(user, provider(providerId(provider))).await()
        identity()
    }

    override suspend fun changeEmail(email: String) = attempt {
        val user = auth().currentUser ?: error("SIGN_IN_REQUIRED")
        verifyBeforeUpdateEmail(user, email).await()
        IdentityResult.Completed
    }

    override suspend fun sendPasswordReset(email: String) = attempt {
        try {
            sendPasswordResetEmail(auth(), email).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // Only suppress account existence; connectivity/configuration failures must remain visible.
            if (error.asDynamic().code != "auth/user-not-found") throw error
        }
        IdentityResult.Completed
    }

    override suspend fun confirmPasswordReset(actionCode: String, password: String) = attempt {
        confirmPasswordReset(auth(), actionCode, password).await()
        IdentityResult.Completed
    }

    override suspend fun sendEmailVerification() = attempt {
        val user = auth().currentUser ?: error("SIGN_IN_REQUIRED")
        sendEmailVerification(user).await()
        IdentityResult.Completed
    }

    override suspend fun refreshIdentity() = attempt { identity() }

    override suspend fun signOut() {
        actions.withLock {
            pendingResolver = null
            pendingSecret = null
            try { signOut(auth()).await() } catch (_: Throwable) { /* Local state is cleared by Firebase when configured. */ }
        }
    }
}

/** Fixed messages never expose provider responses, tokens or whether an email has an account. */
private fun identityFailureMessage(code: String?): String = when (code) {
    "IDENTITY_CONFIG_UNAVAILABLE" -> "The sign-in service is unavailable. Please try again later or contact your administrator."
    "IDENTITY_NOT_CONFIGURED", "auth/invalid-api-key", "auth/app-not-authorized", "auth/invalid-app-id",
    "auth/configuration-not-found" -> "Sign-in is not configured for this portal. Contact your administrator."
    "auth/operation-not-allowed" -> "This sign-in method is not enabled. Use your organisation's approved method or contact your administrator."
    "auth/unauthorized-domain" -> "Sign-in is not enabled for this web address. Contact your administrator."
    "auth/invalid-credential", "auth/invalid-login-credentials", "auth/wrong-password", "auth/user-not-found",
    "auth/invalid-email", "auth/user-disabled" -> "Unable to sign in with those details. Check your email and password, or use Forgot password."
    "auth/network-request-failed" -> "Could not reach the sign-in service. Check your connection and try again."
    "auth/too-many-requests" -> "Too many attempts. Wait a few minutes before trying again."
    "auth/popup-blocked" -> "Your browser blocked the sign-in window. Allow pop-ups for this site and try again."
    "auth/popup-closed-by-user", "auth/cancelled-popup-request" -> "The sign-in window was closed. Try again when you're ready."
    "auth/requires-recent-login", "auth/user-token-expired", "SIGN_IN_REQUIRED" -> "Sign in again before continuing."
    "NO_PENDING_ENROLLMENT" -> "Start authenticator setup again, then use the code from the newly added entry."
    "INVALID_ENROLLMENT_CODE" -> "Enter the six-digit code from your authenticator app."
    "auth/invalid-multi-factor-session", "auth/missing-multi-factor-session", "auth/session-expired",
    "auth/invalid-verification-id" -> "Authenticator setup has expired. Sign in again, start a new setup and replace the old authenticator entry."
    "auth/unverified-email" -> "Verify your email, then sign in again before setting up an authenticator."
    "auth/second-factor-already-in-use", "auth/maximum-second-factor-count-exceeded" -> "An authenticator is already registered or the account has reached its limit. Sign in again to check your account."
    "auth/unsupported-first-factor" -> "Sign in with email and password or your approved identity provider before setting up an authenticator."
    "auth/admin-restricted-operation" -> "Authenticator setup is restricted by the project's identity settings. Contact your administrator."
    "auth/invalid-verification-code", "auth/code-expired" -> "That verification code is invalid or expired. Enter a current code and try again."
    "auth/weak-password", "auth/password-does-not-meet-requirements" -> "Choose a stronger password that meets your organisation's requirements."
    "auth/email-already-in-use", "auth/account-exists-with-different-credential", "auth/credential-already-in-use" ->
        "This account could not be set up with that method. Try signing in or use account recovery."
    // Only a bounded SDK error identifier is exposed, never a provider message or payload.
    else -> if (code != null && Regex("^auth/[a-z][a-z0-9-]{1,64}$").matches(code))
        "This request could not be completed. Reference: $code."
    else "This request could not be completed. Try again or contact your administrator."
}
