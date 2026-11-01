package com.example.admin.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.admin.features.auth.FirebaseIdentity
import com.example.admin.features.auth.FirebaseIdentityProvider
import com.example.admin.features.auth.IdentityResult
import com.example.admin.features.auth.SocialProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Android Firebase Auth implementation with encrypted persisted sessions and Google Credential Manager sign-in. */
class AndroidFirebaseIdentityProvider(
    private val activity: Activity,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : FirebaseIdentityProvider {
    private val credentialManager = CredentialManager.create(activity)

    override suspend fun signIn(email: String, password: String): IdentityResult = attempt {
        await(auth.signInWithEmailAndPassword(email, password))
        identityForCurrentUser()
    }

    override suspend fun signUp(name: String, email: String, password: String): IdentityResult = attempt {
        val user = await(auth.createUserWithEmailAndPassword(email, password)).user
            ?: return@attempt IdentityResult.Error("Firebase did not create an account. Please try again.")
        if (name.isNotBlank()) {
            await(user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()))
        }
        identityFor(user)
    }

    override suspend fun signInWith(provider: SocialProvider): IdentityResult = when (provider) {
        SocialProvider.Google -> signInWithGoogle()
        SocialProvider.Facebook -> IdentityResult.Error("Facebook sign-in is supported in the browser app; this Android adapter is not configured for Facebook.")
        SocialProvider.Apple, SocialProvider.Microsoft -> attempt {
            val providerId=when(provider){SocialProvider.Apple->"apple.com";SocialProvider.Facebook->"facebook.com";else->"microsoft.com"}
            val oauth=com.google.firebase.auth.OAuthProvider.newBuilder(providerId)
            await(auth.startActivityForSignInWithProvider(activity,oauth.build()))
            identityForCurrentUser()
        }
    }

    override suspend fun sendPasswordReset(email: String): IdentityResult = attempt {
        await(auth.sendPasswordResetEmail(email))
        IdentityResult.Completed
    }

    override suspend fun confirmPasswordReset(actionCode: String, password: String): IdentityResult = attempt {
        await(auth.confirmPasswordReset(actionCode, password))
        IdentityResult.Completed
    }

    override suspend fun sendEmailVerification(): IdentityResult = attempt {
        val user = auth.currentUser ?: return@attempt IdentityResult.Error("No signed-in account was found.")
        await(user.sendEmailVerification())
        IdentityResult.Completed
    }

    override suspend fun refreshIdentity(): IdentityResult = attempt {
        val user = auth.currentUser ?: return@attempt IdentityResult.Error("No signed-in account was found.")
        await(user.reload())
        identityFor(user)
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private suspend fun signInWithGoogle(): IdentityResult = attempt {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activity.getString(activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName).also { require(it != 0) { "Register the ResQOps Android app in Firebase and install google-services.json" } }))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val credential = credentialManager.getCredential(activity, request).credential
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return@attempt IdentityResult.Error("Google did not return an ID credential. Please try again.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        await(auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)))
        identityForCurrentUser()
    }

    private suspend fun identityForCurrentUser(): IdentityResult {
        val user = auth.currentUser ?: return IdentityResult.Error("No signed-in account was found.")
        return identityFor(user)
    }

    private suspend fun identityFor(user: FirebaseUser): IdentityResult {
        val token = await(user.getIdToken(true)).token
            ?: return IdentityResult.Error("Firebase could not issue a sign-in token. Please try again.")
        return IdentityResult.SignedIn(
            FirebaseIdentity(
                uid = user.uid,
                email = user.email.orEmpty(),
                emailVerified = user.isEmailVerified,
                idToken = token
            )
        )
    }

    private suspend fun <T> await(task: Task<T>): T = withContext(Dispatchers.IO) {
        Tasks.await(task, 10, TimeUnit.SECONDS)
    }

    private suspend fun attempt(block: suspend () -> IdentityResult): IdentityResult = try {
        block()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        IdentityResult.Error("Firebase could not complete that request. Check your connection and try again.")
    }
}