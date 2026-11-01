@file:JsModule("firebase/auth")
@file:JsNonModule

package com.example.admin.features.auth

import kotlin.js.Promise

external val browserSessionPersistence: dynamic
external val TotpMultiFactorGenerator: dynamic

external class GoogleAuthProvider()
external class FacebookAuthProvider()
external class OAuthProvider(providerId: String)
external class SAMLAuthProvider(providerId: String)

external fun getAuth(app: dynamic): dynamic
external fun setPersistence(auth: dynamic, persistence: dynamic): Promise<dynamic>
external fun signInWithEmailAndPassword(auth: dynamic, email: String, password: String): Promise<dynamic>
external fun createUserWithEmailAndPassword(auth: dynamic, email: String, password: String): Promise<dynamic>
external fun updateProfile(user: dynamic, profile: dynamic): Promise<dynamic>
external fun sendEmailVerification(user: dynamic): Promise<dynamic>
external fun sendPasswordResetEmail(auth: dynamic, email: String): Promise<dynamic>
external fun confirmPasswordReset(auth: dynamic, code: String, password: String): Promise<dynamic>
external fun signOut(auth: dynamic): Promise<dynamic>
external fun signInWithPopup(auth: dynamic, provider: dynamic): Promise<dynamic>
external fun linkWithPopup(user: dynamic, provider: dynamic): Promise<dynamic>
external fun verifyBeforeUpdateEmail(user: dynamic, email: String): Promise<dynamic>
external fun getMultiFactorResolver(auth: dynamic, error: dynamic): dynamic
external fun multiFactor(user: dynamic): dynamic
