package com.hisabnikash.app.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager private constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val userDisplayName: String
        get() = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "ব্যবহারকারী"

    val userEmail: String?
        get() = auth.currentUser?.email

    val userPhotoUrl: String?
        get() = auth.currentUser?.photoUrl?.toString()

    /**
     * ইমেইল ও পাসওয়ার্ড দিয়ে নতুন অ্যাকাউন্ট তৈরি করা
     */
    suspend fun signUpWithEmail(name: String, email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("ইউজার তৈরিতে সমস্যা হয়েছে")

            // নাম আপডেট করা
            if (name.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            _currentUser.value = auth.currentUser
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signUpWithEmail", e)
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * ইমেইল ও পাসওয়ার্ড দিয়ে লগইন করা
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("লগইন সম্পন্ন করা যায়নি")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithEmail", e)
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * গুগল আইডি টোকেন দিয়ে ফায়ারবেজে সাইন-ইন করা
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("গুগল লগইন সম্পন্ন হয়নি")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signInWithGoogle", e)
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * পাসওয়ার্ড রিসেট লিংক পাঠানো
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendPasswordReset", e)
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * সাইন আউট করা
     */
    fun signOut() {
        try {
            auth.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in signOut", e)
        }
    }

    /**
     * ফায়ারবেজ এরর মেসেজগুলোকে সহজ বাংলায় রূপান্তর
     */
    private fun mapAuthException(e: Exception): Exception {
        val msg = e.message ?: ""
        val bengaliMessage = when {
            msg.contains("The email address is already in use", ignoreCase = true) ->
                "এই ইমেইল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট তৈরি করা আছে।"
            msg.contains("The email address is badly formatted", ignoreCase = true) ->
                "ইমেইল ঠিকানাটি সঠিক নয়। অনুগ্রহ করে সঠিক ইমেইল দিন।"
            msg.contains("Password should be at least 6 characters", ignoreCase = true) ->
                "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            msg.contains("wrong password", ignoreCase = true) ||
            msg.contains("There is no user record", ignoreCase = true) ->
                "ইমেইল বা পাসওয়ার্ড ভুল হয়েছে। পুনরায় চেষ্টা করুন।"
            msg.contains("network error", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                "ইন্টারনেট সংযোগ পাওয়া যায়নি। আপনার ডেটা বা ওয়াইফাই চেক করুন।"
            msg.contains("too many requests", ignoreCase = true) ->
                "অতিরিক্ত ভুল চেষ্টার কারণে সাময়িকভাবে বন্ধ রাখা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন।"
            else -> e.localizedMessage ?: "একটি অপ্রত্যাশিত সমস্যা হয়েছে। আবার চেষ্টা করুন।"
        }
        return Exception(bengaliMessage)
    }

    companion object {
        private const val TAG = "AuthManager"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }
    }
}
