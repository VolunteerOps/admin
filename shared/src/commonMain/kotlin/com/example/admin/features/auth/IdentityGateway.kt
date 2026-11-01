package com.example.admin.features.auth

/** Firebase owns credentials. Only a verified backend database membership grants app access. */
val OPERATIONS_API_BASE_URL: String get() = com.example.admin.core.network.BackendConnection.apiOrigin
enum class SocialProvider { Google, Apple, Facebook, Microsoft }
data class FirebaseIdentity(val uid: String, val email: String, val emailVerified: Boolean, val idToken: String)
data class Membership(val userId: String, val role: String, val organizationId: String?, val active: Boolean, val approved: Boolean, val setupComplete: Boolean = false)
sealed interface IdentityResult {
    data class SignedIn(val identity: FirebaseIdentity) : IdentityResult
    /** Ephemeral enrollment material; never include it in logs or saved UI state. */
    class MfaEnrollmentReady(val secretKey: String) : IdentityResult {
        override fun toString(): String = "MfaEnrollmentReady([redacted])"
    }
    data class Error(val message: String, val code: String? = null) : IdentityResult
    data object Completed : IdentityResult
}
expect fun browserIdentityProvider(): FirebaseIdentityProvider?
interface FirebaseIdentityProvider {
    suspend fun enterpriseSignIn(providerId:String):IdentityResult = IdentityResult.Error("Institution SSO requires the configured browser adapter.")
    suspend fun completeMfa(code:String):IdentityResult = IdentityResult.Error("Complete MFA in the configured browser adapter.")
    suspend fun enrollMfa(code:String?):IdentityResult = IdentityResult.Error("Authenticator enrollment requires the configured browser adapter.")
    suspend fun linkProvider(provider:SocialProvider):IdentityResult = IdentityResult.Error("Provider linking requires the configured browser adapter.")
    suspend fun changeEmail(email:String):IdentityResult = IdentityResult.Error("Verified email changes require the configured browser adapter.")

    suspend fun signIn(email: String, password: String): IdentityResult
    suspend fun signUp(name: String, email: String, password: String): IdentityResult
    suspend fun signInWith(provider: SocialProvider): IdentityResult
    suspend fun sendPasswordReset(email: String): IdentityResult
    suspend fun confirmPasswordReset(actionCode: String, password: String): IdentityResult
    suspend fun sendEmailVerification(): IdentityResult
    suspend fun refreshIdentity(): IdentityResult
    suspend fun signOut()
}
interface DatabaseAccessGateway {
    /** Send Firebase ID token over HTTPS; the server verifies it and looks up its UID. */
    suspend fun membership(idToken: String): Membership?
    /** Backend must authorize UID, validate setup and ignore any requested privilege as authority. */
    suspend fun saveSetup(idToken: String, setup: SetupDraft): Membership?
}
object IdentityServices {
    // Set these during platform startup when Firebase and the backend verifier are configured.
    // Provider SDKs manage credentials/refresh securely; this UI never persists tokens or passwords.
    var firebase: FirebaseIdentityProvider = browserIdentityProvider() ?: PasswordIdentityProvider()
    var database: DatabaseAccessGateway = BackendIdentityGateway()
}
private object UnconfiguredFirebase : FirebaseIdentityProvider {
    private fun unavailable() = IdentityResult.Error("Sign-in is temporarily unavailable. Please try again shortly.")
    override suspend fun signIn(email: String,password: String) = unavailable()
    override suspend fun signUp(name: String,email: String,password: String) = unavailable()
    override suspend fun signInWith(provider: SocialProvider) = unavailable()
    override suspend fun sendPasswordReset(email: String) = unavailable()
    override suspend fun confirmPasswordReset(actionCode: String,password: String) = unavailable()
    override suspend fun sendEmailVerification() = unavailable()
    override suspend fun refreshIdentity() = unavailable()
    override suspend fun signOut() {}
}
private object UnconfiguredDatabase : DatabaseAccessGateway {
    override suspend fun membership(idToken: String): Membership? = null
    override suspend fun saveSetup(idToken: String,setup: SetupDraft): Membership? = null
}
fun Membership.canEnter(admin: Boolean): Boolean = if(admin)
    com.example.admin.business.OperationsPolicy.canCoordinate(role,active,approved,organizationId.orEmpty())
    else active && approved
fun validEmail(value: String): Boolean = com.example.admin.business.OperationsPolicy.validEmail(value)
fun validNewPassword(value: String): Boolean = value.length >= 8 && value.length <= 128 && value.any { !it.isWhitespace() }

data class SetupDraft(
    val step: Int = 0, val location: String = "", val choices: Set<String> = emptySet(),
    val availability: String = "A few hours a month", val organization: String = "", val team: String = "",
    val requestedRole: String = "Coordinator", val updates: Boolean = true, val displayName: String = ""
) {
    fun encode(): String = listOf(step.toString(),location,choices.joinToString("~"),availability,organization,team,requestedRole,updates.toString())
        .joinToString("|") { it.replace("%","%25").replace("|","%7C").replace("\n","%0A") }
    companion object {
        fun decode(raw: String?): SetupDraft {
            val p=raw?.split('|')?.map { it.replace("%0A","\n").replace("%7C","|").replace("%25","%") } ?: return SetupDraft()
            if(p.size != 8) return SetupDraft()
            return SetupDraft(p[0].toIntOrNull()?.coerceIn(0,2) ?: 0,p[1],p[2].split('~').filter { it.isNotBlank() }.toSet(),p[3],p[4],p[5],p[6],p[7].toBooleanStrictOrNull() ?: true)
        }
    }
}

/** Accept a Firebase recovery link or its opaque code; Firebase validates expiry and reuse. */
fun recoveryCode(value: String): String {
    val input=value.trim()
    if(!input.contains("://")) return input
    val code=input.substringAfter('?', "").substringBefore('#').split('&')
        .firstOrNull { it.substringBefore('=') == "oobCode" }?.substringAfter('=', "") ?: return ""
    return Regex("%([0-9a-fA-F]{2})").replace(code) { it.groupValues[1].toInt(16).toChar().toString() }
}
