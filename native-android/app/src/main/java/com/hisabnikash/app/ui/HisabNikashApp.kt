package com.hisabnikash.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hisabnikash.app.ui.auth.AuthState
import com.hisabnikash.app.ui.auth.AuthViewModel
import com.hisabnikash.app.ui.dashboard.DashboardScreen
import com.hisabnikash.app.ui.notes.NotesScreen
import com.hisabnikash.app.ui.transactions.TransactionsScreen

@Composable
fun HisabNikashApp(authViewModel: AuthViewModel = viewModel()) {
    val state by authViewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        AuthState.Loading -> LoadingScreen()
        AuthState.SignedOut -> SignInScreen(authViewModel::signIn)
        AuthState.FirebaseConfigurationRequired -> FirebaseSetupScreen()
        is AuthState.SignedIn -> AuthenticatedApp(
            userId = current.user.uid,
            userName = current.user.displayName ?: current.user.email ?: "ব্যবহারকারী",
            onSignOut = authViewModel::signOut,
        )
        is AuthState.Error -> ErrorScreen(current.message, authViewModel::dismissError)
    }
}

@Composable
private fun AuthenticatedApp(userId: String, userName: String, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                userId = userId,
                userName = userName,
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = onSignOut,
            )
        }
        composable("transactions") {
            TransactionsScreen(userId = userId, onBack = { navController.popBackStack() })
        }
        composable("notes") {
            NotesScreen(userId = userId, onBack = { navController.popBackStack() })
        }
        listOf("debts", "market", "more", "settings").forEach { route ->
            composable(route) { FeaturePlaceholder(route) { navController.popBackStack() } }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { CircularProgressIndicator() }
}

@Composable
private fun SignInScreen(onSignIn: (Activity) -> Unit) {
    val activity = LocalContext.current as? Activity
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("হিসাব নিকাশ", style = MaterialTheme.typography.displaySmall)
            Text("আপনার হিসাব নিরাপদে রাখতে Google দিয়ে সাইন ইন করুন।", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            Button(onClick = { activity?.let(onSignIn) }, enabled = activity != null) { Text("Google দিয়ে সাইন ইন") }
        }
    }
}

@Composable
private fun FirebaseSetupScreen() {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Firebase setup প্রয়োজন", style = MaterialTheme.typography.headlineSmall)
            Text("app/google-services.json যোগ করুন, তারপর অ্যাপটি আবার চালু করুন।", modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onDismiss: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("সাইন-ইন করা যায়নি", style = MaterialTheme.typography.headlineSmall)
            Text(message, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
            Button(onClick = onDismiss) { Text("আবার চেষ্টা করুন") }
        }
    }
}

@Composable
private fun FeaturePlaceholder(route: String, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(route.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium)
            Text("এই feature-টি এখন React সংস্করণ থেকে native Compose-এ আনা হচ্ছে।", modifier = Modifier.padding(vertical = 12.dp))
            Button(onClick = onBack) { Text("ড্যাশবোর্ডে ফিরে যান") }
        }
    }
}
