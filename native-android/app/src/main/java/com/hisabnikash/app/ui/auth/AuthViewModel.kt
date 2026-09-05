package com.hisabnikash.app.ui.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.hisabnikash.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object FirebaseConfigurationRequired : AuthState
    data class SignedIn(val user: FirebaseUser) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val firebaseConfigured = FirebaseApp.getApps(application).isNotEmpty()
    private val repository by lazy { AuthRepository() }
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user == null) {
            _state.value = AuthState.SignedOut
        } else {
            viewModelScope.launch {
                // Profile creation mirrors the existing React application's users/{uid} document.
                runCatching { repository.ensureProfile(user) }
                _state.value = AuthState.SignedIn(user)
            }
        }
    }

    init {
        if (!firebaseConfigured) {
            _state.value = AuthState.FirebaseConfigurationRequired
        } else {
            repository.addAuthStateListener(authListener)
        }
    }

    fun signIn(activity: Activity) = viewModelScope.launch {
        _state.value = AuthState.Loading
        runCatching { repository.signInWithGoogle(activity) }
            .onFailure {
                Log.e("HisabNikashAuth", "Google sign-in failed", it)
                _state.value = AuthState.Error(it.message ?: "Google sign-in ব্যর্থ হয়েছে।")
            }
    }

    fun signOut() = viewModelScope.launch {
        runCatching { repository.signOut() }
            .onFailure { _state.value = AuthState.Error(it.message ?: "Sign out ব্যর্থ হয়েছে।") }
    }

    fun dismissError() {
        _state.value = if (firebaseConfigured) AuthState.SignedOut else AuthState.FirebaseConfigurationRequired
    }

    override fun onCleared() {
        if (firebaseConfigured) repository.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
