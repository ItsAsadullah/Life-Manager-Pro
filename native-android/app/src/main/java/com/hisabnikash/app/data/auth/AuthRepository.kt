package com.hisabnikash.app.data.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val credentialManager by lazy { CredentialManager.create(auth.app.applicationContext) }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) = auth.addAuthStateListener(listener)

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) = auth.removeAuthStateListener(listener)

    suspend fun signInWithGoogle(activity: Activity) {
        val token = requestGoogleIdToken(activity)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }

    suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    suspend fun ensureProfile(user: FirebaseUser) {
        val reference = firestore.collection("users").document(user.uid)
        if (reference.get().await().exists()) return

        reference.set(
            mapOf(
                "uid" to user.uid,
                "name" to (user.displayName ?: "Anonymous User"),
                "email" to (user.email ?: ""),
                "photoURL" to (user.photoUrl?.toString() ?: ""),
                "createdAt" to Instant.now().toString(),
            ),
        ).await()
    }

    private suspend fun requestGoogleIdToken(activity: Activity): String {
        val clientId = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName,
        ).takeIf { it != 0 }?.let(activity::getString)
            ?: error("Firebase configuration is missing default_web_client_id. Download a new google-services.json.")

        // This is the explicit button flow. Unlike the bottom-sheet lookup flow, it always
        // presents Google account sign-in for a first-time user on the device.
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = credentialManager.getCredential(activity, request).credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Google account credential পাওয়া যায়নি। আবার চেষ্টা করুন।")
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (error: GoogleIdTokenParsingException) {
            throw IllegalStateException("Google sign-in response পড়া যায়নি।", error)
        }
    }
}
