package com.hisabnikash.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.DashboardScreen
import com.hisabnikash.app.ui.reports.ReportsScreen
import com.hisabnikash.app.ui.settings.SettingsScreen
import com.hisabnikash.app.ui.transactions.TransactionsScreen

private data class BottomNavItem(
    val destination: AppDestination,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        destination = AppDestination.Dashboard,
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        destination = AppDestination.Transactions,
        icon = Icons.Default.List
    ),
    BottomNavItem(
        destination = AppDestination.Reports,
        icon = Icons.Default.Assessment
    ),
    BottomNavItem(
        destination = AppDestination.Settings,
        icon = Icons.Default.Settings
    )
)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,

        bottomBar = {
            GlassBottomNavigation(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->

                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {

                            popUpTo(
                                AppDestination.Dashboard.route
                            ) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(
                route = AppDestination.Dashboard.route
            ) {
                DashboardScreen()
            }

            composable(
                route = AppDestination.Transactions.route
            ) {
                TransactionsScreen()
            }

            composable(
                route = AppDestination.Reports.route
            ) {
                ReportsScreen()
            }

            composable(
                route = AppDestination.Settings.route
            ) {
                val viewModel: com.hisabnikash.app.ui.dashboard.TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.popBackStack() },
                    onNavigateToBudget = { navController.popBackStack() },
                    onNavigateToDebts = { navController.popBackStack() }
                )
            }

            composable(
                route = AppDestination.MarketMemo.route
            ) {
                val viewModel: com.hisabnikash.app.ui.dashboard.TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.hisabnikash.app.ui.market.MarketMemoScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppDestination.Notes.route
            ) {
                val viewModel: com.hisabnikash.app.ui.dashboard.TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.hisabnikash.app.ui.notes.NotesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppDestination.AiChatbot.route
            ) {
                val viewModel: com.hisabnikash.app.ui.dashboard.TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.hisabnikash.app.ui.ai.AiChatbotScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppDestination.AiScanner.route
            ) {
                val viewModel: com.hisabnikash.app.ui.dashboard.TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                com.hisabnikash.app.ui.ai.AiScannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun GlassBottomNavigation(
    currentRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = Color.White.copy(alpha = 0.82f)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),

            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            bottomNavItems.forEach { item ->

                val selected =
                    currentRoute == item.destination.route

                GlassNavigationItem(
                    item = item,
                    selected = selected,
                    onClick = {
                        onDestinationSelected(item.destination)
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(
                width = 72.dp,
                height = 58.dp
            )
            .clickable(
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (selected) {

            GlassCard(
                modifier = Modifier.size(
                    width = 54.dp,
                    height = 32.dp
                ),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = Color(33, 150, 243)
                    .copy(alpha = 0.16f)
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.destination.label,
                        modifier = Modifier.size(22.dp),
                        tint = Color(33, 150, 243)
                    )
                }
            }

        } else {

            Icon(
                imageVector = item.icon,
                contentDescription = item.destination.label,
                modifier = Modifier.size(24.dp),
                tint = Color(100, 105, 115)
            )
        }

        Text(
            text = item.destination.label,
            color = if (selected) {
                Color(33, 150, 243)
            } else {
                Color(100, 105, 115)
            }
        )
    }
}
